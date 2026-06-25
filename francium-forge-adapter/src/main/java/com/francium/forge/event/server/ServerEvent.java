package com.francium.forge.event.server;

import com.francium.forge.event.FMLEvent;

/**
 * 伺服器事件基底類別
 * 
 * 對應 Forge 的 ServerLifecycleEvent / ServerTickEvent
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ServerEvent extends FMLEvent {
    
    /** 伺服器 ID */
    private final String serverId;
    
    /**
     * 建立伺服器事件
     */
    public ServerEvent(String serverId) {
        this.serverId = serverId;
    }
    
    /**
     * 取得伺服器 ID
     */
    public String getServerId() {
        return serverId;
    }
    
    // ===== 子事件 =====
    
    /**
     * 伺服器啟動中事件
     */
    public static class ServerStartingEvent extends ServerEvent {
        public ServerStartingEvent(String serverId) {
            super(serverId);
        }
    }
    
    /**
     * 伺服器啟動完成事件
     */
    public static class ServerStartedEvent extends ServerEvent {
        public ServerStartedEvent(String serverId) {
            super(serverId);
        }
    }
    
    /**
     * 伺服器關閉中事件
     */
    public static class ServerStoppingEvent extends ServerEvent {
        public ServerStoppingEvent(String serverId) {
            super(serverId);
        }
    }
    
    /**
     * 伺服器已關閉事件
     */
    public static class ServerStoppedEvent extends ServerEvent {
        public ServerStoppedEvent(String serverId) {
            super(serverId);
        }
    }
    
    /**
     * 伺服器 Tick 事件
     */
    public static class ServerTickEvent extends ServerEvent {
        private final long tickCount;
        private final Phase phase;
        
        public ServerTickEvent(String serverId, long tickCount, Phase phase) {
            super(serverId);
            this.tickCount = tickCount;
            this.phase = phase;
        }
        
        public long getTickCount() {
            return tickCount;
        }
        
        public Phase getPhase() {
            return phase;
        }
        
        /** Tick 階段 */
        public enum Phase {
            START,
            END
        }
    }
    
    /**
     * 玩家加入伺服器事件
     */
    public static class PlayerJoinEvent extends ServerEvent {
        private final String playerName;
        private final String playerUUID;
        
        public PlayerJoinEvent(String serverId, String playerName, String playerUUID) {
            super(serverId);
            this.playerName = playerName;
            this.playerUUID = playerUUID;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public String getPlayerUUID() {
            return playerUUID;
        }
    }
    
    /**
     * 玩家離開伺服器事件
     */
    public static class PlayerLeaveEvent extends ServerEvent {
        private final String playerName;
        private final String playerUUID;
        
        public PlayerLeaveEvent(String serverId, String playerName, String playerUUID) {
            super(serverId);
            this.playerName = playerName;
            this.playerUUID = playerUUID;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public String getPlayerUUID() {
            return playerUUID;
        }
    }
    
    /**
     * 指令執行事件
     * 可取消：如果取消，指令不會被執行
     */
    public static class CommandEvent extends ServerEvent {
        private final String command;
        private final String senderName;
        private final String senderType;
        
        public CommandEvent(String serverId, String command, 
                            String senderName, String senderType) {
            super(serverId);
            this.command = command;
            this.senderName = senderName;
            this.senderType = senderType;
        }
        
        public String getCommand() {
            return command;
        }
        
        public String getSenderName() {
            return senderName;
        }
        
        public String getSenderType() {
            return senderType;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
}
