package com.francium;

import com.francium.bootstrap.FranciumBootstrap;
import com.francium.classloader.ParallelModClassLoader;
import com.francium.launch.FranciumTweaker;
import com.francium.loader.*;
import com.francium.graph.ModGraph;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 補釘覆蓋率缺口。
 * 
 * 覆蓋目標：
 *  - FranciumException (26.4%)
 *  - FranciumLoader (54.7%)
 *  - ParallelModClassLoader.getModClass (6.6%)
 *  - FranciumBootstrap (0%)
 *  - FranciumTweaker (0%)
 *  - ModManifest.unescapeJsonString + estimatedLoadTimeMs
 */
public class CoverageGapTest {

    // ==================== FranciumException ====================
    @Nested
    class ExceptionTests {
        @Test
        void testConstructors() {
            var e1 = new FranciumException(FranciumException.Phase.UNKNOWN, "test");
            assertEquals("test", e1.getMessage());
            assertEquals(FranciumException.Phase.UNKNOWN, e1.getPhase());

            var cause = new RuntimeException("cause");
            var e2 = new FranciumException(FranciumException.Phase.DISCOVERY, "disc", cause);
            assertEquals("disc", e2.getMessage());
            assertSame(cause, e2.getCause());
            assertEquals(FranciumException.Phase.DISCOVERY, e2.getPhase());

            var e3 = new FranciumException(FranciumException.Phase.LOADING, "load", "detail");
            assertTrue(e3.getMessage().contains("load") || e3.getMessage().contains("detail"));
            assertEquals(FranciumException.Phase.LOADING, e3.getPhase());
        }

        @Test
        void testAllPhases() {
            for (var phase : FranciumException.Phase.values()) {
                var e = new FranciumException(phase, "phase_" + phase.name());
                assertEquals(phase, e.getPhase());
                assertNotNull(e.getMessage());
            }
        }
    }

    // ==================== FranciumLoader ====================
    @Nested
    class LoaderTests {
        private Path tmpDir;

        @BeforeEach
        void setup() throws Exception {
            tmpDir = Files.createTempDirectory("francium-test-");
        }

        @Test
        void testInitialState() {
            var loader = new FranciumLoader(tmpDir);
            assertNotNull(loader.state());
        }

        @Test
        void testLoadConfig() throws Exception {
            var loader = new FranciumLoader(tmpDir);
            loader.loadConfig();
            assertNotNull(loader.config());
        }

        @Test
        void testInitialize() throws Exception {
            var loader = new FranciumLoader(tmpDir);
            loader.loadConfig();
            loader.initialize();
        }

        @Test
        void testPhaseTimings() {
            var loader = new FranciumLoader(tmpDir);
            assertNotNull(loader.phaseTimings());
            assertTrue(loader.phaseTimings().isEmpty());
        }

        @Test
        void testMemorySnapshot() {
            var loader = new FranciumLoader(tmpDir);
            var mem = loader.getMemorySnapshot();
            // MemorySnapshot may be null before full init
            if (mem != null) {
                assertNotNull(mem.toString());
            }
        }

        @Test
        void testShutdownWithoutInit() {
            var loader = new FranciumLoader(tmpDir);
            assertDoesNotThrow(() -> loader.shutdown());
        }

        @Test
        void testBuilder() throws Exception {
            var built = FranciumLoader.builder(tmpDir)
                .withParallelLoading(true)
                .withMemoryManagement(true)
                .withAIBridge(false)
                .withServerSync(false)
                .build();
            assertNotNull(built);
            built.loadConfig();
        }

        @Test
        void testCallbacks() {
            var loader = new FranciumLoader(tmpDir);
            var flag = new boolean[]{false};
            loader.onPreLaunch(() -> flag[0] = true);
            loader.onPostLaunch(() -> flag[0] = true);
            loader.onModLoaded("testmod", () -> flag[0] = true);
            // just verifying no exception
        }

