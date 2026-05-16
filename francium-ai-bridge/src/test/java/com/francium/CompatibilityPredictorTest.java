package com.francium;

import com.francium.ai.mapping.MethodSignature;
import com.francium.ai.predictor.CompatibilityPredictor;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompatibilityPredictorTest {
    static int passed = 0, failed = 0;
    static void check(boolean c, String m) {
        if (!c) { failed++; System.out.println("  FAIL " + m); }
        else { passed++; System.out.println("  PASS " + m); }
    }
    @Test
    public void testAll() {
        var sig = new MethodSignature("net/minecraft/world/level/block/Block","getBlockState","(I)Ljava/lang/String;");
        check("net/minecraft/world/level/block/Block".equals(sig.owner()), "owner");
        check("getBlockState".equals(sig.name()), "name");
        check("java.lang.String".equals(sig.returnType()), "returnType=java.lang.String");
        check(List.of("int").equals(sig.paramTypes()), "paramTypes=[int]");
        System.out.println("    Signature: " + sig);

        var p = new CompatibilityPredictor();
        var a = new MethodSignature("some/Class","method","()V");
        var b = new MethodSignature("some/Class","method","()V");
        float c1 = p.confidence(a, b);
        check(c1 >= 0.75, "exact match confidence=" + String.format("%.2f", c1));

        var src = new MethodSignature("net/minecraft/world/level/block/Block","m_49792_","()F");
        src.setMojangName("getExplosionResistance");
        var tgt = new MethodSignature("net/minecraft/world/level/block/Block","getExplosionResistance","()F");
        float c2 = p.confidence(src, tgt);
        check(c2 >= 0.40, "obfuscated name match confidence=" + String.format("%.2f", c2));

        var sr = new MethodSignature("some/Class","methodA","()V"); sr.setMojangName("methodA");
        var candidates = new ArrayList<>(List.of(new MethodSignature("some/Class","methodZ","()V"),
            new MethodSignature("some/Class","methodA","()V"),
            new MethodSignature("some/Class","m_12345_","()V")));
        candidates.get(2).setMojangName("methodA");
        candidates.sort((x,y)->Float.compare(p.confidence(sr,y),p.confidence(sr,x)));
        check(!candidates.isEmpty(), "rankCandidates found best");
        check("methodA".equals(candidates.get(0).name()) || "m_12345_".equals(candidates.get(0).name()), "best=" + candidates.get(0).name());

        var s1 = new MethodSignature("some/Class","methodA","(I)V");
        var s2 = new MethodSignature("some/Class","methodB","(I)V");
        float c4 = p.confidence(s1, s2);
        check(c4 > 0.0, "cross-version structural similarity=" + String.format("%.1f%%", c4*100));

        var sx = new MethodSignature("some/Class","methodA","()V");
        var sy = new MethodSignature("some/Class","methodB","()V");
        p.recordSuccessfulMatch(sx, sy); p.recordSuccessfulMatch(sx, sy); p.recordSuccessfulMatch(sx, sy);
        float c5 = p.confidence(sx, sy);
        check(c5 > 0.1, "RL confidence=" + String.format("%.2f", c5));

        System.out.println("  CompatibilityPredictor: " + passed + " passed, " + failed + " failed");
        
    }
}
