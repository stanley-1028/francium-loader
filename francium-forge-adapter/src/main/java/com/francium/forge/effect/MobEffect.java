package com.francium.forge.effect;

/**
 * 藥水效果基底類別
 * 
 * 對應 Forge/Minecraft 的 MobEffect
 * 表示一種藥水效果類型
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class MobEffect {
    
    /** 效果 ID */
    private final String id;
    
    /** 效果名稱 */
    private final String name;
    
    /** 效果描述 */
    private final String description;
    
    /** 是否為正面效果 */
    private final boolean beneficial;
    
    /** 效果顏色 */
    private final int color;
    
    /** 是否為瞬間效果 */
    private final boolean instant;
    
    /**
     * 建立藥水效果
     * 
     * @param id 效果 ID
     * @param name 效果名稱
     * @param beneficial 是否為正面效果
     * @param color 效果顏色
     */
    public MobEffect(String id, String name, boolean beneficial, int color) {
        this(id, name, "", beneficial, color, false);
    }
    
    /**
     * 建立藥水效果
     * 
     * @param id 效果 ID
     * @param name 效果名稱
     * @param description 效果描述
     * @param beneficial 是否為正面效果
     * @param color 效果顏色
     */
    public MobEffect(String id, String name, String description, 
                     boolean beneficial, int color) {
        this(id, name, description, beneficial, color, false);
    }
    
    /**
     * 建立藥水效果
     * 
     * @param id 效果 ID
     * @param name 效果名稱
     * @param description 效果描述
     * @param beneficial 是否為正面效果
     * @param color 效果顏色
     * @param instant 是否為瞬間效果
     */
    public MobEffect(String id, String name, String description, 
                     boolean beneficial, int color, boolean instant) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Effect ID cannot be null or empty");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Effect name cannot be null or empty");
        }
        
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.beneficial = beneficial;
        this.color = color;
        this.instant = instant;
    }
    
    /**
     * 取得效果 ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 取得效果名稱
     */
    public String getName() {
        return name;
    }
    
    /**
     * 取得效果描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 檢查是否為正面效果
     */
    public boolean isBeneficial() {
        return beneficial;
    }
    
    /**
     * 取得效果顏色
     */
    public int getColor() {
        return color;
    }
    
    /**
     * 檢查是否為瞬間效果
     */
    public boolean isInstant() {
        return instant;
    }
    
    /**
     * 檢查指定等級是否有效
     * 
     * @param amplifier 等級
     * @return 是否有效
     */
    public boolean isDurationEffectTick(int amplifier) {
        // 預設：非瞬間效果在任何等級都有效
        return !instant;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MobEffect)) return false;
        MobEffect other = (MobEffect) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return id + " (" + name + ")";
    }
}
