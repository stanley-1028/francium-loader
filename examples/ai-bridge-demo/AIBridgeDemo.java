package com.francium;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Bridge Demo — Francium's cross-version magic, in one runnable file.
 *
 * 演示内容：一个为 MC 1.20.4 编写的模组，如何在 1.21 上自动适配运行。
 * 不需要 Minecraft、不需要编译模组，一个 main 方法展示完整流程。
 *
 * 运行: java AIBridgeDemo.java
 * 或: javac AIBridgeDemo.java && java AIBridgeDemo
 *
 * 「你的模组，Francium 替你桥接未来。」
 */
public class AIBridgeDemo {

    // ─── 模拟 Minecraft API（不同版本的混淆方法名）─────────────

    /**
     * MC 各版本的真实混淆变化示例。
     * Mojang 每版本重混淆，方法名变成 m_xxxxx_() 这种无意义名字。
     * 但语义完全一致：getBlockState 永远返回方块状态。
     */
    static final Map<String, Map<String, String>> MC_API = Map.of(
        "1.20.4", Map.ofEntries(
            Map.entry("Block.getBlockState",  "m_12345_"),  // 获取方块状态
            Map.entry("Entity.getPos",        "m_67890_"),  // 获取实体坐标
            Map.entry("World.setBlock",       "m_11111_"),  // 设置方块
            Map.entry("ItemStack.getCount",   "m_22222_"),  // 获取物品数量
            Map.entry("Player.sendMessage",   "m_33333_"),  // 发送消息
            Map.entry("Level.isDay",          "m_44444_"),  // 判断白天
            Map.entry("BlockPos.above",       "m_55555_"),  // 上方坐标
            Map.entry("Server.getTPS",        "m_66666_"),  // 获取TPS
            Map.entry("Chunk.isLoaded",       "m_77777_"),  // 区块加载状态
            Map.entry("RecipeManager.get",    "m_88888_")   // 获取配方
        ),
        "1.21", Map.ofEntries(
            Map.entry("Block.getBlockState",  "m_99999_"),  // ← 名字变了！
            Map.entry("Entity.getPos",        "m_88888_"),
            Map.entry("World.setBlock",       "m_77777_"),
            Map.entry("ItemStack.getCount",   "m_66666_"),
            Map.entry("Player.sendMessage",   "m_55555_"),
            Map.entry("Level.isDay",          "m_44444_"),
            Map.entry("BlockPos.above",       "m_33333_"),
            Map.entry("Server.getTPS",        "m_22222_"),
            Map.entry("Chunk.isLoaded",       "m_11111_"),
            Map.entry("RecipeManager.get",    "m_00000_")
        ),
        "1.22", Map.ofEntries(
            Map.entry("Block.getBlockState",  "m_ABCDE_"),
            Map.entry("Entity.getPos",        "m_BCDEF_"),
            Map.entry("World.setBlock",       "m_CDEFG_"),
            Map.entry("ItemStack.getCount",   "m_DEFGH_"),
            Map.entry("Player.sendMessage",   "m_EFGHI_"),
            Map.entry("Level.isDay",          "m_FGHIJ_"),
            Map.entry("BlockPos.above",       "m_GHIJK_"),
            Map.entry("Server.getTPS",        "m_HIJKL_"),
            Map.entry("Chunk.isLoaded",       "m_IJKLM_"),
            Map.entry("RecipeManager.get",    "m_JKLMN_")
        )
    );

    // ─── 模拟一个为 MC 1.20.4 编写的模组 ─────────────────

    static class ModBytecode {
        String modName;
        String targetVersion;
        List<ApiCall> apiCalls;

        record ApiCall(String className, String methodName, String description, String signature) {}

        static ModBytecode of(String name, String mcVersion) {
            ModBytecode mod = new ModBytecode();
            mod.modName = name;
            mod.targetVersion = mcVersion;
            mod.apiCalls = new ArrayList<>();
            return mod;
        }

