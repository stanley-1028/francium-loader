package com.francium.forge.block;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 方塊狀態測試
 * 
 * 測試 BlockState 和各種 Property 的功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class BlockStateTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== BlockState 測試 ===");
        System.out.println();
        
        testBooleanProperty();
        testIntegerProperty();
        testEnumProperty();
        testDirection();
        testBasicBlockState();
        testSetValue();
        testGetAllStates();
        testToString();
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
    
    private static void testBooleanProperty() {
        try {
            BooleanProperty prop = BooleanProperty.create("open");
            
            assertEqual("open", prop.getName(), "布林屬性 - 名稱");
            assertEqual(Boolean.class, prop.getValueClass(), "布林屬性 - 類型");
            
            Collection<Boolean> values = prop.getPossibleValues();
            assertEqual(2, values.size(), "布林屬性 - 值數量");
            assertTrue(values.contains(true), "布林屬性 - 包含 true");
            assertTrue(values.contains(false), "布林屬性 - 包含 false");
            
            assertEqual("true", prop.getName(true), "布林屬性 - true 字串");
            assertEqual("false", prop.getName(false), "布林屬性 - false 字串");
            
            assertTrue(prop.getValue("true"), "布林屬性 - 解析 true");
            assertFalse(prop.getValue("false"), "布林屬性 - 解析 false");
            
            passed++;
            System.out.println("✓ testBooleanProperty");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBooleanProperty: " + e.getMessage());
        }
    }
    
    private static void testIntegerProperty() {
        try {
            IntegerProperty prop = IntegerProperty.create("age", 0, 5);
            
            assertEqual("age", prop.getName(), "整數屬性 - 名稱");
            assertEqual(0, prop.getMin(), "整數屬性 - 最小值");
            assertEqual(5, prop.getMax(), "整數屬性 - 最大值");
            
            Collection<Integer> values = prop.getPossibleValues();
            assertEqual(6, values.size(), "整數屬性 - 值數量");
            assertTrue(values.contains(0), "整數屬性 - 包含 0");
            assertTrue(values.contains(5), "整數屬性 - 包含 5");
            
            assertEqual("3", prop.getName(3), "整數屬性 - 字串表示");
            assertEqual(3, (int) prop.getValue("3"), "整數屬性 - 解析值");
            
            passed++;
            System.out.println("✓ testIntegerProperty");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testIntegerProperty: " + e.getMessage());
        }
    }
    
    private static void testEnumProperty() {
        try {
            EnumProperty<Direction.Axis> prop = EnumProperty.create("axis", Direction.Axis.class);
            
            assertEqual("axis", prop.getName(), "列舉屬性 - 名稱");
            
            Collection<Direction.Axis> values = prop.getPossibleValues();
            assertEqual(3, values.size(), "列舉屬性 - 值數量");
            
            assertEqual("x", prop.getName(Direction.Axis.X), "列舉屬性 - 字串表示");
            assertEqual(Direction.Axis.X, prop.getValue("x"), "列舉屬性 - 解析值");
            assertEqual(Direction.Axis.Y, prop.getValue("Y"), "列舉屬性 - 解析值（大小寫不敏感）");
            
            passed++;
            System.out.println("✓ testEnumProperty");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEnumProperty: " + e.getMessage());
        }
    }
    
    private static void testDirection() {
        try {
            assertEqual(Direction.UP, Direction.DOWN.getOpposite(), "方向 - 相反方向");
            assertEqual(Direction.SOUTH, Direction.NORTH.getOpposite(), "方向 - 相反方向");
            assertEqual(Direction.EAST, Direction.WEST.getOpposite(), "方向 - 相反方向");
            
            assertEqual(Direction.EAST, Direction.NORTH.getClockWise(), "方向 - 順時針");
            assertEqual(Direction.SOUTH, Direction.EAST.getClockWise(), "方向 - 順時針");
            
            assertEqual(Direction.WEST, Direction.NORTH.getCounterClockWise(), "方向 - 逆時針");
            
            assertTrue(Direction.UP.isVertical(), "方向 - 垂直");
            assertTrue(Direction.DOWN.isVertical(), "方向 - 垂直");
            assertTrue(Direction.NORTH.isHorizontal(), "方向 - 水平");
            assertTrue(Direction.SOUTH.isHorizontal(), "方向 - 水平");
            
            assertEqual(-1, Direction.DOWN.getStepY(), "方向 - 步長");
            assertEqual(1, Direction.UP.getStepY(), "方向 - 步長");
            assertEqual(-1, Direction.NORTH.getStepZ(), "方向 - 步長");
            
            passed++;
            System.out.println("✓ testDirection");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testDirection: " + e.getMessage());
        }
    }
    
    private static void testBasicBlockState() {
        try {
            BooleanProperty openProp = BooleanProperty.create("open");
            IntegerProperty ageProp = IntegerProperty.create("age", 0, 3);
            
            BlockState state = new BlockState("minecraft:door", Arrays.asList(openProp, ageProp));
            
            assertEqual("minecraft:door", state.getBlockId(), "方塊狀態 - 方塊 ID");
            assertEqual(2, state.getProperties().size(), "方塊狀態 - 屬性數量");
            
            assertFalse(state.getValue(openProp), "方塊狀態 - 布林預設值");
            assertEqual(0, (int) state.getValue(ageProp), "方塊狀態 - 整數預設值");
            
            passed++;
            System.out.println("✓ testBasicBlockState");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBasicBlockState: " + e.getMessage());
        }
    }
    
    private static void testSetValue() {
        try {
            BooleanProperty openProp = BooleanProperty.create("open");
            IntegerProperty ageProp = IntegerProperty.create("age", 0, 3);
            
            BlockState state = new BlockState("minecraft:door", Arrays.asList(openProp, ageProp));
            
            BlockState newState = state.setValue(openProp, true);
            
            // 原始狀態應該不變
            assertFalse(state.getValue(openProp), "設定值 - 原始狀態不變");
            // 新狀態應該有新的值
            assertTrue(newState.getValue(openProp), "設定值 - 新狀態有新值");
            // 其他屬性應該保持不變
            assertEqual(0, (int) newState.getValue(ageProp), "設定值 - 其他屬性不變");
            
            passed++;
            System.out.println("✓ testSetValue");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testSetValue: " + e.getMessage());
        }
    }
    
    private static void testGetAllStates() {
        try {
            BooleanProperty prop1 = BooleanProperty.create("a");
            BooleanProperty prop2 = BooleanProperty.create("b");
            
            BlockState state = new BlockState("test", Arrays.asList(prop1, prop2));
            
            List<BlockState> allStates = state.getAllStates();
            assertEqual(4, allStates.size(), "所有狀態 - 數量（2x2=4）");
            
            // 驗證每個狀態都是唯一的
            for (int i = 0; i < allStates.size(); i++) {
                for (int j = i + 1; j < allStates.size(); j++) {
                    assertFalse(allStates.get(i).equals(allStates.get(j)), 
                        "所有狀態 - 狀態唯一");
                }
            }
            
            passed++;
            System.out.println("✓ testGetAllStates");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testGetAllStates: " + e.getMessage());
        }
    }
    
    private static void testToString() {
        try {
            BooleanProperty openProp = BooleanProperty.create("open");
            
            BlockState state = new BlockState("minecraft:door", Arrays.asList(openProp));
            
            String str = state.toString();
            assertTrue(str.contains("minecraft:door"), "字串表示 - 包含方塊 ID");
            assertTrue(str.contains("open=false"), "字串表示 - 包含屬性值");
            
            BlockState newState = state.setValue(openProp, true);
            String newStr = newState.toString();
            assertTrue(newStr.contains("open=true"), "字串表示 - 包含新屬性值");
            
            passed++;
            System.out.println("✓ testToString");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testToString: " + e.getMessage());
        }
    }
    
    private static void testParameterValidation() {
        try {
            // 測試 null 屬性名稱
            try {
                BooleanProperty.create(null);
                failed++;
                System.out.println("✗ testParameterValidation - null 名稱應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試空屬性名稱
            try {
                BooleanProperty.create("");
                failed++;
                System.out.println("✗ testParameterValidation - 空名稱應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試無效的布林值
            BooleanProperty prop = BooleanProperty.create("test");
            try {
                prop.getValue("invalid");
                failed++;
                System.out.println("✗ testParameterValidation - 無效布林值應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試超出範圍的整數
            IntegerProperty intProp = IntegerProperty.create("test", 0, 5);
            try {
                intProp.getValue("10");
                failed++;
                System.out.println("✗ testParameterValidation - 超出範圍整數應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試無效的列舉值
            EnumProperty<Direction.Axis> enumProp = EnumProperty.create("axis", Direction.Axis.class);
            try {
                enumProp.getValue("invalid");
                failed++;
                System.out.println("✗ testParameterValidation - 無效列舉值應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            passed++;
            System.out.println("✓ testParameterValidation");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testParameterValidation: " + e.getMessage());
        }
    }
    
    private static void assertEqual(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
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
