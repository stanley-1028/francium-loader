package com.francium.forge.adapter;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.*;

/**
 * Forge 模組偵測器
 * 
 * 負責偵測和解析 Forge 模組，支援兩種格式：
 * 1. mcmod.info - 舊版 Forge 模組格式
 * 2. mods.toml - 新版 Forge 模組格式 (1.13+)
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeModDetector {
    
    /** mcmod.info 檔案路徑 */
    private static final String MCMOD_INFO_PATH = "mcmod.info";
    
    /** mods.toml 檔案路徑 */
    private static final String MODS_TOML_PATH = "META-INF/mods.toml";
    
    /** FML 核心模組標記 */
    private static final String FML_MOD_TYPE = "FMLModType";
    
    /**
     * 檢查 JAR 檔案是否為 Forge 模組
     * 
     * @param jarPath JAR 檔案路徑
     * @return 是否為 Forge 模組
     */
    public static boolean isForgeMod(Path jarPath) {
        if (jarPath == null || !Files.exists(jarPath)) {
            return false;
        }
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // 檢查是否有 mcmod.info 或 mods.toml
            boolean hasMcModInfo = jarFile.getJarEntry(MCMOD_INFO_PATH) != null;
            boolean hasModsToml = jarFile.getJarEntry(MODS_TOML_PATH) != null;
            
            // 檢查是否有 FML 相關類別
            boolean hasFMLClasses = jarFile.stream()
                .anyMatch(entry -> entry.getName().startsWith("net/minecraftforge/fml/")
                    || entry.getName().startsWith("net/minecraftforge/common/"));
            
            return hasMcModInfo || hasModsToml || hasFMLClasses;
            
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 解析 Forge 模組中繼資料
     * 
     * @param jarPath JAR 檔案路徑
     * @return 模組中繼資料列表（一個 JAR 可能包含多個模組）
     * @throws IOException 讀取失敗時拋出
     */
    public static List<ForgeModMetadata> parseModMetadata(Path jarPath) throws IOException {
        List<ForgeModMetadata> metadataList = new ArrayList<>();
        
        if (!isForgeMod(jarPath)) {
            return metadataList;
        }
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // 先嘗試解析新版 mods.toml
            JarEntry modsTomlEntry = jarFile.getJarEntry(MODS_TOML_PATH);
            if (modsTomlEntry != null) {
                String content = readJarEntry(jarFile, modsTomlEntry);
                metadataList.addAll(parseModsToml(content, jarPath.toString()));
            }
            
            // 再嘗試解析舊版 mcmod.info
            JarEntry mcModInfoEntry = jarFile.getJarEntry(MCMOD_INFO_PATH);
            if (mcModInfoEntry != null) {
                String content = readJarEntry(jarFile, mcModInfoEntry);
                metadataList.addAll(parseMcModInfo(content, jarPath.toString()));
            }
            
            // 如果都沒有解析到，嘗試從其他線索推斷
            if (metadataList.isEmpty()) {
                ForgeModMetadata inferred = inferModMetadata(jarFile, jarPath.toString());
                if (inferred != null) {
                    metadataList.add(inferred);
                }
            }
        }
        
        return metadataList;
    }
    
    /**
     * 解析 mcmod.info 檔案（舊版格式）
     * 
     * mcmod.info 是 JSON 格式，包含一個或多個模組定義
     */
    private static List<ForgeModMetadata> parseMcModInfo(String content, String sourcePath) {
        List<ForgeModMetadata> metadataList = new ArrayList<>();
        
        try {
            // 簡單的 JSON 解析（不依賴外部函式庫）
            // mcmod.info 格式通常是一個 JSON 陣列
            content = content.trim();
            
            // 移除開頭的 [ 和結尾的 ]
            if (content.startsWith("[")) {
                content = content.substring(1);
            }
            if (content.endsWith("]")) {
                content = content.substring(0, content.length() - 1);
            }
            
            // 分割多個模組物件
            List<String> modObjects = splitJsonObjects(content);
            for (String modObj : modObjects) {
                ForgeModMetadata metadata = parseSingleMcModInfo(modObj);
                if (metadata != null && metadata.isValid()) {
                    metadata.setSourcePath(sourcePath);
                    metadata.setFormatVersion(1);
                    metadataList.add(metadata);
                }
            }
            
        } catch (Exception e) {
            // 解析失敗，嘗試簡單提取
            ForgeModMetadata fallback = new ForgeModMetadata();
            fallback.setModId("unknown_forge_mod");
            fallback.setName("Unknown Forge Mod");
            fallback.setVersion("0.0.0");
            fallback.setSourcePath(sourcePath);
            fallback.setFormatVersion(1);
            metadataList.add(fallback);
        }
        
        return metadataList;
    }
    
    /**
     * 解析單個 mcmod.info 物件
     */
    private static ForgeModMetadata parseSingleMcModInfo(String json) {
        ForgeModMetadata metadata = new ForgeModMetadata();
        
        // 提取各個欄位
        metadata.setModId(extractJsonString(json, "modid"));
        metadata.setName(extractJsonString(json, "name"));
        metadata.setVersion(extractJsonString(json, "version"));
        metadata.setDescription(extractJsonString(json, "description"));
        metadata.setMcVersion(extractJsonString(json, "mcversion"));
        metadata.setModClass(extractJsonString(json, "modClass"));
        
        // 作者列表
        String authorsStr = extractJsonString(json, "authors");
        if (authorsStr != null && !authorsStr.isEmpty()) {
            metadata.setAuthors(new String[]{authorsStr});
        }
        
        // 依賴列表（簡單處理）
        String depsStr = extractJsonString(json, "dependencies");
        if (depsStr != null && !depsStr.isEmpty()) {
            metadata.setDependencies(new ForgeModMetadata.Dependency[0]);
        }
        
        return metadata;
    }
    
    /**
     * 解析 mods.toml 檔案（新版格式）
     * 
     * mods.toml 是 TOML 格式，用於 Forge 1.13+
     */
    private static List<ForgeModMetadata> parseModsToml(String content, String sourcePath) {
        List<ForgeModMetadata> metadataList = new ArrayList<>();
        
        try {
            // 簡單的 TOML 解析
            // 找到 [[mods]] 區段
            String[] sections = content.split("\\[\\[mods\\]\\]");
            
            for (int i = 1; i < sections.length; i++) {
                String section = sections[i];
                // 找到下一個 [[ 或 [ 之前的內容
                int nextSection = section.indexOf("[[");
                if (nextSection == -1) {
                    nextSection = section.indexOf("\n[");
                }
                if (nextSection != -1) {
                    section = section.substring(0, nextSection);
                }
                
                ForgeModMetadata metadata = parseSingleModsToml(section);
                if (metadata != null && metadata.isValid()) {
                    metadata.setSourcePath(sourcePath);
                    metadata.setFormatVersion(2);
                    metadataList.add(metadata);
                }
            }
            
            // 如果沒有找到 [[mods]]，嘗試從頂層提取
            if (metadataList.isEmpty()) {
                ForgeModMetadata metadata = parseSingleModsToml(content);
                if (metadata != null && metadata.isValid()) {
                    metadata.setSourcePath(sourcePath);
                    metadata.setFormatVersion(2);
                    metadataList.add(metadata);
                }
            }
            
        } catch (Exception e) {
            // 解析失敗
            ForgeModMetadata fallback = new ForgeModMetadata();
            fallback.setModId("unknown_forge_mod");
            fallback.setName("Unknown Forge Mod");
            fallback.setVersion("0.0.0");
            fallback.setSourcePath(sourcePath);
            fallback.setFormatVersion(2);
            metadataList.add(fallback);
        }
        
        return metadataList;
    }
    
    /**
     * 解析單個 mods.toml 模組區段
     */
    private static ForgeModMetadata parseSingleModsToml(String section) {
        ForgeModMetadata metadata = new ForgeModMetadata();
        
        // 提取各個欄位（TOML 格式：key = "value"）
        metadata.setModId(extractTomlString(section, "modId"));
        metadata.setName(extractTomlString(section, "displayName"));
        metadata.setVersion(extractTomlString(section, "version"));
        metadata.setDescription(extractTomlString(section, "description"));
        
        // 作者
        String authorsStr = extractTomlString(section, "authors");
        if (authorsStr != null && !authorsStr.isEmpty()) {
            metadata.setAuthors(authorsStr.split(",\\s*"));
        }
        
        // Minecraft 版本
        String mcVersion = extractTomlString(section, "minecraftVersion");
        if (mcVersion == null) {
            mcVersion = extractTomlString(section, "mcVersion");
        }
        metadata.setMcVersion(mcVersion);
        
        return metadata;
    }
    
    /**
     * 從 JAR 檔案的其他線索推斷模組資訊
     */
    private static ForgeModMetadata inferModMetadata(JarFile jarFile, String sourcePath) {
        // 嘗試從 MANIFEST.MF 獲取資訊
        try {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                String modId = attrs.getValue("FMLModType");
                if (modId != null) {
                    ForgeModMetadata metadata = new ForgeModMetadata();
                    metadata.setModId(modId);
                    metadata.setName(attrs.getValue("Implementation-Title"));
                    metadata.setVersion(attrs.getValue("Implementation-Version"));
                    metadata.setSourcePath(sourcePath);
                    return metadata;
                }
            }
        } catch (IOException e) {
            // 忽略
        }
        
        // 嘗試從檔名推斷
        String fileName = new File(sourcePath).getName();
        if (fileName.endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        
        ForgeModMetadata metadata = new ForgeModMetadata();
        metadata.setModId(fileName.toLowerCase().replaceAll("[^a-z0-9_-]", "_"));
        metadata.setName(fileName);
        metadata.setVersion("0.0.0");
        metadata.setSourcePath(sourcePath);
        return metadata;
    }
    
    /**
     * 讀取 JAR 條目內容
     */
    private static String readJarEntry(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
    
    /**
     * 從 JSON 字串中提取字串值
     */
    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    /**
     * 從 TOML 字串中提取字串值
     */
    private static String extractTomlString(String content, String key) {
        String pattern = key + "\\s*=\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    /**
     * 分割 JSON 物件陣列
     */
    private static List<String> splitJsonObjects(String content) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    objects.add(content.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        
        return objects;
    }
    
    /**
     * 掃描目錄中的所有 Forge 模組
     * 
     * @param modsDir 模組目錄
     * @return Forge 模組中繼資料列表
     */
    public static List<ForgeModMetadata> scanForgeMods(Path modsDir) throws IOException {
        List<ForgeModMetadata> allMods = new ArrayList<>();
        
        if (!Files.exists(modsDir) || !Files.isDirectory(modsDir)) {
            return allMods;
        }
        
        try (Stream<Path> paths = Files.list(modsDir)) {
            List<Path> jarFiles = paths
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
            
            for (Path jarFile : jarFiles) {
                if (isForgeMod(jarFile)) {
                    allMods.addAll(parseModMetadata(jarFile));
                }
            }
        }
        
        return allMods;
    }
}
