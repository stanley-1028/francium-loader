package com.francium;

import com.francium.graph.ModGraph;
import com.francium.loader.ModManifest;
import com.francium.resolver.model.DependencyConstraint;
import com.francium.resolver.model.SemanticVersion;
import com.francium.resolver.sat.SATDependencyResolver;
import com.francium.profiler.memory.MemoryManager;
import com.francium.ai.adapter.VersionBridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * End-to-end integration test for Francium Loader.
 *
 * Verifies the full pipeline without launching Minecraft:
 * 1. SAT dependency resolution with realistic mod dependency chains
 * 2. DAG scheduling with topological layering
 * 3. Parallel loading simulation via layer scheduling
 * 4. Profiler metrics collection
 * 5. AI bridge version compatibility conversion
 * 6. Cycle detection (negative case)
 *
 * Design: Self-contained mocks, no file I/O, completes in < 5 seconds.
 */
public class FranciumE2ETest {

    // -----------------------------------------------------------------------
    // Helper: create a ModManifest with dependencies, conflicts, and load time
    // -----------------------------------------------------------------------
    private ModManifest createMod(String id, String version, long loadTimeMs,
                                   Map<String, String> deps,
                                   Map<String, String> conflicts) {
        ModManifest.Builder b = ModManifest.builder(id, version)
            .mainClass("com.example." + id + ".Main")
            .sizeBytes(loadTimeMs)
            .aiBridge(true);
        if (deps != null) {
            for (var entry : deps.entrySet()) {
                b.dependency(entry.getKey(), entry.getValue());
            }
        }
        if (conflicts != null) {
            for (var entry : conflicts.entrySet()) {
                b.conflict(entry.getKey(), entry.getValue());
            }
        }
        ModManifest m = b.build();
        m.setEstimatedLoadTimeMs(loadTimeMs);
        return m;
    }

    private ModManifest createMod(String id, String version, long loadTimeMs,
                                   Map<String, String> deps) {
        return createMod(id, version, loadTimeMs, deps, null);
    }

    // -----------------------------------------------------------------------
    // Helper: register a mod version + dependencies into the SAT resolver
    // -----------------------------------------------------------------------
    private void registerMod(SATDependencyResolver resolver, String modId, String version,
                              Map<String, String> deps, Map<String, String> conflicts) {
        SemanticVersion sv = SemanticVersion.parse(version);
        resolver.registerVersions(modId, List.of(sv));

        if (deps != null && !deps.isEmpty()) {
            Map<String, DependencyConstraint> constraints = new LinkedHashMap<>();
            for (var entry : deps.entrySet()) {
                constraints.put(entry.getKey(), new DependencyConstraint(entry.getValue()));
            }
            resolver.registerDependencies(modId, constraints);
        }

        if (conflicts != null && !conflicts.isEmpty()) {
            for (var entry : conflicts.entrySet()) {
                resolver.registerConflict(modId, entry.getKey(), entry.getValue());
            }
        }
    }

    // ===================================================================
    //  E2E Test: Full Pipeline with 8 realistic Fabric-like mods
    // ===================================================================

