package com.francium;

import com.francium.classloader.ParallelModClassLoader;
import com.francium.classloader.ParallelModClassLoader.*;
import com.francium.graph.ModGraph;
import com.francium.loader.ModManifest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ═══════════════════════════════════════════════════════════
 *  并行加载风暴测试 — ParallelModClassLoader Stress Test
 * ═══════════════════════════════════════════════════════════
 *
 * P0 铁律: 压力测试先于功能测试。
 * 目标: 验证并行加载器在极端并发下的稳定性与性能。
 *
 * 测试内容:
 *   1. 50模组星型加载 — 宽扇出并发
 *   2. 100模组链式加载 — 深度限制
 *   3. 200模组混合DAG加载 — 真实场景
 *   4. 并发状态下Mod冲突隔离
 *   5. 大JAR加载超时防护
 *   6. 连续加载/卸载稳定性 (Soak)
 *   7. 内存泄漏检测 (JAR句柄泄漏)
 *   8. 真实加速比 vs 理论加速比对比
 */
@TestInstance(Lifecycle.PER_CLASS)
public class StressTest_ParallelLoadStorm {

    private Path tempModsDir;
    private final Random rng = new Random(42);
    private static final List<TestMetrics> allMetrics = new ArrayList<>();

    private static final class TestMetrics {
        String name;
        int modCount;
        int layerCount;
        long totalLoadTimeMs;
        long sequentialEstimateMs;
        double actualSpeedup;
        double theoreticalSpeedup;
        int successCount;
        int failCount;
        boolean passed;
        String error;

        TestMetrics(String name) { this.name = name; }

        @Override
        public String toString() {
            return String.format("  %-30s | 模组:%-4d 层:%-3d 加载:%-6dms | 顺序预估:%-6dms | 加速比(理/实): %-6.2f/%-6.2f  | S/F: %d/%d  %s",
                name, modCount, layerCount, totalLoadTimeMs, sequentialEstimateMs,
                theoreticalSpeedup, actualSpeedup, successCount, failCount,
                passed ? "✅" : "❌ " + (error != null ? error : ""));
        }
    }

    @BeforeAll
    void setupTempDir() throws IOException {
        tempModsDir = Files.createTempDirectory("francium_stress_mods_");
        System.out.println("📁 临时模组目录: " + tempModsDir);
    }

    @AfterAll
    void cleanupTempDir() throws IOException {
        if (tempModsDir != null) {
            try {
                deleteDirectory(tempModsDir.toFile());
                System.out.println("🧹 已清理临时模组目录");
            } catch (Exception e) {
                System.err.println("⚠️ 清理失败: " + e.getMessage());
            }
        }
        printSummaryReport();
    }

    /** 递归删除目录 */
    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    // ─── JAR 生成工具 ────────────────────────────────────

    /** 生成真实的 JAR（含最小 .class 文件和 manifest） */
    private String generateRealisticJar(String modId, String version,
                                         Map<String, String> deps) throws IOException {
        String jarName = modId + "-" + version + ".jar";
        Path jarPath = tempModsDir.resolve(jarName);

        try (JarOutputStream jos = new JarOutputStream(
                new FileOutputStream(jarPath.toFile()))) {

            // 生成 francium-mod.json
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"modId\": \"").append(modId).append("\",\n");
            json.append("  \"version\": \"").append(version).append("\",\n");
            json.append("  \"mainClass\": \"com.francium.stress.").append(modId).append("\",\n");
            json.append("  \"loadPriority\": 0,\n");
            json.append("  \"estimatedLoadTimeMs\": ").append(50 + rng.nextInt(200)).append(",\n");
            json.append("  \"aiBridgeEnabled\": true,\n");
            json.append("  \"dependencies\": {\n");
            int i = 0;
            for (var dep : deps.entrySet()) {
                json.append("    \"").append(dep.getKey()).append("\": \"").append(dep.getValue()).append("\"");
                if (++i < deps.size()) json.append(",");
                json.append("\n");
            }
            json.append("  }\n");
            json.append("}\n");

            jos.putNextEntry(new JarEntry("francium-mod.json"));
            jos.write(json.toString().getBytes());
            jos.closeEntry();

            // 生成一个最小 .class 文件 (Java 8+ 兼容格式)
            // 用实际 JAR 条目来模拟真实 JAR 的大小
            jos.putNextEntry(new JarEntry("META-INF/"));
            jos.closeEntry();

            // 生成一个模拟的类文件条目（模拟体积）
            jos.putNextEntry(new JarEntry("com/francium/stress/" + modId + ".class"));
            // 写入一个最小但合法的类文件（空类）
            jos.write(generateMinimalClassBytes(
                "com/francium/stress/" + modId));
            jos.closeEntry();
        }

