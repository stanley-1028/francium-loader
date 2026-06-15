package com.francium;

import com.francium.classloader.ParallelModClassLoader;
import com.francium.classloader.ParallelModClassLoader.DiscoveryResult;
import com.francium.graph.ModGraph;
import com.francium.loader.FranciumLoader;
import com.francium.loader.FranciumException;
import com.francium.loader.FranciumLoader.FranciumReport;
import com.francium.resolver.model.DependencyConstraint;
import com.francium.resolver.model.SemanticVersion;
import com.francium.resolver.sat.SATDependencyResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minecraft 整合測試。
 *
 * 模擬真實 Minecraft 啟動流程：
 * 1. 建立含 francium-mod.json 的 JAR 檔案
 * 2. 用 ParallelModClassLoader 掃描
 * 3. SAT 依賴解析 + DAG 拓撲分層
 * 4. LaunchWrapper 風格五階段啟動（FranciumLoader API）
 * 5. 衝突檢測與大規模模組測試
 *
 * 注意：此測試不使用真實 .class 檔案（避免 JaCoCo instrumentation 干擾），
 * 而是聚焦於 mod 發現、依賴解析、DAG 排程與生命週期 API。
 */
public class MinecraftIntegrationTest {

    @TempDir
    Path tempDir;

    /** 建立一個 mod JAR（僅含 francium-mod.json，無 .class）。 */
    private Path createModJar(String modId, String version, String mainClass) throws IOException {
        return createModJar(modId, version, mainClass, null, null);
    }

