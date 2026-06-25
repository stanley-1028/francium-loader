package com.francium.forge;

import com.francium.forge.adapter.ForgeModMetadata;
import com.francium.forge.adapter.ForgeModDetector;
import com.francium.forge.lifecycle.*;
import com.francium.forge.registry.*;
import com.francium.forge.event.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Forge 適配層演示程式
 * 
 * 用來演示和測試 Forge 適配層的基本功能
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeAdapterDemo {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("  Francium Forge Adapter Demo");
        System.out.println("  版本: " + ForgeAdapter.VERSION);
        System.out.println("=".repeat(60));
        System.out.println();
        
        // 測試 1: Forge 適配器初始化
        System.out.println("【測試 1】Forge 適配器初始化");
        System.out.println("-".repeat(40));
        testAdapterInitialization();
        System.out.println();
        
        // 測試 2: 生命週期系統
        System.out.println("【測試 2】FML 生命週期系統");
        System.out.println("-".repeat(40));
        testLifecycle();
        System.out.println();
        
        // 測試 3: 註冊系統
        System.out.println("【測試 3】註冊系統");
        System.out.println("-".repeat(40));
        testRegistry();
        System.out.println();
        
        // 測試 4: 事件系統
        System.out.println("【測試 4】事件系統");
        System.out.println("-".repeat(40));
        testEventBus();
        System.out.println();
        
        // 測試 5: 延遲註冊
        System.out.println("【測試 5】延遲註冊 (DeferredRegister)");
        System.out.println("-".repeat(40));
        testDeferredRegister();
        System.out.println();
        
        // 測試 6: 模組中繼資料
        System.out.println("【測試 6】模組中繼資料");
        System.out.println("-".repeat(40));
        testModMetadata();
        System.out.println();
        
        System.out.println("=".repeat(60));
        System.out.println("  所有測試完成！");
        System.out.println("=".repeat(60));
    }
    
    private static void testAdapterInitialization() {
        ForgeAdapter adapter = new ForgeAdapter();
        adapter.initialize();
        
        System.out.println("✅ 適配器初始化成功");
        System.out.println("   版本: " + adapter.getVersion());
        System.out.println("   已初始化: " + adapter.isInitialized());
        System.out.println("   當前階段: " + adapter.getCurrentStage());
        System.out.println("   模組數量: " + adapter.getModCount());
    }
    
    private static void testLifecycle() {
        FMLLifecycleManager manager = new FMLLifecycleManager();
        
        // 建立測試模組
        ForgeModMetadata metadata = new ForgeModMetadata();
        metadata.setModId("testmod");
        metadata.setName("Test Mod");
        metadata.setVersion("1.0.0");
        metadata.setDescription("A test mod for Forge adapter");
        metadata.setMcVersion("1.20.4");
        
        ForgeModContainer container = manager.registerMod(metadata);
        System.out.println("✅ 模組註冊成功: " + container.getModId());
        
        // 添加生命週期監聽器
        final int[] stageCount = {0};
        manager.addLifecycleListener(new FMLLifecycleManager.LifecycleListener() {
            @Override
            public void onStageChange(FMLLifecycle previous, FMLLifecycle current) {
                stageCount[0]++;
                System.out.println("   階段變更: " + previous + " → " + current);
            }
            
            @Override
            public void onModLoaded(String modId, FMLLifecycle stage) {
                // 模組載入事件
            }
            
            @Override
            public void onError(String modId, FMLLifecycle stage, Throwable error) {
                System.err.println("   錯誤: " + modId + " @ " + stage);
            }
        });
        
        // 開始載入
        manager.startLoading();
        System.out.println("✅ 生命週期載入完成");
        System.out.println("   當前階段: " + manager.getCurrentStage());
        System.out.println("   階段變更次數: " + stageCount[0]);
        System.out.println("   載入完成: " + manager.isLoadingComplete());
        System.out.println("   模組容器當前階段: " + container.getCurrentStage());
    }
    
    private static void testRegistry() {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        
        // 建立測試註冊表
        ForgeRegistry<String> testRegistry = manager.createRegistry("test", String.class);
        System.out.println("✅ 註冊表建立: " + testRegistry.getName());
        
        // 註冊一些項目
        testRegistry.register("test:item1", "Item 1");
        testRegistry.register("test:item2", "Item 2");
        testRegistry.register("test:item3", "Item 3");
        System.out.println("✅ 註冊 3 個項目");
        System.out.println("   註冊表大小: " + testRegistry.size());
        
        // 查詢項目
        String item = testRegistry.getValue("test:item2");
        System.out.println("   查詢 test:item2 = " + item);
        
        // 檢查是否存在
        System.out.println("   包含 test:item1: " + testRegistry.containsKey("test:item1"));
        System.out.println("   包含 test:nonexist: " + testRegistry.containsKey("test:nonexist"));
        
        // 凍結註冊表
        testRegistry.freeze();
        System.out.println("✅ 註冊表已凍結");
        System.out.println("   已凍結: " + testRegistry.isFrozen());
        
        // 測試管理器
        System.out.println("   註冊表總數: " + manager.getRegistryCount());
        System.out.println("   註冊項目總數: " + manager.getTotalEntryCount());
    }
    
    private static void testEventBus() {
        FMLEventBus eventBus = new FMLEventBus("TestBus");
        
        // 註冊事件處理器
        final int[] eventCount = {0};
        eventBus.addListener(FMLEvent.class, event -> {
            eventCount[0]++;
            System.out.println("   收到事件: " + event.getClass().getSimpleName());
        });
        
        // 發布一個測試事件
        FMLEvent testEvent = new FMLEvent(true) {}; // 匿名內部類，可取消
        eventBus.post(testEvent);
        System.out.println("✅ 事件發布成功");
        System.out.println("   事件處理次數: " + eventCount[0]);
        
        // 測試可取消事件
        FMLEvent cancelableEvent = new FMLEvent(true) {};
        eventBus.addListener(FMLEvent.class, event -> {
            if (event.isCancelable()) {
                event.setCanceled(true);
                System.out.println("   事件已取消");
            }
        });
        eventBus.post(cancelableEvent);
        System.out.println("   事件已取消: " + cancelableEvent.isCanceled());
    }
    
    private static void testDeferredRegister() {
        // 建立延遲註冊器
        DeferredRegister<String> deferred = DeferredRegister.create("test_deferred", "mymod", String.class);
        
        // 註冊一些項目
        deferred.register("hello", () -> "Hello World");
        deferred.register("foo", () -> "Foo Bar");
        deferred.register("baz", () -> "Baz Qux");
        System.out.println("✅ 延遲註冊 " + deferred.getEntryCount() + " 個項目");
        
        // 執行註冊
        deferred.registerAll();
        System.out.println("✅ 全部註冊完成");
        System.out.println("   是否已註冊: " + deferred.isRegistered());
        
        // 從註冊表查詢
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<String> registry = manager.getRegistry("test_deferred");
        if (registry != null) {
            System.out.println("   註冊表大小: " + registry.size());
            System.out.println("   查詢 mymod:hello = " + registry.getValue("mymod:hello"));
        }
    }
    
    private static void testModMetadata() {
        // 測試建構器
        ForgeModMetadata metadata = new ForgeModMetadata();
        metadata.setModId("examplemod");
        metadata.setName("Example Mod");
        metadata.setVersion("2.0.0");
        metadata.setDescription("An example mod for testing");
        metadata.setAuthors(new String[]{"Example Author"});
        metadata.setMcVersion("1.20.4");
        metadata.setForgeVersion("47.1.0");
        metadata.setModClass("com.example.mod.ExampleMod");
        metadata.setFormatVersion(1);
        
        System.out.println("✅ 模組中繼資料建立成功");
        System.out.println("   模組 ID: " + metadata.getModId());
        System.out.println("   名稱: " + metadata.getName());
        System.out.println("   版本: " + metadata.getVersion());
        System.out.println("   MC 版本: " + metadata.getMcVersion());
        System.out.println("   有效: " + metadata.isValid());
        
        // 測試依賴
        ForgeModMetadata.Dependency dep1 = new ForgeModMetadata.Dependency();
        dep1.setModId("forge");
        dep1.setMandatory(true);
        dep1.setVersionRange("[47.1.0,)");
        dep1.setSide("both");
        
        ForgeModMetadata.Dependency dep2 = new ForgeModMetadata.Dependency();
        dep2.setModId("minecraft");
        dep2.setMandatory(true);
        dep2.setVersionRange("1.20.4");
        dep2.setSide("both");
        
        metadata.setDependencies(new ForgeModMetadata.Dependency[]{dep1, dep2});
        System.out.println("   依賴數量: " + metadata.getDependencies().length);
    }
}
