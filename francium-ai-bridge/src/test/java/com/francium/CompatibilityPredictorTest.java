package com.francium;

import com.francium.ai.mapping.MethodSignature;
import com.francium.ai.predictor.CompatibilityPredictor;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompatibilityPredictorTest {
    @Test
    public void testAll() {
        var sig = new MethodSignature("net/minecraft/world/level/block/Block","getBlockState","(I)Ljava/lang/String;");
        assertEquals("net/minecraft/world/level/block/Block", sig.owner(), "owner");
        assertEquals("getBlockState", sig.name(), "name");
        assertEquals("java.lang.String", sig.returnType(), "returnType");
        assertEquals(List.of("int"), sig.paramTypes(), "paramTypes");
        System.out.println("    Signature: " + sig);

        var p = new CompatibilityPredictor();
        var a = new MethodSignature("some/Class","method","()V");
        var b = new MethodSignature("some/Class","method","()V");
        float c1 = p.confidence(a, b);
        assertTrue(c1 >= 0.75, "exact match confidence=" + c1);

        var src = new MethodSignature("net/minecraft/world/level/block/Block","m_49792_","()F");
        src.setMojangName("getExplosionResistance");
        var tgt = new MethodSignature("net/minecraft/world/level/block/Block","getExplosionResistance","()F");
        float c2 = p.confidence(src, tgt);
        assertTrue(c2 >= 0.40, "obfuscated name match confidence=" + c2);

        var sr = new MethodSignature("some/Class","methodA","()V"); sr.setMojangName("methodA");
        var candidates = new ArrayList<>(List.of(new MethodSignature("some/Class","methodZ","()V"),
            new MethodSignature("some/Class","methodA","()V"),
            new MethodSignature("some/Class","m_12345_","()V")));
        candidates.get(2).setMojangName("methodA");
        candidates.sort((x,y)->Float.compare(p.confidence(sr,y),p.confidence(sr,x)));
        assertFalse(candidates.isEmpty(), "rankCandidates found best");
        String bestName = candidates.get(0).name();
        assertTrue("methodA".equals(bestName) || "m_12345_".equals(bestName), "best=" + bestName);

        var s1 = new MethodSignature("some/Class","methodA","(I)V");
        var s2 = new MethodSignature("some/Class","methodB","(I)V");
        float c4 = p.confidence(s1, s2);
        assertTrue(c4 > 0.0, "cross-version structural similarity=" + (c4*100) + "%");

        var sx = new MethodSignature("some/Class","methodA","()V");
        var sy = new MethodSignature("some/Class","methodB","()V");
        p.recordSuccessfulMatch(sx, sy); p.recordSuccessfulMatch(sx, sy); p.recordSuccessfulMatch(sx, sy);
        float c5 = p.confidence(sx, sy);
        assertTrue(c5 > 0.1, "RL confidence=" + c5);
    }
}
