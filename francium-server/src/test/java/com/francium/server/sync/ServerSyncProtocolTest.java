package com.francium.server.sync;

import com.francium.server.sync.ServerSyncProtocol.ServerModEntry;
import com.francium.server.sync.ServerSyncProtocol.ServerModList;
import com.francium.server.sync.ServerSyncProtocol.SyncResult;
import com.francium.server.sync.ServerSyncProtocol.SyncResult.ModAction;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServerSyncProtocolTest {

    @Test
    void serverModListFromJson() {
        String json = """
                {"serverId":"srv-1","mcVersion":"1.21","timestamp":1000,"mods":[{"id":"sodium","ver":"0.6.0","sha256":"abc","required":true}]}
                """;
        ServerModList list = ServerModList.fromJson(json);
        assertEquals("srv-1", list.serverId);
        assertEquals("1.21", list.mcVersion);
        assertEquals(1, list.mods.size());
        assertEquals("sodium", list.mods.getFirst().modId);
    }

    @Test
    void serverModListFromJsonEmptyMods() {
        ServerModList list = ServerModList.fromJson("{\"serverId\":\"s1\",\"mcVersion\":\"1.21\",\"mods\":[]}");
        assertTrue(list.mods.isEmpty());
    }

    @Test
    void serverModListFromJsonNullReturnsEmpty() {
        ServerModList list = ServerModList.fromJson(null);
        assertNotNull(list);
    }

    @Test
    void serverModEntryToJson() {
        ServerModEntry entry = new ServerModEntry();
        entry.modId = "test-mod";
        entry.version = "1.0.0";
        entry.sha256 = "abc";
        entry.required = true;
        String json = entry.toJson();
        assertTrue(json.contains("test-mod"));
        assertTrue(json.contains("1.0.0"));
        assertTrue(json.contains("true"));
    }

    @Test
    void signModListReturnsSignature() throws Exception {
        // ECDSA signatures are non-deterministic — just verify signing doesn't throw
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(256);
        KeyPair kp = gen.generateKeyPair();

        ServerSyncProtocol protocol = new ServerSyncProtocol();
        ServerModList list = ServerModList.fromJson("{\"serverId\":\"test\",\"mcVersion\":\"1.21\",\"mods\":[]}");

        String signature = protocol.signModList(list, kp.getPrivate());
        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    void verifyModFileSha256() {
        ServerSyncProtocol protocol = new ServerSyncProtocol();
        byte[] data = "hello world".getBytes();
        String expected = bytesToHex(sha256(data));
        assertTrue(protocol.verifyModFile(data, expected));
        assertFalse(protocol.verifyModFile(data, "0000000000000000000000000000000000000000000000000000000000000000"));
    }

    @Test
    void compareDetectsMissingMod() {
        ServerSyncProtocol protocol = new ServerSyncProtocol();
        ServerModList serverList = ServerModList.fromJson(
            "{\"serverId\":\"s\",\"mcVersion\":\"1.21\",\"mods\":[{\"id\":\"sodium\",\"ver\":\"0.6.0\",\"sha256\":\"abc\",\"required\":true}]}");

        SyncResult result = protocol.compare(serverList, Map.of());
        assertFalse(result.compatible);
        assertEquals(1, result.actions.size());
        assertEquals(SyncResult.Action.DOWNLOAD, result.actions.getFirst().action());
    }

    @Test
    void compareDetectsOutdatedMod() {
        ServerSyncProtocol protocol = new ServerSyncProtocol();
        ServerModList serverList = ServerModList.fromJson(
            "{\"serverId\":\"s\",\"mcVersion\":\"1.21\",\"mods\":[{\"id\":\"sodium\",\"ver\":\"0.6.0\",\"sha256\":\"abc\",\"required\":true}]}");

        SyncResult result = protocol.compare(serverList, Map.of("sodium", "0.5.0"));
        assertFalse(result.compatible);
        assertEquals(SyncResult.Action.UPDATE, result.actions.getFirst().action());
    }

    @Test
    void compareReportsCompatibleWhenAllMatch() {
        ServerSyncProtocol protocol = new ServerSyncProtocol();
        ServerModList serverList = ServerModList.fromJson(
            "{\"serverId\":\"s\",\"mcVersion\":\"1.21\",\"mods\":[{\"id\":\"sodium\",\"ver\":\"0.6.0\",\"sha256\":\"abc\",\"required\":true}]}");

        SyncResult result = protocol.compare(serverList, Map.of("sodium", "0.6.0"));
        assertTrue(result.compatible);
    }

    @Test
    void modActionRecord() {
        ModAction action = new ModAction("sodium", "0.6.0", SyncResult.Action.DOWNLOAD, "missing");
        assertEquals("sodium", action.modId());
        assertEquals(SyncResult.Action.DOWNLOAD, action.action());
        assertEquals("missing", action.reason());
    }

    // ─── helpers ───

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
