package com.francium.manager;

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
 * Modrinth API 轉接器。
 * 將 Modrinth API v2 (https://api.modrinth.com/v2/) 的響應格式
 * 轉換為 Francium 的內部 RegistryMod 格式。
 *
 * Modrinth API 文檔: https://docs.modrinth.com/
 */
class ModrinthAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModrinthAdapter.class);
    private static final String BASE = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "FranciumLoader/1.0";

    private final HttpClient http;
    private final Gson gson;

    ModrinthAdapter() {
        this.http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
    }

    /** 搜尋 Modrinth 專案，返回 Francium RegistryMod 列表。 */
    List<PackageManager.RegistryMod> searchProjects(String query) {
        try {
            String url = BASE + "/search?query=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                + "&facets=[[\"project_type:mod\"]]&limit=20";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
            HttpResponse<String> resp = http.send(req, BodyHandlers.ofString());
            if (resp.statusCode() != 200) return List.of();

            SearchResult sr = gson.fromJson(resp.body(), SearchResult.class);
            if (sr == null || sr.hits == null) return List.of();
            return sr.hits.stream().map(this::toRegistryMod).toList();
        } catch (Exception e) {
            LOGGER.warn("Modrinth search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /** 取得單一專案詳細資訊 + 版本列表。 */
    PackageManager.RegistryMod fetchProject(String projectId) {
        try {
            // 取得專案資訊
            HttpRequest projReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/project/" + java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8)))
                .header("User-Agent", USER_AGENT)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
            HttpResponse<String> projResp = http.send(projReq, BodyHandlers.ofString());
            if (projResp.statusCode() != 200) return null;
            ProjectResponse project = gson.fromJson(projResp.body(), ProjectResponse.class);
            if (project == null) return null;

            // 取得版本列表
            HttpRequest verReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/project/" + java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8) + "/version"))
                .header("User-Agent", USER_AGENT)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
            HttpResponse<String> verResp = http.send(verReq, BodyHandlers.ofString());
            List<VersionResponse> versions = new ArrayList<>();
            if (verResp.statusCode() == 200) {
                VersionResponse[] arr = gson.fromJson(verResp.body(), VersionResponse[].class);
                if (arr != null) versions = Arrays.asList(arr);
            }

            PackageManager.RegistryMod mod = new PackageManager.RegistryMod();
            mod.modId = project.id != null ? project.id : project.slug;
            mod.name = project.title;
            mod.description = project.description;
            mod.authors = project.author != null ? List.of(project.author) : List.of();
            mod.license = project.license != null ? project.license.id : null;
            mod.downloads = project.downloads;
            mod.versions = versions.stream()
                .filter(v -> v.files != null && !v.files.isEmpty())
                .map(v -> {
                    PackageManager.RegistryMod.ModVersion mv = new PackageManager.RegistryMod.ModVersion();
                    mv.version = v.versionNumber;
                    mv.mcVersion = v.gameVersions != null && !v.gameVersions.isEmpty()
                        ? v.gameVersions.get(0) : "";
                    var primaryFile = v.files.stream()
                        .filter(f -> f.primary || (f.url != null && f.url.endsWith(".jar")))
                        .findFirst().orElse(null);
                    if (primaryFile != null) {
                        mv.downloadUrl = primaryFile.url;
                        mv.sha256 = primaryFile.hashes != null ? primaryFile.hashes.sha256 : null;
                        mv.size = primaryFile.size;
                    }
                    mv.dependencies = new HashMap<>();
                    if (v.dependencies != null) {
                        for (var dep : v.dependencies) {
                            if ("required".equals(dep.dependencyType) && dep.projectId != null) {
                                mv.dependencies.put(dep.projectId, "*");
                            }
                        }
                    }
                    return mv;
                })
                .filter(mv -> mv.downloadUrl != null)
                .toList();
            return mod;
        } catch (Exception e) {
            LOGGER.warn("Modrinth fetch failed for {}: {}", projectId, e.getMessage());
            return null;
        }
    }

    // ─── Modrinth API DTOs ─────────────────────────────────

    private PackageManager.RegistryMod toRegistryMod(SearchHit hit) {
        PackageManager.RegistryMod mod = new PackageManager.RegistryMod();
        mod.modId = hit.projectId != null ? hit.projectId : hit.slug;
        mod.name = hit.title;
        mod.description = hit.description;
        mod.authors = hit.author != null ? List.of(hit.author) : List.of();
        mod.downloads = hit.downloads;
        mod.versions = List.of(); // 搜尋結果不包含版本資訊
        return mod;
    }

    static class SearchResult {
        List<SearchHit> hits;
    }

    static class SearchHit {
        @SerializedName("project_id") String projectId;
        String slug;
        String title;
        String description;
        String author;
        long downloads;
        String categories;
    }

    static class ProjectResponse {
        String id;
        String slug;
        String title;
        String description;
        String author;
        long downloads;
        License license;
    }

    static class License {
        String id;
    }

    static class VersionResponse {
        @SerializedName("version_number") String versionNumber;
        @SerializedName("game_versions") List<String> gameVersions;
        List<VersionFile> files;
        List<VersionDependency> dependencies;
    }

    static class VersionFile {
        boolean primary;
        String url;
        long size;
        Hashes hashes;
    }

    static class Hashes {
        String sha256;
        String sha512;
    }

    static class VersionDependency {
        @SerializedName("project_id") String projectId;
        @SerializedName("dependency_type") String dependencyType;
    }
}
