package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bare-bones test to verify your mod's basic sanity.
 * Francium mods can be tested without Minecraft running!
 */
class ExampleModTest {

    @Test
    void testModHasId() {
        ExampleMod mod = new ExampleMod();
        assertNotNull(mod.getModId());
        assertFalse(mod.getModId().isBlank());
    }

    @Test
    void testModDoesNotExplodeOnInit() {
        ExampleMod mod = new ExampleMod();
        // Should not throw
        assertDoesNotThrow(mod::onInitialize);
    }
}
