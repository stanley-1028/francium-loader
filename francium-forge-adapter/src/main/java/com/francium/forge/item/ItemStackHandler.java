package com.francium.forge.item;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品堆疊處理器
 * 
 * 對應 Forge 的 ItemStackHandler
 * 基本的物品容器實作
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ItemStackHandler implements IItemHandler {
    
    /** 物品堆疊列表 */
    protected final List<ItemStack> stacks;
    
    /** 每個槽位的預設最大數量 */
    protected final int defaultSlotLimit;
    
    /**
     * 建立物品處理器
     * 
     * @param size 槽位數量
     * @throws IllegalArgumentException 如果 size <= 0
     */
    public ItemStackHandler(int size) {
        this(size, 64);
    }
    
    /**
     * 建立物品處理器
     * 
     * @param size 槽位數量
     * @param defaultSlotLimit 每個槽位的預設最大數量
     * @throws IllegalArgumentException 如果參數無效
     */
    public ItemStackHandler(int size, int defaultSlotLimit) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive: " + size);
        }
        if (defaultSlotLimit <= 0) {
            throw new IllegalArgumentException("Default slot limit must be positive: " + defaultSlotLimit);
        }
        
        this.defaultSlotLimit = defaultSlotLimit;
        this.stacks = new ArrayList<>(size);
        
        // 初始化所有槽位為空
        for (int i = 0; i < size; i++) {
            stacks.add(ItemStack.EMPTY.copy());
        }
    }
    
    @Override
    public int getSlots() {
        return stacks.size();
    }
    
    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return stacks.get(slot).copy();
    }
    
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty()) {
            return stack;
        }
        
        validateSlotIndex(slot);
        
        if (!isItemValid(slot, stack)) {
            return stack;
        }
        
        ItemStack existing = stacks.get(slot);
        
        // 如果槽位為空
        if (existing.isEmpty()) {
            int limit = getSlotLimit(slot);
            int count = Math.min(stack.getCount(), limit);
            
            if (!simulate) {
                ItemStack newStack = stack.copy();
                newStack.setCount(count);
                stacks.set(slot, newStack);
            }
            
            if (count == stack.getCount()) {
                return ItemStack.EMPTY.copy();
            }
            
            ItemStack remaining = stack.copy();
            remaining.shrink(count);
            return remaining;
        }
        
        // 如果槽位不為空，檢查是否為相同物品
        if (!existing.isItemEqual(stack)) {
            return stack;
        }
        
        int limit = getSlotLimit(slot);
        int available = limit - existing.getCount();
        
        if (available <= 0) {
            return stack;
        }
        
        int count = Math.min(stack.getCount(), available);
        
        if (!simulate) {
            existing.grow(count);
        }
        
        if (count == stack.getCount()) {
            return ItemStack.EMPTY.copy();
        }
        
        ItemStack remaining = stack.copy();
        remaining.shrink(count);
        return remaining;
    }
    
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY.copy();
        }
        
        validateSlotIndex(slot);
        
        ItemStack existing = stacks.get(slot);
        
        if (existing.isEmpty()) {
            return ItemStack.EMPTY.copy();
        }
        
        int count = Math.min(amount, existing.getCount());
        
        if (count == 0) {
            return ItemStack.EMPTY.copy();
        }
        
        ItemStack extracted = existing.copy();
        extracted.setCount(count);
        
        if (!simulate) {
            existing.shrink(count);
        }
        
        return extracted;
    }
    
    @Override
    public int getSlotLimit(int slot) {
        return defaultSlotLimit;
    }
    
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
    
    /**
     * 驗證槽位索引
     * 
     * @throws IndexOutOfBoundsException 如果索引無效
     */
    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            throw new IndexOutOfBoundsException("Slot index out of bounds: " + slot + " (size: " + stacks.size() + ")");
        }
    }
    
    /**
     * 取得所有物品堆疊的副本
     */
    public List<ItemStack> getStacks() {
        List<ItemStack> copies = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            copies.add(stack.copy());
        }
        return copies;
    }
    
    /**
     * 取得總物品數量
     */
    public int getTotalItemCount() {
        int total = 0;
        for (ItemStack stack : stacks) {
            total += stack.getCount();
        }
        return total;
    }
    
    /**
     * 清空所有槽位
     */
    public void clear() {
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY.copy());
        }
    }
    
    /**
     * 檢查是否為空
     */
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "ItemStackHandler[slots=" + getSlots() + ", total=" + getTotalItemCount() + "]";
    }
}