    private Path createModJar(String modId, String version, String mainClass,
                              Map<String, String> deps, Map<String, String> conflicts) throws IOException {
        Path jar = tempDir.resolve(modId + "-" + version + ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar.toFile()))) {
            jos.putNextEntry(new JarEntry("francium-mod.json"));
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"modId\": \"").append(modId).append("\",\n");
            json.append("  \"version\": \"").append(version).append("\",\n");
            json.append("  \"name\": \"").append(modId).append("\",\n");
            json.append("  \"mainClass\": \"").append(mainClass).append("\",\n");
            if (deps != null && !deps.isEmpty()) {
                json.append("  \"dependencies\": {\n");
                var it = deps.entrySet().iterator();
                while (it.hasNext()) {
                    var e = it.next();
                    json.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"");
                    if (it.hasNext()) json.append(",");
                    json.append("\n");
                }
                json.append("  },\n");
            }
            if (conflicts != null && !conflicts.isEmpty()) {
                json.append("  \"conflicts\": {\n");
                var it = conflicts.entrySet().iterator();
                while (it.hasNext()) {
                    var e = it.next();
                    json.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"");
                    if (it.hasNext()) json.append(",");
                    json.append("\n");
                }
                json.append("  },\n");
            }
            json.append("  \"loadPriority\": 0,\n");
            json.append("  \"aiBridgeEnabled\": true\n");
            json.append("}\n");
            jos.write(json.toString().getBytes());
            jos.closeEntry();
        }
        return jar;
    }

    /** JAR 建立在 tempDir/mods/ 下（給 FranciumLoader 使用）。 */
    private Path createModJarInModsDir(String modId, String version, String mainClass,
                                       Map<String, String> deps) throws IOException {
        return createModJarInModsDirWithConflicts(modId, version, mainClass, deps, null);
    }

    private Path createModJarInModsDir(String modId, String version, String mainClass) throws IOException {
        return createModJarInModsDir(modId, version, mainClass, null);
    }

    private Path createModJarInModsDirWithConflicts(String modId, String version, String mainClass,
                                                    Map<String, String> deps,
                                                    Map<String, String> conflicts) throws IOException {
        Path modsDir = tempDir.resolve("mods");
        java.nio.file.Files.createDirectories(modsDir);
        Path jar = modsDir.resolve(modId + "-" + version + ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar.toFile()))) {
            jos.putNextEntry(new JarEntry("francium-mod.json"));
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"modId\": \"").append(modId).append("\",\n");
            json.append("  \"version\": \"").append(version).append("\",\n");
            json.append("  \"name\": \"").append(modId).append("\",\n");
            json.append("  \"mainClass\": \"").append(mainClass).append("\",\n");
            if (deps != null && !deps.isEmpty()) {
                json.append("  \"dependencies\": {\n");
                var it = deps.entrySet().iterator();
                while (it.hasNext()) {
                    var e = it.next();
                    json.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"");
                    if (it.hasNext()) json.append(",");
                    json.append("\n");
                }
                json.append("  },\n");
            }
            json.append("  \"loadPriority\": 0\n");
            json.append("}\n");
            jos.write(json.toString().getBytes());
            jos.closeEntry();
        }
        return jar;
    }

    // ════════════════════════════════════════════════════════════
    //  Test 1: 完整管線 — 掃描 → SAT → DAG
    // ════════════════════════════════════════════════════════════

    @Test
    void fullPipelineWithRealJars() throws Exception {
        createModJar("library",       "1.0.0", "com.example.Library");
        createModJar("core-api",      "2.0.0", "com.example.CoreApi",
            Map.of("library", ">=1.0.0"), null);
        createModJar("my-mod",        "1.5.0", "com.example.MyMod",
            Map.of("core-api", ">=2.0.0", "library", ">=1.0.0"), null);
        createModJar("my-addon",      "0.1.0", "com.example.MyAddon",
            Map.of("my-mod", ">=1.0.0"), null);

        // Phase 1: 掃描
        ModGraph graph = new ModGraph();
        graph.addExternalProvider("minecraft");
        ParallelModClassLoader classLoader = new ParallelModClassLoader(graph, tempDir);
        DiscoveryResult discovered = classLoader.discoverMods();
        assertEquals(4, discovered.found.size(), "Should discover 4 mods");
        assertEquals(4, discovered.totalJars, "Should see 4 JARs");

        // Phase 2: SAT 解析
        SATDependencyResolver resolver = new SATDependencyResolver();
        for (var manifest : discovered.found) {
            SemanticVersion sv = SemanticVersion.tryParse(manifest.version());
            if (sv != null) resolver.registerVersions(manifest.modId(), List.of(sv));
            Map<String, DependencyConstraint> deps = new LinkedHashMap<>();
            for (var e : manifest.dependencies().entrySet())
                deps.put(e.getKey(), new DependencyConstraint(e.getValue()));
            resolver.registerDependencies(manifest.modId(), deps);
        }
        var satResult = resolver.solve(List.of("my-mod", "my-addon"));
        assertTrue(satResult.success, "SAT should resolve all 4 mods");
        assertEquals(4, satResult.solution.size());

        // Phase 3: DAG
        for (var e : satResult.solution.entrySet()) {
            var manifest = discovered.found.stream()
                .filter(m -> m.modId().equals(e.getKey())).findFirst().orElseThrow();
            graph.addMod(manifest, e.getValue().toString());
        }
        var layers = graph.getLayers();
        assertTrue(layers.size() >= 3, "DAG: >=3 layers");

        // library is in layer 0 (no deps)
        assertTrue(layers.get(0).contains("library"), "Layer 0: library");

        classLoader.shutdown();
    }

    // ════════════════════════════════════════════════════════════
    //  Test 2: LaunchWrapper 風格 — 五階段啟動
    // ════════════════════════════════════════════════════════════

    @Test
    void launchWrapperStyleFivePhaseLifecycle() throws Exception {
        // JAR 放 tempDir/mods/ 下（FranciumLoader 預設 mods 目錄）
        createModJarInModsDir("lib-a", "1.0.0", "com.example.LibA");
        createModJarInModsDir("mod-b", "2.0.0", "com.example.ModB",
            Map.of("lib-a", ">=1.0.0"));

        FranciumLoader loader = FranciumLoader.builder(tempDir)
            .withParallelLoading(true)
            .withMemoryManagement(true)
            .build();

        try {

        // Phase 1
        loader.scanMods();
        assertEquals(FranciumLoader.LoaderState.DISCOVERING, loader.state());
        assertTrue(loader.phaseTimings().containsKey("discovery"));

        // Phase 2
        loader.resolveDependencies();
        assertTrue(loader.phaseTimings().containsKey("resolution"));

        // Phase 3
        loader.buildLoadGraph();
        assertTrue(loader.modGraph().getLayerCount() > 0, "DAG: >=1 layer");

        // Phase 4
        // Phase 4: 嘗試載入（可能因無真實 .class 而拋 FranciumException，
        // 但生命週期 API 應正確執行至此）
        try {
            loader.loadMods();
            assertEquals(FranciumLoader.LoaderState.READY, loader.state());
        } catch (FranciumException e) {
            assertEquals(FranciumException.Phase.LOADING, e.getPhase(),
                "If loadMods fails, it should be a LOADING phase error");
            assertEquals(FranciumLoader.LoaderState.LOADING, loader.state());
        }

        // Phase 5: 報告（即使 loading 失敗也應有部分數據）
        FranciumReport report = loader.getReport();
        assertTrue(report.totalMods > 0, "Report: non-zero mod count");
        assertTrue(report.layers > 0, "Report: >0 layers");

        } finally {
            loader.shutdown();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Test 3: 衝突檢測
    // ════════════════════════════════════════════════════════════

    @Test
    void conflictingVersionsDetectedBySAT() throws Exception {
        createModJar("dep-a", "1.0.0", "com.example.DepA",
            Map.of("shared", ">=2.0.0"), null);
        createModJar("dep-b", "1.0.0", "com.example.DepB",
            Map.of("shared", "<2.0.0"), null);
        createModJar("shared", "1.5.0", "com.example.Shared");

        ModGraph graph = new ModGraph();
        ParallelModClassLoader cl = new ParallelModClassLoader(graph, tempDir);
        var discovered = cl.discoverMods();

        SATDependencyResolver resolver = new SATDependencyResolver();
        for (var m : discovered.found) {
            SemanticVersion sv = SemanticVersion.tryParse(m.version());
            if (sv != null) resolver.registerVersions(m.modId(), List.of(sv));
            Map<String, DependencyConstraint> deps = new LinkedHashMap<>();
            for (var e : m.dependencies().entrySet())
                deps.put(e.getKey(), new DependencyConstraint(e.getValue()));
            resolver.registerDependencies(m.modId(), deps);
        }
        var result = resolver.solve(List.of("dep-a", "dep-b"));
        assertFalse(result.success, "SAT: conflict should fail");
        cl.shutdown();
    }

    // ════════════════════════════════════════════════════════════
    //  Test 4: 大量獨立 mod 在同一層
    // ════════════════════════════════════════════════════════════

    @Test
    void bulkIndependentModsSingleLayer() throws IOException {
        int n = 50;
        for (int i = 0; i < n; i++)
            createModJar("m-" + i, "1.0." + i, "com.example.M" + i);

        ModGraph graph = new ModGraph();
        ParallelModClassLoader cl = new ParallelModClassLoader(graph, tempDir);
        var discovered = cl.discoverMods();
        assertEquals(n, discovered.totalJars, "Found " + n + " mods");

        for (var m : discovered.found) graph.addMod(m, m.version());
        var layers = graph.getLayers();
        assertEquals(1, layers.size(), "Independent: 1 layer");
        assertEquals(n, layers.get(0).size(), "All 50 in layer 0");

        cl.shutdown();
    }
}
