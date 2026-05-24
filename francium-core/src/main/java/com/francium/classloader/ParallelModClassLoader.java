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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelModClassLoader.class);

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
        result.totalJars = jarFiles.length;

        for (File jarFile : jarFiles) {
            // Skip empty/corrupt JARs gracefully instead of crashing the whole loader
            if (jarFile.length() == 0) {
                LOGGER.warn("  Skipping empty JAR: {}", jarFile.getName());
                result.skipped.add(jarFile.getName());
                continue;
            }
            try (JarFile jar = new JarFile(jarFile)) {
                // Priority order: francium-mod.json > fabric.mod.json > mods.toml
                // Only the FIRST found format is used per JAR to avoid duplicate registration.
                boolean registered = false;

                // 1. Francium native format
                var entry = jar.getJarEntry("francium-mod.json");
                if (entry != null && !registered) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ModManifest manifest = ModManifest.fromJson(new String(is.readAllBytes()));
                        if (manifest != null) {
                            manifest.jarSourcePath = jarFile.toPath();
                            modPaths.computeIfAbsent(manifest.modId(), k -> new ArrayList<>())
                                    .add(jarFile.toPath());
                            result.found.add(manifest);
                            registered = true;
                        }
                    }
                }
                // 2. Backward compat: Fabric format
                entry = jar.getJarEntry("fabric.mod.json");
                if (entry != null && !registered) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ModManifest manifest = ModManifest.fromFabricJson(new String(is.readAllBytes()));
                        if (manifest != null) {
                            manifest.jarSourcePath = jarFile.toPath();
                            modPaths.computeIfAbsent(manifest.modId(), k -> new ArrayList<>())
                                    .add(jarFile.toPath());
                            result.found.add(manifest);
                            registered = true;
                        }
                    }
                }
                // 3. Backward compat: Forge/NeoForge format
                entry = jar.getJarEntry("META-INF/mods.toml");
                if (entry != null && !registered) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ModManifest manifest = ModManifest.fromForgeToml(new String(is.readAllBytes()));
                        if (manifest != null) {
                            manifest.jarSourcePath = jarFile.toPath();
                            modPaths.computeIfAbsent(manifest.modId(), k -> new ArrayList<>())
                                    .add(jarFile.toPath());
                            result.found.add(manifest);
                            registered = true;
                        }
                    }
                }

                if (!registered) {
                    LOGGER.debug("  No recognized mod manifest in: {}", jarFile.getName());
                    result.skipped.add(jarFile.getName());
                }
            } catch (java.util.zip.ZipException e) {
                LOGGER.warn("  Corrupt or invalid JAR, skipping: {} - {}", jarFile.getName(), e.getMessage());
                result.skipped.add(jarFile.getName());
            } catch (IOException e) {
                LOGGER.warn("  IO error reading JAR, skipping: {} - {}", jarFile.getName(), e.getMessage());
                result.skipped.add(jarFile.getName());
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
        
        LOGGER.info("Fr: Loading " + modGraph.getModCount() + " mods in " 
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
                LOGGER.error("  ⚠ Mod load failed: " + failedModId + " - " + cause.getMessage());
            }
        }
        
        detail.layerTimeMs = System.currentTimeMillis() - layerStart;
        LOGGER.info(String.format("  Layer %d: %d mods loaded in %dms (%d success, %d failed)\n",
            layerIndex, detail.modCount, detail.layerTimeMs, detail.success, detail.failed));
        
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

    /** 返回指定 mod 的加載狀態。 */
    public LoadStatus getStatus(String modId) {
        return loadStatuses.getOrDefault(modId, LoadStatus.PENDING);
    }

    /** 返回所有 mod 的加載耗時（ms），唯讀。 */
    public Map<String, Long> getLoadTimes() {
        return Collections.unmodifiableMap(loadTimes);
    }

    /** 優雅關閉 ForkJoinPool，等待進行中的任務完成。 */
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
    /** 模組掃描結果，包含發現的 mod 和被跳過的檔案。 */
    public static class DiscoveryResult {
        /** 成功解析 manifest 的 mod 列表 */
        public List<ModManifest> found = new ArrayList<>();
        /** 被跳過的檔案名稱列表（格式不識別或損毀） */
        public List<String> skipped = new ArrayList<>();
        /** mods 目錄中的 JAR 總數 */
        public int totalJars = 0;
    }

    /** 完整加載報告，包含各層細節與效能統計。 */
    public static class LoadReport {
        /** 所有層的實際總加載耗時（ms） */
        public long totalLoadTimeMs;
        /** 循序加載的預估耗時（ms），用於對比加速比 */
        public long sequentialEstimatedMs;
        /** 實際加速比 = sequentialEstimatedMs / totalLoadTimeMs */
        public double actualSpeedup;
        /** 各層的加載細節 */
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

    /** 單一拓撲層的加載結果。 */
    public static class LayerLoadDetail {
        /** 層索引（0 為最底層） */
        public int layerIndex;
        /** 該層中的 mod 總數 */
        public int modCount;
        /** 該層的實際加載耗時（ms） */
        public long layerTimeMs;
        /** 成功加載的 mod 數 */
        public int success = 0;
        /** 加載失敗的 mod 數 */
        public int failed = 0;
        /** 被跳過的 mod 數（外部依賴） */
        public int skipped = 0;
        /** 成功加載的 mod 結果列表 */
        public List<ModLoadResult> results = new ArrayList<>();
        /** 加載失敗的記錄列表 */
        public List<LoadFailure> failures = new ArrayList<>();
        
        public LayerLoadDetail(int index) { this.layerIndex = index; }
    }

    /**
     * 單個 mod 的加載結果。
     * @param modId 模組識別碼
     * @param version 解析後的版本號
     * @param mainClass 加載後的主類別
     * @param loadTimeMs 加載耗時（ms）
     */
    public record ModLoadResult(String modId, String version, Class<?> mainClass, long loadTimeMs) {}

    /**
     * 單個 mod 的加載失敗記錄。
     * @param modId 模組識別碼
     * @param error 導致失敗的異常
     */
    public record LoadFailure(String modId, Throwable error) {}
}
