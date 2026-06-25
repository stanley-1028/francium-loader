package com.francium.forge.event.world;

import com.francium.forge.event.FMLEvent;

/**
 * 世界事件基底類別
 * 
 * 對應 Forge 的 WorldEvent
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class WorldEvent extends FMLEvent {
    
    /** 世界名稱 */
    private final String worldName;
    
    /** 維度 ID */
    private final String dimension;
    
    /**
     * 建立世界事件
     */
    public WorldEvent(String worldName, String dimension) {
        this.worldName = worldName;
        this.dimension = dimension;
    }
    
    /**
     * 取得世界名稱
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * 取得維度
     */
    public String getDimension() {
        return dimension;
    }
    
    // ===== 子事件 =====
    
    /**
     * 世界載入事件
     */
    public static class LoadEvent extends WorldEvent {
        public LoadEvent(String worldName, String dimension) {
            super(worldName, dimension);
        }
    }
    
    /**
     * 世界卸載事件
     */
    public static class UnloadEvent extends WorldEvent {
        public UnloadEvent(String worldName, String dimension) {
            super(worldName, dimension);
        }
    }
    
    /**
     * 世界保存事件
     */
    public static class SaveEvent extends WorldEvent {
        public SaveEvent(String worldName, String dimension) {
            super(worldName, dimension);
        }
    }
    
    /**
     * 世界 Tick 事件
     */
    public static class TickEvent extends WorldEvent {
        private final long tickCount;
        private final Phase phase;
        
        public TickEvent(String worldName, String dimension, long tickCount, Phase phase) {
            super(worldName, dimension);
            this.tickCount = tickCount;
            this.phase = phase;
        }
        
        public long getTickCount() {
            return tickCount;
        }
        
        public Phase getPhase() {
            return phase;
        }
        
        /** Tick 階段 */
        public enum Phase {
            START,
            END
        }
    }
    
    /**
     * 爆炸事件
     * 可取消：如果取消，爆炸不會發生
     */
    public static class ExplosionEvent extends WorldEvent {
        private final double x;
        private final double y;
        private final double z;
        private final float radius;
        private final String exploderType;
        
        public ExplosionEvent(String worldName, String dimension,
                              double x, double y, double z, float radius,
                              String exploderType) {
            super(worldName, dimension);
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.exploderType = exploderType;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
        
        public double getZ() {
            return z;
        }
        
        public float getRadius() {
            return radius;
        }
        
        public String getExploderType() {
            return exploderType;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
    
    /**
     * 雷擊事件
     */
    public static class LightningStrikeEvent extends WorldEvent {
        private final double x;
        private final double y;
        private final double z;
        
        public LightningStrikeEvent(String worldName, String dimension,
                                    double x, double y, double z) {
            super(worldName, dimension);
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
        
        public double getZ() {
            return z;
        }
    }
    
    /**
     * 潛行方塊生成事件
     * 可取消：如果取消，方塊不會生成
     */
    public static class BlockGrowEvent extends WorldEvent {
        private final int x;
        private final int y;
        private final int z;
        private final String blockId;
        
        public BlockGrowEvent(String worldName, String dimension,
                              int x, int y, int z, String blockId) {
            super(worldName, dimension);
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockId = blockId;
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
        
        public int getZ() {
            return z;
        }
        
        public String getBlockId() {
            return blockId;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
}
