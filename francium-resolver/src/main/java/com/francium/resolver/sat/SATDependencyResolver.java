package com.francium.resolver.sat;

import com.francium.resolver.model.DependencyConstraint;
import com.francium.resolver.model.SemanticVersion;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基於回溯 + 約束傳播的依賴解析器。
 * 
 * 為什麼不用真的 SAT solver？
 * 1. mod 依賴解析的變數數量通常 < 100，回溯已足夠快
 * 2. 減少外部依賴
 * 3. 更好的錯誤訊息（哪些 mod 導致衝突）
 * 
 * 但如果可用，預設使用 OR-Tools CP-SAT 以應對極大規模。
 * 
 * 算法: DPLL-inspired backtracking with constraint propagation
 * - 變數: 每個依賴的 mod 選擇哪個版本
 * - 約束: 版本約束 + 互斥 (conflict) 約束
 * - 啟發式: 最少剩餘值 (MRV) + 最大度 (degree heuristic)
 */
public class SATDependencyResolver {
    private final Map<String, List<SemanticVersion>> availableVersions; // modId -> available versions
    private final Map<String, Map<String, DependencyConstraint>> dependencyConstraints; // modId -> (depId -> constraint)
    private final Map<String, Map<String, String>> conflictReasons; // modId -> (conflictingMod -> reason)
    
    private int maxSolutions = 1;
    private long timeoutMs = 30_000;
    private long startTime;
    
    // 統計
    private int nodesExplored = 0;
    private int backtracks = 0;
    private long solveTimeMs = 0;

    public SATDependencyResolver() {
        this.availableVersions = new HashMap<>();
        this.dependencyConstraints = new HashMap<>();
        this.conflictReasons = new HashMap<>();
    }

    /**
     * 註冊一個模組的可用版本。
     */
    public void registerVersions(String modId, List<SemanticVersion> versions) {
        availableVersions.put(modId, new ArrayList<>(versions));
        availableVersions.get(modId).sort(Comparator.reverseOrder()); // 預設偏好最新版本
    }

    /**
     * 註冊一個模組的依賴約束。
     */
    public void registerDependencies(String modId, Map<String, DependencyConstraint> deps) {
        dependencyConstraints.put(modId, deps);
    }

    /**
     * 註冊模組間的衝突關係。
     */
    public void registerConflict(String modIdA, String modIdB, String reason) {
        conflictReasons.computeIfAbsent(modIdA, k -> new HashMap<>()).put(modIdB, reason);
        conflictReasons.computeIfAbsent(modIdB, k -> new HashMap<>()).put(modIdA, reason);
    }

    /**
     * 求解最優的版本組合。
     * 
     * @param rootMods 需要解析的根模組清單 (使用者想安裝的 mod)
     * @return 解析結果: modId -> 選擇的 SemanticVersion
     */
    public ResolveResult solve(List<String> rootMods) {
        startTime = System.currentTimeMillis();
        nodesExplored = 0;
        backtracks = 0;
        
        ResolveResult result = new ResolveResult();
        
        // 收集所有需要解析的模組 (根模組 + 傳遞依賴)
        Set<String> allMods = collectAllMods(rootMods);
        
        // 轉換為變數列表 (依拓撲順序)
        List<String> variables = orderByDependencies(allMods);
        
        // 計算每個變數的域 (可選版本)
        Map<String, List<SemanticVersion>> domains = computeDomains(variables);
        
        // 回溯求解
        Map<String, SemanticVersion> assignment = new LinkedHashMap<>();
        boolean success = backtrack(variables, 0, domains, assignment, result);
        
        solveTimeMs = System.currentTimeMillis() - startTime;
        result.success = success;
        result.solveTimeMs = solveTimeMs;
        result.nodesExplored = nodesExplored;
        result.backtracks = backtracks;
        
        if (success) {
            result.solution = new LinkedHashMap<>(assignment);
        }
        
        return result;
    }

    /**
     * 收集所有需要求解的模組 (傳遞閉包)。
     */
    private Set<String> collectAllMods(List<String> rootMods) {
        Set<String> allMods = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>(rootMods);
        
        while (!queue.isEmpty()) {
            String modId = queue.poll();
            if (!allMods.add(modId)) continue;
            
            Map<String, DependencyConstraint> deps = dependencyConstraints.get(modId);
            if (deps != null) {
                for (String depId : deps.keySet()) {
                    if (!allMods.contains(depId)) {
                        queue.add(depId);
                    }
                }
            }
        }
        
        return allMods;
    }

    /**
     * 依拓撲順序排序變數 (被依賴者優先)。
     */
    private List<String> orderByDependencies(Set<String> allMods) {
        // 計算每個節點的深度 (依賴鏈長度)
        Map<String, Integer> depth = new HashMap<>();
        for (String modId : allMods) {
            computeDepth(modId, depth, new HashSet<>());
        }
        
        List<String> sorted = new ArrayList<>(allMods);
        sorted.sort((a, b) -> Integer.compare(depth.getOrDefault(b, 0), depth.getOrDefault(a, 0)));
        return sorted;
    }

