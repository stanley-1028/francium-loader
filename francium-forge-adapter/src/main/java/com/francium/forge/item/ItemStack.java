package com.francium.forge.item;

/**
 * 物品堆疊
 * 
 * 對應 Forge 的 ItemStack
 * 表示一定數量的某個物品
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ItemStack {
    
    /** 物品 ID */
    private final String itemId;
    
    /** 物品數量 */
    private int count;
    
    /** 最大堆疊數量 */
    private final int maxStackSize;
    
    /** 空的物品堆疊 */
    public static final ItemStack EMPTY = new ItemStack("empty", 0, 1);
    
    /**
     * 建立物品堆疊
     * 
     * @param itemId 物品 ID
     * @param count 數量
     * @throws IllegalArgumentException 如果 itemId 為 null/空或 count 為負數
     */
    public ItemStack(String itemId, int count) {
        this(itemId, count, 64); // 預設最大堆疊 64
    }
    
    /**
     * 建立物品堆疊
     * 
     * @param itemId 物品 ID
     * @param count 數量
     * @param maxStackSize 最大堆疊數量
     * @throws IllegalArgumentException 如果參數無效
     */
    public ItemStack(String itemId, int count, int maxStackSize) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        if (count < 0) {
            throw new IllegalArgumentException("Item count cannot be negative: " + count);
        }
        if (maxStackSize <= 0) {
            throw new IllegalArgumentException("Max stack size must be positive: " + maxStackSize);
        }
        this.itemId = itemId;
        this.count = count;
        this.maxStackSize = maxStackSize;
    }
    
    /**
     * 建立物品堆疊（複製）
     * 
     * @param other 要複製的物品堆疊
     * @throws IllegalArgumentException 如果 other 為 null
     */
    public ItemStack(ItemStack other) {
        if (other == null) {
            throw new IllegalArgumentException("ItemStack to copy cannot be null");
        }
        this.itemId = other.itemId;
        this.count = other.count;
        this.maxStackSize = other.maxStackSize;
    }
    
    /**
     * 取得物品 ID
     */
    public String getItemId() {
        return itemId;
    }
    
    /**
     * 取得數量
     */
    public int getCount() {
        return count;
    }
    
    /**
     * 設定數量
     * 
     * @param count 數量（必須 >= 0 且 <= maxStackSize）
     * @throws IllegalArgumentException 如果 count 無效
     */
    public void setCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Item count cannot be negative: " + count);
        }
        if (count > maxStackSize) {
            throw new IllegalArgumentException("Item count cannot exceed max stack size: " + count + " > " + maxStackSize);
        }
        this.count = count;
    }
    
    /**
     * 取得最大堆疊數量
     */
    public int getMaxStackSize() {
        return maxStackSize;
    }
    
    /**
     * 檢查是否為空
     */
    public boolean isEmpty() {
        return count <= 0;
    }
    
    /**
     * 檢查是否已滿
     */
    public boolean isFull() {
        return count >= maxStackSize;
    }
    
    /**
     * 增加數量
     * 
     * @param amount 要增加的數量
     * @return 實際增加的數量
     * @throws IllegalArgumentException 如果 amount 為負數
     */
    public int grow(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot grow by negative amount: " + amount);
        }
        int actual = Math.min(amount, maxStackSize - count);
        count += actual;
        return actual;
    }
    
    /**
     * 減少數量
     * 
     * @param amount 要減少的數量
     * @return 實際減少的數量
     * @throws IllegalArgumentException 如果 amount 為負數
     */
    public int shrink(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot shrink by negative amount: " + amount);
        }
        int actual = Math.min(amount, count);
        count -= actual;
        return actual;
    }
    
    /**
     * 檢查是否為相同物品（不考慮數量）
     */
    public boolean isItemEqual(ItemStack other) {
        return other != null && itemId.equals(other.itemId);
    }
    
    /**
     * 複製
     */
    public ItemStack copy() {
        return new ItemStack(this);
    }
    
    /**
     * 分割物品堆疊
     * 
     * @param amount 要分割的數量
     * @return 分割出的物品堆疊
     */
    public ItemStack split(int amount) {
        if (amount <= 0) {
            return EMPTY.copy();
        }
        int actual = Math.min(amount, count);
        ItemStack result = new ItemStack(itemId, actual, maxStackSize);
        count -= actual;
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ItemStack that = (ItemStack) obj;
        return count == that.count && itemId.equals(that.itemId);
    }
    
    @Override
    public int hashCode() {
        int result = itemId.hashCode();
        result = 31 * result + count;
        return result;
    }
    
    @Override
    public String toString() {
        return "ItemStack{" + itemId + ", " + count + "/" + maxStackSize + "}";
    }
}
