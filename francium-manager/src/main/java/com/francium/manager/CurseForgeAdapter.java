package com.francium.manager;

import com.francium.resolver.model.SemanticVersion;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CurseForge API 轉接器。
 *
 * 將 CurseForge API v1 的響應格式轉換為 Francium 內部 RegistryMod 格式。
 *
 * API 文檔: https://docs.curseforge.com/
 * API Key 申請: https://console.curseforge.com/
 *
 * 使用方式:
 *   系統屬性: -Dcurseforge.api_key=YOUR_KEY
 *   或環境變數: CURSEFORGE_API_KEY
 *   或直接在程式碼中設定: System.setProperty("curseforge.api_key", "YOUR_KEY");
 *
 * 注意: CurseForge 要求所有請求攜帶 API Key（免費申請）。
 */
public class CurseForgeAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CurseForgeAdapter.class);
    private static final String BASE = "https://api.curseforge.com/v1";
    private static final String USER_AGENT = "FranciumLoader/1.0";
    private static final int GAME_ID = 432; // Minecraft

    private final HttpClient http;
    private final Gson gson;
    private final String apiKey;

    public CurseForgeAdapter() {
        this(getApiKeyFromEnv());
    }

    public CurseForgeAdapter(String apiKey) {
        this.http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
        this.apiKey = apiKey;
    }

    private static String getApiKeyFromEnv() {
        String key = System.getProperty("curseforge.api_key");
        if (key != null && !key.isEmpty()) return key;
        key = System.getenv("CURSEFORGE_API_KEY");
        if (key != null && !key.isEmpty()) return key;
        LOGGER.warn("CurseForge API key not found. Set -Dcurseforge.api_key=YOUR_KEY " +
            "or CURSEFORGE_API_KEY environment variable.");
        return "";
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(BASE + path))
            .header("User-Agent", USER_AGENT)
            .header("x-api-key", apiKey)
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(15));
    }

    /** 搜尋 CurseForge 專案。 */
    public List<PackageManager.RegistryMod> searchProjects(String query) {
        if (!isAvailable()) return List.of();
        try {
            String url = "/mods/search?gameId=" + GAME_ID
                + "&searchFilter=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                + "&pageSize=20&sortField=2&sortOrder=desc"; // sort by total downloads
            var resp = http.send(request(url).GET().build(), BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                LOGGER.warn("CurseForge search failed: HTTP {}", resp.statusCode());
                return List.of();
            }
            SearchResponse sr = gson.fromJson(resp.body(), SearchResponse.class);
            if (sr == null || sr.data == null) return List.of();
            return sr.data.stream()
                .filter(m -> m.classId != null && m.classId == 6) // 6 = Mods class
                .map(this::toRegistryMod)
                .toList();
        } catch (Exception e) {
            LOGGER.warn("CurseForge search error: {}", e.getMessage());
            return List.of();
        }
    }

    /** 取得單一專案詳細資訊 + 最新檔案（版本）。 */
    public PackageManager.RegistryMod fetchProject(long modId) {
        if (!isAvailable()) return null;
        try {
            var modResp = http.send(request("/mods/" + modId).GET().build(), BodyHandlers.ofString());
            if (modResp.statusCode() != 200) return null;
            ModResponse mr = gson.fromJson(modResp.body(), ModResponse.class);
            if (mr == null || mr.data == null) return null;
            CFFile latest = mr.data.latestFilesIndex != null && !mr.data.latestFilesIndex.isEmpty()
                ? mr.data.latestFilesIndex.get(0) : null;
            if (latest == null) return null;

            PackageManager.RegistryMod mod = new PackageManager.RegistryMod();
            mod.modId = String.valueOf(mr.data.id);
            mod.name = mr.data.name;
            mod.description = mr.data.summary;
            mod.authors = mr.data.authors != null
                ? mr.data.authors.stream().map(a -> a.name).toList()
                : List.of();
            mod.downloads = mr.data.downloadCount;
            mod.license = null;

            PackageManager.RegistryMod.ModVersion mv = new PackageManager.RegistryMod.ModVersion();
            mv.version = latest.displayName != null ? latest.displayName : latest.fileName;
            if (mv.version == null) mv.version = "latest";
            // ★ BUG FIX: CurseForge 的 displayName 可能含前綴 "v" 或非標準版本格式（如 "Release 1.2.3"），
            //   嘗試用 tryParse 驗證是否為有效語義版本，若不是則改用 fileName 中的版本號
            if (SemanticVersion.tryParse(mv.version) == null && latest.fileName != null) {
                // 從 fileName 中提取版本（如 "MyMod-1.2.3.jar" → "1.2.3"）
                java.util.regex.Matcher verMatcher = java.util.regex.Pattern.compile(
                    "(\\d+\\.\\d+\\.\\d+(?:[-.][a-zA-Z0-9]+)?)").matcher(latest.fileName);
                if (verMatcher.find()) {
                    mv.version = verMatcher.group(1);
                }
            }
            mv.mcVersion = latest.gameVersion != null ? latest.gameVersion : "";
            mv.downloadUrl = latest.downloadUrl;
            // ★ BUG FIX: 若 downloadUrl 為空，無法下載，跳過此版本
            if (mv.downloadUrl == null || mv.downloadUrl.isBlank()) return null;
            mv.size = latest.fileLength;
            mv.dependencies = new HashMap<>();
            mv.optionalDependencies = new HashMap<>();
            // CurseForge API 不直接提供依賴資訊在 latestFilesIndex 中
            mod.versions = List.of(mv);

            return mod;
        } catch (Exception e) {
            LOGGER.warn("CurseForge fetch failed for mod {}: {}", modId, e.getMessage());
            return null;
        }
    }

    /** 透過 slug 查詢 mod ID（用於從 modId slug 解析）。 */
    public Long resolveModId(String slug) {
        if (!isAvailable()) return null;
        try {
            var resp = http.send(
                request("/mods/search?gameId=" + GAME_ID + "&slug="
                    + java.net.URLEncoder.encode(slug, java.nio.charset.StandardCharsets.UTF_8))
                    .GET().build(), BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            SearchResponse sr = gson.fromJson(resp.body(), SearchResponse.class);
            if (sr != null && sr.data != null && !sr.data.isEmpty()) {
                return sr.data.get(0).id;
            }
        } catch (Exception e) {
            LOGGER.warn("CurseForge slug resolve failed: {}", e.getMessage());
        }
        return null;
    }

    // ─── 轉換 helper ─────────────────────────────────

    private PackageManager.RegistryMod toRegistryMod(ModSummary s) {
        PackageManager.RegistryMod mod = new PackageManager.RegistryMod();
        mod.modId = String.valueOf(s.id);
        mod.name = s.name;
        mod.description = s.summary;
        mod.authors = s.authors != null
            ? s.authors.stream().map(a -> a.name).toList()
            : List.of();
        mod.downloads = s.downloadCount;
        mod.versions = List.of();
        return mod;
    }

    // ─── CurseForge API DTOs ──────────────────────────────

    static class SearchResponse {
        List<ModSummary> data;
    }

    static class ModSummary {
        long id;
        String name;
        String summary;
        Long classId;
        long downloadCount;
        List<ModAuthor> authors;
    }

    static class ModResponse {
        ModDetail data;
    }

    static class ModDetail {
        long id;
        String name;
        String summary;
        long downloadCount;
        List<ModAuthor> authors;
        List<CFFile> latestFilesIndex;
    }

    static class CFFile {
        @SerializedName("display_name") String displayName;
        @SerializedName("file_name") String fileName;
        @SerializedName("game_version") String gameVersion;
        @SerializedName("download_url") String downloadUrl;
        @SerializedName("file_length") long fileLength;
    }

    static class ModAuthor {
        String name;
        String url;
    }
}
