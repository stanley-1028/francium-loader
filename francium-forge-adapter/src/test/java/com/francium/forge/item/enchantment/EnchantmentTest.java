package com.francium.forge.item.enchantment;

/**
 * 附魔系統測試
 * 
 * 測試 Enchantment、EnchantmentInstance、Enchantments 的功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class EnchantmentTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== Enchantment 測試 ===");
        System.out.println();
        
        testEnchantmentBasic();
        testEnchantmentCategory();
        testEnchantmentInstance();
        testBuiltinEnchantments();
        testEnchantmentCompatibility();
        testEnchantmentCost();
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
    
    private static void testEnchantmentBasic() {
        try {
            Enchantment enchantment = new Enchantment(
                "test:sharpness", "銳利", Enchantment.Rarity.COMMON,
                EnchantmentCategory.SWORD, 5
            );
            
            assertEqual("test:sharpness", enchantment.getId(), "附魔 - ID");
            assertEqual("銳利", enchantment.getName(), "附魔 - 名稱");
            assertEqual(Enchantment.Rarity.COMMON, enchantment.getRarity(), "附魔 - 稀有度");
            assertEqual(1, enchantment.getMinLevel(), "附魔 - 最小等級");
            assertEqual(5, enchantment.getMaxLevel(), "附魔 - 最大等級");
            
            assertFalse(enchantment.isCurse(), "附魔 - 不是詛咒");
            assertFalse(enchantment.isTreasure(), "附魔 - 不是寶藏");
            assertTrue(enchantment.isTradeable(), "附魔 - 可交易");
            assertTrue(enchantment.isDiscoverable(), "附魔 - 可發現");
            
            passed++;
            System.out.println("✓ testEnchantmentBasic");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEnchantmentBasic: " + e.getMessage());
        }
    }
    
    private static void testEnchantmentCategory() {
        try {
            EnchantmentCategory armor = EnchantmentCategory.ARMOR;
            EnchantmentCategory helmet = EnchantmentCategory.HELMET;
            EnchantmentCategory sword = EnchantmentCategory.SWORD;
            
            assertTrue(armor.includes(helmet), "類別 - ARMOR 包含 HELMET");
            assertTrue(armor.includes(EnchantmentCategory.CHESTPLATE), "類別 - ARMOR 包含 CHESTPLATE");
            assertTrue(armor.includes(EnchantmentCategory.LEGGINGS), "類別 - ARMOR 包含 LEGGINGS");
            assertTrue(armor.includes(EnchantmentCategory.BOOTS), "類別 - ARMOR 包含 BOOTS");
            assertFalse(armor.includes(sword), "類別 - ARMOR 不包含 SWORD");
            
            assertTrue(EnchantmentCategory.WEARABLE.includes(armor), "類別 - WEARABLE 包含 ARMOR");
            
            passed++;
            System.out.println("✓ testEnchantmentCategory");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEnchantmentCategory: " + e.getMessage());
        }
    }
    
    private static void testEnchantmentInstance() {
        try {
            Enchantment sharpness = Enchantments.SHARPNESS;
            EnchantmentInstance instance = new EnchantmentInstance(sharpness, 3);
            
            assertEqual(sharpness, instance.getEnchantment(), "附魔實例 - 附魔類型");
            assertEqual(3, instance.getLevel(), "附魔實例 - 等級");
            assertEqual("minecraft:sharpness", instance.getEnchantmentId(), "附魔實例 - 附魔 ID");
            assertEqual("鋒利", instance.getEnchantmentName(), "附魔實例 - 附魔名稱");
            
            // 測試不同等級的實例
            EnchantmentInstance instance2 = new EnchantmentInstance(sharpness, 5);
            assertEqual(5, instance2.getLevel(), "附魔實例 - 最高等級");
            
            // 測試相等性
            EnchantmentInstance instance3 = new EnchantmentInstance(sharpness, 3);
            assertTrue(instance.equals(instance3), "附魔實例 - 相等性");
            assertFalse(instance.equals(instance2), "附魔實例 - 不相等");
            
            passed++;
            System.out.println("✓ testEnchantmentInstance");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEnchantmentInstance: " + e.getMessage());
        }
    }
    
    private static void testBuiltinEnchantments() {
        try {
            Enchantment[] enchantments = Enchantments.values();
            assertTrue(enchantments.length >= 20, "內建附魔 - 數量足夠");
            
            // 測試幾個常用附魔
            assertNotNull(Enchantments.SHARPNESS, "內建附魔 - SHARPNESS");
            assertNotNull(Enchantments.EFFICIENCY, "內建附魔 - EFFICIENCY");
            assertNotNull(Enchantments.PROTECTION, "內建附魔 - PROTECTION");
            assertNotNull(Enchantments.UNBREAKING, "內建附魔 - UNBREAKING");
            assertNotNull(Enchantments.MENDING, "內建附魔 - MENDING");
            
            // 測試屬性
            assertEqual(5, Enchantments.SHARPNESS.getMaxLevel(), "內建附魔 - 鋒利最大等級");
            assertEqual(5, Enchantments.EFFICIENCY.getMaxLevel(), "內建附魔 - 效率最大等級");
            assertEqual(4, Enchantments.PROTECTION.getMaxLevel(), "內建附魔 - 保護最大等級");
            assertEqual(3, Enchantments.UNBREAKING.getMaxLevel(), "內建附魔 - 耐久最大等級");
            
            // 測試寶藏附魔
            assertTrue(Enchantments.MENDING.isTreasure(), "內建附魔 - 經驗修補是寶藏");
            assertTrue(Enchantments.FROST_WALKER.isTreasure(), "內建附魔 - 冰霜行者是寶藏");
            
            // 測試詛咒附魔
            assertTrue(Enchantments.BINDING_CURSE.isCurse(), "內建附魔 - 綁定詛咒是詛咒");
            assertTrue(Enchantments.VANISHING_CURSE.isCurse(), "內建附魔 - 消失詛咒是詛咒");
            
            // 測試 byId
            Enchantment found = Enchantments.byId("minecraft:sharpness");
            assertNotNull(found, "內建附魔 - 根據 ID 查詢");
            assertEqual(Enchantments.SHARPNESS, found, "內建附魔 - 查詢結果正確");
            
            passed++;
            System.out.println("✓ testBuiltinEnchantments");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBuiltinEnchantments: " + e.getMessage());
        }
    }
    
    private static void testEnchantmentCompatibility() {
        try {
            // 大部分附魔預設是相容的
            assertTrue(Enchantments.SHARPNESS.isCompatibleWith(Enchantments.UNBREAKING), 
                "相容性 - 鋒利和耐久相容");
            
            // 相同的附魔不相容
            assertFalse(Enchantments.SHARPNESS.isCompatibleWith(Enchantments.SHARPNESS), 
                "相容性 - 相同附魔不相容");
            
            // null 不相容
            assertFalse(Enchantments.SHARPNESS.isCompatibleWith(null), 
                "相容性 - null 不相容");
            
            // 附魔實例相容性
            EnchantmentInstance sharpness3 = new EnchantmentInstance(Enchantments.SHARPNESS, 3);
            EnchantmentInstance unbreaking2 = new EnchantmentInstance(Enchantments.UNBREAKING, 2);
            assertTrue(sharpness3.isCompatibleWith(unbreaking2), 
                "附魔實例相容性 - 鋒利和耐久相容");
            
            passed++;
            System.out.println("✓ testEnchantmentCompatibility");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEnchantmentCompatibility: " + e.getMessage());
        }
    }
    
    private static void testEnchantmentCost() {
        try {
            Enchantment sharpness = Enchantments.SHARPNESS;
            
            // 測試不同等級的經驗需求
            assertEqual(11, sharpness.getMinCost(1), "經驗需求 - 等級 1 最小");
            assertEqual(16, sharpness.getMaxCost(1), "經驗需求 - 等級 1 最大");
            
            assertEqual(21, sharpness.getMinCost(2), "經驗需求 - 等級 2 最小");
            assertEqual(26, sharpness.getMaxCost(2), "經驗需求 - 等級 2 最大");
            
            assertEqual(51, sharpness.getMinCost(5), "經驗需求 - 等級 5 最小");
            assertEqual(56, sharpness.getMaxCost(5), "經驗需求 - 等級 5 最大");
            
            passed++;
            System.out.println("✓ testEnchantmentCost");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEnchantmentCost: " + e.getMessage());
        }
    }
    
    private static void testParameterValidation() {
        try {
            // 測試 null 附魔 ID
            try {
                new Enchantment(null, "test", Enchantment.Rarity.COMMON, EnchantmentCategory.SWORD, 5);
                failed++;
                System.out.println("✗ testParameterValidation - null ID 應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試空附魔名稱
            try {
                new Enchantment("test:id", "", Enchantment.Rarity.COMMON, EnchantmentCategory.SWORD, 5);
                failed++;
                System.out.println("✗ testParameterValidation - 空名稱應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試 null 稀有度
            try {
                new Enchantment("test:id", "test", null, EnchantmentCategory.SWORD, 5);
                failed++;
                System.out.println("✗ testParameterValidation - null 稀有度應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試最大等級小於最小等級
            try {
                new Enchantment("test:id", "test", "", Enchantment.Rarity.COMMON,
                    java.util.Collections.singleton(EnchantmentCategory.SWORD),
                    3, 1, false, false, true, true);
                failed++;
                System.out.println("✗ testParameterValidation - max < min 應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試附魔實例等級超出範圍
            try {
                new EnchantmentInstance(Enchantments.SHARPNESS, 10);
                failed++;
                System.out.println("✗ testParameterValidation - 等級超出範圍應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試附魔實例等級小於最小值
            try {
                new EnchantmentInstance(Enchantments.SHARPNESS, 0);
                failed++;
                System.out.println("✗ testParameterValidation - 等級小於最小值應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試 null 附魔實例
            try {
                new EnchantmentInstance(null, 1);
                failed++;
                System.out.println("✗ testParameterValidation - null 附魔應該丟出例外");
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
    
    private static void assertEqual(int expected, int actual, String message) {
        if (expected != actual) {
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