        ModBytecode addCall(String apiMethod, String description, String signature) {
            apiCalls.add(new ApiCall(
                apiMethod.split("\\.")[0],
                apiMethod.split("\\.")[1],
                description,
                signature
            ));
            return this;
        }
    }

    // ─── AI Bridge Engine ────────────────────────────────

    static class BridgeEngine {
        String sourceVersion;
        String targetVersion;
        float confidenceThreshold = 0.85f;

        // 多版本映射数据库（模拟Francium的MappingDatabase）
        Map<String, Map<String, String>> mappingDb = new HashMap<>();

        BridgeEngine(String source, String target) {
            this.sourceVersion = source;
            this.targetVersion = target;
            initMappings();
        }

        // 初始化：建立版本间的映射关系
        void initMappings() {
            Map<String, String> src = MC_API.get(sourceVersion);
            Map<String, String> tgt = MC_API.get(targetVersion);
            if (src == null || tgt == null) return;

            // 反向构建：语义名 → 源版本混淆名
            Map<String, String> semanticToSrc = new HashMap<>();
            for (var e : src.entrySet()) semanticToSrc.put(e.getKey(), e.getValue());

            mappingDb.put(sourceVersion, semanticToSrc);
            mappingDb.put(targetVersion, tgt);
        }

        /**
         * 分析模组在目标版本的兼容性。
         *
         * Francium 实际行为：
         * 1. ASM 提取字节码中的所有外部方法调用
         * 2. MappingDatabase 查找每个调用在目标版本的对应方法
         * 3. CompatibilityPredictor 按特征相似度排序候选
         * 4. AdapterGenerator 为不匹配的调用生成桥接代码
         */
        BridgeReport analyze(ModBytecode mod) {
            BridgeReport report = new BridgeReport();
            report.modName = mod.modName;
            report.sourceVersion = sourceVersion;
            report.targetVersion = targetVersion;

            Map<String, String> srcObf = MC_API.get(sourceVersion);
            Map<String, String> tgtObf = MC_API.get(targetVersion);

            for (ModBytecode.ApiCall call : mod.apiCalls) {
                String semanticKey = call.className + "." + call.methodName;

                // Step 1：检查精确映射（理想情况）
                // Francium 查找 MappingDatabase，确认语义方法名在目标版本存在
                if (tgtObf.containsKey(semanticKey)) {
                    String srcName = srcObf.get(semanticKey);
                    String tgtName = tgtObf.get(semanticKey);

                    if (srcName.equals(tgtName)) {
                        // 运气好：混淆名恰好一致（极罕见）
                        report.directMatches++;
                        report.results.add(new MatchResult(call, "直接兼容",
                            srcName, tgtName, 1.0f, MatchType.DIRECT));
                    } else {
                        // 典型情况：方法名变了，但Francium通过语义映射找到对应
                        float confidence = 0.95f; // 精确语义匹配，高置信度
                        report.aiMapped++;
                        report.results.add(new MatchResult(call, "AI语义映射",
                            srcName, tgtName, confidence, MatchType.AI_MAPPED));
                    }
                } else {
                    // Step 2：结构搜索（方法签名相似度 + 调用模式匹配）
                    // Francium 的 CompatibilityPredictor 会计算特征相似度
                    float bestConfidence = 0f;
                    String bestMatch = null;

                    for (var entry : tgtObf.entrySet()) {
                        float sim = calculateStructuralSimilarity(call, entry.getKey());
                        if (sim > bestConfidence) {
                            bestConfidence = sim;
                            bestMatch = entry.getKey();
                        }
                    }

                    if (bestConfidence >= confidenceThreshold) {
                        report.structuralMapped++;
                        String srcName = srcObf.getOrDefault(semanticKey, semanticKey);
                        report.results.add(new MatchResult(call, "结构搜索匹配",
                            srcName, tgtObf.get(bestMatch),
                            bestConfidence, MatchType.STRUCTURAL));
                    } else if (bestConfidence >= 0.6f) {
                        report.lowConfidence++;
                        report.results.add(new MatchResult(call, "低置信度匹配",
                            srcObf.getOrDefault(semanticKey, semanticKey),
                            tgtObf.getOrDefault(bestMatch, "???"),
                            bestConfidence, MatchType.LOW_CONFIDENCE));
                    } else {
                        report.unmapped++;
                        report.results.add(new MatchResult(call, "无法映射",
                            srcObf.getOrDefault(semanticKey, semanticKey),
                            "???", bestConfidence, MatchType.UNMAPPED));
                    }
                }
            }

            report.total = mod.apiCalls.size();
            report.compatibilityScore = report.total > 0
                ? (float)(report.directMatches + report.aiMapped + report.structuralMapped) / report.total
                : 1.0f;

            return report;
        }

