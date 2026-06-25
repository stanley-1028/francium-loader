package com.francium.forge.fluid;

/**
 * 流體處理器介面
 * 
 * 對應 Forge 的 IFluidHandler
 * 用於表示可以儲存/傳輸流體的方塊/物品
 * 
 * 流體單位：mB（毫桶），1000 mB = 1 桶
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public interface IFluidHandler {
    
    /**
     * 取得儲存槽數量
     */
    int getTanks();
    
    /**
     * 取得指定儲存槽的流體
     * 
     * @param tank 儲存槽索引
     * @return 流體堆疊，如果槽為空則傳回空的 FluidStack
     */
    FluidStack getFluidInTank(int tank);
    
    /**
     * 取得指定儲存槽的容量
     * 
     * @param tank 儲存槽索引
     * @return 容量（mB）
     */
    int getTankCapacity(int tank);
    
    /**
     * 檢查指定儲存槽是否可以儲存指定流體
     * 
     * @param tank 儲存槽索引
     * @param stack 流體堆疊
     * @return 是否可以儲存
     */
    boolean isFluidValid(int tank, FluidStack stack);
    
    /**
     * 填入流體
     * 
     * @param resource 要填入的流體
     * @param simulate 是否為模擬
     * @return 實際填入的量
     */
    int fill(FluidStack resource, boolean simulate);
    
    /**
     * 排出流體
     * 
     * @param resource 要排出的流體（類型和最大量）
     * @param simulate 是否為模擬
     * @return 實際排出的流體
     */
    FluidStack drain(FluidStack resource, boolean simulate);
    
    /**
     * 排出流體（指定最大量）
     * 
     * @param maxDrain 最大排出量
     * @param simulate 是否為模擬
     * @return 實際排出的流體
     */
    FluidStack drain(int maxDrain, boolean simulate);
}
