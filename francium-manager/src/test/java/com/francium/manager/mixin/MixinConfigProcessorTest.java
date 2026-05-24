package com.francium.manager.mixin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MixinConfigProcessorTest {

    @TempDir
    Path tempDir;

    private MixinConfigProcessor processor;
    private File modsDir;

    @BeforeEach
    void setUp() {
        processor = new MixinConfigProcessor();
        modsDir = tempDir.toFile();
    }

    @Test
    void discoversMixinConfigInJar() throws IOException {
        createJarWithMixins(modsDir, "test-mod.jar", """
                {
                    "package": "com.example.mixin",
                    "mixins": ["ExampleMixin"],
                    "client": ["ClientMixin"],
                    "server": ["ServerMixin"]
                }
                """);

        int count = processor.discoverAndRegister(modsDir);
        assertEquals(1, count, "Should discover one mixin config");

        assertTrue(
            MixinConfigProcessor.getRegisteredConfigs().contains("mixins.json"),
            "Should register mixins.json"
        );
    }

    @Test
    void discoversFranciumMixinConfig() throws IOException {
        createCustomJar(modsDir, "francium-mod.jar", "francium.mixins.json", """
                {
                    "package": "com.francium.mixin",
                    "mixins": ["FranciumMixin"]
                }
                """);

        int count = processor.discoverAndRegister(modsDir);
        assertEquals(1, count);
    }

    @Test
    void emptyDirectoryReturnsZero() {
        int count = processor.discoverAndRegister(modsDir);
        assertEquals(0, count, "Empty dir should return 0");
    }

    @Test
    void jarWithoutMixinConfigReturnsZero() throws IOException {
        createCustomJar(modsDir, "no-mixin.jar", "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n");
        int count = processor.discoverAndRegister(modsDir);
        assertEquals(0, count, "JAR without mixins.json should not register");
    }

    @Test
    void parseMixinClassesReturnsAllEntries() throws IOException {
        File jar = createJarWithMixins(modsDir, "full-mod.jar", """
                {
                    "package": "com.example.mixin",
                    "mixins": ["MixinA", "MixinB"],
                    "client": ["ClientMixin"],
                    "server": ["ServerMixin"]
                }
                """);

        List<String> classes = processor.parseMixinClasses(jar, "mixins.json");
        assertEquals(4, classes.size(), "Should extract all 4 mixin classes");
        assertTrue(classes.contains("MixinA"));
        assertTrue(classes.contains("MixinB"));
        assertTrue(classes.contains("ClientMixin"));
        assertTrue(classes.contains("ServerMixin"));
    }

    @Test
    void parseMixinClassesReturnsEmptyForMissingConfig() throws IOException {
        File jar = createCustomJar(modsDir, "empty.jar", "dummy.txt", "hello");
        List<String> classes = processor.parseMixinClasses(jar, "mixins.json");
        assertTrue(classes.isEmpty(), "Missing config should return empty list");
    }

    @Test
    void parseMixinClassesSkipsMissingClientAndServerKeys() throws IOException {
        File jar = createJarWithMixins(modsDir, "minimal-mod.jar", """
                {
                    "package": "com.example.mixin",
                    "mixins": ["OnlyMixin"]
                }
                """);

        List<String> classes = processor.parseMixinClasses(jar, "mixins.json");
        assertEquals(1, classes.size(), "Only mixins array entries should be returned");
        assertEquals("OnlyMixin", classes.getFirst());
    }

    @Test
    void registerConfigAddsToGlobalSet() {
        // Register a unique config name to avoid interference from other tests
        String unique = "custom-" + System.nanoTime() + ".mixins.json";
        assertFalse(MixinConfigProcessor.getRegisteredConfigs().contains(unique));
        MixinConfigProcessor.registerConfig(unique);
        assertTrue(MixinConfigProcessor.getRegisteredConfigs().contains(unique));
    }

    @Test
    void discoversMultipleJarsEachWithMixinConfigs() throws IOException {
        createJarWithMixins(modsDir, "mod-a.jar", """
                {"package":"mod.a","mixins":["A"]}
                """);
        createJarWithMixins(modsDir, "mod-b.jar", """
                {"package":"mod.b","client":["B"]}
                """);
        createJarWithMixins(modsDir, "mod-c.jar", """
                {"package":"mod.c","server":["C"]}
                """);

        int count = processor.discoverAndRegister(modsDir);
        assertEquals(3, count, "Should discover all three mixin configs");
    }

    @Test
    void invalidJsonContentIsSkipped() throws IOException {
        createJarWithMixins(modsDir, "bad-mod.jar", "not valid json");
        int count = processor.discoverAndRegister(modsDir);
        assertEquals(0, count, "Invalid JSON should be skipped");
    }

    @Test
    void nonJarFilesAreSkipped() throws IOException {
        File txt = modsDir.toPath().resolve("readme.txt").toFile();
        txt.createNewFile();
        File zip = modsDir.toPath().resolve("data.zip").toFile();
        zip.createNewFile();

        int count = processor.discoverAndRegister(modsDir);
        assertEquals(0, count, "Non-jar files should be skipped");
    }

    // --- helpers ---

    private File createJarWithMixins(File dir, String jarName, String mixinsJson) throws IOException {
        return createCustomJar(dir, jarName, "mixins.json", mixinsJson);
    }

    private File createCustomJar(File dir, String jarName, String entryName, String content) throws IOException {
        File jar = new File(dir, jarName);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            jos.putNextEntry(new JarEntry(entryName));
            jos.write(content.getBytes("UTF-8"));
            jos.closeEntry();
        }
        return jar;
    }

    @AfterEach
    void tearDown() {
        // Reset global state so tests don't interfere with each other
        // (the registered configs set is static)
    }
}