        /**
         * 模拟结构相似度计算。
         * Francium 真实的 CompatibilityPredictor 会使用：
         * - 描述符编辑距离（参数类型、返回类型）
         * - 类继承层级匹配
         * - 调用上下文（AST 分析）
         * - 历史成功记录（ML 加权）
         */
        float calculateStructuralSimilarity(ModBytecode.ApiCall call, String targetSemantic) {
            float score = 0f;

            // 1. 方法名相似度（Levenshtein）
            String srcMethod = call.methodName.toLowerCase();
            String[] tgtParts = targetSemantic.split("\\.");
            String tgtMethod = tgtParts.length > 1 ? tgtParts[1].toLowerCase() : targetSemantic.toLowerCase();

            double nameSim = stringSimilarity(srcMethod, tgtMethod);
            score += 0.25f * (float)nameSim;

            // 2. 类名匹配
            double classSim = stringSimilarity(call.className.toLowerCase(), tgtParts[0].toLowerCase());
            score += 0.15f * (float)classSim;

            // 3. 签名结构相似（get, set, is 前缀匹配）
            if (srcMethod.startsWith("get") && tgtMethod.startsWith("get")) score += 0.20f;
            if (srcMethod.startsWith("set") && tgtMethod.startsWith("set")) score += 0.20f;
            if (srcMethod.startsWith("is") && tgtMethod.startsWith("is")) score += 0.20f;

            // 4. 描述中的关键词匹配
            Set<String> srcWords = extractKeywords(call.description);
            Set<String> tgtWords = extractKeywords(targetSemantic);
            if (!srcWords.isEmpty() && !tgtWords.isEmpty()) {
                long overlap = srcWords.stream().filter(tgtWords::contains).count();
                score += 0.20f * ((float)overlap / Math.max(srcWords.size(), tgtWords.size()));
            }

            return Math.min(1.0f, score);
        }

        double stringSimilarity(String a, String b) {
            if (a.equals(b)) return 1.0;
            if (a.isEmpty() || b.isEmpty()) return 0.0;

            // N-gram 相似度（比 Levenshtein 更适合短字符串）
            Set<String> aGrams = new HashSet<>();
            Set<String> bGrams = new HashSet<>();
            int n = Math.min(2, Math.min(a.length(), b.length()));

            for (int i = 0; i <= a.length() - n; i++) aGrams.add(a.substring(i, i + n));
            for (int i = 0; i <= b.length() - n; i++) bGrams.add(b.substring(i, i + n));

            Set<String> union = new HashSet<>(aGrams);
            union.addAll(bGrams);
            Set<String> intersection = new HashSet<>(aGrams);
            intersection.retainAll(bGrams);

            return union.isEmpty() ? 0.0 : (double)intersection.size() / union.size();
        }

        Set<String> extractKeywords(String text) {
            return Arrays.stream(text.toLowerCase().split("[^a-z]+"))
                .filter(w -> w.length() > 2)
                .collect(Collectors.toSet());
        }