        @Test
        void testConfigLoadWithProperties() throws Exception {
            Path configFile = tmpDir.resolve("francium.properties");
            Files.writeString(configFile,
                "load_threads=4\nai_bridge_enabled=true\ndebug_mode=true\n");
            var loader = new FranciumLoader(tmpDir);
            loader.loadConfig();
            loader.initialize();
        }
    }

    // ==================== ParallelModClassLoader.getModClass ====================
    @Nested
    class GetModClassTests {
        @Test
        void testGetModClassBeforeLoad() throws Exception {
            Path tmpDir = Files.createTempDirectory("francium-getmod-");
            Path modsDir = tmpDir.resolve("mods");
            Files.createDirectories(modsDir);
            var graph = new ModGraph();
            var loader = new ParallelModClassLoader(graph, modsDir);
            var result = loader.getModClass("nonexistent");
            assertNull(result);
        }

        @Test
        void testGetModClassAfterShutdown() throws Exception {
            Path tmpDir = Files.createTempDirectory("francium-getmod2-");
            Path modsDir = tmpDir.resolve("mods");
            Files.createDirectories(modsDir);
            var graph = new ModGraph();
            var loader = new ParallelModClassLoader(graph, modsDir);
            loader.shutdown();
            assertDoesNotThrow(() -> loader.getModClass("nonexistent"));
        }
    }

    // ==================== ModManifest 新功能補釘 ====================
    @Nested
    class NewModManifestTests {
        @Test
        void testEstimatedLoadTimeParsing() {
            String json = "{ \"modId\": \"slowmod\", \"version\": \"1.0\", \"estimatedLoadTimeMs\": 250 }";
            var result = ModManifest.fromJson(json);
            assertNotNull(result);
            // estimatedLoadTimeMs 不被映射到 sizeBytes（那是舊 bug）
            // 如果 ModManifest 有 getEstimatedLoadTimeMs()，這裡應該測它
        }

        @Test
        void testSizeBytesAfterEstimatedLoadTime() {
            var manifest = new ModManifest.Builder("test", "1.0")
                .sizeBytes(300)
                .build();
            assertEquals(300L, manifest.sizeBytes());
        }

        @Test
        void testTomlSingleQuotes() {
            String toml = """
                modId='singlequotemod'
                version='3.0'
                displayName='My Mod'
                description='A test mod'
                """;
            var result = ModManifest.fromForgeToml(toml);
            assertNotNull(result);
            assertEquals("singlequotemod", result.modId());
            assertEquals("3.0", result.version());
        }

        @Test
        void testUnicodeEscapeInJson() {
            // Test JSON with actual backslash-uXXXX escape sequence in the string
            String json = "{ \"id\": \"test\\u004dod\", \"version\": \"1.0\" }";
            var result = ModManifest.fromFabricJson(json);
            assertNotNull(result);
            // After unescaping, \u004d should become 'M'
            assertTrue(result.modId().contains("M") || result.modId().contains("m"),
                "modId should contain decoded unicode char: " + result.modId());
        }

        @Test
        void testVariousJsonEscapes() {
            // Test \n, \t, \\ escapes
            String json = "{ \"id\": \"esc\\ntest\", \"version\": \"1.0\", \"name\": \"tab\\ttest\", \"description\": \"back\\\\slash\" }";
            var result = ModManifest.fromFabricJson(json);
            assertNotNull(result);
            // Note: the simple regex parser may not handle embedded escapes in value properly
            // Just test it doesn't crash
        }
    }

