package com.francium.forge.entity;

import java.util.*;

/**
 * 屬性映射
 * 
 * 對應 Forge/Minecraft 的 AttributeMap
 * 管理一個實體的所有屬性實例
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class AttributeMap {
    
    /** 所有屬性實例：屬性 ID -> 屬性實例 */
    private final Map<String, AttributeInstance> instances = new HashMap<>();
    
    /**
     * 建立屬性映射
     */
    public AttributeMap() {}
    
    /**
     * 取得指定屬性的實例
     * 
     * @param attribute 屬性
     * @return 屬性實例
     * @throws IllegalArgumentException 如果屬性不存在
     */
    public AttributeInstance getInstance(Attribute attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute cannot be null");
        }
        AttributeInstance instance = instances.get(attribute.getId());
        if (instance == null) {
            throw new IllegalArgumentException("Attribute not found: " + attribute.getId());
        }
        return instance;
    }
    
    /**
     * 取得指定屬性的實例（如果不存在則建立）
     * 
     * @param attribute 屬性
     * @return 屬性實例
     */
    public AttributeInstance getOrCreateInstance(Attribute attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute cannot be null");
        }
        return instances.computeIfAbsent(attribute.getId(), id -> new AttributeInstance(attribute));
    }
    
    /**
     * 註冊屬性
     * 
     * @param attribute 屬性
     * @return 屬性實例
     */
    public AttributeInstance registerAttribute(Attribute attribute) {
        return getOrCreateInstance(attribute);
    }
    
    /**
     * 檢查是否包含指定屬性
     * 
     * @param attribute 屬性
     * @return 是否包含
     */
    public boolean hasAttribute(Attribute attribute) {
        if (attribute == null) {
            return false;
        }
        return instances.containsKey(attribute.getId());
    }
    
    /**
     * 檢查是否包含指定屬性
     * 
     * @param attributeId 屬性 ID
     * @return 是否包含
     */
    public boolean hasAttribute(String attributeId) {
        if (attributeId == null) {
            return false;
        }
        return instances.containsKey(attributeId);
    }
    
    /**
     * 取得指定屬性的值
     * 
     * @param attribute 屬性
     * @return 屬性值
     * @throws IllegalArgumentException 如果屬性不存在
     */
    public double getValue(Attribute attribute) {
        return getInstance(attribute).getValue();
    }
    
    /**
     * 設定指定屬性的基礎值
     * 
     * @param attribute 屬性
     * @param baseValue 基礎值
     * @throws IllegalArgumentException 如果屬性不存在
     */
    public void setBaseValue(Attribute attribute, double baseValue) {
        getInstance(attribute).setBaseValue(baseValue);
    }
    
    /**
     * 為指定屬性添加修飾符
     * 
     * @param attribute 屬性
     * @param modifier 修飾符
     * @return 是否成功添加
     */
    public boolean addModifier(Attribute attribute, AttributeModifier modifier) {
        return getOrCreateInstance(attribute).addModifier(modifier);
    }
    
    /**
     * 為指定屬性移除修飾符
     * 
     * @param attribute 屬性
     * @param modifierId 修飾符 ID
     * @return 是否成功移除
     */
    public boolean removeModifier(Attribute attribute, UUID modifierId) {
        if (!hasAttribute(attribute)) {
            return false;
        }
        return getInstance(attribute).removeModifier(modifierId);
    }
    
    /**
     * 取得所有屬性實例
     */
    public Collection<AttributeInstance> getInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }
    
    /**
     * 取得屬性數量
     */
    public int getAttributeCount() {
        return instances.size();
    }
    
    /**
     * 移除所有屬性
     */
    public void clear() {
        instances.clear();
    }
    
    @Override
    public String toString() {
        return "AttributeMap{" + instances.size() + " attributes}";
    }
}
