package com.francium.forge.entity;

import java.util.UUID;

/**
 * 實體屬性測試
 * 
 * 測試 Attribute、AttributeModifier、AttributeInstance、AttributeMap 的功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class AttributeTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== Attribute 測試 ===");
        System.out.println();
        
        testAttribute();
        testAttributeModifier();
        testAttributeInstanceBasic();
        testAttributeInstanceAddition();
        testAttributeInstanceMultiplyBase();
        testAttributeInstanceMultiplyTotal();
        testAttributeInstanceMultipleModifiers();
        testAttributeInstanceClamp();
        testAttributeMap();
        testBuiltinAttributes();
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
    
    private static void testAttribute() {
        try {
            Attribute attr = new Attribute("test:health", 20.0, 0.0, 100.0, "測試屬性");
            
            assertEqual("test:health", attr.getId(), "屬性 - ID");
            assertEqual(20.0, attr.getDefaultValue(), "屬性 - 預設值");
            assertEqual(0.0, attr.getMinValue(), "屬性 - 最小值");
            assertEqual(100.0, attr.getMaxValue(), "屬性 - 最大值");
            assertEqual("測試屬性", attr.getDescription(), "屬性 - 描述");
            
            assertEqual(50.0, attr.clampValue(50.0), "屬性 - 鉗位中間值");
            assertEqual(0.0, attr.clampValue(-10.0), "屬性 - 鉗位小於最小值");
            assertEqual(100.0, attr.clampValue(200.0), "屬性 - 鉗位大於最大值");
            
            passed++;
            System.out.println("✓ testAttribute");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testAttribute: " + e.getMessage());
        }
    }
    
    private static void testAttributeModifier() {
        try {
            UUID id = UUID.randomUUID();
            AttributeModifier modifier = new AttributeModifier(id, "test_modifier", 5.0, AttributeModifier.Operation.ADDITION);
            
            assertEqual(id, modifier.getId(), "修飾符 - ID");
            assertEqual("test_modifier", modifier.getName(), "修飾符 - 名稱");
            assertEqual(5.0, modifier.getAmount(), "修飾符 - 值");
            assertEqual(AttributeModifier.Operation.ADDITION, modifier.getOperation(), "修飾符 - 運算方式");
            
            // 測試自動產生 UUID
            AttributeModifier modifier2 = new AttributeModifier("test2", 3.0, AttributeModifier.Operation.MULTIPLY_BASE);
            assertNotNull(modifier2.getId(), "修飾符 - 自動 UUID");
            assertEqual("test2", modifier2.getName(), "修飾符 - 名稱2");
            
            passed++;
            System.out.println("✓ testAttributeModifier");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testAttributeModifier: " + e.getMessage());
        }
    }
    
    private static void testAttributeInstanceBasic() {
        try {
            Attribute attr = new Attribute("test:health", 20.0, 0.0, 100.0);
            AttributeInstance instance = new AttributeInstance(attr);
            
            assertEqual(20.0, instance.getBaseValue(), "屬性實例 - 基礎值");
            assertEqual(20.0, instance.getValue(), "屬性實例 - 計算值（無修飾符）");
            assertEqual(0, instance.getModifierCount(), "屬性實例 - 修飾符數量");
            
            instance.setBaseValue(30.0);
            assertEqual(30.0, instance.getBaseValue(), "屬性實例 - 設定基礎值");
            assertEqual(30.0, instance.getValue(), "屬性實例 - 計算值（新基礎值）");
            
            passed++;
            System.out.println("✓ testAttributeInstanceBasic");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testAttributeInstanceBasic: " + e.getMessage());
        }
    }
    
    private static void testAttributeInstanceAddition() {
        try {
            Attribute attr = new Attribute("test:health", 20.0, 0.0, 100.0);
            AttributeInstance instance = new AttributeInstance(attr);
            
            AttributeModifier modifier = new AttributeModifier("plus_5", 5.0, AttributeModifier.Operation.ADDITION);
            assertTrue(instance.addModifier(modifier), "加法修飾符 - 添加成功");
            
            assertEqual(25.0, instance.getValue(), "加法修飾符 - 計算值");
            assertEqual(1, instance.getModifierCount(), "加法修飾符 - 數量");
            
            // 重複添加應該返回 false
            assertFalse(instance.addModifier(modifier), "加法修飾符 - 重複添加");
            
            passed++;
            System.out.println("✓ testAttributeInstanceAddition");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testAttributeInstanceAddition: " + e.getMessage());
        }
    }
    
    private static void testAttributeInstanceMultiplyBase() {
        try {
            Attribute attr = new Attribute("test:damage", 10.0, 0.0, 100.0);
            AttributeInstance instance = new AttributeInstance(attr);
            
            AttributeModifier modifier = new AttributeModifier("boost", 0.5, AttributeModifier.Operation.MULTIPLY_BASE);
            instance.addModifier(modifier);
            
            // 基礎值 10 + 加法 0 = 10
            // 乘以 (1 + 0.5) = 1.5
            // 結果 = 10 * 1.5 = 15
            assertEqual(15.0, instance.getValue(), "乘法基礎值修飾符 - 計算值");
            
            passed++;
            System.out.println("✓ testAttributeInstanceMultiplyBase");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testAttributeInstanceMultiplyBase: " + e.getMessage());
        }
    }
    
    private static void testAttributeInstanceMultiplyTotal() {
        try {
            Attribute attr = new Attribute("test:damage", 10.0, 0.0, 100.0);
            AttributeInstance instance = new AttributeInstance(attr);
            
            // 先加一個加法修飾符
            instance.addModifier(new AttributeModifier("plus_10", 10.0, AttributeModifier.Operation.ADDITION));
            // 再加一個乘法總值修飾符
            instance.addModifier(new AttributeModifier("double", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            
            // 基礎值 10 + 加法 10 = 20
            // 乘以基礎值 (1 + 0) = 1
            // 乘以總值 (1 + 1) = 2
            // 結果 = 20 * 1 * 2 = 40
            assertEqual(40.0, instance.getValue(), "乘法總值修飾符 - 計算值");
            
            passed++;
            System.out.println("✓ testAttributeInstanceMultiplyTotal");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testAttributeInstanceMultiplyTotal: " + e.getMessage());
        }
    }
    
    private static void testAttributeInstanceMultipleModifiers() {
        try {
            Attribute attr = new Attribute("test:damage", 10.0, 0.0, 100.0);
            AttributeInstance instance = new AttributeInstance(attr);
            
            // 添加多個修飾符
            instance.addModifier(new AttributeModifier("plus_5", 5.0, AttributeModifier.Operation.ADDITION));
            instance.addModifier(new AttributeModifier("plus_3", 3.0, AttributeModifier.Operation.ADDITION));
            instance.addModifier(new AttributeModifier("boost_50", 0.5, AttributeModifier.Operation.MULTIPLY_BASE));
            instance.addModifier(new AttributeModifier("boost_20", 0.2, AttributeModifier.Operation.MULTIPLY_BASE));
            
            // 基礎值 10 + 加法 5+3 = 18
            // 乘以基礎值 (1 + 0.5 + 0.2) = 1.7
            // 結果 = 18 * 1.7 = 30.6
            assertEqual(30.6, instance.getValue(), "多個修飾符 - 計算值");
            assertEqual(4, instance.getModifierCount(), "多個修飾符 - 數量");
            
            // 移除一個修飾符
            UUID modifierId = instance.getModifiers().iterator().next().getId();
            assertTrue(instance.removeModifier(modifierId), "多個修飾符 - 移除成功");
            assertEqual(3, instance.getModifierCount(), "多個修飾符 - 移除後數量");
            
            passed++;
            System.out.println("✓ testAttributeInstanceMultipleModifiers");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testAttributeInstanceMultipleModifiers: " + e.getMessage());
        }
    }
    
    private static void testAttributeInstanceClamp() {
        try {
            Attribute attr = new Attribute("test:health", 20.0, 0.0, 100.0);
            AttributeInstance instance = new AttributeInstance(attr);
            
            // 添加一個很大的加法修飾符，應該被鉗位到最大值
            instance.addModifier(new AttributeModifier("huge", 1000.0, AttributeModifier.Operation.ADDITION));
            assertEqual(100.0, instance.getValue(), "鉗位 - 大於最大值");
            
            // 清除修飾符
            instance.clearModifiers();
            assertEqual(20.0, instance.getValue(), "鉗位 - 清除後");
            
            // 設定基礎值超出範圍，應該被鉗位
            instance.setBaseValue(200.0);
            assertEqual(100.0, instance.getBaseValue(), "鉗位 - 基礎值超出範圍");
            
            instance.setBaseValue(-10.0);
            assertEqual(0.0, instance.getBaseValue(), "鉗位 - 基礎值小於最小值");
            
            passed++;
            System.out.println("✓ testAttributeInstanceClamp");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testAttributeInstanceClamp: " + e.getMessage());
        }
    }
    
    private static void testAttributeMap() {
        try {
            AttributeMap map = new AttributeMap();
            
            assertEqual(0, map.getAttributeCount(), "屬性映射 - 初始數量");
            
            // 註冊屬性
            Attribute health = new Attribute("test:health", 20.0, 0.0, 100.0);
            AttributeInstance instance = map.registerAttribute(health);
            
            assertNotNull(instance, "屬性映射 - 註冊實例不為 null");
            assertEqual(1, map.getAttributeCount(), "屬性映射 - 註冊後數量");
            assertTrue(map.hasAttribute(health), "屬性映射 - 包含屬性");
            
            // 取得已註冊的屬性
            AttributeInstance instance2 = map.getInstance(health);
            assertEqual(instance, instance2, "屬性映射 - 取得相同實例");
            
            // 設定基礎值
            map.setBaseValue(health, 30.0);
            assertEqual(30.0, map.getValue(health), "屬性映射 - 設定值");
            
            // 添加修飾符
            map.addModifier(health, new AttributeModifier("plus_10", 10.0, AttributeModifier.Operation.ADDITION));
            assertEqual(40.0, map.getValue(health), "屬性映射 - 添加修飾符");
            
            passed++;
            System.out.println("✓ testAttributeMap");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testAttributeMap: " + e.getMessage());
        }
    }
    
    private static void testBuiltinAttributes() {
        try {
            Attribute[] attrs = Attributes.values();
            assertTrue(attrs.length >= 10, "內建屬性 - 數量足夠");
            
            // 測試幾個常用屬性
            assertNotNull(Attributes.MAX_HEALTH, "內建屬性 - MAX_HEALTH");
            assertNotNull(Attributes.ATTACK_DAMAGE, "內建屬性 - ATTACK_DAMAGE");
            assertNotNull(Attributes.MOVEMENT_SPEED, "內建屬性 - MOVEMENT_SPEED");
            assertNotNull(Attributes.ARMOR, "內建屬性 - ARMOR");
            
            assertEqual(20.0, Attributes.MAX_HEALTH.getDefaultValue(), "內建屬性 - 生命值預設值");
            assertEqual(2.0, Attributes.ATTACK_DAMAGE.getDefaultValue(), "內建屬性 - 攻擊傷害預設值");
            
            // 測試 byId
            Attribute found = Attributes.byId("minecraft:generic.max_health");
            assertNotNull(found, "內建屬性 - 根據 ID 查詢");
            assertEqual(Attributes.MAX_HEALTH, found, "內建屬性 - 查詢結果正確");
            
            passed++;
            System.out.println("✓ testBuiltinAttributes");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBuiltinAttributes: " + e.getMessage());
        }
    }
    
    private static void testParameterValidation() {
        try {
            // 測試 null 屬性 ID
            try {
                new Attribute(null, 20.0, 0.0, 100.0);
                failed++;
                System.out.println("✗ testParameterValidation - null ID 應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試 min > max
            try {
                new Attribute("test", 20.0, 100.0, 0.0);
                failed++;
                System.out.println("✗ testParameterValidation - min > max 應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試預設值超出範圍
            try {
                new Attribute("test", 200.0, 0.0, 100.0);
                failed++;
                System.out.println("✗ testParameterValidation - 預設值超出範圍應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試 null 修飾符 ID
            try {
                new AttributeModifier(null, "test", 5.0, AttributeModifier.Operation.ADDITION);
                failed++;
                System.out.println("✗ testParameterValidation - null 修飾符 ID 應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試 null 屬性實例
            try {
                new AttributeInstance(null);
                failed++;
                System.out.println("✗ testParameterValidation - null 屬性實例應該丟出例外");
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
    
    private static void assertEqual(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.0001) {
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
