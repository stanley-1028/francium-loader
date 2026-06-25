package com.francium.forge.item;

/**
 * 物品堆疊測試
 * 
 * 測試 ItemStack 的各種功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ItemStackTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== ItemStack 測試 ===");
        System.out.println();
        
        testBasicCreation();
        testEmptyStack();
        testGrow();
        testShrink();
        testSplit();
        testCopy();
        testIsItemEqual();
        testBoundaryConditions();
        testParameterValidation();
        
        System.out.println();
        System.out.println("=== 測試結果 ===");
        System.out.println("通過: " + passed);
        System.out.println("失敗: " + failed);
        System.out.println("總計: " + (passed + failed));
        
        if (failed > 0) {
            System.exit(1);
        }
    }
    
    private static void testBasicCreation() {
        try {
            ItemStack stack = new ItemStack("minecraft:diamond", 32);
            assertEqual("minecraft:diamond", stack.getItemId(), "基本建立 - 物品ID");
            assertEqual(32, stack.getCount(), "基本建立 - 數量");
            assertEqual(64, stack.getMaxStackSize(), "基本建立 - 最大堆疊");
            assertFalse(stack.isEmpty(), "基本建立 - 不為空");
            passed++;
            System.out.println("✓ testBasicCreation");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBasicCreation: " + e.getMessage());
        }
    }
    
    private static void testEmptyStack() {
        try {
            ItemStack empty = ItemStack.EMPTY;
            assertTrue(empty.isEmpty(), "空堆疊 - isEmpty");
            assertEqual(0, empty.getCount(), "空堆疊 - 數量為0");
            passed++;
            System.out.println("✓ testEmptyStack");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEmptyStack: " + e.getMessage());
        }
    }
    
    private static void testGrow() {
        try {
            ItemStack stack = new ItemStack("minecraft:diamond", 32);
            stack.grow(10);
            assertEqual(42, stack.getCount(), "增加數量 - 正常增加");
            
            stack.grow(30);
            assertEqual(64, stack.getCount(), "增加數量 - 不超過最大堆疊");
            passed++;
            System.out.println("✓ testGrow");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testGrow: " + e.getMessage());
        }
    }
    
    private static void testShrink() {
        try {
            ItemStack stack = new ItemStack("minecraft:diamond", 32);
            stack.shrink(10);
            assertEqual(22, stack.getCount(), "減少數量 - 正常減少");
            
            stack.shrink(30);
            assertEqual(0, stack.getCount(), "減少數量 - 不低於0");
            assertTrue(stack.isEmpty(), "減少數量 - 變為空");
            passed++;
            System.out.println("✓ testShrink");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testShrink: " + e.getMessage());
        }
    }
    
    private static void testSplit() {
        try {
            ItemStack stack = new ItemStack("minecraft:diamond", 32);
            ItemStack split = stack.split(10);
            
            assertEqual(22, stack.getCount(), "分割 - 剩餘數量");
            assertEqual(10, split.getCount(), "分割 - 分出數量");
            assertEqual("minecraft:diamond", split.getItemId(), "分割 - 物品ID相同");
            passed++;
            System.out.println("✓ testSplit");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testSplit: " + e.getMessage());
        }
    }
    
    private static void testCopy() {
        try {
            ItemStack original = new ItemStack("minecraft:diamond", 32);
            ItemStack copy = original.copy();
            
            assertEqual(original.getItemId(), copy.getItemId(), "複製 - 物品ID相同");
            assertEqual(original.getCount(), copy.getCount(), "複製 - 數量相同");
            
            copy.grow(10);
            assertEqual(32, original.getCount(), "複製 - 獨立性");
            assertEqual(42, copy.getCount(), "複製 - 獨立性");
            passed++;
            System.out.println("✓ testCopy");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testCopy: " + e.getMessage());
        }
    }
    
    private static void testIsItemEqual() {
        try {
            ItemStack stack1 = new ItemStack("minecraft:diamond", 32);
            ItemStack stack2 = new ItemStack("minecraft:diamond", 64);
            ItemStack stack3 = new ItemStack("minecraft:iron_ingot", 32);
            
            assertTrue(stack1.isItemEqual(stack2), "相同物品 - 相等");
            assertFalse(stack1.isItemEqual(stack3), "不同物品 - 不相等");
            passed++;
            System.out.println("✓ testIsItemEqual");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testIsItemEqual: " + e.getMessage());
        }
    }
    
    private static void testBoundaryConditions() {
        try {
            ItemStack fullStack = new ItemStack("minecraft:diamond", 64);
            assertTrue(fullStack.isFull(), "滿堆疊 - isFull");
            
            ItemStack zeroStack = new ItemStack("minecraft:diamond", 0);
            assertTrue(zeroStack.isEmpty(), "零數量 - 為空");
            
            passed++;
            System.out.println("✓ testBoundaryConditions");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBoundaryConditions: " + e.getMessage());
        }
    }
    
    private static void testParameterValidation() {
        try {
            new ItemStack(null, 10);
            failed++;
            System.out.println("✗ testParameterValidation - null itemId 應該丟出例外");
            return;
        } catch (IllegalArgumentException e) {
            // 預期的例外
        }
        
        try {
            new ItemStack("", 10);
            failed++;
            System.out.println("✗ testParameterValidation - 空 itemId 應該丟出例外");
            return;
        } catch (IllegalArgumentException e) {
            // 預期的例外
        }
        
        try {
            new ItemStack("minecraft:diamond", -1);
            failed++;
            System.out.println("✗ testParameterValidation - 負數量 應該丟出例外");
            return;
        } catch (IllegalArgumentException e) {
            // 預期的例外
        }
        
        try {
            new ItemStack("minecraft:diamond", 10, 0);
            failed++;
            System.out.println("✗ testParameterValidation - 最大堆疊為0 應該丟出例外");
            return;
        } catch (IllegalArgumentException e) {
            // 預期的例外
        }
        
        passed++;
        System.out.println("✓ testParameterValidation");
    }
    
    private static void assertEqual(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": 預期 " + expected + " 但得到 " + actual);
        }
    }
    
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message + ": 預期為 true");
        }
    }
    
    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message + ": 預期為 false");
        }
    }
}