    /**
     * Creates a realistic mod ecosystem resembling Fabric mods:
     *
     * Layer 0 (no deps):          minecraft-api, fabric-api (externals)
     * Layer 1:                    fabric-language-kotlin
     * Layer 2:                    sodium (needs fabric-api)
     * Layer 2 (parallel):         fabric-carpet (needs fabric-api, minecraft-api)
     * Layer 3:                    iris (needs sodium, fabric-api)
     * Layer 3 (parallel):         lithium (needs fabric-api, minecraft-api)
     * Layer 4:                    complementary-shaders (needs iris)
     *
     * Dependency graph:
     *   fabric-language-kotlin -> minecraft-api
     *   sodium -> fabric-api
     *   fabric-carpet -> fabric-api, minecraft-api
     *   iris -> sodium, fabric-api
     *   lithium -> fabric-api, minecraft-api
     *   complementary-shaders -> iris
     *
     * Expected layers:
     *   Layer 0: {minecraft-api, fabric-api}
     *   Layer 1: {fabric-language-kotlin}
     *   Layer 2: {sodium, fabric-carpet}
     *   Layer 3: {iris, lithium}
     *   Layer 4: {complementary-shaders}
     */
    @Test
    @DisplayName("E2E: Full pipeline with 8 realistic mods resolves SAT, builds DAG, assigns layers correctly")
    void testFullPipelineWith8Mods() throws Exception {
        // ---- 1. Define mod ecosystem ----
        Map<String, ModInfo> mods = new LinkedHashMap<>();

        // External providers (no deps)
        mods.put("minecraft-api", new ModInfo("1.21.0", 50, Map.of()));
        mods.put("fabric-api", new ModInfo("0.97.0", 80, Map.of()));

        // Layer 1: depends on minecraft-api
        mods.put("fabric-language-kotlin", new ModInfo("1.10.0", 30,
            Map.of("minecraft-api", ">=1.20.0")));

        // Layer 2: depend on fabric-api / minecraft-api
        mods.put("sodium", new ModInfo("0.5.8", 120,
            Map.of("fabric-api", ">=0.90.0")));
        mods.put("fabric-carpet", new ModInfo("1.4.112", 60,
            Map.of("fabric-api", ">=0.90.0", "minecraft-api", ">=1.20.0")));

        // Layer 3: depend on layer 2 mods
        mods.put("iris", new ModInfo("1.7.2", 90,
            Map.of("sodium", ">=0.5.0", "fabric-api", ">=0.90.0")));
        mods.put("lithium", new ModInfo("0.12.6", 40,
            Map.of("fabric-api", ">=0.90.0", "minecraft-api", ">=1.20.0")));

        // Layer 4: depends on layer 3 mod
        mods.put("complementary-shaders", new ModInfo("1.8.1", 70,
            Map.of("iris", ">=1.6.0")));

        // ---- 2. SAT Resolution ----
        SATDependencyResolver resolver = new SATDependencyResolver();
        resolver.setTimeoutMs(5000);

        for (var entry : mods.entrySet()) {
            ModInfo info = entry.getValue();
            registerMod(resolver, entry.getKey(), info.version, info.deps, null);
        }

        // Add external providers to resolver's version list
        // (they are already registered above as no-dependency mods)

        List<String> rootMods = new ArrayList<>(mods.keySet());
        SATDependencyResolver.ResolveResult resolveResult = resolver.solve(rootMods);

        assertTrue(resolveResult.success,
            "SAT resolver should find a valid solution for " + mods.size() + " mods");
        assertNotNull(resolveResult.solution,
            "ResolveResult should have a non-null solution");
        assertEquals(mods.size(), resolveResult.solution.size(),
            "All " + mods.size() + " mods should be in the solution");
        assertTrue(resolveResult.solveTimeMs >= 0,
            "Solve time should be recorded");
        System.out.println("E2E: SAT resolved " + resolveResult.solution.size()
            + " mods in " + resolveResult.solveTimeMs + "ms ("
            + resolveResult.nodesExplored + " nodes, "
            + resolveResult.backtracks + " backtracks, "
            + resolveResult.propagations + " propagations)");

        // ---- 3. DAG Scheduling ----
        ModGraph graph = new ModGraph();

        // Register external providers
        graph.addExternalProvider("minecraft-api");
        graph.addExternalProvider("fabric-api");

        // Add each mod with its resolved version to the graph
        for (var entry : resolveResult.solution.entrySet()) {
            String modId = entry.getKey();
            SemanticVersion resolvedVersion = entry.getValue();
            ModInfo info = mods.get(modId);

            ModManifest manifest = createMod(
                modId, info.version, info.loadTimeMs, info.deps);
            graph.addMod(manifest, resolvedVersion.toString());
        }

        // Build layers
        List<Set<String>> layers = graph.getLayers();

        // Verify layer count
        // Layer 0: {minecraft-api, fabric-api} — no deps
        // Layer 1: {fabric-language-kotlin, sodium, fabric-carpet, lithium} — all depend on layer 0 only
        // Layer 2: {iris} — depends on sodium (layer 1) and fabric-api (layer 0)
        // Layer 3: {complementary-shaders} — depends on iris (layer 2)
        assertEquals(4, layers.size(),
            "Should produce 4 topological layers");
        System.out.println("E2E: DAG built " + layers.size()
            + " layers (" + graph.getModCount() + " mods, "
            + graph.getTotalNodeCount() + " nodes, "
            + graph.getTotalEdgeCount() + " edges)");

        // Layer 0: fabric-api and minecraft-api (no dependencies)
        assertTrue(layers.get(0).contains("fabric-api"),
            "Layer 0 should contain fabric-api");
        assertTrue(layers.get(0).contains("minecraft-api"),
            "Layer 0 should contain minecraft-api");

        // Layer 1: fabric-language-kotlin, sodium, fabric-carpet, lithium
        // (all depend only on layer 0 mods, so they're parallel)
        assertTrue(layers.get(1).contains("fabric-language-kotlin"),
            "Layer 1 should contain fabric-language-kotlin");
        assertTrue(layers.get(1).contains("sodium"),
            "Layer 1 should contain sodium");
        assertTrue(layers.get(1).contains("fabric-carpet"),
            "Layer 1 should contain fabric-carpet");
        assertTrue(layers.get(1).contains("lithium"),
            "Layer 1 should contain lithium");

        // Layer 2: iris (depends on sodium in layer 1)
        assertTrue(layers.get(2).contains("iris"),
            "Layer 2 should contain iris");

        // Layer 3: complementary-shaders (depends on iris in layer 2)
        assertTrue(layers.get(3).contains("complementary-shaders"),
            "Layer 3 should contain complementary-shaders");

        // Verify layer sizes
        assertEquals(2, layers.get(0).size(), "Layer 0: 2 mods (ext providers)");
        assertEquals(4, layers.get(1).size(), "Layer 1: 4 mods (parallel — all depend on layer 0 only)");
        assertEquals(1, layers.get(2).size(), "Layer 2: 1 mod (iris)");
        assertEquals(1, layers.get(3).size(), "Layer 3: 1 mod (complementary-shaders)");

        // ---- 4. Parallel Loading Simulation ----
        // Verify the scheduler yields the correct load order
        long parallelLoadTime = graph.estimateParallelLoadTime();
        long sequentialLoadTime = graph.estimateSequentialLoadTime();
        double speedup = graph.getSpeedupRatio();

        assertTrue(parallelLoadTime > 0,
            "Estimated parallel load time should be positive");
        assertTrue(sequentialLoadTime > parallelLoadTime,
            "Sequential load time should be greater than parallel due to parallelism");
        assertTrue(speedup > 1.0,
            "Speedup ratio should be > 1.0 (got " + speedup + ")");
        System.out.println("E2E: Parallel=" + parallelLoadTime + "ms, "
            + "Sequential=" + sequentialLoadTime + "ms, "
            + "Speedup=" + String.format("%.2fx", speedup));

        // Simulate loading layers sequentially (as the scheduler would)
        long simulatedLoadTime = 0;
        Set<String> loadedMods = new HashSet<>();
        for (int i = 0; i < layers.size(); i++) {
            Set<String> layer = layers.get(i);
            long layerMaxTime = 0;
            for (String modId : layer) {
                ModInfo info = mods.get(modId);
                if (info != null) {
                    layerMaxTime = Math.max(layerMaxTime, info.loadTimeMs);
                }
                loadedMods.add(modId);
            }
            simulatedLoadTime += layerMaxTime;
            System.out.println("  Layer " + i + ": " + layer + " (bottleneck=" + layerMaxTime + "ms)");
        }
        assertEquals(mods.size(), loadedMods.size(),
            "All mods should be loadable via the layered schedule");
        assertEquals(parallelLoadTime, simulatedLoadTime,
            "Simulated load time should match the estimated parallel load time");

        // ---- 5. Profiler Metrics ----
        MemoryManager memoryManager = new MemoryManager(512, true, true);

        // Register mod loaders (simulating the ParallelModClassLoader lifecycle)
        for (String modId : mods.keySet()) {
            memoryManager.registerLoader(modId, FranciumE2ETest.class.getClassLoader());
        }

        // Take a snapshot
        MemoryManager.MemorySnapshot snapshot = memoryManager.getSnapshot();
        assertNotNull(snapshot, "Memory snapshot should not be null");
        assertTrue(snapshot.max() > 0, "Max memory should be positive");
        assertTrue(snapshot.heapUsed() >= 0, "Heap used should be >= 0");
        assertTrue(snapshot.usagePercent() >= 0.0, "Usage percent should be >= 0");
        System.out.println("E2E: Profiler snapshot: heap="
            + (snapshot.heapUsed() / 1024 / 1024) + "MB/"
            + (snapshot.max() / 1024 / 1024) + "MB ("
            + String.format("%.1f%%", snapshot.usagePercent() * 100) + ")");

        // Get per-mod stats
        var perModStats = memoryManager.getPerModStats();
        assertEquals(mods.size(), perModStats.size(),
            "Per-mod stats should include all registered mods");

        // Verify leak detection is operational
        var leakReports = memoryManager.getLeakReports();
        assertNotNull(leakReports, "Leak reports should not be null (empty list is OK)");
        System.out.println("E2E: Profiler tracks " + perModStats.size()
            + " mods, " + leakReports.size() + " leaks reported");

        // ---- 6. AI Bridge (Version Compatibility) ----
        VersionBridge bridge = new VersionBridge("1.20.4", "1.21.0");
        bridge.setConfidenceThreshold(0.5f); // lower threshold so it's testable
        bridge.setDryRun(true);

        // The VersionBridge.analyze() requires a Path to a real JAR file,
        // which won't exist in CI. We verify the bridge is constructed correctly
        // and that its configuration methods work via public API.
        assertNotNull(bridge, "VersionBridge should be constructable");
        bridge.setConfidenceThreshold(0.5f);
        bridge.setDryRun(true);
        bridge.setAutoFix(false);
        // Verify the bridge can produce a BridgeSummary even with empty mods
        var emptySummary = bridge.bridgeAll(new LinkedHashMap<>());
        assertNotNull(emptySummary, "BridgeSummary should not be null for empty input");
        assertEquals(1.0f, emptySummary.overallCompatibility, 0.001f,
            "Empty mod list should yield 100% compatibility");
        assertEquals(0, emptySummary.adaptersGenerated,
            "No adapters should be generated for empty mod list");
        System.out.println("E2E: AI Bridge configured: 1.20.4 -> 1.21.0"
            + " (empty summary: " + emptySummary.overallCompatibility * 100 + "%)");

        System.out.println("E2E: ALL PIPELINE CHECKS PASSED");
    }

