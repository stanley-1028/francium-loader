package com.francium;

import com.francium.graph.ModGraph;
import com.francium.loader.ModManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Comprehensive test suite for ModGraph.
 * Covers: DAG construction, topological layering, cycle detection,
 * edge cases (empty, single node, isolated nodes), and performance estimation.
 */
public class ModGraphComprehensiveTest {

    private ModGraph graph;

    @BeforeEach
    void setUp() {
        graph = new ModGraph();
    }

    @Test
    @DisplayName("Empty graph has no layers")
    void testEmptyGraph() {
        assertTrue(graph.getLayers().isEmpty(), "Empty graph should have no layers");
        assertEquals(0, graph.getModCount(), "Empty graph has 0 mods");
        assertEquals(0, graph.getTotalNodeCount(), "Empty graph has 0 nodes");
        assertEquals(0, graph.getTotalEdgeCount(), "Empty graph has 0 edges");
        assertEquals(0, graph.getLayerCount(), "Empty graph has 0 layers");
    }

    @Test
    @DisplayName("Single mod without dependencies is in layer 0")
    void testSingleMod() {
        addMod("modA", "1.0.0", 100, Map.of());
        List<Set<String>> layers = graph.getLayers();
        assertEquals(1, layers.size(), "Should have 1 layer");
        assertTrue(layers.get(0).contains("modA"), "Layer 0 should contain modA");
        assertEquals(1, graph.getModCount());
        assertEquals(1, graph.getTotalNodeCount());
    }

    @Test
    @DisplayName("Two independent mods are in the same layer")
    void testIndependentMods() {
        addMod("modA", "1.0.0", 100, Map.of());
        addMod("modB", "1.0.0", 200, Map.of());
        List<Set<String>> layers = graph.getLayers();
        assertEquals(1, layers.size(), "Independent mods should be in a single layer");
        assertEquals(2, layers.get(0).size(), "Layer 0 should contain both mods");
    }

    @Test
    @DisplayName("Dependency chain creates multiple layers")
    void testDependencyChain() {
        // A depends on B, B depends on C
        // Layers: [C], [B], [A]
        addMod("modA", "1.0.0", 100, Map.of("modB", ">=1.0.0"));
        addMod("modB", "1.0.0", 100, Map.of("modC", ">=1.0.0"));
        addMod("modC", "1.0.0", 100, Map.of());

        List<Set<String>> layers = graph.getLayers();
        assertEquals(3, layers.size(), "Chain of 3 mods should create 3 layers");
        assertTrue(layers.get(0).contains("modC"), "Layer 0: modC (no deps)");
        assertTrue(layers.get(1).contains("modB"), "Layer 1: modB (depends on C)");
        assertTrue(layers.get(2).contains("modA"), "Layer 2: modA (depends on B)");
    }

    @Test
    @DisplayName("Diamond dependency creates 2 layers")
    void testDiamondDependency() {
        // A depends on B and C
        // B depends on D, C depends on D
        // Layers: [D], [B, C], [A]
        addMod("modA", "1.0.0", 100, Map.of("modB", ">=1.0.0", "modC", ">=1.0.0"));
        addMod("modB", "1.0.0", 100, Map.of("modD", ">=1.0.0"));
        addMod("modC", "1.0.0", 100, Map.of("modD", ">=1.0.0"));
        addMod("modD", "1.0.0", 100, Map.of());

        List<Set<String>> layers = graph.getLayers();
        assertEquals(3, layers.size(), "Diamond should create 3 layers");
        assertTrue(layers.get(0).contains("modD"), "Layer 0: modD");
        assertEquals(2, layers.get(1).size(), "Layer 1: B and C (parallel)");
        assertTrue(layers.get(1).contains("modB") && layers.get(1).contains("modC"),
            "Layer 1 should contain both B and C");
        assertTrue(layers.get(2).contains("modA"), "Layer 2: modA");
    }

    @Test
    @DisplayName("Circular dependency throws exception")
    void testCircularDependency() {
        // A depends on B, B depends on C, C depends on A → cycle!
        addMod("modA", "1.0.0", 100, Map.of("modB", ">=1.0.0"));
        addMod("modB", "1.0.0", 100, Map.of("modC", ">=1.0.0"));
        addMod("modC", "1.0.0", 100, Map.of("modA", ">=1.0.0"));

        assertThrows(ModGraph.CircularDependencyException.class,
            () -> graph.getLayers(),
            "Circular dependency should throw CircularDependencyException");
    }

