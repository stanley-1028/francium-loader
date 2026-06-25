package com.francium.forge.dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 僅在特定側邊存在的標註
 * 
 * 對應 Forge 的 @OnlyIn 註解
 * 用於標示類別、方法或欄位僅在特定側邊（用戶端/伺服器端）存在
 * 
 * 注意：這是一個標記註解，實際的類別載入過濾需要額外實作
 * 
 * @author Francium Team
 * @since 2.5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface OnlyIn {
    
    /**
     * 指定的側邊
     */
    Dist value();
    
    /**
     * 備註
     */
    String comment() default "";
}
