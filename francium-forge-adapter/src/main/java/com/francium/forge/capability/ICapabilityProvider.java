package com.francium.forge.capability;

/**
 * 能力提供者介面
 * 
 * 對應 Forge 的 ICapabilityProvider
 * 表示可以提供能力的物件（方塊、實體、物品等）
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public interface ICapabilityProvider {
    
    /**
     * 取得指定能力的實例
     * 
     * @param capability 能力類型
     * @param context 上下文（可選）
     * @return 能力實例，如果不支援則傳回 null
     */
    <T> T getCapability(Capability<T> capability, Object context);
    
    /**
     * 檢查是否支援指定能力
     * 
     * @param capability 能力類型
     * @param context 上下文（可選）
     * @return 是否支援
     */
    default boolean hasCapability(Capability<?> capability, Object context) {
        return getCapability(capability, context) != null;
    }
}
