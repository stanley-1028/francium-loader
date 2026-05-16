package com.francium.ai.adapter;

import com.francium.ai.analysis.BytecodeAnalyzer;
import com.francium.ai.mapping.MappingDatabase;
import com.francium.ai.mapping.MethodSignature;
import com.francium.ai.predictor.CompatibilityPredictor;

import java.nio.file.Path;
import java.util.*;

/**
 * AI 驅動的版本橋接器。
 * 
 * 核心概念:
 * Minecraft 每次改版，混淆後的 method/field 名稱會變，但語義結構相似。
 * 例如 1.20.4 的 Block.getBlockState() 在 1.21 可能變成 Block.m_12345_()。
 * 
 * 解決方案:
 * 1. 靜態分析: 比對 mod bytecode 中的呼叫模式
 * 2. 結構匹配: 基於 class hierarchy、method signature、call graph
 * 3. ML 預測: 用歷史 mapping 訓練相似度模型
 * 4. 自動生成 adapter class / Mixin 修補
 * 
 * 這個技術讓一個為 1.20.4 寫的 mod 可以幾乎無修改在 1.21 運行。
 */
public class VersionBridge {
    private final String sourceVersion;   // mod 目標版本 (如 1.20.4)
    private final String targetVersion;   // 當前 MC 版本 (如 1.21)
    private final MappingDatabase mappingDb;
    private final BytecodeAnalyzer analyzer;
    private final CompatibilityPredictor predictor;
    
    private final List<BridgeReport> reports = new ArrayList<>();
    private final Map<String, List<MethodSignature>> unresolvedMappings = new HashMap<>();
    
    // 信心閾值
    private float confidenceThreshold = 0.85f;
    private boolean autoFix = true;
    private boolean dryRun = false;

    public VersionBridge(String sourceVersion, String targetVersion) {
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
        this.mappingDb = new MappingDatabase();
        this.analyzer = new BytecodeAnalyzer();
        this.predictor = new CompatibilityPredictor();
    }

    /**
     * 分析單個模組的兼容性。
     * 
     * @param modJar 模組 JAR 路徑
     * @return 橋接分析報告
     */
    public BridgeReport analyze(Path modJar) throws Exception {
        BridgeReport report = new BridgeReport(modJar.getFileName().toString());
        
        // Step 1: 提取所有外部方法呼叫
        Set<MethodSignature> externalCalls = analyzer.extractExternalCalls(modJar);
        report.totalExternalCalls = externalCalls.size();
        
        // Step 2: 檢查每個呼叫在目標版本是否存在
        int directMatch = 0;
        int needMapping = 0;
        int unresolvable = 0;
        
        for (MethodSignature call : externalCalls) {
            if (mappingDb.existsInVersion(call, targetVersion)) {
                directMatch++;
            } else {
                // 嘗試尋找對應的目標方法
                List<MethodSignature> candidates = mappingDb.findMapping(call, sourceVersion, targetVersion);
                
                if (!candidates.isEmpty()) {
                    // 用 ML 模型排序候選
                    MethodSignature best = predictor.rankCandidates(call, candidates, sourceVersion, targetVersion);
                    
                    if (best != null && predictor.confidence(call, best) >= confidenceThreshold) {
                        report.mappings.add(new MethodMapping(call, best, predictor.confidence(call, best)));
                        needMapping++;
                    } else {
                        report.unmappedCalls.add(call);
                        unresolvable++;
                    }
                } else {
                    // 啟動結構搜索 (耗時較長但更精確)
                    MethodSignature structural = mappingDb.structuralSearch(call, sourceVersion, targetVersion);
                    if (structural != null) {
                        report.mappings.add(new MethodMapping(call, structural, 0.6f));
                        needMapping++;
                        mappingDb.learnMapping(call, structural); // 學習到的新映射
                    } else {
                        report.unmappedCalls.add(call);
                        unresolvable++;
                    }
                }
            }
        }
        
        report.directMatchCount = directMatch;
        report.mappedCount = needMapping;
        report.unresolvableCount = unresolvable;
        report.compatibilityScore = externalCalls.isEmpty() ? 1.0f
            : (float)(directMatch + needMapping) / externalCalls.size();
        
        reports.add(report);
        return report;
    }

    /**
     * 生成 Adaptor 類別的字節碼。
     * 對每個需要映射的方法，生成一個中間層。
     */
    public byte[] generateAdapter(String modId, List<MethodMapping> mappings) {
        AdapterGenerator gen = new AdapterGenerator(modId, mappings);
        return gen.generate();
    }

    /**
     * 批量分析並生成修補。
     * 
     * @param mods 所有需要橋接的模組
     * @return 整體兼容性報告
     */
    public BridgeSummary bridgeAll(Map<String, Path> mods) throws Exception {
        BridgeSummary summary = new BridgeSummary();
        summary.sourceVersion = sourceVersion;
        summary.targetVersion = targetVersion;
        
        for (var entry : mods.entrySet()) {
            BridgeReport report = analyze(entry.getValue());
            summary.reports.add(report);
            
            if (report.needsAdapter() && autoFix && !dryRun) {
                byte[] adapter = generateAdapter(entry.getKey(), report.mappings);
                summary.adaptersGenerated++;
                summary.adapterBytes.put(entry.getKey(), adapter);
            }
        }
        
        summary.calculateOverallScore();
        return summary;
    }

    // --- 設定 ---
    public void setConfidenceThreshold(float threshold) { this.confidenceThreshold = threshold; }
    public void setAutoFix(boolean autoFix) { this.autoFix = autoFix; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    // --- 數據類 ---
    public static class BridgeReport {
        public String modName;
        public int totalExternalCalls;
        public int directMatchCount;
        public int mappedCount;
        public int unresolvableCount;
        public float compatibilityScore; // 0.0 ~ 1.0
        public List<MethodMapping> mappings = new ArrayList<>();
        public List<MethodSignature> unmappedCalls = new ArrayList<>();
        
        public BridgeReport(String modName) { this.modName = modName; }
        
        public boolean needsAdapter() { return mappedCount > 0; }
        public boolean isFullyCompatible() { return unresolvableCount == 0; }
        
        @Override
        public String toString() {
            return String.format("[%s] compat=%.0f%% matched=%d mapped=%d fail=%d",
                modName, compatibilityScore * 100, directMatchCount, mappedCount, unresolvableCount);
        }
    }

    public record MethodMapping(MethodSignature source, MethodSignature target, float confidence) {}

    public static class BridgeSummary {
        public String sourceVersion;
        public String targetVersion;
        public List<BridgeReport> reports = new ArrayList<>();
        public int adaptersGenerated = 0;
        public Map<String, byte[]> adapterBytes = new HashMap<>();
        public float overallCompatibility;
        
        public void calculateOverallScore() {
            if (reports.isEmpty()) {
                overallCompatibility = 1.0f;
                return;
            }
            overallCompatibility = (float) reports.stream()
                .mapToDouble(r -> r.compatibilityScore)
                .average()
                .orElse(0);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("=== Version Bridge: %s → %s ===\n", sourceVersion, targetVersion));
            sb.append(String.format("Overall compatibility: %.1f%%\n", overallCompatibility * 100));
            sb.append(String.format("Adapters generated: %d\n", adaptersGenerated));
            for (BridgeReport r : reports) {
                sb.append("  ").append(r).append("\n");
            }
            return sb.toString();
        }
    }
}
