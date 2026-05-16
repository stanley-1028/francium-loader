package com.francium;

import com.francium.resolver.model.*;
import com.francium.resolver.sat.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SATDependencyResolverTest {
    static int passed = 0, failed = 0;
    static void check(boolean c, String m) {
        if (!c) { failed++; System.out.println("  FAIL " + m); }
        else { passed++; System.out.println("  PASS " + m); }
    }
    static SemanticVersion v(String s) { return SemanticVersion.parse(s); }
    static DependencyConstraint c(String s) { return new DependencyConstraint(s); }

    @Test
    public void testAll() {
        var solver = new SATDependencyResolver();
        solver.registerVersions("A", List.of(v("1.0.0")));
        solver.registerVersions("B", List.of(v("1.2.0"),v("1.4.0"),v("1.5.0")));
        solver.registerVersions("C", List.of(v("2.0.0"),v("3.0.0")));
        solver.registerVersions("D", List.of(v("1.0.0")));
        solver.registerDependencies("A", Map.of("B",c(">=1.4.0"),"C",c(">=2.0.0")));
        solver.registerDependencies("D", Map.of("B",c(">=1.0.0")));
        var result = solver.solve(List.of("A","D"));
        check(result.success, "SAT solve successful");
        check(result.solution != null && result.solution.containsKey("B"), "B resolved");
        check(result.solution != null && result.solution.get("B").toString().startsWith("1."), "B=" + (result.solution != null ? result.solution.get("B") : "null"));
        System.out.println("    " + result);

        solver = new SATDependencyResolver();
        solver.registerVersions("A", List.of(v("1.0.0")));
        solver.registerVersions("B", List.of(v("1.0.0")));
        solver.registerDependencies("A", Map.of("B",c(">=2.0.0")));
        result = solver.solve(List.of("A"));
        check(!result.success, "no solution for impossible constraint");

        solver = new SATDependencyResolver();
        solver.registerVersions("A", List.of(v("1.0.0")));
        solver.registerVersions("B", List.of(v("1.5.0")));
        solver.registerVersions("C", List.of(v("2.0.0"),v("3.0.0")));
        solver.registerDependencies("A", Map.of("B",c(">=1.0.0")));
        solver.registerDependencies("B", Map.of("C",c(">=2.0.0")));
        result = solver.solve(List.of("A"));
        check(result.success, "transitive deps resolved");
        check(result.solution != null && result.solution.containsKey("C"), "at least A, B, C resolved");
        System.out.println("    " + result);

        solver = new SATDependencyResolver(); solver.setTimeoutMs(5000);
        Random rng = new Random(42);
        for (int i=0;i<50;i++) {
            List<SemanticVersion> vers = new ArrayList<>();
            for (int j=0;j<3;j++) vers.add(new SemanticVersion(1,rng.nextInt(10),rng.nextInt(20)));
            solver.registerVersions("mod"+i, vers);
            if (i>0) solver.registerDependencies("mod"+i, Map.of("mod"+rng.nextInt(i),new DependencyConstraint("*")));
        }
        long t0 = System.currentTimeMillis();
        result = solver.solve(List.of("mod49"));
        long ms = System.currentTimeMillis()-t0;
        check(ms < 5000, "50 mods resolved in " + ms + "ms (<5s)");
        System.out.println("    50 mods: " + ms + "ms, nodes=" + result.nodesExplored);

        System.out.println("  SATResolver: " + passed + " passed, " + failed + " failed");
        
    }
}
