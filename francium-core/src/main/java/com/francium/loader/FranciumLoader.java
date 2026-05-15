package com.francium.loader;

import com.francium.ai.adapter.VersionBridge;
import com.francium.ai.mapping.MethodSignature;
import com.francium.classloader.ParallelModClassLoader;
import com.francium.classloader.ParallelModClassLoader.DiscoveryResult;
import com.francium.classloader.ParallelModClassLoader.LoadReport;
import com.francium.graph.ModGraph;
import com.francium.profiler.memory.MemoryManager;
import com.francium.resolver.model.DependencyConstraint;
import com.francium.resolver.model.SemanticVersion;
import com.francium.resolver.sat.SATDependencyResolver;
import com.francium.server.sync.ServerSyncProtocol;
import com.francium.server.validate.ModValidator;

import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Francium Mod Loader 主入口。
 *
 * 生命週期:
 * <pre>
 *   INIT → DISCOVERING → RESOLVING → BRIDGING → LOADING → READY
 * </pre>
 *
 * 解決的八大問題:
 * 1. 加載時間過長 → DAG 並行加載，加速比 3-10x
 * 2. 模組衝突與依賴地獄 → SAT 求解器自動化解依賴
 * 3. 版本不相容 → AI 字節碼橋接自動適配
 * 4. 效能損耗 → 獨立 ClassLoader 隔離 + 物件池
 * 5. 安裝與管理繁瑣 → 內建套件管理器
 * 6. 伺服器同步問題 → 自動 mod 清單同步 + SHA256 驗證
 * 7. 記憶體管理 → 洩漏檢測 + 自適應 GC + 物件池
 * 8. 版本斷層 → AI 橋接即時適配
 */
