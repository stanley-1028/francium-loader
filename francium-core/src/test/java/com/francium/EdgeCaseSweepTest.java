package com.francium;

import com.francium.classloader.ParallelModClassLoader;
import com.francium.classloader.ParallelModClassLoader.*;
import com.francium.graph.ModGraph;
import com.francium.loader.ModManifest;
import com.francium.loader.ModManifest.Builder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ═══════════════════════════════════════════════════════════
 *  边角案例大扫除 — Edge Case Sweep
 * ═══════════════════════════════════════════════════════════
 *
 *  覆盖七个模块的边角情况:
 *   1. ModManifest JSON 解析边界 (空/损坏/特殊字符/编码)
 *   2. ModManifest Fabric 格式边界
 *   3. ModManifest Forge TOML 格式边界
 *   4. ModGraph 依赖/查询边界
 *   5. ParallelModClassLoader 生命周期边界
 *   6. LoaderConfig 配置边界
 *   7. 版本字符串/依赖约束边界
 */
@TestInstance(Lifecycle.PER_CLASS)
public class EdgeCaseSweepTest {

    // ═══════════════════════════════════════════════════════════
    //  1. ModManifest JSON 解析边角
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ModManifest JSON 解析边界")
    class JsonParsingEdgeCases {

        @Test
        @DisplayName("空字符串返回 null")
        void emptyStringReturnsNull() {
            assertNull(ModManifest.fromJson(""), "Empty string should return null");
        }

        @Test
        @DisplayName("纯空白字符串返回 null")
        void whitespaceOnlyReturnsNull() {
            assertNull(ModManifest.fromJson("   \n\t\r  "), "Whitespace should return null");
        }

        @Test
        @DisplayName("null 输入 (空引用转字符串)")
        void nullAsStringInput() {
            // 如果是调用方传了 "null" 字符串
            assertNull(ModManifest.fromJson("null"), "String 'null' should return null");
        }

        @Test
        @DisplayName("缺少 modId 返回 null")
        void missingModIdReturnsNull() {
            String json = "{ \"version\": \"1.0.0\" }";
            assertNull(ModManifest.fromJson(json), "No modId should return null");
        }

        @Test
        @DisplayName("缺少 version 返回 null")
        void missingVersionReturnsNull() {
            String json = "{ \"modId\": \"testmod\" }";
            assertNull(ModManifest.fromJson(json), "No version should return null");
        }

        @Test
        @DisplayName("截断的 JSON (缺少闭合括号)")
        void truncatedJsonNoClosingBrace() {
            String json = "{ \"modId\": \"testmod\", \"version\": \"1.0.0\" ";
            // 应该返回 null 或部分结果
            var result = ModManifest.fromJson(json);
            // 只要能处理不崩溃就行
            System.out.println("  Truncated JSON result: " + result);
        }

        @Test
        @DisplayName("JSON 末尾有多余逗号")
        void trailingComma() {
            String json = "{ \"modId\": \"testmod\", \"version\": \"1.0.0\", }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result, "Should still parse with trailing comma");
            assertEquals("testmod", result.modId());
        }

        @Test
        @DisplayName("包含 Unicode 转义字符")
        void unicodeEscapedCharacters() {
            String json = "{ \"modId\": \"test\\u004dod\", \"version\": \"1.0.0\" }";
            // 我们的简易解析器不处理转义, 但不应崩溃
            var result = ModManifest.fromJson(json);
            // 可能保留原始字符串
            System.out.println("  Unicode escaped result modId: " + (result != null ? result.modId() : "null"));
        }

        @Test
        @DisplayName("中文字符正常解析")
        void chineseCharacters() {
            String json = "{ \"modId\": \"中文模组\", \"version\": \"1.0.0\", \"name\": \"测试\" }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result);
            assertEquals("中文模组", result.modId());
            assertEquals("测试", result.name());
        }

        @Test
        @DisplayName("版本号包含特殊字符 (SNAPSHOT, beta, rc)")
        void versionWithSpecialSuffix() {
            String json = "{ \"modId\": \"snapmod\", \"version\": \"2.0.0-SNAPSHOT+2024.01.15\" }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result);
            assertEquals("2.0.0-SNAPSHOT+2024.01.15", result.version());
        }

