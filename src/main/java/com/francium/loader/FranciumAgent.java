package com.francium.loader;

import com.francium.manager.mixin.MixinConfigProcessor;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java Agent entry point for Francium Loader.
 * Hooks into the JVM at startup to inject mod classes and process Mixin configurations
 * before Minecraft classes are loaded.
 */
public class FranciumAgent {

    private static final Logger LOG = Logger.getLogger(FranciumAgent.class.getName());

    public static void premain(String agentArgs, Instrumentation inst) {
        LOG.info("[FranciumAgent] Initializing Francium Loader Agent v" + getVersion());

        try {
            // 1. Parse agent args (mods directory path)
            String modsDir = parseAgentArgs(agentArgs);
            LOG.info("[FranciumAgent] Mods directory: " + modsDir);

            // 2. Discover and process mod JARs
            File modsDirectory = new File(modsDir != null ? modsDir : "mods");
            if (!modsDirectory.exists()) {
                modsDirectory.mkdirs();
            }

            File[] jarFiles = modsDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                LOG.info("[FranciumAgent] No mod JARs found in " + modsDirectory.getAbsolutePath());
                return;
            }

            LOG.info("[FranciumAgent] Found " + jarFiles.length + " mod JAR(s)");

            // 3. Add each mod JAR to the system classpath via Instrumentation
            for (File jarFile : jarFiles) {
                try {
                    inst.appendToSystemClassLoaderSearch(new JarFile(jarFile));
                    LOG.info("[FranciumAgent] Added to classpath: " + jarFile.getName());
                } catch (Exception e) {
                    LOG.warning("[FranciumAgent] Failed to add " + jarFile.getName() + ": " + e.getMessage());
                }
            }

            // 4. Process Mixin configurations from all mod JARs
            try {
                MixinConfigProcessor processor = new MixinConfigProcessor();
                int configs = processor.discoverAndRegister(modsDirectory);
                LOG.info("[FranciumAgent] Registered " + configs + " Mixin configuration(s)");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "[FranciumAgent] Mixin processing error: " + e.getMessage(), e);
            }

            LOG.info("[FranciumAgent] Agent initialization complete");

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[FranciumAgent] Failed to initialize: " + e.getMessage(), e);
        }
    }

    private static String parseAgentArgs(String args) {
        if (args == null || args.isEmpty()) return null;
        for (String arg : args.split(";")) {
            if (arg.startsWith("modsDir=")) {
                return arg.substring(8);
            }
        }
        return args; // treat entire string as mods dir path
    }

    private static String getVersion() {
        String v = FranciumAgent.class.getPackage().getImplementationVersion();
        return v != null ? v : "1.7.0";
    }
}
