package com.francium.forge.capability;

import com.francium.forge.fluid.IFluidHandler;
import com.francium.forge.fluid.MultiFluidTank;

/**
 * 流體能力提供者
 * 
 * 將流體處理器包裝為能力
 * 對應 Forge 的 FluidHandlerProvider
 * 
 * 用於儲存槽、機器、管道等流體容器
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class FluidCapabilityProvider implements ICapabilityProvider {
    
    /** 能力名稱 */
    public static final String CAPABILITY_NAME = "fluid_handler";
    
    /** 流體處理器 */
    private final IFluidHandler fluidHandler;
    
    /**
     * 建立流體能力提供者
     * 
     * @param fluidHandler 流體處理器
     * @throws IllegalArgumentException 如果 fluidHandler 為 null
     */
    public FluidCapabilityProvider(IFluidHandler fluidHandler) {
        if (fluidHandler == null) {
            throw new IllegalArgumentException("Fluid handler cannot be null");
        }
        this.fluidHandler = fluidHandler;
    }
    
    /**
     * 建立流體能力提供者（指定槽位數量和容量）
     * 
     * @param tanks 槽位數量
     * @param capacity 每個槽位的容量
     * @throws IllegalArgumentException 如果參數無效
     */
    public FluidCapabilityProvider(int tanks, int capacity) {
        if (tanks <= 0) {
            throw new IllegalArgumentException("Tank count must be positive: " + tanks);
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive: " + capacity);
        }
        this.fluidHandler = new MultiFluidTank(tanks, capacity);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, Object context) {
        if (capability != null && CAPABILITY_NAME.equals(capability.getName())) {
            return (T) fluidHandler;
        }
        return null;
    }
    
    @Override
    public boolean hasCapability(Capability<?> capability, Object context) {
        return capability != null && CAPABILITY_NAME.equals(capability.getName());
    }
    
    /**
     * 取得流體處理器
     */
    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }
    
    /**
     * 建立流體能力
     * 
     * @return 流體能力
     */
    public static Capability<IFluidHandler> createFluidCapability() {
        return new Capability<>(
            CAPABILITY_NAME,
            IFluidHandler.class,
            () -> new MultiFluidTank(1, 1000)
        );
    }
    
    @Override
    public String toString() {
        return "FluidCapabilityProvider[tanks=" + fluidHandler.getTanks() + "]";
    }
}