        @Test
        @DisplayName("mainClass 为空白字符串")
        void mainClassIsBlank() {
            String json = "{ \"modId\": \"nomain\", \"version\": \"1.0\", \"mainClass\": \"\" }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result);
            assertEquals("", result.mainClass());
        }

        @Test
        @DisplayName("loadPriority 为负值")
        void negativeLoadPriority() {
            String json = "{ \"modId\": \"earlymod\", \"version\": \"1.0\", \"loadPriority\": -10 }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result);
            assertEquals(-10, result.loadPriority());
        }

        @Test
        @DisplayName("loadPriority 为零值")
        void zeroLoadPriority() {
            String json = "{ \"modId\": \"defaultmod\", \"version\": \"1.0\", \"loadPriority\": 0 }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result);
            assertEquals(0, result.loadPriority());
        }

        @Test
        @DisplayName("aiBridgeEnabled 为 false")
        void aiBridgeDisabled() {
            String json = "{ \"modId\": \"noaimod\", \"version\": \"1.0\", \"aiBridgeEnabled\": false }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result);
            assertFalse(result.aiBridgeEnabled());
        }

        @Test
        @DisplayName("aiBridgeEnabled 为各种布尔格式")
        void aiBridgeVariousBoolFormats() {
            // "true" should work
            var r1 = ModManifest.fromJson("{ \"modId\": \"a\", \"version\": \"1\", \"aiBridgeEnabled\": true }");
            assertNotNull(r1);
            assertTrue(r1.aiBridgeEnabled());

            // "TRUE" (uppercase)
            var r2 = ModManifest.fromJson("{ \"modId\": \"b\", \"version\": \"1\", \"aiBridgeEnabled\": TRUE }");
            assertNotNull(r2);
        }

        @Test
        @DisplayName("超大 JSON (10万字符) 不崩溃")
        void hugeJsonDoesNotCrash() {
            StringBuilder sb = new StringBuilder();
            sb.append("{ \"modId\": \"bigmod\", \"version\": \"1.0\", \"description\": \"");
            // Generate 100KB of text
            for (int i = 0; i < 100_000; i++) {
                sb.append('A');
            }
            sb.append("\" }");
            var result = ModManifest.fromJson(sb.toString());
            assertNotNull(result);
            assertEquals("bigmod", result.modId());
        }

        @Test
        @DisplayName("空依赖对象")
        void emptyDependencies() {
            String json = "{ \"modId\": \"nomodep\", \"version\": \"1.0\", \"dependencies\": {} }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result);
            assertTrue(result.dependencies().isEmpty());
        }

        @Test
        @DisplayName("依赖值含版本运算符")
        void dependencyWithVersionOperators() {
            String json = "{ \"modId\": \"depmod\", \"version\": \"1.0\", " +
                "\"dependencies\": { \"modA\": \">=1.2.0 <2.0.0\", \"modB\": \"~1.0\", \"modC\": \"^2.0\" } }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result);
            assertEquals(">=1.2.0 <2.0.0", result.dependencies().get("modA"));
            assertEquals("~1.0", result.dependencies().get("modB"));
            assertEquals("^2.0", result.dependencies().get("modC"));
        }