    @Test
    @DisplayName("Self-dependency throws exception")
    void testSelfDependency() {
        addMod("modA", "1.0.0", 100, Map.of("modA", ">=1.0.0"));
        assertThrows(ModGraph.CircularDependencyException.class,
            () -> graph.getLayers(),
            "Self-dependency should throw CircularDependencyException");
    }

    @Test
    @DisplayName("ModConflictException for duplicate mod with different version")
    void testDuplicateModConflict() {
        addMod("modA", "1.0.0", 100, Map.of());
        assertThrows(ModGraph.ModConflictException.class,
            () -> addMod("modA", "2.0.0", 100, Map.of()),
            "Adding same mod with different version should throw");
    }

    @Test
    @DisplayName("Duplicate mod with same version returns false")
    void testDuplicateModSameVersion() {
        addMod("modA", "1.0.0", 100, Map.of());
        boolean added = graph.addMod(
            createManifest("modA", "1.0.0", 100, Map.of()), "1.0.0");
        assertFalse(added, "Adding same mod+version should return false");
        assertEquals(1, graph.getModCount(), "Mod count should not increase");
    }

    @Test
    @DisplayName("Isolated nodes with no deps are in layer 0")
    void testIsolatedNodes() {
        // Create a graph where some nodes have dependencies and some don't
        addMod("modA", "1.0.0", 100, Map.of("modB", ">=1.0.0"));
        addMod("modB", "1.0.0", 100, Map.of());
        addMod("modIsolated1", "1.0.0", 100, Map.of());
        addMod("modIsolated2", "1.0.0", 100, Map.of());

        List<Set<String>> layers = graph.getLayers();
        // Layer 0: B, Isolated1, Isolated2 (all have no deps)
        // Layer 1: A (depends on B)
        assertEquals(2, layers.size(), "Should create 2 layers");
        assertTrue(layers.get(0).contains("modB"), "Layer 0: modB");
        assertTrue(layers.get(0).contains("modIsolated1"), "Layer 0: isolated1");
        assertTrue(layers.get(0).contains("modIsolated2"), "Layer 0: isolated2");
        assertTrue(layers.get(1).contains("modA"), "Layer 1: modA");
    }

    @Test
    @DisplayName("External provider is tracked in node count")
    void testExternalProvider() {
        graph.addExternalProvider("minecraft");
        addMod("modA", "1.0.0", 100, Map.of("minecraft", ">=1.20.0"));

        assertEquals(2, graph.getTotalNodeCount(), "Should track both mod and external provider");
        assertEquals(1, graph.getModCount(), "Only modA is a registered mod");
    }

    @Test
    @DisplayName("Load time estimation shows speedup with parallelism")
    void testLoadTimeEstimation() {
        addMod("modA", "1.0.0", 100, Map.of("modB", ">=1.0.0"));
        addMod("modB", "1.0.0", 150, Map.of());
        addMod("modC", "1.0.0", 200, Map.of());

        long parallel = graph.estimateParallelLoadTime();
        long sequential = graph.estimateSequentialLoadTime();
        double ratio = graph.getSpeedupRatio();

        // Sequential: 100 + 150 + 200 = 450
        // Parallel: max(150, 200) + 100 = 300 (layer 0: B+C, layer 1: A)
        assertTrue(parallel < sequential, "Parallel time should be less than sequential");
        assertTrue(ratio > 1.0, "Speedup ratio should be > 1.0");
        assertEquals(450, sequential, "Sequential should sum all load times");
    }

    @Test
    @DisplayName("Multiple layers with different widths")
    void testMultipleLayers() {
        // Layer 0: X (no deps)
        // Layer 1: A, B, C (depend on X)
        // Layer 2: D, E (depend on A, B)
        addMod("modX", "1.0.0", 100, Map.of());
        addMod("modA", "1.0.0", 100, Map.of("modX", ">=1.0.0"));
        addMod("modB", "1.0.0", 100, Map.of("modX", ">=1.0.0"));
        addMod("modC", "1.0.0", 100, Map.of("modX", ">=1.0.0"));
        addMod("modD", "1.0.0", 100, Map.of("modA", ">=1.0.0", "modB", ">=1.0.0"));
        addMod("modE", "1.0.0", 100, Map.of("modA", ">=1.0.0"));

        List<Set<String>> layers = graph.getLayers();
        assertEquals(3, layers.size(), "Should create 3 layers");
        assertEquals(1, layers.get(0).size(), "Layer 0: only X");
        assertEquals(3, layers.get(1).size(), "Layer 1: A, B, C");
        assertEquals(2, layers.get(2).size(), "Layer 2: D, E");
    }

