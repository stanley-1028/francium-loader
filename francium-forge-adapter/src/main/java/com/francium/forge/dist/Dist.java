package com.francium.forge.dist;

/**
 * 發行版本（側邊）列舉
 * 
 * 對應 Forge 的 Dist
 * 用於標示程式碼僅在特定側邊執行
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public enum Dist {
    
    /** 用戶端（客戶端） */
    CLIENT,
    
    /** 伺服器端（服務端） */
    SERVER;
    
    /**
     * 檢查是否為用戶端
     */
    public boolean isClient() {
        return this == CLIENT;
    }
    
    /**
     * 檢查是否為伺服器端
     */
    public boolean isServer() {
        return this == SERVER;
    }
    
    /**
     * 取得目前執行的側邊
     */
    public static Dist getCurrent() {
        // 簡單實現：預設為用戶端
        // 實際上應該根據環境判斷
        return DistManager.getCurrentDist();
    }
}