        @Test
        @DisplayName("带 BOM 的 JSON")
        void jsonWithBom() {
            // UTF-8 BOM: 0xEF, 0xBB, 0xBF
            String json = "\uFEFF{ \"modId\": \"bommod\", \"version\": \"1.0\" }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result, "Should handle BOM prefix");
            assertEquals("bommod", result.modId());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  2. ModManifest Fabric 格式边界
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ModManifest Fabric 格式边界")
    class FabricFormatEdgeCases {

        @Test
        @DisplayName("标准 Fabric 格式")
        void standardFabricFormat() {
            String json = "{ \"id\": \"fabricmod\", \"version\": \"1.0.0\", " +
                "\"name\": \"Fabric Mod\", \"description\": \"Test\", " +
                "\"entrypoints\": { \"main\": [ \"com.example.Main\" ] } }";
            var result = ModManifest.fromFabricJson(json);
            assertNotNull(result);
            assertEquals("fabricmod", result.modId());
            assertEquals("com.example.Main", result.mainClass());
        }

        @Test
        @DisplayName("Fabric entrypoints 为字符串格式")
        void fabricEntrypointStringFormat() {
            String json = "{ \"id\": \"stringmod\", \"version\": \"1.0\", " +
                "\"entrypoints\": { \"main\": \"com.example.Main\" } }";
            var result = ModManifest.fromFabricJson(json);
            assertNotNull(result);
            assertEquals("com.example.Main", result.mainClass());
        }

        @Test
        @DisplayName("Fabric 缺少 id 返回 null")
        void fabricMissingIdReturnsNull() {
            String json = "{ \"version\": \"1.0\", \"name\": \"NoId\" }";
            assertNull(ModManifest.fromFabricJson(json));
        }

        @Test
        @DisplayName("Fabric 缺少 version 时使用 'unknown'")
        void fabricMissingVersionUsesUnknown() {
            String json = "{ \"id\": \"noversion\" }";
            var result = ModManifest.fromFabricJson(json);
            assertNotNull(result);
            assertEquals("unknown", result.version());
        }

        @Test
        @DisplayName("Fabric 多 entrypoint 数组格式")
        void fabricMultipleEntrypoints() {
            String json = "{ \"id\": \"multimod\", \"version\": \"1.0\", " +
                "\"entrypoints\": { " +
                "\"main\": [ \"com.example.Main\", \"com.example.Backup\" ], " +
                "\"client\": [ \"com.example.ClientInit\" ] " +
                "} }";
            var result = ModManifest.fromFabricJson(json);
            assertNotNull(result);
            // 应取第一个 entrypoint
            assertEquals("com.example.Main", result.mainClass());
        }

        @Test
        @DisplayName("Fabric 没有 entrypoints 时不崩溃")
        void fabricNoEntrypoints() {
            String json = "{ \"id\": \"noentry\", \"version\": \"1.0\" }";
            var result = ModManifest.fromFabricJson(json);
            assertNotNull(result);
            assertNull(result.mainClass(), "No entrypoints means null mainClass");
        }

        @Test
        @DisplayName("Fabric 有 depends 但不含目标")
        void fabricEmptyDepends() {
            String json = "{ \"id\": \"emptydep\", \"version\": \"1.0\", \"depends\": {} }";
            var result = ModManifest.fromFabricJson(json);
            assertNotNull(result);
            assertTrue(result.dependencies().isEmpty());
        }

        @Test
        @DisplayName("Fabric 使用 recommends 代替 depends")
        void fabricRecommendsNotDepends() {
            String json = "{ \"id\": \"recommendmod\", \"version\": \"1.0\", " +
                "\"recommends\": { \"fabric-api\": \">=0.90\" } }";
            var result = ModManifest.fromFabricJson(json);
            assertNotNull(result);
            // recommends 不在解析范围内, 所以 dep 为空
            assertTrue(result.dependencies().isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  3. ModManifest Forge TOML 格式边界
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ModManifest Forge TOML 格式边界")
    class ForgeTomlFormatEdgeCases {

        @Test
        @DisplayName("标准 Forge mods.toml")
        void standardForgeToml() {
            String toml = "modLoader=\"javafml\"\n" +
                "loaderVersion=\"[40,)\"\n" +
                "[[mods]]\n" +
                "modId=\"forgemod\"\n" +
                "version=\"1.0.0\"\n" +
                "displayName=\"Forge Mod\"\n" +
                "description=\"A test\"\n";
            var result = ModManifest.fromForgeToml(toml);
            assertNotNull(result);
            assertEquals("forgemod", result.modId());
        }

        @Test
        @DisplayName("TOML 缺少 modId 返回 null")
        void tomlMissingModId() {
            String toml = "version=\"1.0.0\"\n";
            assertNull(ModManifest.fromForgeToml(toml));
        }

        @Test
        @DisplayName("TOML 缺少 version 使用 'unknown'")
        void tomlMissingVersion() {
            String toml = "modId=\"noversionmod\"\n";
            var result = ModManifest.fromForgeToml(toml);
            assertNotNull(result);
            assertEquals("unknown", result.version());
        }

        @Test
        @DisplayName("TOML 含多个 [[mods]] 区块")
        void tomlMultipleModsSections() {
            String toml = "modLoader=\"javafml\"\n" +
                "[[mods]]\n" +
                "modId=\"mainmod\"\n" +
                "version=\"1.0\"\n" +
                "[[mods]]\n" +
                "modId=\"submod\"\n" +
                "version=\"0.5\"\n";
            // 应解析第一个
            var result = ModManifest.fromForgeToml(toml);
            assertNotNull(result);
            assertEquals("mainmod", result.modId());
        }

        @Test
        @DisplayName("TOML 格式含注释和空白")
        void tomlWithComments() {
            String toml = "# This is a comment\n" +
                "\n" +
                "modLoader=\"javafml\"\n" +
                "# Another comment\n" +
                "[[mods]]\n" +
                "modId=\"commentmod\"\n" +
                "version=\"2.0\"\n" +
                "description=\"Has\\nnewlines\"\n";
            var result = ModManifest.fromForgeToml(toml);
            assertNotNull(result);
            assertEquals("commentmod", result.modId());
        }

        @Test
        @DisplayName("TOML 使用单引号字符串")
        void tomlSingleQuotedString() {
            String toml = "modId='singlequotemod'\nversion='3.0'\n";
            // 我们的解析器只匹配双引号
            var result = ModManifest.fromForgeToml(toml);
            // 单引号不被解析器处理 → 返回 null
            System.out.println("  Single-quoted TOML result: " + result);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  4. ModGraph 边界
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ModGraph 依赖/查询边界")
    class ModGraphEdgeCases {

        private ModGraph graph;

        @BeforeEach
        void setUp() {
            graph = new ModGraph();
        }

        @Test
        @DisplayName("依赖项不存在于图中时仍能正确分层 (外部依赖)")
        void dependencyNotInGraph() {
            // modA 依赖 modB，但 modB 未注册
            var modA = ModManifest.builder("modA", "1.0")
                .mainClass("ex.ModA")
                .dependency("modB", ">=1.0")
                .build();
            graph.addMod(modA, "1.0");

            // 应该不抛出异常，modB 被视为外部提供者
            var layers = graph.getLayers();
            assertFalse(layers.isEmpty(), "Should still produce layers");
            // modA depends on modB (external), so modA is in layer 1
            assertEquals(2, layers.size(), "Should have 2 layers: external dep + modA");
            assertTrue(layers.get(1).contains("modA"), "modA should be in layer 1");

            System.out.println("  Missing dep test - layers: " + layers.size()
                + ", total nodes: " + graph.getTotalNodeCount()
                + ", mods: " + graph.getModCount());
        }

        @Test
        @DisplayName("获取不存在的 mod 的 manifest 返回 null")
        void getManifestForNonExistentMod() {
            assertNull(graph.getManifest("nonexistent"));
        }

        @Test
        @DisplayName("空图调用 estimateSequentialLoadTime 返回 0")
        void emptyGraphLoadTimeZero() {
            assertEquals(0, graph.estimateSequentialLoadTime());
            assertEquals(0, graph.estimateParallelLoadTime());
            // getSpeedupRatio returns 1.0 when parallel time is 0 (p > 0 ? s/p : 1.0)
            assertEquals(1.0, graph.getSpeedupRatio(), 0.001);
        }

        @Test
        @DisplayName("添加外部提供者后图状态正确")
        void externalProviderCount() {
            graph.addExternalProvider("minecraft");
            graph.addExternalProvider("fabric-api");
            assertEquals(2, graph.getTotalNodeCount());
            assertEquals(0, graph.getModCount());
            // 外部提供者也是节点，所以 getLayers() 不为空（它们都进入 layer 0）
            assertEquals(1, graph.getLayers().size(), "External providers form 1 layer");
            assertTrue(graph.getLayers().get(0).contains("minecraft"));
            assertTrue(graph.getLayers().get(0).contains("fabric-api"));
        }

        @Test
        @DisplayName("重复添加相同外部提供者不增加计数")
        void duplicateExternalProvider() {
            graph.addExternalProvider("minecraft");
            graph.addExternalProvider("minecraft"); // 重复
            assertEquals(1, graph.getTotalNodeCount(),
                "Duplicate external provider should not increase count");
        }

        @Test
        @DisplayName("双层多叉树 DAG (各层空集安全检查)")
        void multiBranchDagWithEmptyLayerGuard() {
            // 创建一个图，其中一层在中间为空（理论上不应发生）
            graph.addMod(makeMod("root", Map.of()), "1.0");
            graph.addMod(makeMod("leaf1", Map.of("root", "1.0")), "1.0");
            graph.addMod(makeMod("leaf2", Map.of("root", "1.0")), "1.0");
            graph.addMod(makeMod("leaf3", Map.of("root", "1.0")), "1.0");

            var layers = graph.getLayers();
            assertEquals(2, layers.size(), "Root+leaves = 2 layers");
            assertFalse(layers.get(0).isEmpty(), "Layer 0 should not be empty");
            assertFalse(layers.get(1).isEmpty(), "Layer 1 should not be empty");
        }

        @Test
        @DisplayName("5000 个独立 mod 快速解析 (性能边界)")
        void bulkIndependentMods() {
            for (int i = 0; i < 5000; i++) {
                graph.addMod(makeMod("bulk" + i, Map.of()), "1.0");
            }
            long start = System.nanoTime();
            var layers = graph.getLayers();
            long elapsed = System.nanoTime() - start;

            assertEquals(1, layers.size());
            assertEquals(5000, layers.get(0).size());
            assertTrue(elapsed < 2_000_000_000L,
                "5000 mods should resolve in < 2s, took " + (elapsed / 1_000_000) + "ms");
            System.out.println("  5000 bulk mods resolved in " + (elapsed / 1_000_000) + "ms");
        }

        private ModManifest makeMod(String id, Map<String, String> deps) {
            var b = ModManifest.builder(id, "1.0").mainClass("ex." + id);
            deps.forEach(b::dependency);
            return b.build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  5. ParallelModClassLoader 生命周期边界
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ParallelModClassLoader 生命周期边界")
    class ParallelModClassLoaderEdgeCases {

        private Path tempDir;

        @BeforeEach
        void setUp() throws IOException {
            tempDir = Files.createTempDirectory("francium_edge_");
        }

        @AfterEach
        void tearDown() throws IOException {
            deleteDir(tempDir.toFile());
        }

        @Test
        @DisplayName("loadAll() 调用两次抛出 IllegalStateException")
        void doubleLoadAllThrows() throws Exception {
            ModGraph graph = new ModGraph();
            // Use empty mainClass so class loading is skipped (library-type mod)
            var manifest = ModManifest.builder("single", "1.0")
                .mainClass("")
                .build();
            graph.addMod(manifest, "1.0");
            createMinimalJar("single", "1.0", Map.of());

            var loader = new ParallelModClassLoader(graph, tempDir);
            try {
                loader.discoverMods();
                loader.loadAll(); // 第一次调用

                assertThrows(IllegalStateException.class,
                    () -> loader.loadAll(),  // 第二次调用
                    "loadAll() twice should throw IllegalStateException");
            } finally {
                loader.shutdown();
            }
        }

        @Test
        @DisplayName("shutdown() 在 load 前调用不抛出异常")
        void shutdownBeforeLoad() {
            ModGraph graph = new ModGraph();
            var loader = new ParallelModClassLoader(graph, tempDir);
            // 不加载，直接关闭
            loader.shutdown();
            // 两次关闭
            loader.shutdown();
            // 应优雅完成，无异常
        }

        @Test
        @DisplayName("shutdown() 后调用 getStatus 不崩溃")
        void getStatusAfterShutdown() {
            ModGraph graph = new ModGraph();
            var loader = new ParallelModClassLoader(graph, tempDir);
            loader.shutdown();
            // 关闭后仍可查询状态
            assertEquals(LoadStatus.PENDING, loader.getStatus("nonexistent"));
        }

        @Test
        @DisplayName("getStatus 查询不存在的 mod 返回 PENDING")
        void getStatusNonExistentMod() {
            ModGraph graph = new ModGraph();
            var loader = new ParallelModClassLoader(graph, tempDir);
            try {
                assertEquals(LoadStatus.PENDING, loader.getStatus("i_do_not_exist"));
            } finally {
                loader.shutdown();
            }
        }

        @Test
        @DisplayName("discoverMods 在空目录中返回 0 结果")
        void discoverModsEmptyDir() throws Exception {
            ModGraph graph = new ModGraph();
            var loader = new ParallelModClassLoader(graph, tempDir);
            try {
                var result = loader.discoverMods();
                assertNotNull(result);
                assertEquals(0, result.found.size(), "No mods found in empty dir");
                assertEquals(0, result.totalJars, "No jars in empty dir");
            } finally {
                loader.shutdown();
            }
        }

        @Test
        @DisplayName("discoverMods 目录不存在时抛出 IOException")
        void discoverModsNonExistentDir() {
            Path nonExistent = tempDir.resolve("does_not_exist");
            ModGraph graph = new ModGraph();
            var loader = new ParallelModClassLoader(graph, nonExistent);
            try {
                assertThrows(IOException.class,
                    () -> loader.discoverMods(),
                    "Non-existent directory should throw IOException");
            } finally {
                loader.shutdown();
            }
        }

        @Test
        @DisplayName("discoverMods 只识别 JAR 文件,忽略其他格式")
        void discoverModsSkipsNonJarFiles() throws Exception {
            // 创建非 JAR 文件
            Files.writeString(tempDir.resolve("readme.txt"), "hello");
            Files.writeString(tempDir.resolve("config.json"), "{}");
            Files.writeString(tempDir.resolve("mod.jar"), "not a real jar"); // 伪 JAR

            ModGraph graph = new ModGraph();
            var loader = new ParallelModClassLoader(graph, tempDir);
            try {
                var result = loader.discoverMods();
                // 3 个文件都符合 .jar 后缀但打不开
                assertEquals(1, result.totalJars, "Only mod.jar matches .jar filter");
                // 应该都跳过（不是真实 JAR 或没有 manifest）
                assertEquals(1, result.skipped.size(), "Only mod.jar is scanned and skipped");
                assertEquals(0, result.found.size(), "No valid mod manifest found");
            } finally {
                loader.shutdown();
            }
        }

        @Test
        @DisplayName("getLoadTimes 在 load 前返回空 map")
        void getLoadTimesBeforeLoad() {
            ModGraph graph = new ModGraph();
            var loader = new ParallelModClassLoader(graph, tempDir);
            try {
                var times = loader.getLoadTimes();
                assertNotNull(times);
                assertTrue(times.isEmpty(), "Before load, load times should be empty");
            } finally {
                loader.shutdown();
            }
        }

        // ─── 辅助 ───

        private void createMinimalJar(String modId, String version,
                                       Map<String, String> deps) throws IOException {
            String jarName = modId + "-" + version + ".jar";
            try (JarOutputStream jos = new JarOutputStream(
                    new FileOutputStream(tempDir.resolve(jarName).toFile()))) {
                StringBuilder json = new StringBuilder();
                json.append("{\"modId\":\"").append(modId).append("\",");
                json.append("\"version\":\"").append(version).append("\",");
                json.append("\"mainClass\":\"ex.").append(modId).append("\",");
                json.append("\"dependencies\":{");
                int i = 0;
                for (var e : deps.entrySet()) {
                    if (i++ > 0) json.append(",");
                    json.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
                }
                json.append("}}");
                jos.putNextEntry(new ZipEntry("francium-mod.json"));
                jos.write(json.toString().getBytes());
                jos.closeEntry();
            }
        }

        private ModManifest makeManifest(String id, String version,
                                          Map<String, String> deps) {
            var b = ModManifest.builder(id, version).mainClass("ex." + id);
            deps.forEach(b::dependency);
            return b.build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  6. LoaderConfig 配置边界
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("LoaderConfig 配置解析边界")
    class LoaderConfigEdgeCases {

        @Test
        @DisplayName("不存在的配置文件返回默认值")
        void missingConfigReturnsDefault() {
            Path fakePath = Path.of("/nonexistent/francium.conf");
            var config = com.francium.loader.LoaderConfig.load(fakePath);
            assertNotNull(config);
            assertTrue(config.aiBridgeEnabled);
            assertEquals(0.85f, config.aiConfidenceThreshold, 0.001);
        }

        @Test
        @DisplayName("空配置文件保持默认值")
        void emptyConfigKeepsDefaults() throws Exception {
            Path tempConfig = Files.createTempFile("francium", ".conf");
            try {
                var config = com.francium.loader.LoaderConfig.load(tempConfig);
                assertTrue(config.aiBridgeEnabled);
                assertFalse(config.aggressiveGC);
                assertEquals("INFO", config.logLevel);
            } finally {
                Files.deleteIfExists(tempConfig);
            }
        }

        @Test
        @DisplayName("错误配置行被优雅忽略")
        void badConfigLinesIgnored() throws Exception {
            Path tempConfig = Files.createTempFile("francium", ".conf");
            try {
                Files.writeString(tempConfig,
                    "invalid line without equals\n" +
                    "# comment\n" +
                    "=onlyvalue\n" +
                    "key=\n" +
                    "maxParallelMods=4\n");
                var config = com.francium.loader.LoaderConfig.load(tempConfig);
                assertEquals(4, config.maxParallelMods,
                    "Valid line should be parsed despite bad lines");
            } finally {
                Files.deleteIfExists(tempConfig);
            }
        }

        @Test
        @DisplayName("配置文件含各种值类型")
        void configVariousTypes() throws Exception {
            Path tempConfig = Files.createTempFile("francium", ".conf");
            try {
                Files.writeString(tempConfig,
                    "maxParallelMods=8\n" +
                    "aiBridgeEnabled=false\n" +
                    "aiConfidenceThreshold=0.95\n" +
                    "memoryWarningThresholdMB=1024\n" +
                    "logLevel=DEBUG\n" +
                    "verboseLogging=true\n" +
                    "serverSyncUrl=\"https://example.com/sync\"\n");
                var config = com.francium.loader.LoaderConfig.load(tempConfig);
                assertEquals(8, config.maxParallelMods);
                assertFalse(config.aiBridgeEnabled);
                assertEquals(0.95f, config.aiConfidenceThreshold, 0.001);
                assertEquals(1024, config.memoryWarningThresholdMB);
                assertEquals("DEBUG", config.logLevel);
                assertTrue(config.verboseLogging);
                assertEquals("https://example.com/sync", config.serverSyncUrl);
            } finally {
                Files.deleteIfExists(tempConfig);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  7. Builder 与边界值
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Builder 与 API 边界")
    class BuilderAndApiEdgeCases {

        @Test
        @DisplayName("Builder 链式调用构建复杂 mod")
        void builderChainAllFields() {
            var manifest = ModManifest.builder("fullmod", "2.0.0")
                .name("Full Featured Mod")
                .description("A mod with all fields populated")
                .mainClass("com.example.FullMod")
                .authors(List.of("Alice", "Bob"))
                .dependency("lib1", ">=1.0")
                .dependency("lib2", "~2.0")
                .optionalDependency("opt1", ">=0.5")
                .conflict("badmod", "Incompatible license")
                .mcVersionRange("1.20", "1.21")
                .mixinConfig("mixins.fullmod.json")
                .aiBridge(true)
                .loadPriority(5)
                .sizeBytes(1024 * 1024)
                .entryPoint("postLaunch")
                .build();

            assertEquals("fullmod", manifest.modId());
            assertEquals("2.0.0", manifest.version());
            assertEquals("Full Featured Mod", manifest.name());
            assertEquals("com.example.FullMod", manifest.mainClass());
            assertEquals(2, manifest.authors().size());
            assertEquals(2, manifest.dependencies().size());
            assertEquals(1, manifest.optionalDependencies().size());
            assertEquals(1, manifest.conflicts().size());
            assertEquals("1.20", manifest.mcVersionMin());
            assertEquals("1.21", manifest.mcVersionMax());
            assertEquals(1, manifest.mixinConfigs().size());
            assertTrue(manifest.aiBridgeEnabled());
            assertEquals(5, manifest.loadPriority());
            assertEquals(1024 * 1024, manifest.sizeBytes());
            assertEquals("postLaunch", manifest.entryPointType());
        }

        @Test
        @DisplayName("两个相同 modId+version 的 manifest equals 为 true")
        void manifestEqualsByIdAndVersion() {
            var m1 = ModManifest.builder("same", "1.0").mainClass("ex.A").build();
            var m2 = ModManifest.builder("same", "1.0").mainClass("ex.B").build(); // 不同 mainClass
            assertEquals(m1, m2, "Equals should be based on modId+version only");
            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        @DisplayName("不同 version 的 manifest equals 为 false")
        void manifestDifferentVersionNotEqual() {
            var m1 = ModManifest.builder("mod", "1.0").build();
            var m2 = ModManifest.builder("mod", "2.0").build();
            assertNotEquals(m1, m2);
        }

        @Test
        @DisplayName("toString 格式为 modId@version")
        void manifestToStringFormat() {
            var m = ModManifest.builder("displaymod", "3.1.4").build();
            assertEquals("displaymod@3.1.4", m.toString());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════════════════════

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
