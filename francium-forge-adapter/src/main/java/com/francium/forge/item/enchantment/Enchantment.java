package com.francium.forge.item.enchantment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 附魔基底類別
 * 
 * 對應 Forge/Minecraft 的 Enchantment
 * 表示一種附魔類型
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class Enchantment {
    
    /** 附魔 ID */
    private final String id;
    
    /** 附魔名稱 */
    private final String name;
    
    /** 附魔描述 */
    private final String description;
    
    /** 稀有度 */
    private final Rarity rarity;
    
    /** 可以應用的物品類別 */
    private final Set<EnchantmentCategory> categories;
    
    /** 最小等級 */
    private final int minLevel;
    
    /** 最大等級 */
    private final int maxLevel;
    
    /** 是否為詛咒附魔 */
    private final boolean curse;
    
    /** 是否為寶藏附魔 */
    private final boolean treasure;
    
    /** 是否可以交易獲得 */
    private final boolean tradeable;
    
    /** 是否可以在附魔台獲得 */
    private final boolean discoverable;
    
    /**
     * 稀有度列舉
     */
    public enum Rarity {
        /** 普通 */
        COMMON(10),
        /** 罕見 */
        UNCOMMON(5),
        /** 稀有 */
        RARE(2),
        /** 史詩 */
        VERY_RARE(1);
        
        private final int weight;
        
        Rarity(int weight) {
            this.weight = weight;
        }
        
        public int getWeight() {
            return weight;
        }
    }
    
    /**
     * 建立附魔
     * 
     * @param id 附魔 ID
     * @param name 附魔名稱
     * @param rarity 稀有度
     * @param category 物品類別
     * @param maxLevel 最大等級
     */
    public Enchantment(String id, String name, Rarity rarity, 
                       EnchantmentCategory category, int maxLevel) {
        this(id, name, "", rarity, Collections.singleton(category), 1, maxLevel, false, false, true, true);
    }
    
    /**
     * 建立附魔
     * 
     * @param id 附魔 ID
     * @param name 附魔名稱
     * @param description 附魔描述
     * @param rarity 稀有度
     * @param categories 物品類別
     * @param minLevel 最小等級
     * @param maxLevel 最大等級
     * @param curse 是否為詛咒附魔
     * @param treasure 是否為寶藏附魔
     * @param tradeable 是否可以交易獲得
     * @param discoverable 是否可以在附魔台獲得
     */
    public Enchantment(String id, String name, String description, Rarity rarity,
                       Set<EnchantmentCategory> categories, int minLevel, int maxLevel,
                       boolean curse, boolean treasure, boolean tradeable, boolean discoverable) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Enchantment ID cannot be null or empty");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Enchantment name cannot be null or empty");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("Rarity cannot be null");
        }
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("Categories cannot be null or empty");
        }
        if (minLevel < 1) {
            throw new IllegalArgumentException("Min level must be at least 1");
        }
        if (maxLevel < minLevel) {
            throw new IllegalArgumentException("Max level cannot be less than min level");
        }
        
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.rarity = rarity;
        this.categories = Collections.unmodifiableSet(new HashSet<>(categories));
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.curse = curse;
        this.treasure = treasure;
        this.tradeable = tradeable;
        this.discoverable = discoverable;
    }
    
    /**
     * 取得附魔 ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 取得附魔名稱
     */
    public String getName() {
        return name;
    }
    
    /**
     * 取得附魔描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 取得稀有度
     */
    public Rarity getRarity() {
        return rarity;
    }
    
    /**
     * 取得可以應用的物品類別
     */
    public Set<EnchantmentCategory> getCategories() {
        return categories;
    }
    
    /**
     * 檢查是否可以應用在指定類別的物品上
     * 
     * @param category 物品類別
     * @return 是否可以應用
     */
    public boolean canApplyTo(EnchantmentCategory category) {
        if (categories.contains(category)) {
            return true;
        }
        // 檢查是否有包含此類別的大類別
        for (EnchantmentCategory cat : categories) {
            if (cat.includes(category)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 取得最小等級
     */
    public int getMinLevel() {
        return minLevel;
    }
    
    /**
     * 取得最大等級
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * 檢查是否為詛咒附魔
     */
    public boolean isCurse() {
        return curse;
    }
    
    /**
     * 檢查是否為寶藏附魔
     */
    public boolean isTreasure() {
        return treasure;
    }
    
    /**
     * 檢查是否可以交易獲得
     */
    public boolean isTradeable() {
        return tradeable;
    }
    
    /**
     * 檢查是否可以在附魔台獲得
     */
    public boolean isDiscoverable() {
        return discoverable;
    }
    
    /**
     * 檢查此附魔是否與另一個附魔相容
     * 
     * @param other 另一個附魔
     * @return 是否相容
     */
    public boolean isCompatibleWith(Enchantment other) {
        if (other == null) {
            return false;
        }
        // 預設：不同的附魔都相容
        return !this.equals(other);
    }
    
    /**
     * 取得指定等級的最小經驗需求
     * 
     * @param level 等級
     * @return 最小經驗需求
     */
    public int getMinCost(int level) {
        if (level < minLevel || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level " + level + " out of range [" + minLevel + ", " + maxLevel + "]"
            );
        }
        // 預設計算方式
        return 1 + level * 10;
    }
    
    /**
     * 取得指定等級的最大經驗需求
     * 
     * @param level 等級
     * @return 最大經驗需求
     */
    public int getMaxCost(int level) {
        return getMinCost(level) + 5;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Enchantment)) return false;
        Enchantment other = (Enchantment) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return id + " (" + name + ", max level: " + maxLevel + ")";
    }
}
