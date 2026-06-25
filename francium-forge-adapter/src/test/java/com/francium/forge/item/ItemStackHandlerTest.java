package com.francium.forge.item;

/**
 * 物品處理器測試
 * 
 * 測試 ItemStackHandler 的各種功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ItemStackHandlerTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== ItemStackHandler 測試 ===");
        System.out.println();
        
        testBasicCreation();
        testInsertItem();
        testExtractItem();
        testSlotValidation();
        testIsEmpty();
        testClear();
        testGetTotalItemCount();
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
            ItemStackHandler handler = new ItemStackHandler(9);
            assertEqual(9, handler.getSlots(), "基本建立 - 槽位數量");
            assertTrue(handler.isEmpty(), "基本建立 - 初始為空");
            assertEqual(0, handler.getTotalItemCount(), "基本建立 - 總數為0");
            passed++;
            System.out.println("✓ testBasicCreation");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBasicCreation: " + e.getMessage());
        }
    }
    
    private static void testInsertItem() {
        try {
            ItemStackHandler handler = new ItemStackHandler(3);
            
            ItemStack stack = new ItemStack("minecraft:diamond", 32);
            ItemStack remaining = handler.insertItem(0, stack, false);
            
            assertTrue(remaining.isEmpty(), "插入 - 全部插入，無剩餘");
            assertEqual(32, handler.getStackInSlot(0).getCount(), "插入 - 槽位0數量");
            
            // 再插入一些，測試堆疊
            ItemStack stack2 = new ItemStack("minecraft:diamond", 40);
            ItemStack remaining2 = handler.insertItem(0, stack2, false);
            
            assertEqual(8, remaining2.getCount(), "插入 - 部分插入，剩餘8個");
            assertEqual(64, handler.getStackInSlot(0).getCount(), "插入 - 槽位0已滿");
            
            passed++;
            System.out.println("✓ testInsertItem");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testInsertItem: " + e.getMessage());
        }
    }
    
    private static void testExtractItem() {
        try {
            ItemStackHandler handler = new ItemStackHandler(3);
            handler.insertItem(0, new ItemStack("minecraft:diamond", 32), false);
            
            ItemStack extracted = handler.extractItem(0, 10, false);
            assertEqual(10, extracted.getCount(), "提取 - 提取10個");
            assertEqual(22, handler.getStackInSlot(0).getCount(), "提取 - 剩餘22個");
            
            // 提取超過數量
            ItemStack extracted2 = handler.extractItem(0, 50, false);
            assertEqual(22, extracted2.getCount(), "提取 - 提取剩餘全部");
            assertTrue(handler.getStackInSlot(0).isEmpty(), "提取 - 槽位為空");
            
            passed++;
            System.out.println("✓ testExtractItem");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testExtractItem: " + e.getMessage());
        }
    }
    
    private static void testSlotValidation() {
        try {
            ItemStackHandler handler = new ItemStackHandler(3);
            
            // 測試無效槽位
            try {
                handler.getStackInSlot(-1);
                failed++;
                System.out.println("✗ testSlotValidation - 負數槽位應該丟出例外");
                return;
            } catch (IndexOutOfBoundsException e) {
                // 預期的例外
            }
            
            try {
                handler.getStackInSlot(3);
                failed++;
                System.out.println("✗ testSlotValidation - 超出範圍槽位應該丟出例外");
                return;
            } catch (IndexOutOfBoundsException e) {
                // 預期的例外
            }
            
            passed++;
            System.out.println("✓ testSlotValidation");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testSlotValidation: " + e.getMessage());
        }
    }
    
    private static void testIsEmpty() {
        try {
            ItemStackHandler handler = new ItemStackHandler(3);
            assertTrue(handler.isEmpty(), "空容器 - isEmpty");
            
            handler.insertItem(1, new ItemStack("minecraft:diamond", 1), false);
            assertFalse(handler.isEmpty(), "有物品 - 不為空");
            
            handler.extractItem(1, 1, false);
            assertTrue(handler.isEmpty(), "清空後 - 為空");
            
            passed++;
            System.out.println("✓ testIsEmpty");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testIsEmpty: " + e.getMessage());
        }
    }
    
    private static void testClear() {
        try {
            ItemStackHandler handler = new ItemStackHandler(3);
            handler.insertItem(0, new ItemStack("minecraft:diamond", 32), false);
            handler.insertItem(1, new ItemStack("minecraft:iron_ingot", 16), false);
            
            assertFalse(handler.isEmpty(), "清空前 - 不為空");
            
            handler.clear();
            assertTrue(handler.isEmpty(), "清空後 - 為空");
            assertEqual(0, handler.getTotalItemCount(), "清空後 - 總數為0");
            
            passed++;
            System.out.println("✓ testClear");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testClear: " + e.getMessage());
        }
    }
    
    private static void testGetTotalItemCount() {
        try {
            ItemStackHandler handler = new ItemStackHandler(3);
            handler.insertItem(0, new ItemStack("minecraft:diamond", 32), false);
            handler.insertItem(1, new ItemStack("minecraft:iron_ingot", 16), false);
            handler.insertItem(2, new ItemStack("minecraft:gold_ingot", 8), false);
            
            assertEqual(56, handler.getTotalItemCount(), "總數計算 - 32+16+8=56");
            
            passed++;
            System.out.println("✓ testGetTotalItemCount");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testGetTotalItemCount: " + e.getMessage());
        }
    }
    
    private static void testParameterValidation() {
        try {
            new ItemStackHandler(0);
            failed++;
            System.out.println("✗ testParameterValidation - 0槽位應該丟出例外");
            return;
        } catch (IllegalArgumentException e) {
            // 預期的例外
        }
        
        try {
            new ItemStackHandler(-1);
            failed++;
            System.out.println("✗ testParameterValidation - 負數槽位應該丟出例外");
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
