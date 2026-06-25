package com.francium.forge.event.player;

import com.francium.forge.event.FMLEvent;

/**
 * 玩家事件基底類別
 * 
 * 對應 Forge 的 PlayerEvent
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class PlayerEvent extends FMLEvent {
    
    /** 玩家名稱 */
    private final String playerName;
    
    /** 玩家 UUID（字串形式） */
    private final String playerUUID;
    
    /**
     * 建立玩家事件
     * 
     * @param playerName 玩家名稱
     * @param playerUUID 玩家 UUID
     */
    public PlayerEvent(String playerName, String playerUUID) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
    }
    
    /**
     * 取得玩家名稱
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * 取得玩家 UUID
     */
    public String getPlayerUUID() {
        return playerUUID;
    }
    
    // ===== 子事件 =====
    
    /**
     * 玩家登入事件
     */
    public static class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(String playerName, String playerUUID) {
            super(playerName, playerUUID);
        }
    }
    
    /**
     * 玩家登出事件
     */
    public static class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(String playerName, String playerUUID) {
            super(playerName, playerUUID);
        }
    }
    
    /**
     * 玩家重生事件
     */
    public static class PlayerRespawnEvent extends PlayerEvent {
        private final boolean endConquered;
        
        public PlayerRespawnEvent(String playerName, String playerUUID, boolean endConquered) {
            super(playerName, playerUUID);
            this.endConquered = endConquered;
        }
        
        public boolean isEndConquered() {
            return endConquered;
        }
    }
    
    /**
     * 玩家改變維度事件
     */
    public static class PlayerChangedDimensionEvent extends PlayerEvent {
        private final String fromDim;
        private final String toDim;
        
        public PlayerChangedDimensionEvent(String playerName, String playerUUID, 
                                           String fromDim, String toDim) {
            super(playerName, playerUUID);
            this.fromDim = fromDim;
            this.toDim = toDim;
        }
        
        public String getFromDim() {
            return fromDim;
        }
        
        public String getToDim() {
            return toDim;
        }
    }
    
    /**
     * 玩家拾取物品事件
     */
    public static class ItemPickupEvent extends PlayerEvent {
        private final String itemId;
        private final int count;
        
        public ItemPickupEvent(String playerName, String playerUUID, 
                               String itemId, int count) {
            super(playerName, playerUUID);
            this.itemId = itemId;
            this.count = count;
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
     * 玩家製作物品事件
     */
    public static class PlayerCraftedEvent extends PlayerEvent {
        private final String itemId;
        private final int count;
        
        public PlayerCraftedEvent(String playerName, String playerUUID,
                                  String itemId, int count) {
            super(playerName, playerUUID);
            this.itemId = itemId;
            this.count = count;
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
     * 玩家經驗值變化事件
     */
    public static class PlayerXpEvent extends PlayerEvent {
        private final int oldXp;
        private final int newXp;
        
        public PlayerXpEvent(String playerName, String playerUUID,
                             int oldXp, int newXp) {
            super(playerName, playerUUID);
            this.oldXp = oldXp;
            this.newXp = newXp;
        }
        
        public int getOldXp() {
            return oldXp;
        }
        
        public int getNewXp() {
            return newXp;
        }
        
        public int getXpChange() {
            return newXp - oldXp;
        }
    }
}