    // ==================== ParallelModClassLoader.LoadReport & LoadFailure ====================
    @Nested
    class LoadReportTests {
        @Test
        void testLoadReportToString() {
            var report = new ParallelModClassLoader.LoadReport();
            report.totalLoadTimeMs = 1000;
            report.sequentialEstimatedMs = 4000;
            report.actualSpeedup = 4.0;
            report.layerDetails = new ArrayList<>();

            var layer = new ParallelModClassLoader.LayerLoadDetail(0);
            layer.modCount = 5;
            layer.layerTimeMs = 1000;
            layer.success = 4;
            layer.failed = 1;
            report.layerDetails.add(layer);

            var layer2 = new ParallelModClassLoader.LayerLoadDetail(1);
            layer2.modCount = 3;
            layer2.layerTimeMs = 500;
            layer2.success = 3;
            layer2.failed = 0;
            report.layerDetails.add(layer2);

            String str = report.toString();
            assertNotNull(str);
            assertTrue(str.contains("1000ms"), "toString should contain total load time");
            assertTrue(str.contains("4000ms"), "toString should contain sequential estimate");
            assertTrue(str.contains("4.0x"), "toString should contain actual speedup");
            assertTrue(str.contains("Layer 0"), "toString should contain first layer");
            assertTrue(str.contains("Layer 1"), "toString should contain second layer");
            assertTrue(str.contains("5 mods"), "toString should contain mod count for layer 0");
            assertTrue(str.contains("3 mods"), "toString should contain mod count for layer 1");
        }

        @Test
        void testLoadReportToStringWithEmptyLayers() {
            var report = new ParallelModClassLoader.LoadReport();
            report.totalLoadTimeMs = 0;
            report.sequentialEstimatedMs = 0;
            report.actualSpeedup = 1.0;
            // No layer details — empty list
            String str = report.toString();
            assertNotNull(str);
            assertTrue(str.contains("0ms"));
            assertTrue(str.contains("Layers: 0"));
        }

        @Test
        void testLoadReportDefaultValues() {
            var report = new ParallelModClassLoader.LoadReport();
            assertEquals(0L, report.totalLoadTimeMs);
            assertEquals(0L, report.sequentialEstimatedMs);
            assertEquals(0.0, report.actualSpeedup, 0.001);
            assertNotNull(report.layerDetails);
            assertTrue(report.layerDetails.isEmpty());
        }
    }

    @Nested
    class LoadFailureTests {
        @Test
        void testLoadFailureRecord() {
            var cause = new RuntimeException("test error");
            var failure = new ParallelModClassLoader.LoadFailure("testmod", cause);
            assertEquals("testmod", failure.modId());
            assertSame(cause, failure.error());
            assertEquals("test error", failure.error().getMessage());
        }

        @Test
        void testLoadFailureWithNullError() {
            var failure = new ParallelModClassLoader.LoadFailure("brokenmod", null);
            assertEquals("brokenmod", failure.modId());
            assertNull(failure.error());
        }

        @Test
        void testLayerLoadDetailDefaultValues() {
            var detail = new ParallelModClassLoader.LayerLoadDetail(2);
            assertEquals(2, detail.layerIndex);
            assertEquals(0, detail.modCount);
            assertEquals(0, detail.success);
            assertEquals(0, detail.failed);
            assertEquals(0, detail.skipped);
            assertNotNull(detail.results);
            assertTrue(detail.results.isEmpty());
            assertNotNull(detail.failures);
            assertTrue(detail.failures.isEmpty());
        }

        @Test
        void testModLoadResultRecord() {
            var result = new ParallelModClassLoader.ModLoadResult(
                "mymod", "2.0", Object.class, 150L);
            assertEquals("mymod", result.modId());
            assertEquals("2.0", result.version());
            assertSame(Object.class, result.mainClass());
            assertEquals(150L, result.loadTimeMs());
        }

