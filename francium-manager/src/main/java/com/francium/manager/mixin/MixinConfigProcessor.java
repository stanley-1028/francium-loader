package com.francium.manager.mixin;

import com.francium.api.PublicApi;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Discovers and registers Mixin configuration files from mod JARs.
 * Reads mixins.json from each JAR's root.
 */
@PublicApi
public class MixinConfigProcessor {

    private static final Set<String> MIXIN_CONFIG_FILES = new LinkedHashSet<>();
    private static final List<String> KNOWN_CONFIG_NAMES = Arrays.asList(
            "mixins.json",
            "francium.mixins.json",
            "modid.mixins.json"
    );

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(MixinConfigProcessor.class.getName());

    /**
     * Scan a directory of mod JARs for Mixin configs.
     */
    public int discoverAndRegister(File modsDir) {
        int count = 0;
        File[] jarFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) return 0;

        for (File jar : jarFiles) {
            try (ZipFile zip = new ZipFile(jar)) {
                for (String configName : KNOWN_CONFIG_NAMES) {
                    ZipEntry entry = zip.getEntry(configName);
                    if (entry != null) {
                        String content = readEntry(zip, entry);
                        if (isMixinConfig(content)) {
                            MIXIN_CONFIG_FILES.add(configName);
                            count++;
                            LOG.info("[MixinConfigProcessor] Registered mixin config: " + configName + " from " + jar.getName());
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                // Skip invalid JARs
            }
        }
        return count;
    }

    /**
     * Parse a mixins.json from within a JAR and collect mixin class names.
     */
    public List<String> parseMixinClasses(File jarFile, String configPath) {
        List<String> mixinClasses = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jarFile)) {
            ZipEntry entry = zip.getEntry(configPath);
            if (entry == null) return mixinClasses;

            String content = readEntry(zip, entry);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            if (json.has("mixins")) {
                for (JsonElement elem : json.getAsJsonArray("mixins")) {
                    mixinClasses.add(elem.getAsString());
                }
            }
            if (json.has("client")) {
                for (JsonElement elem : json.getAsJsonArray("client")) {
                    mixinClasses.add(elem.getAsString());
                }
            }
            if (json.has("server")) {
                for (JsonElement elem : json.getAsJsonArray("server")) {
                    mixinClasses.add(elem.getAsString());
                }
            }
        } catch (IOException e) {
            // skip
        }
        return mixinClasses;
    }

    public static Set<String> getRegisteredConfigs() {
        return Collections.unmodifiableSet(MIXIN_CONFIG_FILES);
    }

    public static void registerConfig(String config) {
        MIXIN_CONFIG_FILES.add(config);
    }

    private boolean isMixinConfig(String content) {
        try {
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            return json.has("package") || json.has("mixins") || json.has("client") || json.has("server");
        } catch (Exception e) {
            return false;
        }
    }

    private String readEntry(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream is = zip.getInputStream(entry)) {
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }
}
