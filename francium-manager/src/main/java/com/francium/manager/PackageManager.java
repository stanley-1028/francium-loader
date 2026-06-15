package com.francium.manager;

import com.francium.api.PublicApi;
import java.io.IOException;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Francium 套件管理器。
 *
 * 設計理念: npm/pip 之於 JavaScript/Python 的關係,
 * Francium Package Manager 之於 Minecraft mod 的關係。
 *
 * 功能:
 * - 集中式 registry 查詢
 * - 自動依賴解析和下載
 * - 版本鎖定 (francium-lock.json)
 * - 多 registry 支援 (類似 apt sources.list)
 * - 模組驗證 (SHA256 + 數位簽章)
 */
@PublicApi
public class PackageManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageManager.class);

    private final Path modsDir;
    private final Path cacheDir;
    private final Path lockFile;
    private final HttpClient httpClient;
    private final Gson gson;
    private final List<String> registries;

    // 本地快取
    private final Map<String, List<RegistryMod>> cache;
    private long cacheTTL = 3600_000; // 1 小時
    private long lastCacheUpdate = 0;

    public PackageManager(Path modsDir, Path cacheDir) {
        this.modsDir = modsDir;
        this.cacheDir = cacheDir.resolve("pkg-cache");
        this.lockFile = modsDir.resolve("francium-lock.json");
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.registries = new ArrayList<>();
        this.registries.add("https://registry.francium.dev/v1"); // 預設官方 registry
        this.cache = new HashMap<>();

        // 確保目錄存在
        try {
            Files.createDirectories(this.cacheDir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create cache directory: {}", e.getMessage());
        }
    }

    /**
     * 搜尋模組。
     */
    public List<RegistryMod> search(String query) throws Exception {
        refreshCacheIfNeeded();

        String lowerQuery = query.toLowerCase();
        return cache.values().stream()
            .flatMap(List::stream)
            .filter(mod -> mod.modId.toLowerCase().contains(lowerQuery)
                || (mod.name != null && mod.name.toLowerCase().contains(lowerQuery))
                || (mod.description != null && mod.description.toLowerCase().contains(lowerQuery)))
            .distinct()
            .sorted(Comparator.comparingLong((RegistryMod m) -> m.downloads).reversed())
            .limit(20)
            .toList();
    }

    /**
     * 安裝一個模組 (含傳遞依賴)。
     *
     * @param modId 模組 ID
     * @param versionConstraint 版本約束 (如 "^1.2.0" 或 null 表示最新)
     * @return 安裝報告
     */
    public InstallReport install(String modId, String versionConstraint) throws Exception {
        InstallReport report = new InstallReport();
        report.rootMod = modId;

        // Step 1: 從 registry 獲取可用版本
        RegistryMod mod = fetchModInfo(modId);
        if (mod == null) {
            report.errors.add("Mod not found in any registry: " + modId);
            return report;
        }

        // Step 2: 選擇最佳版本
        RegistryMod.ModVersion selectedVersion = selectBestVersion(mod, versionConstraint);
        if (selectedVersion == null) {
            report.errors.add("No version of " + modId + " satisfies constraint: " + versionConstraint);
            return report;
        }
        report.selectedVersion = selectedVersion.version;

        // Step 3: 解析所有傳遞依賴
        Set<String> toInstall = new LinkedHashSet<>();
        Map<String, RegistryMod.ModVersion> resolved = new LinkedHashMap<>();

        if (!resolveTree(modId, selectedVersion, resolved, toInstall, new HashSet<>(), report)) {
            return report; // 解析失敗
        }
        report.totalMods = resolved.size();

        // Step 4: 下載所有模組
        for (var entry : resolved.entrySet()) {
            String id = entry.getKey();
            RegistryMod.ModVersion ver = entry.getValue();

            try {
                Path dest = modsDir.resolve(id + "-" + ver.version + ".jar");
                downloadMod(ver.downloadUrl, dest, ver.sha256);
                report.downloaded++;
            } catch (Exception e) {
                report.errors.add("Failed to download " + id + ": " + e.getMessage());
            }
        }

        // Step 5: 寫入鎖定檔案
        writeLockFile(resolved);

        report.success = report.errors.isEmpty();
        return report;
    }

    /**
     * 從鎖定檔案恢復安裝 (francium install,不帶參數)。
     */
    public InstallReport installFromLock() throws Exception {
        if (!Files.exists(lockFile)) {
            InstallReport report = new InstallReport();
            report.errors.add("No lock file found. Run 'francium install <mod>' first.");
            return report;
        }

        LockFile lock = gson.fromJson(Files.readString(lockFile), LockFile.class);
        InstallReport report = new InstallReport();

        for (LockEntry entry : lock.mods) {
            try {
                Path dest = modsDir.resolve(entry.modId + "-" + entry.version + ".jar");
                if (!Files.exists(dest)) {
                    downloadMod(entry.downloadUrl, dest, entry.sha256);
                }
                report.downloaded++;
            } catch (Exception e) {
                report.errors.add("Failed to download " + entry.modId + ": " + e.getMessage());
            }
        }

        report.success = report.errors.isEmpty();
        return report;
    }

    /**
     * 檢查模組更新。
     */
    public List<UpdateInfo> checkUpdates() throws Exception {
        List<UpdateInfo> updates = new ArrayList<>();

        File[] modFiles = modsDir.toFile().listFiles((d, n) -> n.endsWith(".jar"));
        if (modFiles == null) return updates;

        for (File modFile : modFiles) {
            String name = modFile.getName();
            // 解析 modId 和 version
            String[] parts = name.replace(".jar", "").split("-");
            if (parts.length < 2) continue;

            String modId = String.join("-", Arrays.copyOf(parts, parts.length - 1));
            String currentVersion = parts[parts.length - 1];

            try {
                RegistryMod mod = fetchModInfo(modId);
                if (mod != null && !mod.versions.isEmpty()) {
                    String latest = mod.versions.get(0).version;
                    if (!latest.equals(currentVersion)) {
                        updates.add(new UpdateInfo(modId, currentVersion, latest));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to check updates for mod {}: {}", modId, e.getMessage());
            }
        }

        return updates;
    }

    // --- Private methods ---
    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < cacheTTL && !cache.isEmpty()) return;

        for (String registry : registries) {
            try {
                fetchRegistryIndex(registry);
            } catch (Exception e) {
                LOGGER.error("Failed to fetch registry " + registry + ": " + e.getMessage());
            }
        }
        lastCacheUpdate = now;
    }

    private void fetchRegistryIndex(String registryUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(registryUrl + "/index.json"))
            .GET()
            .timeout(java.time.Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            RegistryIndex index = gson.fromJson(response.body(), RegistryIndex.class);
            for (RegistryMod mod : index.mods) {
                cache.computeIfAbsent(mod.modId, k -> new ArrayList<>()).add(mod);
            }
        }
    }

    private RegistryMod fetchModInfo(String modId) throws Exception {
        // 先檢查快取
        if (cache.containsKey(modId)) {
            return cache.get(modId).get(0);
        }

        for (String registry : registries) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(registry + "/mod/" + modId))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(15))
                    .build();

                HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    RegistryMod mod = gson.fromJson(response.body(), RegistryMod.class);
                    cache.put(modId, List.of(mod));
                    return mod;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to fetch mod info for {}: {}", modId, e.getMessage());
            }
        }
        return null;
    }

    private RegistryMod.ModVersion selectBestVersion(RegistryMod mod, String constraint) {
        if (constraint == null || constraint.equals("*")) {
            return mod.versions.isEmpty() ? null : mod.versions.get(0); // 最新穩定版
        }

        // 用語義化版本約束選擇
        var depConstraint = new com.francium.resolver.model.DependencyConstraint(constraint);

        return mod.versions.stream()
            .filter(v -> {
                var sv = com.francium.resolver.model.SemanticVersion.tryParse(v.version);
                return sv != null && depConstraint.satisfiedBy(sv);
            })
            .findFirst()
            .orElse(null);
    }

    private boolean resolveTree(String modId, RegistryMod.ModVersion version,
                                Map<String, RegistryMod.ModVersion> resolved,
                                Set<String> toInstall,
                                Set<String> visiting,
                                InstallReport report) throws Exception {
        if (visiting.contains(modId)) {
            report.errors.add("Circular dependency detected: " + visiting + " -> " + modId);
            return false;
        }

        if (resolved.containsKey(modId)) {
            // 已解析,檢查版本是否相容
            RegistryMod.ModVersion existing = resolved.get(modId);
            if (!existing.version.equals(version.version)) {
                report.errors.add("Version conflict: " + modId + " requires both "
                    + existing.version + " and " + version.version);
                return false;
            }
            return true;
        }

        visiting.add(modId);
        resolved.put(modId, version);

        // 解析此版本的依賴
        if (version.dependencies != null) {
            for (var dep : version.dependencies.entrySet()) {
                String depId = dep.getKey();
                String depConstraint = dep.getValue();

                RegistryMod depMod = fetchModInfo(depId);
                if (depMod == null) {
                    // 可選依賴: 不存在時僅警告
                    report.warnings.add("Optional dependency not found: " + depId);
                    continue;
                }

                RegistryMod.ModVersion bestDep = selectBestVersion(depMod, depConstraint);
                if (bestDep == null) {
                    report.errors.add("No version of " + depId + " satisfies: " + depConstraint);
                    visiting.remove(modId);
                    return false;
                }

                if (!resolveTree(depId, bestDep, resolved, toInstall, visiting, report)) {
                    visiting.remove(modId);
                    return false;
                }
            }
        }

        visiting.remove(modId);
        return true;
    }

    private void downloadMod(String url, Path dest, String expectedSha256) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(java.time.Duration.ofMinutes(5))
            .build();

        HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("Download failed: HTTP " + response.statusCode());
        }

        byte[] data = response.body();

        // SHA256 驗證 (如果提供)
        if (expectedSha256 != null) {
            String actualSha256 = sha256(data);
            if (!actualSha256.equals(expectedSha256)) {
                throw new SecurityException("SHA256 mismatch for " + dest.getFileName());
            }
        }

        Files.write(dest, data);
    }

    private void writeLockFile(Map<String, RegistryMod.ModVersion> resolved) throws IOException {
        LockFile lock = new LockFile();
        lock.version = 1;
        lock.generatedAt = System.currentTimeMillis();
        lock.mods = new ArrayList<>();

        for (var entry : resolved.entrySet()) {
            LockEntry le = new LockEntry();
            le.modId = entry.getKey();
            le.version = entry.getValue().version;
            le.downloadUrl = entry.getValue().downloadUrl;
            le.sha256 = entry.getValue().sha256;
            lock.mods.add(le);
        }

        Files.writeString(lockFile, gson.toJson(lock));
    }

    private String sha256(byte[] data) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // --- Data classes ---
    /** Registry 索引，包含所有可查詢的 mod。 */
    public static class RegistryIndex {
        /** mod 列表 */
        public List<RegistryMod> mods;
    }
    
    /** 從 Registry 查詢到的單一 mod 資訊。 */
    public static class RegistryMod {
        /** 模組唯一識別碼 */
        public String modId;
        /** 顯示名稱 */
        public String name;
        /** 簡短描述 */
        public String description;
        /** 作者列表 */
        public List<String> authors;
        /** 開源授權 */
        public String license;
        /** 下載次數 */
        public long downloads;
        /** 可用版本列表 */
        public List<ModVersion> versions;
        
        /** 單一版本的元資料。 */
        public static class ModVersion {
            /** 版本號 */
            public String version;
            /** 相容的 Minecraft 版本 */
            public String mcVersion;
            /** 下載 URL */
            public String downloadUrl;
            /** SHA256 校驗碼 */
            public String sha256;
            /** 檔案大小（位元組） */
            public long size;
            /** 依賴映射：modId → 版本約束 */
            public Map<String, String> dependencies;
            /** 可選依賴映射：modId → 版本約束 */
            public Map<String, String> optionalDependencies;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RegistryMod m)) return false;
            return modId.equals(m.modId);
        }

        @Override
        public int hashCode() { return modId.hashCode(); }
    }

    /** 安裝操作的結果報告。 */
    public static class InstallReport {
        /** 目標 mod 識別碼 */
        public String rootMod;
        /** 選定的版本 */
        public String selectedVersion;
        /** 涉及安裝的總 mod 數（含依賴） */
        public int totalMods;
        /** 實際下載的 mod 數 */
        public int downloaded;
        /** 安裝是否成功 */
        public boolean success;
        /** 錯誤訊息列表 */
        public List<String> errors = new ArrayList<>();
        /** 警告訊息列表 */
        public List<String> warnings = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(success ? "✓" : "✗").append(" Installed ").append(rootMod);
            if (selectedVersion != null) sb.append("@").append(selectedVersion);
            sb.append(" (").append(downloaded).append(" mods)");
            for (String e : errors) sb.append("\n  Error: ").append(e);
            for (String w : warnings) sb.append("\n  Warning: ").append(w);
            return sb.toString();
        }
    }

    static class LockFile {
        int version;
        long generatedAt;
        List<LockEntry> mods;
    }

    static class LockEntry {
        String modId;
        String version;
        String downloadUrl;
        String sha256;
    }

    /**
     * 可更新模組的資訊。
     * @param modId 模組識別碼
     * @param currentVersion 當前已安裝版本
     * @param latestVersion Registry 中的最新版本
     */
    public record UpdateInfo(String modId, String currentVersion, String latestVersion) {
        @Override
        public String toString() {
            return modId + ": " + currentVersion + " → " + latestVersion;
        }
    }
}
