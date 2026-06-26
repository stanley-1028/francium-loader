package com.francium.forge.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 網路通道
 * 
 * 對應 Forge 的 NetworkChannel / SimpleChannel
 * 用於模組之間的網路通訊
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class NetworkChannel {
    
    /** 通道名稱 */
    private final String name;
    
    /** 通道版本 */
    private final String version;
    
    /** 註冊的封包類型 */
    private final Map<Integer, PacketType<?>> packetTypes = new ConcurrentHashMap<>();
    
    /** 下一個封包 ID */
    private int nextPacketId = 0;
    
    /** 通道是否已關閉 */
    private volatile boolean closed = false;
    
    /**
     * 建立網路通道
     * 
     * @param name 通道名稱
     * @param version 通道版本
     */
    public NetworkChannel(String name, String version) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty");
        }
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Channel version cannot be null or empty");
        }
        this.name = name;
        this.version = version;
    }
    
    /**
     * 取得通道名稱
     */
    public String getName() {
        return name;
    }
    
    /**
     * 取得通道版本
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 註冊封包類型
     * 
     * @param <T> 封包類型
     * @param packetClass 封包類別
     * @param decoder 解碼器（從位元組陣列建立封包）
     * @param encoder 編碼器（將封包轉換為位元組陣列）
     * @param handler 處理器（處理封包）
     * @return 封包 ID
     */
    public <T extends Packet<?>> int registerPacket(Class<T> packetClass, 
                                                      Supplier<T> decoder,
                                                      Consumer<T> encoder,
                                                      Consumer<T> handler) {
        if (closed) {
            throw new IllegalStateException("Channel is closed");
        }
        if (packetClass == null) {
            throw new IllegalArgumentException("Packet class cannot be null");
        }
        if (decoder == null) {
            throw new IllegalArgumentException("Decoder cannot be null");
        }
        
        int id = nextPacketId++;
        packetTypes.put(id, new PacketType<>(packetClass, decoder, encoder, handler));
        return id;
    }
    
    /**
     * 根據 ID 取得封包類型
     * 
     * @param id 封包 ID
     * @return 封包類型，如果找不到則返回 null
     */
    public PacketType<?> getPacketType(int id) {
        return packetTypes.get(id);
    }
    
    /**
     * 檢查是否包含指定 ID 的封包類型
     * 
     * @param id 封包 ID
     * @return 是否包含
     */
    public boolean hasPacketType(int id) {
        return packetTypes.containsKey(id);
    }
    
    /**
     * 取得註冊的封包數量
     */
    public int getPacketCount() {
        return packetTypes.size();
    }
    
    /**
     * 傳送封包到伺服器
     * 
     * @param packet 要傳送的封包
     */
    public void sendToServer(Packet<?> packet) {
        if (closed) {
            throw new IllegalStateException("Channel is closed");
        }
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }
        // 在實際實現中，這裡會將封包發送到伺服器
        // 目前只是一個簡化實現
    }
    
    /**
     * 傳送封包到用戶端
     * 
     * @param packet 要傳送的封包
     * @param player 目標玩家
     */
    public void sendToClient(Packet<?> packet, Object player) {
        if (closed) {
            throw new IllegalStateException("Channel is closed");
        }
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }
        // 在實際實現中，這裡會將封包發送到指定玩家
        // 目前只是一個簡化實現
    }
    
    /**
     * 廣播封包到所有玩家
     * 
     * @param packet 要廣播的封包
     */
    public void sendToAll(Packet<?> packet) {
        if (closed) {
            throw new IllegalStateException("Channel is closed");
        }
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }
        // 在實際實現中，這裡會將封包廣播到所有玩家
        // 目前只是一個簡化實現
    }
    
    /**
     * 關閉通道
     */
    public void close() {
        closed = true;
        packetTypes.clear();
    }
    
    /**
     * 檢查通道是否已關閉
     */
    public boolean isClosed() {
        return closed;
    }
    
    /**
     * 封包類型內部類別
     */
    public static class PacketType<T extends Packet<?>> {
        private final Class<T> packetClass;
        private final Supplier<T> decoder;
        private final Consumer<T> encoder;
        private final Consumer<T> handler;
        
        public PacketType(Class<T> packetClass, Supplier<T> decoder, 
                          Consumer<T> encoder, Consumer<T> handler) {
            this.packetClass = packetClass;
            this.decoder = decoder;
            this.encoder = encoder;
            this.handler = handler;
        }
        
        public Class<T> getPacketClass() {
            return packetClass;
        }
        
        public Supplier<T> getDecoder() {
            return decoder;
        }
        
        public Consumer<T> getEncoder() {
            return encoder;
        }
        
        public Consumer<T> getHandler() {
            return handler;
        }
    }
    
    @Override
    public String toString() {
        return name + " (v" + version + ", " + packetTypes.size() + " packets)";
    }
}
