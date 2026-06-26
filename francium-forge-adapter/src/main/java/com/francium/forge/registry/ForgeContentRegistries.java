package com.francium.forge.registry;

import com.francium.forge.effect.MobEffect;
import com.francium.forge.effect.MobEffects;
import com.francium.forge.entity.Attribute;
import com.francium.forge.entity.Attributes;
import com.francium.forge.item.enchantment.Enchantment;
import com.francium.forge.item.enchantment.Enchantments;

/**
 * Forge 內容註冊輔助類別
 * 
 * 將所有內容 API（附魔、藥水效果、屬性等）與註冊系統整合
 * 提供便捷的註冊和查詢方法
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeContentRegistries {
    
    private ForgeContentRegistries() {}
    
    /**
     * 註冊所有內建內容
     * 
     * 將所有內建的附魔、藥水效果、屬性等註冊到對應的註冊表中
     */
    public static void registerBuiltinContent() {
        registerBuiltinEnchantments();
        registerBuiltinEffects();
        registerBuiltinAttributes();
    }
    
    /**
     * 註冊所有內建附魔
     */
    public static void registerBuiltinEnchantments() {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<Enchantment> registry = manager.getOrCreateRegistry(
            ForgeRegistryManager.ENCHANTMENTS, Enchantment.class
        );
        
        for (Enchantment enchantment : Enchantments.values()) {
            if (!registry.containsKey(enchantment.getId())) {
                registry.register(enchantment.getId(), enchantment);
            }
        }
    }
    
    /**
     * 註冊所有內建藥水效果
     */
    public static void registerBuiltinEffects() {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<MobEffect> registry = manager.getOrCreateRegistry(
            ForgeRegistryManager.MOB_EFFECTS, MobEffect.class
        );
        
        for (MobEffect effect : MobEffects.values()) {
            if (!registry.containsKey(effect.getId())) {
                registry.register(effect.getId(), effect);
            }
        }
    }
    
    /**
     * 註冊所有內建屬性
     */
    public static void registerBuiltinAttributes() {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<Attribute> registry = manager.getOrCreateRegistry(
            ForgeRegistryManager.ATTRIBUTES, Attribute.class
        );
        
        for (Attribute attribute : Attributes.values()) {
            if (!registry.containsKey(attribute.getId())) {
                registry.register(attribute.getId(), attribute);
            }
        }
    }
    
    /**
     * 根據 ID 取得附魔
     * 
     * @param id 附魔 ID
     * @return 附魔，如果找不到則返回 null
     */
    public static Enchantment getEnchantment(String id) {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<Enchantment> registry = manager.getRegistry(ForgeRegistryManager.ENCHANTMENTS);
        if (registry == null) {
            return null;
        }
        return registry.getValue(id);
    }
    
    /**
     * 根據 ID 取得藥水效果
     * 
     * @param id 效果 ID
     * @return 藥水效果，如果找不到則返回 null
     */
    public static MobEffect getEffect(String id) {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<MobEffect> registry = manager.getRegistry(ForgeRegistryManager.MOB_EFFECTS);
        if (registry == null) {
            return null;
        }
        return registry.getValue(id);
    }
    
    /**
     * 根據 ID 取得屬性
     * 
     * @param id 屬性 ID
     * @return 屬性，如果找不到則返回 null
     */
    public static Attribute getAttribute(String id) {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<Attribute> registry = manager.getRegistry(ForgeRegistryManager.ATTRIBUTES);
        if (registry == null) {
            return null;
        }
        return registry.getValue(id);
    }
    
    /**
     * 取得所有已註冊的附魔數量
     */
    public static int getEnchantmentCount() {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<Enchantment> registry = manager.getRegistry(ForgeRegistryManager.ENCHANTMENTS);
        if (registry == null) {
            return 0;
        }
        return registry.size();
    }
    
    /**
     * 取得所有已註冊的藥水效果數量
     */
    public static int getEffectCount() {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<MobEffect> registry = manager.getRegistry(ForgeRegistryManager.MOB_EFFECTS);
        if (registry == null) {
            return 0;
        }
        return registry.size();
    }
    
    /**
     * 取得所有已註冊的屬性數量
     */
    public static int getAttributeCount() {
        ForgeRegistryManager manager = ForgeRegistryManager.getInstance();
        ForgeRegistry<Attribute> registry = manager.getRegistry(ForgeRegistryManager.ATTRIBUTES);
        if (registry == null) {
            return 0;
        }
        return registry.size();
    }
}
