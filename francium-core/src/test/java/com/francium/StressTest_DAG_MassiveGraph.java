package com.francium;

import com.francium.graph.ModGraph;
import com.francium.graph.ModGraph.CircularDependencyException;
import com.francium.loader.ModManifest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ═══════════════════════════════════════════════════════════
 *  DAG 大规模压力测试 — ModGraph Stress Test Suite
 * ═══════════════════════════════════════════════════════════
 * 
 * P0 铁律: 压力测试先于功能测试。
 * 目标: 验证 ModGraph 在极端规模下的稳定性、性能与正确性。
 * 
 * 测试内容:
 *   1. 200模组链式依赖 (深度测试)
 *   2. 300模组钻石/星型混合依赖 (广度测试)  
 *   3. 500模组随机依赖 (大规模随机)
 *   4. 1000模组极限规模 (性能基准)
 *   5. 环形依赖检测 (负向测试)
 *   6. 重复模组冲突检测
 *   7. 外部提供者压力
 *   8. 并发添加模组 (线程安全)
 *   9. 加速比可扩展性验证
 *   10. Soak test — 重复构建1000次
 */
@TestInstance(Lifecycle.PER_CLASS)
public class StressTest_DAG_MassiveGraph {

    private ModGraph graph;
    private final Random rng = new Random(42); // 固定种子保证可复现

    private static final class TestMetrics {
        String name;
        int nodeCount;
        int edgeCount;
        int layerCount;
        long buildTimeMs;
        double speedupRatio;
        boolean passed;
        String error;

        TestMetrics(String name) { this.name = name; }

        @Override
        public String toString() {
            return String.format("  %-30s | 节点:%-5d 边:%-6d 层:%-3d 耗时:%-6dms 加速比:%-6.2fx  %s",
                name, nodeCount, edgeCount, layerCount, buildTimeMs, speedupRatio,
                passed ? "✅" : "❌ " + (error != null ? error : ""));
        }
    }

    private static final List<TestMetrics> allMetrics = new ArrayList<>();

    @BeforeEach
    void setUp() {
        graph = new ModGraph();
    }

    // ─── 工具方法 ─────────────────────────────────────────────

    /** 快速创建一个 ModManifest (使用 Builder 模式) */
    private ModManifest makeMod(String modId, String version, Map<String, String> deps) {
        ModManifest.Builder builder = ModManifest.builder(modId, version)
            .mainClass(modId + ".Main")
            .mcVersionRange("1.20.4", "1.21")
            .aiBridge(true)
            .loadPriority(0);
        for (var entry : deps.entrySet()) {
            builder.dependency(entry.getKey(), entry.getValue());
        }
        ModManifest m = builder.build();
        m.setEstimatedLoadTimeMs(50 + rng.nextInt(200));
        return m;
    }

    /** 批量添加模组到图 */
    private void addAll(ModGraph g, List<ModManifest> mods) {
        for (ModManifest m : mods) {
            g.addMod(m, m.version());
        }
    }

    /** 生成链式依赖: mod0 → mod1 → mod2 → ... → modN-1 */
    private List<ModManifest> generateChain(int count) {
        List<ModManifest> mods = new ArrayList<>(count);
        mods.add(makeMod("chain_0", "1.0.0", Map.of()));
        for (int i = 1; i < count; i++) {
            mods.add(makeMod("chain_" + i, "1.0.0", Map.of("chain_" + (i - 1), "1.0.0")));
        }
        return mods;
    }

    /** 生成钻石依赖: 一个根 → N个中间 → 一个叶子 */
    private List<ModManifest> generateDiamond(int width) {
        List<ModManifest> mods = new ArrayList<>();
        mods.add(makeMod("diamond_root", "1.0.0", Map.of()));

        for (int i = 0; i < width; i++) {
            String midId = "diamond_mid_" + i;
            mods.add(makeMod(midId, "1.0.0", Map.of("diamond_root", "1.0.0")));
        }

        Map<String, String> leafDeps = new HashMap<>();
        for (int i = 0; i < width; i++) {
            leafDeps.put("diamond_mid_" + i, "1.0.0");
        }
        mods.add(makeMod("diamond_leaf", "1.0.0", leafDeps));

        return mods;
    }

    /** 生成星型依赖: 一个核心 + N个外围模组依赖该核心 */
    private List<ModManifest> generateStar(String coreId, int satelliteCount) {
        List<ModManifest> mods = new ArrayList<>();
        mods.add(makeMod(coreId, "1.0.0", Map.of()));

        for (int i = 0; i < satelliteCount; i++) {
            mods.add(makeMod("satellite_" + i, "1.0.0", Map.of(coreId, "1.0.0")));
        }
        return mods;
    }

