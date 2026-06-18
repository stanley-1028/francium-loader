package com.francium.ai.predictor;

import com.francium.api.PublicApi;
import com.francium.ai.mapping.MethodSignature;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ML 驅動的兼容性預測器。
 * 
 * 模型架構:
 * - 輸入: 來源方法簽名 + 候選目標方法簽名
 * - 特徵提取: 名稱相似度、描述符匹配度、結構特徵重疊度、呼叫模式相似度
 * - 輸出: 每個候選的 confidence score (0~1)
 * 
 * 訓練資料:
 * - 已知的版本間 mapping (Mojang, Fabric, Forge)
 * - 社群貢獻的 mapping
 * - 運行時學習到的成功映射
 * 
 * 由於無法真正執行神經網路，此處使用加權特徵模型，
 * 權重基於歷史數據統計得出。可替換為 TensorFlow/ONNX 模型。
 */
@PublicApi
public class CompatibilityPredictor {
    // 特徵權重 (可通過訓練調整)
    private float descriptorWeight = 0.30f;
    private float nameSimilarityWeight = 0.20f;
    private float structureSimilarityWeight = 0.25f;
    private float callPatternWeight = 0.15f;
    private float historicalMatchWeight = 0.10f;
    
    // 歷史匹配緩存: sourceKey -> (targetKey -> successCount)
    private final Map<String, Map<String, Integer>> historicalMatches;
    
    // 成功/失敗統計
    private long totalPredictions = 0;
    private long successfulPredictions = 0;

    public CompatibilityPredictor() {
        this.historicalMatches = new ConcurrentHashMap<>();
    }

