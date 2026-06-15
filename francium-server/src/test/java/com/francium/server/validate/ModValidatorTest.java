package com.francium.server.validate;

import com.francium.server.validate.ModValidator.SecurityLevel;
import com.francium.server.validate.ModValidator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModValidatorTest {

    @Test
    void validateValidJarPassesIntegrity() throws Exception {
        ModValidator validator = new ModValidator(SecurityLevel.INTEGRITY);
        Path tempFile = Files.createTempFile("mod-", ".jar");
        try {
            Files.writeString(tempFile, "valid content");
            ValidationResult result = validator.validate(tempFile);
            assertTrue(result.integrityPassed);
            assertTrue(result.passed);
            assertNotNull(result.sha256);
            assertEquals(64, result.sha256.length());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void validateBlockedPatternFails() throws Exception {
        ModValidator validator = new ModValidator(SecurityLevel.INTEGRITY);
        validator.blockMod("hack");

        Path tempFile = Files.createTempFile("hack-client", ".jar");
        try {
            Files.writeString(tempFile, "content");
            ValidationResult result = validator.validate(tempFile);
            assertFalse(result.passed);
            assertTrue(result.errors.stream().anyMatch(e -> e.contains("blocked")));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void validateNonBlockedPatternPasses() throws Exception {
        ModValidator validator = new ModValidator(SecurityLevel.INTEGRITY);
        validator.blockMod("hack");

        Path tempFile = Files.createTempFile("sodium-", ".jar");
        try {
            Files.writeString(tempFile, "content");
            ValidationResult result = validator.validate(tempFile);
            assertTrue(result.passed);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void validateNonExistentFileFails() {
        ModValidator validator = new ModValidator(SecurityLevel.INTEGRITY);
        ValidationResult result = validator.validate(Path.of("does-not-exist.jar"));
        assertFalse(result.passed);
    }

    @Test
    void validateAllScansDirectory() throws Exception {
        ModValidator validator = new ModValidator(SecurityLevel.INTEGRITY);
        Path dir = Files.createTempDirectory("mods-test");
        try {
            Files.writeString(dir.resolve("mod-a.jar"), "data a");
            Files.writeString(dir.resolve("mod-b.jar"), "data b");
            Files.writeString(dir.resolve("readme.txt"), "not a jar");

            List<ValidationResult> results = validator.validateAll(dir);
            assertEquals(2, results.size(), "Should only find .jar files");
        } finally {
            Files.walk(dir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    void validateAllEmptyDirectory() throws IOException {
        ModValidator validator = new ModValidator(SecurityLevel.INTEGRITY);
        Path dir = Files.createTempDirectory("empty-mods");
        try {
            List<ValidationResult> results = validator.validateAll(dir);
            assertTrue(results.isEmpty());
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    @Test
    void validationResultDefaults() {
        ValidationResult result = new ValidationResult("test.jar");
        assertEquals("test.jar", result.fileName);
        assertFalse(result.passed);
        assertTrue(result.errors.isEmpty());
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    void validationResultToString() {
        ValidationResult result = new ValidationResult("my-mod.jar");
        result.sha256 = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        result.passed = true;
        String str = result.toString();
        assertTrue(str.contains("my-mod.jar"));
        assertTrue(str.startsWith("\u2713")); // checkmark for passed
    }

    @Test
    void trustSignerDoesNotThrow() {
        ModValidator validator = new ModValidator(SecurityLevel.INTEGRITY);
        validator.trustSigner("mojang");
        // Verifying the method doesn't throw — no public API to read trusted signers
    }

    @Test
    void behaviorLevelChecksSensitiveAPIs() throws Exception {
        ModValidator validator = new ModValidator(SecurityLevel.BEHAVIOR);
        Path tempFile = Files.createTempFile("safe-mod", ".jar");
        try {
            // 建立真正的 JAR 檔案，內含 .class 條目
            try (var jos = new java.util.jar.JarOutputStream(
                    new java.io.FileOutputStream(tempFile.toFile()))) {
                jos.putNextEntry(new java.util.jar.JarEntry("com/example/Test.class"));
                // 模擬 class 檔案內容中包含 Runtime 引用
                jos.write("java/lang/Runtime normal content".getBytes());
                jos.closeEntry();
            }
            ValidationResult result = validator.validate(tempFile);
            assertTrue(result.passed);
            assertTrue(result.warnings.stream().anyMatch(w -> w.contains("Runtime")),
                "Should detect Runtime usage in .class entry");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
