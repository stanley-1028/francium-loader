package com.francium.forge.fluid;

/**
 * 流體堆疊測試
 * 
 * 測試 FluidStack 的各種功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class FluidStackTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== FluidStack 測試 ===");
        System.out.println();
        
        testBasicCreation();
        testEmptyStack();
        testGrow();
        testShrink();
        testCopy();
        testIsFluidEqual();
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
            FluidStack stack = new FluidStack("minecraft:water", 500);
            assertEqual("minecraft:water", stack.getFluidId(), "基本建立 - 流體ID");
            assertEqual(500, stack.getAmount(), "基本建立 - 數量");
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
            FluidStack empty = FluidStack.empty();
            assertTrue(empty.isEmpty(), "空堆疊 - isEmpty");
            assertEqual(0, empty.getAmount(), "空堆疊 - 數量為0");
            passed++;
            System.out.println("✓ testEmptyStack");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEmptyStack: " + e.getMessage());
        }
    }
    
    private static void testGrow() {
        try {
            FluidStack stack = new FluidStack("minecraft:water", 500);
            stack.grow(300);
            assertEqual(800, stack.getAmount(), "增加數量 - 正常增加");
            passed++;
            System.out.println("✓ testGrow");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testGrow: " + e.getMessage());
        }
    }
    
    private static void testShrink() {
        try {
            FluidStack stack = new FluidStack("minecraft:water", 500);
            stack.shrink(200);
            assertEqual(300, stack.getAmount(), "減少數量 - 正常減少");
            
            stack.shrink(400);
            assertEqual(0, stack.getAmount(), "減少數量 - 不低於0");
            assertTrue(stack.isEmpty(), "減少數量 - 變為空");
            passed++;
            System.out.println("✓ testShrink");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testShrink: " + e.getMessage());
        }
    }
    
    private static void testCopy() {
        try {
            FluidStack original = new FluidStack("minecraft:water", 500);
            FluidStack copy = original.copy();
            
            assertEqual(original.getFluidId(), copy.getFluidId(), "複製 - 流體ID相同");
            assertEqual(original.getAmount(), copy.getAmount(), "複製 - 數量相同");
            
            // 修改副本不影響原件
            copy.grow(300);
            assertEqual(500, original.getAmount(), "複製 - 獨立性");
            assertEqual(800, copy.getAmount(), "複製 - 獨立性");
            passed++;
            System.out.println("✓ testCopy");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testCopy: " + e.getMessage());
        }
    }
    
    private static void testIsFluidEqual() {
        try {
            FluidStack stack1 = new FluidStack("minecraft:water", 500);
            FluidStack stack2 = new FluidStack("minecraft:water", 1000);
            FluidStack stack3 = new FluidStack("minecraft:lava", 500);
            
            assertTrue(stack1.isFluidEqual(stack2), "相同流體 - 相等");
            assertFalse(stack1.isFluidEqual(stack3), "不同流體 - 不相等");
            passed++;
            System.out.println("✓ testIsFluidEqual");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testIsFluidEqual: " + e.getMessage());
        }
    }
    
    private static void testParameterValidation() {
        try {
            new FluidStack(null, 100);
            failed++;
            System.out.println("✗ testParameterValidation - null fluidId 應該丟出例外");
            return;
        } catch (IllegalArgumentException e) {
            // 預期的例外
        }
        
        try {
            new FluidStack("", 100);
            failed++;
            System.out.println("✗ testParameterValidation - 空 fluidId 應該丟出例外");
            return;
        } catch (IllegalArgumentException e) {
            // 預期的例外
        }
        
        try {
            new FluidStack("minecraft:water", -1);
            failed++;
            System.out.println("✗ testParameterValidation - 負數量 應該丟出例外");
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
