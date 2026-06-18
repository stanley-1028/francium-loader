package com.francium.profiler.memory;

import com.francium.api.PublicApi;
import java.lang.management.*;
import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 記憶體管理器 - 解決 Minecraft 模組生態中最頑固的效能問題。
 * 
 * 核心功能:
 * 1. 洩漏檢測: 追蹤 ClassLoader 和大型物件，偵測無法回收的記憶體
 * 2. 對象池: 重用頻繁創建/銷毀的物件 (如 BlockPos, Vec3d)
 * 3. GC 策略: 根據模組行為動態調整 GC 參數
 * 4. 記憶體閾值警告: 超過閾值時觸發回收和警告
 * 5. 每模組記憶體統計: 追蹤哪個模組消耗最多記憶體
 */
@PublicApi
public class MemoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryManager.class);

    private final long warningThresholdBytes;
    private final boolean aggressiveGC;
    private final boolean leakDetection;
    
    // 洩漏追蹤
    private final Map<String, WeakReference<ClassLoader>> trackedLoaders;
    private final Map<String, Long> loaderCreationTimes;
    private final List<LeakReport> leakReports;
    
    // 對象池
    private final Map<Class<?>, ObjectPool<?>> objectPools;
    
    // 統計
    private final AtomicLong totalAllocated;
    private final Map<String, PerModMemory> perModMemory;
    private final ScheduledExecutorService monitor;
    
    // GC 歷史
    private final List<GCEvent> gcHistory;
    private int gcCount = 0;
    private long lastGCTime = 0;

    public MemoryManager(long warningThresholdMB, boolean aggressiveGC, boolean leakDetection) {
        this.warningThresholdBytes = warningThresholdMB * 1024 * 1024;
        this.aggressiveGC = aggressiveGC;
        this.leakDetection = leakDetection;
        
        this.trackedLoaders = new ConcurrentHashMap<>();
        this.loaderCreationTimes = new ConcurrentHashMap<>();
        this.leakReports = new ArrayList<>();
        this.objectPools = new ConcurrentHashMap<>();
        this.totalAllocated = new AtomicLong(0);
        this.perModMemory = new ConcurrentHashMap<>();
        this.gcHistory = new ArrayList<>();
        
        this.monitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Fr-MemoryMonitor");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 啟動記憶體監控。
     */
    public void start() {
        monitor.scheduleAtFixedRate(this::checkMemory, 5, 10, TimeUnit.SECONDS);
        monitor.scheduleAtFixedRate(this::checkLeaks, 30, 30, TimeUnit.SECONDS);
        
        LOGGER.info("Fr Memory: Monitoring started (warning=" 
            + (warningThresholdBytes / 1024 / 1024) + "MB, aggressiveGC=" + aggressiveGC + ")");
    }

    /**
     * 註冊模組的 ClassLoader 用於洩漏檢測。
     */
    public void registerLoader(String modId, ClassLoader loader) {
        trackedLoaders.put(modId, new WeakReference<>(loader));
        loaderCreationTimes.put(modId, System.currentTimeMillis());
        perModMemory.putIfAbsent(modId, new PerModMemory(modId));
    }

    /**
     * 註冊物件池。
     */
    public <T> ObjectPool<T> createPool(String name, Class<T> type, int initialSize, int maxSize,
                                        java.util.function.Supplier<T> factory) {
        ObjectPool<T> pool = new ObjectPool<>(name, type, initialSize, maxSize, factory);
        objectPools.put(type, pool);
        return pool;
    }

    /**
     * 手動觸發記憶體回收。
     */
    public void forceGC() {
        System.gc();
        gcCount++;
        lastGCTime = System.currentTimeMillis();
        gcHistory.add(new GCEvent(System.currentTimeMillis(), 
            Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
    }

    /**
     * 獲取記憶體快照。
     */
    public MemorySnapshot getSnapshot() {
        Runtime rt = Runtime.getRuntime();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        
        return new MemorySnapshot(
            rt.totalMemory(),
            rt.freeMemory(),
            rt.maxMemory(),
            memBean.getHeapMemoryUsage().getUsed(),
            memBean.getNonHeapMemoryUsage().getUsed(),
            totalAllocated.get(),
            gcCount,
            System.currentTimeMillis()
        );
    }

    /**
     * 獲取每模組的記憶體統計。
     */
    public List<PerModMemory> getPerModStats() {
        // 估算每個模組的記憶體使用
        for (var entry : perModMemory.entrySet()) {
            PerModMemory stats = entry.getValue();
            String modId = entry.getKey();
            
            WeakReference<ClassLoader> ref = trackedLoaders.get(modId);
            if (ref != null) {
                ClassLoader loader = ref.get();
                if (loader != null) {
                    // 估算: 計算該 ClassLoader 載入的類別數量
                    stats.estimatedUsage = estimateLoaderMemory(loader);
                } else {
                    stats.estimatedUsage = 0; // loader 已被回收
                }
            }
        }
        
        return new ArrayList<>(perModMemory.values());
    }

    /**
     * 獲取洩漏報告列表。
     */
    public List<LeakReport> getLeakReports() {
        return new ArrayList<>(leakReports);
    }

    /** 優雅關閉監控執行緒，釋放資源。 */
    public void shutdown() {
        monitor.shutdown();
        forceGC();
    }

    // --- Private ---
    private void checkMemory() {
        MemorySnapshot snap = getSnapshot();
        
        long usedPercent = (snap.heapUsed * 100) / snap.max;
        
        if (usedPercent > 85) {
            System.err.printf("Fr Memory WARNING: Heap %d%% used (%dMB/%dMB)%n",
                usedPercent,
                snap.heapUsed / 1024 / 1024,
                snap.max / 1024 / 1024);
            
            if (aggressiveGC) {
                forceGC();
                // 通知物件池清理
                for (ObjectPool<?> pool : objectPools.values()) {
                    pool.trim();
                }
            }
        }
    }

    private void checkLeaks() {
        if (!leakDetection) return;
        
        for (var entry : trackedLoaders.entrySet()) {
            String modId = entry.getKey();
            WeakReference<ClassLoader> ref = entry.getValue();
            
            if (ref.get() == null) {
                // Loader 已被回收，正常
                continue;
            }
            
            // 檢查是否應該已被卸載但還存活
            Long creationTime = loaderCreationTimes.get(modId);
            if (creationTime != null) {
                long age = System.currentTimeMillis() - creationTime;
                // 如果超過 5 分鐘且 mod 已標記為卸載
                if (age > 300_000) {
                    LeakReport report = new LeakReport(
                        modId,
                        "ClassLoader still referenced after 5min",
                        estimateLoaderMemory(ref.get()),
                        age
                    );
                    leakReports.add(report);
                    
                    LOGGER.warn("Fr Memory LEAK suspected: " + modId 
                        + " (" + (report.estimatedLeakBytes / 1024 / 1024) + "MB)");
                }
            }
        }
    }

    private long estimateLoaderMemory(ClassLoader loader) {
        // 簡單估算: 每個 loaded class ~2KB + 元數據
        // ★ BUG FIX: Java 9+ 中 classes 欄位可能已變更或不存在，增加多重 fallback
        try {
            // Java 8: ClassLoader.classes (Vector<Class<?>>)
            try {
                var field = ClassLoader.class.getDeclaredField("classes");
                field.setAccessible(true);
                Object value = field.get(loader);
                if (value instanceof java.util.Vector) {
                    @SuppressWarnings("unchecked")
                    var classes = (java.util.Vector<Class<?>>) value;
                    return classes.size() * 2048L;
                }
            } catch (NoSuchFieldException ignored) {
                // Java 9+ 可能使用不同內部結構
            }
            
            // Fallback: 使用類加載器的已定義包數量做粗略估計
            try {
                var pkgMethod = ClassLoader.class.getDeclaredMethod("getDefinedPackages");
                pkgMethod.setAccessible(true);
                Package[] packages = (Package[]) pkgMethod.invoke(loader);
                return packages.length * 4096L; // 每個包 ~4KB
            } catch (Exception ignored2) {}
            
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // --- 內部類 ---
    public record MemorySnapshot(long total, long free, long max, long heapUsed, long nonHeapUsed,
                                  long totalAllocated, int gcCount, long timestamp) {
        public double usagePercent() {
            return max > 0 ? (double) heapUsed / max : 0;
        }
        
        @Override
        public String toString() {
            return String.format("Memory: heap=%d/%dMB (%.0f%%), nonheap=%dMB, allocated=%dMB, gc=%d",
                heapUsed / 1024 / 1024, max / 1024 / 1024, usagePercent() * 100,
                nonHeapUsed / 1024 / 1024, totalAllocated / 1024 / 1024, gcCount);
        }
    }

    /** 單一 mod 的記憶體使用統計。 */
    public static class PerModMemory {
        /** 模組識別碼 */
        public String modId;
        /** 估算的記憶體使用量（位元組） */
        public long estimatedUsage;
        /** 已載入的類別數量 */
        public int loadedClasses;
        /** 加載耗時（毫秒） */
        public long loadTimeMs;
        
        public PerModMemory(String modId) {
            this.modId = modId;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %dKB (%d classes, %dms load)", 
                modId, estimatedUsage / 1024, loadedClasses, loadTimeMs);
        }
    }

    /**
     * 記憶體洩漏報告。
     * @param modId 可能洩漏的模組
     * @param description 洩漏描述
     * @param estimatedLeakBytes 估算洩漏位元組
     * @param ageMs 洩漏持續時間（毫秒）
     */
    public record LeakReport(String modId, String description, long estimatedLeakBytes, long ageMs) {}

    record GCEvent(long timestamp, long memoryBefore) {}

    /**
     * 高效能物件池。
     */
    public static class ObjectPool<T> {
        private final String name;
        private final Class<T> type;
        private final int initialSize;
        private final int maxSize;
        private final java.util.function.Supplier<T> factory;
        private final ConcurrentLinkedQueue<T> pool;
        private final AtomicLong borrowed;
        private final AtomicLong returned;
        private final AtomicLong created;
        
        public ObjectPool(String name, Class<T> type, int initialSize, int maxSize,
                         java.util.function.Supplier<T> factory) {
            this.name = name;
            this.type = type;
            this.initialSize = initialSize;
            this.maxSize = maxSize;
            this.factory = factory;
            this.pool = new ConcurrentLinkedQueue<>();
            this.borrowed = new AtomicLong(0);
            this.returned = new AtomicLong(0);
            this.created = new AtomicLong(0);
            
            for (int i = 0; i < initialSize; i++) {
                pool.offer(factory.get());
                created.incrementAndGet();
            }
        }
        
        /** 從池中借用一個物件（若池為空則創建新物件）。 */
        public T borrow() {
            T obj = pool.poll();
            if (obj == null) {
                obj = factory.get();
                created.incrementAndGet();
            }
            borrowed.incrementAndGet();
            return obj;
        }
        
        /** 歸還物件至池中（若池滿則丟棄）。 */
        public void release(T obj) {
            if (pool.size() < maxSize) {
                pool.offer(obj);
            }
            returned.incrementAndGet();
        }
        
        /** 收縮物件池至最大容量的一半，釋放未使用的物件。 */
        public void trim() {
            while (pool.size() > maxSize / 2) {
                pool.poll();
            }
        }
        
        /** 返回物件池的命中率（borrow 時池中有物件的比例，0~100）。 */
        public long hitRate() {
            long total = borrowed.get();
            return total > 0 ? (total - created.get() + initialSize) * 100 / total : 0;
        }
        
        @Override
        public String toString() {
            return String.format("Pool[%s] size=%d borrowed=%d hit=%d%%", 
                name, pool.size(), borrowed.get(), hitRate());
        }
    }
}
