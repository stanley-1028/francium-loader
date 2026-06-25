package com.francium.forge.registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 註冊表管理器
 * 
 * 管理所有的 Forge 註冊表，類似 Forge 的 RegistryManager
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeRegistryManager {
    
    /** 單例實例 */
    private static final ForgeRegistryManager INSTANCE = new ForgeRegistryManager();
    
    /** 所有註冊表 */
    private final Map<String, ForgeRegistry<?>> registries = new ConcurrentHashMap<>();
    
    /** 是否凍結所有註冊表 */
    private boolean allFrozen = false;
    
    private ForgeRegistryManager() {
        // 私有建構子，單例模式
    }
    
    /**
     * 取得單例實例
     */
    public static ForgeRegistryManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 建立新的註冊表
     * 
     * @param name 註冊表名稱
     * @param type 註冊表類型
     * @param <T> 註冊項目的類型
     * @return 新建立的註冊表
     * @throws IllegalStateException 如果名稱已存在
     */
    public <T> ForgeRegistry<T> createRegistry(String name, Class<T> type) {
        if (registries.containsKey(name)) {
            throw new IllegalStateException("Registry already exists: " + name);
        }
        
        ForgeRegistry<T> registry = new ForgeRegistry<>(name, type);
        registries.put(name, registry);
        return registry;
    }
    
    /**
     * 取得指定名稱的註冊表
     * 
     * @param name 註冊表名稱
     * @param <T> 註冊項目的類型
     * @return 註冊表，如果不存在則傳回 null
     */
    @SuppressWarnings("unchecked")
    public <T> ForgeRegistry<T> getRegistry(String name) {
        return (ForgeRegistry<T>) registries.get(name);
    }
    
    /**
     * 取得或建立註冊表
     * 
     * @param name 註冊表名稱
     * @param type 註冊表類型
     * @param <T> 註冊項目的類型
     * @return 註冊表
     */
    @SuppressWarnings("unchecked")
    public <T> ForgeRegistry<T> getOrCreateRegistry(String name, Class<T> type) {
        ForgeRegistry<T> registry = (ForgeRegistry<T>) registries.get(name);
        if (registry == null) {
            registry = createRegistry(name, type);
        }
        return registry;
    }
    
    /**
     * 檢查註冊表是否存在
     */
    public boolean hasRegistry(String name) {
        return registries.containsKey(name);
    }
    
    /**
     * 取得所有註冊表的名稱
     */
    public Set<String> getRegistryNames() {
        return Collections.unmodifiableSet(registries.keySet());
    }
    
    /**
     * 取得所有註冊表
     */
    public Collection<ForgeRegistry<?>> getRegistries() {
        return Collections.unmodifiableCollection(registries.values());
    }
    
    /**
     * 凍結所有註冊表
     */
    public void freezeAll() {
        allFrozen = true;
        for (ForgeRegistry<?> registry : registries.values()) {
            registry.freeze();
        }
    }
    
    /**
     * 檢查是否所有註冊表都已凍結
     */
    public boolean isAllFrozen() {
        return allFrozen;
    }
    
    /**
     * 取得註冊表數量
     */
    public int getRegistryCount() {
        return registries.size();
    }
    
    /**
     * 取得所有註冊項目的總數
     */
    public int getTotalEntryCount() {
        int total = 0;
        for (ForgeRegistry<?> registry : registries.values()) {
            total += registry.size();
        }
        return total;
    }
    
    /**
     * 清空所有註冊表（僅用於測試）
     */
    void clearAll() {
        registries.clear();
        allFrozen = false;
    }
    
    // ========== 內建的標準註冊表 ==========
    
    /** 方塊註冊表名稱 */
    public static final String BLOCKS = "blocks";
    
    /** 物品註冊表名稱 */
    public static final String ITEMS = "items";
    
    /** 實體類型註冊表名稱 */
    public static final String ENTITY_TYPES = "entity_types";
    
    /** 方塊實體類型註冊表名稱 */
    public static final String BLOCK_ENTITY_TYPES = "block_entity_types";
    
    /** 附魔註冊表名稱 */
    public static final String ENCHANTMENTS = "enchantments";
    
    /** 藥水效果註冊表名稱 */
    public static final String MOB_EFFECTS = "mob_effects";
    
    /** 聲音事件註冊表名稱 */
    public static final String SOUND_EVENTS = "sound_events";
    
    /** 粒子類型註冊表名稱 */
    public static final String PARTICLE_TYPES = "particle_types";
    
    /** 菜單類型註冊表名稱 */
    public static final String MENU_TYPES = "menu_types";
    
    /** 食譜類型註冊表名稱 */
    public static final String RECIPE_TYPES = "recipe_types";
    
    /** 食譜序列化器註冊表名稱 */
    public static final String RECIPE_SERIALIZERS = "recipe_serializers";
    
    /** 維度類型註冊表名稱 */
    public static final String DIMENSION_TYPES = "dimension_types";
    
    /** 生物群系註冊表名稱 */
    public static final String BIOMES = "biomes";
    
    /** 結構特徵註冊表名稱 */
    public static final String STRUCTURE_FEATURES = "structure_features";
    
    /** 統計類型註冊表名稱 */
    public static final String STAT_TYPES = "stat_types";
    
    /** 遊戲規則類型註冊表名稱 */
    public static final String GAME_RULES = "game_rules";
    
    /** 屬性註冊表名稱 */
    public static final String ATTRIBUTES = "attributes";
    
    /** 位置來源註冊表名稱 */
    public static final String POI_TYPES = "poi_types";
    
    /** 村莊職業註冊表名稱 */
    public static final String VILLAGER_PROFESSIONS = "villager_professions";
}
