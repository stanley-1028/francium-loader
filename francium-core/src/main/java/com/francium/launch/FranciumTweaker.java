package com.francium.launch;

import com.francium.loader.FranciumLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Francium Mod Loader — Minecraft LaunchWrapper 入口。
 * 
 * 實作 net.minecraft.launchwrapper.ITweaker 介面。
 * 需要在建構時引入 LaunchWrapper 作為 compileOnly 依賴。
 * 
 * 使用方式:
 *   --tweakClass com.francium.launch.FranciumTweaker
 */
public class FranciumTweaker /* implements ITweaker */ {

    private static final Logger LOGGER = LoggerFactory.getLogger(FranciumTweaker.class);

    private FranciumLoader loader;
    private List<String> args = new ArrayList<>();


    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        // 儲存啟動參數
        this.args = new ArrayList<>(args);
        
        LOGGER.info("[Francium] Accepting launch options:");
        LOGGER.info("[Francium]   Game dir: " + gameDir);
        LOGGER.info("[Francium]   Profile:  " + profile);
        LOGGER.info("[Francium]   Args:     " + args.size() + " arguments");
    }


    public void injectIntoClassLoader(Object classLoader) {
        LOGGER.info("[Francium] Injecting into LaunchClassLoader...");

        try {
            // 初始化 FranciumLoader
            // gameDir 由 acceptOptions 傳入
            File gameDir = new File("."); // fallback
            for (String arg : args) {
                if (arg.startsWith("--gameDir") && arg.contains(" ")) {
                    gameDir = new File(arg.substring(arg.indexOf(" ") + 1));
                }
            }
            
            loader = FranciumLoader.builder(gameDir.toPath())
                .withParallelLoading(true)
                .withMemoryManagement(true)
                .withAIBridge(false)      // AI 橋接預設關閉，可設定開啟
                .withServerSync(false)    // 單機預設關閉
                .build();

            // === 階段 1: 掃描模組 ===
            LOGGER.info("[Francium] Phase 1: Scanning mods...");
            loader.scanMods();

            // === 階段 2: SAT 依賴解析 ===
            LOGGER.info("[Francium] Phase 2: Resolving dependencies...");
            loader.resolveDependencies();

            // === 階段 3: DAG 拓撲排序 ===
            LOGGER.info("[Francium] Phase 3: Building load graph...");
            loader.buildLoadGraph();

            // === 階段 4: 並行載入 ===
            LOGGER.info("[Francium] Phase 4: Parallel loading...");
            loader.loadMods();

            // === 階段 5: 注入 ClassLoader ===
            // 將 Francium 的 ClassLoader 插入 Launch 類載入鏈
            // 這樣 Minecraft 載入類時會先經過 Francium 的隔離 ClassLoader
            loader.injectInto(classLoader);
            
            LOGGER.info("[Francium] Injection complete. " 
                + loader.getLoadedModCount() + " mods loaded.");

            // === 狀態報告 ===
            FranciumLoader.FranciumReport report = loader.getReport();
            LOGGER.info("[Francium] ──────────────────────────");
            LOGGER.info("[Francium]  Load Report:");
            LOGGER.info("[Francium]    Total mods:    " + report.totalMods);
            LOGGER.info("[Francium]    Loaded:        " + report.loadedMods);
            LOGGER.info("[Francium]    Failed:        " + report.failedMods);
            LOGGER.info("[Francium]    Layers:        " + report.layers);
            LOGGER.info("[Francium]    Max parallel:  " + report.maxParallel);
            LOGGER.info("[Francium]    SAT time:      " + report.satTimeMs + "ms");
            LOGGER.info("[Francium]    Load time:     " + report.loadTimeMs + "ms");
            LOGGER.info("[Francium]    Total time:    " + report.totalTimeMs + "ms");
            LOGGER.info("[Francium] ──────────────────────────");

        } catch (Exception e) {
            LOGGER.error("[Francium] FATAL: Failed to initialize loader!");
            LOGGER.error("Exception", e);
            // 不中斷啟動 — Minecraft 仍可運行，只是沒有 Francium 模組
        }
    }


    public String getLaunchTarget() {
        // 返回 Minecraft 主類 — 不修改原始啟動目標
        return "net.minecraft.client.main.Main";
    }


    public String[] getLaunchArguments() {
        // 傳回原始參數，讓 Minecraft 正常啟動
        return args.toArray(new String[0]);
    }
}
