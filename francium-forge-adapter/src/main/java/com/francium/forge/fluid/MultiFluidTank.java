package com.francium.forge.fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * 多槽流體儲存
 * 
 * 對應 Forge 的 MultiFluidTank
 * 基本的多槽流體儲存實作
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class MultiFluidTank implements IFluidHandler {
    
    /** 儲存槽列表 */
    private final List<FluidTankInfo> tanks;
    
    /**
     * 建立多槽流體儲存
     * 
     * @param tankCount 槽數量
     * @param capacity 每個槽的容量
     */
    public MultiFluidTank(int tankCount, int capacity) {
        this.tanks = new ArrayList<>(tankCount);
        for (int i = 0; i < tankCount; i++) {
            tanks.add(new FluidTankInfo(capacity));
        }
    }
    
    /**
     * 建立單槽流體儲存
     * 
     * @param capacity 容量
     */
    public MultiFluidTank(int capacity) {
        this(1, capacity);
    }
    
    @Override
    public int getTanks() {
        return tanks.size();
    }
    
    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank < 0 || tank >= tanks.size()) {
            return FluidStack.empty();
        }
        return tanks.get(tank).fluid.copy();
    }
    
    @Override
    public int getTankCapacity(int tank) {
        if (tank < 0 || tank >= tanks.size()) {
            return 0;
        }
        return tanks.get(tank).capacity;
    }
    
    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank < 0 || tank >= tanks.size() || stack == null) {
            return false;
        }
        FluidTankInfo info = tanks.get(tank);
        return info.fluid.isEmpty() || info.fluid.isFluidEqual(stack);
    }
    
    @Override
    public int fill(FluidStack resource, boolean simulate) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }
        
        int totalFilled = 0;
        int remaining = resource.getAmount();
        
        // 先嘗試填入已有相同流體的槽
        for (FluidTankInfo tank : tanks) {
            if (remaining <= 0) break;
            if (tank.fluid.isFluidEqual(resource) && !tank.fluid.isEmpty()) {
                int canFill = Math.min(tank.capacity - tank.fluid.getAmount(), remaining);
                if (!simulate) {
                    tank.fluid.grow(canFill);
                }
                totalFilled += canFill;
                remaining -= canFill;
            }
        }
        
        // 再嘗試填入空槽
        if (remaining > 0) {
            for (FluidTankInfo tank : tanks) {
                if (remaining <= 0) break;
                if (tank.fluid.isEmpty()) {
                    int canFill = Math.min(tank.capacity, remaining);
                    if (!simulate) {
                        tank.fluid = new FluidStack(resource.getFluidId(), canFill);
                    }
                    totalFilled += canFill;
                    remaining -= canFill;
                }
            }
        }
        
        return totalFilled;
    }
    
    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        if (resource == null || resource.isEmpty()) {
            return FluidStack.empty();
        }
        
        int totalDrained = 0;
        int remaining = resource.getAmount();
        
        for (FluidTankInfo tank : tanks) {
            if (remaining <= 0) break;
            if (tank.fluid.isFluidEqual(resource)) {
                int canDrain = Math.min(tank.fluid.getAmount(), remaining);
                if (!simulate) {
                    tank.fluid.shrink(canDrain);
                }
                totalDrained += canDrain;
                remaining -= canDrain;
            }
        }
        
        if (totalDrained <= 0) {
            return FluidStack.empty();
        }
        return new FluidStack(resource.getFluidId(), totalDrained);
    }
    
    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        if (maxDrain <= 0) {
            return FluidStack.empty();
        }
        
        // 找第一個非空的槽來排出
        for (FluidTankInfo tank : tanks) {
            if (!tank.fluid.isEmpty()) {
                int canDrain = Math.min(tank.fluid.getAmount(), maxDrain);
                FluidStack result = new FluidStack(tank.fluid.getFluidId(), canDrain);
                if (!simulate) {
                    tank.fluid.shrink(canDrain);
                }
                return result;
            }
        }
        
        return FluidStack.empty();
    }
    
    /**
     * 儲存槽資訊
     */
    private static class FluidTankInfo {
        FluidStack fluid;
        final int capacity;
        
        FluidTankInfo(int capacity) {
            this.fluid = FluidStack.empty();
            this.capacity = capacity;
        }
    }
}
