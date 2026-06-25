package com.francium.forge.block;

/**
 * 方向列舉
 * 
 * 對應 Forge/Minecraft 的 Direction
 * 表示方塊的六個方向
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public enum Direction {
    
    /** 下方（負 Y 軸） */
    DOWN(0, -1, 0, Axis.Y),
    
    /** 上方（正 Y 軸） */
    UP(0, 1, 0, Axis.Y),
    
    /** 北方（負 Z 軸） */
    NORTH(0, 0, -1, Axis.Z),
    
    /** 南方（正 Z 軸） */
    SOUTH(0, 0, 1, Axis.Z),
    
    /** 西方（負 X 軸） */
    WEST(-1, 0, 0, Axis.X),
    
    /** 東方（正 X 軸） */
    EAST(1, 0, 0, Axis.X);
    
    /** X 軸偏移 */
    private final int stepX;
    
    /** Y 軸偏移 */
    private final int stepY;
    
    /** Z 軸偏移 */
    private final int stepZ;
    
    /** 所屬軸 */
    private final Axis axis;
    
    Direction(int stepX, int stepY, int stepZ, Axis axis) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
        this.axis = axis;
    }
    
    /**
     * 取得 X 軸偏移
     */
    public int getStepX() {
        return stepX;
    }
    
    /**
     * 取得 Y 軸偏移
     */
    public int getStepY() {
        return stepY;
    }
    
    /**
     * 取得 Z 軸偏移
     */
    public int getStepZ() {
        return stepZ;
    }
    
    /**
     * 取得所屬軸
     */
    public Axis getAxis() {
        return axis;
    }
    
    /**
     * 取得相反方向
     */
    public Direction getOpposite() {
        switch (this) {
            case DOWN: return UP;
            case UP: return DOWN;
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case WEST: return EAST;
            case EAST: return WEST;
            default: throw new IllegalStateException("Unknown direction: " + this);
        }
    }
    
    /**
     * 取得順時針旋轉後的方向（從上方看）
     */
    public Direction getClockWise() {
        switch (this) {
            case NORTH: return EAST;
            case EAST: return SOUTH;
            case SOUTH: return WEST;
            case WEST: return NORTH;
            default: return this;
        }
    }
    
    /**
     * 取得逆時針旋轉後的方向（從上方看）
     */
    public Direction getCounterClockWise() {
        switch (this) {
            case NORTH: return WEST;
            case WEST: return SOUTH;
            case SOUTH: return EAST;
            case EAST: return NORTH;
            default: return this;
        }
    }
    
    /**
     * 檢查是否為水平方向（東西南北）
     */
    public boolean isHorizontal() {
        return axis == Axis.X || axis == Axis.Z;
    }
    
    /**
     * 檢查是否為垂直方向（上下）
     */
    public boolean isVertical() {
        return axis == Axis.Y;
    }
    
    /**
     * 根據名稱取得方向
     * 
     * @param name 方向名稱
     * @return 方向
     * @throws IllegalArgumentException 如果名稱無效
     */
    public static Direction byName(String name) {
        return valueOf(name.toUpperCase());
    }
    
    /**
     * 根據索引取得方向
     * 
     * @param index 索引 (0-5)
     * @return 方向
     */
    public static Direction byIndex(int index) {
        Direction[] values = values();
        return values[Math.abs(index % values.length)];
    }
    
    /**
     * 軸列舉
     */
    public enum Axis {
        /** X 軸 */
        X,
        /** Y 軸 */
        Y,
        /** Z 軸 */
        Z;
        
        /**
         * 檢查是否為垂直軸
         */
        public boolean isVertical() {
            return this == Y;
        }
        
        /**
         * 檢查是否為水平軸
         */
        public boolean isHorizontal() {
            return this != Y;
        }
    }
}
