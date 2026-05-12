package com.francium.classloader;

import com.francium.graph.ModGraph;
import com.francium.loader.ModManifest;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;

/**
 * 基於 DAG 的並行模組類加載器。
 * 
 * 工作原理:
 * 1. 將 ModGraph 的拓撲層作為並行調度單位
 * 2. 同一層的模組互相獨立，可以並行加載
 * 3. 層與層之間串行保證依賴順序
 * 4. 使用 ForkJoinPool 實現工作竊取並行
 * 
 * 效能: N 個模組，L 層，加速比 = N/L (理想情況)
 */
public class ParallelModClassLoader extends URLClassLoader {
    private final ModGraph modGraph;
    private final Path modsDirectory;
    private final Map<String, ModClassLoader> modLoaders;
    private final Map<String, List<Path>> modPaths;
    private final ExecutorService executor;
    private final ReentrantLock loadLock = new ReentrantLock();
    
    // 加載狀態追蹤
    private final Map<String, LoadStatus> loadStatuses;
    private final Map<String, Long> loadTimes;
    private volatile boolean loaded = false;

    public enum LoadStatus { PENDING, LOADING, LOADED, FAILED, SKIPPED }

    public ParallelModClassLoader(ModGraph modGraph, Path modsDirectory) {
        super(new URL[0], ParallelModClassLoader.class.getClassLoader());
        this.modGraph = modGraph;
        this.modsDirectory = modsDirectory;
        this.modLoaders = new ConcurrentHashMap<>();
        this.modPaths = new ConcurrentHashMap<>();
        this.loadStatuses = new ConcurrentHashMap<>();
        this.loadTimes = new ConcurrentHashMap<>();
        
        // 使用 ForkJoinPool: 工作竊取算法，適合粒度不均的任務
        this.executor = new ForkJoinPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null, true // async mode
        );
    }

    /**
     * 掃描模組目錄，發現所有 JAR 檔案並解析其 manifest。
     */
    public DiscoveryResult discoverMods() throws IOException {
        DiscoveryResult result = new DiscoveryResult();
        File dir = modsDirectory.toFile();
        
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IOException("Mods directory not found: " + modsDirectory);
        }
        
        File[] jarFiles = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jarFiles == null) return result;
        
        for (File jarFile : jarFiles) {
            try (JarFile jar = new JarFile(jarFile)) {
                // 讀取 francium-mod.json (Francium 格式)
                var entry = jar.getJarEntry("francium-mod.json");
                if (entry != null) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ModManifest manifest = ModManifest.fromJson(new String(is.readAllBytes()));
                        modPaths.computeIfAbsent(manifest.modId(), k -> new ArrayList<>())
                                .add(jarFile.toPath());
                        result.found.add(manifest);
                    }
                }
                // 向後兼容: 讀取 fabric.mod.json
                entry = jar.getJarEntry("fabric.mod.json");
                if (entry != null) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ModManifest manifest = ModManifest.fromFabricJson(new String(is.readAllBytes()));
                        modPaths.computeIfAbsent(manifest.modId(), k -> new ArrayList<>())
                                .add(jarFile.toPath());
                        result.found.add(manifest);
                    }
                }
                // 向後兼容: 讀取 META-INF/mods.toml (Forge)
                entry = jar.getJarEntry("META-INF/mods.toml");
                if (entry != null) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ModManifest manifest = ModManifest.fromForgeToml(new String(is.readAllBytes()));
                        modPaths.computeIfAbsent(manifest.modId(), k -> new ArrayList<>())
                                .add(jarFile.toPath());
                        result.found.add(manifest);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 並行加載所有模組。
     * 
     * 加載流程:
     * 1. 構建拓撲層
     * 2. 逐層並行加載
     * 3. 每層內使用 CompletableFuture 並行
     * 4. 等待當前層全部完成後進入下一層
     */
    public LoadReport loadAll() throws Exception {
        if (loaded) throw new IllegalStateException("Already loaded");
        
        modGraph.buildLayers();
        List<Set<String>> layers = modGraph.getLayers();
        
        LoadReport report = new LoadReport();
        long totalStart = System.currentTimeMillis();
        
        System.out.println("Fr: Loading " + modGraph.getModCount() + " mods in " 
            + layers.size() + " layers (estimated speedup: " 
            + String.format("%.1fx", modGraph.getSpeedupRatio()) + ")");
        
        for (int i = 0; i < layers.size(); i++) {
            Set<String> layer = layers.get(i);
            report.layerDetails.add(loadLayer(i, layer));
        }
        
        report.totalLoadTimeMs = System.currentTimeMillis() - totalStart;
        report.sequentialEstimatedMs = modGraph.estimateSequentialLoadTime();
        report.actualSpeedup = (double) report.sequentialEstimatedMs / report.totalLoadTimeMs;
        loaded = true;
        
        return report;
    }

    /**
     * 並行加載一層中的所有模組。
     */
    private LayerLoadDetail loadLayer(int layerIndex, Set<String> layerMods) throws Exception {
        LayerLoadDetail detail = new LayerLoadDetail(layerIndex);
        detail.modCount = layerMods.size();
        
        if (layerMods.isEmpty()) return detail;
        
        long layerStart = System.currentTimeMillis();
        
        // 為每個模組創建加載任務
        List<CompletableFuture<ModLoadResult>> futures = new ArrayList<>();
        
        for (String modId : layerMods) {
            ModManifest manifest = modGraph.getManifest(modId);
            if (manifest == null) {
                // 外部依賴 (如 Minecraft 自身)
                loadStatuses.put(modId, LoadStatus.SKIPPED);
                detail.skipped++;
                continue;
            }
            
            loadStatuses.put(modId, LoadStatus.LOADING);
            
            CompletableFuture<ModLoadResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return loadSingleMod(modId, manifest);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, executor);
            
            futures.add(future);
        }
        
        // 等待該層所有模組加載完成
        CompletableFuture<Void> allInLayer = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allInLayer.get(120, TimeUnit.SECONDS); // 每層最多等 2 分鐘
        } catch (TimeoutException e) {
            allInLayer.cancel(true);
            throw new RuntimeException("Layer " + layerIndex + " load timeout");
        }
        
        // 收集結果
        for (int j = 0; j < futures.size(); j++) {
            try {
                ModLoadResult result = futures.get(j).get();
                detail.results.add(result);
                if (result != null) {
                    loadStatuses.put(result.modId, LoadStatus.LOADED);
                    loadTimes.put(result.modId, result.loadTimeMs);
                }
                detail.success++;
            } catch (ExecutionException e) {
                // 某個模組加載失敗 — 從 layerMods 提取 modId
                Throwable cause = e.getCause();
                String failedModId = layerMods.size() > j 
                    ? layerMods.toArray(new String[0])[j] : "unknown";
                loadStatuses.put(failedModId, LoadStatus.FAILED);
                detail.failures.add(new LoadFailure(failedModId, cause));
                detail.failed++;
                System.err.println("  ⚠ Mod load failed: " + failedModId + " - " + cause.getMessage());
            }
        }
        
        detail.layerTimeMs = System.currentTimeMillis() - layerStart;
        System.out.printf("  Layer %d: %d mods loaded in %dms (%d success, %d failed)\n",
            layerIndex, detail.modCount, detail.layerTimeMs, detail.success, detail.failed);
        
        return detail;
    }

    /**
     * 加載單個模組。
     */
    private ModLoadResult loadSingleMod(String modId, ModManifest manifest) throws Exception {
        long start = System.nanoTime();
        
        // 為每個模組創建獨立的 ClassLoader，實現隔離
        List<Path> paths = modPaths.get(modId);
        if (paths == null || paths.isEmpty()) {
            throw new IOException("No JAR found for mod: " + modId);
        }
        
        URL[] urls = paths.stream()
            .map(p -> { try { return p.toUri().toURL(); } 
                        catch (MalformedURLException e) { 
                            throw new RuntimeException("Invalid JAR path: " + p + " - " + e.getMessage(), e); 
                        }})
            .toArray(URL[]::new);
        
        ModClassLoader modLoader = new ModClassLoader(urls, this);
        modLoaders.put(modId, modLoader);
        
        // 加載主類
        Class<?> mainClass = modLoader.loadClass(manifest.mainClass());
        
        long loadTimeMs = (System.nanoTime() - start) / 1_000_000;
        
        return new ModLoadResult(modId, manifest.version(), mainClass, loadTimeMs);
    }

    /**
     * 獲取已加載模組的主類。
     */
    public Class<?> getModClass(String modId) {
        ModClassLoader loader = modLoaders.get(modId);
        if (loader == null) return null;
        
        ModManifest manifest = modGraph.getManifest(modId);
        if (manifest == null) return null;
        
        try {
            return loader.loadClass(manifest.mainClass());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public LoadStatus getStatus(String modId) {
        return loadStatuses.getOrDefault(modId, LoadStatus.PENDING);
    }

    public Map<String, Long> getLoadTimes() {
        return Collections.unmodifiableMap(loadTimes);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }

    /**
     * 每個模組獨立的 ClassLoader，支援隔離和卸載。
     */
    public static class ModClassLoader extends URLClassLoader {
        public ModClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
    }

    // --- 數據類 ---
    public static class DiscoveryResult {
        public List<ModManifest> found = new ArrayList<>();
        public List<String> skipped = new ArrayList<>();
        public int totalJars = 0;
    }

    public static class LoadReport {
        public long totalLoadTimeMs;
        public long sequentialEstimatedMs;
        public double actualSpeedup;
        public List<LayerLoadDetail> layerDetails = new ArrayList<>();
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Francium Mod Loader Report ===\n");
            sb.append(String.format("Total load time: %dms\n", totalLoadTimeMs));
            sb.append(String.format("Sequential estimate: %dms\n", sequentialEstimatedMs));
            sb.append(String.format("Actual speedup: %.1fx\n", actualSpeedup));
            sb.append(String.format("Layers: %d\n", layerDetails.size()));
            for (LayerLoadDetail layer : layerDetails) {
                sb.append(String.format("  Layer %d: %d mods, %dms (%d ok, %d fail)\n",
                    layer.layerIndex, layer.modCount, layer.layerTimeMs,
                    layer.success, layer.failed));
            }
            return sb.toString();
        }
    }

    public static class LayerLoadDetail {
        public int layerIndex;
        public int modCount;
        public long layerTimeMs;
        public int success = 0;
        public int failed = 0;
        public int skipped = 0;
        public List<ModLoadResult> results = new ArrayList<>();
        public List<LoadFailure> failures = new ArrayList<>();
        
        public LayerLoadDetail(int index) { this.layerIndex = index; }
    }

    public record ModLoadResult(String modId, String version, Class<?> mainClass, long loadTimeMs) {}

    public record LoadFailure(String modId, Throwable error) {}
}