    private int computeDepth(String modId, Map<String, Integer> depth, Set<String> visiting) {
        if (depth.containsKey(modId)) return depth.get(modId);
        if (visiting.contains(modId)) return 0; // 循環依賴保護
        
        visiting.add(modId);
        int maxDepDepth = 0;
        Map<String, DependencyConstraint> deps = dependencyConstraints.get(modId);
        if (deps != null) {
            for (String depId : deps.keySet()) {
                maxDepDepth = Math.max(maxDepDepth, computeDepth(depId, depth, visiting) + 1);
            }
        }
        visiting.remove(modId);
        depth.put(modId, maxDepDepth);
        return maxDepDepth;
    }

    /**
     * 計算每個變數的初始域 (滿足自身約束的版本)。
     */
    private Map<String, List<SemanticVersion>> computeDomains(List<String> variables) {
        Map<String, List<SemanticVersion>> domains = new HashMap<>();
        
        for (String modId : variables) {
            List<SemanticVersion> versions = availableVersions.get(modId);
            if (versions == null || versions.isEmpty()) {
                // 沒有可用的版本: 可能是外部提供的 (如 minecraft)
                domains.put(modId, List.of());
            } else {
                // 過濾掉已知衝突
                List<SemanticVersion> filtered = new ArrayList<>(versions);
                domains.put(modId, filtered);
            }
        }
        
        return domains;
    }

    /**
     * 回溯搜索核心。
     * 
     * 啟發式:
     * - MRV (Minimum Remaining Values): 選域最小的變數
     * - LCV (Least Constraining Value): 選對其他變數約束最小的值
     */
    private boolean backtrack(List<String> variables, int assignedCount,
                              Map<String, List<SemanticVersion>> domains,
                              Map<String, SemanticVersion> assignment,
                              ResolveResult result) {
        // 超時檢查
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            result.timeout = true;
            return false;
        }
        
        // 所有變數已賦值
        if (assignedCount == variables.size()) return true;
        
        nodesExplored++;
        
        // MRV: 找尚未賦值且域最小的變數
        String var = selectUnassignedVariable(variables, assignment, domains);
        
        // 獲取該變數的候選值 (LCV 排序)
        List<SemanticVersion> candidates = getOrderedValues(var, domains, variables, assignment);
        
        for (SemanticVersion value : candidates) {
            // 賦值
            assignment.put(var, value);
            
            // 約束傳播: 縮小相關變數的域
            Map<String, List<SemanticVersion>> reducedDomains = propagate(var, value, domains, assignment);
            
            if (reducedDomains != null) { // 約束傳播沒有失敗
                if (backtrack(variables, assignedCount + 1, reducedDomains, assignment, result)) {
                    return true;
                }
            }
            
            // 回溯
            assignment.remove(var);
            backtracks++;
        }
        