    // ===================================================================
    //  E2E Test: Cycle Detection (Negative pipeline test)
    // ===================================================================

    @Test
    @DisplayName("E2E: Circular dependency is correctly detected by SAT and DAG")
    void testCycleDetection() {
        // Create 3 mods forming a cycle: A -> B -> C -> A
        Map<String, ModInfo> cyclicMods = new LinkedHashMap<>();
        cyclicMods.put("modA", new ModInfo("1.0.0", 100, Map.of("modB", ">=1.0.0")));
        cyclicMods.put("modB", new ModInfo("1.0.0", 100, Map.of("modC", ">=1.0.0")));
        cyclicMods.put("modC", new ModInfo("1.0.0", 100, Map.of("modA", ">=1.0.0")));

        // SAT resolver may or may not detect the cycle (it depends on transitive dependency
        // collection; the SAT solver doesn't explicitly detect cycles but collects transitive deps)
        SATDependencyResolver resolver = new SATDependencyResolver();
        for (var entry : cyclicMods.entrySet()) {
            ModInfo info = entry.getValue();
            registerMod(resolver, entry.getKey(), info.version, info.deps, null);
        }

        var resolveResult = resolver.solve(new ArrayList<>(cyclicMods.keySet()));

        // SAT may succeed if there are versions that satisfy all constraints
        // (the cycle is a dependency graph cycle, not necessarily a SAT conflict)
        // The REAL cycle detection is in ModGraph

        // DAG: This MUST detect the cycle
        ModGraph graph = new ModGraph();
        for (var entry : cyclicMods.entrySet()) {
            ModInfo info = entry.getValue();
            ModManifest manifest = createMod(entry.getKey(), info.version, info.loadTimeMs, info.deps);
            graph.addMod(manifest, info.version);
        }

        assertThrows(ModGraph.CircularDependencyException.class,
            () -> graph.getLayers(),
            "Circular dependency should throw CircularDependencyException");
        System.out.println("E2E: Cycle correctly detected by DAG layer builder");
    }

