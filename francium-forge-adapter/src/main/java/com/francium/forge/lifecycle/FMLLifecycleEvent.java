package com.francium.forge.lifecycle;

import com.francium.forge.event.FMLEvent;

/**
 * FML 生命週期事件
 * 
 * 所有 FML 生命週期階段事件的基底類別
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public abstract class FMLLifecycleEvent extends FMLEvent {
    
    /** 當前生命週期階段 */
    private final FMLLifecycle stage;
    
    /** 模組 ID（如果是特定模組的事件） */
    private final String modId;
    
    protected FMLLifecycleEvent(FMLLifecycle stage) {
        this(stage, null);
    }
    
    protected FMLLifecycleEvent(FMLLifecycle stage, String modId) {
        super(false); // 生命週期事件不可取消
        this.stage = stage;
        this.modId = modId;
    }
    
    public FMLLifecycle getStage() {
        return stage;
    }
    
    public String getModId() {
        return modId;
    }
    
    /**
     * 建構階段事件
     */
    public static class Construction extends FMLLifecycleEvent {
        public Construction() {
            super(FMLLifecycle.CONSTRUCTION);
        }
        
        public Construction(String modId) {
            super(FMLLifecycle.CONSTRUCTION, modId);
        }
    }
    
    /**
     * 預初始化階段事件
     */
    public static class PreInitialization extends FMLLifecycleEvent {
        public PreInitialization() {
            super(FMLLifecycle.PRE_INITIALIZATION);
        }
        
        public PreInitialization(String modId) {
            super(FMLLifecycle.PRE_INITIALIZATION, modId);
        }
    }
    
    /**
     * 初始化階段事件
     */
    public static class Initialization extends FMLLifecycleEvent {
        public Initialization() {
            super(FMLLifecycle.INITIALIZATION);
        }
        
        public Initialization(String modId) {
            super(FMLLifecycle.INITIALIZATION, modId);
        }
    }
    
    /**
     * 後初始化階段事件
     */
    public static class PostInitialization extends FMLLifecycleEvent {
        public PostInitialization() {
            super(FMLLifecycle.POST_INITIALIZATION);
        }
        
        public PostInitialization(String modId) {
            super(FMLLifecycle.POST_INITIALIZATION, modId);
        }
    }
    
    /**
     * 載入完成階段事件
     */
    public static class LoadComplete extends FMLLifecycleEvent {
        public LoadComplete() {
            super(FMLLifecycle.LOAD_COMPLETE);
        }
        
        public LoadComplete(String modId) {
            super(FMLLifecycle.LOAD_COMPLETE, modId);
        }
    }
    
    /**
     * 伺服器即將啟動事件
     */
    public static class ServerAboutToStart extends FMLLifecycleEvent {
        public ServerAboutToStart() {
            super(FMLLifecycle.SERVER_ABOUT_TO_START);
        }
    }
    
    /**
     * 伺服器已啟動事件
     */
    public static class ServerStarted extends FMLLifecycleEvent {
        public ServerStarted() {
            super(FMLLifecycle.SERVER_STARTED);
        }
    }
    
    /**
     * 伺服器即將停止事件
     */
    public static class ServerStopping extends FMLLifecycleEvent {
        public ServerStopping() {
            super(FMLLifecycle.SERVER_STOPPING);
        }
    }
    
    /**
     * 伺服器已停止事件
     */
    public static class ServerStopped extends FMLLifecycleEvent {
        public ServerStopped() {
            super(FMLLifecycle.SERVER_STOPPED);
        }
    }
}
