package com.francium.loader;

import com.francium.api.PublicApi;
import java.util.*;
import java.nio.file.Path;

/**
 * 模組清單 - 描述單個模組的完整元數據。
 * 支援語義化版本、依賴約束、Mixin配置和AI橋接標記。
 */
@PublicApi
public class ModManifest {
    private String modId;
    private String name;
    private String version;
    private String description;
    private List<String> authors;
    private String mainClass;
    
    // 依賴: modId -> 版本約束 (如 ">=1.2.0 <2.0.0")
    private Map<String, String> dependencies;
    // 可選依賴: 有則加載，無則跳過
    private Map<String, String> optionalDependencies;
    // 衝突模組: modId -> 衝突原因
    private Map<String, String> conflicts;
    
    // 支援的 Minecraft 版本範圍
    private String mcVersionMin;
    private String mcVersionMax;
    
    // Mixin 配置路徑
    private List<String> mixinConfigs;
    // AI 橋接標記: 是否允許AI自動適配版本
    private boolean aiBridgeEnabled = true;
    // 加載優先級 (0=默認, 負數=提前, 正數=延後)
    private int loadPriority = 0;
    
    // 模組大小 (用於加載時間預估)
    private long sizeBytes = 0;
    // 預估加載時間 (毫秒，基於歷史數據)
    private long estimatedLoadTimeMs = 0;
    
    // 入口點類型: "preLaunch", "launch", "postLaunch"
    private String entryPointType = "launch";
    
    // 簽名驗證
    private String signature;
    private String publicKeyFingerprint;

    // 實際 JAR 檔案路徑 (由 discoverMods 設定，用於驗證和橋接)
    public transient Path jarSourcePath;

    public ModManifest() {}

    // --- Builder 模式 ---
    public static Builder builder(String modId, String version) {
        return new Builder(modId, version);
    }

    public static class Builder {
        private final ModManifest manifest = new ModManifest();
        
        public Builder(String modId, String version) {
            manifest.modId = modId;
            manifest.version = version;
            manifest.dependencies = new HashMap<>();
            manifest.optionalDependencies = new HashMap<>();
            manifest.conflicts = new HashMap<>();
            manifest.authors = new ArrayList<>();
            manifest.mixinConfigs = new ArrayList<>();
        }
        
        public Builder name(String name) { manifest.name = name; return this; }
        public Builder description(String desc) { manifest.description = desc; return this; }
        public Builder mainClass(String cls) { manifest.mainClass = cls; return this; }
        public Builder authors(List<String> authors) { manifest.authors = authors; return this; }
        public Builder dependency(String modId, String constraint) { 
            manifest.dependencies.put(modId, constraint); return this; 
        }
        public Builder optionalDependency(String modId, String constraint) {
            manifest.optionalDependencies.put(modId, constraint); return this;
        }
        public Builder conflict(String modId, String reason) {
            manifest.conflicts.put(modId, reason); return this;
        }
        public Builder mcVersionRange(String min, String max) {
            manifest.mcVersionMin = min; manifest.mcVersionMax = max; return this;
        }
        public Builder mixinConfig(String config) {
            manifest.mixinConfigs.add(config); return this;
        }
        public Builder aiBridge(boolean enabled) {
            manifest.aiBridgeEnabled = enabled; return this;
        }
        public Builder loadPriority(int priority) {
            manifest.loadPriority = priority; return this;
        }
        public Builder sizeBytes(long bytes) {
            manifest.sizeBytes = bytes; return this;
        }
        public Builder estimatedLoadTimeMs(long ms) {
            manifest.estimatedLoadTimeMs = ms; return this;
        }
        public Builder entryPoint(String type) {
            manifest.entryPointType = type; return this;
        }
        public ModManifest build() { return manifest; }
    }

