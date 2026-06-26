package com.francium.forge.effect;

/**
 * 藥水效果測試
 * 
 * 測試 MobEffect、MobEffectInstance、MobEffects 的功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class MobEffectTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== MobEffect 測試 ===");
        System.out.println();
        
        testMobEffectBasic();
        testMobEffectInstance();
        testBuiltinEffects();
        testEffectDuration();
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
    
    private static void testMobEffectBasic() {
        try {
            MobEffect effect = new MobEffect("test:speed", "速度", true, 0x7CAFC6);
            
            assertEqual("test:speed", effect.getId(), "效果 - ID");
            assertEqual("速度", effect.getName(), "效果 - 名稱");
            assertTrue(effect.isBeneficial(), "效果 - 正面效果");
            assertEqual(0x7CAFC6, effect.getColor(), "效果 - 顏色");
            assertFalse(effect.isInstant(), "效果 - 非瞬間");
            
            // 測試瞬間效果
            MobEffect instantEffect = new MobEffect(
                "test:instant_health", "瞬間治療", "立即恢復生命", 
                true, 0xF82423, true
            );
            assertTrue(instantEffect.isInstant(), "效果 - 瞬間");
            
            passed++;
            System.out.println("✓ testMobEffectBasic");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testMobEffectBasic: " + e.getMessage());
        }
    }
    
    private static void testMobEffectInstance() {
        try {
            MobEffect speed = MobEffects.SPEED;
            MobEffectInstance instance = new MobEffectInstance(speed, 200);
            
            assertEqual(speed, instance.getEffect(), "效果實例 - 效果類型");
            assertEqual("minecraft:speed", instance.getEffectId(), "效果實例 - 效果 ID");
            assertEqual("速度", instance.getEffectName(), "效果實例 - 效果名稱");
            assertEqual(200, instance.getDuration(), "效果實例 - 持續時間");
            assertEqual(0, instance.getAmplifier(), "效果實例 - 等級");
            assertFalse(instance.isFinished(), "效果實例 - 未結束");
            
            // 測試帶等級的效果
            MobEffectInstance instance2 = new MobEffectInstance(speed, 400, 1);
            assertEqual(400, instance2.getDuration(), "效果實例 - 持續時間2");
            assertEqual(1, instance2.getAmplifier(), "效果實例 - 等級2");
            
            // 測試減少持續時間
            instance.decreaseDuration(50);
            assertEqual(150, instance.getDuration(), "效果實例 - 減少持續時間");
            
            // 測試效果結束
            instance.decreaseDuration(200);
            assertEqual(0, instance.getDuration(), "效果實例 - 持續時間歸零");
            assertTrue(instance.isFinished(), "效果實例 - 已結束");
            
            passed++;
            System.out.println("✓ testMobEffectInstance");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testMobEffectInstance: " + e.getMessage());
        }
    }
    
    private static void testBuiltinEffects() {
        try {
            MobEffect[] effects = MobEffects.values();
            assertTrue(effects.length >= 20, "內建效果 - 數量足夠");
            
            // 測試幾個常用效果
            assertNotNull(MobEffects.SPEED, "內建效果 - SPEED");
            assertNotNull(MobEffects.STRENGTH, "內建效果 - STRENGTH");
            assertNotNull(MobEffects.REGENERATION, "內建效果 - REGENERATION");
            assertNotNull(MobEffects.POISON, "內建效果 - POISON");
            assertNotNull(MobEffects.INSTANT_HEALTH, "內建效果 - INSTANT_HEALTH");
            
            // 測試屬性
            assertTrue(MobEffects.SPEED.isBeneficial(), "內建效果 - 速度是正面");
            assertFalse(MobEffects.POISON.isBeneficial(), "內建效果 - 中毒是負面");
            assertTrue(MobEffects.INSTANT_HEALTH.isInstant(), "內建效果 - 瞬間治療是瞬間");
            assertFalse(MobEffects.SPEED.isInstant(), "內建效果 - 速度不是瞬間");
            
            // 測試 byId
            MobEffect found = MobEffects.byId("minecraft:speed");
            assertNotNull(found, "內建效果 - 根據 ID 查詢");
            assertEqual(MobEffects.SPEED, found, "內建效果 - 查詢結果正確");
            
            passed++;
            System.out.println("✓ testBuiltinEffects");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBuiltinEffects: " + e.getMessage());
        }
    }
    
    private static void testEffectDuration() {
        try {
            MobEffect speed = MobEffects.SPEED;
            MobEffectInstance instance = new MobEffectInstance(speed, 100);
            
            // 測試設定持續時間
            instance.setDuration(200);
            assertEqual(200, instance.getDuration(), "持續時間 - 設定");
            
            // 測試設定負數持續時間（應該丟出例外）
            try {
                instance.setDuration(-10);
                failed++;
                System.out.println("✗ testEffectDuration - 設定負數持續時間應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試減少超過目前持續時間
            instance.decreaseDuration(300);
            assertEqual(0, instance.getDuration(), "持續時間 - 減少超過後歸零");
            assertTrue(instance.isFinished(), "持續時間 - 已結束");
            
            // 測試相同效果合併
            MobEffectInstance instance1 = new MobEffectInstance(speed, 100);
            MobEffectInstance instance2 = new MobEffectInstance(speed, 200);
            assertTrue(instance1.combine(instance2), "持續時間 - 合併成功");
            assertEqual(200, instance1.getDuration(), "持續時間 - 合併後取較長時間");
            
            passed++;
            System.out.println("✓ testEffectDuration");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testEffectDuration: " + e.getMessage());
        }
    }
    
    private static void testParameterValidation() {
        try {
            // 測試 null 效果 ID
            try {
                new MobEffect(null, "test", true, 0xFFFFFF);
                failed++;
                System.out.println("✗ testParameterValidation - null ID 應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試空效果名稱
            try {
                new MobEffect("test:id", "", true, 0xFFFFFF);
                failed++;
                System.out.println("✗ testParameterValidation - 空名稱應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試 null 效果實例
            try {
                new MobEffectInstance(null, 100);
                failed++;
                System.out.println("✗ testParameterValidation - null 效果應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試負數持續時間
            try {
                new MobEffectInstance(MobEffects.SPEED, -10);
                failed++;
                System.out.println("✗ testParameterValidation - 負數持續時間應該丟出例外");
                return;
            } catch (IllegalArgumentException e) {
                // 預期的例外
            }
            
            // 測試負數等級
            try {
                new MobEffectInstance(MobEffects.SPEED, 100, -1);
                failed++;
                System.out.println("✗ testParameterValidation - 負數等級應該丟出例外");
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
