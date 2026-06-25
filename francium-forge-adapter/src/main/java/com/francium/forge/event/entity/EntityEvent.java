package com.francium.forge.event.entity;

import com.francium.forge.event.FMLEvent;

/**
 * 實體事件基底類別
 * 
 * 對應 Forge 的 EntityEvent
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class EntityEvent extends FMLEvent {
    
    /** 實體類型 ID */
    private final String entityType;
    
    /** 實體 UUID（字串形式） */
    private final String entityUUID;
    
    /** 位置 */
    private final double x;
    private final double y;
    private final double z;
    
    /** 維度 */
    private final String dimension;
    
    /**
     * 建立實體事件
     */
    public EntityEvent(String entityType, String entityUUID, 
                       double x, double y, double z, String dimension) {
        this.entityType = entityType;
        this.entityUUID = entityUUID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }
    
    /**
     * 取得實體類型
     */
    public String getEntityType() {
        return entityType;
    }
    
    /**
     * 取得實體 UUID
     */
    public String getEntityUUID() {
        return entityUUID;
    }
    
    /**
     * 取得 X 座標
     */
    public double getX() {
        return x;
    }
    
    /**
     * 取得 Y 座標
     */
    public double getY() {
        return y;
    }
    
    /**
     * 取得 Z 座標
     */
    public double getZ() {
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
     * 實體加入世界事件
     */
    public static class EntityJoinWorldEvent extends EntityEvent {
        private final boolean isNew;
        
        public EntityJoinWorldEvent(String entityType, String entityUUID,
                                    double x, double y, double z, String dimension,
                                    boolean isNew) {
            super(entityType, entityUUID, x, y, z, dimension);
            this.isNew = isNew;
        }
        
        public boolean isNew() {
            return isNew;
        }
    }
    
    /**
     * 實體離開世界事件
     */
    public static class EntityLeaveWorldEvent extends EntityEvent {
        public EntityLeaveWorldEvent(String entityType, String entityUUID,
                                     double x, double y, double z, String dimension) {
            super(entityType, entityUUID, x, y, z, dimension);
        }
    }
    
    /**
     * 實體受到傷害事件
     * 可取消：如果取消，實體不會受到傷害
     */
    public static class EntityHurtEvent extends EntityEvent {
        private final String damageSource;
        private final float amount;
        private final String attackerType;
        
        public EntityHurtEvent(String entityType, String entityUUID,
                               double x, double y, double z, String dimension,
                               String damageSource, float amount, String attackerType) {
            super(entityType, entityUUID, x, y, z, dimension);
            this.damageSource = damageSource;
            this.amount = amount;
            this.attackerType = attackerType;
        }
        
        public String getDamageSource() {
            return damageSource;
        }
        
        public float getAmount() {
            return amount;
        }
        
        public String getAttackerType() {
            return attackerType;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
    
    /**
     * 實體死亡事件
     */
    public static class EntityDeathEvent extends EntityEvent {
        private final String damageSource;
        private final java.util.List<String> drops;
        private final int droppedExp;
        
        public EntityDeathEvent(String entityType, String entityUUID,
                                double x, double y, double z, String dimension,
                                String damageSource, java.util.List<String> drops,
                                int droppedExp) {
            super(entityType, entityUUID, x, y, z, dimension);
            this.damageSource = damageSource;
            this.drops = drops;
            this.droppedExp = droppedExp;
        }
        
        public String getDamageSource() {
            return damageSource;
        }
        
        public java.util.List<String> getDrops() {
            return drops;
        }
        
        public int getDroppedExp() {
            return droppedExp;
        }
    }
    
    /**
     * 實體生成事件（自然生成）
     * 可取消：如果取消，實體不會生成
     */
    public static class EntitySpawnPlacementEvent extends EntityEvent {
        private final String spawnReason;
        
        public EntitySpawnPlacementEvent(String entityType, String entityUUID,
                                         double x, double y, double z, String dimension,
                                         String spawnReason) {
            super(entityType, entityUUID, x, y, z, dimension);
            this.spawnReason = spawnReason;
        }
        
        public String getSpawnReason() {
            return spawnReason;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
    
    /**
     * 實體改變維度事件
     */
    public static class EntityTravelToDimensionEvent extends EntityEvent {
        private final String fromDimension;
        private final String toDimension;
        
        public EntityTravelToDimensionEvent(String entityType, String entityUUID,
                                            double x, double y, double z, String dimension,
                                            String fromDimension, String toDimension) {
            super(entityType, entityUUID, x, y, z, dimension);
            this.fromDimension = fromDimension;
            this.toDimension = toDimension;
        }
        
        public String getFromDimension() {
            return fromDimension;
        }
        
        public String getToDimension() {
            return toDimension;
        }
        
        @Override
        public boolean isCancelable() {
            return true;
        }
    }
}
