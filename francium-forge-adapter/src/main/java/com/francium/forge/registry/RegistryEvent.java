package com.francium.forge.registry;

import com.francium.forge.event.FMLEvent;

/**
 * 註冊事件
 * 
 * 類似 Forge 的 RegistryEvent，用於在適當的時機觸發物件註冊
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class RegistryEvent extends FMLEvent {
    
    /** 註冊表名稱 */
    private final String registryName;
    
    /** 註冊表 */
    private final ForgeRegistry<?> registry;
    
    public RegistryEvent(String registryName, ForgeRegistry<?> registry) {
        super(false); // 註冊事件不可取消
        this.registryName = registryName;
        this.registry = registry;
    }
    
    /**
     * 取得註冊表名稱
     */
    public String getRegistryName() {
        return registryName;
    }
    
    /**
     * 取得註冊表
     */
    public ForgeRegistry<?> getRegistry() {
        return registry;
    }
    
    /**
     * 註冊事件 - 新註冊表建立
     * 
     * 當新的註冊表被建立時觸發
     */
    public static class NewRegistry extends RegistryEvent {
        public NewRegistry(String registryName, ForgeRegistry<?> registry) {
            super(registryName, registry);
        }
    }
    
    /**
     * 註冊事件 - 註冊階段
     * 
     * 在此事件中應該向註冊表註冊所有物件
     */
    public static class Register extends RegistryEvent {
        public Register(String registryName, ForgeRegistry<?> registry) {
            super(registryName, registry);
        }
    }
    
    /**
     * 註冊事件 - 註冊完成
     * 
     * 當所有物件都已註冊後觸發
     */
    public static class RegisterAll extends FMLEvent {
        public RegisterAll() {
            super(false);
        }
    }
    
    /**
     * 註冊事件 - 註冊表凍結
     * 
     * 當註冊表被凍結時觸發，此後不能再註冊新物件
     */
    public static class Freeze extends RegistryEvent {
        public Freeze(String registryName, ForgeRegistry<?> registry) {
            super(registryName, registry);
        }
    }
}
