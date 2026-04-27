package com.francium;

import com.francium.graph.ModGraph;
import com.francium.loader.ModManifest;
import java.util.*;

public class ModGraphTest {
    static int passed = 0, failed = 0;
    static void check(boolean c, String m) {
        if (!c) { failed++; System.out.println("  FAIL " + m); }
        else { passed++; System.out.println("  PASS " + m); }
    }

    static ModManifest m(String id, String ver, String... deps) {
        var b = ModManifest.builder(id, ver).mainClass("ex." + id);
        for (int i=0;i<deps.length;i++) b.dependency(deps[i], "*");
        return b.build();
    }

    public static void main(String[] args) {
        // 1. Simple DAG
        ModGraph g = new ModGraph();
        g.addMod(m("D","1.0"),"1.0"); g.addMod(m("E","1.0"),"1.0");
        g.addMod(m("B","1.0","D"),"1.0"); g.addMod(m("C","1.0","E"),"1.0");
        g.addMod(m("A","1.0","B","C"),"1.0");
        var layers = g.getLayers();
        check(layers.size() == 3, "3 topological layers (expected 3, got " + layers.size() + ")");
        check(layers.get(0).contains("D"), "Layer 0 contains D");
        check(layers.get(0).contains("E"), "Layer 0 contains E");
        check(layers.get(1).contains("B"), "Layer 1 contains B");
        check(layers.get(1).contains("C"), "Layer 1 contains C");
        check(layers.get(2).contains("A"), "Layer 2 contains A");
        check(layers.stream().mapToInt(Set::size).sum() == 5, "total 5 nodes");

        // 2. Independent mods
        g = new ModGraph();
        for (int i=0;i<10;i++) g.addMod(m("M"+i,"1.0"),"1.0");
        check(g.getLayers().size()==1, "independent mods = 1 layer");
        check(g.getLayers().get(0).size()==10, "all 10 mods in one layer");

        // 3. Chain
        g = new ModGraph();
        g.addMod(m("M5","1.0"),"1.0"); g.addMod(m("M4","1.0","M5"),"1.0");
        g.addMod(m("M3","1.0","M4"),"1.0"); g.addMod(m("M2","1.0","M3"),"1.0");
        g.addMod(m("M1","1.0","M2"),"1.0");
        layers = g.getLayers();
        check(layers.size()==5, "chain = 5 layers");
        for (int i=0;i<5;i++) check(layers.get(i).size()==1, "layer "+i+" has 1 mod");

        // 4. Circular detection
        g = new ModGraph();
        g.addMod(m("X","1.0","Y"),"1.0"); g.addMod(m("Y","1.0","Z"),"1.0");
        g.addMod(m("Z","1.0","X"),"1.0");
        try { g.getLayers(); check(false,"should throw"); }
        catch (ModGraph.CircularDependencyException e) {
            check(true, "circular dependency detected");
            check(e.cycles() != null && !e.cycles().isEmpty(), "cycles list non-empty");
        }

        // 5. Duplicate rejection
        g = new ModGraph();
        var mm = m("mod","1.0");
        g.addMod(mm,"1.0");
        check(!g.addMod(mm,"1.0"), "duplicate same version rejected");
        check(g.getLayers().get(0).size()==1, "mod count stays 1");

        System.out.println("  ModGraph: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
