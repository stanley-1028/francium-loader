package com.francium.forge.energy;

/**
 * 能量儲存介面
 * 
 * 對應 Forge 的 IEnergyStorage
 * 用於表示可以儲存能量的方塊/物品
 * 
 * 能量單位：FE（Forge Energy）/ RF（Redstone Flux）
 * 1 FE = 1 RF
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public interface IEnergyStorage {
    
    /**
     * 接收能量
     * 
     * @param maxReceive 最大接收量
     * @param simulate 是否為模擬（true 時不實際改變能量）
     * @return 實際接收的能量
     */
    int receiveEnergy(int maxReceive, boolean simulate);
    
    /**
     * 提取能量
     * 
     * @param maxExtract 最大提取量
     * @param simulate 是否為模擬（true 時不實際改變能量）
     * @return 實際提取的能量
     */
    int extractEnergy(int maxExtract, boolean simulate);
    
    /**
     * 取得目前儲存的能量
     */
    int getEnergyStored();
    
    /**
     * 取得最大儲存能量
     */
    int getMaxEnergyStored();
    
    /**
     * 是否可以接收能量
     */
    boolean canReceive();
    
    /**
     * 是否可以提取能量
     */
    boolean canExtract();
}
