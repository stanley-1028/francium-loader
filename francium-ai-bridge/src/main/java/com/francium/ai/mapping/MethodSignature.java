package com.francium.ai.mapping;

import com.francium.api.PublicApi;
import java.util.*;

/**
 * 方法的完整簽名，包含混淆前後的對應關係。
 * 
 * Minecraft 混淆鏈: 
 * Mojang Mappings (可讀) → Obfuscated (runtime) → Intermediary (Fabric)
 * 
 * 我們同時存儲多種表示以最大化匹配成功率。
 */
@PublicApi
public class MethodSignature {
    private final String owner;          // 所屬類別全名
    private final String name;           // 方法名 (可能是混淆的)
    private final String descriptor;     // JVM 描述符 (如 (I)Ljava/lang/String;)
    private final String returnType;     // 返回類型
    private final List<String> paramTypes; // 參數類型列表
    
    // 多種名稱表示
    private String mojangName;           // Mojang 可讀名稱
    private String intermediaryName;     // Fabric Intermediary
    private String seargeName;           // Forge SRG
    private String obfuscatedName;       // Notch 混淆名
    
    // 結構特徵 (用於結構匹配)
    private int instructionCount;        // 方法體指令數
    private Set<String> calledMethods;   // 內部呼叫的其他方法
    private Set<String> fieldAccesses;   // 訪問的欄位
    private int lineNumber;              // 在源碼中的行號 (如果可獲得)
    
    private String mappedVersion;        // 此簽名對應的 MC 版本

    public MethodSignature(String owner, String name, String descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.paramTypes = parseParamTypes(descriptor);
        this.returnType = parseReturnType(descriptor);
        this.calledMethods = new HashSet<>();
        this.fieldAccesses = new HashSet<>();
    }

    /**
     * 計算兩個方法簽名的相似度 (0.0 ~ 1.0)。
     * 
     * 相似度計算因素:
     * - 所屬類別結構相似度 (包名、類名 edit distance)
     * - 方法描述符匹配度 (參數類型、返回類型)
     * - 呼叫模式相似度 (內部呼叫了哪些方法)
     * - 欄位訪問模式
     * - 指令數量比例
     */
    public float similarity(MethodSignature other) {
        float score = 0f;
        float weightSum = 0f;
        
        // Factor 1: 描述符精確匹配 (權重 0.35)
        weightSum += 0.35f;
        if (this.descriptor.equals(other.descriptor)) {
            score += 0.35f;
        } else if (this.paramTypes.equals(other.paramTypes)) {
            score += 0.25f; // 參數相同但返回不同
        }
        
        // Factor 2: 類名相似度 (權重 0.25)
        weightSum += 0.25f;
        score += 0.25f * classNameSimilarity(this.owner, other.owner);
        
        // Factor 3: 呼叫模式重疊 (權重 0.20)
        if (!this.calledMethods.isEmpty() && !other.calledMethods.isEmpty()) {
            weightSum += 0.20f;
            Set<String> intersection = new HashSet<>(this.calledMethods);
            intersection.retainAll(other.calledMethods);
            Set<String> union = new HashSet<>(this.calledMethods);
            union.addAll(other.calledMethods);
            score += 0.20f * ((float) intersection.size() / union.size());
        }
        
        // Factor 4: 欄位訪問重疊 (權重 0.10)
        if (!this.fieldAccesses.isEmpty() && !other.fieldAccesses.isEmpty()) {
            weightSum += 0.10f;
            Set<String> intersection = new HashSet<>(this.fieldAccesses);
            intersection.retainAll(other.fieldAccesses);
            Set<String> union = new HashSet<>(this.fieldAccesses);
            union.addAll(other.fieldAccesses);
            score += 0.10f * ((float) intersection.size() / union.size());
        }
        
        // Factor 5: 指令數量比例 (權重 0.10)
        if (this.instructionCount > 0 && other.instructionCount > 0) {
            weightSum += 0.10f;
            float ratio = (float) Math.min(this.instructionCount, other.instructionCount)
                        / Math.max(this.instructionCount, other.instructionCount);
            score += 0.10f * ratio;
        }
        
        return weightSum > 0 ? score / weightSum : 0f;
    }

