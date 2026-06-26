package com.francium.forge.block.entity;

import com.francium.forge.block.BlockState;

/**
 * 方塊實體基底類別
 * 
 * 對應 Forge/Minecraft 的 BlockEntity
 * 表示具有特殊功能的方塊（如箱子、熔爐、附魔台等）
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class BlockEntity {
    
    /** 方塊實體類型 */
    private final BlockEntityType<?> type;
    
    /** 方塊狀態 */
    private BlockState blockState;
    
    /** 世界位置 */
    private int x;
    private int y;
    private int z;
    
    /** 維度 */
    private String dimension;
    
    /** 是否移除 */
    private boolean removed;
    
    /**
     * 建立方塊實體
     * 
     * @param type 方塊實體類型
     * @param blockState 方塊狀態
     * @param x X 座標
     * @param y Y 座標
     * @param z Z 座標
     */
    public BlockEntity(BlockEntityType<?> type, BlockState blockState, int x, int y, int z) {
        if (type == null) {
            throw new IllegalArgumentException("BlockEntityType cannot be null");
        }
        if (blockState == null) {
            throw new IllegalArgumentException("BlockState cannot be null");
        }
        
        this.type = type;
        this.blockState = blockState;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = "minecraft:overworld";
        this.removed = false;
    }
    
    /**
     * 取得方塊實體類型
     */
    public BlockEntityType<?> getType() {
        return type;
    }
    
    /**
     * 取得方塊狀態
     */
    public BlockState getBlockState() {
        return blockState;
    }
    
    /**
     * 設定方塊狀態
     * 
     * @param blockState 方塊狀態
     */
    public void setBlockState(BlockState blockState) {
        if (blockState == null) {
            throw new IllegalArgumentException("BlockState cannot be null");
        }
        this.blockState = blockState;
    }
    
    /**
     * 取得 X 座標
     */
    public int getX() {
        return x;
    }
    
    /**
     * 取得 Y 座標
     */
    public int getY() {
        return y;
    }
    
    /**
     * 取得 Z 座標
     */
    public int getZ() {
        return z;
    }
    
    /**
     * 設定位置
     * 
     * @param x X 座標
     * @param y Y 座標
     * @param z Z 座標
     */
    public void setPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * 取得維度
     */
    public String getDimension() {
        return dimension;
    }
    
    /**
     * 設定維度
     * 
     * @param dimension 維度
     */
    public void setDimension(String dimension) {
        if (dimension == null || dimension.isEmpty()) {
            throw new IllegalArgumentException("Dimension cannot be null or empty");
        }
        this.dimension = dimension;
    }
    
    /**
     * 檢查是否已移除
     */
    public boolean isRemoved() {
        return removed;
    }
    
    /**
     * 標記為已移除
     */
    public void setRemoved() {
        this.removed = true;
    }
    
    /**
     * 取消移除
     */
    public void clearRemoved() {
        this.removed = false;
    }
    
    /**
     * 每 tick 呼叫
     * 
     * 子類別可以覆寫此方法來實現每 tick 的邏輯
     */
    public void tick() {
        // 預設實現：什麼都不做
    }
    
    /**
     * 檢查是否有變更
     * 
     * 子類別可以覆寫此方法來標記需要保存的變更
     */
    public void setChanged() {
        // 預設實現：什麼都不做
    }
    
    /**
     * 檢查是否有效
     */
    public boolean isValid() {
        return !removed;
    }
    
    @Override
    public String toString() {
        return type.getId() + " at (" + x + ", " + y + ", " + z + ") in " + dimension;
    }
}
