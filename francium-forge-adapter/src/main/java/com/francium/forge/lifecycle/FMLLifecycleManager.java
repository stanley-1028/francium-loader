package com.francium.forge.lifecycle;

import com.francium.forge.adapter.ForgeModMetadata;
import com.francium.forge.event.FMLEventBus;

import java.util.*;

/**
 * FML 生命週期管理器
 * 
 * 管理 Forge 模組的生命週期，模擬 Forge Mod Loader 的載入流程
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class FMLLifecycleManager {
    
    /** 事件匯流排 */
    private final FMLEventBus eventBus;
    
    /** 當前生命週期階段 */
    private FMLLifecycle currentStage = FMLLifecycle.CONSTRUCTION;
    
    /** 已載入的模組 */
    private final Map<String, ForgeModContainer> modContainers = new LinkedHashMap<>();
    
    /** 生命週期監聽器 */
    private final List<LifecycleListener> listeners = new ArrayList<>();
    
    /**
     * 生命週期監聽器介面
     */
    public interface LifecycleListener {
        void onStageChange(FMLLifecycle previous, FMLLifecycle current);
        void onModLoaded(String modId, FMLLifecycle stage);
        void onError(String modId, FMLLifecycle stage, Throwable error);
    }
    
    public FMLLifecycleManager() {
        this(new FMLEventBus("FML"));
    }
    
    public FMLLifecycleManager(FMLEventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * 註冊模組
     * 
     * @param metadata 模組中繼資料
     * @return 模組容器
     */
    public ForgeModContainer registerMod(ForgeModMetadata metadata) {
        if (metadata == null || !metadata.isValid()) {
            throw new IllegalArgumentException("Invalid mod metadata");
        }
        
        String modId = metadata.getModId();
        if (modContainers.containsKey(modId)) {
            throw new IllegalStateException("Mod already registered: " + modId);
        }
        
        ForgeModContainer container = new ForgeModContainer(metadata);
        modContainers.put(modId, container);
        
        return container;
    }
    
    /**
     * 開始載入流程
     * 
     * 依次執行所有生命週期階段
     */
    public void startLoading() {
        // 確保在 CONSTRUCTION 階段
        if (currentStage != FMLLifecycle.CONSTRUCTION) {
            throw new IllegalStateException("Loading already started");
        }
        
        // 執行各個生命週期階段
        advanceTo(FMLLifecycle.CONSTRUCTION);
        advanceTo(FMLLifecycle.PRE_INITIALIZATION);
        advanceTo(FMLLifecycle.INITIALIZATION);
        advanceTo(FMLLifecycle.POST_INITIALIZATION);
        advanceTo(FMLLifecycle.LOAD_COMPLETE);
    }
    
    /**
     * 前進到指定的生命週期階段
     * 
     * @param targetStage 目標階段
     */
    public void advanceTo(FMLLifecycle targetStage) {
        if (targetStage.ordinal() <= currentStage.ordinal()) {
            return; // 已經在這個階段或更後面
        }
        
        // 逐步前進到目標階段
        while (currentStage != targetStage) {
            FMLLifecycle nextStage = currentStage.next();
            if (nextStage == currentStage) {
                break; // 無法再前進
            }
            
            FMLLifecycle previous = currentStage;
            currentStage = nextStage;
            
            // 觸發階段變更事件
            fireStageChange(previous, currentStage);
            
            // 對每個模組執行該階段
            for (ForgeModContainer container : modContainers.values()) {
                try {
                    container.advanceTo(currentStage);
                    fireModLoaded(container.getModId(), currentStage);
                } catch (Exception e) {
                    fireError(container.getModId(), currentStage, e);
                    container.setError(e);
                }
            }
            
            // 發布廣域生命週期事件
            fireLifecycleEvent(currentStage);
        }
    }
    
    /**
     * 發布生命週期事件到事件匯流排
     */
    private void fireLifecycleEvent(FMLLifecycle stage) {
        FMLLifecycleEvent event = createLifecycleEvent(stage);
        if (event != null) {
            eventBus.post(event);
        }
    }
    
    /**
     * 建立對應階段的生命週期事件
     */
    private FMLLifecycleEvent createLifecycleEvent(FMLLifecycle stage) {
        switch (stage) {
            case CONSTRUCTION:
                return new FMLLifecycleEvent.Construction();
            case PRE_INITIALIZATION:
                return new FMLLifecycleEvent.PreInitialization();
            case INITIALIZATION:
                return new FMLLifecycleEvent.Initialization();
            case POST_INITIALIZATION:
                return new FMLLifecycleEvent.PostInitialization();
            case LOAD_COMPLETE:
                return new FMLLifecycleEvent.LoadComplete();
            case SERVER_ABOUT_TO_START:
                return new FMLLifecycleEvent.ServerAboutToStart();
            case SERVER_STARTED:
                return new FMLLifecycleEvent.ServerStarted();
            case SERVER_STOPPING:
                return new FMLLifecycleEvent.ServerStopping();
            case SERVER_STOPPED:
                return new FMLLifecycleEvent.ServerStopped();
            default:
                return null;
        }
    }
    
    /**
     * 觸發階段變更
     */
    private void fireStageChange(FMLLifecycle previous, FMLLifecycle current) {
        for (LifecycleListener listener : listeners) {
            try {
                listener.onStageChange(previous, current);
            } catch (Exception e) {
                // 忽略監聽器錯誤
            }
        }
    }
    
    /**
     * 觸發模組載入事件
     */
    private void fireModLoaded(String modId, FMLLifecycle stage) {
        for (LifecycleListener listener : listeners) {
            try {
                listener.onModLoaded(modId, stage);
            } catch (Exception e) {
                // 忽略監聽器錯誤
            }
        }
    }
    
    /**
     * 觸發錯誤事件
     */
    private void fireError(String modId, FMLLifecycle stage, Throwable error) {
        for (LifecycleListener listener : listeners) {
            try {
                listener.onError(modId, stage, error);
            } catch (Exception e) {
                // 忽略監聽器錯誤
            }
        }
    }
    
    /**
     * 添加生命週期監聽器
     */
    public void addLifecycleListener(LifecycleListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除生命週期監聽器
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 取得當前生命週期階段
     */
    public FMLLifecycle getCurrentStage() {
        return currentStage;
    }
    
    /**
     * 取得模組容器
     */
    public ForgeModContainer getModContainer(String modId) {
        return modContainers.get(modId);
    }
    
    /**
     * 取得所有已註冊的模組
     */
    public Collection<ForgeModContainer> getModContainers() {
        return Collections.unmodifiableCollection(modContainers.values());
    }
    
    /**
     * 取得已載入的模組數量
     */
    public int getModCount() {
        return modContainers.size();
    }
    
    /**
     * 取得事件匯流排
     */
    public FMLEventBus getEventBus() {
        return eventBus;
    }
    
    /**
     * 檢查載入是否完成
     */
    public boolean isLoadingComplete() {
        return currentStage == FMLLifecycle.LOAD_COMPLETE 
            || currentStage.ordinal() > FMLLifecycle.LOAD_COMPLETE.ordinal();
    }
    
    /**
     * 取得有錯誤的模組
     */
    public List<ForgeModContainer> getErroredMods() {
        List<ForgeModContainer> errored = new ArrayList<>();
        for (ForgeModContainer container : modContainers.values()) {
            if (container.hasError()) {
                errored.add(container);
            }
        }
        return errored;
    }
}
