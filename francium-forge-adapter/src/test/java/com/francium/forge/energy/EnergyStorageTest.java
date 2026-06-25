package com.francium.forge.energy;

/**
 * 能量儲存測試
 * 
 * 測試 EnergyStorage 的各種功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class EnergyStorageTest {
    
    private static int passed = 0;
    private static int failed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== EnergyStorage 測試 ===");
        System.out.println();
        
        testBasicCreation();
        testReceiveEnergy();
        testExtractEnergy();
        testBoundaryConditions();
        testCanReceiveAndExtract();
        testFillPercentage();
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
            EnergyStorage storage = new EnergyStorage(1000);
            assertEqual(1000, storage.getMaxEnergyStored(), "基本建立 - 最大容量");
            assertEqual(0, storage.getEnergyStored(), "基本建立 - 初始能量為0");
            assertTrue(storage.isEmpty(), "基本建立 - 初始為空");
            assertFalse(storage.isFull(), "基本建立 - 初始未滿");
            passed++;
            System.out.println("✓ testBasicCreation");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBasicCreation: " + e.getMessage());
        }
    }
    
    private static void testReceiveEnergy() {
        try {
            EnergyStorage storage = new EnergyStorage(1000);
            
            int received = storage.receiveEnergy(500, false);
            assertEqual(500, received, "接收能量 - 接收500");
            assertEqual(500, storage.getEnergyStored(), "接收能量 - 當前能量500");
            
            // 再接收一些，測試邊界
            int received2 = storage.receiveEnergy(600, false);
            assertEqual(500, received2, "接收能量 - 只能再接收500");
            assertEqual(1000, storage.getEnergyStored(), "接收能量 - 已滿");
            assertTrue(storage.isFull(), "接收能量 - isFull");
            
            passed++;
            System.out.println("✓ testReceiveEnergy");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testReceiveEnergy: " + e.getMessage());
        }
    }
    
    private static void testExtractEnergy() {
        try {
            EnergyStorage storage = new EnergyStorage(1000);
            storage.receiveEnergy(800, false);
            
            int extracted = storage.extractEnergy(300, false);
            assertEqual(300, extracted, "提取能量 - 提取300");
            assertEqual(500, storage.getEnergyStored(), "提取能量 - 剩餘500");
            
            // 提取超過數量
            int extracted2 = storage.extractEnergy(1000, false);
            assertEqual(500, extracted2, "提取能量 - 提取剩餘全部");
            assertEqual(0, storage.getEnergyStored(), "提取能量 - 已空");
            assertTrue(storage.isEmpty(), "提取能量 - isEmpty");
            
            passed++;
            System.out.println("✓ testExtractEnergy");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testExtractEnergy: " + e.getMessage());
        }
    }
    
    private static void testBoundaryConditions() {
        try {
            EnergyStorage storage = new EnergyStorage(1000, 100, 50);
            
            // 測試最大接收
            storage.receiveEnergy(1000, false);
            assertEqual(100, storage.getEnergyStored(), "最大接收 - 每次最多100");
            
            // 測試最大提取
            int extracted = storage.extractEnergy(1000, false);
            assertEqual(50, extracted, "最大提取 - 每次最多50");
            
            passed++;
            System.out.println("✓ testBoundaryConditions");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testBoundaryConditions: " + e.getMessage());
        }
    }
    
    private static void testCanReceiveAndExtract() {
        try {
            EnergyStorage storage = new EnergyStorage(1000);
            assertTrue(storage.canReceive(), "可接收 - 允許接收");
            assertTrue(storage.canExtract(), "可提取 - 允許提取");
            
            // 測試不允許接收的情況
            EnergyStorage noReceive = new EnergyStorage(1000, 0, 100);
            assertFalse(noReceive.canReceive(), "不可接收 - 不允許接收");
            assertTrue(noReceive.canExtract(), "可提取 - 允許提取");
            
            // 測試不允許提取的情況
            EnergyStorage noExtract = new EnergyStorage(1000, 100, 0);
            assertTrue(noExtract.canReceive(), "可接收 - 允許接收");
            assertFalse(noExtract.canExtract(), "不可提取 - 不允許提取");
            
            passed++;
            System.out.println("✓ testCanReceiveAndExtract");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testCanReceiveAndExtract: " + e.getMessage());
        }
    }
    
    private static void testFillPercentage() {
        try {
            EnergyStorage storage = new EnergyStorage(1000);
            
            assertEqual(0.0, storage.getFillPercentage(), 0.001, "填充率 - 0%");
            
            storage.receiveEnergy(500, false);
            assertEqual(0.5, storage.getFillPercentage(), 0.001, "填充率 - 50%");
            
            storage.receiveEnergy(500, false);
            assertEqual(1.0, storage.getFillPercentage(), 0.001, "填充率 - 100%");
            
            passed++;
            System.out.println("✓ testFillPercentage");
        } catch (Exception e) {
            failed++;
            System.out.println("✗ testFillPercentage: " + e.getMessage());
        }
    }
    
    private static void testParameterValidation() {
        try {
            new EnergyStorage(-1);
            failed++;
            System.out.println("✗ testParameterValidation - 負容量應該丟出例外");
            return;
        } catch (IllegalArgumentException e) {
            // 預期的例外
        }
        
        // 負的 maxReceive 會被轉換為 0，這是正常行為
        EnergyStorage storage = new EnergyStorage(1000, -1, 100);
        assertEqual(0, storage.getMaxReceive(), "負最大接收 - 轉換為0");
        
        passed++;
        System.out.println("✓ testParameterValidation");
    }
    
    private static void assertEqual(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": 預期 " + expected + " 但得到 " + actual);
        }
    }
    
    private static void assertEqual(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) {
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