    // --- Getters ---
    public String modId() { return modId; }
    public String name() { return name; }
    public String version() { return version; }
    public String description() { return description; }
    public List<String> authors() { return authors != null ? Collections.unmodifiableList(authors) : List.of(); }
    public String mainClass() { return mainClass; }
    public Map<String, String> dependencies() { 
        return dependencies != null ? Collections.unmodifiableMap(dependencies) : Map.of(); 
    }
    public Map<String, String> optionalDependencies() { 
        return optionalDependencies != null ? Collections.unmodifiableMap(optionalDependencies) : Map.of(); 
    }
    public Map<String, String> conflicts() { 
        return conflicts != null ? Collections.unmodifiableMap(conflicts) : Map.of(); 
    }
    public String mcVersionMin() { return mcVersionMin; }
    public String mcVersionMax() { return mcVersionMax; }
    public List<String> mixinConfigs() { return mixinConfigs != null ? Collections.unmodifiableList(mixinConfigs) : List.of(); }
    public boolean aiBridgeEnabled() { return aiBridgeEnabled; }
    public int loadPriority() { return loadPriority; }
    public long sizeBytes() { return sizeBytes; }
    public long estimatedLoadTimeMs() { return estimatedLoadTimeMs; }
    public String entryPointType() { return entryPointType; }
    public String signature() { return signature; }
    
    public void setSignature(String sig) { this.signature = sig; }
    public void setEstimatedLoadTimeMs(long ms) { this.estimatedLoadTimeMs = ms; }