        /**
         * 生成适配器代码（模拟）。
         * 真实 Francium 用 ASM 生成字节码，这里展示生成的代码逻辑。
         */
        List<String> generateAdapterCode(BridgeReport report) {
            List<String> code = new ArrayList<>();
            code.add("/**");
            code.add(" * Francium Auto-Generated Bridge Adapter");
            code.add(" * Source: " + sourceVersion + " → Target: " + targetVersion);
            code.add(" * Generated for: " + report.modName);
            code.add(" * DO NOT EDIT — regenerated on each load");
            code.add(" */");
            code.add("public class " + report.modName.replace("-", "_") + "_FranciumBridge {");
            code.add("");

            for (MatchResult r : report.results) {
                if (r.type == MatchType.AI_MAPPED || r.type == MatchType.STRUCTURAL || r.type == MatchType.LOW_CONFIDENCE) {
                    code.add("    // " + r.call.methodName + "(): " + r.note);
                    code.add("    // " + r.sourceName + " → " + r.targetName + " (confidence: " + (int)(r.confidence*100) + "%)");
                    code.add("    public static Object bridge$" + r.call.methodName + "(Object[] args) {");
                    code.add("        return " + r.call.className + "." + r.targetName + "(args);");
                    code.add("    }");
                    code.add("");
                }
            }

            code.add("}");
            return code;
        }
    }

    // ─── 数据类型 ────────────────────────────────────────

    enum MatchType { DIRECT, AI_MAPPED, STRUCTURAL, LOW_CONFIDENCE, UNMAPPED }

    static class MatchResult {
        ModBytecode.ApiCall call;
        String note;
        String sourceName;
        String targetName;
        float confidence;
        MatchType type;

        MatchResult(ModBytecode.ApiCall call, String note,
                    String sourceName, String targetName,
                    float confidence, MatchType type) {
            this.call = call;
            this.note = note;
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.confidence = confidence;
            this.type = type;
        }
    }

    static class BridgeReport {
        String modName;
        String sourceVersion;
        String targetVersion;
        List<MatchResult> results = new ArrayList<>();
        int total = 0;
        int directMatches = 0;
        int aiMapped = 0;
        int structuralMapped = 0;
        int lowConfidence = 0;
        int unmapped = 0;
        float compatibilityScore = 0f;

        boolean needsAdapter() { return aiMapped + structuralMapped + lowConfidence > 0; }
        boolean isReady() { return unmapped == 0; }
        String compatibilityRating() {
            if (compatibilityScore >= 0.95f) return "✅ 几乎完美兼容";
            if (compatibilityScore >= 0.85f) return "✅ 高兼容性，自动桥接有效";
            if (compatibilityScore >= 0.60f) return "⚠️ 部分兼容，需人工审查部分API";
            return "❌ 兼容性不足，需要开发者介入";
        }
    }

    // ─── 主演示 ──────────────────────────────────────────

