package com.francium.ai.adapter;

import com.francium.api.PublicApi;
import com.francium.ai.analysis.BytecodeAnalyzer;
import com.francium.ai.mapping.MappingDatabase;
import com.francium.ai.mapping.MethodSignature;
import com.francium.ai.predictor.CompatibilityPredictor;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
@PublicApi
public class VersionBridge {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(VersionBridge.class);

    private final String sourceVersion;   // mod 目標版本 (如 1.20.4)
    private final String targetVersion;   // 當前 MC 版本 (如 1.21)
    private final MappingDatabase mappingDb;
    private final BytecodeAnalyzer analyzer;
    private final CompatibilityPredictor predictor;
    
    private final List<BridgeReport> reports = new ArrayList<>();
    private final Map<String, List<MethodSignature>> unresolvedMappings = new ConcurrentHashMap<>();
    
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
            try {
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
                        // ★ BUG FIX: findMapping 已在內部調用 structuralSearchResults，
                        // 此處無需重複調用，直接標記為無法解析
                        report.unmappedCalls.add(call);
                        unresolvable++;
                    }
                }
            } catch (Exception e) {
                // ★ BUG FIX: 單一方法分析失敗不應影響整個 mod 的分析
                //   記錄錯誤後繼續分析其他方法
                LOGGER.warn("Failed to analyze method call {}: {}", call, e.getMessage());
                report.unmappedCalls.add(call);
                unresolvable++;
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
    /** 設定 AI 映射的置信度閾值（低於此值則標記為無法解析）。 */
    public void setConfidenceThreshold(float threshold) { this.confidenceThreshold = threshold; }
    /** 啟用或停用自動修復（生成適配器）。 */
    public void setAutoFix(boolean autoFix) { this.autoFix = autoFix; }
    /** 啟用報告模式：只分析不改寫，不生成適配器。 */
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    // --- 數據類 ---
    /** 單一模組的版本橋接分析報告。 */
    public static class BridgeReport {
        /** 模組檔案名稱 */
        public String modName;
        /** 外部 API 呼叫總數 */
        public int totalExternalCalls;
        /** 直接兼容（無需映射）的方法數 */
        public int directMatchCount;
        /** 成功映射的方法數 */
        public int mappedCount;
        /** 無法解析的方法數 */
        public int unresolvableCount;
        /** 兼容性分數 (0.0 ~ 1.0) */
        public float compatibilityScore;
        /** 已建立的映射列表 */
        public List<MethodMapping> mappings = new ArrayList<>();
        /** 無法匹配的外部呼叫列表 */
        public List<MethodSignature> unmappedCalls = new ArrayList<>();
        
        public BridgeReport(String modName) { this.modName = modName; }
        
        /** 此 mod 是否需要生成橋接適配器。 */
        public boolean needsAdapter() { return mappedCount > 0; }
        /** 此 mod 是否完全兼容（無未解析的呼叫）。 */
        public boolean isFullyCompatible() { return unresolvableCount == 0; }
        
        @Override
        public String toString() {
            return String.format("[%s] compat=%.0f%% matched=%d mapped=%d fail=%d",
                modName, compatibilityScore * 100, directMatchCount, mappedCount, unresolvableCount);
        }
    }

    public record MethodMapping(MethodSignature source, MethodSignature target, float confidence) {}

    /** 批次橋接的整體摘要，匯總所有 mod 的分析結果。 */
    public static class BridgeSummary {
        /** 來源 Minecraft 版本 */
        public String sourceVersion;
        /** 目標 Minecraft 版本 */
        public String targetVersion;
        /** 各 mod 的橋接報告列表 */
        public List<BridgeReport> reports = new ArrayList<>();
        /** 生成的適配器數量 */
        public int adaptersGenerated = 0;
        /** modId → 適配器位元組映射 */
        public Map<String, byte[]> adapterBytes = new HashMap<>();
        /** 整體兼容性分數 (0.0 ~ 1.0) */
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
