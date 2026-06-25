package com.francium.forge.entity;

/**
 * 內建屬性常數
 * 
 * 對應 Forge/Minecraft 的內建屬性
 * 包含常用的實體屬性定義
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public final class Attributes {
    
    private Attributes() {}
    
    // ===== 通用屬性 =====
    
    /** 最大生命值 */
    public static final Attribute MAX_HEALTH = new Attribute(
        "minecraft:generic.max_health",
        20.0,  // 預設值
        0.0,   // 最小值
        1024.0, // 最大值
        "最大生命值"
    );
    
    /** 跟隨範圍 */
    public static final Attribute FOLLOW_RANGE = new Attribute(
        "minecraft:generic.follow_range",
        32.0,  // 預設值
        0.0,   // 最小值
        2048.0, // 最大值
        "跟隨範圍"
    );
    
    /** 擊退抗性 */
    public static final Attribute KNOCKBACK_RESISTANCE = new Attribute(
        "minecraft:generic.knockback_resistance",
        0.0,   // 預設值
        0.0,   // 最小值
        1.0,   // 最大值
        "擊退抗性"
    );
    
    /** 移動速度 */
    public static final Attribute MOVEMENT_SPEED = new Attribute(
        "minecraft:generic.movement_speed",
        0.699999988079071,  // 預設值
        0.0,   // 最小值
        1024.0, // 最大值
        "移動速度"
    );
    
    /** 飛行速度 */
    public static final Attribute FLYING_SPEED = new Attribute(
        "minecraft:generic.flying_speed",
        0.4000000059604645,  // 預設值
        0.0,   // 最小值
        1024.0, // 最大值
        "飛行速度"
    );
    
    /** 攻擊傷害 */
    public static final Attribute ATTACK_DAMAGE = new Attribute(
        "minecraft:generic.attack_damage",
        2.0,   // 預設值
        0.0,   // 最小值
        2048.0, // 最大值
        "攻擊傷害"
    );
    
    /** 攻擊速度 */
    public static final Attribute ATTACK_SPEED = new Attribute(
        "minecraft:generic.attack_speed",
        4.0,   // 預設值
        0.0,   // 最小值
        1024.0, // 最大值
        "攻擊速度"
    );
    
    /** 攻擊擊退 */
    public static final Attribute ATTACK_KNOCKBACK = new Attribute(
        "minecraft:generic.attack_knockback",
        0.0,   // 預設值
        0.0,   // 最小值
        5.0,   // 最大值
        "攻擊擊退"
    );
    
    /** 護甲值 */
    public static final Attribute ARMOR = new Attribute(
        "minecraft:generic.armor",
        0.0,   // 預設值
        0.0,   // 最小值
        30.0,  // 最大值
        "護甲值"
    );
    
    /** 護甲韌性 */
    public static final Attribute ARMOR_TOUGHNESS = new Attribute(
        "minecraft:generic.armor_toughness",
        0.0,   // 預設值
        0.0,   // 最小值
        20.0,  // 最大值
        "護甲韌性"
    );
    
    /** 幸運值 */
    public static final Attribute LUCK = new Attribute(
        "minecraft:generic.luck",
        0.0,   // 預設值
        -1024.0, // 最小值
        1024.0,  // 最大值
        "幸運值"
    );
    
    // ===== 玩家專屬屬性 =====
    
    /** 挖掘速度 */
    public static final Attribute MINING_SPEED = new Attribute(
        "minecraft:player.mining_speed",
        1.0,   // 預設值
        0.0,   // 最小值
        1024.0, // 最大值
        "挖掘速度"
    );
    
    /** 潛行速度 */
    public static final Attribute SNEAKING_SPEED = new Attribute(
        "minecraft:player.sneaking_speed",
        0.30000001192092896,  // 預設值
        0.0,   // 最小值
        1.0,   // 最大值
        "潛行速度"
    );
    
    /** 水下挖掘速度 */
    public static final Attribute SUBMERGED_MINING_SPEED = new Attribute(
        "minecraft:player.submerged_mining_speed",
        0.2,   // 預設值
        0.0,   // 最小值
        1024.0, // 最大值
        "水下挖掘速度"
    );
    
    /** 騎乘跳躍力量 */
    public static final Attribute JUMP_STRENGTH = new Attribute(
        "minecraft:horse.jump_strength",
        0.7,   // 預設值
        0.0,   // 最小值
        2.0,   // 最大值
        "跳躍力量"
    );
    
    /**
     * 取得所有內建屬性
     */
    public static Attribute[] values() {
        return new Attribute[] {
            MAX_HEALTH,
            FOLLOW_RANGE,
            KNOCKBACK_RESISTANCE,
            MOVEMENT_SPEED,
            FLYING_SPEED,
            ATTACK_DAMAGE,
            ATTACK_SPEED,
            ATTACK_KNOCKBACK,
            ARMOR,
            ARMOR_TOUGHNESS,
            LUCK,
            MINING_SPEED,
            SNEAKING_SPEED,
            SUBMERGED_MINING_SPEED,
            JUMP_STRENGTH
        };
    }
    
    /**
     * 根據 ID 取得屬性
     * 
     * @param id 屬性 ID
     * @return 屬性，如果找不到則返回 null
     */
    public static Attribute byId(String id) {
        if (id == null) {
            return null;
        }
        for (Attribute attr : values()) {
            if (attr.getId().equals(id)) {
                return attr;
            }
        }
        return null;
    }
}
