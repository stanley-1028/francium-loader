package com.francium.forge.registry;

import java.util.*;
import java.util.function.Supplier;

/**
 * 延遲註冊工具
 * 
 * 類似 Forge 的 DeferredRegister，用於延遲註冊物件到註冊表
 * 可以在模組建構階段收集所有註冊項目，然後在註冊事件中統一註冊
 * 
 * @param <T> 註冊項目的類型
 * @author Francium Team
 * @since 2.5.0
 */
public class DeferredRegister<T> {
    
    /** 註冊表名稱 */
    private final String registryName;
    
    /** 模組 ID */
    private final String modId;
    
    /** 註冊表類型 */
    private final Class<T> type;
    
    /** 待註冊的項目：名稱 -> 供應商 */
    private final Map<String, Supplier<? extends T>> entries = new LinkedHashMap<>();
    
    /** 是否已註冊 */
    private volatile boolean registered = false;
    
    /**
     * 建立延遲註冊工具
     * 
     * @param registryName 註冊表名稱
     * @param modId 模組 ID
     * @param type 註冊表類型
     * @throws IllegalArgumentException 如果任何參數為 null/空
     */
    public DeferredRegister(String registryName, String modId, Class<T> type) {
        if (registryName == null || registryName.isEmpty()) {
            throw new IllegalArgumentException("Registry name cannot be null or empty");
        }
        if (modId == null || modId.isEmpty()) {
            throw new IllegalArgumentException("Mod ID cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Registry type cannot be null");
        }
        this.registryName = registryName;
        this.modId = modId;
        this.type = type;
    }
    
    /**
     * 建立延遲註冊工具（靜態工廠方法）
     * 
     * @param registryName 註冊表名稱
     * @param modId 模組 ID
     * @param type 註冊表類型
     * @param <T> 註冊項目的類型
     * @return 延遲註冊工具
     */
    public static <T> DeferredRegister<T> create(String registryName, String modId, Class<T> type) {
        return new DeferredRegister<>(registryName, modId, type);
    }
    
    /**
     * 註冊一個項目
     * 
     * @param name 項目名稱（不包含模組 ID）
     * @param supplier 項目供應商
     * @param <I> 項目的具體類型
     * @return 註冊物件的參考
     * @throws IllegalStateException 如果已經註冊
     * @throws IllegalArgumentException 如果 name 或 supplier 為 null/空
     */
    @SuppressWarnings("unchecked")
    public <I extends T> RegistryObject<I> register(String name, Supplier<I> supplier) {
        if (registered) {
            throw new IllegalStateException("Already registered");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Registry name cannot be null or empty");
        }
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier cannot be null");
        }
        
        String fullKey = modId + ":" + name;
        entries.put(name, supplier);
        
        return new RegistryObject<>(fullKey, (Class<I>) type);
    }
    
    /**
     * 執行註冊，將所有待註冊項目註冊到對應的註冊表
     * 
     * @param registryManager 註冊表管理器
     * @throws IllegalArgumentException 如果 registryManager 為 null
     */
    @SuppressWarnings("unchecked")
    public void registerAll(ForgeRegistryManager registryManager) {
        if (registered) {
            return;
        }
        if (registryManager == null) {
            throw new IllegalArgumentException("Registry manager cannot be null");
        }
        
        ForgeRegistry<T> registry = registryManager.getOrCreateRegistry(registryName, type);
        
        for (Map.Entry<String, Supplier<? extends T>> entry : entries.entrySet()) {
            String name = entry.getKey();
            String fullKey = modId + ":" + name;
            T value = entry.getValue().get();
            
            if (value == null) {
                throw new IllegalStateException("Supplier returned null for: " + fullKey);
            }
            
            registry.register(fullKey, value);
        }
        
        registered = true;
    }
    
    /**
     * 執行註冊（使用預設的註冊表管理器）
     */
    public void registerAll() {
        registerAll(ForgeRegistryManager.getInstance());
    }
    
    /**
     * 取得註冊表名稱
     */
    public String getRegistryName() {
        return registryName;
    }
    
    /**
     * 取得模組 ID
     */
    public String getModId() {
        return modId;
    }
    
    /**
     * 取得註冊表類型
     */
    public Class<T> getType() {
        return type;
    }
    
    /**
     * 取得待註冊的項目數量
     */
    public int getEntryCount() {
        return entries.size();
    }
    
    /**
     * 檢查是否已註冊
     */
    public boolean isRegistered() {
        return registered;
    }
    
    /**
     * 取得所有待註冊的項目名稱
     */
    public Set<String> getEntryNames() {
        return Collections.unmodifiableSet(entries.keySet());
    }
    
    /**
     * 註冊物件參考
     * 
     * 類似 Forge 的 RegistryObject，用於延遲取得註冊的物件
     * 
     * @param <I> 物件類型
     */
    public static class RegistryObject<I> {
        
        /** 完整的資源位置 */
        private final String key;
        
        /** 物件類型 */
        private final Class<I> type;
        
        /** 快取的物件實例 */
        private volatile I cached;
        
        /** 是否已快取 */
        private volatile boolean cachedFlag = false;
        
        RegistryObject(String key, Class<I> type) {
            this.key = key;
            this.type = type;
        }
        
        /**
         * 取得註冊的物件
         * 
         * @return 註冊的物件
         * @throws IllegalStateException 如果物件尚未註冊
         */
        @SuppressWarnings("unchecked")
        public I get() {
            if (cachedFlag) {
                return cached;
            }
            
            // 從註冊表中取得
            String registryName = extractRegistryName(key);
            ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
            ForgeRegistry<?> registry = manager.getRegistry(registryName);
            
            if (registry == null) {
                throw new IllegalStateException("Registry not found: " + registryName);
            }
            
            Object value = registry.getValue(key);
            if (value == null) {
                throw new IllegalStateException("Object not registered: " + key);
            }
            
            cached = (I) value;
            cachedFlag = true;
            
            return cached;
        }
        
        /**
         * 取得資源位置
         */
        public String getId() {
            return key;
        }
        
        /**
         * 檢查物件是否已註冊
         */
        public boolean isPresent() {
            try {
                get();
                return true;
            } catch (IllegalStateException e) {
                return false;
            }
        }
        
        /**
         * 從鍵中提取註冊表名稱
         * 這是一個簡單的實現，實際上需要更複雜的邏輯
         */
        private String extractRegistryName(String key) {
            // 簡單實現：根據命名空間推斷
            // 實際上應該有更好的方式來關聯 RegistryObject 和其所屬的註冊表
            return "blocks"; // 預設，實際上應該從外部傳入
        }
        
        @Override
        public String toString() {
            return "RegistryObject[" + key + "]";
        }
    }
}
