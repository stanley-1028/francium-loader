package com.francium.forge.entity;

import java.util.UUID;

/**
 * 屬性修飾符
 * 
 * 對應 Forge/Minecraft 的 AttributeModifier
 * 用於修改屬性值的修飾符（如裝備、藥水效果等）
 * 
 * 支援三種運算方式：
 * - ADDITION：加法（直接加上值）
 * - MULTIPLY_BASE：乘法（乘以基礎值）
 * - MULTIPLY_TOTAL：乘法（乘以總值）
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class AttributeModifier {
    
    /** 修飾符 ID */
    private final UUID id;
    
    /** 修飾符名稱 */
    private final String name;
    
    /** 修飾值 */
    private final double amount;
    
    /** 運算方式 */
    private final Operation operation;
    
    /**
     * 運算方式列舉
     */
    public enum Operation {
        /** 加法：直接加上值 */
        ADDITION(0),
        /** 乘法：乘以基礎值 */
        MULTIPLY_BASE(1),
        /** 乘法：乘以總值 */
        MULTIPLY_TOTAL(2);
        
        private final int id;
        
        Operation(int id) {
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
        
        public static Operation fromId(int id) {
            for (Operation op : values()) {
                if (op.id == id) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Invalid operation ID: " + id);
        }
    }
    
    /**
     * 建立屬性修飾符
     * 
     * @param id 修飾符 ID
     * @param name 修飾符名稱
     * @param amount 修飾值
     * @param operation 運算方式
     */
    public AttributeModifier(UUID id, String name, double amount, Operation operation) {
        if (id == null) {
            throw new IllegalArgumentException("Modifier ID cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Modifier name cannot be null or empty");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.operation = operation;
    }
    
    /**
     * 建立屬性修飾符（自動產生 UUID）
     * 
     * @param name 修飾符名稱
     * @param amount 修飾值
     * @param operation 運算方式
     */
    public AttributeModifier(String name, double amount, Operation operation) {
        this(UUID.randomUUID(), name, amount, operation);
    }
    
    /**
     * 取得修飾符 ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * 取得修飾符名稱
     */
    public String getName() {
        return name;
    }
    
    /**
     * 取得修飾值
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * 取得運算方式
     */
    public Operation getOperation() {
        return operation;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AttributeModifier)) return false;
        AttributeModifier other = (AttributeModifier) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return name + " (" + operation + ": " + amount + ")";
    }
}
