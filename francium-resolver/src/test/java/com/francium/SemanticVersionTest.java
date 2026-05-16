package com.francium;

import com.francium.resolver.model.SemanticVersion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** SemanticVersion hand-written test (runs with java, no JUnit needed) */
public class SemanticVersionTest {
    static int passed = 0, failed = 0;
    static void check(boolean c, String m) {
        if (!c) { failed++; System.out.println("  FAIL " + m); }
        else { passed++; System.out.println("  PASS " + m); }
    }
    @Test
    public void testAll() {
        var v = SemanticVersion.parse("1.20.4");
        check(v != null, "major=1"); check(v.major() == 1, "minor=20");
        check(v.minor() == 20, "patch=4"); check(v.patch() == 4, "toString=1.20.4");
        check("1.20.4".equals(v.toString()), "pre major=1");
        var pre = SemanticVersion.parse("1.20.4-pre3");
        check(pre != null, "minor=20"); check(pre.major() == 1, "patch=4");
        check("pre3".equals(pre.preRelease()), "preRelease=pre3");
        var a = SemanticVersion.parse("1.20.4");
        var b = SemanticVersion.parse("1.21.0");
        check(a.compareTo(b) < 0, "1.20.4 < 1.21.0");
        check(b.compareTo(a) > 0, "1.21.0 > 1.20.4");
        check(a.compareTo(SemanticVersion.parse("1.20.4")) == 0, "1.20.4 == 1.20.4");
        check(a.equals(SemanticVersion.parse("1.20.4")), "equals works");
        check(a.hashCode() == SemanticVersion.parse("1.20.4").hashCode(), "hashCode consistent");
        check(SemanticVersion.parse("1.20.4-rc1").compareTo(a) < 0, "pre-release < release");
        check(a.nextMajor().major() == 2, "nextMajor");
        check(a.nextMinor().minor() == 21, "nextMinor");
        check(a.nextPatch().patch() == 5, "nextPatch");
        System.out.println("  SemanticVersion: " + passed + " passed, " + failed + " failed");
        
    }
}
