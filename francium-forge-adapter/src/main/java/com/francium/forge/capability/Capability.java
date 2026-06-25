package com.francium.forge.capability;

/**
 * 能力類別
 * 
 * 對應 Forge 的 Capability
 * 表示一種能力的定義和預設實作
 * 
 * @param <T> 能力介面類型
 * @author Francium Team
 * @since 2.5.0
 */
public class Capability<T> {
    
    /** 能力名稱 */
    private final String name;
    
    /** 能力介面類別 */
    private final Class<T> type;
    
    /** 預設實作工廠 */
    private final ICapabilityFactory<T> defaultFactory;
    
    /**
     * 建立能力
     * 
     * @param name 能力名稱
     * @param type 能力介面類別
     * @param defaultFactory 預設實作工廠
     */
    public Capability(String name, Class<T> type, ICapabilityFactory<T> defaultFactory) {
        this.name = name;
        this.type = type;
        this.defaultFactory = defaultFactory;
    }
    
    /**
     * 取得能力名稱
     */
    public String getName() {
        return name;
    }
    
    /**
     * 取得能力介面類別
     */
    public Class<T> getType() {
        return type;
    }
    
    /**
     * 取得預設實作
     */
    public T getDefaultInstance() {
        return defaultFactory.create();
    }
    
    /**
     * 能力工廠介面
     */
    public interface ICapabilityFactory<T> {
        T create();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Capability<?> that = (Capability<?>) obj;
        return name.equals(that.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return "Capability{" + name + "}";
    }
}
