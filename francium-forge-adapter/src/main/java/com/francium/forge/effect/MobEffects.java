package com.francium.forge.effect;

/**
 * 內建藥水效果常數
 * 
 * 對應 Forge/Minecraft 的內建藥水效果
 * 包含常用的藥水效果定義
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public final class MobEffects {
    
    private MobEffects() {}
    
    // ===== 正面效果 =====
    
    /** 速度 */
    public static final MobEffect SPEED = new MobEffect(
        "minecraft:speed",
        "速度",
        "提高移動速度",
        true,
        0x7CAFC6
    );
    
    /** 減速 */
    public static final MobEffect SLOWNESS = new MobEffect(
        "minecraft:slowness",
        "減速",
        "降低移動速度",
        false,
        0x5A6C81
    );
    
    /** 加速挖掘 */
    public static final MobEffect HASTE = new MobEffect(
        "minecraft:haste",
        "加速挖掘",
        "提高挖掘速度",
        true,
        0xD9C043
    );
    
    /** 減速挖掘 */
    public static final MobEffect MINING_FATIGUE = new MobEffect(
        "minecraft:mining_fatigue",
        "減速挖掘",
        "降低挖掘速度",
        false,
        0x4A4217
    );
    
    /** 力量 */
    public static final MobEffect STRENGTH = new MobEffect(
        "minecraft:strength",
        "力量",
        "提高近戰傷害",
        true,
        0x932423
    );
    
    /** 瞬間治療 */
    public static final MobEffect INSTANT_HEALTH = new MobEffect(
        "minecraft:instant_health",
        "瞬間治療",
        "立即恢復生命值",
        true,
        0xF82423,
        true
    );
    
    /** 瞬間傷害 */
    public static final MobEffect INSTANT_DAMAGE = new MobEffect(
        "minecraft:instant_damage",
        "瞬間傷害",
        "立即造成傷害",
        false,
        0x430A09,
        true
    );
    
    /** 跳躍提升 */
    public static final MobEffect JUMP_BOOST = new MobEffect(
        "minecraft:jump_boost",
        "跳躍提升",
        "提高跳躍高度",
        true,
        0x22FF4C
    );
    
    /** 噁心 */
    public static final MobEffect NAUSEA = new MobEffect(
        "minecraft:nausea",
        "噁心",
        "畫面扭曲",
        false,
        0x551D4A
    );
    
    /** 生命恢復 */
    public static final MobEffect REGENERATION = new MobEffect(
        "minecraft:regeneration",
        "生命恢復",
        "逐漸恢復生命值",
        true,
        0xCD5CAB
    );
    
    /** 抗性提升 */
    public static final MobEffect RESISTANCE = new MobEffect(
        "minecraft:resistance",
        "抗性提升",
        "減少所有傷害",
        true,
        0x99453A
    );
    
    /** 防火 */
    public static final MobEffect FIRE_RESISTANCE = new MobEffect(
        "minecraft:fire_resistance",
        "防火",
        "免疫火焰傷害",
        true,
        0xE49A3A
    );
    
    /** 水下呼吸 */
    public static final MobEffect WATER_BREATHING = new MobEffect(
        "minecraft:water_breathing",
        "水下呼吸",
        "在水下不會溺水",
        true,
        0x2E5299
    );
    
    /** 隱身 */
    public static final MobEffect INVISIBILITY = new MobEffect(
        "minecraft:invisibility",
        "隱身",
        "使生物看不見你",
        true,
        0x7F8392
    );
    
    /** 失明 */
    public static final MobEffect BLINDNESS = new MobEffect(
        "minecraft:blindness",
        "失明",
        "視野變暗",
        false,
        0x1F1F23
    );
    
    /** 夜視 */
    public static final MobEffect NIGHT_VISION = new MobEffect(
        "minecraft:night_vision",
        "夜視",
        "在黑暗中也能看清",
        true,
        0x1F1FA1
    );
    
    /** 飢餓 */
    public static final MobEffect HUNGER = new MobEffect(
        "minecraft:hunger",
        "飢餓",
        "加速飢餓值消耗",
        false,
        0x587653
    );
    
    /** 虛弱 */
    public static final MobEffect WEAKNESS = new MobEffect(
        "minecraft:weakness",
        "虛弱",
        "降低近戰傷害",
        false,
        0x484D48
    );
    
    /** 中毒 */
    public static final MobEffect POISON = new MobEffect(
        "minecraft:poison",
        "中毒",
        "逐漸造成傷害（不會致死）",
        false,
        0x4E9331
    );
    
    /** 凋零 */
    public static final MobEffect WITHER = new MobEffect(
        "minecraft:wither",
        "凋零",
        "逐漸造成傷害（會致死）",
        false,
        0x352A27
    );
    
    /** 生命提升 */
    public static final MobEffect HEALTH_BOOST = new MobEffect(
        "minecraft:health_boost",
        "生命提升",
        "提高最大生命值",
        true,
        0xF87D23
    );
    
    /** 傷害吸收 */
    public static final MobEffect ABSORPTION = new MobEffect(
        "minecraft:absorption",
        "傷害吸收",
        "提供吸收傷害的護盾",
        true,
        0x2552A5
    );
    
    /** 飽食 */
    public static final MobEffect SATURATION = new MobEffect(
        "minecraft:saturation",
        "飽食",
        "恢復飢餓值",
        true,
        0xF82423,
        true
    );
    
    /** 發光 */
    public static final MobEffect GLOWING = new MobEffect(
        "minecraft:glowing",
        "發光",
        "使生物發出輪廓光",
        false,
        0x94A061
    );
    
    /** 漂浮 */
    public static final MobEffect LEVITATION = new MobEffect(
        "minecraft:levitation",
        "漂浮",
        "使生物向上漂浮",
        false,
        0xCEFFFF
    );
    
    /** 幸運 */
    public static final MobEffect LUCK = new MobEffect(
        "minecraft:luck",
        "幸運",
        "提高好運",
        true,
        0x339900
    );
    
    /** 厄運 */
    public static final MobEffect UNLUCK = new MobEffect(
        "minecraft:unluck",
        "厄運",
        "降低好運",
        false,
        0xC0A44D
    );
    
    /** 緩降 */
    public static final MobEffect SLOW_FALLING = new MobEffect(
        "minecraft:slow_falling",
        "緩降",
        "減少掉落速度",
        true,
        0xFFEFD5
    );
    
    /** 共鳴 */
    public static final MobEffect CONDUIT_POWER = new MobEffect(
        "minecraft:conduit_power",
        "共鳴",
        "水下全方位強化",
        true,
        0x1DC2D1
    );
    
    /** 海豚的恩惠 */
    public static final MobEffect DOLPHINS_GRACE = new MobEffect(
        "minecraft:dolphins_grace",
        "海豚的恩惠",
        "提高游泳速度",
        true,
        0x88A3BE
    );
    
    /** 不祥之兆 */
    public static final MobEffect BAD_OMEN = new MobEffect(
        "minecraft:bad_omen",
        "不祥之兆",
        "進入村莊會觸發襲擊",
        false,
        0x0B6138
    );
    
    /** 村莊英雄 */
    public static final MobEffect HERO_OF_THE_VILLAGE = new MobEffect(
        "minecraft:hero_of_the_village",
        "村莊英雄",
        "村民交易打折",
        true,
        0x44FF44
    );
    
    /**
     * 取得所有內建藥水效果
     */
    public static MobEffect[] values() {
        return new MobEffect[] {
            SPEED, SLOWNESS, HASTE, MINING_FATIGUE, STRENGTH,
            INSTANT_HEALTH, INSTANT_DAMAGE, JUMP_BOOST, NAUSEA,
            REGENERATION, RESISTANCE, FIRE_RESISTANCE, WATER_BREATHING,
            INVISIBILITY, BLINDNESS, NIGHT_VISION, HUNGER, WEAKNESS,
            POISON, WITHER, HEALTH_BOOST, ABSORPTION, SATURATION,
            GLOWING, LEVITATION, LUCK, UNLUCK, SLOW_FALLING,
            CONDUIT_POWER, DOLPHINS_GRACE, BAD_OMEN, HERO_OF_THE_VILLAGE
        };
    }
    
    /**
     * 根據 ID 取得藥水效果
     * 
     * @param id 效果 ID
     * @return 效果，如果找不到則返回 null
     */
    public static MobEffect byId(String id) {
        if (id == null) {
            return null;
        }
        for (MobEffect effect : values()) {
            if (effect.getId().equals(id)) {
                return effect;
            }
        }
        return null;
    }
}