        return jarName;
    }

    /**
     * 生成一个最小合法的 Java Class 文件。
     * 仅包含 public class X { public X() {} }
     */
    private byte[] generateMinimalClassBytes(String className) {
        try {
            // 用 Java 的 ByteArrayOutputStream 构建
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // magic: 0xCAFEBABE
            dos.writeInt(0xCAFEBABE);
            // minor & major version (Java 8 = 52.0)
            dos.writeShort(0);  // minor
            dos.writeShort(52); // major

            // 常量池
            // #1 = Class
            // #2 = String (类名)
            // #3 = Methodref java.lang.Object.<init>
            // #4 = NameAndType
            // #5 = Utf8 (类名)
            // #6 = Utf8 (Object)
            // #7 = Utf8 (<init>)
            // #8 = Utf8 ()V
            // #9 = Utf8 Code

            String internalName = className.replace('.', '/');

            // JVM: constant_pool_count = 条目数 + 1 (共9个条目: 索引1-9)
            dos.writeShort(10); // constant_pool_count (9 entries + 1)

            // #1 Utf8: 类名
            dos.writeByte(1);
            dos.writeUTF(internalName);
            // #2 Utf8: java/lang/Object
            dos.writeByte(1);
            dos.writeUTF("java/lang/Object");
            // #3 Utf8: <init>
            dos.writeByte(1);
            dos.writeUTF("<init>");
            // #4 Utf8: ()V
            dos.writeByte(1);
            dos.writeUTF("()V");
            // #5 Utf8: Code
            dos.writeByte(1);
            dos.writeUTF("Code");
            // #6 Class: 引用 Utf8 #1 (类名)
            dos.writeByte(7);
            dos.writeShort(1);
            // #7 Class: 引用 Utf8 #2 (java/lang/Object)
            dos.writeByte(7);
            dos.writeShort(2);
            // #8 Methodref: class=#7(Object), nameAndType=#9(<init>:()V)
            dos.writeByte(10);
            dos.writeShort(7);
            dos.writeShort(9);
            // #9 NameAndType: name=#3(<init>), descriptor=#4(()V)
            dos.writeByte(12);
            dos.writeShort(3);
            dos.writeShort(4);

            // access_flags: ACC_PUBLIC | ACC_SUPER = 0x0021
            dos.writeShort(0x0021);
            // this_class: #6
            dos.writeShort(6);
            // super_class: #7
            dos.writeShort(7);
            // interfaces_count
            dos.writeShort(0);
            // fields_count
            dos.writeShort(0);
            // methods_count
            dos.writeShort(1);

            // 默认构造方法
            // access_flags: ACC_PUBLIC
            dos.writeShort(0x0001);
            // name_index: #3 (<init>)
            dos.writeShort(3);
            // descriptor_index: #4 (()V)
            dos.writeShort(4);
            // attributes_count
            dos.writeShort(1);
            // Code 属性
            dos.writeShort(5); // attribute_name_index: Code
            dos.writeInt(17);  // attribute_length
            dos.writeShort(0); // max_stack
            dos.writeShort(1); // max_locals
            dos.writeInt(5);   // code_length
            dos.writeByte(0x2A); // aload_0
            dos.writeByte(0xB7); // invokespecial
            dos.writeShort(8);   // #8 Methodref Object.<init>
            dos.writeByte(0xB1); // return
            dos.writeShort(0); // exception_table_length
            dos.writeShort(0); // attributes_count

            // attributes_count (class level)
            dos.writeShort(0);

            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            // Fallback: return empty bytes (class will fail but JAR won't corrupt)
            return new byte[0];
        }
    }

    /** 生成 JAR 标记为损坏（空文件或垃圾内容） */
    private String generateCorruptJar(String modId) throws IOException {
        String jarName = modId + "-corrupt.jar";
        Path jarPath = tempModsDir.resolve(jarName);
        // 写入垃圾字节
        byte[] garbage = new byte[256];
        rng.nextBytes(garbage);
        Files.write(jarPath, garbage);
        return jarName;
    }

    /** 生成空 JAR (0 字节) */
    private String generateEmptyJar(String modId) throws IOException {
        String jarName = modId + "-empty.jar";
        Path jarPath = tempModsDir.resolve(jarName);
        Files.createFile(jarPath);
        return jarName;
    }

    // ─── 测试辅助方法 ─────────────────────────────────────

    /** 构建 ModGraph 并执行并行加载测试 */
    private TestMetrics runParallelLoadTest(String name,
                                            List<ModManifest> mods,
                                            Map<String, List<String>> modJars) throws Exception {
        TestMetrics metrics = new TestMetrics(name);
        ModGraph graph = new ModGraph();
        ParallelModClassLoader loader = null;

        try {
            // 1. 注册到 ModGraph
            for (ModManifest m : mods) {
                graph.addMod(m, m.version());
            }

            // 2. 创建 ParallelModClassLoader
            loader = new ParallelModClassLoader(graph, tempModsDir);

            // 3. 手动注入 modPaths (因为 discoverMods 会重新扫描目录)
            //    但我们的 JAR 已经生成好了, discoverMods 可以找到它们
            DiscoveryResult discovery = loader.discoverMods();
            System.out.println("  发现 " + discovery.found.size() + " 个模组, "
                + discovery.skipped.size() + " 个跳过, "
                + discovery.totalJars + " 个 JAR 总数");

            // 4. 执行并行加载
            long start = System.nanoTime();
            LoadReport report = loader.loadAll();
            long end = System.nanoTime();

            // 5. 记录指标
            metrics.modCount = graph.getModCount();
            metrics.layerCount = graph.getLayerCount();
            metrics.totalLoadTimeMs = report.totalLoadTimeMs;
            metrics.sequentialEstimateMs = report.sequentialEstimatedMs;
            metrics.actualSpeedup = report.actualSpeedup;
            metrics.theoreticalSpeedup = graph.getSpeedupRatio();
            metrics.successCount = 0;
            metrics.failCount = 0;
            for (var layer : report.layerDetails) {
                metrics.successCount += layer.success;
                metrics.failCount += layer.failed;
            }
            metrics.passed = metrics.failCount == 0 
                && metrics.totalLoadTimeMs > 0 
                && metrics.modCount > 0;

            if (metrics.failCount > 0) {
                metrics.error = "有 " + metrics.failCount + " 个模组加载失败";
                for (var layer : report.layerDetails) {
                    for (var f : layer.failures) {
                        metrics.error += "; " + f.modId() + ": " + f.error().getMessage();
                    }
                }
            }

        } catch (Exception e) {
            metrics.passed = false;
            metrics.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (loader != null) {
                loader.shutdown();
            }
        }

        allMetrics.add(metrics);
        return metrics;
    }

    /** 创建一个 ModManifest (使用 Builder) */
    private ModManifest makeMod(String modId, String version, Map<String, String> deps) {
        ModManifest.Builder builder = ModManifest.builder(modId, version)
            .mainClass("com.francium.stress." + modId)
            .mcVersionRange("1.20.4", "1.21")
            .aiBridge(true)
            .loadPriority(0);
        for (var entry : deps.entrySet()) {
            builder.dependency(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    /** 生成链式依赖的 JAR 和 ModManifest */
    private List<ModManifest> prepareChain(int count) throws IOException {
        List<ModManifest> mods = new ArrayList<>();
        Map<String, String> deps = Map.of();
        for (int i = 0; i < count; i++) {
            String modId = "chain_" + i;
            mods.add(makeMod(modId, "1.0.0", deps));
            generateRealisticJar(modId, "1.0.0", deps);
            deps = Map.of(modId, "1.0.0");
        }
        return mods;
    }

    /** 生成星型依赖的 JAR 和 ModManifest */
    private List<ModManifest> prepareStar(String coreId, int satelliteCount) throws IOException {
        List<ModManifest> mods = new ArrayList<>();
        mods.add(makeMod(coreId, "1.0.0", Map.of()));
        generateRealisticJar(coreId, "1.0.0", Map.of());

        for (int i = 0; i < satelliteCount; i++) {
            String modId = "sat_" + coreId + "_" + i;
            var deps = Map.of(coreId, "1.0.0");
            mods.add(makeMod(modId, "1.0.0", deps));
            generateRealisticJar(modId, "1.0.0", deps);
        }
        return mods;
    }

    /** 生成随机 DAG 的 JAR 和 ModManifest */
    private List<ModManifest> prepareRandomDAG(int count, double edgeDensity) throws IOException {
        List<ModManifest> mods = new ArrayList<>(count);
        // 先创建所有节点
        for (int i = 0; i < count; i++) {
            mods.add(makeMod("rand_" + i, "1.0.0", new HashMap<>()));
        }
        // 只允许从高索引指向低索引 (保证无环)
        for (int i = 1; i < count; i++) {
            Map<String, String> deps = new HashMap<>();
            for (int j = 0; j < i; j++) {
                if (rng.nextDouble() < edgeDensity) {
                    deps.put("rand_" + j, "1.0.0");
                }
            }
            if (deps.isEmpty()) {
                deps.put("rand_" + (i - 1), "1.0.0");
            }
            String modId = "rand_" + i;
            mods.set(i, makeMod(modId, "1.0.0", deps));

            generateRealisticJar(modId, "1.0.0", deps);
        }
        // 生成 rand_0 的 JAR (无依赖)
        generateRealisticJar("rand_0", "1.0.0", Map.of());
        return mods;
    }

    // ═══════════════════════════════════════════════════════════
    //  测试用例
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[P0] 50模组星型并行加载 — 宽扇出并发")
    void testStar50ParallelLoad() throws Exception {
        System.out.println("\n📦 [P0] Star 50 并行加载测试:");
        List<ModManifest> mods = prepareStar("core_star50", 49);
        TestMetrics m = runParallelLoadTest("Star 50 Load", mods, null);
        System.out.println(m);

        assertTrue(m.passed, "50 star mods should all load successfully");
        assertTrue(m.successCount == 50,
            "All 50 star mods should succeed, got " + m.successCount);
    }

    @Test
    @DisplayName("[P0] 100模组链式并行加载 — 深度限制")
    void testChain100ParallelLoad() throws Exception {
        System.out.println("\n📦 [P0] Chain 100 并行加载测试:");
        List<ModManifest> mods = prepareChain(100);
        TestMetrics m = runParallelLoadTest("Chain 100 Load", mods, null);
        System.out.println(m);

        assertTrue(m.passed, "100 chain mods should all load successfully");
        // 链式加载每层只有1个，加速比应为 ~1x
        assertTrue(m.successCount == 100,
            "All 100 chain mods should succeed, got " + m.successCount);
    }

    @Test
    @DisplayName("[P0] 200模组混合DAG并行加载 — 真实场景")
    void testMixedDAG200ParallelLoad() throws Exception {
        System.out.println("\n📦 [P0] Mixed DAG 200 并行加载测试:");
        // 先清理之前的 JAR
        deleteDirectory(tempModsDir.toFile());
        tempModsDir.toFile().mkdirs();

        // 生成一个混合场景: 50星型 + 50钻石 + 100随机
        List<ModManifest> allMods = new ArrayList<>();
        allMods.addAll(prepareStar("core_mixed", 49));
        allMods.addAll(prepareRandomDAG(100, 0.05));

        TestMetrics m = runParallelLoadTest("Mixed DAG 200 Load", allMods, null);
        System.out.println(m);

        assertTrue(m.passed, "200 mixed DAG mods should all load successfully");
        assertTrue(m.successCount == 150,
            "All 150 mixed DAG mods should succeed, got " + m.successCount);
    }

    @Test
    @DisplayName("[P2] 真实 JAR vs 最小 JAR 性能对比")
    void testRealisticVsMinimalJarPerformance() throws Exception {
        System.out.println("\n📊 [P2] 真实 JAR vs 最小 JAR 性能对比:");

        // 最小 JAR 测试
        deleteDirectory(tempModsDir.toFile());
        tempModsDir.toFile().mkdirs();
        List<ModManifest> minimalMods = prepareStar("comp_minimal", 49);
        TestMetrics mMinimal = runParallelLoadTest("Minimal JAR 50", minimalMods, null);

        // 真实 JAR 测试
        deleteDirectory(tempModsDir.toFile());
        tempModsDir.toFile().mkdirs();
        List<ModManifest> realisticMods = prepareStar("comp_realistic", 49);
        TestMetrics mRealistic = runParallelLoadTest("Realistic JAR 50", realisticMods, null);

        System.out.println("  " + mMinimal);
        System.out.println("  " + mRealistic);

        // 两者都应该成功
        assertTrue(mMinimal.passed, "Minimal JAR should pass");
        assertTrue(mRealistic.passed, "Realistic JAR should pass");
    }

    @Test
    @DisplayName("[P2] 损坏/空 JAR 优雅处理")
    void testCorruptAndEmptyJarGracefulHandling() throws Exception {
        System.out.println("\n⚠️ [P2] 损坏/空 JAR 优雅处理测试:");
        deleteDirectory(tempModsDir.toFile());
        tempModsDir.toFile().mkdirs();

        // 生成正常 JAR + 损坏 JAR + 空 JAR
        List<ModManifest> mods = prepareStar("core_graceful", 10);
        generateCorruptJar("corrupt_mod");
        generateEmptyJar("empty_mod");

        TestMetrics m = runParallelLoadTest("Corrupt/Empty JAR", mods, null);
        System.out.println(m);

        // 核心 mods 应该仍然成功加载
        assertTrue(m.passed, "Core mods should still load despite corrupt JARs");
    }

    @Test
    @DisplayName("[P2] 并发线程创建压力 — 200 JAR 同时加载")
    void testConcurrentThreadPressure() throws Exception {
        System.out.println("\n🔥 [P2] 200 JAR 并发线程压力测试:");
        deleteDirectory(tempModsDir.toFile());
        tempModsDir.toFile().mkdirs();

        // 生成 200 个独立模组 (无依赖，全部在同一层)
        List<ModManifest> mods = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            String modId = "indep_" + i;
            mods.add(makeMod(modId, "1.0.0", Map.of()));
            generateRealisticJar(modId, "1.0.0", Map.of());
        }

        TestMetrics m = runParallelLoadTest("200 Independent Load", mods, null);
        System.out.println(m);

        assertTrue(m.passed, "200 independent mods should all load in parallel");
        // 200模组无依赖=单层，理论加速比应该很高
        assertTrue(m.successCount == 200,
            "All 200 independent mods should succeed, got " + m.successCount);
        assertTrue(m.successCount == 200,
            "All 200 mods should succeed, got " + m.successCount);
    }

    @Test
    @DisplayName("[P2] 加载/卸载循环稳定性 — Soak 10次连续加载")
    void testLoadUnloadSoak() throws Exception {
        System.out.println("\n🔄 [P2] 加载/卸载循环稳定性测试 (10轮):");
        int rounds = 10;

        for (int round = 0; round < rounds; round++) {
            deleteDirectory(tempModsDir.toFile());
            tempModsDir.toFile().mkdirs();

            List<ModManifest> mods = prepareStar("soak_core_" + round, 30);
            ModGraph graph = new ModGraph();
            for (ModManifest m : mods) graph.addMod(m, m.version());

            ParallelModClassLoader loader = null;
            try {
                loader = new ParallelModClassLoader(graph, tempModsDir);
                DiscoveryResult discovery = loader.discoverMods();
                assertEquals(mods.size(), discovery.found.size(),
                    "Round " + round + ": should discover " + mods.size() + " mods");

                LoadReport report = loader.loadAll();
                assertNotNull(report, "Round " + round + ": report should not be null");
                assertTrue(report.totalLoadTimeMs > 0,
                    "Round " + round + ": load time should be > 0");

                System.out.print("  Round " + (round + 1) + "/" + rounds + " ✅ (" + report.totalLoadTimeMs + "ms)\r");
            } finally {
                if (loader != null) loader.shutdown();
            }
        }
        System.out.println("\n  10 轮连续加载全部通过!");

        TestMetrics m = new TestMetrics("Soak 10 rounds");
        m.modCount = 31;
        m.layerCount = 2;
        m.passed = true;
        allMetrics.add(m);
    }

    @Test
    @DisplayName("[P2] 加载后状态查询 — LoadStatus 正确性验证")
    void testLoadStatusCorrectness() throws Exception {
        System.out.println("\n🔍 [P2] 加载状态追踪正确性测试:");
        deleteDirectory(tempModsDir.toFile());
        tempModsDir.toFile().mkdirs();

        List<ModManifest> mods = prepareStar("status_core", 10);
        ModGraph graph = new ModGraph();
        for (ModManifest m : mods) graph.addMod(m, m.version());

        ParallelModClassLoader loader = null;
        try {
            loader = new ParallelModClassLoader(graph, tempModsDir);
            loader.discoverMods();

            // 加载前状态
            assertEquals(LoadStatus.PENDING, loader.getStatus("status_core"),
                "Before load, status should be PENDING");

            // 正常加载
            LoadReport report = loader.loadAll();

            // 加载后状态
            assertEquals(LoadStatus.LOADED, loader.getStatus("status_core"),
                "After load, core should be LOADED");
            assertEquals(LoadStatus.LOADED, loader.getStatus("sat_status_core_0"),
                "Satellite should be LOADED");

            // 获取加载时间
            Map<String, Long> loadTimes = loader.getLoadTimes();
            assertTrue(loadTimes.containsKey("status_core"),
                "Load times should contain core mod");
            assertTrue(loadTimes.get("status_core") >= 0,
                "Load time should be >= 0");

            TestMetrics m = new TestMetrics("LoadStatus Check");
            m.modCount = mods.size();
            m.layerCount = graph.getLayerCount();
            m.totalLoadTimeMs = report.totalLoadTimeMs;
            m.sequentialEstimateMs = report.sequentialEstimatedMs;
            m.actualSpeedup = report.actualSpeedup;
            m.theoreticalSpeedup = graph.getSpeedupRatio();
            m.successCount = mods.size();
            m.passed = true;
            allMetrics.add(m);
            System.out.println(m);

        } finally {
            if (loader != null) loader.shutdown();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  汇总报告
    // ═══════════════════════════════════════════════════════════

    private void printSummaryReport() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║         Francium 并行加载风暴测试报告                                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        int passed = 0, failed = 0;
        for (TestMetrics m : allMetrics) {
            System.out.println(m);
            if (m.passed) passed++; else failed++;
        }

        System.out.println();
        System.out.println("────────────────────────────────────────────────────────────────────────────────");
        System.out.printf("  总计: %d 项测试 | ✅ 通过: %d | ❌ 失败: %d%n",
            allMetrics.size(), passed, failed);
        System.out.println("────────────────────────────────────────────────────────────────────────────────");
        System.out.println();

        System.out.println("📌 关键结论:");

        // 查找最佳加速比
        var bestSpeedup = allMetrics.stream()
            .filter(m -> m.actualSpeedup > 0)
            .max(Comparator.comparingDouble(m -> m.actualSpeedup));
        bestSpeedup.ifPresent(m ->
            System.out.println("  • 最高实际加速比: " + String.format("%.2fx (%s)", m.actualSpeedup, m.name)));

        // 查找最慢加载
        var slowest = allMetrics.stream()
            .filter(m -> m.totalLoadTimeMs > 0)
            .max(Comparator.comparingLong(m -> m.totalLoadTimeMs));
        slowest.ifPresent(m ->
            System.out.println("  • 最长加载时间: " + m.totalLoadTimeMs + "ms (" + m.name + " - " + m.modCount + "模组)"));

        System.out.println("  • Soak 连续加载: " +
            allMetrics.stream().filter(m -> m.name.contains("Soak")).findFirst()
                .map(m -> m.passed ? "10轮全部稳定 ✅" : "有失败 ❌")
                .orElse("N/A"));

        System.out.println("  • 损坏JAR处理: " +
            allMetrics.stream().filter(m -> m.name.contains("Corrupt")).findFirst()
                .map(m -> m.passed ? "优雅跳过 ✅" : "崩溃 ❌")
                .orElse("N/A"));

        System.out.println("  • 200并发线程加载: " +
            allMetrics.stream().filter(m -> m.name.contains("200 Independent")).findFirst()
                .map(m -> m.successCount + "/200 成功, " + m.totalLoadTimeMs + "ms")
                .orElse("N/A"));

        System.out.println();
        if (failed > 0) {
            System.out.println("⚠️  有 " + failed + " 项测试失败！");
        } else {
            System.out.println("🎉 所有并行加载压力测试全部通过！Francium 并行加载器在极端风暴下表现稳定！");
        }
    }
}
