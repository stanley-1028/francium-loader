package com.francium.ai.mapping;

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
public class MappingDatabase {
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
    }

    private void loadJsonMappings(Path file) throws IOException {
        // 自訂 JSON 格式
        String content = Files.readString(file);
        // 依賴 Gson，這裡用簡單的結構假設
        // 實際實作會更複雜
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
        return candidates.stream()
            .max(Comparator.comparingDouble(sig::similarity))
            .filter(m -> sig.similarity(m) > 0.3f)
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