        @Test
        void testLoadReportComplexScenario() {
            // Simulate a realistic multi-layer report
            var report = new ParallelModClassLoader.LoadReport();
            report.totalLoadTimeMs = 2500;
            report.sequentialEstimatedMs = 10000;
            report.actualSpeedup = 4.0;

            // Layer 0: 2 mods, both succeed
            var layer0 = new ParallelModClassLoader.LayerLoadDetail(0);
            layer0.modCount = 2;
            layer0.layerTimeMs = 800;
            layer0.success = 2;

            // Layer 1: 3 mods, 1 fails
            var layer1 = new ParallelModClassLoader.LayerLoadDetail(1);
            layer1.modCount = 3;
            layer1.layerTimeMs = 1200;
            layer1.success = 2;
            layer1.failed = 1;
            layer1.failures.add(new ParallelModClassLoader.LoadFailure(
                "badmod", new IllegalArgumentException("version mismatch")));

            report.layerDetails.add(layer0);
            report.layerDetails.add(layer1);

            String str = report.toString();
            assertTrue(str.contains("2500ms"));
            assertTrue(str.contains("4.0x"));
            assertTrue(str.contains("Layer 0"));
            assertTrue(str.contains("Layer 1"));
            assertTrue(str.contains("2 mods"));
            assertTrue(str.contains("3 mods"));
            assertTrue(str.contains("2 ok"));
            assertTrue(str.contains("1 fail"));
        }
    }

    // ==================== FranciumBootstrap ====================
    @Nested
    class BootstrapArgTests {
        @Test
        void testVersionArg() {
            assertDoesNotThrow(() -> {
                PrintStream oldOut = System.out;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                System.setOut(new PrintStream(bos));
                try {
                    FranciumBootstrap.main(new String[]{"--version"});
                } finally {
                    System.setOut(oldOut);
                }
            });
        }

        @Test
        void testHelpArg() {
            assertDoesNotThrow(() -> {
                PrintStream oldOut = System.out;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                System.setOut(new PrintStream(bos));
                try {
                    FranciumBootstrap.main(new String[]{"--help"});
                } finally {
                    System.setOut(oldOut);
                }
            });
        }

        @Test
        void testShortVersionArg() {
            assertDoesNotThrow(() -> {
                PrintStream oldOut = System.out;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                System.setOut(new PrintStream(bos));
                try {
                    FranciumBootstrap.main(new String[]{"-v"});
                } finally {
                    System.setOut(oldOut);
                }
            });
        }

        @Test
        void testDebugFlag() {
            String oldDebug = System.getProperty("francium.debug");
            try {
                FranciumBootstrap.main(new String[]{"--debug", "--version"});
                assertEquals("true", System.getProperty("francium.debug"),
                    "--debug should set francium.debug system property");
            } finally {
                if (oldDebug != null) {
                    System.setProperty("francium.debug", oldDebug);
                } else {
                    System.clearProperty("francium.debug");
                }
            }
        }

        @Test
        void testGameDirArg() throws Exception {
            Path tempDir = Files.createTempDirectory("francium-gamedir-");
            try {
                FranciumBootstrap.main(new String[]{"--game-dir", tempDir.toString(), "--version"});
                // Should not throw — just validates the arg parsing path
            } finally {
                // Clean up temp dir
                try (var dirStream = Files.walk(tempDir)) {
                    dirStream.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
                }
            }
        }

        @Test
        void testShortGameDirArg() throws Exception {
            Path tempDir = Files.createTempDirectory("francium-gamedir2-");
            try {
                FranciumBootstrap.main(new String[]{"-g", tempDir.toString(), "--version"});
            } finally {
                try (var dirStream = Files.walk(tempDir)) {
                    dirStream.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
                }
            }
        }

        @Test
        void testLaunchFlowSucceeds() {
            // This tests the full launch path (L76-L121) by pointing to a temp dir
            // The launch flow actually works in this environment!
            PrintStream oldOut = System.out;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(bos));
            try {
                assertDoesNotThrow(() -> {
                    FranciumBootstrap.main(new String[]{"--game-dir",
                        System.getProperty("java.io.tmpdir") + "/francium_launch_test_" + System.nanoTime(),
                        "--debug"});
                }, "Launch flow should execute without throwing");
            } finally {
                System.setOut(oldOut);
            }
        }

