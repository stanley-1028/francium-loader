package com.francium.loader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 加載器設定，支援 TOML-like 格式。
 */
public class LoaderConfig {
    // 並行配置
    public int maxParallelMods = Runtime.getRuntime().availableProcessors();
    public int layerTimeoutSeconds = 120;
    
    // AI 橋接配置
    public boolean aiBridgeEnabled = true;
    public float aiConfidenceThreshold = 0.85f;
    public boolean aiBridgeReportOnly = false; // 僅報告而不自動修復
    
    // 記憶體管理
    public boolean memoryLeakDetection = true;
    public long memoryWarningThresholdMB = 512;
    public boolean aggressiveGC = false;
    
    // 伺服器同步
    public boolean serverSyncEnabled = true;
    public String serverSyncUrl = "";
    public boolean autoDownloadMods = false;
    
    // 套件管理器
    public String registryUrl = "https://registry.francium.dev/v1";
    public List<String> additionalRegistries = new ArrayList<>();
    
    // 除錯
    public boolean verboseLogging = false;
    public boolean profileLoading = true;
    public String logLevel = "INFO";

    public static LoaderConfig load(Path configPath) {
        LoaderConfig config = new LoaderConfig();
        if (!Files.exists(configPath)) {
            return config;
        }
        
        try {
            List<String> lines = Files.readAllLines(configPath);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;
                
                String key = parts[0].trim();
                String value = parts[1].trim().replaceAll("^\"|\"$", "");
                
                switch (key) {
                    case "maxParallelMods" -> config.maxParallelMods = Integer.parseInt(value);
                    case "layerTimeoutSeconds" -> config.layerTimeoutSeconds = Integer.parseInt(value);
                    case "aiBridgeEnabled" -> config.aiBridgeEnabled = Boolean.parseBoolean(value);
                    case "aiConfidenceThreshold" -> config.aiConfidenceThreshold = Float.parseFloat(value);
                    case "aiBridgeReportOnly" -> config.aiBridgeReportOnly = Boolean.parseBoolean(value);
                    case "memoryLeakDetection" -> config.memoryLeakDetection = Boolean.parseBoolean(value);
                    case "memoryWarningThresholdMB" -> config.memoryWarningThresholdMB = Long.parseLong(value);
                    case "aggressiveGC" -> config.aggressiveGC = Boolean.parseBoolean(value);
                    case "serverSyncEnabled" -> config.serverSyncEnabled = Boolean.parseBoolean(value);
                    case "serverSyncUrl" -> config.serverSyncUrl = value;
                    case "autoDownloadMods" -> config.autoDownloadMods = Boolean.parseBoolean(value);
                    case "registryUrl" -> config.registryUrl = value;
                    case "verboseLogging" -> config.verboseLogging = Boolean.parseBoolean(value);
                    case "profileLoading" -> config.profileLoading = Boolean.parseBoolean(value);
                    case "logLevel" -> config.logLevel = value;
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }
        
        return config;
    }

    public static LoaderConfig createDefault() {
        return new LoaderConfig();
    }
}
