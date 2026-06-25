package com.francium.forge.config;

import java.util.Arrays;
import java.util.List;

/**
 * 設定檔規格測試
 * 
 * 測試 ModConfigSpec 的各種功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ModConfigSpecTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== ModConfigSpec 測試 ===");
        System.out.println();
        
        testBasicBuilder();
        testBooleanConfig();
        testIntConfig();
        testDoubleConfig();
        testStringConfig();
        testEnumConfig();
        testListConfig();
        testCategories();
        testIntRangeValidation();
        testDoubleRangeValidation();
        
        System.out.println();
        System.out.println("=== 測試結果 ===");
        System.out.println("通過: " + passed);
        System.out.println("失敗: " + failed);
        System.out.println("總計: " + (passed + failed));
        
        if (failed > 0) {
            System.exit(1);
        }
    }
    
    private static void testBasicBuilder() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            ModConfigSpec spec = builder.build();
            
            assertNotNull(spec, "基本建立 - 規格不為 null");
            assertEqual(0, spec.getValues().size(), "基本建立 - 預設值數量");
            
            passed++;
            System.out.println("✓ testBasicBuilder");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBasicBuilder: " + e.getMessage());
        }
    }
    
    private static void testBooleanConfig() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            ModConfigSpec.BooleanValue value = builder.define("testBool", true);
            ModConfigSpec spec = builder.build();
            
            assertTrue(value.get(), "布林設定 - 預設值");
            assertNotNull(spec.getValue("testBool"), "布林設定 - 可從規格取得");
            
            value.set(false);
            assertFalse(value.get(), "布林設定 - 設定值");
            
            passed++;
            System.out.println("✓ testBooleanConfig");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBooleanConfig: " + e.getMessage());
        }
    }
    
    private static void testIntConfig() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            ModConfigSpec.IntValue value = builder.defineInRange("testInt", 50, 0, 100);
            ModConfigSpec spec = builder.build();
            
            assertEqual(50, value.get(), "整數設定 - 預設值");
            assertEqual(0, value.getMin(), "整數設定 - 最小值");
            assertEqual(100, value.getMax(), "整數設定 - 最大值");
            
            value.set(75);
            assertEqual(75, value.get(), "整數設定 - 設定值");
            
            passed++;
            System.out.println("✓ testIntConfig");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testIntConfig: " + e.getMessage());
        }
    }
    
    private static void testDoubleConfig() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            ModConfigSpec.DoubleValue value = builder.defineInRange("testDouble", 3.14, 0.0, 10.0);
            ModConfigSpec spec = builder.build();
            
            assertEqual(3.14, value.get(), "雙精度設定 - 預設值");
            assertEqual(0.0, value.getMin(), "雙精度設定 - 最小值");
            assertEqual(10.0, value.getMax(), "雙精度設定 - 最大值");
            
            value.set(6.28);
            assertEqual(6.28, value.get(), "雙精度設定 - 設定值");
            
            passed++;
            System.out.println("✓ testDoubleConfig");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testDoubleConfig: " + e.getMessage());
        }
    }
    
    private static void testStringConfig() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            ConfigValue<String> value = builder.define("testString", "hello");
            ModConfigSpec spec = builder.build();
            
            assertEqual("hello", value.get(), "字串設定 - 預設值");
            
            value.set("world");
            assertEqual("world", value.get(), "字串設定 - 設定值");
            
            passed++;
            System.out.println("✓ testStringConfig");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testStringConfig: " + e.getMessage());
        }
    }
    
    private static void testEnumConfig() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            ConfigValue<TestEnum> value = builder.defineEnum("testEnum", TestEnum.MEDIUM, TestEnum.class);
            ModConfigSpec spec = builder.build();
            
            assertEqual(TestEnum.MEDIUM, value.get(), "列舉設定 - 預設值");
            
            value.set(TestEnum.HIGH);
            assertEqual(TestEnum.HIGH, value.get(), "列舉設定 - 設定值");
            
            passed++;
            System.out.println("✓ testEnumConfig");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEnumConfig: " + e.getMessage());
        }
    }
    
    private static void testListConfig() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            List<String> defaultList = Arrays.asList("a", "b", "c");
            ConfigValue<List<String>> value = builder.defineList("testList", defaultList);
            ModConfigSpec spec = builder.build();
            
            assertEqual(3, value.get().size(), "清單設定 - 預設大小");
            assertEqual("a", value.get().get(0), "清單設定 - 第一個元素");
            
            passed++;
            System.out.println("✓ testListConfig");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testListConfig: " + e.getMessage());
        }
    }
    
    private static void testCategories() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            
            builder.push("general");
            builder.define("option1", true);
            builder.push("advanced");
            builder.define("option2", false);
            builder.pop();
            builder.defineInRange("option3", 42, 0, 100);
            builder.pop();
            
            ModConfigSpec spec = builder.build();
            
            assertTrue(spec.getCategories().containsKey("general"), "類別 - general 存在");
            assertTrue(spec.getCategories().containsKey("general.advanced"), "類別 - general.advanced 存在");
            
            assertNotNull(spec.getValue("general.option1"), "類別 - 可取得 general.option1");
            assertNotNull(spec.getValue("general.advanced.option2"), "類別 - 可取得 general.advanced.option2");
            assertNotNull(spec.getValue("general.option3"), "類別 - 可取得 general.option3");
            
            passed++;
            System.out.println("✓ testCategories");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testCategories: " + e.getMessage());
        }
    }
    
    private static void testIntRangeValidation() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            ModConfigSpec.IntValue value = builder.defineInRange("testInt", 50, 0, 100);
            builder.build();
            
            try {
                value.set(-1);
                failed++;
                System.out.println("✗ testIntRangeValidation - 小於最小值應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            try {
                value.set(101);
                failed++;
                System.out.println("✗ testIntRangeValidation - 大於最大值應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            passed++;
            System.out.println("✓ testIntRangeValidation");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testIntRangeValidation: " + e.getMessage());
        }
    }
    
    private static void testDoubleRangeValidation() {
        try {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            ModConfigSpec.DoubleValue value = builder.defineInRange("testDouble", 5.0, 0.0, 10.0);
            builder.build();
            
            try {
                value.set(-1.0);
                failed++;
                System.out.println("✗ testDoubleRangeValidation - 小於最小值應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            try {
                value.set(10.1);
                failed++;
                System.out.println("✗ testDoubleRangeValidation - 大於最大值應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            passed++;
            System.out.println("✓ testDoubleRangeValidation");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testDoubleRangeValidation: " + e.getMessage());
        }
    }
    
    // 測試用列舉
    private enum TestEnum {
        LOW, MEDIUM, HIGH
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
    
    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError(message + ": 預期不為 null");
        }
    }
}
