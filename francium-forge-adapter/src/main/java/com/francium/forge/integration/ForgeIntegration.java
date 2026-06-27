package com.francium.forge.integration;

import com.francium.forge.ForgeAdapter;
import com.francium.forge.registry.ForgeRegistryManager;
import com.francium.forge.registry.ForgeContentRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Forge 適配層整合器
 * 
 * 負責將 Forge 適配層與 Francium 核心加載器整合
 * 提供統一的初始化和管理介面
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeIntegration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeIntegration.class);
    
    /** 單例實例 */
    private static volatile ForgeIntegration instance;
    
    /** Forge 適配器 */
    private ForgeAdapter forgeAdapter;
    
    /** 是否已初始化 */
    private volatile boolean initialized = false;
    
    /** 遊戲目錄 */
    private Path gameDir;
    
    /** mods 目錄 */
    private Path modsDir;
    
    /** config 目錄 */
    private Path configDir;
    
    /**
     * 取得單例實例
     */
    public static ForgeIntegration getInstance() {
        if (instance == null) {
            synchronized (ForgeIntegration.class) {
                if (instance == null) {
                    instance = new ForgeIntegration();
                }
            }
        }
        return instance;
    }
    
    /**
     * 私有建構子
     */
    private ForgeIntegration() {
    }
    
    /**
     * 初始化 Forge 整合
     * 
     * @param gameDir 遊戲目錄
     * @param modsDir mods 目錄
     * @param configDir config 目錄
     */
    public void initialize(Path gameDir, Path modsDir, Path configDir) {
        if (initialized) {
            LOGGER.warn("ForgeIntegration already initialized");
            return;
        }
        
        if (gameDir == null) {
            throw new IllegalArgumentException("Game directory cannot be null");
        }
        
        this.gameDir = gameDir;
        this.modsDir = modsDir != null ? modsDir : gameDir.resolve("mods");
        this.configDir = configDir != null ? configDir : gameDir.resolve("config");
        
        LOGGER.info("Initializing Forge Adapter Integration...");
        
        try {
            // 1. 初始化註冊表管理器
            ForgeRegistryManager registryManager = ForgeRegistryManager.getInstance();
            LOGGER.info("  - ForgeRegistryManager initialized ({} registries)", 
                registryManager.getRegistryCount());
            
            // 2. 註冊內建內容（附魔、藥水效果、屬性等）
            ForgeContentRegistries.registerBuiltinContent();
            LOGGER.info("  - Builtin content registered ({} enchantments, {} effects, {} attributes)",
                ForgeContentRegistries.getEnchantmentCount(),
                ForgeContentRegistries.getEffectCount(),
                ForgeContentRegistries.getAttributeCount());
            
            // 3. 初始化 Forge 適配器
            this.forgeAdapter = new ForgeAdapter();
            this.forgeAdapter.initialize();
            LOGGER.info("  - ForgeAdapter initialized (version: {})", ForgeAdapter.VERSION);
            
            this.initialized = true;
            LOGGER.info("Forge Adapter Integration initialized successfully!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Forge Integration: " + e.getMessage(), e);
            throw new RuntimeException("Forge Integration initialization failed", e);
        }
    }
    
    /**
     * 掃描並載入 Forge 模組
     * 
     * @return 偵測到的 Forge 模組數量
     */
    public int scanAndLoadForgeMods() {
        if (!initialized) {
            throw new IllegalStateException("ForgeIntegration not initialized");
        }
        
        LOGGER.info("Scanning for Forge mods in: {}", modsDir);
        
        try {
            // 使用 ForgeAdapter 載入所有 Forge 模組
            int loadedCount = forgeAdapter.loadMods(modsDir);
            
            LOGGER.info("Loaded {} Forge mods", loadedCount);
            
            // 啟動 Forge 生命週期
            if (loadedCount > 0) {
                forgeAdapter.startLoading();
                LOGGER.info("Forge mod loading started, advancing lifecycle...");
                
                // 推進生命週期到 PRE_INITIALIZATION
                forgeAdapter.advanceTo(
                    com.francium.forge.lifecycle.FMLLifecycle.PRE_INITIALIZATION);
            }
            
            return loadedCount;
            
        } catch (Exception e) {
            LOGGER.error("Failed to scan Forge mods: " + e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 推進 Forge 生命週期
     * 
     * @param lifecycle 目標生命週期階段
     */
    public void advanceLifecycle(com.francium.forge.lifecycle.FMLLifecycle lifecycle) {
        if (!initialized) {
            throw new IllegalStateException("ForgeIntegration not initialized");
        }
        
        if (forgeAdapter != null) {
            forgeAdapter.advanceTo(lifecycle);
            LOGGER.info("Forge lifecycle advanced to: {}", lifecycle);
        }
    }
    
    /**
     * 取得 Forge 適配器
     */
    public ForgeAdapter getForgeAdapter() {
        return forgeAdapter;
    }
    
    /**
     * 檢查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 取得遊戲目錄
     */
    public Path getGameDir() {
        return gameDir;
    }
    
    /**
     * 取得 mods 目錄
     */
    public Path getModsDir() {
        return modsDir;
    }
    
    /**
     * 取得 config 目錄
     */
    public Path getConfigDir() {
        return configDir;
    }
    
    /**
     * 取得已載入的模組數量
     */
    public int getLoadedModCount() {
        if (forgeAdapter != null) {
            return forgeAdapter.getModCount();
        }
        return 0;
    }
    
    /**
     * 關閉整合
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }
        
        LOGGER.info("Shutting down Forge Integration...");
        
        // 推進生命週期到 SERVER_STOPPED
        if (forgeAdapter != null) {
            try {
                forgeAdapter.advanceTo(
                    com.francium.forge.lifecycle.FMLLifecycle.SERVER_STOPPED);
            } catch (Exception e) {
                LOGGER.warn("Error during lifecycle shutdown: " + e.getMessage());
            }
        }
        
        this.initialized = false;
        LOGGER.info("Forge Integration shut down");
    }
}
