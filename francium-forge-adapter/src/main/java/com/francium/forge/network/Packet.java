package com.francium.forge.network;

/**
 * 封包基底類別
 * 
 * 對應 Forge/Minecraft 的 Packet
 * 表示一個網路封包
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public abstract class Packet<T extends PacketListener> {
    
    /** 封包 ID */
    private final int id;
    
    /** 封包名稱 */
    private final String name;
    
    /**
     * 建立封包
     * 
     * @param id 封包 ID
     * @param name 封包名稱
     */
    protected Packet(int id, String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Packet name cannot be null or empty");
        }
        this.id = id;
        this.name = name;
    }
    
    /**
     * 取得封包 ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * 取得封包名稱
     */
    public String getName() {
        return name;
    }
    
    /**
     * 處理封包
     * 
     * @param listener 封包監聽器
     */
    public abstract void handle(T listener);
    
    /**
     * 檢查是否為伺服器端封包
     */
    public boolean isServerbound() {
        return false;
    }
    
    /**
     * 檢查是否為用戶端封包
     */
    public boolean isClientbound() {
        return false;
    }
    
    @Override
    public String toString() {
        return name + " (id=" + id + ")";
    }
}
