package com.francium.forge.event.item;

import com.francium.forge.event.FMLEvent;

/**
 * 物品事件基底類別
 * 
 * 對應 Forge 的 ItemEvent
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ItemEvent extends FMLEvent {
    
    /** 物品 ID */
    private final String itemId;
    
    /** 物品數量 */
    private final int count;
    
    /**
     * 建立物品事件
     */
    public ItemEvent(String itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }
    
    /**
     * 取得物品 ID
     */
    public String getItemId() {
        return itemId;
    }
    
    /**
     * 取得物品數量
     */
    public int getCount() {
        return count;
    }
    
    // ===== 子事件 =====
    
    /**
     * 物品被製作事件
     */
    public static class CraftedEvent extends ItemEvent {
        private final String crafterName;
        
        public CraftedEvent(String itemId, int count, String crafterName) {
            super(itemId, count);
            this.crafterName = crafterName;
        }
        
        public String getCrafterName() {
            return crafterName;
        }
    }
    
    /**
     * 物品被熔煉事件
     */
    public static class SmeltedEvent extends ItemEvent {
        private final String smelterName;
        
        public SmeltedEvent(String itemId, int count, String smelterName) {
            super(itemId, count);
            this.smelterName = smelterName;
        }
        
        public String getSmelterName() {
            return smelterName;
        }
    }
    
    /**
     * 物品被拾取事件
     */
    public static class PickupEvent extends ItemEvent {
        private final String pickerName;
        
        public PickupEvent(String itemId, int count, String pickerName) {
            super(itemId, count);
            this.pickerName = pickerName;
        }
        
        public String getPickerName() {
            return pickerName;
        }
    }
    
    /**
     * 物品被丟棄事件
     */
    public static class DroppedByPlayerEvent extends ItemEvent {
        private final String playerName;
        
        public DroppedByPlayerEvent(String itemId, int count, String playerName) {
            super(itemId, count);
            this.playerName = playerName;
        }
        
        public String getPlayerName() {
            return playerName;
        }
    }
    
    /**
     * 物品損壞事件
     * 可取消：如果取消，物品不會損壞
     */
    public static class ItemDamageEvent extends ItemEvent {
        private final int originalDamage;
        private final int newDamage;
        private final String ownerName;
        
        public ItemDamageEvent(String itemId, int count, int originalDamage, 
                               int newDamage, String ownerName) {
            super(itemId, count);
            this.originalDamage = originalDamage;
            this.newDamage = newDamage;
            this.ownerName = ownerName;
        }
        
        public int getOriginalDamage() {
            return originalDamage;
        }
        
        public int getNewDamage() {
            return newDamage;
        }
        
        public String getOwnerName() {
            return ownerName;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
    
    /**
     * 物品附魔事件
     */
    public static class EnchantEvent extends ItemEvent {
        private final String enchanterName;
        private final int enchantmentLevel;
        private final String enchantmentId;
        
        public EnchantEvent(String itemId, int count, String enchanterName,
                            int enchantmentLevel, String enchantmentId) {
            super(itemId, count);
            this.enchanterName = enchanterName;
            this.enchantmentLevel = enchantmentLevel;
            this.enchantmentId = enchantmentId;
        }
        
        public String getEnchanterName() {
            return enchanterName;
        }
        
        public int getEnchantmentLevel() {
            return enchantmentLevel;
        }
        
        public String getEnchantmentId() {
            return enchantmentId;
        }
    }
}