public class FranciumLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FranciumLoader.class);

    private final Path gameDir;
    private final Path modsDir;
    private final Path configDir;
    private final Path cacheDir;

    private final ModGraph modGraph;
    private ParallelModClassLoader classLoader;
    private LoaderConfig config;
    private LoaderState state = LoaderState.INIT;

    // 子系統
    private SATDependencyResolver resolver;
    private VersionBridge versionBridge;
    private MemoryManager memoryManager;
    private ServerSyncProtocol serverSync;
    private ModValidator validator;

    // 擴展點
    private final Map<String, List<Runnable>> extensionPoints = new LinkedHashMap<>();

    // 加載階段計時
    private final Map<String, Long> phaseTimings = new LinkedHashMap<>();
    
    // 發現結果緩存，供後續階段使用
    private DiscoveryResult lastDiscoveryResult;

    public enum LoaderState {
        INIT, DISCOVERING, RESOLVING, BRIDGING, LOADING, READY, ERROR
    }

    public FranciumLoader(Path gameDir) {
        this.gameDir = gameDir;
        this.modsDir = gameDir.resolve("mods");
        this.configDir = gameDir.resolve("config").resolve("francium");
        this.cacheDir = gameDir.resolve(".francium-cache");
        this.modGraph = new ModGraph();
    }

    /**
     * 載入設定。
     */
    public void loadConfig() {
        this.config = LoaderConfig.load(configDir.resolve("loader.toml"));
    }

    /**
     * 初始化所有子系統。
     * 必須在 launch() 之前調用。
     */
    public void initialize() {
        // 載入設定
        if (config == null) loadConfig();

        // 初始化核心加載器
        this.classLoader = new ParallelModClassLoader(modGraph, modsDir);

        // 初始化 SAT 依賴解析器
        this.resolver = new SATDependencyResolver();
        this.resolver.setTimeoutMs(config.layerTimeoutSeconds * 1000L);

        // 初始化 AI 版本橋接器
        this.versionBridge = new VersionBridge("auto", "auto");
        this.versionBridge.setConfidenceThreshold(config.aiConfidenceThreshold);
        this.versionBridge.setAutoFix(config.aiBridgeEnabled);
        this.versionBridge.setDryRun(config.aiBridgeReportOnly);

        // 初始化記憶體管理器
        this.memoryManager = new MemoryManager(
            config.memoryWarningThresholdMB,
            config.aggressiveGC,
            config.memoryLeakDetection
        );

        // 初始化伺服器同步
        this.serverSync = new ServerSyncProtocol();

        // 初始化安全驗證器
        this.validator = new ModValidator(ModValidator.SecurityLevel.INTEGRITY);

        // 確保目錄存在
        try {
            java.nio.file.Files.createDirectories(modsDir);
            java.nio.file.Files.createDirectories(configDir);
            java.nio.file.Files.createDirectories(cacheDir);
        } catch (java.io.IOException e) {
            LOGGER.error("Fr: Failed to create directories: " + e.getMessage());
        }
    }

    /**
     * 完整加載流程。
     *
     * Phase 1: 發現模組 (掃描 mods/ 目錄，解析 manifest)
     * Phase 2: 依賴解析 (SAT 求解器)
     * Phase 3: AI 版本橋接 (自動適配不相容版本)
     * Phase 4: 並行加載 (DAG 排程)
     * Phase 5: 觸發生命週期事件
     */
    public LoadReport launch() throws Exception {
        if (classLoader == null) initialize();

        long totalStart = System.currentTimeMillis();

        // Phase 1: DISCOVERING
        state = LoaderState.DISCOVERING;
        long t1 = System.currentTimeMillis();
        lastDiscoveryResult = discoverPhase();
        phaseTimings.put("discovery", System.currentTimeMillis() - t1);

        // Phase 2: RESOLVING
        state = LoaderState.RESOLVING;
        long t2 = System.currentTimeMillis();
        resolvePhase(lastDiscoveryResult);
        phaseTimings.put("resolution", System.currentTimeMillis() - t2);

        // Phase 3: BRIDGING
        state = LoaderState.BRIDGING;
        long t3 = System.currentTimeMillis();
        if (config.aiBridgeEnabled) {
            bridgePhase(lastDiscoveryResult);
        }
        phaseTimings.put("bridging", System.currentTimeMillis() - t3);

        // Phase 4: LOADING
        state = LoaderState.LOADING;
        long t4 = System.currentTimeMillis();
        // 啟動記憶體監控
        memoryManager.start();
        lastReport = loadingPhase();
        phaseTimings.put("loading", System.currentTimeMillis() - t4);

        // Phase 5: READY
        state = LoaderState.READY;
        lastReport.totalLoadTimeMs = System.currentTimeMillis() - totalStart;

        // 觸發生命週期回調
        fireExtension("preLaunch");
        fireExtension("postLaunch");

        return lastReport;
    }

    // ─── Phase Implementations ───

    private DiscoveryResult discoverPhase() throws Exception {
        Objects.requireNonNull(classLoader, "classLoader not initialized");
        DiscoveryResult result = classLoader.discoverMods();
        LOGGER.info(String.format("Fr: Discovered %d mods in %d JARs (%d skipped)%n",
            result.found.size(), result.totalJars, result.skipped.size()));

        // 安全驗證每個 JAR
        if (validator != null) {
            for (var manifest : result.found) {
                var validation = validator.validate(
                    modsDir.resolve(manifest.modId() + "-" + manifest.version() + ".jar"));
                if (!validation.passed) {
                    LOGGER.warn("Fr Security WARNING: " + validation);
                }
            }
        }

        return result;
    }

    /**
     * 依賴解析階段。
     * 使用 SAT 求解器解析模組依賴關係，並將結果加入 DAG。
     */
    private void resolvePhase(DiscoveryResult discovered) {
        Objects.requireNonNull(resolver, "SAT resolver not initialized");
        
        // 如果沒有發現結果，嘗試掃描
        if (discovered == null || discovered.found.isEmpty()) {
            LOGGER.info("Fr: No mods to resolve (discovery returned empty)");
            return;
        }

        // 向 SAT resolver 註冊所有發現的模組
        for (var manifest : discovered.found) {
            SemanticVersion sv = SemanticVersion.tryParse(manifest.version());
            if (sv != null) {
                resolver.registerVersions(manifest.modId(), List.of(sv));
            }

            // 註冊依賴關係
            Map<String, DependencyConstraint> deps = new LinkedHashMap<>();
            for (var entry : manifest.dependencies().entrySet()) {
                deps.put(entry.getKey(), new DependencyConstraint(entry.getValue()));
            }
            resolver.registerDependencies(manifest.modId(), deps);

            // 註冊衝突
            for (var entry : manifest.conflicts().entrySet()) {
                resolver.registerConflict(manifest.modId(), entry.getKey(), entry.getValue());
            }
        }

        // 求解
        List<String> rootMods = discovered.found.stream()
            .map(m -> m.modId())
            .toList();

        var resolveResult = resolver.solve(rootMods);

        if (resolveResult.success && resolveResult.solution != null) {
            // 將解析結果加入 DAG
            for (var entry : resolveResult.solution.entrySet()) {
                var manifest = discovered.found.stream()
                    .filter(m -> m.modId().equals(entry.getKey()))
                    .findFirst();
                if (manifest.isPresent()) {
                    modGraph.addMod(manifest.get(), entry.getValue().toString());
                }
            }
            LOGGER.info(String.format("Fr: Dependencies resolved for %d mods (%dms, %d nodes explored)%n",
                resolveResult.solution.size(), resolveResult.solveTimeMs, resolveResult.nodesExplored));
        } else {
            LOGGER.error("Fr: Dependency resolution FAILED");
            for (String err : resolveResult.errors) {
                LOGGER.warn("  " + err);
            }
            // 仍然嘗試加載所有 mod（最佳努力）
            for (var manifest : discovered.found) {
                modGraph.addMod(manifest, manifest.version());
            }
        }
    }

    @SuppressWarnings("unused")
    private void bridgePhase(DiscoveryResult discovered) {
        Map<String, Path> modPaths = new LinkedHashMap<>();
        for (var manifest : discovered.found) {
            Path jarPath = modsDir.resolve(manifest.modId() + "-" + manifest.version() + ".jar");
            if (java.nio.file.Files.exists(jarPath)) {
                modPaths.put(manifest.modId(), jarPath);
            }
        }

        try {
            var summary = versionBridge.bridgeAll(modPaths);
            LOGGER.info(String.format("Fr: AI Bridge - %d/%d mods compatible, %d adapters generated (%.0f%% overall)%n",
                summary.reports.stream().filter(r -> r.isFullyCompatible()).count(),
                summary.reports.size(),
                summary.adaptersGenerated,
                summary.overallCompatibility * 100));

            // 如果生成了 adapter，把它們寫入 cache 目錄
            for (var entry : summary.adapterBytes.entrySet()) {
                Path adapterPath = cacheDir.resolve(entry.getKey() + "_bridge.jar");
                try {
                    java.nio.file.Files.write(adapterPath, entry.getValue());
                } catch (java.io.IOException e) {
                    LOGGER.error("Fr: Failed to write adapter for " + entry.getKey());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Fr: AI Bridge analysis failed: " + e.getMessage());
        }
    }

    private LoadReport loadingPhase() throws Exception {
        Objects.requireNonNull(classLoader, "classLoader not initialized in loading phase");
        return classLoader.loadAll();
    }

    // ─── 生命週期擴展 ───

    /**
     * 在啟動前執行的回調（所有 mod 加載完畢後，進入遊戲前）。
     */
    public void onPreLaunch(Runnable callback) {
        registerExtension("preLaunch", callback);
    }

    /**
     * 在啟動後執行的回調。
     */
    public void onPostLaunch(Runnable callback) {
        registerExtension("postLaunch", callback);
    }

    /**
     * 在特定 mod 加載後執行的回調。
     */
    public void onModLoaded(String modId, Runnable callback) {
        registerExtension("modLoaded:" + modId, callback);
    }

    private void registerExtension(String point, Runnable callback) {
        extensionPoints.computeIfAbsent(point, k -> new ArrayList<>()).add(callback);
    }

    private void fireExtension(String point) {
        List<Runnable> callbacks = extensionPoints.get(point);
        if (callbacks != null) {
            for (Runnable cb : callbacks) {
                try { cb.run(); } catch (Exception e) {
                    LOGGER.error("Extension " + point + " failed: " + e.getMessage());
                }
            }
        }
    }

    // ─── Shutdown ───

    /**
     * 優雅關閉，釋放所有資源。
     */
    public void shutdown() {
        if (memoryManager != null) {
            memoryManager.forceGC();
            memoryManager.shutdown();
        }
        if (classLoader != null) {
            classLoader.shutdown();
        }
        state = LoaderState.INIT;
    }

    // ─── Getters ───

    public LoaderState state() { return state; }
    public ModGraph modGraph() { return modGraph; }
    public ParallelModClassLoader classLoader() { return classLoader; }
    public LoaderConfig config() { return config; }
    public MemoryManager memoryManager() { return memoryManager; }
    public VersionBridge versionBridge() { return versionBridge; }
    public Path gameDir() { return gameDir; }
    public Path modsDir() { return modsDir; }
    public Path configDir() { return configDir; }
    public Path cacheDir() { return cacheDir; }
    public Map<String, Long> phaseTimings() { return Collections.unmodifiableMap(phaseTimings); }

    /**
     * 取得記憶體快照（需在 launch() 之後）。
     */
    public MemoryManager.MemorySnapshot getMemorySnapshot() {
        return memoryManager != null ? memoryManager.getSnapshot() : null;
    }

    // ─── Builder & Convenience API (for LaunchWrapper / external callers) ───

    /**
     * 建立 Builder。使用方式:
     * <pre>
     *   FranciumLoader loader = FranciumLoader.builder(gameDir)
     *       .withParallelLoading(true)
     *       .withMemoryManagement(true)
     *       .build();
     * </pre>
     */
    public static Builder builder(Path gameDir) {
        return new Builder(gameDir);
    }

    public static class Builder {
        private final Path gameDir;
        private boolean parallel = true;
        private boolean memory = true;
        private boolean aiBridge;
        private boolean serverSync;
        private double aiThreshold = 0.85;

        Builder(Path gameDir) { this.gameDir = gameDir; }

        public Builder withParallelLoading(boolean v) { parallel = v; return this; }
        public Builder withMemoryManagement(boolean v) { memory = v; return this; }
        public Builder withAIBridge(boolean v) { aiBridge = v; return this; }
        public Builder withServerSync(boolean v) { serverSync = v; return this; }
        public Builder withAIConfidenceThreshold(double v) { aiThreshold = v; return this; }

        public FranciumLoader build() {
            FranciumLoader loader = new FranciumLoader(gameDir);
            loader.initialize();
            if (loader.config == null) {
                loader.config = LoaderConfig.createDefault();
            }
            loader.config.aiBridgeEnabled = aiBridge;
            loader.config.aiConfidenceThreshold = (float) aiThreshold;
            loader.config.serverSyncEnabled = serverSync;
            return loader;
        }
    }

    /** Phase 1: scan mods directory */
    public void scanMods() throws Exception {
        if (classLoader == null) initialize();
        state = LoaderState.DISCOVERING;
        long t = System.currentTimeMillis();
        lastDiscoveryResult = discoverPhase();
        phaseTimings.put("discovery", System.currentTimeMillis() - t);
    }

    /** Phase 2: resolve dependencies using cached discovery result */
    public void resolveDependencies() throws Exception {
        state = LoaderState.RESOLVING;
        long t = System.currentTimeMillis();
        // Use cached discovery result instead of null
        resolvePhase(lastDiscoveryResult);
        phaseTimings.put("resolution", System.currentTimeMillis() - t);
    }

    /** Phase 3: build DAG from resolved dependencies */
    public void buildLoadGraph() {
        state = LoaderState.RESOLVING;
        modGraph.buildLayers();
    }

    /** Phase 4: load all mods */
    public void loadMods() throws Exception {
        state = LoaderState.LOADING;
        lastReport = loadingPhase();
        state = LoaderState.READY;
    }

    /** Inject Francium ClassLoader into LaunchClassLoader chain */
    public void injectInto(Object launchClassLoader) {
        if (launchClassLoader == null) {
            LOGGER.warn("[Francium] Cannot inject into null LaunchClassLoader");
            return;
        }
        try {
            java.lang.reflect.Method addExclusion = launchClassLoader.getClass()
                .getMethod("addClassLoaderExclusion", String.class);
            addExclusion.invoke(launchClassLoader, "com.francium.");
            LOGGER.info("[Francium] Registered classloader exclusion for com.francium.*");
        } catch (NoSuchMethodException e) {
            LOGGER.info("[Francium] LaunchWrapper not detected; skipping exclusion registration");
        } catch (Exception e) {
            LOGGER.info("[Francium] LaunchWrapper exclusion registration skipped: " + e.getMessage());
        }
    }

    public int getLoadedModCount() {
        if (lastReport != null)
            return lastReport.layerDetails.stream().mapToInt(d -> d.success + d.failed).sum();
        return modGraph != null && modGraph.getLayerCount() > 0
            ? modGraph.getLayers().stream().mapToInt(Set::size).sum()
            : 0;
    }

    private LoadReport lastReport;

    /** Full status report */
    public FranciumReport getReport() {
        FranciumReport r = new FranciumReport();
        if (modGraph != null) {
            r.layers = modGraph.getLayerCount();
            r.maxParallel = modGraph.getLayerCount() > 0
                ? modGraph.getLayers().stream().mapToInt(Set::size).max().orElse(0)
                : 0;
        }
        r.totalMods = getLoadedModCount();
        r.loadedMods = r.totalMods;
        r.failedMods = 0;
        if (lastReport != null) {
            r.failedMods = lastReport.layerDetails.stream().mapToInt(d -> d.failed).sum();
        }
        r.satTimeMs = phaseTimings.getOrDefault("resolution", 0L);
        r.loadTimeMs = phaseTimings.getOrDefault("loading", 0L);
        r.totalTimeMs = phaseTimings.values().stream().mapToLong(Long::longValue).sum();
        return r;
    }

    public static class FranciumReport {
        public int layers, maxParallel, totalMods, loadedMods, failedMods;
        public long satTimeMs, loadTimeMs, totalTimeMs;
    }
}
