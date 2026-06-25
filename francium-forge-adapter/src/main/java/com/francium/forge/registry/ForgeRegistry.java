package com.francium.forge.registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 註冊表
 * 
 * 模擬 Forge 的 IForgeRegistry，用於管理各種遊戲物件的註冊
 * 
 * @param <T> 註冊項目的類型
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeRegistry<T> {
    
    /** 註冊表名稱 */
    private final String name;
    
    /** 註冊表類型 */
    private final Class<T> type;
    
    /** 註冊項目映射：資源位置 -> 註冊項目 */
    private final Map<String, T> entries = new ConcurrentHashMap<>();
    
    /** 反向映射：註冊項目 -> 資源位置 */
    private final Map<T, String> reverseEntries = new IdentityHashMap<>();
    
    /** 註冊順序列表 */
    private final List<String> order = new ArrayList<>();
    
    /** 是否凍結（凍結後不能再註冊） */
    private boolean frozen = false;
    
    /** 預設值（找不到時傳回） */
    private T defaultValue;
    
    public ForgeRegistry(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }
    
    /**
     * 註冊項目
     * 
     * @param key 資源位置（如 "minecraft:stone"）
     * @param value 註冊項目
     * @return 註冊的項目
     * @throws IllegalStateException 如果註冊表已凍結
     * @throws IllegalArgumentException 如果鍵已存在
     */
    public T register(String key, T value) {
        if (frozen) {
            throw new IllegalStateException("Registry is frozen: " + name);
        }
        
        if (entries.containsKey(key)) {
            throw new IllegalArgumentException("Key already registered: " + key);
        }
        
        entries.put(key, value);
        reverseEntries.put(value, key);
        order.add(key);
        
        return value;
    }
    
    /**
     * 根據鍵取得註冊項目
     * 
     * @param key 資源位置
     * @return 註冊項目，如果不存在則傳回預設值或 null
     */
    public T getValue(String key) {
        T value = entries.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
    
    /**
     * 根據項目取得鍵
     * 
     * @param value 註冊項目
     * @return 資源位置，如果不存在則傳回 null
     */
    public String getKey(T value) {
        return reverseEntries.get(value);
    }
    
    /**
     * 檢查鍵是否存在
     */
    public boolean containsKey(String key) {
        return entries.containsKey(key);
    }
    
    /**
     * 檢查值是否存在
     */
    public boolean containsValue(T value) {
        return reverseEntries.containsKey(value);
    }
    
    /**
     * 取得所有鍵
     */
    public Set<String> keySet() {
        return Collections.unmodifiableSet(entries.keySet());
    }
    
    /**
     * 取得所有值
     */
    public Collection<T> values() {
        return Collections.unmodifiableCollection(entries.values());
    }
    
    /**
     * 取得註冊項目數量
     */
    public int size() {
        return entries.size();
    }
    
    /**
     * 取得註冊表名稱
     */
    public String getName() {
        return name;
    }
    
    /**
     * 取得註冊表類型
     */
    public Class<T> getType() {
        return type;
    }
    
    /**
     * 凍結註冊表，禁止進一步註冊
     */
    public void freeze() {
        this.frozen = true;
    }
    
    /**
     * 檢查註冊表是否已凍結
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * 取得預設值
     */
    public T getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * 設定預設值
     */
    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    /**
     * 取得註冊順序的鍵列表
     */
    public List<String> getKeysInOrder() {
        return Collections.unmodifiableList(order);
    }
    
    /**
     * 根據索引取得鍵
     */
    public String getKey(int index) {
        if (index < 0 || index >= order.size()) {
            return null;
        }
        return order.get(index);
    }
    
    /**
     * 根據索引取得值
     */
    public T getValue(int index) {
        String key = getKey(index);
        if (key == null) {
            return null;
        }
        return getValue(key);
    }
    
    /**
     * 清空註冊表（僅用於測試）
     */
    void clear() {
        entries.clear();
        reverseEntries.clear();
        order.clear();
        frozen = false;
    }
    
    @Override
    public String toString() {
        return "ForgeRegistry[" + name + ", size=" + size() + "]";
    }
}
