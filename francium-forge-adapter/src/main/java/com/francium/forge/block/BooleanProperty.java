package com.francium.forge.block;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * 布林值屬性
 * 
 * 表示方塊狀態中的布林值屬性（如是否打開、是否充能等）
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class BooleanProperty extends Property<Boolean> {
    
    /** 所有可能的值 */
    private static final Collection<Boolean> VALUES = Collections.unmodifiableList(
        Arrays.asList(false, true)
    );
    
    /**
     * 建立布林值屬性
     * 
     * @param name 屬性名稱
     */
    protected BooleanProperty(String name) {
        super(name, Boolean.class);
    }
    
    /**
     * 建立布林值屬性
     * 
     * @param name 屬性名稱
     * @return 布林值屬性
     */
    public static BooleanProperty create(String name) {
        return new BooleanProperty(name);
    }
    
    @Override
    public Collection<Boolean> getPossibleValues() {
        return VALUES;
    }
    
    @Override
    public String getName(Boolean value) {
        return value.toString();
    }
    
    @Override
    public Boolean getValue(String value) {
        if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean value: " + value);
    }
}
