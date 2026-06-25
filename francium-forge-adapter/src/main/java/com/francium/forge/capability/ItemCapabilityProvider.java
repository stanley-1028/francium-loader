package com.francium.forge.capability;

import com.francium.forge.item.IItemHandler;
import com.francium.forge.item.ItemStackHandler;

/**
 * 物品能力提供者
 * 
 * 將物品處理器包裝為能力
 * 對應 Forge 的 ItemHandlerProvider
 * 
 * 這是最常用的能力之一
 * 用於箱子、機器、背包等物品容器
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ItemCapabilityProvider implements ICapabilityProvider {
    
    /** 能力名稱 */
    public static final String CAPABILITY_NAME = "item_handler";
    
    /** 物品處理器 */
    private final IItemHandler itemHandler;
    
    /**
     * 建立物品能力提供者
     * 
     * @param itemHandler 物品處理器
     * @throws IllegalArgumentException 如果 itemHandler 為 null
     */
    public ItemCapabilityProvider(IItemHandler itemHandler) {
        if (itemHandler == null) {
            throw new IllegalArgumentException("Item handler cannot be null");
        }
        this.itemHandler = itemHandler;
    }
    
    /**
     * 建立物品能力提供者（指定槽位數量）
     * 
     * @param slots 槽位數量
     * @throws IllegalArgumentException 如果 slots <= 0
     */
    public ItemCapabilityProvider(int slots) {
        this.itemHandler = new ItemStackHandler(slots);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, Object context) {
        if (capability != null && CAPABILITY_NAME.equals(capability.getName())) {
            return (T) itemHandler;
        }
        return null;
    }
    
    @Override
    public boolean hasCapability(Capability<?> capability, Object context) {
        return capability != null && CAPABILITY_NAME.equals(capability.getName());
    }
    
    /**
     * 取得物品處理器
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
    
    /**
     * 建立物品能力
     * 
     * @return 物品能力
     */
    public static Capability<IItemHandler> createItemCapability() {
        return new Capability<>(
            CAPABILITY_NAME,
            IItemHandler.class,
            () -> new ItemStackHandler(1)
        );
    }
    
    @Override
    public String toString() {
        return "ItemCapabilityProvider[slots=" + itemHandler.getSlots() + "]";
    }
}
