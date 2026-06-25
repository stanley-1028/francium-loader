package com.francium.forge.capability;

import java.util.HashMap;
import java.util.Map;

/**
 * 能力管理員
 * 
 * 對應 Forge 的 CapabilityManager
 * 管理所有已註冊的能力
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class CapabilityManager {
    
    /** 執行個體 */
    private static final CapabilityManager INSTANCE = new CapabilityManager();
    
    /** 已註冊的能力 */
    private final Map<String, Capability<?>> capabilities = new HashMap<>();
    
    /** 是否已初始化 */
    private boolean initialized = false;
    
    /**
     * 私有建構子
     */
    private CapabilityManager() {}
    
    /**
     * 取得執行個體
     */
    public static CapabilityManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 註冊能力
     * 
     * @param capability 能力
     */
    public synchronized <T> void register(Capability<T> capability) {
        if (initialized) {
            throw new IllegalStateException("Capabilities already initialized");
        }
        if (capabilities.containsKey(capability.getName())) {
            throw new IllegalArgumentException("Capability already registered: " + capability.getName());
        }
        capabilities.put(capability.getName(), capability);
    }
    
    /**
     * 取得能力
     * 
     * @param name 能力名稱
     * @return 能力，如果不存在則傳回 null
     */
    @SuppressWarnings("unchecked")
    public <T> Capability<T> getCapability(String name) {
        return (Capability<T>) capabilities.get(name);
    }
    
    /**
     * 檢查能力是否存在
     * 
     * @param name 能力名稱
     * @return 是否存在
     */
    public boolean hasCapability(String name) {
        return capabilities.containsKey(name);
    }
    
    /**
     * 取得所有已註冊的能力數量
     */
    public int getCapabilityCount() {
        return capabilities.size();
    }
    
    /**
     * 初始化（標記為已初始化，之後不能再註冊）
     */
    public synchronized void initialize() {
        initialized = true;
    }
    
    /**
     * 檢查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 重設（僅用於測試）
     */
    synchronized void reset() {
        capabilities.clear();
        initialized = false;
    }
}
