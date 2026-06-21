package com.francium.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void installDownloadsModAndWritesLockFile() throws Exception {
        byte[] modBytes = "fake mod jar".getBytes(StandardCharsets.UTF_8);

        try (TestRegistry registry = new TestRegistry()) {
            registry.respond("/mod/example-mod", """
                {
                  "modId": "example-mod",
                  "versions": [
                    {
                      "version": "1.0.0",
                      "downloadUrl": "%s/files/example-mod.jar",
                      "sha256": "%s",
                      "dependencies": {},
                      "optionalDependencies": {}
                    }
                  ]
                }
                """.formatted(registry.baseUrl(), sha256(modBytes)));
            registry.respond("/files/example-mod.jar", modBytes);
            registry.start();

            Path modsDir = tempDir.resolve("mods");
            PackageManager manager = packageManagerUsing(registry.baseUrl(), modsDir);

            PackageManager.InstallReport report = manager.install("example-mod", null);

            assertTrue(report.success, report.errors::toString);
            assertTrue(Files.exists(modsDir.resolve("example-mod-1.0.0.jar")));
            assertTrue(Files.exists(modsDir.resolve("francium-lock.json")));
        }
    }

    @Test
    void installDoesNotWriteLockFileWhenDownloadUrlIsMissing() throws Exception {
        try (TestRegistry registry = new TestRegistry()) {
            registry.respond("/mod/broken-mod", """
                {
                  "modId": "broken-mod",
                  "versions": [
                    {
                      "version": "1.0.0",
                      "dependencies": {},
                      "optionalDependencies": {}
                    }
                  ]
                }
                """);
            registry.start();

            Path modsDir = tempDir.resolve("mods");
            PackageManager manager = packageManagerUsing(registry.baseUrl(), modsDir);

            PackageManager.InstallReport report = manager.install("broken-mod", null);

            assertFalse(report.success);
            assertFalse(Files.exists(modsDir.resolve("francium-lock.json")));
        }
    }

    @SuppressWarnings("unchecked")
    private PackageManager packageManagerUsing(String registryUrl, Path modsDir) throws Exception {
        PackageManager manager = new PackageManager(modsDir, tempDir.resolve("cache"));
        Field registries = PackageManager.class.getDeclaredField("registries");
        registries.setAccessible(true);
        List<String> values = (List<String>) registries.get(manager);
        values.clear();
        values.add(registryUrl);
        return manager;
    }

    private static String sha256(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static class TestRegistry implements AutoCloseable {
        private final HttpServer server;

        TestRegistry() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void respond(String path, String body) {
            respond(path, body.getBytes(StandardCharsets.UTF_8));
        }

        void respond(String path, byte[] body) {
            server.createContext(path, exchange -> {
                exchange.sendResponseHeaders(200, body.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
        }

        void start() {
            server.start();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
