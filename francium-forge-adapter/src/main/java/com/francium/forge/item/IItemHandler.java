package com.francium.forge.item;

/**
 * 物品處理器介面
 * 
 * 對應 Forge 的 IItemHandler
 * 表示可以儲存和操作物品的容器
 * 
 * 這是 Forge 中最常用的能力之一
 * 用於箱子、機器、背包等物品容器
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public interface IItemHandler {
    
    /**
     * 取得槽位數量
     * 
     * @return 槽位數量
     */
    int getSlots();
    
    /**
     * 取得指定槽位的物品堆疊
     * 
     * @param slot 槽位索引
     * @return 物品堆疊，如果槽位為空則傳回空堆疊
     */
    ItemStack getStackInSlot(int slot);
    
    /**
     * 插入物品到指定槽位
     * 
     * @param slot 槽位索引
     * @param stack 要插入的物品堆疊
     * @param simulate 是否為模擬（不實際修改）
     * @return 剩餘的物品堆疊（未能插入的部分）
     */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
    
    /**
     * 從指定槽位提取物品
     * 
     * @param slot 槽位索引
     * @param amount 要提取的數量
     * @param simulate 是否為模擬（不實際修改）
     * @return 實際提取的物品堆疊
     */
    ItemStack extractItem(int slot, int amount, boolean simulate);
    
    /**
     * 取得指定槽位的最大物品數量
     * 
     * @param slot 槽位索引
     * @return 最大物品數量
     */
    int getSlotLimit(int slot);
    
    /**
     * 檢查物品是否可以放入指定槽位
     * 
     * @param slot 槽位索引
     * @param stack 物品堆疊
     * @return 是否可以放入
     */
    default boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
