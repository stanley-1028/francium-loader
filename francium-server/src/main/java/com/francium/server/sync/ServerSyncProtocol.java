package com.francium.server.sync;

import com.francium.api.PublicApi;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import java.security.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 伺服器-用戶端模組同步協議。
 * 
 * 解決問題: 玩家加入伺服器時 mod 版本不匹配，
 * 需要手動尋找、下載、安裝正確版本的 mod。
 * 
 * Francium 解決方案:
 * 1. 伺服器宣告其 mod 清單 (含版本和 SHA256)
 * 2. 用戶端連接時比對，自動下載缺失/版本不符的 mod
 * 3. 選項: 自動啟用伺服器 mod 設定檔
 * 4. 安全: mod 簽名驗證，防止惡意 mod
 */
@PublicApi
public class ServerSyncProtocol {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSyncProtocol.class);

    /**
     * 伺服器端 mod 清單。
     */
    /** 伺服器端宣告的 mod 清單，用於 client-server 同步比對。 */
    public static class ServerModList {
        /** 伺服器唯一識別碼 */
        public String serverId;
        /** 伺服器顯示名稱 */
        public String serverName;
        /** 伺服器運行的 Minecraft 版本 */
        public String mcVersion;
        /** 伺服器使用的 Francium 版本 */
        public String franciumVersion;
        /** 清單產生時間戳（毫秒） */
        public long timestamp;
        /** mod 條目列表 */
        public List<ServerModEntry> mods;
        /** 伺服器簽名（用於驗證） */
        public String signature;
        
        public byte[] toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"serverId\":").append(jsonEscape(serverId)).append(",");
            sb.append("\"mcVersion\":").append(jsonEscape(mcVersion)).append(",");
            sb.append("\"timestamp\":").append(timestamp).append(",");
            sb.append("\"mods\":[");
            if (mods != null) {
                for (int i = 0; i < mods.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(mods.get(i).toJson());
                }
            }
            sb.append("]");
            if (signature != null) sb.append(",\"signature\":").append(jsonEscape(signature));
            sb.append("}");
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }
        
        public static ServerModList fromJson(String json) {
            if (json == null || json.isBlank()) return new ServerModList();
            ServerModList list = new ServerModList();
            list.mods = new ArrayList<>();
            try {
                // Simple JSON parsing without external dependencies
                list.serverId = extractString(json, "serverId");
                list.mcVersion = extractString(json, "mcVersion");
                list.franciumVersion = extractString(json, "franciumVersion");
                list.signature = extractString(json, "signature");
                list.timestamp = extractLong(json, "timestamp", 0L);
                
                // Parse mods array
                String modsArray = extractArray(json, "mods");
                if (modsArray != null && !modsArray.equals("[]")) {
                    List<String> modObjects = splitJsonObjects(modsArray);
                    for (String modJson : modObjects) {
                        ServerModEntry entry = new ServerModEntry();
                        entry.modId = extractString(modJson, "id");
                        entry.name = extractString(modJson, "name");
                        entry.version = extractString(modJson, "ver");
                        entry.sha256 = extractString(modJson, "sha256");
                        entry.downloadUrl = extractString(modJson, "downloadUrl");
                        entry.required = extractBool(modJson, "required", true);
                        entry.serverOnly = extractBool(modJson, "serverOnly", false);
                        entry.size = extractLong(modJson, "size", 0L);
                        list.mods.add(entry);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[ServerSync] Failed to parse mod list: " + e.getMessage());
            }
            return list;
        }
    }

    /**
     * 單個模組資訊。
     */
    /** 伺服器 mod 清單中的單一條目。 */
    public static class ServerModEntry {
        /** 模組識別碼 */
        public String modId;
        /** 模組顯示名稱 */
        public String name;
        /** 所需版本 */
        public String version;
        /** SHA256 校驗碼 */
        public String sha256;
        /** 檔案大小（位元組） */
        public long size;
        /** 可選的下載連結 */
        public String downloadUrl;
        /** 是否為必要 mod（false = 可選） */
        public boolean required;
        /** 僅伺服器端需要（用戶端無需安裝） */
        public boolean serverOnly;
        /** 伺服器推薦的設定檔（base64 編碼） */
        public String configTemplate;
        
        public String toJson() {
            return "{" +
                "\"id\":" + jsonEscape(modId) + "," +
                "\"ver\":" + jsonEscape(version) + "," +
                "\"sha256\":" + jsonEscape(sha256) + "," +
                "\"required\":" + required + "," +
                "\"serverOnly\":" + serverOnly +
                "}";
        }
    }

    /**
     * 同步結果。
     */
    /** 用戶端與伺服器的 mod 同步結果。 */
    public static class SyncResult {
        /** 是否完全兼容（無需任何操作） */
        public boolean compatible;
        /** 需要執行的操作列表 */
        public List<ModAction> actions = new ArrayList<>();
        /** 同步過程中的警告訊息 */
        public List<String> warnings = new ArrayList<>();
        
        /** 針對單一 mod 的同步操作類型。 */
        public enum Action {
            /** 需要下載 */
            DOWNLOAD,
            /** 需要更新 */
            UPDATE,
            /** 需要移除 */
            REMOVE,
            /** 已是最新，無需操作 */
            OK,
            /** 用戶端有多餘的 mod（可用可不用） */
            EXTRA_CLIENT_MOD
        }
        
        /**
         * 單一 mod 的同步操作記錄。
         * @param modId 模組識別碼
         * @param version 目標版本
         * @param action 操作類型
         * @param reason 操作原因說明
         */
        public record ModAction(String modId, String version, Action action, String reason) {}
    }

    /**
     * 用戶端比對本地 mod 與伺服器清單。
     * 
     * @param serverList 伺服器發來的 mod 清單
     * @param clientMods 用戶端已安裝的 mod (modId -> version)
     * @return 同步操作建議
     */
    public SyncResult compare(ServerModList serverList, Map<String, String> clientMods) {
        SyncResult result = new SyncResult();
        
        Set<String> serverModIds = new HashSet<>();
        
        for (ServerModEntry serverMod : serverList.mods) {
            if (serverMod.serverOnly) continue; // 用戶端不需要
            serverModIds.add(serverMod.modId);
            
            if (!serverMod.required) {
                continue; // 可選模組，不強制同步
            }
            
            String clientVersion = clientMods.get(serverMod.modId);
            
            if (clientVersion == null) {
                // 用戶端缺少此模組
                result.actions.add(new SyncResult.ModAction(
                    serverMod.modId, serverMod.version, 
                    SyncResult.Action.DOWNLOAD,
                    "Missing mod required by server"));
            } else if (!clientVersion.equals(serverMod.version)) {
                // 版本不匹配
                result.actions.add(new SyncResult.ModAction(
                    serverMod.modId, serverMod.version,
                    SyncResult.Action.UPDATE,
                    "Version mismatch: client=" + clientVersion + " server=" + serverMod.version));
            } else {
                result.actions.add(new SyncResult.ModAction(
                    serverMod.modId, serverMod.version,
                    SyncResult.Action.OK, "Matched"));
            }
        }
        
        // 檢查用戶端多餘的 mod
        for (String clientModId : clientMods.keySet()) {
            if (!serverModIds.contains(clientModId)) {
                result.actions.add(new SyncResult.ModAction(
                    clientModId, clientMods.get(clientModId),
                    SyncResult.Action.EXTRA_CLIENT_MOD,
                    "Client has mod not present on server"));
            }
        }
        
        // 計算相容性
        boolean hasRequiredMissing = result.actions.stream()
            .anyMatch(a -> a.action == SyncResult.Action.DOWNLOAD || a.action == SyncResult.Action.UPDATE);
        
        result.compatible = !hasRequiredMissing;
        
        return result;
    }

    /**
     * 為伺服器端產生 mod 清單的數位簽章。
     * 使用 ECDSA 或 Ed25519。
     */
    public String signModList(ServerModList list, PrivateKey privateKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(privateKey);
            sig.update(list.toJson());
            byte[] signatureBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign mod list", e);
        }
    }

    /**
     * 用戶端驗證伺服器 mod 清單的簽章。
     */
    public boolean verifyModList(ServerModList list, PublicKey publicKey) {
        if (list.signature == null) return false;
        
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            sig.update(list.toJson());
            return sig.verify(Base64.getDecoder().decode(list.signature));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 驗證下載的模組檔案的完整性。
     */
    public boolean verifyModFile(byte[] modData, String expectedSha256) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(modData);
            String actual = bytesToHex(digest);
            return actual.equals(expectedSha256);
        } catch (Exception e) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
    
    private static String jsonEscape(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    
    private static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }
    
    private static long extractLong(String json, String key, long def) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return def;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);
        if (end < 0) return def;
        try { return Long.parseLong(json.substring(start, end).trim()); }
        catch (NumberFormatException e) { return def; }
    }
    
    private static boolean extractBool(String json, String key, boolean def) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return def;
        start += search.length();
        if (json.substring(start).startsWith("true")) return true;
        if (json.substring(start).startsWith("false")) return false;
        return def;
    }
    
    private static String extractArray(String json, String key) {
        String search = "\"" + key + "\":[";
        int start = json.indexOf(search);
        if (start < 0) return "[]";
        start += search.length() - 1;
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return json.substring(start, i + 1); }
        }
        return "[]";
    }
    
    private static List<String> splitJsonObjects(String array) {
        List<String> objects = new ArrayList<>();
        if (array == null || array.length() < 2) return objects;
        String inner = array.substring(1, array.length() - 1).trim();
        if (inner.isEmpty()) return objects;
        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '{') { if (depth == 0) objStart = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && objStart >= 0) { objects.add(inner.substring(objStart, i + 1)); objStart = -1; } }
        }
        return objects;
    }
}
