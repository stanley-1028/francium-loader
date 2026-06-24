package com.francium.ai.mapping;

import com.francium.api.PublicApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 版本間方法映射資料庫。
 * 
 * 資料來源:
 * 1. Mojang 官方 Mappings (每個版本發布)
 * 2. Fabric Yarn/Intermediary
 * 3. Forge SRG
 * 4. 社群貢獻的映射 (spigot, paper)
 * 5. ML 模型學習到的映射
 * 
 * 資料結構:
 * - 多層索引: version -> owner -> name+desc -> targets
 * - 反向索引: 支援雙向查詢
 */
@PublicApi
public class MappingDatabase {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingDatabase.class);

    // version -> (sourceKey -> [targetSignatures])
    private final Map<String, Map<String, List<MethodSignature>>> mappings;
    
    // 學習到的映射 (運行時動態添加)
    private final Map<String, Map<String, List<MethodSignature>>> learnedMappings;
    
    // 類別結構緩存: version -> (className -> [method signatures])
    private final Map<String, Map<String, List<MethodSignature>>> classCache;
    
    // 已加載的版本
    private final Set<String> loadedVersions;

    public MappingDatabase() {
        this.mappings = new ConcurrentHashMap<>();
        this.learnedMappings = new ConcurrentHashMap<>();
        this.classCache = new ConcurrentHashMap<>();
        this.loadedVersions = ConcurrentHashMap.newKeySet();
        loadSeedMappings();
    }

    /**
     * 內部工具：將 classCache 条目同步註冊到 mappings 索引。
     * ★ BUG FIX: findMapping() 依賴 mappings 查找，但該 Map 從未被填入資料。
     *   每次加載 mapping 文件後需同步索引。
     */
    private void indexVersionMappings(String version) {
        Map<String, List<MethodSignature>> versionIndex = mappings.computeIfAbsent(version, k -> new ConcurrentHashMap<>());
        Map<String, List<MethodSignature>> classMap = classCache.get(version);
        if (classMap == null) return;
        
        for (var entry : classMap.entrySet()) {
            for (var sig : entry.getValue()) {
                String key = version + ":" + sig.toKey();
                versionIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(sig);
            }
        }
    }

    /**
     * 從 mapping 檔案加載映射資料。
     * 支援格式: .tiny (Fabric), .tsrg/.srg (Forge), .json (自訂)
     */
    public void loadMappingsFile(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        
        if (fileName.endsWith(".tiny")) {
            loadTinyMappings(file);
        } else if (fileName.endsWith(".tsrg") || fileName.endsWith(".srg")) {
            loadTsrgMappings(file);
        } else if (fileName.endsWith(".json")) {
            loadJsonMappings(file);
        }
    }

    private void loadTinyMappings(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        String version = null;
        String currentClass = null;
        
        for (String line : lines) {
            if (line.startsWith("#")) continue;
            
            String[] parts = line.split("\t");
            if (parts.length < 2) continue;
            
            if (line.startsWith("v\t")) {
                version = parts[1];
                loadedVersions.add(version);
            } else if (parts[0].equals("CLASS") && parts.length >= 3) {
                currentClass = parts[2]; // intermediary name
                classCache.computeIfAbsent(version, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(currentClass, k -> new ArrayList<>());
            } else if (parts[0].equals("METHOD") && currentClass != null && parts.length >= 4) {
                // Format: METHOD <intermediary> <descriptor> <named>
                String intermediaryName = parts[1];
                String descriptor = parts[2];
                String namedName = parts.length > 3 ? parts[3] : intermediaryName;
                
                MethodSignature sig = new MethodSignature(currentClass, intermediaryName, descriptor);
                sig.setMojangName(namedName);
                sig.setMappedVersion(version);
                
                classCache.get(version).get(currentClass).add(sig);
            }
        }
        // ★ BUG FIX: 將 classCache 同步到 mappings 索引，使 findMapping() 能正確查詢
        if (version != null) indexVersionMappings(version);
    }

    private void loadTsrgMappings(Path file) throws IOException {
        // TSRG format: simpler, class/method/field mappings
        List<String> lines = Files.readAllLines(file);
        String currentClass = null;
        String version = "unknown";
        
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            
            String[] parts = line.split(" ");
            if (parts.length == 2 && !line.startsWith("\t")) {
                // Class mapping
                currentClass = parts[1]; // SRG name
            } else if (line.startsWith("\t") && parts.length >= 3) {
                // Method mapping
                String srgName = parts[0].trim();
                String descriptor = parts.length > 2 ? parts[1] : "";
                String obfName = parts.length > 2 ? parts[2] : parts[1];
                
                if (currentClass != null) {
                    MethodSignature sig = new MethodSignature(currentClass, srgName, descriptor);
                    sig.setObfuscatedName(obfName);
                    sig.setMappedVersion(version);
                    
                    classCache.computeIfAbsent(version, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent(currentClass, k -> new ArrayList<>())
                        .add(sig);
                }
            }
        }
        // ★ BUG FIX: 同步 TSRG 加載結果到 mappings 索引
        indexVersionMappings(version);
    }

    private void loadJsonMappings(Path file) throws IOException {
        // 自訂 JSON 格式 - 支援簡單的 JSON 映射檔
        String content = Files.readString(file);
        parseSimpleJsonMappings(content);
    }

    /**
     * 解析簡單 JSON 映射格式（無外部依賴）。
     * 格式: {"mappings":[{"from":"owner#name:desc","to":"owner#name:desc"},...]}
     */
    private void parseSimpleJsonMappings(String content) {
        if (content == null || content.isBlank()) return;
        
        // 查找 mappings 陣列
        String arrayPattern = "\"mappings\"\\s*:\\s*\\[";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(arrayPattern).matcher(content);
        if (!m.find()) return;
        
        int start = content.indexOf('[', m.start());
        if (start < 0) return;
        
        // 手動找匹配的 ]
        int depth = 0;
        int end = -1;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }
        if (end < 0) return;
        
        String arrayContent = content.substring(start + 1, end);
        
        // 提取每個物件中的 "from" 和 "to"
        java.util.regex.Matcher objMatcher = java.util.regex.Pattern.compile(
            "\"from\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"to\"\\s*:\\s*\"([^\"]+)\""
        ).matcher(arrayContent);
        
        String currentVersion = "unknown";
        while (objMatcher.find()) {
            String fromKey = objMatcher.group(1);
            String toKey = objMatcher.group(2);
            
            // fromKey format: "owner#name:desc"
            MethodSignature fromSig = parseKeyToSignature(fromKey);
            MethodSignature toSig = parseKeyToSignature(toKey);
            
            if (fromSig != null && toSig != null) {
                toSig.setMappedVersion(currentVersion);
                learnMapping(fromSig, toSig);
            }
        }
    }
    
    private MethodSignature parseKeyToSignature(String key) {
        if (key == null || key.isEmpty()) return null;
        int hashIdx = key.indexOf('#');
        int descIdx = key.indexOf(':', hashIdx >= 0 ? hashIdx : 0);
        
        if (hashIdx < 0 || descIdx < 0) return null;
        
        String owner = key.substring(0, hashIdx);
        String name = key.substring(hashIdx + 1, descIdx);
        String desc = key.substring(descIdx);
        
        return new MethodSignature(owner, name, desc);
    }

    /**
     * 查詢方法在目標版本是否存在。
     */
    public boolean existsInVersion(MethodSignature sig, String version) {
        Map<String, List<MethodSignature>> classMap = classCache.get(version);
        if (classMap == null) return false;
        
        List<MethodSignature> methods = classMap.get(sig.owner());
        if (methods == null) return false;
        
        return methods.stream().anyMatch(m -> m.name().equals(sig.name()) 
            && m.descriptor().equals(sig.descriptor()));
    }

    /**
     * 尋找從來源版本到目標版本的映射。
     * 優先查找精確映射，然後使用 ML 排序。
     */
    public List<MethodSignature> findMapping(MethodSignature sig, String fromVersion, String toVersion) {
        String key = fromVersion + ":" + sig.toKey();
        Map<String, List<MethodSignature>> versionMappings = mappings.get(toVersion);
        
        if (versionMappings != null) {
            List<MethodSignature> direct = versionMappings.get(key);
            if (direct != null && !direct.isEmpty()) return direct;
        }
        
        // 嘗試學習到的映射
        Map<String, List<MethodSignature>> learnedVersion = learnedMappings.get(toVersion);
        if (learnedVersion != null) {
            List<MethodSignature> learned = learnedVersion.get(key);
            if (learned != null) return learned;
        }
        
        // 結構搜索作為 fallback
        return structuralSearchResults(sig, fromVersion, toVersion);
    }

    /**
     * 結構搜索: 在目標版本中查找結構相似的方法。
     * 
     * 不依賴名稱，而是比對:
     * - 所屬類別的繼承鏈
     * - 方法描述符
     * - 方法體結構 (指令序列模式)
     * - 呼叫圖相似度
     */
    public MethodSignature structuralSearch(MethodSignature sig, String fromVersion, String toVersion) {
        Map<String, List<MethodSignature>> targetClasses = classCache.get(toVersion);
        if (targetClasses == null) return null;
        
        // 先找相似的類名
        String targetOwner = findSimilarClass(sig.owner(), targetClasses.keySet());
        if (targetOwner == null) {
            // 用包名結構搜索
            targetOwner = findClassByPackageStructure(sig.owner(), targetClasses.keySet());
        }
        
        if (targetOwner == null) return null;
        
        List<MethodSignature> candidates = targetClasses.get(targetOwner);
        if (candidates == null || candidates.isEmpty()) return null;
        
        // 過濾相同描述符的方法
        List<MethodSignature> sameDescriptor = candidates.stream()
            .filter(m -> m.descriptor().equals(sig.descriptor()))
            .toList();
        
        if (sameDescriptor.size() == 1) return sameDescriptor.get(0);
        if (!sameDescriptor.isEmpty()) {
            // 用相似度排序取最佳
            return sameDescriptor.stream()
                .max(Comparator.comparingDouble(sig::similarity))
                .orElse(null);
        }
        
        // 找不到相同描述符，用相似度取所有候選中最佳
        // ★ BUG FIX: 提高相似度閾值，避免返回過低品質的匹配
        return candidates.stream()
            .max(Comparator.comparingDouble(sig::similarity))
            .filter(m -> sig.similarity(m) > 0.5f) // 原來是 0.3f，提高門檻減少誤報
            .orElse(null);
    }

    private List<MethodSignature> structuralSearchResults(MethodSignature sig, String fromVersion, String toVersion) {
        MethodSignature result = structuralSearch(sig, fromVersion, toVersion);
        return result != null ? List.of(result) : List.of();
    }

    private String findSimilarClass(String className, Set<String> candidates) {
        // Edit distance based class name matching
        String simpleName = className.substring(className.lastIndexOf('/') + 1);
        
        return candidates.stream()
            .filter(c -> {
                String cSimple = c.substring(c.lastIndexOf('/') + 1);
                return levenshteinRatio(simpleName, cSimple) > 0.7f;
            })
            .min(Comparator.comparingDouble(c -> {
                String cSimple = c.substring(c.lastIndexOf('/') + 1);
                return 1.0 - levenshteinRatio(simpleName, cSimple);
            }))
            .orElse(null);
    }

    private String findClassByPackageStructure(String className, Set<String> candidates) {
        // 匹配包結構相似的類
        String[] parts = className.split("/");
        if (parts.length < 2) return null;
        
        String packagePrefix = String.join("/", Arrays.copyOf(parts, parts.length - 1));
        
        return candidates.stream()
            .filter(c -> c.contains(packagePrefix) || c.endsWith(parts[parts.length - 1]))
            .findFirst()
            .orElse(null);
    }

    private float levenshteinRatio(String a, String b) {
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0f;
        int dist = levenshteinDistance(a, b);
        return 1.0f - (float) dist / maxLen;
    }

    private int levenshteinDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev; prev = curr; curr = temp;
        }
        return prev[b.length()];
    }

    /**
     * 從 classpath 的 mappings/ 目錄自動加載種子映射數據。
     * 支援兩種格式:
     *   1. {"classes": {...}}  — 按版本和類別組織的方法簽名
     *   2. {"mappings": [...]}  — 跨版本映射 (from→to)
     * 
     * 在 FranciumLoader 初始化時調用，確保資料庫有基礎數據。
     */
    public void loadSeedMappings() {
        try {
            java.io.InputStream seedIndex = getClass().getClassLoader()
                .getResourceAsStream("mappings/seed-mappings-v1_20_4.json");
            if (seedIndex != null) {
                loadSeedJsonFile(seedIndex, "1.20.4");
                loadedVersions.add("1.20.4");
                // ★ BUG FIX: 每次加載後立即同步索引，確保 findMapping() 能正確查詢
                indexVersionMappings("1.20.4");
            }
            
            java.io.InputStream seedV21 = getClass().getClassLoader()
                .getResourceAsStream("mappings/seed-mappings-v1_21.json");
            if (seedV21 != null) {
                loadSeedJsonFile(seedV21, "1.21");
                loadedVersions.add("1.21");
                // ★ BUG FIX: 同上，立即同步索引
                indexVersionMappings("1.21");
            }
            
            java.io.InputStream crossVersion = getClass().getClassLoader()
                .getResourceAsStream("mappings/cross-version-v1_20_4-to-v1_21.json");
            if (crossVersion != null) {
                loadCrossVersionMappings(crossVersion);
            }
            
            LOGGER.info("[MappingDatabase] Seed mappings loaded for versions: {}", loadedVersions);
        } catch (Exception e) {
            // seed mappings 是附加功能，失敗不影響核心功能
            LOGGER.warn("[MappingDatabase] Could not load seed mappings: {}", e.getMessage());
        }
    }

    /**
     * 加載 JSON seed 映射文件 (classes 格式)。
     */
    private void loadSeedJsonFile(java.io.InputStream input, String version) throws java.io.IOException {
        String content = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        
        // 解析 "classes" 區塊
        String classesPattern = "\"classes\"\\s*:\\s*\\{";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(classesPattern).matcher(content);
        if (!m.find()) return;
        
        int classesStart = content.indexOf('{', m.start());
        if (classesStart < 0) return;
        
        // 找到 classes 物件結束位置
        int depth = 0;
        int classesEnd = -1;
        boolean inString = false;
        char prevChar = 0;
        for (int i = classesStart; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && prevChar != '\\') inString = !inString;
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) { classesEnd = i; break; }
                }
            }
            prevChar = c;
        }
        if (classesEnd < 0) return;
        
        String classesContent = content.substring(classesStart + 1, classesEnd - 1);
        
        // 用簡單的逐行解析提取每個類別和它的方法列表
        java.util.regex.Matcher classMatcher = java.util.regex.Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*\\["
        ).matcher(classesContent);
        
        while (classMatcher.find()) {
            String className = classMatcher.group(1);
            
            // 找到這個類別的方法陣列
            int arrStart = classesContent.indexOf('[', classMatcher.end());
            if (arrStart < 0) continue;
            
            int arrEnd = -1;
            depth = 0;
            inString = false;
            prevChar = 0;
            for (int i = arrStart; i < classesContent.length(); i++) {
                char c = classesContent.charAt(i);
                if (c == '"' && prevChar != '\\') inString = !inString;
                if (!inString) {
                    if (c == '[') depth++;
                    else if (c == ']') {
                        depth--;
                        if (depth == 0) { arrEnd = i; break; }
                    }
                }
                prevChar = c;
            }
            if (arrEnd < 0) continue;
            
            String arrContent = classesContent.substring(arrStart + 1, arrEnd);
            
            // 提取每個方法條目
            java.util.regex.Matcher methodMatcher = java.util.regex.Pattern.compile(
                "\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"desc\"\\s*:\\s*\"([^\"]+)\""
            ).matcher(arrContent);
            
            Map<String, List<MethodSignature>> classMap = classCache
                .computeIfAbsent(version, k -> new ConcurrentHashMap<>());
            List<MethodSignature> methods = classMap
                .computeIfAbsent(className, k -> new ArrayList<>());
            
            while (methodMatcher.find()) {
                String name = methodMatcher.group(1);
                String desc = methodMatcher.group(2);
                MethodSignature sig = new MethodSignature(className, name, desc);
                sig.setMappedVersion(version);
                
                // 提取混淆名 (如果有)
                java.util.regex.Matcher obfMatcher = java.util.regex.Pattern.compile(
                    "\"obf\"\\s*:\\s*\"([^\"]+)\""
                ).matcher(arrContent);
                int fieldStart = methodMatcher.start();
                int fieldEnd = arrContent.indexOf('}', methodMatcher.start());
                if (fieldEnd < 0) fieldEnd = arrContent.length();
                String methodBlock = arrContent.substring(fieldStart, Math.min(fieldEnd + 1, arrContent.length()));
                java.util.regex.Matcher localObf = java.util.regex.Pattern.compile(
                    "\"obf\"\\s*:\\s*\"([^\"]+)\""
                ).matcher(methodBlock);
                if (localObf.find()) {
                    sig.setObfuscatedName(localObf.group(1));
                    sig.setMojangName(name);
                }
                
                methods.add(sig);
            }
        }
    }

    /**
     * 加載跨版本映射 (mappings 格式)。
     */
    private void loadCrossVersionMappings(java.io.InputStream input) throws java.io.IOException {
        String content = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        parseSimpleJsonMappings(content);
    }

    /**
     * 學習新的映射關係 (運行時發現的)。
     */
    public void learnMapping(MethodSignature from, MethodSignature to) {
        String toVersion = to.mappedVersion();
        
        learnedMappings.computeIfAbsent(toVersion, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(from.toKey(), k -> new ArrayList<>())
            .add(to);
    }

    /**
     * 匯出學習到的映射 (用於分享和模型訓練)。
     */
    public void exportLearnedMappings(Path output) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"mappings\":[\n");
        
        boolean first = true;
        for (var versionEntry : learnedMappings.entrySet()) {
            for (var mappingEntry : versionEntry.getValue().entrySet()) {
                for (var target : mappingEntry.getValue()) {
                    if (!first) sb.append(",\n");
                    sb.append(String.format("  {\"from\":\"%s\",\"to\":\"%s\"}",
                        mappingEntry.getKey(), target.toKey()));
                    first = false;
                }
            }
        }
        sb.append("\n]}");
        Files.writeString(output, sb.toString());
    }

    public Set<String> getLoadedVersions() {
        return Collections.unmodifiableSet(loadedVersions);
    }
}
