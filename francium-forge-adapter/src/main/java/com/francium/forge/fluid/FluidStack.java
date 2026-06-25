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
     * @param amount 數量（mB）
     */
    public FluidStack(String fluidId, int amount) {
        this.fluidId = fluidId;
        this.amount = amount;
    }
    
    /**
     * 建立流體堆疊（複製）
     */
    public FluidStack(FluidStack other) {
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
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    /**
     * 增加數量
     */
    public void grow(int amount) {
        this.amount += amount;
    }
    
    /**
     * 減少數量
     */
    public void shrink(int amount) {
        this.amount -= amount;
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
