package com.francium.forge.network;

/**
 * 封包監聽器介面
 * 
 * 對應 Forge/Minecraft 的 PacketListener
 * 用於處理網路封包
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public interface PacketListener {
    
    /**
     * 取得連線狀態
     * 
     * @return 連線狀態
     */
    ConnectionState getConnectionState();
    
    /**
     * 檢查連線是否仍在作用
     * 
     * @return 是否在作用
     */
    boolean isConnected();
    
    /**
     * 中斷連線
     * 
     * @param reason 中斷原因
     */
    void disconnect(String reason);
    
    /**
     * 傳送封包
     * 
     * @param packet 要傳送的封包
     */
    void send(Packet<?> packet);
    
    /**
     * 連線狀態列舉
     */
    enum ConnectionState {
        /** 握手階段 */
        HANDSHAKING,
        /** 狀態查詢 */
        STATUS,
        /** 登入階段 */
        LOGIN,
        /** 遊戲中 */
        PLAY
    }
}
