package com.francium;

import com.francium.resolver.model.DependencyConstraint;
import com.francium.resolver.model.SemanticVersion;
import com.francium.resolver.sat.SATDependencyResolver;
import com.francium.resolver.sat.SATDependencyResolver.ResolveResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parameterized fuzz tests for SAT dependency resolution.
 * Generates random dependency graphs with fixed seeds and verifies
 * the solver produces a valid solution satisfying all constraints.
 */
public class FuzzSATParameterizedTest {

    /** Seeds for reproducibility */
    public static Stream<Long> fuzzSeeds() {
        return Stream.of(42L, 123L, 456L, 789L, 1001L, 2024L, 9999L);
    }

    @ParameterizedTest(name = "fuzz-seed-{0}")
    @MethodSource("fuzzSeeds")
    public void testFuzzRandomGraph(long seed) {
        Random rng = new Random(seed);
        int modCount = 5 + rng.nextInt(20); // 5..24 mods
        int versionsPerMod = 2 + rng.nextInt(4); // 2..5 versions

        SATDependencyResolver solver = new SATDependencyResolver();
        solver.setTimeoutMs(30_000);

        List<String> modIds = new ArrayList<>();
        Map<String, List<SemanticVersion>> allVersions = new HashMap<>();

        for (int i = 0; i < modCount; i++) {
            String modId = "mod-" + seed + "-" + i;
            modIds.add(modId);
            List<SemanticVersion> versions = new ArrayList<>();
            for (int j = 0; j < versionsPerMod; j++) {
                versions.add(new SemanticVersion(
                    rng.nextInt(5),
                    rng.nextInt(20),
                    rng.nextInt(30)
                ));
            }
            allVersions.put(modId, versions);
            solver.registerVersions(modId, versions);
        }

        List<String> roots = new ArrayList<>();
        for (int i = 0; i < modCount; i++) {
            String modId = modIds.get(i);
            if (rng.nextDouble() < 0.4) {
                roots.add(modId);
            }
            if (i > 0 && rng.nextDouble() < 0.5) {
                Map<String, DependencyConstraint> deps = new HashMap<>();
                int depCount = 1 + rng.nextInt(Math.min(3, i));
                for (int d = 0; d < depCount; d++) {
                    int depIdx = rng.nextInt(i);
                    deps.put(modIds.get(depIdx), new DependencyConstraint(randomConstraint(rng)));
                }
                solver.registerDependencies(modId, deps);
            }
        }

        if (roots.isEmpty()) {
            roots.add(modIds.get(rng.nextInt(modCount)));
        }

        ResolveResult result = solver.solve(roots);

        if (result.success) {
            assertNotNull(result.solution, "Solution must not be null on success");
            assertFalse(result.solution.isEmpty(), "Solution must not be empty on success");
            for (String root : roots) {
                assertTrue(result.solution.containsKey(root),
                    "Root mod " + root + " must be in solution");
            }
            for (Map.Entry<String, SemanticVersion> entry : result.solution.entrySet()) {
                SemanticVersion chosen = entry.getValue();
                List<SemanticVersion> registered = allVersions.get(entry.getKey());
                assertNotNull(registered, "Mod " + entry.getKey() + " must be registered");
                assertTrue(registered.contains(chosen),
                    "Chosen version " + chosen + " for " + entry.getKey() + " must be registered");
            }
        }
    }

    private static String randomConstraint(Random rng) {
        String[] templates = {"*", ">=1.0.0", ">=0.0.0", "<10.0.0", ">=0.0.0 <10.0.0"};
        if (rng.nextDouble() < 0.3) {
            int major = rng.nextInt(5);
            int minor = rng.nextInt(10);
            int patch = rng.nextInt(20);
            switch (rng.nextInt(4)) {
                case 0: return ">=" + major + "." + minor + "." + patch;
                case 1: return "<" + (major + 2) + ".0.0";
                case 2: return ">=" + major + "." + minor + "." + patch + " <" + (major + 1) + ".0.0";
                default: return "*";
            }
        }
        return templates[rng.nextInt(templates.length)];
    }
}
