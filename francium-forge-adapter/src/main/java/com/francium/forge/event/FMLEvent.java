package com.francium.forge.event;

/**
 * FML 事件基底類別
 * 
 * 所有 Forge/FML 事件的基底類別
 * 模擬 Forge 的 Event 系統
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public abstract class FMLEvent {
    
    /** 事件是否已被取消 */
    private boolean canceled = false;
    
    /** 事件是否可取消 */
    private final boolean cancelable;
    
    /** 事件結果（用於可設定結果的事件） */
    private Result result = Result.DEFAULT;
    
    /**
     * 事件結果列舉
     */
    public enum Result {
        /** 預設行為 */
        DEFAULT,
        /** 允許 / 成功 */
        ALLOW,
        /** 拒絕 / 失敗 */
        DENY
    }
    
    protected FMLEvent() {
        this(false);
    }
    
    protected FMLEvent(boolean cancelable) {
        this.cancelable = cancelable;
    }
    
    /**
     * 檢查事件是否可取消
     */
    public boolean isCancelable() {
        return cancelable;
    }
    
    /**
     * 檢查事件是否已被取消
     */
    public boolean isCanceled() {
        return canceled;
    }
    
    /**
     * 設定事件是否取消
     * 
     * @throws IllegalStateException 如果事件不可取消
     */
    public void setCanceled(boolean canceled) {
        if (!cancelable) {
            throw new IllegalStateException("Event is not cancelable: " + getClass().getName());
        }
        this.canceled = canceled;
    }
    
    /**
     * 取得事件結果
     */
    public Result getResult() {
        return result;
    }
    
    /**
     * 設定事件結果
     */
    public void setResult(Result result) {
        this.result = result;
    }
    
    /**
     * 取得事件名稱
     */
    public String getEventName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public String toString() {
        return getEventName() + (canceled ? " (canceled)" : "");
    }
}
