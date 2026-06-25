package com.francium.forge.capability;

import com.francium.forge.energy.EnergyStorage;
import com.francium.forge.energy.IEnergyStorage;

/**
 * 能量能力提供者
 * 
 * 將能量儲存包裝為能力
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class EnergyCapabilityProvider implements ICapabilityProvider {
    
    /** 能量能力名稱 */
    public static final String CAPABILITY_NAME = "francium:energy";
    
    /** 能量儲存 */
    private final EnergyStorage energyStorage;
    
    /**
     * 建立能量能力提供者
     * 
     * @param capacity 容量
     */
    public EnergyCapabilityProvider(int capacity) {
        this.energyStorage = new EnergyStorage(capacity);
    }
    
    /**
     * 建立能量能力提供者
     * 
     * @param capacity 容量
     * @param maxTransfer 最大傳輸速率
     */
    public EnergyCapabilityProvider(int capacity, int maxTransfer) {
        this.energyStorage = new EnergyStorage(capacity, maxTransfer);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, Object context) {
        if (capability != null && CAPABILITY_NAME.equals(capability.getName())) {
            return (T) energyStorage;
        }
        return null;
    }
    
    /**
     * 取得能量儲存
     */
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    /**
     * 建立能量能力
     */
    public static Capability<IEnergyStorage> createEnergyCapability() {
        return new Capability<>(
            CAPABILITY_NAME,
            IEnergyStorage.class,
            () -> new EnergyStorage(1000)
        );
    }
}
