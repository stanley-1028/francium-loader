package com.francium.forge.recipe;

import com.francium.forge.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配方管理員
 * 
 * 對應 Forge/Minecraft 的 RecipeManager
 * 管理所有配方
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class RecipeManager {
    
    /** 單例實例 */
    private static volatile RecipeManager instance;
    
    /** 所有配方（按 ID 索引） */
    private final Map<String, Recipe> recipesById = new HashMap<>();
    
    /** 按類型分組的配方 */
    private final Map<RecipeType<?>, List<Recipe>> recipesByType = new HashMap<>();
    
    /** 是否已載入 */
    private volatile boolean loaded = false;
    
    /**
     * 取得單例實例
     */
    public static RecipeManager getInstance() {
        if (instance == null) {
            synchronized (RecipeManager.class) {
                if (instance == null) {
                    instance = new RecipeManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 建立配方管理員
     */
    private RecipeManager() {
        // 初始化內建配方類型
        recipesByType.put(RecipeType.CRAFTING, new ArrayList<>());
        recipesByType.put(RecipeType.SMELTING, new ArrayList<>());
        recipesByType.put(RecipeType.BLASTING, new ArrayList<>());
        recipesByType.put(RecipeType.SMOKING, new ArrayList<>());
        recipesByType.put(RecipeType.CAMPFIRE_COOKING, new ArrayList<>());
        recipesByType.put(RecipeType.STONECUTTING, new ArrayList<>());
        recipesByType.put(RecipeType.SMITHING, new ArrayList<>());
    }
    
    /**
     * 註冊配方
     * 
     * @param recipe 配方
     */
    public void register(Recipe recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe cannot be null");
        }
        
        String id = recipe.getId();
        if (recipesById.containsKey(id)) {
            throw new IllegalArgumentException("Recipe with ID '" + id + "' already exists");
        }
        
        recipesById.put(id, recipe);
        
        RecipeType<?> type = recipe.getType();
        List<Recipe> typeRecipes = recipesByType.computeIfAbsent(type, k -> new ArrayList<>());
        typeRecipes.add(recipe);
    }
    
    /**
     * 根據 ID 取得配方
     * 
     * @param id 配方 ID
     * @return 配方（Optional）
     */
    public Optional<Recipe> getRecipe(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(recipesById.get(id));
    }
    
    /**
     * 取得指定類型的所有配方
     * 
     * @param type 配方類型
     * @return 配方列表
     */
    public List<Recipe> getRecipesByType(RecipeType<?> type) {
        if (type == null) {
            return Collections.emptyList();
        }
        List<Recipe> recipes = recipesByType.get(type);
        if (recipes == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(recipes);
    }
    
    /**
     * 取得所有配方
     */
    public List<Recipe> getAllRecipes() {
        return Collections.unmodifiableList(new ArrayList<>(recipesById.values()));
    }
    
    /**
     * 取得配方數量
     */
    public int getRecipeCount() {
        return recipesById.size();
    }
    
    /**
     * 取得指定類型的配方數量
     */
    public int getRecipeCount(RecipeType<?> type) {
        if (type == null) {
            return 0;
        }
        List<Recipe> recipes = recipesByType.get(type);
        return recipes != null ? recipes.size() : 0;
    }
    
    /**
     * 檢查是否包含指定 ID 的配方
     */
    public boolean containsRecipe(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return recipesById.containsKey(id);
    }
    
    /**
     * 根據輸出物品查找配方
     * 
     * @param result 輸出物品
     * @return 配方列表
     */
    public List<Recipe> findRecipesByResult(ItemStack result) {
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Recipe> matching = new ArrayList<>();
        for (Recipe recipe : recipesById.values()) {
            if (recipe.getResult().isItemEqual(result)) {
                matching.add(recipe);
            }
        }
        return Collections.unmodifiableList(matching);
    }
    
    /**
     * 清除所有配方
     */
    public void clear() {
        recipesById.clear();
        for (List<Recipe> recipes : recipesByType.values()) {
            recipes.clear();
        }
        loaded = false;
    }
    
    /**
     * 檢查是否已載入
     */
    public boolean isLoaded() {
        return loaded;
    }
    
    /**
     * 標記為已載入
     */
    public void setLoaded() {
        this.loaded = true;
    }
    
    /**
     * 取得所有配方類型
     */
    public List<RecipeType<?>> getRecipeTypes() {
        return Collections.unmodifiableList(new ArrayList<>(recipesByType.keySet()));
    }
}
