package com.francium.forge.block;

import java.util.Collection;

/**
 * 方塊狀態屬性
 * 
 * 對應 Forge/Minecraft 的 Property
 * 表示方塊狀態中的一個屬性（如方向、是否打開等）
 * 
 * @param <T> 屬性值的類型
 * @author Francium Team
 * @since 2.5.0
 */
public abstract class Property<T extends Comparable<T>> {
    
    /** 屬性名稱 */
    private final String name;
    
    /** 屬性值的類型 */
    private final Class<T> valueClass;
    
    /**
     * 建立屬性
     * 
     * @param name 屬性名稱
     * @param valueClass 屬性值的類型
     */
    protected Property(String name, Class<T> valueClass) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Property name cannot be null or empty");
        }
        if (valueClass == null) {
            throw new IllegalArgumentException("Value class cannot be null");
        }
        this.name = name;
        this.valueClass = valueClass;
    }
    
    /**
     * 取得屬性名稱
     */
    public String getName() {
        return name;
    }
    
    /**
     * 取得屬性值的類型
     */
    public Class<T> getValueClass() {
        return valueClass;
    }
    
    /**
     * 取得所有可能的值
     */
    public abstract Collection<T> getPossibleValues();
    
    /**
     * 將值轉換為字串
     * 
     * @param value 值
     * @return 字串表示
     */
    public abstract String getName(T value);
    
    /**
     * 從字串解析值
     * 
     * @param value 字串值
     * @return 解析後的值
     * @throws IllegalArgumentException 如果無法解析
     */
    public abstract T getValue(String value);
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Property)) return false;
        Property<?> other = (Property<?>) obj;
        return name.equals(other.name) && valueClass.equals(other.valueClass);
    }
    
    @Override
    public int hashCode() {
        return 31 * name.hashCode() + valueClass.hashCode();
    }
    
    @Override
    public String toString() {
        return name;
    }
}
