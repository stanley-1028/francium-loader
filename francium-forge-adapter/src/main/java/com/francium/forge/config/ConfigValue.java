package com.francium.forge.config;

import java.util.function.Supplier;

/**
 * 設定項目的包裝類別
 * 
 * 對應 Forge 的 ConfigValue
 * 
 * @author Francium Team
 * @since 2.5.0
 * @param <T> 設定值的類型
 */
public class ConfigValue<T> implements Supplier<T> {
    
    /** 設定路徑 */
    private final String path;
    
    /** 預設值 */
    private final T defaultValue;
    
    /** 目前值 */
    private T value;
    
    /** 註解 */
    private final String comment;
    
    /** 是否需要重新啟動 */
    private final boolean requiresWorldRestart;
    private final boolean requiresMcRestart;
    
    /**
     * 建立設定項目
     * 
     * @param path 設定路徑
     * @param defaultValue 預設值
     * @param comment 註解
     * @param requiresWorldRestart 是否需要重新載入世界
     * @param requiresMcRestart 是否需要重新啟動遊戲
     */
    ConfigValue(String path, T defaultValue, String comment, 
                boolean requiresWorldRestart, boolean requiresMcRestart) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.comment = comment;
        this.requiresWorldRestart = requiresWorldRestart;
        this.requiresMcRestart = requiresMcRestart;
    }
    
    /**
     * 取得設定值
     */
    @Override
    public T get() {
        return value;
    }
    
    /**
     * 取得設定路徑
     */
    public String getPath() {
        return path;
    }
    
    /**
     * 取得預設值
     */
    public T getDefault() {
        return defaultValue;
    }
    
    /**
     * 取得註解
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * 設定新值
     */
    public void set(T value) {
        this.value = value;
    }
    
    /**
     * 重設為預設值
     */
    public void reset() {
        this.value = defaultValue;
    }
    
    /**
     * 檢查是否需要重新載入世界
     */
    public boolean requiresWorldRestart() {
        return requiresWorldRestart;
    }
    
    /**
     * 檢查是否需要重新啟動遊戲
     */
    public boolean requiresMcRestart() {
        return requiresMcRestart;
    }
    
    @Override
    public String toString() {
        return path + " = " + value + " (default: " + defaultValue + ")";
    }
}
