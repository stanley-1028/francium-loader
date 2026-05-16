package com.francium;

import com.francium.graph.ModGraph;
import com.francium.loader.ModManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ModGraph DAG scheduling and topological layering.
 */
public class ModGraphTest {

    static ModManifest m(String id, String ver, String... deps) {
        var b = ModManifest.builder(id, ver).mainClass("ex." + id);
        for (String dep : deps) b.dependency(dep, "*");
        return b.build();
    }

    @Test
    @DisplayName("Simple DAG produces 3 topological layers")
    void testSimpleDAG() {
        ModGraph g = new ModGraph();
        g.addMod(m("D","1.0"),"1.0"); g.addMod(m("E","1.0"),"1.0");
        g.addMod(m("B","1.0","D"),"1.0"); g.addMod(m("C","1.0","E"),"1.0");
        g.addMod(m("A","1.0","B","C"),"1.0");
        var layers = g.getLayers();
        assertEquals(3, layers.size(), "3 topological layers");
        assertTrue(layers.get(0).contains("D"), "Layer 0 contains D");
        assertTrue(layers.get(0).contains("E"), "Layer 0 contains E");
        assertTrue(layers.get(1).contains("B"), "Layer 1 contains B");
        assertTrue(layers.get(1).contains("C"), "Layer 1 contains C");
        assertTrue(layers.get(2).contains("A"), "Layer 2 contains A");
        assertEquals(5, layers.stream().mapToInt(Set::size).sum(), "total 5 nodes");
    }

    @Test
    @DisplayName("Independent mods all go to layer 0")
    void testIndependentMods() {
        ModGraph g = new ModGraph();
        for (int i = 0; i < 10; i++) g.addMod(m("M" + i, "1.0"), "1.0");
        assertEquals(1, g.getLayers().size(), "independent mods = 1 layer");
        assertEquals(10, g.getLayers().get(0).size(), "all 10 mods in one layer");
    }

    @Test
    @DisplayName("Chain produces 5 layers")
    void testChain() {
        ModGraph g = new ModGraph();
        g.addMod(m("M5","1.0"),"1.0"); g.addMod(m("M4","1.0","M5"),"1.0");
        g.addMod(m("M3","1.0","M4"),"1.0"); g.addMod(m("M2","1.0","M3"),"1.0");
        g.addMod(m("M1","1.0","M2"),"1.0");
        var layers = g.getLayers();
        assertEquals(5, layers.size(), "chain = 5 layers");
        for (int i = 0; i < 5; i++)
            assertEquals(1, layers.get(i).size(), "layer " + i + " has 1 mod");
    }

    @Test
    @DisplayName("Circular dependency throws CircularDependencyException")
    void testCircularDependency() {
        ModGraph g = new ModGraph();
        g.addMod(m("X","1.0","Y"),"1.0"); g.addMod(m("Y","1.0","Z"),"1.0");
        g.addMod(m("Z","1.0","X"),"1.0");
        assertThrows(ModGraph.CircularDependencyException.class, g::getLayers,
            "Circular dependency should throw");
    }

    @Test
    @DisplayName("Duplicate version is rejected")
    void testDuplicateRejection() {
        ModGraph g = new ModGraph();
        var mm = m("mod","1.0");
        g.addMod(mm,"1.0");
        assertFalse(g.addMod(mm,"1.0"), "duplicate same version rejected");
        assertEquals(1, g.getLayers().get(0).size(), "mod count stays 1");
    }
}
