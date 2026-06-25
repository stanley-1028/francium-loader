package com.francium.forge.fluid;

/**
 * 流體堆疊
 * 
 * 對應 Forge 的 FluidStack
 * 表示一定量的流體
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class FluidStack {
    
    /** 流體 ID */
    private final String fluidId;
    
    /** 流體數量（單位：mB，毫桶） */
    private int amount;
    
    /**
     * 建立流體堆疊
     * 
     * @param fluidId 流體 ID
     * @param amount 數量（mB，必須 >= 0）
     * @throws IllegalArgumentException 如果 fluidId 為 null/空或 amount 為負數
     */
    public FluidStack(String fluidId, int amount) {
        if (fluidId == null || fluidId.isEmpty()) {
            throw new IllegalArgumentException("Fluid ID cannot be null or empty");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Fluid amount cannot be negative: " + amount);
        }
        this.fluidId = fluidId;
        this.amount = amount;
    }
    
    /**
     * 建立流體堆疊（複製）
     * 
     * @param other 要複製的流體堆疊
     * @throws IllegalArgumentException 如果 other 為 null
     */
    public FluidStack(FluidStack other) {
        if (other == null) {
            throw new IllegalArgumentException("FluidStack to copy cannot be null");
        }
        this.fluidId = other.fluidId;
        this.amount = other.amount;
    }
    
    /**
     * 取得流體 ID
     */
    public String getFluidId() {
        return fluidId;
    }
    
    /**
     * 取得數量
     */
    public int getAmount() {
        return amount;
    }
    
    /**
     * 設定數量
     * 
     * @param amount 數量（必須 >= 0）
     * @throws IllegalArgumentException 如果 amount 為負數
     */
    public void setAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Fluid amount cannot be negative: " + amount);
        }
        this.amount = amount;
    }
    
    /**
     * 增加數量
     * 
     * @param amount 要增加的數量
     * @throws IllegalArgumentException 如果 amount 為負數
     */
    public void grow(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot grow by negative amount: " + amount);
        }
        this.amount += amount;
    }
    
    /**
     * 減少數量
     * 
     * @param amount 要減少的數量
     * @throws IllegalArgumentException 如果 amount 為負數
     */
    public void shrink(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot shrink by negative amount: " + amount);
        }
        this.amount = Math.max(0, this.amount - amount);
    }
    
    /**
     * 檢查是否為空
     */
    public boolean isEmpty() {
        return amount <= 0;
    }
    
    /**
     * 檢查是否為相同流體
     */
    public boolean isFluidEqual(FluidStack other) {
        return other != null && fluidId.equals(other.fluidId);
    }
    
    /**
     * 複製
     */
    public FluidStack copy() {
        return new FluidStack(this);
    }
    
    /**
     * 建立空的流體堆疊
     */
    public static FluidStack empty() {
        return new FluidStack("empty", 0);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FluidStack that = (FluidStack) obj;
        return amount == that.amount && fluidId.equals(that.fluidId);
    }
    
    @Override
    public int hashCode() {
        int result = fluidId.hashCode();
        result = 31 * result + amount;
        return result;
    }
    
    @Override
    public String toString() {
        return "FluidStack{" + fluidId + ", " + amount + "mB}";
    }
}
