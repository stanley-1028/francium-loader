package com.francium.forge.lifecycle;

import com.francium.forge.adapter.ForgeModMetadata;

/**
 * Forge 模組容器
 * 
 * 封裝一個 Forge 模組的實例和狀態
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeModContainer {
    
    /** 模組中繼資料 */
    private final ForgeModMetadata metadata;
    
    /** 模組實例 */
    private Object modInstance;
    
    /** 模組類別 */
    private Class<?> modClass;
    
    /** 當前生命週期階段 */
    private FMLLifecycle currentStage = FMLLifecycle.CONSTRUCTION;
    
    /** 載入錯誤 */
    private Throwable error;
    
    /** 是否載入成功 */
    private boolean loaded = false;
    
    public ForgeModContainer(ForgeModMetadata metadata) {
        this.metadata = metadata;
    }
    
    /**
     * 取得模組 ID
     */
    public String getModId() {
        return metadata.getModId();
    }
    
    /**
     * 取得模組名稱
     */
    public String getName() {
        return metadata.getName();
    }
    
    /**
     * 取得模組版本
     */
    public String getVersion() {
        return metadata.getVersion();
    }
    
    /**
     * 取得模組中繼資料
     */
    public ForgeModMetadata getMetadata() {
        return metadata;
    }
    
    /**
     * 取得模組實例
     */
    public Object getModInstance() {
        return modInstance;
    }
    
    /**
     * 設定模組實例
     */
    public void setModInstance(Object modInstance) {
        this.modInstance = modInstance;
    }
    
    /**
     * 取得模組類別
     */
    public Class<?> getModClass() {
        return modClass;
    }
    
    /**
     * 設定模組類別
     */
    public void setModClass(Class<?> modClass) {
        this.modClass = modClass;
    }
    
    /**
     * 取得當前生命週期階段
     */
    public FMLLifecycle getCurrentStage() {
        return currentStage;
    }
    
    /**
     * 前進到指定的生命週期階段
     * 
     * 子類別可以覆寫這個方法來執行對應階段的邏輯
     */
    public void advanceTo(FMLLifecycle stage) {
        if (stage.ordinal() <= currentStage.ordinal()) {
            return;
        }
        this.currentStage = stage;
    }
    
    /**
     * 取得載入錯誤
     */
    public Throwable getError() {
        return error;
    }
    
    /**
     * 設定載入錯誤
     */
    public void setError(Throwable error) {
        this.error = error;
    }
    
    /**
     * 檢查是否有錯誤
     */
    public boolean hasError() {
        return error != null;
    }
    
    /**
     * 檢查是否載入成功
     */
    public boolean isLoaded() {
        return loaded && !hasError();
    }
    
    /**
     * 設定是否載入成功
     */
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
    
    @Override
    public String toString() {
        return getModId() + " " + getVersion() + " [" + currentStage + "]";
    }
}
