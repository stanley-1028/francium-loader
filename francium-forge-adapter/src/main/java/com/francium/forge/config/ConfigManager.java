package com.francium.forge.config;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 設定管理員
 * 
 * 對應 Forge 的 ConfigTracker / ModConfigLoader
 * 管理所有模組的設定檔
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ConfigManager {
    
    /** 單例實例 */
    private static ConfigManager instance;
    
    /** 設定檔目錄 */
    private Path configDir;
    
    /** 所有設定檔：modId -> (type -> config) */
    private final Map<String, Map<ModConfigType, ModConfig>> configs = new ConcurrentHashMap<>();
    
    /**
     * 私有建構子
     */
    private ConfigManager() {}
    
    /**
     * 取得單例實例
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    /**
     * 初始化設定管理員
     * 
     * @param configDir 設定檔目錄
     */
    public void initialize(Path configDir) {
        this.configDir = configDir;
        try {
            Files.createDirectories(configDir);
        } catch (Exception e) {
            // 忽略，目錄可能已存在
        }
    }
    
    /**
     * 取得設定檔目錄
     */
    public Path getConfigDir() {
        return configDir;
    }
    
    /**
     * 註冊設定檔
     * 
     * @param modId 模組 ID
     * @param type 設定類型
     * @param spec 設定規格
     * @return 設定檔實例
     */
    public ModConfig registerConfig(String modId, ModConfigType type, ModConfigSpec spec) {
        ModConfig config = new ModConfig(type, modId, spec);
        
        // 設定檔案路徑
        Path typeDir = configDir.resolve(type.getDirectory());
        Path configPath = typeDir.resolve(modId + type.getExtension());
        config.setPath(configPath);
        
        // 註冊
        configs.computeIfAbsent(modId, k -> new EnumMap<>(ModConfigType.class))
               .put(type, config);
        
        return config;
    }
    
    /**
     * 載入所有設定檔
     */
    public void loadAll() {
        for (Map<ModConfigType, ModConfig> modConfigs : configs.values()) {
            for (ModConfig config : modConfigs.values()) {
                try {
                    config.load();
                } catch (Exception e) {
                    // 記錄錯誤但繼續
                    System.err.println("[ConfigManager] Failed to load config: " + config.getModId() + " - " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 儲存所有設定檔
     */
    public void saveAll() {
        for (Map<ModConfigType, ModConfig> modConfigs : configs.values()) {
            for (ModConfig config : modConfigs.values()) {
                try {
                    config.save();
                } catch (Exception e) {
                    System.err.println("[ConfigManager] Failed to save config: " + config.getModId() + " - " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 取得模組的特定類型設定
     */
    public ModConfig getConfig(String modId, ModConfigType type) {
        Map<ModConfigType, ModConfig> modConfigs = configs.get(modId);
        if (modConfigs != null) {
            return modConfigs.get(type);
        }
        return null;
    }
    
    /**
     * 取得模組的所有設定
     */
    public Map<ModConfigType, ModConfig> getConfigs(String modId) {
        return configs.getOrDefault(modId, Collections.emptyMap());
    }
    
    /**
     * 取得所有已註冊的模組 ID
     */
    public Set<String> getRegisteredMods() {
        return Collections.unmodifiableSet(configs.keySet());
    }
    
    /**
     * 取得設定總數
     */
    public int getConfigCount() {
        int count = 0;
        for (Map<ModConfigType, ModConfig> modConfigs : configs.values()) {
            count += modConfigs.size();
        }
        return count;
    }
    
    /**
     * 重設所有設定為預設值
     */
    public void resetAll() {
        for (Map<ModConfigType, ModConfig> modConfigs : configs.values()) {
            for (ModConfig config : modConfigs.values()) {
                config.reset();
            }
        }
    }
}
