package com.francium.forge.event.block;

import com.francium.forge.event.FMLEvent;

/**
 * 方塊事件基底類別
 * 
 * 對應 Forge 的 BlockEvent
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class BlockEvent extends FMLEvent {
    
    /** 方塊 ID */
    private final String blockId;
    
    /** 方塊位置 */
    private final int x;
    private final int y;
    private final int z;
    
    /** 維度 */
    private final String dimension;
    
    /**
     * 建立方塊事件
     */
    public BlockEvent(String blockId, int x, int y, int z, String dimension) {
        this.blockId = blockId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }
    
    /**
     * 取得方塊 ID
     */
    public String getBlockId() {
        return blockId;
    }
    
    /**
     * 取得 X 座標
     */
    public int getX() {
        return x;
    }
    
    /**
     * 取得 Y 座標
     */
    public int getY() {
        return y;
    }
    
    /**
     * 取得 Z 座標
     */
    public int getZ() {
        return z;
    }
    
    /**
     * 取得維度
     */
    public String getDimension() {
        return dimension;
    }
    
    // ===== 子事件 =====
    
    /**
     * 方塊被破壞事件
     * 可取消：如果取消，方塊不會被破壞
     */
    public static class BreakEvent extends BlockEvent {
        private final String breakerName;
        private final int expDrop;
        
        public BreakEvent(String blockId, int x, int y, int z, String dimension,
                          String breakerName, int expDrop) {
            super(blockId, x, y, z, dimension);
            this.breakerName = breakerName;
            this.expDrop = expDrop;
        }
        
        public String getBreakerName() {
            return breakerName;
        }
        
        public int getExpDrop() {
            return expDrop;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
    
    /**
     * 方塊被放置事件
     * 可取消：如果取消，方塊不會被放置
     */
    public static class PlaceEvent extends BlockEvent {
        private final String placerName;
        private final String placedAgainst;
        
        public PlaceEvent(String blockId, int x, int y, int z, String dimension,
                          String placerName, String placedAgainst) {
            super(blockId, x, y, z, dimension);
            this.placerName = placerName;
            this.placedAgainst = placedAgainst;
        }
        
        public String getPlacerName() {
            return placerName;
        }
        
        public String getPlacedAgainst() {
            return placedAgainst;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
    
    /**
     * 方塊掉落物品事件
     */
    public static class DropsEvent extends BlockEvent {
        private final String breakerName;
        private final java.util.List<String> drops;
        
        public DropsEvent(String blockId, int x, int y, int z, String dimension,
                          String breakerName, java.util.List<String> drops) {
            super(blockId, x, y, z, dimension);
            this.breakerName = breakerName;
            this.drops = drops;
        }
        
        public String getBreakerName() {
            return breakerName;
        }
        
        public java.util.List<String> getDrops() {
            return drops;
        }
    }
    
    /**
     * 方塊實體更新事件（每 tick）
     */
    public static class NeighborNotifyEvent extends BlockEvent {
        private final String notifiedBlockId;
        
        public NeighborNotifyEvent(String blockId, int x, int y, int z, String dimension,
                                   String notifiedBlockId) {
            super(blockId, x, y, z, dimension);
            this.notifiedBlockId = notifiedBlockId;
        }
        
        public String getNotifiedBlockId() {
            return notifiedBlockId;
        }
    }
    
    /**
     * 作物生長事件
     * 可取消：如果取消，作物不會生長
     */
    public static class CropGrowEvent extends BlockEvent {
        private final int originalAge;
        private final int newAge;
        
        public CropGrowEvent(String blockId, int x, int y, int z, String dimension,
                             int originalAge, int newAge) {
            super(blockId, x, y, z, dimension);
            this.originalAge = originalAge;
            this.newAge = newAge;
        }
        
        public int getOriginalAge() {
            return originalAge;
        }
        
        public int getNewAge() {
            return newAge;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
}
