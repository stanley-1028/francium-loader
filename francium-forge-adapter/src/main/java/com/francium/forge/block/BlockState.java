package com.francium.forge.block;

import java.util.*;

/**
 * 方塊狀態
 * 
 * 對應 Forge/Minecraft 的 BlockState
 * 表示方塊的一個特定狀態，包含所有屬性的值
 * 
 * 每個方塊可以有多個狀態（如不同方向、是否打開等）
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class BlockState {
    
    /** 方塊 ID */
    private final String blockId;
    
    /** 所有屬性的值：屬性 -> 值 */
    private final Map<Property<?>, Comparable<?>> values;
    
    /** 狀態的字串表示（快取） */
    private volatile String stringCache;
    
    /**
     * 建立方塊狀態
     * 
     * @param blockId 方塊 ID
     * @param properties 屬性集合
     */
    public BlockState(String blockId, Collection<Property<?>> properties) {
        if (blockId == null || blockId.isEmpty()) {
            throw new IllegalArgumentException("Block ID cannot be null or empty");
        }
        if (properties == null) {
            throw new IllegalArgumentException("Properties cannot be null");
        }
        
        this.blockId = blockId;
        this.values = new HashMap<>();
        
        // 初始化所有屬性為預設值（第一個值）
        for (Property<?> property : properties) {
            Comparable<?> defaultValue = property.getPossibleValues().iterator().next();
            this.values.put(property, defaultValue);
        }
    }
    
    /**
     * 私有建構子（用於複製）
     */
    private BlockState(String blockId, Map<Property<?>, Comparable<?>> values) {
        this.blockId = blockId;
        this.values = new HashMap<>(values);
    }
    
    /**
     * 取得方塊 ID
     */
    public String getBlockId() {
        return blockId;
    }
    
    /**
     * 取得指定屬性的值
     * 
     * @param property 屬性
     * @param <T> 屬性值的類型
     * @return 屬性的值
     * @throws IllegalArgumentException 如果屬性不存在
     */
    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T getValue(Property<T> property) {
        if (property == null) {
            throw new IllegalArgumentException("Property cannot be null");
        }
        Comparable<?> value = values.get(property);
        if (value == null) {
            throw new IllegalArgumentException("Property not found: " + property.getName());
        }
        return (T) value;
    }
    
    /**
     * 設定指定屬性的值，傳回新的狀態
     * 
     * @param property 屬性
     * @param value 新的值
     * @param <T> 屬性值的類型
     * @return 新的方塊狀態
     * @throws IllegalArgumentException 如果屬性不存在或值無效
     */
    public <T extends Comparable<T>> BlockState setValue(Property<T> property, T value) {
        if (property == null) {
            throw new IllegalArgumentException("Property cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (!values.containsKey(property)) {
            throw new IllegalArgumentException("Property not found: " + property.getName());
        }
        if (!property.getPossibleValues().contains(value)) {
            throw new IllegalArgumentException(
                "Invalid value " + value + " for property " + property.getName()
            );
        }
        
        BlockState newState = new BlockState(blockId, values);
        newState.values.put(property, value);
        return newState;
    }
    
    /**
     * 取得所有屬性
     */
    public Collection<Property<?>> getProperties() {
        return Collections.unmodifiableSet(values.keySet());
    }
    
    /**
     * 取得所有屬性的值映射
     */
    public Map<Property<?>, Comparable<?>> getValues() {
        return Collections.unmodifiableMap(values);
    }
    
    /**
     * 取得可能的狀態數量
     */
    public int getStateCount() {
        int count = 1;
        for (Property<?> property : values.keySet()) {
            count *= property.getPossibleValues().size();
        }
        return count;
    }
    
    /**
     * 取得所有可能的狀態
     */
    public List<BlockState> getAllStates() {
        List<BlockState> states = new ArrayList<>();
        generateStates(new ArrayList<>(values.keySet()), 0, this, states);
        return states;
    }
    
    /**
     * 遞迴產生所有可能的狀態
     */
    @SuppressWarnings("unchecked")
    private void generateStates(List<Property<?>> properties, int index, 
                                 BlockState current, List<BlockState> states) {
        if (index >= properties.size()) {
            states.add(current);
            return;
        }
        
        Property property = properties.get(index);
        for (Object value : property.getPossibleValues()) {
            BlockState newState = current.setValue(property, (Comparable) value);
            generateStates(properties, index + 1, newState, states);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BlockState)) return false;
        BlockState other = (BlockState) obj;
        return blockId.equals(other.blockId) && values.equals(other.values);
    }
    
    @Override
    public int hashCode() {
        return 31 * blockId.hashCode() + values.hashCode();
    }
    
    @Override
    public String toString() {
        if (stringCache == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(blockId);
            
            if (!values.isEmpty()) {
                sb.append('[');
                boolean first = true;
                List<Property<?>> sortedProperties = new ArrayList<>(values.keySet());
                sortedProperties.sort(Comparator.comparing(Property::getName));
                
                for (Property<?> property : sortedProperties) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    sb.append(property.getName());
                    sb.append('=');
                    sb.append(getValueString(property));
                }
                sb.append(']');
            }
            
            stringCache = sb.toString();
        }
        return stringCache;
    }
    
    /**
     * 取得屬性值的字串表示
     */
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> String getValueString(Property<T> property) {
        T value = (T) values.get(property);
        return property.getName(value);
    }
}
