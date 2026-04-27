package com.francium.graph;

import com.francium.loader.ModManifest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基於 DAG 的模組依賴圖。
 * 
 * 核心創新:
 * 1. 構建有向無環圖表示模組依賴關係 (內建鄰接表，零外部依賴)
 * 2. 拓撲分層: 獨立模組進入同一層，可並行加載
 * 3. 環檢測: 發現循環依賴即報錯
 * 4. 加載時間預估: 為每一層計算並行加載瓶頸
 */
public class ModGraph {
    // 鄰接表: 依賴者 → 被依賴者 (A depends on B → edge A→B)
    private final Map<String, Set<String>> adjacency;
    // 反向鄰接表: 被依賴者 → 依賴者 (B → [A], 用於快速查詢誰依賴 B)
    private final Map<String, Set<String>> reverseAdjacency;
    private final Map<String, ModManifest> manifestMap;
    private final Map<String, String> versionMap;
    
    // 拓撲分層結果
    private List<Set<String>> layers;
    // 每個節點的最長路徑長度
    private Map<String, Integer> longestPath;
    
    public ModGraph() {
        this.adjacency = new ConcurrentHashMap<>();
        this.reverseAdjacency = new ConcurrentHashMap<>();
        this.manifestMap = new ConcurrentHashMap<>();
        this.versionMap = new ConcurrentHashMap<>();
    }

    /**
     * 添加模組節點及其依賴邊。
     * 邊方向: 依賴者 → 被依賴者
     */
    public boolean addMod(ModManifest manifest, String resolvedVersion) {
        String nodeId = manifest.modId();
        
        if (manifestMap.containsKey(nodeId)) {
            ModManifest existing = manifestMap.get(nodeId);
            if (!existing.version().equals(manifest.version())) {
                throw new ModConflictException(
                    "Duplicate mod " + nodeId + ": " + existing.version() + " vs " + manifest.version());
            }
            return false;
        }
        
        adjacency.putIfAbsent(nodeId, new LinkedHashSet<>());
        reverseAdjacency.putIfAbsent(nodeId, new LinkedHashSet<>());
        manifestMap.put(nodeId, manifest);
        versionMap.put(nodeId, resolvedVersion);
        
        for (String depId : manifest.dependencies().keySet()) {
            adjacency.putIfAbsent(depId, new LinkedHashSet<>());
            reverseAdjacency.putIfAbsent(depId, new LinkedHashSet<>());
            // edge direction: dependency → depender (B must load before A)
            adjacency.get(depId).add(nodeId);
            reverseAdjacency.get(nodeId).add(depId);
        }
        
        return true;
    }

    /**
     * 標記外部提供的模組。
     */
    public void addExternalProvider(String modId) {
        adjacency.putIfAbsent(modId, new LinkedHashSet<>());
        reverseAdjacency.putIfAbsent(modId, new LinkedHashSet<>());
    }

