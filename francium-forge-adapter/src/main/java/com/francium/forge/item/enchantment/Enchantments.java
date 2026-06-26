package com.francium.forge.item.enchantment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/**
 * 內建附魔常數
 * 
 * 對應 Forge/Minecraft 的內建附魔
 * 包含常用的附魔定義
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public final class Enchantments {
    
    private Enchantments() {}
    
    // ===== 武器附魔 =====
    
    /** 鋒利 */
    public static final Enchantment SHARPNESS = new Enchantment(
        "minecraft:sharpness",
        "鋒利",
        "增加近戰傷害",
        Enchantment.Rarity.COMMON,
        new HashSet<>(Arrays.asList(EnchantmentCategory.SWORD, EnchantmentCategory.WEAPON)),
        1, 5,
        false, false, true, true
    );
    
    /** 殺戮 */
    public static final Enchantment SMITE = new Enchantment(
        "minecraft:smite",
        "殺戮",
        "增加對不死生物的傷害",
        Enchantment.Rarity.UNCOMMON,
        new HashSet<>(Arrays.asList(EnchantmentCategory.SWORD, EnchantmentCategory.WEAPON)),
        1, 5,
        false, false, true, true
    );
    
    /** 節肢殺手 */
    public static final Enchantment BANE_OF_ARTHROPODS = new Enchantment(
        "minecraft:bane_of_arthropods",
        "節肢殺手",
        "增加對節肢生物的傷害",
        Enchantment.Rarity.UNCOMMON,
        new HashSet<>(Arrays.asList(EnchantmentCategory.SWORD, EnchantmentCategory.WEAPON)),
        1, 5,
        false, false, true, true
    );
    
    /** 擊退 */
    public static final Enchantment KNOCKBACK = new Enchantment(
        "minecraft:knockback",
        "擊退",
        "增加擊退距離",
        Enchantment.Rarity.UNCOMMON,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.SWORD)),
        1, 2,
        false, false, true, true
    );
    
    /** 火焰附加 */
    public static final Enchantment FIRE_ASPECT = new Enchantment(
        "minecraft:fire_aspect",
        "火焰附加",
        "使目標著火",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.SWORD)),
        1, 2,
        false, false, true, true
    );
    
    /** 掠奪 */
    public static final Enchantment LOOTING = new Enchantment(
        "minecraft:looting",
        "掠奪",
        "增加掉落物數量",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.SWORD)),
        1, 3,
        false, false, true, true
    );
    
    /** 掃蕩 */
    public static final Enchantment SWEEPING = new Enchantment(
        "minecraft:sweeping",
        "掃蕩",
        "增加掃蕩攻擊傷害",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.SWORD)),
        1, 3,
        false, false, true, true
    );
    
    // ===== 工具附魔 =====
    
    /** 效率 */
    public static final Enchantment EFFICIENCY = new Enchantment(
        "minecraft:efficiency",
        "效率",
        "增加挖掘速度",
        Enchantment.Rarity.COMMON,
        new HashSet<>(Arrays.asList(
            EnchantmentCategory.PICKAXE, EnchantmentCategory.AXE, 
            EnchantmentCategory.SHOVEL, EnchantmentCategory.HOE
        )),
        1, 5,
        false, false, true, true
    );
    
    /** 絲綢之觸 */
    public static final Enchantment SILK_TOUCH = new Enchantment(
        "minecraft:silk_touch",
        "絲綢之觸",
        "使方塊掉落自身",
        Enchantment.Rarity.VERY_RARE,
        new HashSet<>(Arrays.asList(
            EnchantmentCategory.PICKAXE, EnchantmentCategory.AXE, 
            EnchantmentCategory.SHOVEL, EnchantmentCategory.HOE
        )),
        1, 1,
        false, false, true, true
    );
    
    /** 耐久 */
    public static final Enchantment UNBREAKING = new Enchantment(
        "minecraft:unbreaking",
        "耐久",
        "減少耐久消耗",
        Enchantment.Rarity.UNCOMMON,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BREAKABLE)),
        1, 3,
        false, false, true, true
    );
    
    /** 幸運 */
    public static final Enchantment FORTUNE = new Enchantment(
        "minecraft:fortune",
        "幸運",
        "增加方塊掉落物",
        Enchantment.Rarity.RARE,
        new HashSet<>(Arrays.asList(
            EnchantmentCategory.PICKAXE, EnchantmentCategory.AXE, 
            EnchantmentCategory.SHOVEL, EnchantmentCategory.HOE
        )),
        1, 3,
        false, false, true, true
    );
    
    // ===== 護甲附魔 =====
    
    /** 保護 */
    public static final Enchantment PROTECTION = new Enchantment(
        "minecraft:protection",
        "保護",
        "減少所有傷害",
        Enchantment.Rarity.COMMON,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.ARMOR)),
        1, 4,
        false, false, true, true
    );
    
    /** 火焰保護 */
    public static final Enchantment FIRE_PROTECTION = new Enchantment(
        "minecraft:fire_protection",
        "火焰保護",
        "減少火焰傷害",
        Enchantment.Rarity.UNCOMMON,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.ARMOR)),
        1, 4,
        false, false, true, true
    );
    
    /** 爆炸保護 */
    public static final Enchantment BLAST_PROTECTION = new Enchantment(
        "minecraft:blast_protection",
        "爆炸保護",
        "減少爆炸傷害",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.ARMOR)),
        1, 4,
        false, false, true, true
    );
    
    /** 彈道保護 */
    public static final Enchantment PROJECTILE_PROTECTION = new Enchantment(
        "minecraft:projectile_protection",
        "彈道保護",
        "減少彈道傷害",
        Enchantment.Rarity.UNCOMMON,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.ARMOR)),
        1, 4,
        false, false, true, true
    );
    
    /** 摔落保護 */
    public static final Enchantment FEATHER_FALLING = new Enchantment(
        "minecraft:feather_falling",
        "摔落保護",
        "減少摔落傷害",
        Enchantment.Rarity.UNCOMMON,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BOOTS)),
        1, 4,
        false, false, true, true
    );
    
    /** 水下呼吸 */
    public static final Enchantment RESPIRATION = new Enchantment(
        "minecraft:respiration",
        "水下呼吸",
        "延長水下呼吸時間",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.HELMET)),
        1, 3,
        false, false, true, true
    );
    
    /** 水下速掘 */
    public static final Enchantment AQUA_AFFINITY = new Enchantment(
        "minecraft:aqua_affinity",
        "水下速掘",
        "加快水下挖掘速度",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.HELMET)),
        1, 1,
        false, false, true, true
    );
    
    /** 荊棘 */
    public static final Enchantment THORNS = new Enchantment(
        "minecraft:thorns",
        "荊棘",
        "反彈傷害給攻擊者",
        Enchantment.Rarity.VERY_RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.CHESTPLATE)),
        1, 3,
        false, false, true, true
    );
    
    /** 深海探索者 */
    public static final Enchantment DEPTH_STRIDER = new Enchantment(
        "minecraft:depth_strider",
        "深海探索者",
        "加快水下移動速度",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BOOTS)),
        1, 3,
        false, false, true, true
    );
    
    /** 冰霜行者 */
    public static final Enchantment FROST_WALKER = new Enchantment(
        "minecraft:frost_walker",
        "冰霜行者",
        "在水面上生成霜冰",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BOOTS)),
        1, 2,
        false, true, false, false
    );
    
    /** 靈魂疾行 */
    public static final Enchantment SOUL_SPEED = new Enchantment(
        "minecraft:soul_speed",
        "靈魂疾行",
        "加快靈魂沙上的移動速度",
        Enchantment.Rarity.VERY_RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BOOTS)),
        1, 3,
        false, true, false, false
    );
    
    // ===== 弓附魔 =====
    
    /** 力量 */
    public static final Enchantment POWER = new Enchantment(
        "minecraft:power",
        "力量",
        "增加弓箭傷害",
        Enchantment.Rarity.COMMON,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BOW)),
        1, 5,
        false, false, true, true
    );
    
    /** 衝擊 */
    public static final Enchantment PUNCH = new Enchantment(
        "minecraft:punch",
        "衝擊",
        "增加弓箭擊退",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BOW)),
        1, 2,
        false, false, true, true
    );
    
    /** 火焰 */
    public static final Enchantment FLAME = new Enchantment(
        "minecraft:flame",
        "火焰",
        "使箭著火",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BOW)),
        1, 1,
        false, false, true, true
    );
    
    /** 無限 */
    public static final Enchantment INFINITY = new Enchantment(
        "minecraft:infinity",
        "無限",
        "射擊不消耗普通箭矢",
        Enchantment.Rarity.VERY_RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BOW)),
        1, 1,
        false, false, true, true
    );
    
    // ===== 釣魚竿附魔 =====
    
    /** 海之眷顧 */
    public static final Enchantment LUCK_OF_THE_SEA = new Enchantment(
        "minecraft:luck_of_the_sea",
        "海之眷顧",
        "增加釣魚寶物機率",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.FISHING_ROD)),
        1, 3,
        false, false, true, true
    );
    
    /** 誘餌 */
    public static final Enchantment LURE = new Enchantment(
        "minecraft:lure",
        "誘餌",
        "加快釣魚速度",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.FISHING_ROD)),
        1, 3,
        false, false, true, true
    );
    
    // ===== 詛咒附魔 =====
    
    /** 綁定詛咒 */
    public static final Enchantment BINDING_CURSE = new Enchantment(
        "minecraft:binding_curse",
        "綁定詛咒",
        "使裝備無法脫下",
        Enchantment.Rarity.VERY_RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.WEARABLE)),
        1, 1,
        true, true, false, false
    );
    
    /** 消失詛咒 */
    public static final Enchantment VANISHING_CURSE = new Enchantment(
        "minecraft:vanishing_curse",
        "消失詛咒",
        "死亡時物品消失",
        Enchantment.Rarity.VERY_RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.VANISHABLE)),
        1, 1,
        true, true, false, false
    );
    
    // ===== 其他 =====
    
    /** 經驗修補 */
    public static final Enchantment MENDING = new Enchantment(
        "minecraft:mending",
        "經驗修補",
        "用經驗修復耐久",
        Enchantment.Rarity.RARE,
        new HashSet<>(Collections.singletonList(EnchantmentCategory.BREAKABLE)),
        1, 1,
        false, true, false, false
    );
    
    /**
     * 取得所有內建附魔
     */
    public static Enchantment[] values() {
        return new Enchantment[] {
            SHARPNESS, SMITE, BANE_OF_ARTHROPODS, KNOCKBACK, FIRE_ASPECT, LOOTING, SWEEPING,
            EFFICIENCY, SILK_TOUCH, UNBREAKING, FORTUNE,
            PROTECTION, FIRE_PROTECTION, BLAST_PROTECTION, PROJECTILE_PROTECTION,
            FEATHER_FALLING, RESPIRATION, AQUA_AFFINITY, THORNS, DEPTH_STRIDER,
            FROST_WALKER, SOUL_SPEED,
            POWER, PUNCH, FLAME, INFINITY,
            LUCK_OF_THE_SEA, LURE,
            BINDING_CURSE, VANISHING_CURSE,
            MENDING
        };
    }
    
    /**
     * 根據 ID 取得附魔
     * 
     * @param id 附魔 ID
     * @return 附魔，如果找不到則返回 null
     */
    public static Enchantment byId(String id) {
        if (id == null) {
            return null;
        }
        for (Enchantment enchantment : values()) {
            if (enchantment.getId().equals(id)) {
                return enchantment;
            }
        }
        return null;
    }
}
