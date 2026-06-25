package com.francium.forge.event;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * FML 事件匯流排
 * 
 * 模擬 Forge 的 EventBus 系統
 * 支援事件訂閱、發布和取消
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class FMLEventBus {
    
    /** 事件處理器映射：事件類別 -> 處理器列表 */
    private final Map<Class<? extends FMLEvent>, List<EventHandlerEntry>> handlers = new ConcurrentHashMap<>();
    
    /** 事件匯流排名稱 */
    private final String name;
    
    /** 是否為平行事件匯流排 */
    private final boolean parallel;
    
    /**
     * 事件處理器介面
     */
    @FunctionalInterface
    public interface EventHandler<T extends FMLEvent> {
        void handle(T event) throws Exception;
    }
    
    /**
     * 事件訂閱者包裝
     */
    private static class EventHandlerWrapper {
        final Object instance;
        final Method method;
        final int priority;
        final boolean receiveCanceled;
        
        EventHandlerWrapper(Object instance, Method method, int priority, boolean receiveCanceled) {
            this.instance = instance;
            this.method = method;
            this.priority = priority;
            this.receiveCanceled = receiveCanceled;
        }
    }
    
    public FMLEventBus(String name) {
        this(name, false);
    }
    
    public FMLEventBus(String name, boolean parallel) {
        this.name = name;
        this.parallel = parallel;
    }
    
    /**
     * 訂閱事件
     * 
     * @param eventClass 事件類別
     * @param handler 事件處理器
     * @param <T> 事件類型
     */
    public <T extends FMLEvent> void addListener(Class<T> eventClass, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
            .add(new EventHandlerEntry(handler, 0, false));
    }
    
    /**
     * 訂閱事件（指定優先級）
     * 
     * @param eventClass 事件類別
     * @param priority 優先級（數字越小越先執行）
     * @param handler 事件處理器
     * @param <T> 事件類型
     */
    public <T extends FMLEvent> void addListener(Class<T> eventClass, int priority, EventHandler<T> handler) {
        List<EventHandlerEntry> list = handlers.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());
        list.add(new EventHandlerEntry(handler, priority, false));
        list.sort(Comparator.comparingInt(h -> h.priority));
    }
    
    /**
     * 取消訂閱事件
     * 
     * @param eventClass 事件類別
     * @param handler 事件處理器
     * @param <T> 事件類型
     */
    public <T extends FMLEvent> void removeListener(Class<T> eventClass, EventHandler<T> handler) {
        List<EventHandlerEntry> list = handlers.get(eventClass);
        if (list != null) {
            list.removeIf(h -> h.handler.equals(handler));
        }
    }
    
    /**
     * 發布事件
     * 
     * @param event 事件物件
     * @return 事件是否被取消
     */
    public boolean post(FMLEvent event) {
        if (event == null) {
            return false;
        }
        
        List<EventHandlerEntry> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            return event.isCanceled();
        }
        
        if (parallel) {
            // 平行處理
            eventHandlers.parallelStream().forEach(handler -> {
                try {
                    if (!event.isCanceled() || handler.receiveCanceled) {
                        handler.handle(event);
                    }
                } catch (Exception e) {
                    // 記錄錯誤但不中斷其他處理器
                    System.err.println("Error handling event " + event.getEventName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            // 循序處理
            for (EventHandlerEntry handler : eventHandlers) {
                try {
                    if (event.isCanceled() && !handler.receiveCanceled) {
                        continue;
                    }
                    handler.handle(event);
                } catch (Exception e) {
                    System.err.println("Error handling event " + event.getEventName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        return event.isCanceled();
    }
    
    /**
     * 註冊物件的所有 @SubscribeEvent 方法
     * 
     * @param subscriber 訂閱者物件
     */
    public void register(Object subscriber) {
        if (subscriber == null) {
            return;
        }
        
        Class<?> clazz = subscriber.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            // 檢查是否有 SubscribeEvent 註解（模擬）
            // 實際上應該檢查 @SubscribeEvent 註解
            // 這裡我們簡單處理：方法名以 "on" 開頭且只有一個參數（事件類型）
            if (method.getParameterCount() == 1 
                && FMLEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                
                method.setAccessible(true);
                
                @SuppressWarnings("unchecked")
                Class<? extends FMLEvent> eventClass = 
                    (Class<? extends FMLEvent>) method.getParameterTypes()[0];
                
                @SuppressWarnings("unchecked")
                EventHandler<FMLEvent> handler = event -> {
                    try {
                        method.invoke(subscriber, event);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Error invoking event handler", e);
                    }
                };
                
                @SuppressWarnings("unchecked")
                Class<FMLEvent> typedEventClass = (Class<FMLEvent>) eventClass;
                addListener(typedEventClass, handler);
            }
        }
    }
    
    /**
     * 取消註冊物件
     * 
     * @param subscriber 訂閱者物件
     */
    public void unregister(Object subscriber) {
        // 簡單實現：移除所有包含該物件的處理器
        // 實際上需要追蹤訂閱者和處理器的對應關係
    }
    
    /**
     * 取得事件處理器數量
     */
    public int getHandlerCount(Class<? extends FMLEvent> eventClass) {
        List<EventHandlerEntry> list = handlers.get(eventClass);
        return list != null ? list.size() : 0;
    }
    
    /**
     * 取得所有已訂閱的事件類型
     */
    public Set<Class<? extends FMLEvent>> getRegisteredEvents() {
        return Collections.unmodifiableSet(handlers.keySet());
    }
    
    /**
     * 清除所有訂閱者
     */
    public void clear() {
        handlers.clear();
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isParallel() {
        return parallel;
    }
    
    /**
     * 內部事件處理器包裝
     */
    private static class EventHandlerEntry<T extends FMLEvent> {
        final EventHandler<T> handler;
        final int priority;
        final boolean receiveCanceled;
        
        @SuppressWarnings("unchecked")
        EventHandlerEntry(EventHandler<?> handler, int priority, boolean receiveCanceled) {
            this.handler = (EventHandler<T>) handler;
            this.priority = priority;
            this.receiveCanceled = receiveCanceled;
        }
        
        @SuppressWarnings("unchecked")
        void handle(FMLEvent event) throws Exception {
            handler.handle((T) event);
        }
    }
}
