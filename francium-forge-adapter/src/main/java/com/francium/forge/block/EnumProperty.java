package com.francium.forge.block;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * 列舉屬性
 * 
 * 表示方塊狀態中的列舉屬性（如方向、形狀等）
 * 
 * @param <T> 列舉類型
 * @author Francium Team
 * @since 2.5.0
 */
public class EnumProperty<T extends Enum<T>> extends Property<T> {
    
    /** 所有可能的值 */
    private final T[] values;
    
    /**
     * 建立列舉屬性
     * 
     * @param name 屬性名稱
     * @param clazz 列舉類型
     */
    protected EnumProperty(String name, Class<T> clazz) {
        super(name, clazz);
        this.values = clazz.getEnumConstants();
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Enum must have at least one constant");
        }
    }
    
    /**
     * 建立列舉屬性
     * 
     * @param name 屬性名稱
     * @param clazz 列舉類型
     * @param <T> 列舉類型
     * @return 列舉屬性
     */
    public static <T extends Enum<T>> EnumProperty<T> create(String name, Class<T> clazz) {
        return new EnumProperty<>(name, clazz);
    }
    
    @Override
    public Collection<T> getPossibleValues() {
        return Collections.unmodifiableList(Arrays.asList(values));
    }
    
    @Override
    public String getName(T value) {
        return value.name().toLowerCase();
    }
    
    @Override
    public T getValue(String value) {
        for (T enumValue : values) {
            if (enumValue.name().equalsIgnoreCase(value)) {
                return enumValue;
            }
        }
        throw new IllegalArgumentException("Invalid enum value: " + value);
    }
}
