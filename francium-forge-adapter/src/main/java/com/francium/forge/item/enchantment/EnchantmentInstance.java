package com.francium.forge.item.enchantment;

/**
 * 附魔實例
 * 
 * 對應 Forge/Minecraft 的 EnchantmentInstance
 * 表示一個具體的附魔實例，包含附魔類型和等級
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class EnchantmentInstance {
    
    /** 附魔類型 */
    private final Enchantment enchantment;
    
    /** 附魔等級 */
    private final int level;
    
    /**
     * 建立附魔實例
     * 
     * @param enchantment 附魔類型
     * @param level 附魔等級
     */
    public EnchantmentInstance(Enchantment enchantment, int level) {
        if (enchantment == null) {
            throw new IllegalArgumentException("Enchantment cannot be null");
        }
        if (level < enchantment.getMinLevel() || level > enchantment.getMaxLevel()) {
            throw new IllegalArgumentException(
                "Level " + level + " out of range [" + 
                enchantment.getMinLevel() + ", " + enchantment.getMaxLevel() + 
                "] for enchantment " + enchantment.getId()
            );
        }
        
        this.enchantment = enchantment;
        this.level = level;
    }
    
    /**
     * 取得附魔類型
     */
    public Enchantment getEnchantment() {
        return enchantment;
    }
    
    /**
     * 取得附魔等級
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * 取得附魔 ID
     */
    public String getEnchantmentId() {
        return enchantment.getId();
    }
    
    /**
     * 取得附魔名稱
     */
    public String getEnchantmentName() {
        return enchantment.getName();
    }
    
    /**
     * 檢查是否與另一個附魔實例相容
     * 
     * @param other 另一個附魔實例
     * @return 是否相容
     */
    public boolean isCompatibleWith(EnchantmentInstance other) {
        if (other == null) {
            return false;
        }
        return enchantment.isCompatibleWith(other.enchantment);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EnchantmentInstance)) return false;
        EnchantmentInstance other = (EnchantmentInstance) obj;
        return enchantment.equals(other.enchantment) && level == other.level;
    }
    
    @Override
    public int hashCode() {
        return 31 * enchantment.hashCode() + level;
    }
    
    @Override
    public String toString() {
        return enchantment.getName() + " " + level;
    }
}
