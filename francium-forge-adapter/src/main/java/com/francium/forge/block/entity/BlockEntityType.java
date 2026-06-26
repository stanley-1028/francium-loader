package com.francium.forge.block.entity;

import com.francium.forge.block.BlockState;

import java.util.function.Supplier;

/**
 * 方塊實體類型
 * 
 * 對應 Forge/Minecraft 的 BlockEntityType
 * 表示一種方塊實體的類型定義
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class BlockEntityType<T extends BlockEntity> {
    
    /** 類型 ID */
    private final String id;
    
    /** 方塊實體工廠 */
    private final BlockEntityFactory<T> factory;
    
    /** 有效的方塊狀態 */
    private final java.util.Set<BlockState> validStates;
    
    /**
     * 方塊實體工廠介面
     */
    @FunctionalInterface
    public interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockEntityType<T> type, BlockState state, int x, int y, int z);
    }
    
    /**
     * 建立方塊實體類型
     * 
     * @param id 類型 ID
     * @param factory 方塊實體工廠
     * @param validStates 有效的方塊狀態
     */
    public BlockEntityType(String id, BlockEntityFactory<T> factory, 
                           java.util.Set<BlockState> validStates) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Factory cannot be null");
        }
        
        this.id = id;
        this.factory = factory;
        this.validStates = validStates != null ? 
            java.util.Collections.unmodifiableSet(new java.util.HashSet<>(validStates)) :
            java.util.Collections.emptySet();
    }
    
    /**
     * 建立方塊實體類型（建構器模式）
     * 
     * @param id 類型 ID
     * @param factory 方塊實體工廠
     * @return 建構器
     */
    public static <T extends BlockEntity> Builder<T> builder(String id, BlockEntityFactory<T> factory) {
        return new Builder<>(id, factory);
    }
    
    /**
     * 取得類型 ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 取得工廠
     */
    public BlockEntityFactory<T> getFactory() {
        return factory;
    }
    
    /**
     * 取得有效的方塊狀態
     */
    public java.util.Set<BlockState> getValidStates() {
        return validStates;
    }
    
    /**
     * 檢查指定的方塊狀態是否有效
     * 
     * @param state 方塊狀態
     * @return 是否有效
     */
    public boolean isValidState(BlockState state) {
        if (state == null) {
            return false;
        }
        if (validStates.isEmpty()) {
            return true; // 如果沒有指定有效狀態，則所有狀態都有效
        }
        return validStates.contains(state);
    }
    
    /**
     * 建立方塊實體
     * 
     * @param state 方塊狀態
     * @param x X 座標
     * @param y Y 座標
     * @param z Z 座標
     * @return 方塊實體
     */
    public T create(BlockState state, int x, int y, int z) {
        if (!isValidState(state)) {
            throw new IllegalArgumentException("Invalid block state for this block entity type");
        }
        return factory.create(this, state, x, y, z);
    }
    
    /**
     * 方塊實體類型建構器
     */
    public static class Builder<T extends BlockEntity> {
        private final String id;
        private final BlockEntityFactory<T> factory;
        private final java.util.Set<BlockState> validStates = new java.util.HashSet<>();
        
        private Builder(String id, BlockEntityFactory<T> factory) {
            this.id = id;
            this.factory = factory;
        }
        
        /**
         * 添加有效的方塊狀態
         */
        public Builder<T> addValidState(BlockState state) {
            if (state != null) {
                validStates.add(state);
            }
            return this;
        }
        
        /**
         * 添加多個有效的方塊狀態
         */
        public Builder<T> addValidStates(BlockState... states) {
            if (states != null) {
                for (BlockState state : states) {
                    if (state != null) {
                        validStates.add(state);
                    }
                }
            }
            return this;
        }
        
        /**
         * 建構方塊實體類型
         */
        public BlockEntityType<T> build() {
            return new BlockEntityType<>(id, factory, validStates);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BlockEntityType)) return false;
        BlockEntityType<?> other = (BlockEntityType<?>) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return id;
    }
}