    @Test
    @DisplayName("GetManifest returns correct manifest")
    void testGetManifest() {
        addMod("modA", "1.0.0", 100, Map.of());
        ModManifest manifest = graph.getManifest("modA");
        assertNotNull(manifest);
        assertEquals("modA", manifest.modId());
        assertEquals("1.0.0", manifest.version());
    }

    @Test
    @DisplayName("GetAllManifests returns all manifests")
    void testGetAllManifests() {
        addMod("modA", "1.0.0", 100, Map.of());
        addMod("modB", "1.0.0", 100, Map.of());
        assertEquals(2, graph.getAllManifests().size());
    }

    @Test
    @DisplayName("CircularDependencyException captures cycle details")
    void testCycleDetails() {
        addMod("modA", "1.0.0", 100, Map.of("modB", ">=1.0.0"));
        addMod("modB", "1.0.0", 100, Map.of("modC", ">=1.0.0"));
        addMod("modC", "1.0.0", 100, Map.of("modA", ">=1.0.0"));

        try {
            graph.getLayers();
            fail("Should have thrown");
        } catch (ModGraph.CircularDependencyException e) {
            assertNotNull(e.cycles());
            assertFalse(e.cycles().isEmpty(), "Should have at least one cycle");
            // Cycle should include A, B, C
            List<String> cycle = e.cycles().get(0);
            assertTrue(cycle.contains("modA"));
            assertTrue(cycle.contains("modB"));
            assertTrue(cycle.contains("modC"));
        }
    }

    @Test
    @DisplayName("Large graph with 1000 mods resolves quickly")
    void testLargeGraph() {
        int modCount = 1000;
        // Create a chain: mod1 -> mod2 -> mod3 -> ... -> mod1000
        for (int i = 1; i <= modCount; i++) {
            String modId = "mod" + i;
            Map<String, String> deps = new HashMap<>();
            if (i > 1) {
                deps.put("mod" + (i - 1), ">=1.0.0");
            }
            addMod(modId, "1.0." + i, 50, deps);
        }

        long start = System.nanoTime();
        List<Set<String>> layers = graph.getLayers();
        long duration = System.nanoTime() - start;

        assertEquals(modCount, layers.size(), "Chain of 1000 mods should create 1000 layers");
        assertTrue(duration < 5_000_000_000L, "Large graph should resolve in < 5 seconds");
    }

    @Test
    @DisplayName("Star graph has 2 layers")
    void testStarGraph() {
        // Center mod depends on 10 leaf mods
        addMod("center", "1.0.0", 500, Map.of());
        for (int i = 0; i < 10; i++) {
            addMod("leaf" + i, "1.0.0", 50, Map.of("center", ">=1.0.0"));
        }

        List<Set<String>> layers = graph.getLayers();
        assertEquals(2, layers.size(), "Star should create 2 layers");
        assertEquals(1, layers.get(0).size(), "Layer 0: center only");
        assertEquals(10, layers.get(1).size(), "Layer 1: all 10 leaves");
    }

    // ==================== Helpers ====================

    private void addMod(String id, String version, long loadTimeMs, Map<String, String> deps) {
        graph.addMod(createManifest(id, version, loadTimeMs, deps), version);
    }

    private ModManifest createManifest(String id, String version, long loadTimeMs,
                                        Map<String, String> deps) {
        ModManifest.Builder builder = ModManifest.builder(id, version)
            .mainClass("com.example." + id + ".Main")
            .sizeBytes(loadTimeMs);
        for (var entry : deps.entrySet()) {
            builder.dependency(entry.getKey(), entry.getValue());
        }
        ModManifest m = builder.build();
        m.setEstimatedLoadTimeMs(loadTimeMs);
        return m;
    }
}
