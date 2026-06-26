package com.francium.forge.recipe;

/**
 * 配方類型
 * 
 * 對應 Forge/Minecraft 的 RecipeType
 * 表示一種配方的類型
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class RecipeType<T extends Recipe> {
    
    /** 類型 ID */
    private final String id;
    
    /** 類型名稱 */
    private final String name;
    
    // 內建配方類型
    public static final RecipeType<Recipe> CRAFTING = new RecipeType<>("minecraft:crafting", "Crafting");
    public static final RecipeType<Recipe> SMELTING = new RecipeType<>("minecraft:smelting", "Smelting");
    public static final RecipeType<Recipe> BLASTING = new RecipeType<>("minecraft:blasting", "Blasting");
    public static final RecipeType<Recipe> SMOKING = new RecipeType<>("minecraft:smoking", "Smoking");
    public static final RecipeType<Recipe> CAMPFIRE_COOKING = new RecipeType<>("minecraft:campfire_cooking", "Campfire Cooking");
    public static final RecipeType<Recipe> STONECUTTING = new RecipeType<>("minecraft:stonecutting", "Stonecutting");
    public static final RecipeType<Recipe> SMITHING = new RecipeType<>("minecraft:smithing", "Smithing");
    
    /**
     * 建立配方類型
     * 
     * @param id 類型 ID
     * @param name 類型名稱
     */
    public RecipeType(String id, String name) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Recipe type ID cannot be null or empty");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Recipe type name cannot be null or empty");
        }
        this.id = id;
        this.name = name;
    }
    
    /**
     * 取得類型 ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 取得類型名稱
     */
    public String getName() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RecipeType)) return false;
        RecipeType<?> other = (RecipeType<?>) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return name;
    }
}
