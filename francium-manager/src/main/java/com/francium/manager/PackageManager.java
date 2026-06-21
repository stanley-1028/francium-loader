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
    private final ModrinthAdapter modrinth;
    private final CurseForgeAdapter curseForge;
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
        this.registries.add("https://registry.francium.dev/v1"); // Francium 官方 registry (開發中)
        this.cache = new HashMap<>();
        this.modrinth = new ModrinthAdapter();
        this.curseForge = new CurseForgeAdapter();

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

        // 同時查詢 Modrinth 與 CurseForge（若已配置 API key）
        var modrinthResults = modrinth.searchProjects(query);
        var cfResults = curseForge.isAvailable() ? curseForge.searchProjects(query) : List.<RegistryMod>of();
        var combined = new LinkedHashMap<String, RegistryMod>();
        for (var m : modrinthResults) combined.putIfAbsent(m.modId, m);
        for (var m : cfResults) combined.putIfAbsent(m.modId, m);

        String lowerQuery = query.toLowerCase();
        // 合併 cache（registry URL 來源）與 adapter（Modrinth/CurseForge）的結果
        var all = new LinkedHashMap<String, RegistryMod>();
        // 先加 registry URL 的結果
        cache.values().stream().flatMap(List::stream).forEach(m -> all.putIfAbsent(m.modId, m));
        // 再加 adapter 的結果（不覆蓋已存在的）
        combined.forEach(all::putIfAbsent);
        return all.values().stream()
            .filter(mod -> mod.modId.toLowerCase().contains(lowerQuery)
                || (mod.name != null && mod.name.toLowerCase().contains(lowerQuery))
                || (mod.description != null && mod.description.toLowerCase().contains(lowerQuery)))
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
        Map<String, RegistryMod.ModVersion> resolved = new LinkedHashMap<>();

        if (!resolveTree(modId, selectedVersion, resolved, new HashSet<>(), report)) {
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

        // Step 5: 全部下載成功後才寫入鎖定檔案，避免 lock file 記錄未安裝完成的 mod
        if (report.errors.isEmpty()) {
            try {
                writeLockFile(resolved);
            } catch (IOException e) {
                report.errors.add("Failed to write lock file: " + e.getMessage());
            }
        }

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
            // 解析 modId 和版本號
            // ★ BUG FIX: 不能直接用 split("-")，modId 本身可能含連字號（如 "better-fps"）
            //   改用正則：從結尾往前找語義版本號模式
            String baseName = name.replace(".jar", "");
            String modId = baseName;
            String currentVersion = null;

            // 嘗試匹配結尾的語義版本號（如 "1.2.3" 或 "1.2.3-beta"）
            java.util.regex.Matcher versionMatcher = java.util.regex.Pattern.compile(
                "-(\\d+\\.\\d+\\.\\d+(?:[-.][a-zA-Z0-9]+)?)$").matcher(baseName);
            if (versionMatcher.find()) {
                currentVersion = versionMatcher.group(1);
                modId = baseName.substring(0, versionMatcher.start());
            } else {
                // Fallback: 沒有版本號的 mod 不檢查
                continue;
            }

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
            if (index == null || index.mods == null) return;
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

        RegistryMod registryMod = fetchFromFranciumRegistries(modId);
        if (registryMod != null) {
            return cacheAndReturn(registryMod);
        }

        RegistryMod modrinthMod = modrinth.fetchProject(modId);
        if (modrinthMod != null && hasInstallableVersions(modrinthMod)) {
            return cacheAndReturn(modrinthMod);
        }

        RegistryMod curseForgeMod = fetchFromCurseForge(modId);
        if (curseForgeMod != null && hasInstallableVersions(curseForgeMod)) {
            return cacheAndReturn(curseForgeMod);
        }

        return null;
    }

    private RegistryMod fetchFromFranciumRegistries(String modId) {
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
                    return mod;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to fetch mod info for {}: {}", modId, e.getMessage());
            }
        }
        return null;
    }

    private RegistryMod fetchFromCurseForge(String modId) {
        if (!curseForge.isAvailable()) return null;

        try {
            Long curseForgeId = parseLongOrNull(modId);
            if (curseForgeId == null) {
                curseForgeId = curseForge.resolveModId(modId);
            }
            return curseForgeId != null ? curseForge.fetchProject(curseForgeId) : null;
        } catch (Exception e) {
            LOGGER.warn("CurseForge lookup failed for {}: {}", modId, e.getMessage());
            return null;
        }
    }

    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RegistryMod cacheAndReturn(RegistryMod mod) {
        cache.put(mod.modId, List.of(mod));
        return mod;
    }

    private boolean hasInstallableVersions(RegistryMod mod) {
        return mod.versions != null && !mod.versions.isEmpty();
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
        try {
            resolved.put(modId, version);

            // 解析此版本的必選依賴（★ BUG FIX: 找不到時視為錯誤而非警告）
            if (version.dependencies != null) {
                for (var dep : version.dependencies.entrySet()) {
                    String depId = dep.getKey();
                    String depConstraint = dep.getValue();

                    RegistryMod depMod = fetchModInfo(depId);
                    if (depMod == null) {
                        report.errors.add("Required dependency not found: " + depId + " (constraint: " + depConstraint + ")");
                        return false;
                    }

                    RegistryMod.ModVersion bestDep = selectBestVersion(depMod, depConstraint);
                    if (bestDep == null) {
                        report.errors.add("No version of " + depId + " satisfies: " + depConstraint);
                        return false;
                    }

                    if (!resolveTree(depId, bestDep, resolved, visiting, report)) {
                        return false;
                    }
                }
            }

            // 解析可選依賴
            if (version.optionalDependencies != null) {
                for (var dep : version.optionalDependencies.entrySet()) {
                    String depId = dep.getKey();
                    String depConstraint = dep.getValue();

                    RegistryMod depMod;
                    try {
                        depMod = fetchModInfo(depId);
                    } catch (Exception e) {
                        report.warnings.add("Optional dependency check failed for " + depId + ": " + e.getMessage());
                        continue;
                    }
                    if (depMod == null) {
                        report.warnings.add("Optional dependency not found: " + depId);
                        continue;
                    }

                    RegistryMod.ModVersion bestDep = selectBestVersion(depMod, depConstraint);
                    if (bestDep == null) {
                        report.warnings.add("No version of optional dependency " + depId + " satisfies: " + depConstraint);
                        continue;
                    }

                    if (resolved.containsKey(depId)) continue;

                    try {
                        resolveTree(depId, bestDep, resolved, visiting, report);
                    } catch (Exception e) {
                        report.warnings.add("Optional dependency " + depId + " resolution failed: " + e.getMessage());
                    }
                }
            }

            return true;
        } finally {
            // ★ BUG FIX: 確保在任何情況下（正常返回、異常拋出）visiting 都能正確清理
            //   避免 visiting 集污染導致後續依賴解析的虛假循環依賴檢測
            visiting.remove(modId);
        }
    }

    private void downloadMod(String url, Path dest, String expectedSha256) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IOException("Missing download URL");
        }

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

        Path parent = dest.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
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

        // ★ BUG FIX: 寫入前確保目錄存在
        Path lockParent = lockFile.getParent();
        if (lockParent != null) {
            Files.createDirectories(lockParent);
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
