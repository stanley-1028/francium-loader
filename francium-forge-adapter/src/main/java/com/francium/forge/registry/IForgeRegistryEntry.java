package com.francium.forge.registry;

/**
 * Forge 註冊項目介面
 * 
 * 對應 Forge 的 IForgeRegistryEntry
 * 所有可註冊的物件都應該實作這個介面
 * 
 * @author Francium Team
 * @since 2.5.0
 * @param <V> 註冊項目的類型
 */
public interface IForgeRegistryEntry<V> {
    
    /**
     * 設定註冊名稱
     * 
     * @param namespace 命名空間（通常是模組 ID）
     * @param path 路徑
     * @return 這個物件本身，用於鏈式呼叫
     */
    V setRegistryName(String namespace, String path);
    
    /**
     * 設定註冊名稱
     * 
     * @param name 註冊名稱（格式：namespace:path）
     * @return 這個物件本身，用於鏈式呼叫
     */
    V setRegistryName(String name);
    
    /**
     * 取得註冊名稱
     * 
     * @return 註冊名稱，如果尚未設定則傳回 null
     */
    String getRegistryName();
    
    /**
     * 取得註冊表類型
     * 
     * @return 註冊表類別
     */
    Class<V> getRegistryType();
    
    /**
     * 簡單的實作基底類別
     * 
     * @param <V> 註冊項目的類型
     */
    abstract class Impl<V extends IForgeRegistryEntry<V>> implements IForgeRegistryEntry<V> {
        
        /** 註冊名稱 */
        private String registryName;
        
        @Override
        @SuppressWarnings("unchecked")
        public V setRegistryName(String namespace, String path) {
            this.registryName = namespace + ":" + path;
            return (V) this;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public V setRegistryName(String name) {
            this.registryName = name;
            return (V) this;
        }
        
        @Override
        public String getRegistryName() {
            return registryName;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public Class<V> getRegistryType() {
            return (Class<V>) getClass();
        }
    }
}
