package com.francium.forge.lifecycle;

/**
 * FML 生命週期階段
 * 
 * 模擬 Forge Mod Loader (FML) 的生命週期階段
 * 參考 Forge 的 LoadingStage 設計
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public enum FMLLifecycle {
    
    /** 初始階段 - 模組剛被發現 */
    CONSTRUCTION("construction", "Construction"),
    
    /** 預初始化階段 - 註冊內容、設定基礎配置 */
    PRE_INITIALIZATION("pre_initialization", "Pre-Initialization"),
    
    /** 初始化階段 - 主要的模組初始化 */
    INITIALIZATION("initialization", "Initialization"),
    
    /** 後初始化階段 - 跨模組整合 */
    POST_INITIALIZATION("post_initialization", "Post-Initialization"),
    
    /** 載入完成階段 - 所有模組都已載入 */
    LOAD_COMPLETE("load_complete", "Load Complete"),
    
    /** 伺服器開始階段 - 伺服器即將啟動 */
    SERVER_ABOUT_TO_START("server_about_to_start", "Server About to Start"),
    
    /** 伺服器開始階段 - 伺服器已啟動 */
    SERVER_STARTED("server_started", "Server Started"),
    
    /** 伺服器停止階段 - 伺服器即將停止 */
    SERVER_STOPPING("server_stopping", "Server Stopping"),
    
    /** 伺服器已停止階段 */
    SERVER_STOPPED("server_stopped", "Server Stopped"),
    
    /** 錯誤狀態 */
    ERRORED("errored", "Errored");
    
    private final String id;
    private final String displayName;
    
    FMLLifecycle(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 取得下一個生命週期階段
     */
    public FMLLifecycle next() {
        FMLLifecycle[] values = values();
        int nextIndex = ordinal() + 1;
        if (nextIndex < values.length && values[nextIndex] != ERRORED) {
            return values[nextIndex];
        }
        return this;
    }
    
    /**
     * 檢查是否為主要的載入階段
     */
    public boolean isLoadingStage() {
        return this == CONSTRUCTION 
            || this == PRE_INITIALIZATION 
            || this == INITIALIZATION 
            || this == POST_INITIALIZATION 
            || this == LOAD_COMPLETE;
    }
    
    /**
     * 檢查是否為伺服器相關階段
     */
    public boolean isServerStage() {
        return this == SERVER_ABOUT_TO_START 
            || this == SERVER_STARTED 
            || this == SERVER_STOPPING 
            || this == SERVER_STOPPED;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
