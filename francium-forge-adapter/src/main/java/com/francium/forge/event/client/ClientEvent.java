package com.francium.forge.event.client;

import com.francium.forge.event.FMLEvent;

/**
 * 用戶端事件基底類別
 * 
 * 對應 Forge 的 ClientTickEvent / RenderGameOverlayEvent 等
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ClientEvent extends FMLEvent {
    
    /**
     * 建立用戶端事件
     */
    public ClientEvent() {
    }
    
    // ===== 子事件 =====
    
    /**
     * 用戶端 Tick 事件
     */
    public static class ClientTickEvent extends ClientEvent {
        private final Phase phase;
        private final long tickCount;
        
        public ClientTickEvent(Phase phase, long tickCount) {
            super();
            this.phase = phase;
            this.tickCount = tickCount;
        }
        
        public Phase getPhase() {
            return phase;
        }
        
        public long getTickCount() {
            return tickCount;
        }
        
        /** Tick 階段 */
        public enum Phase {
            START,
            END
        }
    }
    
    /**
     * 畫面渲染事件
     */
    public static class RenderGameOverlayEvent extends ClientEvent {
        private final float partialTicks;
        private final ElementType elementType;
        
        public RenderGameOverlayEvent(float partialTicks, ElementType elementType) {
            super();
            this.partialTicks = partialTicks;
            this.elementType = elementType;
        }
        
        public float getPartialTicks() {
            return partialTicks;
        }
        
        public ElementType getElementType() {
            return elementType;
        }
        
        /** 覆蓋元素類型 */
        public enum ElementType {
            ALL,
            HELMET,
            PORTAL,
            CROSSHAIRS,
            BOSSHEALTH,
            BOSSINFO,
            TEXT,
            HOTBAR,
            EXPERIENCE,
            JUMPBAR,
            HEALTH,
            ARMOR,
            FOOD,
            HEALTHMOUNT,
            AIR,
            CHAT,
            PLAYER_LIST,
            DEBUG,
            POTION_ICONS,
            SUBTITLES,
            FPS_GRAPH
        }
    }
    
    /**
     * 畫面開啟事件
     */
    public static class ScreenOpenEvent extends ClientEvent {
        private final String screenName;
        
        public ScreenOpenEvent(String screenName) {
            super();
            this.screenName = screenName;
        }
        
        public String getScreenName() {
            return screenName;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
    
    /**
     * 輸入事件（按鍵）
     */
    public static class KeyInputEvent extends ClientEvent {
        private final int keyCode;
        private final boolean pressed;
        
        public KeyInputEvent(int keyCode, boolean pressed) {
            super();
            this.keyCode = keyCode;
            this.pressed = pressed;
        }
        
        public int getKeyCode() {
            return keyCode;
        }
        
        public boolean isPressed() {
            return pressed;
        }
    }
    
    /**
     * 滑鼠輸入事件
     */
    public static class MouseInputEvent extends ClientEvent {
        private final int button;
        private final boolean pressed;
        private final double x;
        private final double y;
        
        public MouseInputEvent(int button, boolean pressed, double x, double y) {
            super();
            this.button = button;
            this.pressed = pressed;
            this.x = x;
            this.y = y;
        }
        
        public int getButton() {
            return button;
        }
        
        public boolean isPressed() {
            return pressed;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
    }
    
    /**
     * 世界載入事件（用戶端）
     */
    public static class ClientWorldLoadEvent extends ClientEvent {
        private final String worldName;
        
        public ClientWorldLoadEvent(String worldName) {
            super();
            this.worldName = worldName;
        }
        
        public String getWorldName() {
            return worldName;
        }
    }
    
    /**
     * 玩家登入事件（用戶端）
     */
    public static class ClientPlayerJoinEvent extends ClientEvent {
        private final String playerName;
        
        public ClientPlayerJoinEvent(String playerName) {
            super();
            this.playerName = playerName;
        }
        
        public String getPlayerName() {
            return playerName;
        }
    }
    
    /**
     * 連接伺服器事件
     */
    public static class ClientConnectedToServerEvent extends ClientEvent {
        private final String serverAddress;
        private final int serverPort;
        
        public ClientConnectedToServerEvent(String serverAddress, int serverPort) {
            super();
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }
        
        public String getServerAddress() {
            return serverAddress;
        }
        
        public int getServerPort() {
            return serverPort;
        }
    }
    
    /**
     * 斷開連接事件
     */
    public static class ClientDisconnectionEvent extends ClientEvent {
        private final String reason;
        
        public ClientDisconnectionEvent(String reason) {
            super();
            this.reason = reason;
        }
        
        public String getReason() {
            return reason;
        }
    }
}
