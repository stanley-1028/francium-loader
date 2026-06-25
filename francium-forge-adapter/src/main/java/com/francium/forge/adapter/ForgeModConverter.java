package com.francium.forge.adapter;

import com.francium.loader.ModManifest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Forge 模組轉換工具
 * 
 * 將 Forge 模組中繼資料轉換為 Francium 的 ModManifest 格式
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeModConverter {
    
    /**
     * 將 Forge 模組中繼資料轉換為 Francium ModManifest
     * 
     * @param forgeMetadata Forge 模組中繼資料
     * @return Francium ModManifest
     */
    public static ModManifest toModManifest(ForgeModMetadata forgeMetadata) {
        if (forgeMetadata == null || !forgeMetadata.isValid()) {
            return null;
        }
        
        ModManifest.Builder builder = ModManifest.builder()
            .modId(forgeMetadata.getModId())
            .version(forgeMetadata.getVersion())
            .name(forgeMetadata.getName())
            .description(forgeMetadata.getDescription())
            .authors(forgeMetadata.getAuthors() != null ? 
                String.join(", ", forgeMetadata.getAuthors()) : null)
            .mcVersion(forgeMetadata.getMcVersion())
            .aiBridge(false); // Forge 模組預設關閉 AI 橋接，需要更謹慎
        
        // 轉換依賴
        if (forgeMetadata.getDependencies() != null) {
            for (ForgeModMetadata.Dependency dep : forgeMetadata.getDependencies()) {
                String versionRange = convertVersionRange(dep.getVersionRange());
                if (dep.isMandatory()) {
                    builder.addDependency(dep.getModId(), versionRange);
                } else {
                    builder.addOptionalDependency(dep.getModId(), versionRange);
                }
            }
        }
        
        // 設定來源路徑
        if (forgeMetadata.getSourcePath() != null) {
            ModManifest manifest = builder.build();
            manifest.jarSourcePath = forgeMetadata.getSourcePath();
            return manifest;
        }
        
        return builder.build();
    }
    
    /**
     * 將 Forge 版本範圍轉換為 Francium 格式
     * 
     * Forge 格式示例：
     * - "[1.0.0,)" - 大於等於 1.0.0
     * - "(,2.0.0]" - 小於等於 2.0.0
     * - "[1.0.0,2.0.0]" - 1.0.0 到 2.0.0 之間
     * 
     * Francium 格式示例：
     * - "^1.0.0" - 相容 1.x.x
     * - "~1.0.0" - 相容 1.0.x
     * - ">=1.0.0" - 大於等於
     * - "<=2.0.0" - 小於等於
     */
    private static String convertVersionRange(String forgeRange) {
        if (forgeRange == null || forgeRange.isEmpty()) {
            return "*";
        }
        
        // 簡單轉換，覆蓋常見情況
        // 實際上需要更完整的版本範圍解析器
        
        if (forgeRange.equals("*") || forgeRange.isEmpty()) {
            return "*";
        }
        
        // Forge 常用的 "[1.0,)" -> ">=1.0"
        if (forgeRange.startsWith("[") && forgeRange.endsWith(",)")) {
            String version = forgeRange.substring(1, forgeRange.length() - 2);
            return ">=" + version;
        }
        
        // Forge 常用的 "(,2.0]" -> "<=2.0"
        if (forgeRange.startsWith("(,") && forgeRange.endsWith("]")) {
            String version = forgeRange.substring(2, forgeRange.length() - 1);
            return "<=" + version;
        }
        
        // 預設：直接使用（可能不完全相容，但能工作）
        return forgeRange;
    }
    
    /**
     * 從 JAR 檔案載入 Forge 模組並轉換為 ModManifest 列表
     * 
     * @param jarPath JAR 檔案路徑
     * @return ModManifest 列表
     */
    public static List<ModManifest> loadAndConvert(Path jarPath) {
        List<ForgeModMetadata> forgeMods = ForgeModDetector.parseModMetadata(jarPath);
        List<ModManifest> result = new ArrayList<>();
        
        for (ForgeModMetadata forgeMod : forgeMods) {
            ModManifest manifest = toModManifest(forgeMod);
            if (manifest != null) {
                result.add(manifest);
            }
        }
        
        return result;
    }
    
    /**
     * 掃描目錄中的所有 Forge 模組並轉換
     * 
     * @param modsDir 模組目錄
     * @return ModManifest 列表
     */
    public static List<ModManifest> scanAndConvert(Path modsDir) {
        List<ForgeModMetadata> forgeMods = ForgeModDetector.scanForgeMods(modsDir);
        List<ModManifest> result = new ArrayList<>();
        
        for (ForgeModMetadata forgeMod : forgeMods) {
            ModManifest manifest = toModManifest(forgeMod);
            if (manifest != null) {
                result.add(manifest);
            }
        }
        
        return result;
    }
}