    @Override
    public String toString() {
        return modId + "@" + version;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ModManifest m)) return false;
        return Objects.equals(modId, m.modId) && Objects.equals(version, m.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, version);
    }

    // --- 靜態工廠：從不同格式解析 ---

    /**
     * 從 Francium 自訂 JSON 格式解析。
     */
    public static ModManifest fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        
        String modId = extractJsonValue(json, "modId");
        String version = extractJsonValue(json, "version");
        if (modId == null || version == null) return null;
        
        Builder builder = new Builder(modId, version);
        
        String name = extractJsonValue(json, "name");
        if (name != null) builder.name(name);
        
        String mainClass = extractJsonValue(json, "mainClass");
        if (mainClass != null) builder.mainClass(mainClass);
        
        String desc = extractJsonValue(json, "description");
        if (desc != null) builder.description(desc);
        
        // 解析必選依賴
        String depsStr = extractJsonObject(json, "dependencies");
        if (depsStr != null) {
            parseDependencies(depsStr, builder);
        }

        // ★ BUG FIX: 解析可選依賴 — 原本 fromJson() 完全忽略了 optionalDependencies 欄位
        String optDepsStr = extractJsonObject(json, "optionalDependencies");
        if (optDepsStr != null) {
            parseOptionalDependencies(optDepsStr, builder);
        }
        
        String priority = extractJsonValue(json, "loadPriority");
        if (priority != null) {
            try { builder.loadPriority(Integer.parseInt(priority)); } catch (NumberFormatException ignored) {}
        }
        
        String aiBridge = extractJsonValue(json, "aiBridgeEnabled");
        if (aiBridge != null) builder.aiBridge(Boolean.parseBoolean(aiBridge));
        
        // [BUG FIX] 從 JSON 正確讀取 estimatedLoadTimeMs，而非錯誤賦值給 sizeBytes
        String estLoadTime = extractJsonValue(json, "estimatedLoadTimeMs");
        if (estLoadTime != null) {
            try { builder.estimatedLoadTimeMs(Long.parseLong(estLoadTime)); } catch (NumberFormatException ignored) {}
        }
        
        // 也讀取 sizeBytes
        String size = extractJsonValue(json, "sizeBytes");
        if (size != null) {
            try { builder.sizeBytes(Long.parseLong(size)); } catch (NumberFormatException ignored) {}
        }
        
        return builder.build();
    }

    /**
     * 從 Fabric 的 fabric.mod.json 格式解析。
     */
    public static ModManifest fromFabricJson(String json) {
        if (json == null || json.isBlank()) return null;
        
        String modId = extractJsonValue(json, "id");
        String version = extractJsonValue(json, "version");
        if (modId == null) return null;
        if (version == null) version = "unknown";
        
        Builder builder = new Builder(modId, version);
        
        String name = extractJsonValue(json, "name");
        if (name != null) builder.name(name);
        
        String desc = extractJsonValue(json, "description");
        if (desc != null) builder.description(desc);
        
        // Fabric uses "entrypoints" -> "main"
        String entrypointsObj = extractJsonObject(json, "entrypoints");
        if (entrypointsObj != null) {
            String main = extractJsonValue(entrypointsObj, "main");
            if (main != null && main.startsWith("[")) {
                // 陣列格式: ["com.example.Mod"] → 提取第一個元素
                main = extractJsonArrayFirst(entrypointsObj, "main");
            }
            if (main != null && !main.isBlank()) {
                // 去掉可能的包裹
                if (main.startsWith("\"") && main.endsWith("\"")) {
                    main = main.substring(1, main.length() - 1);
                }
                builder.mainClass(main);
            }
        }
        
        // Fabric dependencies
        String depsObj = extractJsonObject(json, "depends");
        if (depsObj != null) {
            parseDependencies(depsObj, builder);
        }
        
        builder.aiBridge(true); // Fabric mods are good candidates for AI bridging
        
        return builder.build();
    }

    /**
     * 從 Forge 的 mods.toml 格式解析。
     */
    public static ModManifest fromForgeToml(String toml) {
        if (toml == null || toml.isBlank()) return null;
        
        String modId = extractTomlValue(toml, "modId");
        String version = extractTomlValue(toml, "version");
        if (modId == null) return null;
        if (version == null) version = "unknown";
        
        Builder builder = new Builder(modId, version);
        
        String displayName = extractTomlValue(toml, "displayName");
        if (displayName != null) builder.name(displayName);
        
        String desc = extractTomlValue(toml, "description");
        if (desc != null) builder.description(desc);
        
        // Forge uses [[dependencies.<modid>]]
        // Simplified parsing
        builder.aiBridge(false); // Forge mods need more caution for AI bridging
        
        return builder.build();
    }

    // --- 簡單 JSON 工具 (避免外部依賴) ---
    
    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) return unescapeJsonString(m.group(1));
        
        // 嘗試數字/布林值
        pattern = "\"" + key + "\"\\s*:\\s*([^,}\\n]*)";
        m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) return m.group(1).trim();
        
        return null;
    }

    /**
     * Decode JSON escape sequences including \\uXXXX unicode escapes.
     */
    private static String unescapeJsonString(String s) {
        if (s == null || s.indexOf('\\') < 0) return s;
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '\"' -> sb.append('\"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 5 < s.length()) {
                            try {
                                sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                    }
                    default -> sb.append(next);
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static String extractJsonObject(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\{";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (!m.find()) return null;
        
        int start = m.end() - 1; // include the {
        int depth = 0;
        int end = start;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') {
                depth--;
                if (depth == 0) { end = i + 1; break; }
            }
        }
        return json.substring(start, end);
    }

    private static void parseDependencies(String depsJson, Builder builder) {
        if (depsJson == null) return;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"").matcher(depsJson);
        while (m.find()) {
            builder.dependency(m.group(1), m.group(2));
        }
    }

    /**
     * 解析可選依賴的 JSON 鍵值對，逐一註冊到 Builder 中。
     * ★ BUG FIX: 新增此方法，避免 fromJson() 中的 optionalDependencies 被靜默丟棄。
     */
    private static void parseOptionalDependencies(String depsJson, Builder builder) {
        if (depsJson == null) return;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"").matcher(depsJson);
        while (m.find()) {
            builder.optionalDependency(m.group(1), m.group(2));
        }
    }

    private static String extractTomlValue(String toml, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            key + "\\s*=\\s*\"([^\"]*)\"").matcher(toml);
        if (m.find()) return m.group(1);
        // Single quotes: key = 'value'
        m = java.util.regex.Pattern.compile(
            key + "\\s*=\\s*'([^']*)'").matcher(toml);
        if (m.find()) return m.group(1);
        return null;
    }

    /**
     * 從 JSON 陣列中提取第一個字串元素。
     * 例如: "main": ["com.example.Mod"] → "com.example.Mod"
     */
    private static String extractJsonArrayFirst(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\[\\s*\"([^\"]+)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }
}