    /** 生成随机依赖图 (保证无环) */
    private List<ModManifest> generateRandomDAG(int count, double edgeDensity) {
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
            // 至少依赖一个已有的节点，保证图连通
            if (deps.isEmpty() && i > 0) {
                deps.put("rand_" + (i - 1), "1.0.0");
            }
            mods.set(i, makeMod("rand_" + i, "1.0.0", deps));
        }
        return mods;
    }

    /** 生成带环的图用于检测 (故意制造环) */
    private List<ModManifest> generateCyclic(int count) {
        List<ModManifest> mods = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String next = "cyclic_" + ((i + 1) % count);
            mods.add(makeMod("cyclic_" + i, "1.0.0", Map.of(next, "1.0.0")));
        }
        return mods;
    }

    /** 执行单次压力测试并记录指标 */
    private TestMetrics runStressTest(String name, Runnable testLogic) {
        TestMetrics metrics = new TestMetrics(name);
        try {
            long start = System.nanoTime();
            testLogic.run();
            long end = System.nanoTime();

            metrics.buildTimeMs = (end - start) / 1_000_000;
            metrics.nodeCount = graph.getTotalNodeCount();
            metrics.edgeCount = graph.getTotalEdgeCount();
            metrics.layerCount = graph.getLayerCount();
            metrics.speedupRatio = graph.getSpeedupRatio();
            metrics.passed = true;
        } catch (Exception e) {
            metrics.passed = false;
            metrics.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        allMetrics.add(metrics);
        return metrics;
    }

    // ═══════════════════════════════════════════════════════════
    //  测试用例
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[P0] 200模组链式依赖 — 深度极限")
    void testChainOf200() {
        TestMetrics m = runStressTest("Chain 200", () -> {
            addAll(graph, generateChain(200));
            graph.buildLayers();
            assertEquals(200, graph.getLayerCount(), "Chain of 200 should have 200 layers");
            assertEquals(199, graph.getTotalEdgeCount());
        });
        System.out.println(m);
    }

    @Test
    @DisplayName("[P0] 300模组钻石依赖 — 宽度极限")  
    void testDiamondOf300() {
        TestMetrics m = runStressTest("Diamond 300", () -> {
            addAll(graph, generateDiamond(298)); // root + 298 mid + 1 leaf = 300
            graph.buildLayers();
            assertEquals(3, graph.getLayerCount(), "Diamond should have 3 layers");
        });
        System.out.println(m);
    }

    @Test
    @DisplayName("[P0] 500模组星型依赖 — 扇出极限")
    void testStarOf500() {
        TestMetrics m = runStressTest("Star 500", () -> {
            addAll(graph, generateStar("core", 499));
            graph.buildLayers();
            assertEquals(2, graph.getLayerCount(), "Star should have 2 layers");
            // 验证加速比: 500 mods, 2 layers, 理想加速比 ~250x
            assertTrue(graph.getSpeedupRatio() > 100, 
                "Star 500 speedup should be > 100x, got " + graph.getSpeedupRatio());
        });
        System.out.println(m);
    }

    @Test
    @DisplayName("[P0] 500模组随机DAG — 大规模随机")
    void testRandomDAG500() {
        TestMetrics m = runStressTest("Random DAG 500", () -> {
            addAll(graph, generateRandomDAG(500, 0.05));
            graph.buildLayers();
            assertTrue(graph.getLayerCount() > 0, "Should have at least 1 layer");
            assertTrue(graph.getLayerCount() < 500, "Layers should be less than nodes");
        });
        System.out.println(m);
    }

    @Test
    @DisplayName("[P0] 1000模组随机DAG — 极限规模+性能基准")
    void testRandomDAG1000() {
        TestMetrics m = runStressTest("Random DAG 1000", () -> {
            addAll(graph, generateRandomDAG(1000, 0.03));
            graph.buildLayers();
            // 验证结果正确性: 总节点数
            assertEquals(1000, graph.getTotalNodeCount(), "Should have 1000 nodes");
        });
        // 构建时间检查必须在 runStressTest 返回后
        assertTrue(m.buildTimeMs < 5000, "Build time should be < 5s for 1000 nodes, got " + m.buildTimeMs + "ms");
        System.out.println(m);
    }

    @Test
    @DisplayName("[P0] 环检测 — 200节点环形依赖")
    void testCycleDetection200() {
        TestMetrics m = runStressTest("Cycle Detection 200", () -> {
            addAll(graph, generateCyclic(200));
            CircularDependencyException ex = assertThrows(
                CircularDependencyException.class,
                () -> graph.buildLayers(),
                "Circular graph should throw"
            );
            assertFalse(ex.cycles().isEmpty(), "Should report at least one cycle");
            System.out.println("  ✅ 检测到环: " + ex.cycles().get(0).size() + " 个节点参与");
        });
        // 手动修正指标 (因为抛异常了)
        m.nodeCount = graph.getTotalNodeCount();
        m.edgeCount = graph.getTotalEdgeCount();
        System.out.println(m);
    }

    @Test
    @DisplayName("[P0] 重复模组冲突检测")
    void testDuplicateModConflict() {
        TestMetrics m = runStressTest("Duplicate Conflict", () -> {
            graph.addMod(makeMod("dup", "1.0.0", Map.of()), "1.0.0");
            assertThrows(ModGraph.ModConflictException.class,
                () -> graph.addMod(makeMod("dup", "2.0.0", Map.of()), "2.0.0"),
                "Version conflict should throw");
            // 同版本添加应返回false但不抛异常
            assertFalse(graph.addMod(makeMod("dup", "1.0.0", Map.of()), "1.0.0"),
                "Same version should return false");
        });
        System.out.println(m);
    }

    @Test
    @DisplayName("[P2] 100个外部提供者压力")
    void testExternalProvidersStress() {
        TestMetrics m = runStressTest("100 Providers", () -> {
            // 添加100个外部提供者
            for (int i = 0; i < 100; i++) {
                graph.addExternalProvider("provider_" + i);
            }
            // 50个模组依赖这些提供者
            List<ModManifest> mods = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                Map<String, String> deps = new HashMap<>();
                deps.put("provider_" + (i % 100), "1.0.0");
                deps.put("provider_" + ((i + 1) % 100), "1.0.0");
                mods.add(makeMod("consumer_" + i, "1.0.0", deps));
            }
            addAll(graph, mods);
            graph.buildLayers();

            assertEquals(150, graph.getTotalNodeCount(), "100 providers + 50 mods = 150 nodes");
            assertTrue(graph.getLayerCount() >= 2, "Should have at least 2 layers");
        });
        System.out.println(m);
    }

    @Test
    @DisplayName("[P2] 并发安全 — 多线程同时添加模组")
    void testConcurrentAddMod() throws Exception {
        TestMetrics m = new TestMetrics("Concurrent Add (100 threads)");
        int threadCount = 100;
        int modsPerThread = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> errors = new ConcurrentLinkedQueue<>();

        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread th = new Thread(() -> {
                for (int i = 0; i < modsPerThread; i++) {
                    String modId = "con_" + threadId + "_" + i;
                    try {
                        graph.addMod(makeMod(modId, "1.0.0", Map.of()), "1.0.0");
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        errors.add("Thread " + threadId + " mod " + i + ": " + e.getMessage());
                    }
                }
            });
            threads.add(th);
            th.start();
        }

        long start = System.nanoTime();
        for (Thread th : threads) {
            th.join(30_000); // 30s timeout
        }
        long end = System.nanoTime();

        m.buildTimeMs = (end - start) / 1_000_000;
        m.nodeCount = graph.getTotalNodeCount();
        m.passed = successCount.get() == threadCount * modsPerThread
            && failCount.get() == 0
            && graph.getTotalNodeCount() == threadCount * modsPerThread;

        if (!m.passed) {
            m.error = String.format("Success: %d, Fail: %d, Nodes: %d (expected %d)",
                successCount.get(), failCount.get(), graph.getTotalNodeCount(), threadCount * modsPerThread);
            if (!errors.isEmpty()) {
                String first = errors.peek();
                m.error += " | First error: " + first;
            }
        }
        allMetrics.add(m);
        System.out.println(m);
    }

    @Test
    @DisplayName("[P2] Soak Test — 重复构建分层1000次")
    void testSoakRepeatedBuild() {
        TestMetrics m = new TestMetrics("Soak 1000x rebuild");
        try {
            // 先建一个500节点的图
            List<ModManifest> mods = generateRandomDAG(500, 0.04);
            addAll(graph, mods);

            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                graph.buildLayers();
                // 每次验证层数一致性
                int layers = graph.getLayerCount();
                assertTrue(layers > 0 && layers <= 500,
                    "Layers should be between 1 and 500, got " + layers);
            }
            long end = System.nanoTime();

            m.buildTimeMs = (end - start) / 1_000_000;
            m.nodeCount = graph.getTotalNodeCount();
            m.edgeCount = graph.getTotalEdgeCount();
            m.layerCount = graph.getLayerCount();
            m.speedupRatio = graph.getSpeedupRatio();
            m.passed = true;

            double avgTimePerBuild = (double) m.buildTimeMs / 1000;
            System.out.println("  ⏱ 平均每次 buildLayers(): " + String.format("%.3f", avgTimePerBuild) + " ms");
            assertTrue(avgTimePerBuild < 50, "Average build time should be < 50ms for 500 nodes");
        } catch (Exception e) {
            m.passed = false;
            m.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        allMetrics.add(m);
        System.out.println(m);
    }

    @Test
    @DisplayName("[P2] 加速比可扩展性 — 验证N增长时加速比趋势")
    void testSpeedupScalability() {
        System.out.println("\n📈 加速比可扩展性测试:");
        int[] sizes = {10, 50, 100, 200, 500};
        List<Double> speedups = new ArrayList<>();

        for (int size : sizes) {
            ModGraph g = new ModGraph();
            addAll(g, generateStar("scalability_core_" + size, size - 1));
            g.buildLayers();
            double ratio = g.getSpeedupRatio();
            speedups.add(ratio);
            System.out.printf("  N=%-4d  加速比=%-8.2fx  层数=%-3d%n",
                size, ratio, g.getLayerCount());

            // 星型图应该只有2层: core + satellites
            assertEquals(2, g.getLayerCount(),
                "Star graph should have exactly 2 layers for N=" + size);
        }

        // 验证加速比随N增长
        for (int i = 1; i < speedups.size(); i++) {
            assertTrue(speedups.get(i) >= speedups.get(i - 1) * 0.9,
                "Speedup should not regress significantly. "
                + sizes[i] + ": " + speedups.get(i) + " vs "
                + sizes[i - 1] + ": " + speedups.get(i - 1));
        }

        TestMetrics m = new TestMetrics("Speedup Scalability");
        m.passed = true;
        m.buildTimeMs = -1;
        m.nodeCount = 500;
        m.edgeCount = 499;
        m.layerCount = 2;
        m.speedupRatio = speedups.get(speedups.size() - 1);
        allMetrics.add(m);
        System.out.println(m);
    }

    // ═══════════════════════════════════════════════════════════
    //  汇总报告 (在所有测试之后执行)
    // ═══════════════════════════════════════════════════════════

    @AfterAll
    void printSummaryReport() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         Francium DAG 大规模压力测试报告                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        int passed = 0, failed = 0;
        for (TestMetrics m : allMetrics) {
            System.out.println(m);
            if (m.passed) passed++; else failed++;
        }

        System.out.println();
        System.out.println("────────────────────────────────────────────────────────");
        System.out.printf("  总计: %d 项测试 | ✅ 通过: %d | ❌ 失败: %d%n",
            allMetrics.size(), passed, failed);
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println();

        // 输出关键结论
        System.out.println("📌 关键结论:");
        System.out.println("  • DAG 500节点构建速度: " +
            allMetrics.stream()
                .filter(m -> m.name.contains("Random DAG 500"))
                .findFirst()
                .map(m -> m.buildTimeMs + " ms")
                .orElse("N/A"));
        System.out.println("  • DAG 1000节点构建速度: " +
            allMetrics.stream()
                .filter(m -> m.name.contains("Random DAG 1000"))
                .findFirst()
                .map(m -> m.buildTimeMs + " ms")
                .orElse("N/A"));
        System.out.println("  • Soak test 平均分层耗时: " +
            allMetrics.stream()
                .filter(m -> m.name.contains("Soak"))
                .findFirst()
                .map(m -> String.format("%.3f ms", (double) m.buildTimeMs / 1000))
                .orElse("N/A"));
        System.out.println("  • 最大加速比 (Star 500): " +
            allMetrics.stream()
                .filter(m -> m.name.contains("Star 500"))
                .findFirst()
                .map(m -> String.format("%.2fx", m.speedupRatio))
                .orElse("N/A"));

        System.out.println();
        if (failed > 0) {
            System.out.println("⚠️  有 " + failed + " 项测试失败！需要排查！");
        } else {
            System.out.println("🎉 所有压力测试全部通过！Francium DAG 在极端压力下表现稳定！");
        }
    }
}
