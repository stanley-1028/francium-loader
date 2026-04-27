package com.francium;

import com.francium.resolver.model.DependencyConstraint;
import com.francium.resolver.model.SemanticVersion;
import java.util.List;

public class DependencyConstraintTest {
    static int passed = 0, failed = 0;
    static void check(boolean c, String m) {
        if (!c) { failed++; System.out.println("  FAIL " + m); }
        else { passed++; System.out.println("  PASS " + m); }
    }
    public static void main(String[] args) {
        var c = new DependencyConstraint("1.20.4");
        check(c.satisfiedBy(SemanticVersion.parse("1.20.4")), "exact match");
        check(!c.satisfiedBy(SemanticVersion.parse("1.21.0")), "exact reject");
        var ge = new DependencyConstraint(">=1.20.0");
        check(ge.satisfiedBy(SemanticVersion.parse("1.20.4")), ">= 1.20.4 ok");
        check(ge.satisfiedBy(SemanticVersion.parse("2.0.0")), ">= 2.0.0 ok");
        check(!ge.satisfiedBy(SemanticVersion.parse("1.19.2")), ">= reject 1.19.2");
        var lt = new DependencyConstraint("<1.20.4");
        check(lt.satisfiedBy(SemanticVersion.parse("1.19.2")), "< 1.20.4 ok");
        check(!lt.satisfiedBy(SemanticVersion.parse("2.0.0")), "< reject 2.0.0");
        var range = new DependencyConstraint(">=1.20.0 <2.0.0");
        check(range.satisfiedBy(SemanticVersion.parse("1.20.4")), "range 1.20.4 ok");
        check(!range.satisfiedBy(SemanticVersion.parse("2.0.0")), "range reject 2.0.0");
        check(!range.satisfiedBy(SemanticVersion.parse("1.19.2")), "range reject 1.19.2");
        var caret = new DependencyConstraint("^1.5.0");
        check(caret.satisfiedBy(SemanticVersion.parse("1.9.9")), "^ caret 1.5.0 ok");
        check(!caret.satisfiedBy(SemanticVersion.parse("2.0.0")), "^ caret reject 2.0.0");
        var wc = new DependencyConstraint("*");
        check(wc.satisfiedBy(SemanticVersion.parse("1.20.4")), "* wildcard ok");
        check(wc.isWildcard(), "* isWildcard");
        var bm = new DependencyConstraint(">=1.20.0");
        var best = bm.bestMatch(List.of(SemanticVersion.parse("1.19.2"),SemanticVersion.parse("1.20.4"),SemanticVersion.parse("1.21.0"),SemanticVersion.parse("2.0.0")));
        check(best != null && "2.0.0".equals(best.toString()), "bestMatch=2.0.0");
        var i1 = new DependencyConstraint(">=1.20.0 <2.0.0");
        var i2 = new DependencyConstraint(">=1.21.0");
        var inter = i1.intersect(i2);
        check(inter != null && inter.satisfiedBy(SemanticVersion.parse("1.21.0")), "intersection 1.21.0 ok");
        check(inter == null || !inter.satisfiedBy(SemanticVersion.parse("1.20.0")), "intersection reject 1.20.0");
        System.out.println("  DependencyConstraint: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
