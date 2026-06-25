package com.francium.forge.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 訂閱事件註解
 * 
 * 對應 Forge 的 @SubscribeEvent
 * 標記在方法上表示該方法是事件處理器
 * 
 * 使用方式：
 * <pre>
 * {@code
 * @SubscribeEvent
 * public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
 *     // 處理事件
 * }
 * }
 * </pre>
 * 
 * @author Francium Team
 * @since 2.5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
    
    /**
     * 事件優先級
     * 數字越小越先執行
     * 預設為 0
     */
    int priority() default 0;
    
    /**
     * 是否接收已取消的事件
     * 預設為 false
     */
    boolean receiveCanceled() default false;
}
