package com.francium.forge.registry;

/**
 * 註冊表測試
 * 
 * 測試 ForgeRegistry 的各種功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeRegistryTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== ForgeRegistry 測試 ===");
        System.out.println();
        
        testBasicRegistration();
        testDuplicateKey();
        testFreeze();
        testGetKey();
        testContains();
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
    
    private static void testBasicRegistration() {
        try {
            ForgeRegistry<String> registry = new ForgeRegistry<>("test", String.class);
            
            registry.register("minecraft:diamond", "Diamond");
            registry.register("minecraft:iron", "Iron");
            registry.register("minecraft:gold", "Gold");
            
            assertEqual("Diamond", registry.getValue("minecraft:diamond"), "基本註冊 - 取得值");
            assertEqual("Iron", registry.getValue("minecraft:iron"), "基本註冊 - 取得值");
            assertEqual("Gold", registry.getValue("minecraft:gold"), "基本註冊 - 取得值");
            
            assertEqual(3, registry.size(), "基本註冊 - 鍵數量");
            
            passed++;
            System.out.println("✓ testBasicRegistration");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBasicRegistration: " + e.getMessage());
        }
    }
    
    private static void testDuplicateKey() {
        try {
            ForgeRegistry<String> registry = new ForgeRegistry<>("test", String.class);
            
            registry.register("minecraft:diamond", "Diamond");
            
            // 重複註冊應該丟出例外
            try {
                registry.register("minecraft:diamond", "Diamond 2");
                failed++;
                System.out.println("✗ testDuplicateKey - 重複註冊應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 確保值沒有被覆蓋
            assertEqual("Diamond", registry.getValue("minecraft:diamond"), "重複註冊 - 值未被覆蓋");
            
            passed++;
            System.out.println("✓ testDuplicateKey");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testDuplicateKey: " + e.getMessage());
        }
    }
    
    private static void testFreeze() {
        try {
            ForgeRegistry<String> registry = new ForgeRegistry<>("test", String.class);
            
            registry.register("minecraft:diamond", "Diamond");
            assertFalse(registry.isFrozen(), "凍結前 - 未凍結");
            
            registry.freeze();
            assertTrue(registry.isFrozen(), "凍結後 - 已凍結");
            
            // 凍結後註冊應該丟出例外
            try {
                registry.register("minecraft:iron", "Iron");
                failed++;
                System.out.println("✗ testFreeze - 凍結後註冊應該丟出例外");
                return;
            } catch (IllegalStateException e) {
                // 預期的例外
            }
            
            passed++;
            System.out.println("✓ testFreeze");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testFreeze: " + e.getMessage());
        }
    }
    
    private static void testGetKey() {
        try {
            ForgeRegistry<String> registry = new ForgeRegistry<>("test", String.class);
            
            String value = "Diamond";
            registry.register("minecraft:diamond", value);
            
            assertEqual("minecraft:diamond", registry.getKey(value), "取得鍵 - 正確");
            
            passed++;
            System.out.println("✓ testGetKey");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testGetKey: " + e.getMessage());
        }
    }
    
    private static void testContains() {
        try {
            ForgeRegistry<String> registry = new ForgeRegistry<>("test", String.class);
            
            registry.register("minecraft:diamond", "Diamond");
            
            assertTrue(registry.containsKey("minecraft:diamond"), "包含鍵 - 存在");
            assertFalse(registry.containsKey("minecraft:iron"), "包含鍵 - 不存在");
            
            assertTrue(registry.containsValue("Diamond"), "包含值 - 存在");
            assertFalse(registry.containsValue("Iron"), "包含值 - 不存在");
            
            passed++;
            System.out.println("✓ testContains");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testContains: " + e.getMessage());
        }
    }
    
    private static void testParameterValidation() {
        try {
            ForgeRegistry<String> registry = new ForgeRegistry<>("test", String.class);
            
            try {
                registry.register(null, "Diamond");
                failed++;
                System.out.println("✗ testParameterValidation - null key 應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            try {
                registry.register("", "Diamond");
                failed++;
                System.out.println("✗ testParameterValidation - 空 key 應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            try {
                registry.register("minecraft:diamond", null);
                failed++;
                System.out.println("✗ testParameterValidation - null value 應該丟出例外");
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
