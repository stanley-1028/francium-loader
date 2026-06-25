package com.francium.forge.energy;

/**
 * 能量儲存基本實作
 * 
 * 對應 Forge 的 EnergyStorage
 * 提供基本的能量儲存功能
 * 
 * 能量單位：FE（Forge Energy）/ RF（Redstone Flux）
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class EnergyStorage implements IEnergyStorage {
    
    /** 目前儲存的能量 */
    protected int energy;
    
    /** 最大儲存能量 */
    protected int capacity;
    
    /** 最大接收速率 */
    protected int maxReceive;
    
    /** 最大提取速率 */
    protected int maxExtract;
    
    /**
     * 建立能量儲存
     * 
     * @param capacity 最大儲存能量
     */
    public EnergyStorage(int capacity) {
        this(capacity, capacity, capacity, 0);
    }
    
    /**
     * 建立能量儲存
     * 
     * @param capacity 最大儲存能量
     * @param maxTransfer 最大傳輸速率（接收和提取相同）
     */
    public EnergyStorage(int capacity, int maxTransfer) {
        this(capacity, maxTransfer, maxTransfer, 0);
    }
    
    /**
     * 建立能量儲存
     * 
     * @param capacity 最大儲存能量
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     */
    public EnergyStorage(int capacity, int maxReceive, int maxExtract) {
        this(capacity, maxReceive, maxExtract, 0);
    }
    
    /**
     * 建立能量儲存
     * 
     * @param capacity 最大儲存能量（必須 >= 0）
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     * @param energy 初始能量
     * @throws IllegalArgumentException 如果 capacity 為負數
     */
    public EnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative: " + capacity);
        }
        this.capacity = capacity;
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
        this.energy = Math.max(0, Math.min(capacity, energy));
    }
    
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }
        
        if (maxReceive <= 0) {
            return 0;
        }
        
        int energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate) {
            energy += energyReceived;
        }
        return energyReceived;
    }
    
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) {
            return 0;
        }
        
        if (maxExtract <= 0) {
            return 0;
        }
        
        int energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate) {
            energy -= energyExtracted;
        }
        return energyExtracted;
    }
    
    @Override
    public int getEnergyStored() {
        return energy;
    }
    
    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }
    
    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }
    
    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }
    
    /**
     * 直接設定能量（僅供內部使用）
     */
    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(capacity, energy));
    }
    
    /**
     * 取得最大接收速率
     */
    public int getMaxReceive() {
        return maxReceive;
    }
    
    /**
     * 取得最大提取速率
     */
    public int getMaxExtract() {
        return maxExtract;
    }
    
    /**
     * 取得能量填充百分比（0.0 - 1.0）
     */
    public float getFillPercentage() {
        if (capacity <= 0) {
            return 0;
        }
        return (float) energy / capacity;
    }
    
    /**
     * 檢查是否已滿
     */
    public boolean isFull() {
        return energy >= capacity;
    }
    
    /**
     * 檢查是否為空
     */
    public boolean isEmpty() {
        return energy <= 0;
    }
}