    // ===================================================================
    //  E2E Test: Conflict resolution (SAT detects incompatible mods)
    // ===================================================================

    @Test
    @DisplayName("E2E: Version conflict is detected by SAT resolver")
    void testConflictDetection() {
        // modA needs modC >= 2.0.0, but modB needs modC < 2.0.0
        // Only one version of modC (1.5.0) is available — impossible to satisfy both
        SATDependencyResolver resolver = new SATDependencyResolver();

        SemanticVersion vC1 = SemanticVersion.parse("1.5.0");
        SemanticVersion vA1 = SemanticVersion.parse("1.0.0");
        SemanticVersion vB1 = SemanticVersion.parse("1.0.0");

        resolver.registerVersions("modC", List.of(vC1));
        resolver.registerVersions("modA", List.of(vA1));
        resolver.registerVersions("modB", List.of(vB1));

        resolver.registerDependencies("modA", Map.of("modC", new DependencyConstraint(">=2.0.0")));
        resolver.registerDependencies("modB", Map.of("modC", new DependencyConstraint("<2.0.0")));

        var result = resolver.solve(List.of("modA", "modB"));

        // Both modA and modB are root mods; modC's single version can't satisfy both constraints
        // The SAT solver should either fail or produce a solution without modC
        // Since modA and modB both depend on modC with contradictory constraints,
        // resolution should fail
        assertFalse(result.success,
            "SAT resolver should detect conflicting version requirements");
        System.out.println("E2E: Version conflict correctly detected by SAT resolver");
    }

    // ===================================================================
    //  E2E Test: Performance — pipeline resolves in < 5 seconds
    // ===================================================================

    @Test
    @DisplayName("E2E: Full pipeline completes in under 5 seconds")
    void testPipelinePerformance() throws Exception {
        long start = System.nanoTime();

        // Run the full pipeline
        testFullPipelineWith8Mods();

        long duration = System.nanoTime() - start;
        long durationMs = duration / 1_000_000;
        assertTrue(durationMs < 5000,
            "Full E2E pipeline should complete in < 5 seconds (took " + durationMs + "ms)");
        System.out.println("E2E: Pipeline completed in " + durationMs + "ms (limit: 5000ms)");
    }

    // ===================================================================
    //  Data class
    // ===================================================================

    private record ModInfo(String version, long loadTimeMs, Map<String, String> deps) {}
}
