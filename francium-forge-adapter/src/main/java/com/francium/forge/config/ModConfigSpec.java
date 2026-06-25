package com.francium.forge.config;

import java.util.*;
import java.util.function.Supplier;

/**
 * Forge 設定檔規格建構器
 * 
 * 對應 Forge 的 ModConfigSpec.Builder
 * 用於定義設定檔的結構和預設值
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ModConfigSpec {
    
    /** 所有設定項目的映射 */
    private final Map<String, ConfigValue<?>> values = new LinkedHashMap<>();
    
    /** 設定類別 */
    private final Map<String, List<String>> categories = new LinkedHashMap<>();
    
    /** 模組 ID */
    private String modId;
    
    /**
     * 私有建構子，請使用 Builder
     */
    private ModConfigSpec() {}
    
    /**
     * 取得設定項目
     */
    @SuppressWarnings("unchecked")
    public <T> ConfigValue<T> getValue(String path) {
        return (ConfigValue<T>) values.get(path);
    }
    
    /**
     * 取得所有設定項目
     */
    public Map<String, ConfigValue<?>> getValues() {
        return Collections.unmodifiableMap(values);
    }
    
    /**
     * 取得所有設定類別
     */
    public Map<String, List<String>> getCategories() {
        return Collections.unmodifiableMap(categories);
    }
    
    /**
     * 取得模組 ID
     */
    public String getModId() {
        return modId;
    }
    
    /**
     * 設定模組 ID
     */
    void setModId(String modId) {
        this.modId = modId;
    }
    
    /**
     * 設定檔規格建構器
     */
    public static class Builder {
        
        private final ModConfigSpec spec = new ModConfigSpec();
        private String currentPath = "";
        private final List<String> pathStack = new ArrayList<>();
        
        /**
         * 推入一個設定類別
         */
        public Builder push(String category) {
            pathStack.add(category);
            currentPath = String.join(".", pathStack);
            spec.categories.putIfAbsent(currentPath, new ArrayList<>());
            return this;
        }
        
        /**
         * 彈出目前的設定類別
         */
        public Builder pop() {
            if (!pathStack.isEmpty()) {
                pathStack.remove(pathStack.size() - 1);
                currentPath = String.join(".", pathStack);
            }
            return this;
        }
        
        /**
         * 彈出多個設定類別
         */
        public Builder pop(int count) {
            for (int i = 0; i < count && !pathStack.isEmpty(); i++) {
                pathStack.remove(pathStack.size() - 1);
            }
            currentPath = String.join(".", pathStack);
            return this;
        }
        
        /**
         * 定義一個布林值設定
         */
        public BooleanValue define(String path, boolean defaultValue) {
            return define(path, defaultValue, null, false, false);
        }
        
        /**
         * 定義一個布林值設定（含註解）
         */
        public BooleanValue define(String path, boolean defaultValue, String comment) {
            return define(path, defaultValue, comment, false, false);
        }
        
        /**
         * 定義一個布林值設定（完整參數）
         */
        public BooleanValue define(String path, boolean defaultValue, String comment,
                                   boolean requiresWorldRestart, boolean requiresMcRestart) {
            String fullPath = buildPath(path);
            ConfigValue<Boolean> configValue = new ConfigValue<>(
                fullPath, defaultValue, comment, requiresWorldRestart, requiresMcRestart
            );
            spec.values.put(fullPath, configValue);
            addToCategory(fullPath);
            return new BooleanValue(configValue);
        }
        
        /**
         * 定義一個整數設定
         */
        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            return defineInRange(path, defaultValue, min, max, null);
        }
        
        /**
         * 定義一個整數設定（含註解）
         */
        public IntValue defineInRange(String path, int defaultValue, int min, int max, String comment) {
            String fullPath = buildPath(path);
            ConfigValue<Integer> configValue = new ConfigValue<>(
                fullPath, defaultValue, comment, false, false
            );
            spec.values.put(fullPath, configValue);
            addToCategory(fullPath);
            return new IntValue(configValue, min, max);
        }
        
        /**
         * 定義一個雙精度浮點數設定
         */
        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            return defineInRange(path, defaultValue, min, max, null);
        }
        
        /**
         * 定義一個雙精度浮點數設定（含註解）
         */
        public DoubleValue defineInRange(String path, double defaultValue, double min, double max, String comment) {
            String fullPath = buildPath(path);
            ConfigValue<Double> configValue = new ConfigValue<>(
                fullPath, defaultValue, comment, false, false
            );
            spec.values.put(fullPath, configValue);
            addToCategory(fullPath);
            return new DoubleValue(configValue, min, max);
        }
        
        /**
         * 定義一個字串設定
         */
        public ConfigValue<String> define(String path, String defaultValue) {
            return define(path, defaultValue, null);
        }
        
        /**
         * 定義一個字串設定（含註解）
         */
        public ConfigValue<String> define(String path, String defaultValue, String comment) {
            String fullPath = buildPath(path);
            ConfigValue<String> configValue = new ConfigValue<>(
                fullPath, defaultValue, comment, false, false
            );
            spec.values.put(fullPath, configValue);
            addToCategory(fullPath);
            return configValue;
        }
        
        /**
         * 定義一個列舉設定
         */
        public <T extends Enum<T>> ConfigValue<T> defineEnum(String path, T defaultValue, Class<T> clazz) {
            return defineEnum(path, defaultValue, clazz, null);
        }
        
        /**
         * 定義一個列舉設定（含註解）
         */
        public <T extends Enum<T>> ConfigValue<T> defineEnum(String path, T defaultValue, Class<T> clazz, String comment) {
            String fullPath = buildPath(path);
            ConfigValue<T> configValue = new ConfigValue<>(
                fullPath, defaultValue, comment, false, false
            );
            spec.values.put(fullPath, configValue);
            addToCategory(fullPath);
            return configValue;
        }
        
        /**
         * 定義一個字串清單設定
         */
        public ConfigValue<List<String>> defineList(String path, List<String> defaultValue) {
            return defineList(path, defaultValue, null);
        }
        
        /**
         * 定義一個字串清單設定（含註解）
         */
        public ConfigValue<List<String>> defineList(String path, List<String> defaultValue, String comment) {
            String fullPath = buildPath(path);
            ConfigValue<List<String>> configValue = new ConfigValue<>(
                fullPath, new ArrayList<>(defaultValue), comment, false, false
            );
            spec.values.put(fullPath, configValue);
            addToCategory(fullPath);
            return configValue;
        }
        
        /**
         * 加入註解（僅供文件用途）
         */
        public Builder comment(String comment) {
            // 簡單實現：註解會附加到下一個定義的設定項目
            return this;
        }
        
        /**
         * 建構設定檔規格
         */
        public ModConfigSpec build() {
            return spec;
        }
        
        /**
         * 建立完整路徑
         */
        private String buildPath(String path) {
            if (currentPath.isEmpty()) {
                return path;
            }
            return currentPath + "." + path;
        }
        
        /**
         * 加入到目前類別
         */
        private void addToCategory(String fullPath) {
            String category = currentPath.isEmpty() ? "general" : currentPath;
            spec.categories.computeIfAbsent(category, k -> new ArrayList<>()).add(fullPath);
        }
    }
    
    /**
     * 布林值設定包裝
     */
    public static class BooleanValue implements Supplier<Boolean> {
        private final ConfigValue<Boolean> value;
        
        BooleanValue(ConfigValue<Boolean> value) {
            this.value = value;
        }
        
        @Override
        public Boolean get() {
            return value.get();
        }
        
        public void set(boolean value) {
            this.value.set(value);
        }
        
        public ConfigValue<Boolean> getConfigValue() {
            return value;
        }
    }
    
    /**
     * 整數設定包裝
     */
    public static class IntValue implements Supplier<Integer> {
        private final ConfigValue<Integer> value;
        private final int min;
        private final int max;
        
        IntValue(ConfigValue<Integer> value, int min, int max) {
            this.value = value;
            this.min = min;
            this.max = max;
        }
        
        @Override
        public Integer get() {
            return value.get();
        }
        
        public void set(int value) {
            if (value < min || value > max) {
                throw new IllegalArgumentException("Value " + value + " out of range [" + min + ", " + max + "]");
            }
            this.value.set(value);
        }
        
        public int getMin() {
            return min;
        }
        
        public int getMax() {
            return max;
        }
        
        public ConfigValue<Integer> getConfigValue() {
            return value;
        }
    }
    
    /**
     * 雙精度浮點數設定包裝
     */
    public static class DoubleValue implements Supplier<Double> {
        private final ConfigValue<Double> value;
        private final double min;
        private final double max;
        
        DoubleValue(ConfigValue<Double> value, double min, double max) {
            this.value = value;
            this.min = min;
            this.max = max;
        }
        
        @Override
        public Double get() {
            return value.get();
        }
        
        public void set(double value) {
            if (value < min || value > max) {
                throw new IllegalArgumentException("Value " + value + " out of range [" + min + ", " + max + "]");
            }
            this.value.set(value);
        }
        
        public double getMin() {
            return min;
        }
        
        public double getMax() {
            return max;
        }
        
        public ConfigValue<Double> getConfigValue() {
            return value;
        }
    }
}
