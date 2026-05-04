package com.example;

/**
 * ExampleMod — Your Francium mod starts here!
 *
 * Francium calls your main class during the LOADING phase.
 * You get lifecycle hooks for initialization, without needing
 * any @Annotation magic (unless you want Fabric-style mixins).
 *
 * Lifecycle:
 *   DISCOVERING → RESOLVING → BRIDGING → LOADING → READY
 *                                              ↑
 *                                        your code runs here
 */
public class ExampleMod {

    private static final String MOD_ID = "example-mod";
    private static final String MOD_NAME = "Example Mod";

    /**
     * Called when your mod is loaded, before the game starts.
     * This is where you register blocks, items, entities, etc.
     */
    public void onInitialize() {
        log("Hello from Francium! " + MOD_NAME + " is initializing...");

        // Register your stuff here:
        // Registry.register(Blocks.class, ...);
        // Registry.register(Items.class, ...);

        log(MOD_NAME + " initialized successfully!");
    }

    /**
     * Called on the client side when the game is ready.
     * Register rendering, keybinds, HUD elements here.
     */
    public void onInitializeClient() {
        log(MOD_NAME + " client-side init");
    }

    /**
     * Called during mod discovery — return your mod metadata.
     * Francium reads francium-mod.json automatically, but
     * you can override or extend it here.
     */
    public String getModId() {
        return MOD_ID;
    }

    // ─── Utility ────────────────────────────────────────

    private static void log(String msg) {
        System.out.println("[" + MOD_ID + "] " + msg);
    }
}
