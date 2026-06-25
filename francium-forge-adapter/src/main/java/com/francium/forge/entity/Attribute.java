package com.francium.forge.entity;

/**
 * 實體屬性
 * 
 * 對應 Forge/Minecraft 的 Attribute
 * 表示實體的一種屬性（如生命值、攻擊力、移動速度等）
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class Attribute {
    
    /** 屬性 ID */
    private final String id;
    
    /** 預設值 */
    private final double defaultValue;
    
    /** 最小值 */
    private final double minValue;
    
    /** 最大值 */
    private final double maxValue;
    
    /** 描述 */
    private final String description;
    
    /**
     * 建立屬性
     * 
     * @param id 屬性 ID
     * @param defaultValue 預設值
     * @param minValue 最小值
     * @param maxValue 最大值
     */
    public Attribute(String id, double defaultValue, double minValue, double maxValue) {
        this(id, defaultValue, minValue, maxValue, "");
    }
    
    /**
     * 建立屬性
     * 
     * @param id 屬性 ID
     * @param defaultValue 預設值
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param description 描述
     */
    public Attribute(String id, double defaultValue, double minValue, double maxValue, String description) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Attribute ID cannot be null or empty");
        }
        if (minValue > maxValue) {
            throw new IllegalArgumentException("Min value cannot be greater than max value");
        }
        if (defaultValue < minValue || defaultValue > maxValue) {
            throw new IllegalArgumentException(
                "Default value " + defaultValue + " out of range [" + minValue + ", " + maxValue + "]"
            );
        }
        
        this.id = id;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.description = description != null ? description : "";
    }
    
    /**
     * 取得屬性 ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 取得預設值
     */
    public double getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * 取得最小值
     */
    public double getMinValue() {
        return minValue;
    }
    
    /**
     * 取得最大值
     */
    public double getMaxValue() {
        return maxValue;
    }
    
    /**
     * 取得描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 將值鉗位在有效範圍內
     * 
     * @param value 原始值
     * @return 鉗位後的值
     */
    public double clampValue(double value) {
        return Math.max(minValue, Math.min(maxValue, value));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Attribute)) return false;
        Attribute other = (Attribute) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return id + " (default: " + defaultValue + ", range: [" + minValue + ", " + maxValue + "])";
    }
}
