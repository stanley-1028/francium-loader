package com.francium.forge.entity;

import java.util.*;

/**
 * 屬性實例
 * 
 * 對應 Forge/Minecraft 的 AttributeInstance
 * 表示一個實體的具體屬性實例，包含基礎值和多個修飾符
 * 
 * 計算方式：
 * 1. 基礎值 + ADDITION 修飾符
 * 2. 乘以 (1 + MULTIPLY_BASE 修飾符之和)
 * 3. 乘以 (1 + MULTIPLY_TOTAL 修飾符之和)
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class AttributeInstance {
    
    /** 屬性定義 */
    private final Attribute attribute;
    
    /** 基礎值 */
    private double baseValue;
    
    /** 修飾符映射：UUID -> 修飾符 */
    private final Map<UUID, AttributeModifier> modifiers = new HashMap<>();
    
    /** 快取的計算值 */
    private volatile double cachedValue;
    
    /** 是否需要重新計算 */
    private volatile boolean dirty = true;
    
    /**
     * 建立屬性實例
     * 
     * @param attribute 屬性定義
     */
    public AttributeInstance(Attribute attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute cannot be null");
        }
        this.attribute = attribute;
        this.baseValue = attribute.getDefaultValue();
    }
    
    /**
     * 取得屬性定義
     */
    public Attribute getAttribute() {
        return attribute;
    }
    
    /**
     * 取得基礎值
     */
    public double getBaseValue() {
        return baseValue;
    }
    
    /**
     * 設定基礎值
     * 
     * @param baseValue 新的基礎值
     */
    public void setBaseValue(double baseValue) {
        this.baseValue = attribute.clampValue(baseValue);
        this.dirty = true;
    }
    
    /**
     * 取得計算後的值（包含所有修飾符）
     * 
     * @return 計算後的值
     */
    public double getValue() {
        if (dirty) {
            cachedValue = calculateValue();
            dirty = false;
        }
        return cachedValue;
    }
    
    /**
     * 計算最終值
     */
    private double calculateValue() {
        double value = baseValue;
        
        // 第一步：加法修飾符
        double addition = 0;
        double multiplyBase = 0;
        double multiplyTotal = 0;
        
        for (AttributeModifier modifier : modifiers.values()) {
            switch (modifier.getOperation()) {
                case ADDITION:
                    addition += modifier.getAmount();
                    break;
                case MULTIPLY_BASE:
                    multiplyBase += modifier.getAmount();
                    break;
                case MULTIPLY_TOTAL:
                    multiplyTotal += modifier.getAmount();
                    break;
            }
        }
        
        // 加法
        value += addition;
        
        // 乘以基礎值
        value *= (1 + multiplyBase);
        
        // 乘以總值
        value *= (1 + multiplyTotal);
        
        // 鉗位到有效範圍
        return attribute.clampValue(value);
    }
    
    /**
     * 添加修飾符
     * 
     * @param modifier 修飾符
     * @return 是否成功添加（如果已存在則返回 false）
     */
    public boolean addModifier(AttributeModifier modifier) {
        if (modifier == null) {
            throw new IllegalArgumentException("Modifier cannot be null");
        }
        if (modifiers.containsKey(modifier.getId())) {
            return false;
        }
        modifiers.put(modifier.getId(), modifier);
        dirty = true;
        return true;
    }
    
    /**
     * 移除修飾符
     * 
     * @param modifierId 修飾符 ID
     * @return 是否成功移除
     */
    public boolean removeModifier(UUID modifierId) {
        if (modifierId == null) {
            throw new IllegalArgumentException("Modifier ID cannot be null");
        }
        if (modifiers.remove(modifierId) != null) {
            dirty = true;
            return true;
        }
        return false;
    }
    
    /**
     * 移除修飾符
     * 
     * @param modifier 修飾符
     * @return 是否成功移除
     */
    public boolean removeModifier(AttributeModifier modifier) {
        if (modifier == null) {
            throw new IllegalArgumentException("Modifier cannot be null");
        }
        return removeModifier(modifier.getId());
    }
    
    /**
     * 檢查是否包含指定修飾符
     * 
     * @param modifierId 修飾符 ID
     * @return 是否包含
     */
    public boolean hasModifier(UUID modifierId) {
        return modifiers.containsKey(modifierId);
    }
    
    /**
     * 取得指定 ID 的修飾符
     * 
     * @param modifierId 修飾符 ID
     * @return 修飾符，如果不存在則返回 null
     */
    public AttributeModifier getModifier(UUID modifierId) {
        return modifiers.get(modifierId);
    }
    
    /**
     * 取得所有修飾符
     */
    public Collection<AttributeModifier> getModifiers() {
        return Collections.unmodifiableCollection(modifiers.values());
    }
    
    /**
     * 取得修飾符數量
     */
    public int getModifierCount() {
        return modifiers.size();
    }
    
    /**
     * 移除所有修飾符
     */
    public void clearModifiers() {
        if (!modifiers.isEmpty()) {
            modifiers.clear();
            dirty = true;
        }
    }
    
    /**
     * 標記為需要重新計算
     */
    public void markDirty() {
        dirty = true;
    }
    
    @Override
    public String toString() {
        return attribute.getId() + ": base=" + baseValue + ", value=" + getValue() + 
               ", modifiers=" + modifiers.size();
    }
}
