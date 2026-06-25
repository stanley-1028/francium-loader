package com.francium.forge;

import com.francium.forge.adapter.ForgeModDetector;
import com.francium.forge.adapter.ForgeModMetadata;
import com.francium.forge.lifecycle.FMLLifecycle;
import com.francium.forge.lifecycle.FMLLifecycleManager;
import com.francium.forge.lifecycle.ForgeModContainer;
import com.francium.forge.registry.ForgeRegistryManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Forge 適配器主類別
 * 
 * Francium 的 Forge 模組適配層，負責載入和管理 Forge 模組
 * 
 * 這是 Forge 適配層的主要入口點，整合了：
 * - 模組偵測和解析
 * - FML 生命週期管理
 * - 註冊系統
 * - 事件系統
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeAdapter {
    
    /** 適配器版本 */
    public static final String VERSION = "2.5.0";
    
    /** 生命週期管理器 */
    private final FMLLifecycleManager lifecycleManager;
    
    /** 註冊表管理器 */
    private final ForgeRegistryManager registryManager;
    
    /** 是否初始化 */
    private boolean initialized = false;
    
    /**
     * 建立 Forge 適配器
     */
    public ForgeAdapter() {
        this.lifecycleManager = new FMLLifecycleManager();
        this.registryManager = ForgeRegistryManager.getInstance();
    }
    
    /**
     * 初始化適配器
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        // 初始化標準註冊表
        initializeStandardRegistries();
        
        initialized = true;
    }
    
    /**
     * 初始化標準註冊表
     */
    private void initializeStandardRegistries() {
        // 這裡可以預先建立一些標準的註冊表
        // 實際上這些註冊表應該在 Minecraft 初始化時建立
    }
    
    /**
     * 掃描並載入 Forge 模組
     * 
     * @param modsDir 模組目錄
     * @return 載入的模組數量
     * @throws IOException 讀取失敗時拋出
     */
    public int loadMods(Path modsDir) throws IOException {
        if (!initialized) {
            initialize();
        }
        
        // 掃描 Forge 模組
        List<ForgeModMetadata> modMetadataList = ForgeModDetector.scanForgeMods(modsDir);
        
        // 註冊所有模組
        for (ForgeModMetadata metadata : modMetadataList) {
            try {
                lifecycleManager.registerMod(metadata);
            } catch (Exception e) {
                System.err.println("Failed to register Forge mod: " + metadata.getModId());
                e.printStackTrace();
            }
        }
        
        return modMetadataList.size();
    }
    
    /**
     * 開始模組載入流程
     * 
     * 依次執行所有生命週期階段
     */
    public void startLoading() {
        if (!initialized) {
            initialize();
        }
        
        lifecycleManager.startLoading();
    }
    
    /**
     * 前進到指定的生命週期階段
     * 
     * @param stage 目標階段
     */
    public void advanceTo(FMLLifecycle stage) {
        if (!initialized) {
            initialize();
        }
        
        lifecycleManager.advanceTo(stage);
    }
    
    /**
     * 檢查 JAR 檔案是否為 Forge 模組
     * 
     * @param jarPath JAR 檔案路徑
     * @return 是否為 Forge 模組
     */
    public boolean isForgeMod(Path jarPath) {
        return ForgeModDetector.isForgeMod(jarPath);
    }
    
    /**
     * 解析 Forge 模組中繼資料
     * 
     * @param jarPath JAR 檔案路徑
     * @return 模組中繼資料列表
     * @throws IOException 讀取失敗時拋出
     */
    public List<ForgeModMetadata> parseModMetadata(Path jarPath) throws IOException {
        return ForgeModDetector.parseModMetadata(jarPath);
    }
    
    /**
     * 取得生命週期管理器
     */
    public FMLLifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }
    
    /**
     * 取得註冊表管理器
     */
    public ForgeRegistryManager getRegistryManager() {
        return registryManager;
    }
    
    /**
     * 取得當前生命週期階段
     */
    public FMLLifecycle getCurrentStage() {
        return lifecycleManager.getCurrentStage();
    }
    
    /**
     * 取得已載入的模組數量
     */
    public int getModCount() {
        return lifecycleManager.getModCount();
    }
    
    /**
     * 取得指定模組的容器
     * 
     * @param modId 模組 ID
     * @return 模組容器，如果不存在則傳回 null
     */
    public ForgeModContainer getModContainer(String modId) {
        return lifecycleManager.getModContainer(modId);
    }
    
    /**
     * 檢查載入是否完成
     */
    public boolean isLoadingComplete() {
        return lifecycleManager.isLoadingComplete();
    }
    
    /**
     * 取得有錯誤的模組
     */
    public List<ForgeModContainer> getErroredMods() {
        return lifecycleManager.getErroredMods();
    }
    
    /**
     * 取得適配器版本
     */
    public String getVersion() {
        return VERSION;
    }
    
    /**
     * 檢查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public String toString() {
        return "ForgeAdapter[v" + VERSION + ", mods=" + getModCount() + ", stage=" + getCurrentStage() + "]";
    }
}
