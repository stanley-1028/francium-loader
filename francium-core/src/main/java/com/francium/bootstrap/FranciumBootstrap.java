package com.francium.bootstrap;

import com.francium.api.PublicApi;
import com.francium.loader.FranciumLoader;
import com.francium.loader.FranciumLoader.LoaderState;
import com.francium.classloader.ParallelModClassLoader.LoadReport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Francium Mod Loader 啟動入口。
 *
 * <p>用法:
 * <pre>
 *   java -jar francium-loader.jar                    # 預設 .minecraft 目錄
 *   java -jar francium-loader.jar --game-dir /path    # 指定遊戲目錄
 *   java -jar francium-loader.jar --version           # 顯示版本
 *   java -jar francium-loader.jar --help              # 顯示幫助
 * </pre>
 */
@PublicApi
public class FranciumBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(FranciumBootstrap.class);

    private static final String VERSION;

    static {
        String v = FranciumBootstrap.class.getPackage().getImplementationVersion();
        VERSION = (v != null) ? v : "1.6.0-dev";
    }
    private static final String BANNER = """
          ______                     _                 \s
         |  ____|                   (_)                \s
         | |__ __ _ _ __   ___ _   _ _  ___ _ __ ___   \s
         |  __/ _` | '_ \\ / __| | | | |/ __| '_ ` _ \\  \s
         | | | (_| | | | | (__| |_| | | (__| | | | | | \s
         |_|  \\__,_|_| |_|\\___|\\__,_|_|\\___|_| |_| |_| \s
                                                        \s
         下一代 Minecraft 模組加載器 - AI 驅動的版本橋接與 DAG 並行加載
        """;

    public static void main(String[] args) {
        LOGGER.info(BANNER);
        LOGGER.info("  Version: " + VERSION);
        LOGGER.info("  Java: " + System.getProperty("java.version"));
        LOGGER.info("  OS: " + System.getProperty("os.name"));
        LOGGER.info("");

        // ★ BUG FIX: 檢查環境變數 FRANCIUM_GAME_DIR
        String envGameDir = System.getenv("FRANCIUM_GAME_DIR");
        Path gameDir = envGameDir != null && !envGameDir.isEmpty()
            ? Paths.get(envGameDir)
            : Paths.get(System.getProperty("user.home"), ".minecraft");
        boolean showHelp = false;
        boolean showVersion = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--game-dir", "-g" -> {
                    if (i + 1 < args.length) gameDir = Paths.get(args[++i]);
                }
                case "--version", "-v" -> showVersion = true;
                case "--help", "-h" -> showHelp = true;
                case "--debug" -> System.setProperty("francium.debug", "true");
            }
        }

        // ★ BUG FIX: --version 應打印版本後退出
        if (showVersion) {
            LOGGER.info("Francium Mod Loader version: {}", VERSION);
            return;
        }

        if (showHelp) {
            printHelp();
            return;
        }

        // 啟動加載器
        LOGGER.info("Initializing Francium Mod Loader...");
        LOGGER.info("  Game directory: " + gameDir.toAbsolutePath());
        LOGGER.info("");

        FranciumLoader loader = new FranciumLoader(gameDir);

        try {
            loader.loadConfig();
            loader.initialize();

            LOGGER.info("Launching...");
            LOGGER.info("");

            LoadReport report = loader.launch();

            LOGGER.info("");
            LOGGER.info("{}", report);

            // 輸出各階段計時
            LOGGER.info("");
            LOGGER.info("Phase timings:");
            for (var entry : loader.phaseTimings().entrySet()) {
                LOGGER.info(String.format("  %-12s %dms", entry.getKey() + ":", entry.getValue()));
            }

            // 記憶體快照
            var memSnapshot = loader.getMemorySnapshot();
            if (memSnapshot != null) {
                LOGGER.info("");
                LOGGER.info("{}", memSnapshot);
            }

            // 註冊關閉鉤子（★ BUG FIX: 防止重複註冊，為執行緒命名）
            Thread shutdownHook = new Thread(() -> {
                LOGGER.info("Shutting down Francium...");
                loader.shutdown();
            }, "FranciumShutdownHook");
            Runtime.getRuntime().addShutdownHook(shutdownHook);

        } catch (Exception e) {
            LOGGER.warn("");
            LOGGER.error("FATAL: Failed to launch Francium Mod Loader");
            LOGGER.warn("  " + e.getMessage());
            LOGGER.error("Exception", e);
            throw new RuntimeException("Francium Mod Loader failed to launch", e);
        }
    }

    private static void printHelp() {
        LOGGER.info("""
            Usage: francium-loader [options]
                        
            Options:
              --game-dir, -g <path>   Specifies the Minecraft game directory
                                       (default: ~/.minecraft)
              --version, -v           Prints the version and exit
              --help, -h              Shows this help message
              --debug                 Enables debug logging
                        
            Examples:
              java -jar francium-loader.jar
              java -jar francium-loader.jar --game-dir ~/.minecraft
              java -jar francium-loader.jar --debug
                        
            Environment:
              FRANCIUM_GAME_DIR       Alternative way to set game directory
                        
            For more information: https://github.com/stanley-1028/francium-loader
            """);
    }
}