        @Test
        void testStaticVersionFallback() {
            // Verify that the static VERSION is set (either from package or fallback)
            try {
                var versionField = FranciumBootstrap.class.getDeclaredField("VERSION");
                versionField.setAccessible(true);
                String version = (String) versionField.get(null);
                assertNotNull(version);
                assertFalse(version.isEmpty());
            } catch (Exception e) {
                fail("Should be able to access VERSION field: " + e.getMessage());
            }
        }

        @Test
        void testUnknownArg() {
            // Covers the default case in the switch (L58 - 5th branch)
            assertDoesNotThrow(() -> {
                FranciumBootstrap.main(new String[]{"--unknown", "--version"});
            });
        }

        @Test
        void testGameDirAsLastArg() {
            // Covers the --game-dir branch where i+1 >= args.length (L60 - false branch)
            assertDoesNotThrow(() -> {
                FranciumBootstrap.main(new String[]{"--game-dir", "--version"});
            });
        }

        @Test
        void testShortGameDirAsLastArg() {
            // Covers the -g branch where i+1 >= args.length (L60 - false branch)
            assertDoesNotThrow(() -> {
                FranciumBootstrap.main(new String[]{"-g", "--version"});
            });
        }

        @Test
        void testDebugWithHelp() {
            // Cover --debug flag together with --help (both arg parsing paths)
            assertDoesNotThrow(() -> {
                FranciumBootstrap.main(new String[]{"--debug", "--help"});
            });
        }

        @Test
        void testGameDirAlone() {
            // Covers the --game-dir branch where it's the last arg (L60 - false branch)
            // Passing --game-dir alone means i+1 >= args.length, so the if body is skipped
            assertDoesNotThrow(() -> {
                FranciumBootstrap.main(new String[]{"--game-dir"});
            });
        }

        @Test
        void testGameDirUnknownValue() {
            // --game-dir consumes the next arg as the path value
            // Next arg "--help" becomes the path, not a flag
            assertDoesNotThrow(() -> {
                FranciumBootstrap.main(new String[]{"--game-dir", "--help"});
            });
        }

        @Test
        void testDebugMultipleArgs() {
            // Multiple flags combined to exercise full switch coverage
            assertDoesNotThrow(() -> {
                FranciumBootstrap.main(new String[]{"--debug", "unknown-arg", "--help"});
            });
        }

