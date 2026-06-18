package com.francium.classloader;

import com.francium.api.PublicApi;
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
@PublicApi
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

    // 預設 timeout（毫秒），可由 setter 覆蓋
    private volatile long layerTimeoutMs = 120_000;

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

    /** 設定每層加載超時時間（毫秒） */
    public void setLayerTimeoutMs(long timeoutMs) {
        this.layerTimeoutMs = timeoutMs > 0 ? timeoutMs : 120_000;
    }

    /**
     * 將外部 JAR 加入父 ClassLoader，
     * 使所有子 ModClassLoader 都能透過委派找到其類別。
     */
    public void addExternalJar(Path jarPath) throws MalformedURLException {
        URL url = jarPath.toUri().toURL();
        super.addURL(url);
        LOGGER.info("Added external JAR: {} -> {}", jarPath.getFileName(), url);
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
                        ModManifest manifest = ModManifest.fromJson(new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
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
                        ModManifest manifest = ModManifest.fromFabricJson(new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
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
                        ModManifest manifest = ModManifest.fromForgeToml(new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
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
        loadLock.lock();
        try {
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
            report.actualSpeedup = report.sequentialEstimatedMs > 0
                ? (double) report.sequentialEstimatedMs / report.totalLoadTimeMs
                : 1.0;
            loaded = true;
            
            return report;
        } finally {
            loadLock.unlock();
        }
    }

    /**
     * 並行加載一層中的所有模組。
     */
    private LayerLoadDetail loadLayer(int layerIndex, Set<String> layerMods) throws Exception {
        LayerLoadDetail detail = new LayerLoadDetail(layerIndex);
        detail.modCount = layerMods.size();
        
        if (layerMods.isEmpty()) return detail;
        
        long layerStart = System.currentTimeMillis();
        
        // 使用有序列表保持 modId 順序與結果對應，避免 Set 順序不確定性
        List<String> orderedModIds = new ArrayList<>(layerMods);
        
        // 為每個模組創建加載任務
        List<CompletableFuture<ModLoadResult>> futures = new ArrayList<>();
        // 同時保留 modId 到 Future 的映射，以便失敗時精準定位
        Map<String, CompletableFuture<ModLoadResult>> modFutureMap = new ConcurrentHashMap<>();
        
        for (String modId : orderedModIds) {
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
            modFutureMap.put(modId, future);
        }
        
        // 等待該層所有模組加載完成
        CompletableFuture<Void> allInLayer = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allInLayer.get(layerTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            allInLayer.cancel(true);
            throw new RuntimeException("Layer " + layerIndex + " load timeout after " + layerTimeoutMs + "ms");
        } catch (ExecutionException e) {
            // ★ BUG FIX: 單個或多個模組加載失敗時，allOf 會立即完成異常。
            //   不應因此拋出，應繼續收集各模組的個別結果，
            //   讓下方的結果迴圈正確記錄成功/失敗。
            LOGGER.warn("Layer {}: some mods failed during parallel load, collecting results...", layerIndex);
        }
        
        // 收集結果：遍歷 orderedModIds 而非 futures，確保順序正確
        for (String modId : orderedModIds) {
            CompletableFuture<ModLoadResult> future = modFutureMap.get(modId);
            if (future == null) {
                // 被跳過的 mod（manifest == null）
                continue;
            }
            try {
                ModLoadResult result = future.get();
                if (result != null) {
                    detail.results.add(result);
                    loadStatuses.put(result.modId(), LoadStatus.LOADED);
                    loadTimes.put(result.modId(), result.loadTimeMs());
                }
                detail.success++;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                loadStatuses.put(modId, LoadStatus.FAILED);
                detail.failures.add(new LoadFailure(modId, cause));
                detail.failed++;
                LOGGER.error("  \u26a0 Mod load failed: {} - {}", modId, cause.getMessage());
            } catch (Exception e) {
                loadStatuses.put(modId, LoadStatus.FAILED);
                detail.failures.add(new LoadFailure(modId, e));
                detail.failed++;
                LOGGER.error("  \u26a0 Mod load failed: {} - {}", modId, e.getMessage());
            }
        }
        
        detail.layerTimeMs = System.currentTimeMillis() - layerStart;
        LOGGER.info("  Layer {}: {} mods loaded in {}ms ({} success, {} failed)",
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
        
        // 加載主類（庫型 mod 可能沒有 mainClass）
        Class<?> mainClass = null;
        String mainClassName = manifest.mainClass();
        if (mainClassName != null && !mainClassName.isBlank()) {
            mainClass = modLoader.loadClass(mainClassName);
        }
        
        long loadTimeMs = (System.nanoTime() - start) / 1_000_000;
        
        return new ModLoadResult(modId, manifest.version(), mainClass, loadTimeMs);
    }

    /**
     * 獲取已加載模組的主類。
     */
    public Class<?> getModClass(String modId) {
        if (modId == null) return null;
        ModClassLoader loader = modLoaders.get(modId);
        if (loader == null) return null;
        
        ModManifest manifest = modGraph.getManifest(modId);
        if (manifest == null) return null;
        
        String mainClassName = manifest.mainClass();
        if (mainClassName == null || mainClassName.isBlank()) return null;
        
        try {
            return loader.loadClass(mainClassName);
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

    /** 優雅關閉 ForkJoinPool，等待進行中的任務完成，並釋放所有 JAR 檔案句柄。 */
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        // 關閉每個 mod 的獨立 ClassLoader，釋放 JAR 檔案句柄
        for (ModClassLoader child : modLoaders.values()) {
            try { child.close(); } catch (IOException ignored) {}
        }
        // 釋放父 URLClassLoader 持有的 JAR 檔案句柄
        try {
            close();
        } catch (IOException ignored) {}
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
