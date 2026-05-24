package com.francium.profiler.memory;

import com.francium.profiler.memory.MemoryManager.ObjectPool;
import com.francium.profiler.memory.MemoryManager.PerModMemory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryManagerTest {

    // ─── ObjectPool tests ──────────────────────────────────

    @Test
    void objectPoolBorrowAndRelease() {
        ObjectPool<StringBuilder> pool = new ObjectPool<>("test", StringBuilder.class, 1, 10, StringBuilder::new);
        StringBuilder sb = pool.borrow();
        assertNotNull(sb);
        sb.append("hello");
        pool.release(sb);
        // With initialSize=1, the released StringBuilder is the only one in the pool
        StringBuilder sb2 = pool.borrow();
        assertEquals("hello", sb2.toString());
    }

    @Test
    void objectPoolExhaustedCreatesNew() {
        ObjectPool<StringBuilder> pool = new ObjectPool<>("test", StringBuilder.class, 1, 5, StringBuilder::new);
        StringBuilder sb1 = pool.borrow();
        StringBuilder sb2 = pool.borrow();
        assertNotNull(sb1);
        assertNotNull(sb2);
        assertNotSame(sb1, sb2);
    }

    @Test
    void objectPoolDoesNotExceedMaxSize() {
        ObjectPool<StringBuilder> pool = new ObjectPool<>("test", StringBuilder.class, 0, 3, StringBuilder::new);
        // Fill pool with 4 items — max is 3, so 4th should be dropped
        pool.release(new StringBuilder("a"));
        pool.release(new StringBuilder("b"));
        pool.release(new StringBuilder("c"));
        pool.release(new StringBuilder("d")); // dropped: pool already at max 3

        // Borrow 4 times — first 3 should be the ones we put in
        StringBuilder r1 = pool.borrow();
        StringBuilder r2 = pool.borrow();
        StringBuilder r3 = pool.borrow();
        StringBuilder r4 = pool.borrow(); // created new (pool was empty after 3 borrows)

        assertEquals("a", r1.toString());
        assertEquals("b", r2.toString());
        assertEquals("c", r3.toString());
        assertEquals("", r4.toString()); // newly created empty StringBuilder
    }

    @Test
    void objectPoolTrimReducesSize() {
        ObjectPool<StringBuilder> pool = new ObjectPool<>("trim", StringBuilder.class, 10, 20, StringBuilder::new);
        for (int i = 0; i < 10; i++) pool.borrow();
        for (int i = 0; i < 8; i++) pool.release(new StringBuilder());
        pool.trim();
        for (int i = 0; i < 15; i++) assertNotNull(pool.borrow());
    }

    @Test
    void objectPoolHitRate() {
        ObjectPool<StringBuilder> pool = new ObjectPool<>("hit", StringBuilder.class, 2, 10, StringBuilder::new);
        pool.borrow();
        pool.borrow();
        pool.borrow(); // created
        pool.borrow(); // created
        pool.borrow(); // created
        pool.release(new StringBuilder());
        pool.release(new StringBuilder());
        pool.borrow(); // hit
        long rate = pool.hitRate();
        assertTrue(rate > 0 && rate <= 100);
    }

    @Test
    void objectPoolToString() {
        ObjectPool<StringBuilder> pool = new ObjectPool<>("my-pool", StringBuilder.class, 1, 5, StringBuilder::new);
        String str = pool.toString();
        assertTrue(str.contains("my-pool"));
        assertTrue(str.contains("size="));
        assertTrue(str.contains("borrowed="));
        assertTrue(str.contains("hit="));
    }

    // ─── MemoryManager lifecycle tests ─────────────────────

    @Test
    void startAndShutdownLifecycle() {
        MemoryManager mm = new MemoryManager(512L, false, false);
        mm.start();
        mm.shutdown();
        mm.shutdown(); // double shutdown safe
    }

    @Test
    void registerLoader() {
        MemoryManager mm = new MemoryManager(512L, false, false);
        mm.start();
        mm.registerLoader("testmod", MemoryManagerTest.class.getClassLoader());
        mm.shutdown();
    }

    @Test
    void forceGCExecutes() {
        MemoryManager mm = new MemoryManager(512L, true, true);
        mm.start();
        mm.forceGC();
        mm.shutdown();
    }

    @Test
    void aggressiveGCMode() {
        MemoryManager mm = new MemoryManager(512L, true, false);
        mm.start();
        mm.registerLoader("aggressive-test", getClass().getClassLoader());
        mm.forceGC();
        mm.shutdown();
    }

    @Test
    void leakDetectionMode() {
        MemoryManager mm = new MemoryManager(512L, false, true);
        mm.start();
        mm.registerLoader("leak-test", getClass().getClassLoader());
        mm.shutdown();
    }

    // ─── PerModMemory tests ────────────────────────────────

    @Test
    void perModMemoryConstructorAndToString() {
        PerModMemory mem = new PerModMemory("test-mod");
        assertEquals("test-mod", mem.modId);
        String str = mem.toString();
        assertTrue(str.contains("test-mod"));
    }

    @Test
    void perModMemoryFields() {
        PerModMemory mem = new PerModMemory("data-mod");
        mem.estimatedUsage = 2048L;
        mem.loadedClasses = 42;
        mem.loadTimeMs = 500L;
        assertEquals(2048L, mem.estimatedUsage);
        assertEquals(42, mem.loadedClasses);
        assertEquals(500L, mem.loadTimeMs);
    }

    @Test
    void perModMemoryDefaultValues() {
        PerModMemory mem = new PerModMemory("default");
        assertEquals(0L, mem.estimatedUsage);
        assertEquals(0, mem.loadedClasses);
        assertEquals(0L, mem.loadTimeMs);
    }

    // ─── LeakReport tests ──────────────────────────────────

    @Test
    void leakReportRecord() {
        MemoryManager.LeakReport report = new MemoryManager.LeakReport("leaky-mod", "classloader not released", 1024L, 5000L);
        assertEquals("leaky-mod", report.modId());
        assertEquals("classloader not released", report.description());
        assertEquals(1024L, report.estimatedLeakBytes());
        assertEquals(5000L, report.ageMs());
    }

    @Test
    void leakReportToString() {
        MemoryManager.LeakReport report = new MemoryManager.LeakReport("mod-x", "held reference", 256L, 1000L);
        String str = report.toString();
        assertTrue(str.contains("mod-x"));
        assertTrue(str.contains("held reference"));
    }
}
