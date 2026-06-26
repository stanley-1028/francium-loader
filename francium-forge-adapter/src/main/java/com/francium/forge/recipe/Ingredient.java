package com.francium.forge.recipe;

import com.francium.forge.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 材料
 * 
 * 對應 Forge/Minecraft 的 Ingredient
 * 表示配方中的一個材料，可以匹配多種物品
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class Ingredient {
    
    /** 匹配的物品堆疊列表 */
    private final List<ItemStack> matchingStacks;
    
    /** 是否為空 */
    private final boolean empty;
    
    /** 空材料 */
    public static final Ingredient EMPTY = new Ingredient(Collections.emptyList());
    
    /**
     * 建立材料
     * 
     * @param matchingStacks 匹配的物品堆疊列表
     */
    public Ingredient(List<ItemStack> matchingStacks) {
        if (matchingStacks == null) {
            throw new IllegalArgumentException("Matching stacks cannot be null");
        }
        this.matchingStacks = Collections.unmodifiableList(new ArrayList<>(matchingStacks));
        this.empty = matchingStacks.isEmpty();
    }
    
    /**
     * 建立單一物品的材料
     * 
     * @param stack 物品堆疊
     * @return 材料
     */
    public static Ingredient of(ItemStack stack) {
        if (stack == null) {
            throw new IllegalArgumentException("ItemStack cannot be null");
        }
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(stack);
        return new Ingredient(stacks);
    }
    
    /**
     * 建立多個物品的材料
     * 
     * @param stacks 物品堆疊陣列
     * @return 材料
     */
    public static Ingredient of(ItemStack... stacks) {
        if (stacks == null) {
            throw new IllegalArgumentException("ItemStacks cannot be null");
        }
        List<ItemStack> stackList = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                stackList.add(stack);
            }
        }
        return new Ingredient(stackList);
    }
    
    /**
     * 取得匹配的物品堆疊列表
     */
    public List<ItemStack> getMatchingStacks() {
        return matchingStacks;
    }
    
    /**
     * 檢查是否為空
     */
    public boolean isEmpty() {
        return empty;
    }
    
    /**
     * 測試物品堆疊是否匹配
     * 
     * @param stack 物品堆疊
     * @return 是否匹配
     */
    public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return empty;
        }
        if (empty) {
            return false;
        }
        for (ItemStack matchingStack : matchingStacks) {
            if (matchingStack.isItemEqual(stack)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 取得匹配數量
     */
    public int getMatchingCount() {
        return matchingStacks.size();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Ingredient)) return false;
        Ingredient other = (Ingredient) obj;
        if (empty != other.empty) return false;
        if (matchingStacks.size() != other.matchingStacks.size()) return false;
        
        // 簡單比較：檢查所有物品 ID 是否相同
        List<String> thisIds = new ArrayList<>();
        for (ItemStack stack : matchingStacks) {
            thisIds.add(stack.getItemId());
        }
        Collections.sort(thisIds);
        
        List<String> otherIds = new ArrayList<>();
        for (ItemStack stack : other.matchingStacks) {
            otherIds.add(stack.getItemId());
        }
        Collections.sort(otherIds);
        
        return thisIds.equals(otherIds);
    }
    
    @Override
    public int hashCode() {
        int result = 0;
        for (ItemStack stack : matchingStacks) {
            result += stack.getItemId().hashCode();
        }
        return result;
    }
    
    @Override
    public String toString() {
        if (empty) {
            return "Ingredient.EMPTY";
        }
        return "Ingredient[" + matchingStacks.size() + " items]";
    }
}