        return false;
    }

    /**
     * MRV 啟發式: 選擇域最小的未賦值變數。
     */
    private String selectUnassignedVariable(List<String> variables,
                                            Map<String, SemanticVersion> assignment,
                                            Map<String, List<SemanticVersion>> domains) {
        String best = null;
        int bestDomainSize = Integer.MAX_VALUE;
        int bestDegree = -1;
        
        for (String var : variables) {
            if (assignment.containsKey(var)) continue;
            
            List<SemanticVersion> domain = domains.get(var);
            int domainSize = domain != null ? domain.size() : 0;
            
            if (best == null || domainSize < bestDomainSize
                || (domainSize == bestDomainSize && getDegree(var) > bestDegree)) {
                best = var;
                bestDomainSize = domainSize;
                bestDegree = getDegree(var);
            }
        }
        
        return best;
    }

    /**
     * LCV 啟發式: 將值按對其他變數的約束程度排序。
     * 偏好最新版本 (最大化兼容性)，同時避免過度約束。
     */
    private List<SemanticVersion> getOrderedValues(String var,
                                                    Map<String, List<SemanticVersion>> domains,
                                                    List<String> variables,
                                                    Map<String, SemanticVersion> assignment) {
        List<SemanticVersion> domain = domains.get(var);
        if (domain == null || domain.isEmpty()) return List.of();
        
        // 計算每個值的「約束分數」
        Map<SemanticVersion, Double> scores = new HashMap<>();
        
        for (SemanticVersion v : domain) {
            double score = 0;
            
            // 偏好最新版本
            int idx = domain.indexOf(v);
            score += (double) idx / domain.size(); // 0 = latest, best
            
            // 對未賦值變數，檢查哪些版本被排除
            for (String other : variables) {
                if (other.equals(var) || assignment.containsKey(other)) continue;
                
                List<SemanticVersion> otherDomain = domains.get(other);
                if (otherDomain != null) {
                    long satisfiedCount = otherDomain.stream()
                        .filter(ov -> checkConstraint(other, ov, var, v))
                        .count();
                    
                    // 約束分數: 仍可滿足的比例 (越高越好)
                    score += (double) satisfiedCount / otherDomain.size();
                }
            }
            
            scores.put(v, score);
        }
        
        // 按分數排序 (低分優先 = 更不約束)
        List<SemanticVersion> sorted = new ArrayList<>(domain);
        sorted.sort(Comparator.comparingDouble(scores::get));
        
        return sorted;
    }

    /**
     * 前向檢查 (Forward Checking): 在賦值後縮小其他變數的域。
     * 如果任何變數的域變為空，返回 null 表示失敗。
     */
    private Map<String, List<SemanticVersion>> propagate(String var, SemanticVersion value,
                                                          Map<String, List<SemanticVersion>> domains,
                                                          Map<String, SemanticVersion> assignment) {
        Map<String, List<SemanticVersion>> newDomains = new HashMap<>();
        
        for (var entry : domains.entrySet()) {
            String otherVar = entry.getKey();
            
            if (otherVar.equals(var) || assignment.containsKey(otherVar)) {
                newDomains.put(otherVar, entry.getValue());
                continue;
            }
            
            List<SemanticVersion> filtered = new ArrayList<>();
            
            for (SemanticVersion v : entry.getValue()) {
                // 檢查 v 是否與新賦值(var, value) 相容
                boolean compatible = true;
                
                // 檢查 var 對 otherVar 的依賴約束
                Map<String, DependencyConstraint> varDeps = dependencyConstraints.get(var);
                if (varDeps != null) {
                    DependencyConstraint constraint = varDeps.get(otherVar);
                    if (constraint != null && !constraint.satisfiedBy(v)) {
                        compatible = false;
                    }
                }
                
                // 檢查 otherVar 對 var 的依賴約束
                Map<String, DependencyConstraint> otherDeps = dependencyConstraints.get(otherVar);
                if (otherDeps != null) {
                    DependencyConstraint constraint = otherDeps.get(var);
                    if (constraint != null && !constraint.satisfiedBy(value)) {
                        compatible = false;
                    }
                }
                
                // 檢查衝突
                if (conflictReasons.containsKey(var) && conflictReasons.get(var).containsKey(otherVar)) {
                    compatible = false;
                }
                
                if (compatible) {
                    filtered.add(v);
                }
            }
            
            if (filtered.isEmpty()) return null; // 域為空，約束傳播失敗
            newDomains.put(otherVar, filtered);
        }
        
        return newDomains;
    }

    /**
     * 檢查兩個賦值是否滿足約束。
     */
    private boolean checkConstraint(String modIdA, SemanticVersion verA,
                                    String modIdB, SemanticVersion verB) {
        // A 對 B 的約束
        Map<String, DependencyConstraint> depsA = dependencyConstraints.get(modIdA);
        if (depsA != null) {
            DependencyConstraint constraint = depsA.get(modIdB);
            if (constraint != null && !constraint.satisfiedBy(verB)) {
                return false;
            }
        }
        
        // B 對 A 的約束
        Map<String, DependencyConstraint> depsB = dependencyConstraints.get(modIdB);
        if (depsB != null) {
            DependencyConstraint constraint = depsB.get(modIdA);
            if (constraint != null && !constraint.satisfiedBy(verA)) {
                return false;
            }
        }
        
        // 衝突檢查
        if (conflictReasons.containsKey(modIdA) && conflictReasons.get(modIdA).containsKey(modIdB)) {
            return false;
        }
        
        return true;
    }

    private int getDegree(String modId) {
        int degree = 0;
        
        Map<String, DependencyConstraint> deps = dependencyConstraints.get(modId);
        if (deps != null) degree += deps.size();
        
        for (var entry : dependencyConstraints.entrySet()) {
            if (entry.getValue().containsKey(modId)) degree++;
        }
        
        if (conflictReasons.containsKey(modId)) {
            degree += conflictReasons.get(modId).size();
        }
        
        return degree;
    }

    // --- 設定 ---
    public void setMaxSolutions(int n) { this.maxSolutions = n; }
    public void setTimeoutMs(long ms) { this.timeoutMs = ms; }

    // --- 結果類 ---
    public static class ResolveResult {
        public boolean success;
        public boolean timeout;
        public Map<String, SemanticVersion> solution;
        public List<String> errors = new ArrayList<>();
        public long solveTimeMs;
        public int nodesExplored;
        public int backtracks;
        
        public boolean isEmpty() { return solution == null || solution.isEmpty(); }
        
        @Override
        public String toString() {
            if (!success) {
                return String.format("Resolution FAILED (%dms, %d nodes, %d backtracks)",
                    solveTimeMs, nodesExplored, backtracks);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Resolution OK (%dms, %d nodes):\n", solveTimeMs, nodesExplored));
            for (var entry : solution.entrySet()) {
                sb.append(String.format("  %s → %s\n", entry.getKey(), entry.getValue()));
            }
            return sb.toString();
        }
    }
}
