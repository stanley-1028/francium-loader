package com.francium.forge.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 整數屬性
 * 
 * 表示方塊狀態中的整數屬性（如年齡、階段等）
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class IntegerProperty extends Property<Integer> {
    
    /** 最小值 */
    private final int min;
    
    /** 最大值 */
    private final int max;
    
    /** 所有可能的值 */
    private final List<Integer> values;
    
    /**
     * 建立整數屬性
     * 
     * @param name 屬性名稱
     * @param min 最小值
     * @param max 最大值
     */
    protected IntegerProperty(String name, int min, int max) {
        super(name, Integer.class);
        if (min > max) {
            throw new IllegalArgumentException("Min cannot be greater than max");
        }
        this.min = min;
        this.max = max;
        
        List<Integer> valueList = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            valueList.add(i);
        }
        this.values = Collections.unmodifiableList(valueList);
    }
    
    /**
     * 建立整數屬性
     * 
     * @param name 屬性名稱
     * @param min 最小值
     * @param max 最大值
     * @return 整數屬性
     */
    public static IntegerProperty create(String name, int min, int max) {
        return new IntegerProperty(name, min, max);
    }
    
    /**
     * 取得最小值
     */
    public int getMin() {
        return min;
    }
    
    /**
     * 取得最大值
     */
    public int getMax() {
        return max;
    }
    
    @Override
    public Collection<Integer> getPossibleValues() {
        return values;
    }
    
    @Override
    public String getName(Integer value) {
        return value.toString();
    }
    
    @Override
    public Integer getValue(String value) {
        try {
            int intValue = Integer.parseInt(value);
            if (intValue < min || intValue > max) {
                throw new IllegalArgumentException(
                    "Value " + value + " out of range [" + min + ", " + max + "]"
                );
            }
            return intValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value: " + value, e);
        }
    }
}
