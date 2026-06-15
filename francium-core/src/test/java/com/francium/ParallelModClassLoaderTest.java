package com.francium;

import com.francium.classloader.ParallelModClassLoader;
import com.francium.classloader.ParallelModClassLoader.DiscoveryResult;
import com.francium.classloader.ParallelModClassLoader.LoadStatus;
import com.francium.graph.ModGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParallelModClassLoader 單元測試。
 * 驗證掃描、狀態追蹤、並行排程與 shutdown 行為。
 */
class ParallelModClassLoaderTest {

    @TempDir
    Path tempDir;

    // ─── helpers ──────────────────────────────────────────────

    /** 建立一個含 francium-mod.json 的 JAR 檔案供測試用。 */
    private File createJarWithManifest(String name, String modId) throws IOException {
        File jar = tempDir.resolve(name).toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("francium-mod.json"));
            String json = "{\"modId\":\"" + modId + "\",\"version\":\"1.0.0\",\"mainClass\":\"com.example." + modId + "\"}";
            jos.write(json.getBytes());
            jos.closeEntry();
        }
        return jar;
    }

    @Test
    void discoverModsFindsJarFiles() throws IOException {
        createJarWithManifest("test-mod.jar", "test-mod");
        createJarWithManifest("another.jar", "another");

        ModGraph graph = new ModGraph();
        ParallelModClassLoader loader = new ParallelModClassLoader(graph, tempDir);

        DiscoveryResult result = loader.discoverMods();
        assertEquals(2, result.totalJars, "Should find 2 jars");
        assertFalse(result.found.isEmpty(), "Should discover mods");
        loader.shutdown();
    }

    @Test
    void discoverModsHandlesEmptyDirectory(@TempDir Path emptyDir) throws IOException {
        ModGraph graph = new ModGraph();
        ParallelModClassLoader loader = new ParallelModClassLoader(graph, emptyDir);

        DiscoveryResult result = loader.discoverMods();
        assertEquals(0, result.totalJars, "Empty dir should have 0 jars");
        loader.shutdown();
    }

    @Test
    void discoverModsThrowsOnMissingDirectory() {
        Path missing = tempDir.resolve("nonexistent");
        ModGraph graph = new ModGraph();
        ParallelModClassLoader loader = new ParallelModClassLoader(graph, missing);

        assertThrows(IOException.class, loader::discoverMods,
            "Missing directory should throw IOException");
        loader.shutdown();
    }

    @Test
    void loadStatusDefaultIsPending() {
        ModGraph graph = new ModGraph();
        ParallelModClassLoader loader = new ParallelModClassLoader(graph, tempDir);

        assertEquals(LoadStatus.PENDING, loader.getStatus("unknown-mod"),
            "Unknown mod should be PENDING");
        loader.shutdown();
    }

    @Test
    void shutdownCompletesGracefully() {
        ModGraph graph = new ModGraph();
        ParallelModClassLoader loader = new ParallelModClassLoader(graph, tempDir);

        // Should not throw or hang
        loader.shutdown();
        loader.shutdown(); // double shutdown should be safe
    }

    @Test
    void discoverModsSkipsNonJarFiles(@TempDir Path tmp) throws IOException {
        tmp.resolve("readme.txt").toFile().createNewFile();
        tmp.resolve("data.zip").toFile().createNewFile();

        ModGraph graph = new ModGraph();
        ParallelModClassLoader loader = new ParallelModClassLoader(graph, tmp);

        DiscoveryResult result = loader.discoverMods();
        assertEquals(0, result.totalJars, "Non-jar files should not be counted");
        loader.shutdown();
    }

    @Test
    void getLoadTimesReturnsEmptyMapBeforeLoad() {
        ModGraph graph = new ModGraph();
        ParallelModClassLoader loader = new ParallelModClassLoader(graph, tempDir);

        assertTrue(loader.getLoadTimes().isEmpty(),
            "Load times should be empty before any load");
        loader.shutdown();
    }
}
