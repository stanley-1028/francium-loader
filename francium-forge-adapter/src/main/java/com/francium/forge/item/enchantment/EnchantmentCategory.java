package com.francium.forge.item.enchantment;

/**
 * 附魔類別
 * 
 * 對應 Forge/Minecraft 的 EnchantmentCategory
 * 表示附魔可以應用在哪些物品上
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public enum EnchantmentCategory {
    
    /** 所有武器 */
    WEAPON,
    /** 劍 */
    SWORD,
    /** 弓 */
    BOW,
    /** 十字弩 */
    CROSSBOW,
    /** 鎬 */
    PICKAXE,
    /** 斧 */
    AXE,
    /** 鏟 */
    SHOVEL,
    /** 鋤 */
    HOE,
    /** 釣魚竿 */
    FISHING_ROD,
    /** 三叉戟 */
    TRIDENT,
    /** 頭盔 */
    HELMET,
    /** 胸甲 */
    CHESTPLATE,
    /** 護腿 */
    LEGGINGS,
    /** 靴子 */
    BOOTS,
    /** 所有護甲 */
    ARMOR,
    /** 所有可附魔物品 */
    BREAKABLE,
    /** 可穿戴物品（護甲 + 頭顱等） */
    WEARABLE,
    /** 書本 */
    BOOK,
    /** 其他 */
    VANISHABLE;
    
    /**
     * 檢查此類別是否包含指定類別
     * 
     * @param category 類別
     * @return 是否包含
     */
    public boolean includes(EnchantmentCategory category) {
        if (this == category) {
            return true;
        }
        // ARMOR 包含所有護甲類別
        if (this == ARMOR) {
            return category == HELMET || category == CHESTPLATE || 
                   category == LEGGINGS || category == BOOTS;
        }
        // WEARABLE 包含 ARMOR 和其他可穿戴物品
        if (this == WEARABLE) {
            return category == ARMOR || category == HELMET || category == CHESTPLATE ||
                   category == LEGGINGS || category == BOOTS;
        }
        // BREAKABLE 包含所有可損壞物品
        if (this == BREAKABLE) {
            return category != BOOK && category != VANISHABLE;
        }
        return false;
    }
}
