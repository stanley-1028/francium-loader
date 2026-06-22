package com.francium;

import com.francium.ai.adapter.VersionBridge;
import com.francium.ai.adapter.VersionBridge.BridgeSummary;
import com.francium.ai.adapter.VersionBridge.BridgeReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class VersionBridgeCacheTest {

    @TempDir
    Path tempDir;

    private Path createDummyModJar(String modId) throws Exception {
        Path jarPath = tempDir.resolve(modId + "-1.0.0.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            // Write a manifest or some content to make it a valid jar
            JarEntry entry = new JarEntry("META-INF/francium-mod.json");
            jos.putNextEntry(entry);
            String json = "{\"modId\": \"" + modId + "\", \"version\": \"1.0.0\"}";
            jos.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jarPath;
    }

    @Test
    public void testAIBridgeCachingFlow() throws Exception {
        Path cacheDir = tempDir.resolve(".francium-cache");
        Files.createDirectories(cacheDir);

        Path dummyMod = createDummyModJar("testmod");

        // 1. First run: No cache, should run analysis and save to cache
        VersionBridge bridge1 = new VersionBridge("1.20.4", "1.21");
        bridge1.setCacheDir(cacheDir);

        Map<String, Path> mods = new LinkedHashMap<>();
        mods.put("testmod", dummyMod);

        BridgeSummary summary1 = bridge1.bridgeAll(mods);
        assertNotNull(summary1);
        assertEquals("1.20.4", summary1.sourceVersion);
        assertEquals("1.21", summary1.targetVersion);

        // Verify cache files are created
        Path metaFile = cacheDir.resolve("testmod_bridge_meta.json");
        assertTrue(Files.exists(metaFile), "Metadata cache file should be created");

        // 2. Second run: Cache exists, should hit the cache!
        VersionBridge bridge2 = new VersionBridge("1.20.4", "1.21");
        bridge2.setCacheDir(cacheDir);

        BridgeSummary summary2 = bridge2.bridgeAll(mods);
        assertNotNull(summary2);
        assertEquals(summary1.reports.size(), summary2.reports.size());
        
        BridgeReport r1 = summary1.reports.get(0);
        BridgeReport r2 = summary2.reports.get(0);
        assertEquals(r1.compatibilityScore, r2.compatibilityScore);
        assertEquals(r1.totalExternalCalls, r2.totalExternalCalls);

        // 3. Third run: Mod JAR changes (hash mismatch), should invalidate cache and re-analyze
        String metaContentBefore = Files.readString(metaFile, java.nio.charset.StandardCharsets.UTF_8);

        // Create new JAR with different content
        Files.delete(dummyMod);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(dummyMod.toFile()))) {
            JarEntry entry = new JarEntry("META-INF/francium-mod.json");
            jos.putNextEntry(entry);
            String json = "{\"modId\": \"testmod\", \"version\": \"2.0.0-different\"}";
            jos.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        // Run bridge again
        VersionBridge bridge3 = new VersionBridge("1.20.4", "1.21");
        bridge3.setCacheDir(cacheDir);
        
        BridgeSummary summary3 = bridge3.bridgeAll(mods);
        assertNotNull(summary3);
        
        // The cache should have been updated with the new hash!
        String metaContentAfter = Files.readString(metaFile, java.nio.charset.StandardCharsets.UTF_8);
        assertNotEquals(metaContentBefore, metaContentAfter, "Cache metadata should be updated with new JAR info (new hash)");
    }
}
