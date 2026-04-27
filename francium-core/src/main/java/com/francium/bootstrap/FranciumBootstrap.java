package com.francium.bootstrap;

import com.francium.loader.FranciumLoader;
import com.francium.loader.FranciumLoader.LoaderState;
import com.francium.classloader.ParallelModClassLoader.LoadReport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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
public class FranciumBootstrap {
    private static final String VERSION = "1.0.0-alpha";
    private static final String BANNER = """
          ______                     _                  \s
         |  ____|                   (_)                 \s
         | |__ __ _ _ __   ___ _   _ _  ___ _ __ ___    \s
         |  __/ _` | '_ \\ / __| | | | |/ __| '_ ` _ \\   \s
         | | | (_| | | | | (__| |_| | | (__| | | | | |  \s
         |_|  \\__,_|_| |_|\\___|\\__,_|_|\\___|_| |_| |_|  \s
                                                        \s
         下一代 Minecraft 模組加載器 - AI 驅動的版本橋接與 DAG 並行加載
        """;

    public static void main(String[] args) {
        System.out.println(BANNER);
        System.out.println("  Version: " + VERSION);
        System.out.println("  Java: " + System.getProperty("java.version"));
        System.out.println("  OS: " + System.getProperty("os.name"));
        System.out.println();

        // 解析命令列參數
        Path gameDir = Paths.get(System.getProperty("user.home"), ".minecraft");
        boolean showHelp = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--game-dir", "-g" -> {
                    if (i + 1 < args.length) gameDir = Paths.get(args[++i]);
                }
                case "--version", "-v" -> {
                    return; // 已顯示
                }
                case "--help", "-h" -> showHelp = true;
                case "--debug" -> System.setProperty("francium.debug", "true");
            }
        }

        if (showHelp) {
            printHelp();
            return;
        }

        // 啟動加載器
        System.out.println("Initializing Francium Mod Loader...");
        System.out.println("  Game directory: " + gameDir.toAbsolutePath());
        System.out.println();

        FranciumLoader loader = new FranciumLoader(gameDir);

        try {
            loader.loadConfig();
            loader.initialize();

            System.out.println("Launching...");
            System.out.println();

            LoadReport report = loader.launch();

            System.out.println();
            System.out.println(report);

            // 輸出各階段計時
            System.out.println();
            System.out.println("Phase timings:");
            for (var entry : loader.phaseTimings().entrySet()) {
                System.out.printf("  %-12s %dms%n", entry.getKey() + ":", entry.getValue());
            }

            // 記憶體快照
            var memSnapshot = loader.getMemorySnapshot();
            if (memSnapshot != null) {
                System.out.println();
                System.out.println(memSnapshot);
            }

            // 註冊關閉鉤子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down Francium...");
                loader.shutdown();
            }));

        } catch (Exception e) {
            System.err.println();
            System.err.println("FATAL: Failed to launch Francium Mod Loader");
            System.err.println("  " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println("""
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
                        
            For more information: https://github.com/francium-loader/francium
            """);
    }
}
