package com.francium.forge.recipe;

import com.francium.forge.item.ItemStack;

/**
 * 配方基底類別
 * 
 * 對應 Forge/Minecraft 的 Recipe
 * 表示一個合成配方
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public abstract class Recipe {
    
    /** 配方 ID */
    private final String id;
    
    /** 配方類型 */
    private final RecipeType<?> type;
    
    /** 輸出結果 */
    private final ItemStack result;
    
    /**
     * 建立配方
     * 
     * @param id 配方 ID
     * @param type 配方類型
     * @param result 輸出結果
     */
    protected Recipe(String id, RecipeType<?> type, ItemStack result) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Recipe ID cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Recipe type cannot be null");
        }
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        
        this.id = id;
        this.type = type;
        this.result = result.copy();
    }
    
    /**
     * 取得配方 ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 取得配方類型
     */
    public RecipeType<?> getType() {
        return type;
    }
    
    /**
     * 取得輸出結果
     */
    public ItemStack getResult() {
        return result.copy();
    }
    
    /**
     * 檢查配方是否匹配
     * 
     * 子類別需要實作這個方法來檢查配方是否匹配
     * 
     * @param inventory 物品欄
     * @return 是否匹配
     */
    public abstract boolean matches(Object inventory);
    
    /**
     * 取得配方輸出
     * 
     * 子類別可以覆寫這個方法來回傳不同的輸出
     * 
     * @param inventory 物品欄
     * @return 輸出結果
     */
    public ItemStack assemble(Object inventory) {
        return result.copy();
    }
    
    /**
     * 取得配方寬度
     */
    public int getWidth() {
        return 0;
    }
    
    /**
     * 取得配方高度
     */
    public int getHeight() {
        return 0;
    }
    
    /**
     * 檢查是否為特殊配方
     * 
     * 特殊配方不會顯示在配方書中
     */
    public boolean isSpecial() {
        return false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Recipe)) return false;
        Recipe other = (Recipe) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return id + " (" + type + ")";
    }
}
