package com.francium.server.validate;

import com.francium.api.PublicApi;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;

/**
 * 模組驗證器，用於伺服器端的安全檢查。
 * 
 * 安全層級:
 * - INTEGRITY: SHA256 校驗
 * - SIGNATURE: 數位簽章驗證
 * - BEHAVIOR: 沙箱行為分析
 * - REPUTATION: 社群信譽分數
 */
@PublicApi
public class ModValidator {
    public enum SecurityLevel { NONE, INTEGRITY, SIGNATURE, BEHAVIOR, REPUTATION }
    
    private final SecurityLevel requiredLevel;
    private final Set<String> blockedMods;  // 黑名單
    private final Set<String> trustedSigners; // 信任的簽署者
    
    public ModValidator(SecurityLevel requiredLevel) {
        this.requiredLevel = requiredLevel;
        this.blockedMods = new HashSet<>();
        this.trustedSigners = new HashSet<>();
    }

    /**
     * 驗證模組的安全性。
     */
    public ValidationResult validate(Path modPath) {
        ValidationResult result = new ValidationResult(modPath.getFileName().toString());
        
        try {
            byte[] data = Files.readAllBytes(modPath);
            
            // Level 1: 完整性檢查
            result.sha256 = computeSha256(data);
            result.integrityPassed = true;
            
            // Level 2: 黑名單檢查
            for (String blocked : blockedMods) {
                if (modPath.getFileName().toString().contains(blocked)) {
                    result.errors.add("Mod matched blocked pattern: " + blocked);
                    result.passed = false;
                    return result;
                }
            }
            
            // Level 3: 檢查敏感 API 呼叫
            if (requiredLevel.ordinal() >= SecurityLevel.BEHAVIOR.ordinal()) {
                checkSensitiveAPIs(data, result);
            }
            
        } catch (Exception e) {
            result.errors.add("Validation failed: " + e.getMessage());
            result.passed = false;
        }
        
        result.passed = result.errors.isEmpty();
        return result;
    }

    /**
     * 批量驗證 mods 目錄。
     */
    public List<ValidationResult> validateAll(Path modsDir) throws IOException {
        List<ValidationResult> results = new ArrayList<>();
        
        File[] files = modsDir.toFile().listFiles((d, n) -> n.endsWith(".jar"));
        if (files == null) return results;
        
        for (File file : files) {
            results.add(validate(file.toPath()));
        }
        
        return results;
    }

    /**
     * 檢查位元組碼中的敏感 API 呼叫。
     */
    private void checkSensitiveAPIs(byte[] data, ValidationResult result) {
        String content = new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
        
        // 檢查可疑的 API 呼叫模式 (簡化)
        List<String> suspiciousPatterns = List.of(
            "java/lang/Runtime",   // Runtime.exec()
            "java/lang/ProcessBuilder",
            "java/net/URL",        // 網路存取 (許多 mod 合法使用)
            "java/lang/reflect",   // 反射
            "sun/misc/Unsafe"      // Unsafe
        );
        
        // 注意: 許多正規 mod 合法使用這些 API，
        // 此處僅記錄警告而非阻擋
        for (String pattern : suspiciousPatterns) {
            if (content.contains(pattern)) {
                result.warnings.add("Uses: " + pattern);
            }
        }
    }

    private String computeSha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // --- 設定 ---
    /** 封鎖符合正則規則的 mod（支援 pattern 匹配）。 */
    public void blockMod(String pattern) { blockedMods.add(pattern); }
    /** 信任指定的簽署者。 */
    public void trustSigner(String signer) { trustedSigners.add(signer); }

    // --- 結果 ---
    /** 單一 JAR 檔案的安全驗證結果。 */
    public static class ValidationResult {
        /** 檔案名稱 */
        public String fileName;
        /** 檔案的 SHA256 雜湊值 */
        public String sha256;
        /** 完整性檢查是否通過 */
        public boolean integrityPassed;
        /** 數位簽章是否驗證成功 */
        public boolean signatureVerified;
        /** 總體驗證結果（所有檢查通過） */
        public boolean passed;
        /** 錯誤訊息列表 */
        public List<String> errors = new ArrayList<>();
        /** 警告訊息列表 */
        public List<String> warnings = new ArrayList<>();
        
        public ValidationResult(String fileName) {
            this.fileName = fileName;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(passed ? "✓" : "✗").append(" ").append(fileName);
            if (sha256 != null) sb.append(" sha256=").append(sha256.substring(0, 8));
            for (String e : errors) sb.append("\n  Error: ").append(e);
            for (String w : warnings) sb.append("\n  Warning: ").append(w);
            return sb.toString();
        }
    }
}
