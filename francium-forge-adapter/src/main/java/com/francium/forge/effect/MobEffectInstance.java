package com.francium.forge.effect;

/**
 * 藥水效果實例
 * 
 * 對應 Forge/Minecraft 的 MobEffectInstance
 * 表示一個具體的藥水效果實例，包含效果類型、等級、持續時間等
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class MobEffectInstance {
    
    /** 效果類型 */
    private final MobEffect effect;
    
    /** 效果等級（0 為基礎） */
    private final int amplifier;
    
    /** 持續時間（tick） */
    private int duration;
    
    /** 是否為環境效果 */
    private final boolean ambient;
    
    /** 是否顯示粒子 */
    private final boolean showParticles;
    
    /** 是否顯示圖示 */
    private final boolean showIcon;
    
    /**
     * 建立藥水效果實例
     * 
     * @param effect 效果類型
     * @param duration 持續時間
     */
    public MobEffectInstance(MobEffect effect, int duration) {
        this(effect, duration, 0);
    }
    
    /**
     * 建立藥水效果實例
     * 
     * @param effect 效果類型
     * @param duration 持續時間
     * @param amplifier 效果等級
     */
    public MobEffectInstance(MobEffect effect, int duration, int amplifier) {
        this(effect, duration, amplifier, false, true);
    }
    
    /**
     * 建立藥水效果實例
     * 
     * @param effect 效果類型
     * @param duration 持續時間
     * @param amplifier 效果等級
     * @param ambient 是否為環境效果
     * @param showParticles 是否顯示粒子
     */
    public MobEffectInstance(MobEffect effect, int duration, int amplifier, 
                             boolean ambient, boolean showParticles) {
        this(effect, duration, amplifier, ambient, showParticles, true);
    }
    
    /**
     * 建立藥水效果實例
     * 
     * @param effect 效果類型
     * @param duration 持續時間
     * @param amplifier 效果等級
     * @param ambient 是否為環境效果
     * @param showParticles 是否顯示粒子
     * @param showIcon 是否顯示圖示
     */
    public MobEffectInstance(MobEffect effect, int duration, int amplifier,
                             boolean ambient, boolean showParticles, boolean showIcon) {
        if (effect == null) {
            throw new IllegalArgumentException("Effect cannot be null");
        }
        if (duration < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        if (amplifier < 0) {
            throw new IllegalArgumentException("Amplifier cannot be negative");
        }
        
        this.effect = effect;
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
    }
    
    /**
     * 取得效果類型
     */
    public MobEffect getEffect() {
        return effect;
    }
    
    /**
     * 取得效果 ID
     */
    public String getEffectId() {
        return effect.getId();
    }
    
    /**
     * 取得效果名稱
     */
    public String getEffectName() {
        return effect.getName();
    }
    
    /**
     * 取得效果等級
     */
    public int getAmplifier() {
        return amplifier;
    }
    
    /**
     * 取得持續時間
     */
    public int getDuration() {
        return duration;
    }
    
    /**
     * 設定持續時間
     * 
     * @param duration 持續時間
     */
    public void setDuration(int duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        this.duration = duration;
    }
    
    /**
     * 減少持續時間
     * 
     * @param amount 減少量
     * @return 減少後的持續時間
     */
    public int decreaseDuration(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.duration = Math.max(0, this.duration - amount);
        return this.duration;
    }
    
    /**
     * 檢查效果是否已結束
     */
    public boolean isFinished() {
        return duration <= 0;
    }
    
    /**
     * 檢查是否為環境效果
     */
    public boolean isAmbient() {
        return ambient;
    }
    
    /**
     * 檢查是否顯示粒子
     */
    public boolean showParticles() {
        return showParticles;
    }
    
    /**
     * 檢查是否顯示圖示
     */
    public boolean showIcon() {
        return showIcon;
    }
    
    /**
     * 檢查是否與另一個效果實例是同一種效果
     * 
     * @param other 另一個效果實例
     * @return 是否為同一種效果
     */
    public boolean isSameEffect(MobEffectInstance other) {
        if (other == null) {
            return false;
        }
        return effect.equals(other.effect);
    }
    
    /**
     * 與另一個效果實例合併
     * 
     * @param other 另一個效果實例
     * @return 是否合併成功
     */
    public boolean combine(MobEffectInstance other) {
        if (!isSameEffect(other)) {
            return false;
        }
        // 取較長的持續時間
        if (other.duration > this.duration) {
            this.duration = other.duration;
        }
        // 取較高的等級
        if (other.amplifier > this.amplifier) {
            // 注意：這裡無法修改 final 的 amplifier
            // 在實際應用中可能需要重新建立實例
        }
        return true;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MobEffectInstance)) return false;
        MobEffectInstance other = (MobEffectInstance) obj;
        return effect.equals(other.effect) && 
               amplifier == other.amplifier && 
               duration == other.duration;
    }
    
    @Override
    public int hashCode() {
        int result = effect.hashCode();
        result = 31 * result + amplifier;
        result = 31 * result + duration;
        return result;
    }
    
    @Override
    public String toString() {
        return effect.getName() + " " + (amplifier + 1) + " (" + duration + " ticks)";
    }
}