    private float classNameSimilarity(String a, String b) {
        // 取簡單類名比較
        String simpleA = a.substring(a.lastIndexOf('/') + 1);
        String simpleB = b.substring(b.lastIndexOf('/') + 1);
        
        if (simpleA.equals(simpleB)) return 1.0f;
        
        // Edit distance based similarity
        int dist = levenshteinDistance(simpleA, simpleB);
        int maxLen = Math.max(simpleA.length(), simpleB.length());
        return maxLen > 0 ? 1.0f - (float) dist / maxLen : 0f;
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    // --- Parser helpers ---
    private static List<String> parseParamTypes(String descriptor) {
        List<String> types = new ArrayList<>();
        int i = 1; // skip opening '('
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            int[] consumed = new int[1];
            String type = parseTypeWithLen(descriptor, i, consumed);
            types.add(type);
            i += consumed[0];
        }
        return types;
    }

    private static String parseReturnType(String descriptor) {
        int parenClose = descriptor.indexOf(')');
        return parseTypeWithLen(descriptor, parenClose + 1, new int[1]);
    }

    /**
     * 解析單個 JVM 類型描述符，同時返回消耗的字元數。
     * JVM 描述符格式: B=byte, C=char, D=double, F=float, I=int, J=long,
     *   S=short, Z=boolean, V=void, L<classname>;, [array
     */
    private static String parseTypeWithLen(String descriptor, int start, int[] consumed) {
        char c = descriptor.charAt(start);
        
        if (c == 'L') {
            int end = descriptor.indexOf(';', start);
            consumed[0] = end - start + 1;
            return descriptor.substring(start + 1, end).replace('/', '.');
        } else if (c == '[') {
            int[] innerConsumed = new int[1];
            String inner = parseTypeWithLen(descriptor, start + 1, innerConsumed);
            consumed[0] = 1 + innerConsumed[0];
            return inner + "[]";
        } else {
            consumed[0] = 1;
            return switch (c) {
                case 'I' -> "int";
                case 'J' -> "long";
                case 'F' -> "float";
                case 'D' -> "double";
                case 'Z' -> "boolean";
                case 'C' -> "char";
                case 'B' -> "byte";
                case 'S' -> "short";
                case 'V' -> "void";
                default -> "unknown";
            };
        }
    }

    // Keep old parseType for backward compat
    private static String parseType(String descriptor, int start) {
        return parseTypeWithLen(descriptor, start, new int[1]);
    }

    // --- Getters and Setters ---
    /** 所屬類別（內部名稱，如 net/minecraft/world/level/block/Block）。 */
    public String owner() { return owner; }
    /** 方法名稱（混淆或映射後）。 */
    public String name() { return name; }
    /** JVM 方法描述子，如 (Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState; */
    public String descriptor() { return descriptor; }
    /** 返回類型（如 java.lang.String 或 void）。 */
    public String returnType() { return returnType; }
    /** 參數類型列表。 */
    public List<String> paramTypes() { return paramTypes; }
    /** Mojang 官方映射名稱（若有）。 */
    public String mojangName() { return mojangName; }
    public void setMojangName(String name) { this.mojangName = name; }
    /** Yarn/Intermediary 映射名稱（若有）。 */
    public String intermediaryName() { return intermediaryName; }
    public void setIntermediaryName(String name) { this.intermediaryName = name; }
    /** 原始混淆名稱（若有）。 */
    public String obfuscatedName() { return obfuscatedName; }
    public void setObfuscatedName(String name) { this.obfuscatedName = name; }
    /** 方法的 bytecode 指令數。 */
    public int instructionCount() { return instructionCount; }
    public void setInstructionCount(int count) { this.instructionCount = count; }
    /** 此方法中呼叫的其他方法集合。 */
    public Set<String> calledMethods() { return calledMethods; }
    public void addCalledMethod(String method) { this.calledMethods.add(method); }
    /** 此方法中存取的欄位集合。 */
    public Set<String> fieldAccesses() { return fieldAccesses; }
    public void addFieldAccess(String field) { this.fieldAccesses.add(field); }
    /** 此簽名所屬的映射版本。 */
    public String mappedVersion() { return mappedVersion; }
    public void setMappedVersion(String v) { this.mappedVersion = v; }

    /** 返回格式化的唯一鍵值，用於映射查詢緩存。 */
    public String toKey() {
        return owner + "#" + name + descriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MethodSignature m)) return false;
        return owner.equals(m.owner) && name.equals(m.name) && descriptor.equals(m.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, descriptor);
    }

    @Override
    public String toString() {
        return owner + "." + name + "(" + String.join(",", paramTypes) + ") -> " + returnType;
    }
}
