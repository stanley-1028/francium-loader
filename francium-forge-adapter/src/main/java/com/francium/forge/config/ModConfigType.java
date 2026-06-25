package com.francium.forge.config;

/**
 * Forge 設定檔類型
 * 
 * 對應 Forge 的 ModConfig.Type
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public enum ModConfigType {
    
    /** 通用設定，兩端都會載入 */
    COMMON("common"),
    
    /** 用戶端設定，僅用戶端載入 */
    CLIENT("client"),
    
    /** 伺服器設定，僅伺服器載入 */
    SERVER("server");
    
    private final String directory;
    
    ModConfigType(String directory) {
        this.directory = directory;
    }
    
    /**
     * 取得設定檔所在的目錄名稱
     */
    public String getDirectory() {
        return directory;
    }
    
    /**
     * 取得副檔名
     */
    public String getExtension() {
        return ".toml";
    }
    
    /**
     * 檢查是否為伺服端設定
     */
    public boolean isServer() {
        return this == SERVER;
    }
    
    /**
     * 檢查是否為用戶端設定
     */
    public boolean isClient() {
        return this == CLIENT;
    }
}