    /**
     * 執行拓撲分層 (Kahn 演算法變體)。
     * 
     * 與標準 Kahn 的差別: 我們不是簡單排序，而是分層——
     * 同一層的節點入度同時歸零，它們之間沒有依賴關係，可以並行處理。
     * 
     * 複雜度: O(V+E)
     */
    public void buildLayers() {
        if (adjacency.isEmpty()) {
            layers = List.of();
            return;
        }
        
        // 計算入度
        Map<String, Integer> inDegree = new HashMap<>();
        for (String v : adjacency.keySet()) {
            inDegree.put(v, 0);
        }
        for (var entry : adjacency.entrySet()) {
            for (String dep : entry.getValue()) {
                inDegree.merge(dep, 0, Integer::sum);
                inDegree.merge(entry.getKey(), 0, Integer::sum);
                inDegree.put(dep, inDegree.get(dep) + 1);
            }
        }
        
        // BFS 分層
        List<Set<String>> result = new ArrayList<>();
        Map<String, Integer> nodeLayer = new HashMap<>();
        longestPath = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        
        // 入度 = 0 → Layer 0
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
                nodeLayer.put(entry.getKey(), 0);
                longestPath.put(entry.getKey(), 0);
            }
        }
        
        if (queue.isEmpty() && !adjacency.isEmpty()) {
            throw new CircularDependencyException(detectCycles());
        }
        
        Map<Integer, Set<String>> layerMap = new LinkedHashMap<>();
        
        while (!queue.isEmpty()) {
            String node = queue.poll();
            int layer = nodeLayer.get(node);
            
            layerMap.computeIfAbsent(layer, k -> new LinkedHashSet<>()).add(node);
            
            // 處理此節點的依賴對象 (通過鄰接表: node → dependency)
            Set<String> deps = adjacency.get(node);
            if (deps != null) {
                for (String dep : deps) {
                    int newDegree = inDegree.get(dep) - 1;
                    inDegree.put(dep, newDegree);
                    
                    int newPath = longestPath.getOrDefault(node, 0) + 1;
                    longestPath.merge(dep, newPath, Math::max);
                    
                    if (newDegree == 0) {
                        nodeLayer.put(dep, layer + 1);
                        queue.add(dep);
                    }
                }
            }
        }
        
        // 檢查未處理節點
        if (nodeLayer.size() < adjacency.size()) {
            throw new CircularDependencyException(detectCycles());
        }
        
        // 按層排序
        result.addAll(
            layerMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList()
        );
        
        this.layers = result;
    }

    /**
     * DFS 檢測循環依賴。
     */
    private List<List<String>> detectCycles() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        for (String start : adjacency.keySet()) {
            if (!visited.contains(start)) {
                List<String> path = new ArrayList<>();
                Set<String> inPath = new HashSet<>();
                dfsFindCycle(start, visited, path, inPath, cycles);
            }
        }
        return cycles;
    }

    private void dfsFindCycle(String current, Set<String> visited,
                               List<String> path, Set<String> inPath,
                               List<List<String>> cycles) {
        visited.add(current);
        path.add(current);
        inPath.add(current);
        
        Set<String> deps = adjacency.get(current);
        if (deps != null) {
            for (String dep : deps) {
                if (inPath.contains(dep)) {
                    // 找到環
                    int startIdx = path.indexOf(dep);
                    List<String> cycle = new ArrayList<>(path.subList(startIdx, path.size()));
                    cycle.add(dep);
                    cycles.add(cycle);
                } else if (!visited.contains(dep)) {
                    dfsFindCycle(dep, visited, path, inPath, cycles);
                }
            }
        }
        
        path.remove(path.size() - 1);
        inPath.remove(current);
    }

    // --- Public API ---
    public List<Set<String>> getLayers() {
        if (layers == null) buildLayers();
        return Collections.unmodifiableList(layers);
    }

    /**
     * 計算並行加載預估時間。
     */
    public long estimateParallelLoadTime() {
        if (layers == null) buildLayers();
        long total = 0;
        for (Set<String> layer : layers) {
            long layerMax = 0;
            for (String modId : layer) {
                ModManifest m = manifestMap.get(modId);
                if (m != null) layerMax = Math.max(layerMax, m.estimatedLoadTimeMs());
            }
            total += layerMax;
        }
        return total;
    }

    public long estimateSequentialLoadTime() {
        return manifestMap.values().stream()
            .mapToLong(ModManifest::estimatedLoadTimeMs)
            .sum();
    }

    public double getSpeedupRatio() {
        long p = estimateParallelLoadTime();
        long s = estimateSequentialLoadTime();
        return p > 0 ? (double) s / p : 1.0;
    }

    public ModManifest getManifest(String modId) { return manifestMap.get(modId); }
    public Map<String, ModManifest> getAllManifests() { return Collections.unmodifiableMap(manifestMap); }
    public int getModCount() { return manifestMap.size(); }
    public int getLayerCount() { if (layers == null) buildLayers(); return layers.size(); }
    public int getTotalNodeCount() { return adjacency.size(); }
    public int getTotalEdgeCount() {
        return adjacency.values().stream().mapToInt(Set::size).sum();
    }

    // --- Exception types ---
    public static class ModConflictException extends RuntimeException {
        public ModConflictException(String msg) { super(msg); }
    }

    public static class CircularDependencyException extends RuntimeException {
        private final List<List<String>> cycles;
        public CircularDependencyException(List<List<String>> cycles) {
            super("Circular dependencies detected: " + cycles);
            this.cycles = cycles;
        }
        public List<List<String>> cycles() { return cycles; }
    }
}