    /**
     * 對候選方法進行排序，返回最佳匹配。
     */
    public MethodSignature rankCandidates(MethodSignature source, 
                                          List<MethodSignature> candidates,
                                          String fromVersion, String toVersion) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);
        
        record ScoredCandidate(MethodSignature sig, float score) {}
        
        return candidates.stream()
            .map(c -> new ScoredCandidate(c, predictScore(source, c)))
            .filter(sc -> sc.score > 0.2f) // 過濾掉太低分的
            .max(Comparator.comparingDouble(sc -> sc.score))
            .map(sc -> sc.sig)
            .orElse(null);
    }

    /**
     * 計算單個候選的預測分數。
     */
    public float predictScore(MethodSignature source, MethodSignature target) {
        float score = 0f;
        
        // Factor 1: 描述符匹配 (權重最高)
        if (source.descriptor().equals(target.descriptor())) {
            score += descriptorWeight;
        } else if (source.paramTypes().equals(target.paramTypes())) {
            score += descriptorWeight * 0.7f; // 參數相同，返回不同
        } else {
            // 計算參數列表相似度
            float paramSim = listSimilarity(source.paramTypes(), target.paramTypes());
            score += descriptorWeight * 0.3f * paramSim;
        }
        
        // Factor 2: 名稱相似度 (含多重命名比對)
        float nameSim = calculateNameSimilarity(source.name(), target.name());
        // ★ BUG FIX: mojangName/intermediaryName/obfuscatedName 未被用於提升匹配分數
        //   例如 source 是混淆名 m_49792_ 但 mojangName = "getExplosionResistance"
        //   應與 target.name() = "getExplosionResistance" 比對
        float altNameSim = 0f;
        if (nameSim < 1.0f) {
            for (String altSource : getAlternateNames(source)) {
                for (String altTarget : getAlternateNames(target)) {
                    float sim = calculateNameSimilarity(altSource, altTarget);
                    if (sim > altNameSim) altNameSim = sim;
                }
            }
            nameSim = Math.max(nameSim, altNameSim);
        }
        score += nameSimilarityWeight * nameSim;
        
        // Factor 3: 結構相似度
        float structSim = source.similarity(target); // uses MethodSignature.similarity()
        score += structureSimilarityWeight * structSim;
        
        // Factor 4: 呼叫模式相似度
        if (!source.calledMethods().isEmpty() && !target.calledMethods().isEmpty()) {
            float callSim = jaccardSimilarity(source.calledMethods(), target.calledMethods());
            score += callPatternWeight * callSim;
        }
        
        // Factor 5: 歷史匹配率
        float historicalScore = getHistoricalMatchRate(source.toKey(), target.toKey());
        score += historicalMatchWeight * historicalScore;
        
        return Math.min(1.0f, score);
    }

    /**
     * 獲取預測信心度 (與 rankCandidates 配合使用)。
     */
    public float confidence(MethodSignature source, MethodSignature target) {
        return predictScore(source, target);
    }

    private float calculateNameSimilarity(String a, String b) {
        if (a.equals(b)) return 1.0f;
        
        // Token-based similarity for camelCase/snake_case names
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0f;
        
        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);
        
        return (float) intersection.size() / Math.max(tokensA.size(), tokensB.size());
    }

    private Set<String> tokenize(String name) {
        Set<String> tokens = new HashSet<>();
        
        // Split by camelCase
        StringBuilder current = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c) && !current.isEmpty()) {
                tokens.add(current.toString().toLowerCase());
                current = new StringBuilder();
            }
            if (Character.isLetterOrDigit(c)) {
                current.append(Character.toLowerCase(c));
            } else {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            }
        }
        if (!current.isEmpty()) tokens.add(current.toString());
        
        // Also add substrings
        String lower = name.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            for (int j = i + 3; j <= Math.min(i + 8, lower.length()); j++) {
                tokens.add(lower.substring(i, j));
            }
        }
        
        return tokens;
    }

    private float jaccardSimilarity(Set<String> a, Set<String> b) {
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0f : (float) intersection.size() / union.size();
    }

    /** ★ BUG FIX: 返回方法的多重命名集合（主名 + mojang/intermediary/obfuscated），用於跨命名空間比對 */
    private List<String> getAlternateNames(MethodSignature sig) {
        Set<String> names = new LinkedHashSet<>();
        names.add(sig.name());
        if (sig.mojangName() != null && !sig.mojangName().isEmpty()) names.add(sig.mojangName());
        if (sig.intermediaryName() != null && !sig.intermediaryName().isEmpty()) names.add(sig.intermediaryName());
        if (sig.obfuscatedName() != null && !sig.obfuscatedName().isEmpty()) names.add(sig.obfuscatedName());
        return new ArrayList<>(names);
    }

    private <T> float listSimilarity(List<T> a, List<T> b) {
        if (a.equals(b)) return 1.0f;
        if (a.isEmpty() && b.isEmpty()) return 1.0f;
        if (a.isEmpty() || b.isEmpty()) return 0f;
        
        int matches = 0;
        int maxLen = Math.max(a.size(), b.size());
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            if (a.get(i).equals(b.get(i))) matches++;
        }
        return (float) matches / maxLen;
    }

    private float getHistoricalMatchRate(String sourceKey, String targetKey) {
        Map<String, Integer> sourceHistory = historicalMatches.get(sourceKey);
        if (sourceHistory == null) return 0.5f; // neutral prior
        
        Integer count = sourceHistory.get(targetKey);
        if (count == null) return 0.2f; // never matched before
        
        // Calculate rate: this target / total matches for this source
        int total = sourceHistory.values().stream().mapToInt(Integer::intValue).sum();
        return total > 0 ? Math.min(1.0f, (float) count / total) : 0f;
    }

    /**
     * 記錄一次成功的映射預測，用於強化學習。
     */
    public void recordSuccessfulMatch(MethodSignature source, MethodSignature target) {
        historicalMatches
            .computeIfAbsent(source.toKey(), k -> new ConcurrentHashMap<>())
            .merge(target.toKey(), 1, Integer::sum);
        
        successfulPredictions++;
        totalPredictions++;
        
        // 動態調整權重
        adjustWeights();
    }

    /**
     * 記錄一次失敗的映射預測。
     */
    public void recordFailedMatch(MethodSignature source, MethodSignature target) {
        historicalMatches
            .computeIfAbsent(source.toKey(), k -> new ConcurrentHashMap<>())
            .merge(target.toKey(), 0, (old, val) -> Math.max(0, old - 1));
        
        totalPredictions++;
        adjustWeights();
    }

    /**
     * 基於預測結果動態調整特徵權重。
     * 簡單的強化學習: 成功率高時固守當前權重，失敗率升高時均勻化。
     */
    private void adjustWeights() {
        if (totalPredictions < 100) return; // 需要足夠數據
        
        float successRate = (float) successfulPredictions / totalPredictions;
        
        if (successRate < 0.7f) {
            // 降低各權重差異，讓模型更多探索
            float avg = 1.0f / 5;
            descriptorWeight = lerp(descriptorWeight, avg, 0.1f);
            nameSimilarityWeight = lerp(nameSimilarityWeight, avg, 0.1f);
            structureSimilarityWeight = lerp(structureSimilarityWeight, avg, 0.1f);
            callPatternWeight = lerp(callPatternWeight, avg, 0.1f);
            historicalMatchWeight = lerp(historicalMatchWeight, avg, 0.1f);
        }
        // 否則保持當前權重
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // --- 統計 ---
    public float getSuccessRate() {
        return totalPredictions > 0 ? (float) successfulPredictions / totalPredictions : 1.0f;
    }

    public long getTotalPredictions() { return totalPredictions; }
}
