package com.francium.server.sync;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import java.security.*;

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
public class ServerSyncProtocol {
    
    /**
     * 伺服器端 mod 清單。
     */
    public static class ServerModList {
        public String serverId;
        public String serverName;
        public String mcVersion;
        public String franciumVersion;
        public long timestamp;
        public List<ServerModEntry> mods;
        public String signature; // 伺服器簽名
        
        public byte[] toJson() {
            // 簡化: 實際使用 Gson
            StringBuilder sb = new StringBuilder();
            sb.append("{\"serverId\":\"").append(serverId).append("\",");
            sb.append("\"mcVersion\":\"").append(mcVersion).append("\",");
            sb.append("\"mods\":[");
            for (int i = 0; i < mods.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(mods.get(i).toJson());
            }
            sb.append("]}");
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }
        
        public static ServerModList fromJson(String json) {
            // 簡化實現；實際使用 Gson
            return new ServerModList();
        }
    }

    /**
     * 單個模組資訊。
     */
    public static class ServerModEntry {
        public String modId;
        public String name;
        public String version;
        public String sha256;
        public long size;
        public String downloadUrl; // 可選: 直接提供下載連結
        public boolean required;   // false = optional mod
        public boolean serverOnly; // client doesn't need this mod
        public String configTemplate; // 伺服器推薦的設定檔 (base64)
        
        public String toJson() {
            return String.format(
                "{\"id\":\"%s\",\"ver\":\"%s\",\"sha256\":\"%s\",\"required\":%b,\"serverOnly\":%b}",
                modId, version, sha256, required, serverOnly);
        }
    }

    /**
     * 同步結果。
     */
    public static class SyncResult {
        public boolean compatible;
        public List<ModAction> actions = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        
        public enum Action { DOWNLOAD, UPDATE, REMOVE, OK, EXTRA_CLIENT_MOD }
        
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
}
