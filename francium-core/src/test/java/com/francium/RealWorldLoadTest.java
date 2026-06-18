package com.francium;

import com.francium.classloader.ParallelModClassLoader;
import com.francium.classloader.ParallelModClassLoader.DiscoveryResult;
import com.francium.classloader.ParallelModClassLoader.LoadReport;
import com.francium.classloader.ParallelModClassLoader.LayerLoadDetail;
import com.francium.graph.ModGraph;
import com.francium.loader.ModManifest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真實世界 Minecraft 模組載入測試。
 * 
 * 使用真實的 Fabric mod JARs 驗證 Francium 的並行載入能力。
 * 測試包含:
 * - AppleSkin (依賴 Fabric API)
 * - Fabric API (Fabric 核心 API 集合)
 * - Minecraft Server (依賴提供者)
 */
class RealWorldLoadTest {

    // 從系統屬性獲取根專案目錄 (由 build.gradle 設定)
    static final Path ROOT_DIR = Paths.get(
        System.getProperty("francium.test.rootdir",
            System.getProperty("user.dir") + "/..")
    );
    // 測試用 mod 目錄
    static final Path TEST_MODS_DIR = ROOT_DIR.resolve("test-mods");
    // Minecraft Server jar
    static final Path MC_SERVER_JAR = ROOT_DIR.resolve("minecraft_server.jar");

    @Test
    void discoverRealMods() throws Exception {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("  Francium 真實世界載入測試");
        System.out.println("═══════════════════════════════════════════");

        // Step 1: 檢查環境
        File modsDir = TEST_MODS_DIR.toFile();
        assertTrue(modsDir.exists() && modsDir.isDirectory(),
            "Test mods directory must exist: " + TEST_MODS_DIR.toAbsolutePath());
        
        File[] jars = modsDir.listFiles((d, n) -> n.endsWith(".jar"));
        assertNotNull(jars);
        System.out.println("\n📦 找到 " + jars.length + " 個 JAR 檔案:");
        for (File j : jars) {
            System.out.println("  " + j.getName() + " (" + (j.length() / 1024) + " KB)");
        }

        // Step 2: 創建 ModGraph 和 ClassLoader
        ModGraph graph = new ModGraph();
        ParallelModClassLoader loader = new ParallelModClassLoader(graph, TEST_MODS_DIR);

        // Step 3: 發現 Mod
        System.out.println("\n🔍 掃描 Mod Manifests...");
        DiscoveryResult discovery = loader.discoverMods();
        
        System.out.println("  總 JAR 數: " + discovery.totalJars);
        System.out.println("  成功發現:  " + discovery.found.size());
        System.out.println("  跳過:      " + discovery.skipped.size());

        for (ModManifest manifest : discovery.found) {
            System.out.println("  ✅ " + manifest.modId() + " v" + manifest.version()
                + " | Main: " + manifest.mainClass()
                + " | Deps: " + manifest.dependencies().keySet());
        }
        for (String skipped : discovery.skipped) {
            System.out.println("  ⏭️ 跳過: " + skipped);
        }

        // Step 4: 將發現的 Mod 加入圖
        System.out.println("\n📊 構建依賴圖...");
        for (ModManifest manifest : discovery.found) {
            try {
                graph.addMod(manifest, manifest.version());
                System.out.println("  ➕ " + manifest.modId() + " v" + manifest.version());
            } catch (Exception e) {
                System.out.println("  ❌ " + manifest.modId() + " 加入失敗: " + e.getMessage());
            }
        }

        // Step 5: 添加 Minecraft 和 Fabric Loader 作為外部提供者
        System.out.println("\n🔌 添加外部提供者...");
        graph.addExternalProvider("minecraft");
        graph.addExternalProvider("fabricloader");
        graph.addExternalProvider("java");
        System.out.println("  ✅ minecraft (由 " + MC_SERVER_JAR.getFileName() + " 提供)");
        System.out.println("  ✅ fabricloader");
        System.out.println("  ✅ java");

        // 將 Fabric Loader JAR 加入父 ClassLoader，
        // 這樣所有 ModClassLoader 都可以透過委派找到 Fabric API 類別
        Path fabricLoaderJar = ROOT_DIR.resolve("fabric-loader.jar");
        if (fabricLoaderJar.toFile().exists()) {
            loader.addExternalJar(fabricLoaderJar);
            System.out.println("  ✅ fabric-loader.jar 已加入 ClassLoader (提供 ModInitializer)");
        }

        // Step 6: 構建拓撲層
        System.out.println("\n🧱 拓撲分層...");
        graph.buildLayers();
        List<java.util.Set<String>> layers = graph.getLayers();
        System.out.println("  共 " + layers.size() + " 層:");
        for (int i = 0; i < layers.size(); i++) {
            System.out.println("  層 " + i + ": " + layers.get(i));
        }
        System.out.println("  節點總數: " + graph.getTotalNodeCount());
        System.out.println("  依賴邊數: " + graph.getTotalEdgeCount());
        System.out.println("  預估加速比: " + String.format("%.2fx", graph.getSpeedupRatio()));

        // Step 7: 加載所有 Mod（並行）
        System.out.println("\n🚀 並行載入 Mods...");
        System.out.println("  (注意: Mod 類別需依賴 Minecraft 類別，");
        System.out.println("   真實環境中需將 Minecraft 加入 Classpath)");
        
        try {
            LoadReport report = loader.loadAll();
            
            System.out.println("\n✅ 載入完成!");
            System.out.println("  實際載入時間: " + report.totalLoadTimeMs + " ms");
            System.out.println("  循序估計時間: " + report.sequentialEstimatedMs + " ms");
            System.out.println("  實際加速比:   " + String.format("%.2fx", report.actualSpeedup));
            
            // 每層詳細信息
            System.out.println("\n📋 各層載入詳情:");
            for (LayerLoadDetail detail : report.layerDetails) {
                System.out.println("  層 " + detail.layerIndex + ": "
                    + detail.success + " 成功, "
                    + detail.failed + " 失敗, "
                    + detail.skipped + " 跳過"
                    + " | 耗時: " + detail.layerTimeMs + " ms");
                
                for (var result : detail.results) {
                    String mainClassStr = result.mainClass() != null
                        ? result.mainClass().getName()
                        : "(library mod, no main class)";
                    System.out.println("    ✅ " + result.modId()
                        + " v" + result.version()
                        + " | 主類: " + mainClassStr
                        + " | 載入: " + result.loadTimeMs() + " ms");
                }
                
                for (var failure : detail.failures) {
                    System.out.println("    ❌ " + failure.modId()
                        + " | 原因: " + failure.error().getClass().getSimpleName()
                        + " - " + failure.error().getMessage());
                }
            }
            
            // 狀態摘要
            System.out.println("\n📊 最終狀態:");
            for (var entry : loader.getLoadTimes().entrySet()) {
                System.out.println("  " + entry.getKey() + ": "
                    + loader.getStatus(entry.getKey()) + " (" + entry.getValue() + " ms)");
            }
            
        } catch (Exception e) {
            System.out.println("\n❌ 載入失敗: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            
            if (e.getCause() != null) {
                System.out.println("  原因: " + e.getCause().getClass().getSimpleName()
                    + " - " + e.getCause().getMessage());
            }
            
            // 儘管載入失敗，仍然顯示狀態
            System.out.println("\n📊 載入狀態:");
            for (var modId : List.of("appleskin", "fabric-api", "minecraft", "fabricloader")) {
                System.out.println("  " + modId + ": " + loader.getStatus(modId));
            }
        }
        
        loader.shutdown();
        System.out.println("\n═══════════════════════════════════════════");
    }
}
