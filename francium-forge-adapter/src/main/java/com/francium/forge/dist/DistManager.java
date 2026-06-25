package com.francium.forge.dist;

/**
 * 側邊管理員
 * 
 * 管理目前執行的側邊（用戶端/伺服器端）
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class DistManager {
    
    /** 目前的側邊 */
    private static Dist currentDist = Dist.CLIENT;
    
    /** 是否已初始化 */
    private static boolean initialized = false;
    
    /**
     * 私有建構子
     */
    private DistManager() {}
    
    /**
     * 初始化側邊管理員
     * 
     * @param dist 目前的側邊
     */
    public static synchronized void initialize(Dist dist) {
        if (initialized) {
            throw new IllegalStateException("DistManager already initialized");
        }
        currentDist = dist;
        initialized = true;
    }
    
    /**
     * 取得目前的側邊
     */
    public static Dist getCurrentDist() {
        return currentDist;
    }
    
    /**
     * 檢查是否為用戶端
     */
    public static boolean isClient() {
        return currentDist.isClient();
    }
    
    /**
     * 檢查是否為伺服器端
     */
    public static boolean isServer() {
        return currentDist.isServer();
    }
    
    /**
     * 檢查是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 重設（僅用於測試）
     */
    static void reset() {
        currentDist = Dist.CLIENT;
        initialized = false;
    }
}