    public static void main(String[] args) {
        printBanner();

        // 情景：我们有一个为 MC 1.20.4 编写的模组 "DiamondFinder"
        // 想在 MC 1.21 上运行，但 Mojang 把方法名全改了

        ModBytecode myMod = ModBytecode.of("diamond-finder", "1.20.4")
            .addCall("Block.getBlockState",  "获取当前方块状态（挖掘判断）",          "(III)LBlockState;")
            .addCall("Entity.getPos",        "获取玩家当前位置",                      "()LBlockPos;")
            .addCall("World.setBlock",       "替换目标方块为空气（挖掘后掉落）",       "(LBlockPos;LBlockState;)V")
            .addCall("ItemStack.getCount",   "检查玩家是否有足够的钻石镐",             "()I")
            .addCall("Player.sendMessage",   "提示玩家发现了钻石",                     "(Ljava/lang/String;)V")
            .addCall("Level.isDay",          "只有白天才能使用探测器",                 "()Z")
            .addCall("BlockPos.above",       "获取目标方块上方坐标（放置火把）",       "()LBlockPos;")
            .addCall("Server.getTPS",        "检测服务器性能，低速时降低扫描频率",     "()D")
            .addCall("Chunk.isLoaded",       "检查目标区块是否已加载",                 "()Z")
            .addCall("RecipeManager.get",    "读取钻石探测器的合成配方",               "(LResourceLocation;)LRecipe;");

        // ========================================
        // Phase 1: 分析模组 — BytecodeAnalyzer
        // ========================================
        printSection("Phase 1: 字节码分析 (ASM)");
        System.out.printf("  模组: %s (%s)%n", myMod.modName, myMod.targetVersion);
        System.out.printf("  发现 %d 个外部 Minecraft API 调用:%n%n", myMod.apiCalls.size());

        for (var call : myMod.apiCalls) {
            String srcObf = MC_API.get("1.20.4").get(call.className + "." + call.methodName);
            System.out.printf("    %s.%s()  → %s.%s()  [%s]%n",
                call.className, call.methodName,
                call.className, srcObf, call.description);
        }

        // ========================================
        // Phase 2: 查找映射 — MappingDatabase
        // ========================================
        printSection("Phase 2: 多版本映射查询 (MappingDatabase)");

        BridgeEngine engine = new BridgeEngine("1.20.4", "1.21");
        BridgeReport report = engine.analyze(myMod);

        // ========================================
        // Phase 3: 预测兼容性 — CompatibilityPredictor
        // ========================================
        printSection("Phase 3: 兼容性预测 (CompatibilityPredictor)");

        System.out.println("  逐个API调用分析：");
        System.out.println("  " + "─".repeat(78));
        System.out.printf("  %-28s %-22s %-15s %s%n", "API调用", "源混淆名→目标混淆名", "置信度", "方式");
        System.out.println("  " + "─".repeat(78));

        for (MatchResult r : report.results) {
            String apiLabel = String.format("%s.%s()", r.call.className, r.call.methodName);
            String mappingLabel = String.format("%s → %s", r.sourceName, r.targetName);
            String confLabel = String.format("%.0f%%", r.confidence * 100);
            String icon = switch (r.type) {
                case DIRECT -> "✅";
                case AI_MAPPED -> "🧠";
                case STRUCTURAL -> "🔍";
                case LOW_CONFIDENCE -> "⚠️";
                case UNMAPPED -> "❌";
            };

            System.out.printf("  %-28s %-22s %-15s %s %s%n",
                apiLabel, mappingLabel, confLabel, icon, r.note);
        }
        System.out.println("  " + "─".repeat(78));

        // ========================================
        // Phase 4: 生成适配器 — AdapterGenerator
        // ========================================
        printSection("Phase 4: 适配器生成 (AdapterGenerator)");

        List<String> adapterCode = engine.generateAdapterCode(report);

        if (adapterCode.size() > 5) {
            System.out.println("  Francium 自动生成桥接适配器字节码 ↓\n");
            for (String line : adapterCode) {
                System.out.println("  " + line);
            }
            System.out.println();
            System.out.println("  → Francium 将此适配器注入模组 ClassLoader");
            System.out.println("  → 模组调用 m_12345_() 时自动路由到 m_99999_()");
            System.out.println("  → 模组无需修改代码即可在 MC 1.21 上运行");
        }

        // ========================================
        // Phase 5: 报告摘要
        // ========================================
        printSection("Phase 5: 最终报告");

        System.out.printf("  源版本:        %s%n", report.sourceVersion);
        System.out.printf("  目标版本:      %s%n", report.targetVersion);
        System.out.printf("  总API调用:     %d%n", report.total);
        System.out.println("  " + "─".repeat(35));
        System.out.printf("  ✅ 直接兼容:     %d (%.0f%%)%n",
            report.directMatches, report.total > 0 ? 100f * report.directMatches / report.total : 0);
        System.out.printf("  🧠 AI语义映射:   %d (%.0f%%)%n",
            report.aiMapped, report.total > 0 ? 100f * report.aiMapped / report.total : 0);
        System.out.printf("  🔍 结构搜索:     %d (%.0f%%)%n",
            report.structuralMapped, report.total > 0 ? 100f * report.structuralMapped / report.total : 0);
        System.out.printf("  ⚠️ 低置信度:     %d (%.0f%%)%n",
            report.lowConfidence, report.total > 0 ? 100f * report.lowConfidence / report.total : 0);
        System.out.printf("  ❌ 无法映射:     %d (%.0f%%)%n",
            report.unmapped, report.total > 0 ? 100f * report.unmapped / report.total : 0);
        System.out.println("  " + "─".repeat(35));
        System.out.printf("  综合兼容性:    %.0f%%  %s%n",
            report.compatibilityScore * 100, report.compatibilityRating());

        boolean needsAdapter = report.needsAdapter();
        System.out.println();
        System.out.printf("  是否需要适配器: %s%n", needsAdapter ? "是 (已自动生成)" : "否");
        System.out.printf("  是否可立即运行: %s%n", report.isReady() ? "✅ 模组可直接在 MC 1.21 上运行！" : "❌ 需要人工修复部分API");

        // ========================================
        // 额外演示：跨两个大版本的桥接 (1.20.4 → 1.22)
        // ========================================
        printSection("Bonus: 跨两个大版本桥接 (1.20.4 → 1.22)");

        BridgeEngine engine2 = new BridgeEngine("1.20.4", "1.22");
        BridgeReport report2 = engine2.analyze(myMod);

        System.out.printf("  源版本: 1.20.4 → 目标版本: 1.22 (跨两个大版本!)%n");
        System.out.printf("  兼容性: %.0f%% — %s%n",
            report2.compatibilityScore * 100, report2.compatibilityRating());
        System.out.printf("  AI映射: %d | 结构搜索: %d | 未映射: %d%n",
            report2.aiMapped, report2.structuralMapped, report2.unmapped);
        System.out.println();
        System.out.println("  Francium 的 AI 桥接对跨版本场景同样有效。");
        System.out.println("  原理：语义映射不依赖版本间距，只依赖 Mappings 数据库完整性。");
        System.out.println("  Mojang 每版本都重混淆，但 Francium 通过多版本 Mappings + 相似度");
        System.out.println("  模型，让模组在 1.20.4 → 1.22 这样的跨越中也能自动适配。");

        printFooter();
    }

    // ─── 输出美化 ────────────────────────────────────────

    static void printBanner() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║     Francium AI Bridge — 跨版本兼容性演示           ║");
        System.out.println("  ║     你的模组不必重写，Francium 替你桥接未来          ║");
        System.out.println("  ╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    static void printSection(String title) {
        System.out.println();
        System.out.println("  ┌─ " + title);
        System.out.println("  │");
    }

    static void printFooter() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║  核心原理                                           ║");
        System.out.println("  ║                                                     ║");
        System.out.println("  ║  1. ASM 提取字节码中的外部 API 调用                  ║");
        System.out.println("  ║  2. 多版本 Mapping 数据库查找语义对应关系            ║");
        System.out.println("  ║  3. 特征相似度 ML 模型排序候选                       ║");
        System.out.println("  ║  4. 自动生成字节码适配器，注入 ClassLoader            ║");
        System.out.println("  ║                                                     ║");
        System.out.println("  ║  Minecraft 每次改版重混淆方法名，                      ║");
        System.out.println("  ║  但语义结构不变。Francium 理解语义，自动桥接。       ║");
        System.out.println("  ╚══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  「不要等到模组更新，让 AI 替你桥接未来。」");
        System.out.println();
    }
}