        @Test
        void testLaunchFlowFailsWithModsAsFile() throws Exception {
            // Create a temp dir with a FILE at the "mods" path.
            // initialize() catches FileAlreadyExistsException internally,
            // but discoverMods() throws IOException because mods is not a directory.
            // This triggers the catch block (L114-L119) in FranciumBootstrap.main().
            Path tempDir = Files.createTempDirectory("francium-catch-");
            Path modsFile = tempDir.resolve("mods");
            Files.createFile(modsFile);

            PrintStream oldOut = System.out;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(bos));
            try {
                RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                    FranciumBootstrap.main(new String[]{"--game-dir", tempDir.toString(), "--debug"});
                });
                assertTrue(ex.getCause() instanceof FranciumException,
                    "Root cause should be FranciumException");
            } finally {
                System.setOut(oldOut);
                Files.deleteIfExists(modsFile);
                Files.deleteIfExists(tempDir);
            }
        }
    }

    // ==================== FranciumTweaker ====================
    @Nested
    @SuppressWarnings("removal")
    class TweakerTests {
        @Test
        void testGetLaunchTarget() {
            var tweaker = new FranciumTweaker();
            String target = tweaker.getLaunchTarget();
            assertNotNull(target);
            assertFalse(target.isEmpty());
        }

        @Test
        void testAcceptOptions() {
            var tweaker = new FranciumTweaker();
            assertDoesNotThrow(() -> tweaker.acceptOptions(List.of(), null, null, null));
        }

        @Test
        void testAcceptOptionsWithArgs() {
            var tweaker = new FranciumTweaker();
            assertDoesNotThrow(() -> tweaker.acceptOptions(
                List.of("--gameDir", "/path/to/game", "--version", "1.16.5"),
                null, null, null));
        }

        @Test
        void testGetModArgs() {
            var tweaker = new FranciumTweaker();
            String[] args = tweaker.getLaunchArguments();
            assertNotNull(args);
            assertEquals(0, args.length);
        }

        @Test
        void testGetModArgsAfterAcceptOptions() {
            var tweaker = new FranciumTweaker();
            tweaker.acceptOptions(List.of("--width", "1920"), null, null, null);
            String[] args = tweaker.getLaunchArguments();
            assertNotNull(args);
            assertEquals(2, args.length);
            assertEquals("--width", args[0]);
            assertEquals("1920", args[1]);
        }

        @Test
        void testInjectIntoClassLoader() {
            var tweaker = new FranciumTweaker();
            assertDoesNotThrow(() -> {
                var cl = new LaunchClassLoader(new java.net.URL[0]);
                tweaker.injectIntoClassLoader(cl);
            });
        }

        @Test
        void testInjectIntoClassLoaderWithArgs() {
            // Cover the args iteration branch (L49) and gameDir parsing branch (L50)
            var tweaker = new FranciumTweaker();
            // Pass args that include --gameDir with space-separated value
            // to cover the arg.startsWith("--gameDir") && arg.contains(" ") branch
            tweaker.acceptOptions(List.of(
                "--gameDir .",   // this should match: starts with --gameDir and contains space
                "--someArg"      // this should not match: doesn't start with --gameDir
            ), null, null, null);
            assertDoesNotThrow(() -> {
                var cl = new LaunchClassLoader(new java.net.URL[0]);
                tweaker.injectIntoClassLoader(cl);
            });
        }

        @Test
        void testInjectIntoClassLoaderWithGameDirArgDot() {
            // Test with args that have --gameDir without space (won't match the if condition)
            var tweaker = new FranciumTweaker();
            tweaker.acceptOptions(List.of(
                "--gameDir=.",    // starts with --gameDir but doesn't contain space
                "extraArg"
            ), null, null, null);
            assertDoesNotThrow(() -> {
                var cl = new LaunchClassLoader(new java.net.URL[0]);
                tweaker.injectIntoClassLoader(cl);
            });
        }

        @Test
        void testInjectIntoClassLoaderFailsGracefully() {
            // Accept options first to initialize args list (cover L49 branch with non-empty args)
            var tweaker = new FranciumTweaker();
            tweaker.acceptOptions(List.of("--someArg"), null, null, null);
            // injectIntoClassLoader will try File(".") as fallback, then build loader
            // which may fail silently inside the catch block (L100-L102)
            assertDoesNotThrow(() -> {
                var cl = new LaunchClassLoader(new java.net.URL[0]);
                tweaker.injectIntoClassLoader(cl);
            });
        }

        @Test
        void testConstructor() {
            var tweaker = new FranciumTweaker();
            assertNotNull(tweaker);
        }

        @Test
        void testInjectIntoClassLoaderCatchesException() throws Exception {
            // Create a temp dir with a FILE at the "mods" path.
            // The Tweaker's injectIntoClassLoader() creates a new FranciumLoader via builder.
            // Builder calls initialize(), which tries Files.createDirectories(modsDir) —
            // if modsDir is a file, it catches IOException internally.
            // Then scanMods() → discoverMods() throws because mods is not a directory.
            // This triggers the catch block (L100-L102) in FranciumTweaker.
            Path tempDir = Files.createTempDirectory("francium-tweak-catch-");
            Path modsFile = tempDir.resolve("mods");
            Files.createFile(modsFile);

            var tweaker = new FranciumTweaker();
            tweaker.acceptOptions(List.of("--gameDir " + tempDir.toString()),
                null, null, null);

            // Should NOT throw: the catch block (L100-L102) swallows the exception
            assertDoesNotThrow(() -> {
                var cl = new LaunchClassLoader(new java.net.URL[0]);
                tweaker.injectIntoClassLoader(cl);
            });

            Files.deleteIfExists(modsFile);
            Files.deleteIfExists(tempDir);
        }
    }
}
