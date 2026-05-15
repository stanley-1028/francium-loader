# Francium Mod Loader — 開發者文檔 / Developer Guide

> 給開發人員的深度技術文檔：架構設計、擴展點、測試模式。

---

## 目錄 / Table of Contents

1. [架構總覽 / Architecture Overview](#架構總覽--architecture-overview)
2. [SAT 依賴解析器 / SAT Dependency Resolver](#sat-依賴解析器--sat-dependency-resolver)
3. [DAG 調度器 / DAG Scheduler](#dag-調度器--dag-scheduler)
4. [AI 橋接 / AI Bridge](#ai-橋接--ai-bridge)
5. [性能分析器 / Profiler](#性能分析器--profiler)
6. [如何添加新的依賴解析策略 / Adding a Resolver Strategy](#如何添加新的依賴解析策略--adding-a-resolver-strategy)
7. [如何添加新的分析器指標 / Adding a Profiler Metric](#如何添加新的分析器指標--adding-a-profiler-metric)
8. [測試模式 / Test Patterns](#測試模式--test-patterns)
9. [模組生命週期 / Mod Lifecycle](#模組生命週期--mod-lifecycle)

---

## 架構總覽 / Architecture Overview

Francium 採用模塊化架構，各子項目可獨立編譯測試：

```
francium-loader/
│
├── francium-core/           # 核心：DAG 圖、ClassLoader、加載器入口
├── francium-ai-bridge/      # AI 版本橋接（字節碼分析 + 映射 + 適配器生成）
├── francium-resolver/       # SAT 依賴求解器（DPLL + 約束傳播）
├── francium-manager/        # 套件管理器（install/search/update）
├── francium-profiler/       # 記憶體分析器（洩漏偵測 + 物件池 + GC）
├── francium-server/         # 伺服器同步協議 + 安全驗證
└── francium-mod-template/   # 模組開發模板
```

### 數據流 / Data Flow

```
Mod JARs
    │
    ▼
┌─────────────────┐
│  Discovery      │  ← ModManifest 掃描
└────────┬────────┘
         ▼
┌─────────────────┐
│  SAT Resolver   │  ← 依賴求解 + 版本選擇
└────────┬────────┘
         ▼
┌─────────────────┐
│  AI Bridge      │  ← 跨版本適配（可選）
└────────┬────────┘
         ▼
┌─────────────────┐
│  DAG Layering   │  ← 拓撲分層
└────────┬────────┘
         ▼
┌─────────────────┐
│  Parallel Load  │  ← ForkJoinPool 並行加載
└────────┬────────┘
         ▼
┌─────────────────┐
│  Lifecycle      │  ← preLaunch → launch → postLaunch
└─────────────────┘
```

---

## SAT 依賴解析器 / SAT Dependency Resolver

**包**: `com.francium.resolver`
**核心類**: `SATDependencyResolver.java`

### 原理

將依賴解析視為約束滿足問題 (CSP)，使用 DPLL 風格的回溯算法。與傳統的 npm/Maven 解析器不同，SAT 求解器能：

1. 自動檢測版本衝突
2. 嘗試所有可能的版本組合
3. 精確指出衝突源
4. 支援複雜的版本約束（`^1.2.0`、`~1.2.0`、`>=1.0 <2.0`）

### 核心算法 / DPLL Algorithm

```
function solve(未賦值變數, 賦值):
    if 所有變數已賦值 → return 成功
    var = MRV(未賦值變數)              // 最小剩餘值啟發式
    for each value in LCV(var):        // 最少約束值排序
        if 前向檢查(var, value):
            賦值[var] = value
            result = solve(未賦值變數 - var, 賦值)
            if result == 成功 → return 成功
            回溯(var)
    return 失敗
```

### 關鍵類

| 類 | 說明 |
|---|---|
| `SATDependencyResolver` | 主解析器，實現 DPLL 回溯算法 |
| `DependencyConstraint` | 語義化版本約束（`>=1.0 <2.0`、`^1.2.0`等） |
| `SemanticVersion` | SemVer 2.0 實現，支援 pre-release 和比較 |

### SemanticVersion API

```java
// 解析
var v = SemanticVersion.parse("1.20.4-pre3");
v.major();      // 1
v.minor();      // 20
v.patch();      // 4
v.preRelease(); // "pre3"

// 比較
v.compareTo(other);  // -1 / 0 / 1
v.nextMajor();       // 2.0.0
v.nextMinor();       // 1.21.0
v.nextPatch();       // 1.20.5
```

### DependencyConstraint API

```java
var constraint = new DependencyConstraint(">=1.20.0 <2.0.0");
constraint.satisfiedBy(version);        // 檢查版本是否滿足
constraint.bestMatch(versionList);      // 最佳匹配版本
constraint.intersect(other);            // 求交集
constraint.isWildcard();                // 是否為通配符 *
```

---

## DAG 調度器 / DAG Scheduler

**包**: `com.francium.graph`
**核心類**: `ModGraph.java`, `ParallelModClassLoader.java`

### 原理

模組依賴關係形成有向無環圖 (DAG)。使用 Kahn 演算法進行拓撲排序和分層，同一層的模組互相獨立，可用 ForkJoinPool 並行加載。

### ModGraph API

```java
ModGraph graph = new ModGraph();

// 添加模組
graph.addMod(manifest, resolvedVersion);

// 添加外部 Provider（如 minecraft）
graph.addExternalProvider("minecraft");

// 獲取拓撲分層
List<Set<String>> layers = graph.getLayers();

// 性能估計
long parallel = graph.estimateParallelLoadTime();
long sequential = graph.estimateSequentialLoadTime();
double speedup = graph.getSpeedupRatio();
```

### ParallelModClassLoader

繼承 `java.net.URLClassLoader`，每個模組使用獨立的 ClassLoader 實例實現隔離：

```java
public class ParallelModClassLoader extends URLClassLoader {
    // 使用 ForkJoinPool 並行加載同層模組
    public void loadLayer(Set<String> modIds, ForkJoinPool pool);

    // 類加載委派：先檢查自身，再委派給依賴的 ClassLoader
    @Override
    protected Class<?> loadClass(String name, boolean resolve);
}
```

### 異常處理

| 異常 | 說明 |
|---|---|
| `CircularDependencyException` | 檢測到循環依賴，附帶循環路徑 |
| `ModConflictException` | 同 ID 但不同版本的模組衝突 |

---

## AI 橋接 / AI Bridge

**包**: `com.francium.ai`
**核心類**: `VersionBridge.java`, `BytecodeAnalyzer.java`, `AdapterGenerator.java`

AI 版本橋接是 Francium 最具革命性的功能。Minecraft 每次改版後 method/field 名稱會混淆變化，但語義結構一致。

### 工作流程

```
Mod JAR (for MC 1.20.4)
    │
    ▼
BytecodeAnalyzer      ── 用 ASM 解析字節碼
    │                     提取外部方法調用
    ▼
MappingDatabase       ── 查詢多版本映射
    │                     (Mojang / Yarn / SRG)
    ▼
CompatibilityPredictor ── 加權特徵模型排序候選
    │                     描述符、名稱相似度、結構相似度
    ▼
AdapterGenerator      ── 生成橋接 bytecode
    │                     注入到 classpath
    ▼
Mod 在 MC 1.21 透明運行
```

### 核心類

| 類 | 說明 |
|---|---|
| `VersionBridge` | 橋接器入口，協調各組件流程 |
| `BytecodeAnalyzer` | ASM 字節碼分析，提取跨版本調用點 |
| `MethodSignature` | 方法簽名 + 多維相似度計算 |
| `MappingDatabase` | 多版本映射數據庫（Mojang/Yarn/SRG） |
| `CompatibilityPredictor` | ML 加權特徵預測器 |
| `AdapterGenerator` | 自動生成橋接適配器 bytecode |

### 添加新的映射源

```java
// 在 MappingDatabase 中註冊新的映射源
public class MappingDatabase {
    public void registerMappingSource(String name, MappingProvider provider);

    public void loadMojangMappings(Path mappingsFile);
    public void loadYarnMappings(Path mappingsFile);
    public void loadSrgMappings(Path mappingsFile);
}
```

---

## 性能分析器 / Profiler

**包**: `com.francium.profiler`
**核心類**: `MemoryManager.java`, `ProfilerReport.java`

### MemoryManager API

```java
MemoryManager mem = new MemoryManager();

// 獲取記憶體快照
MemorySnapshot snap = mem.snapshot();
snap.totalMB();          // 總記憶體
snap.usedMB();           // 已使用
snap.leakRiskMods();     // 洩漏風險模組列表

// 物件池
ObjectPool pool = mem.createPool("textures", 100);
Texture tex = pool.acquire();
pool.release(tex);

// GC 策略
mem.setGcStrategy(GcStrategy.AGGRESSIVE);
mem.setGcStrategy(GcStrategy.ADAPTIVE);
mem.setGcStrategy(GcStrategy.CONSERVATIVE);
```

### ProfilerReport

```java
ProfilerReport report = new ProfilerReport(mem.snapshot());
report.toJson();          // JSON 格式報告
report.toHtml();          // HTML 可視化報告
report.saveTo(Path.of("report.html"));
```

---

## 如何添加新的依賴解析策略 / Adding a Resolver Strategy

SAT 解析器支援插件式策略。要添加新的解析策略：

### 步驟 1：實現 ResolverStrategy 接口

```java
package com.francium.resolver.strategy;

public interface ResolverStrategy {
    String name();
    ResolveResult resolve(Map<String, List<SemanticVersion>> versions,
                          Map<String, List<DependencyConstraint>> constraints);
}
```

### 步驟 2：實現策略類

```java
package com.francium.resolver.strategy;

public class GreedyResolverStrategy implements ResolverStrategy {

    @Override
    public String name() {
        return "greedy";
    }

    @Override
    public ResolveResult resolve(Map<String, List<SemanticVersion>> versions,
                                  Map<String, List<DependencyConstraint>> constraints) {
        // 貪婪策略：總是選擇最新版本
        Map<String, SemanticVersion> assignment = new HashMap<>();
        for (var entry : versions.entrySet()) {
            String modId = entry.getKey();
            List<SemanticVersion> candidates = entry.getValue();
            // 選擇滿足所有約束的最新版本
            SemanticVersion best = candidates.stream()
                .filter(v -> constraints.getOrDefault(modId, List.of())
                    .stream().allMatch(c -> c.satisfiedBy(v)))
                .max(Comparator.naturalOrder())
                .orElse(null);
            if (best != null) {
                assignment.put(modId, best);
            }
        }
        return new ResolveResult(assignment, Collections.emptyList());
    }
}
```

### 步驟 3：在 SATDependencyResolver 中註冊

```java
SATDependencyResolver resolver = new SATDependencyResolver();
resolver.registerStrategy(new GreedyResolverStrategy());

// 使用策略名稱切換
resolver.setActiveStrategy("greedy");

// 或在構建時指定
SATDependencyResolver resolver = SATDependencyResolver.withStrategy("greedy");
```

### 內建策略

| 策略名稱 | 說明 |
|---|---|
| `dpll` (默認) | DPLL 回溯 + MRV/LCV 啟發式 |
| `greedy` | 貪婪最新版本（快速但不保證最優） |
| `conservative` | 保守策略，偏好最低版本（穩定） |

---

## 如何添加新的分析器指標 / Adding a Profiler Metric

Profiler 支援插件式指標。要添加新的性能指標：

### 步驟 1：實現 MetricProvider

```java
package com.francium.profiler.metric;

public interface MetricProvider {
    String metricName();
    MetricSnapshot collect();
}
```

### 步驟 2：實現指標收集器

```java
package com.francium.profiler.metric;

public class CpuUsageMetric implements MetricProvider {

    private final OperatingSystemMXBean osBean =
        ManagementFactory.getOperatingSystemMXBean();

    @Override
    public String metricName() {
        return "cpu_usage";
    }

    @Override
    public MetricSnapshot collect() {
        double cpuLoad = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();
        return new MetricSnapshot("cpu_usage", Map.of(
            "load_average", cpuLoad,
            "available_processors", availableProcessors,
            "timestamp", System.currentTimeMillis()
        ));
    }
}
```

### 步驟 3：在 MemoryManager 中註冊

```java
MemoryManager mem = new MemoryManager();
mem.registerMetric(new CpuUsageMetric());
mem.registerMetric(new GcActivityMetric());
mem.registerMetric(new ThreadCountMetric());

// 一次性收集所有註冊的指標
Map<String, MetricSnapshot> allMetrics = mem.collectAllMetrics();
```

### 內建指標

| 指標名稱 | 說明 |
|---|---|
| `memory` | 記憶體使用量 + 洩漏檢測 |
| `gc_activity` | GC 頻率和暫停時間 |
| `classloader` | ClassLoader 統計 |
| `thread_count` | 線程數監控 |
| `object_pool` | 物件池命中率 |

---

## 測試模式 / Test Patterns

Francium 包含 70+ 項測試，採用兩種測試風格：

### 風格 1：獨立 `main()` 測試（舊式，適用於無 Gradle 環境）

這些測試可直接用 `java` 命令運行，無需 JUnit 依賴：

```java
package com.francium;

/** SemanticVersion 手寫測試（直接用 java 運行，無需 JUnit） */
public class SemanticVersionTest {
    static int passed = 0, failed = 0;
    static void check(boolean c, String m) {
        if (!c) { failed++; System.out.println("  FAIL " + m); }
        else { passed++; System.out.println("  PASS " + m); }
    }
    public static void main(String[] args) {
        var v = SemanticVersion.parse("1.20.4");
        check(v != null, "parse should succeed");
        check(v.major() == 1, "major should be 1");
        check(v.minor() == 20, "minor should be 20");
        check(v.patch() == 4, "patch should be 4");
        check("1.20.4".equals(v.toString()), "toString should be 1.20.4");

        var a = SemanticVersion.parse("1.20.4");
        var b = SemanticVersion.parse("1.21.0");
        check(a.compareTo(b) < 0, "1.20.4 < 1.21.0");
        check(b.compareTo(a) > 0, "1.21.0 > 1.20.4");
        check(a.compareTo(SemanticVersion.parse("1.20.4")) == 0, "1.20.4 == 1.20.4");

        System.out.println("  SemanticVersion: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
```

### 風格 2：JUnit 5 測試（推薦用於新代碼）

```java
package com.francium;

import com.francium.graph.ModGraph;
import com.francium.loader.ModManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class ModGraphComprehensiveTest {

    private ModGraph graph;

    @BeforeEach
    void setUp() {
        graph = new ModGraph();
    }

    @Test
    @DisplayName("空圖沒有分層")
    void testEmptyGraph() {
        assertTrue(graph.getLayers().isEmpty());
        assertEquals(0, graph.getModCount());
    }

    @Test
    @DisplayName("獨立模組在同一層")
    void testIndependentMods() {
        addMod("modA", "1.0.0", 100, Map.of());
        addMod("modB", "1.0.0", 200, Map.of());
        List<Set<String>> layers = graph.getLayers();
        assertEquals(1, layers.size());
        assertEquals(2, layers.get(0).size());
    }

    @Test
    @DisplayName("循環依賴拋出異常")
    void testCircularDependency() {
        addMod("modA", "1.0.0", 100, Map.of("modB", ">=1.0.0"));
        addMod("modB", "1.0.0", 100, Map.of("modC", ">=1.0.0"));
        addMod("modC", "1.0.0", 100, Map.of("modA", ">=1.0.0"));
        assertThrows(ModGraph.CircularDependencyException.class,
            () -> graph.getLayers());
    }

    // Helper
    private void addMod(String id, String version, long time, Map<String, String> deps) {
        var builder = ModManifest.builder(id, version).mainClass("ex." + id);
        deps.forEach(builder::dependency);
        graph.addMod(builder.build(), version);
    }
}
```

### 測試目錄結構

```
francium-resolver/
└── src/test/java/com/francium/
    ├── SemanticVersionTest.java           # 風格 1: main()
    ├── DependencyConstraintTest.java      # 風格 1: main()
    └── SATDependencyResolverTest.java     # 風格 1: main()

francium-core/
└── src/test/java/com/francium/
    ├── ModGraphTest.java                  # 風格 1: main()
    └── ModGraphComprehensiveTest.java     # 風格 2: JUnit 5

francium-ai-bridge/
└── src/test/java/com/francium/
    ├── CrossVersionTest.java              # 風格 1: main()
    └── CompatibilityPredictorTest.java    # 風格 1: main()
```

### 運行測試

```bash
# 通過 Gradle 運行全部測試（JUnit 5）
./gradlew test

# 獨立運行舊式測試
java francium-resolver/src/test/java/com/francium/SemanticVersionTest.java

# 批量運行所有舊式測試
java TestRunner.java
```

### 測試命名規範

- 檔案：`{Target}Test.java`
- 方法（JUnit 5）：`test{Scenario}` 或 `{method}_{scenario}_expects_{result}`
- 斷言：每個測試一個核心斷言 + 輔助檢查
- Helper 方法：使用 static factory 或 `@BeforeEach` 減少重複

---

## 模組生命週期 / Mod Lifecycle

```
INIT
  │
  ▼
DISCOVERING    ← 掃描 mods/ 目錄，解析 francium-mod.json
  │
  ▼
RESOLVING     ← SAT 求解依賴，選擇版本
  │
  ▼
BRIDGING      ← AI 版本橋接（可選）
  │
  ▼
LAYERING      ← DAG 拓撲分層
  │
  ▼
LOADING       ← ForkJoinPool 並行加載
  │             每層完成後觸發 preLaunch
  │
  ▼
READY         ← 所有模組已加載
  │             觸發 launch → postLaunch
  │
  ▼
RUNNING       ← Minecraft 運行中
  │
  ▼
SHUTDOWN      ← 清理資源
```

### 入口點類型 / Entry Points

| 類型 | 觸發時機 | 適用場景 |
|---|---|---|
| `preLaunch` | 該層加載完畢後 | 初始化數據、註冊監聽器 |
| `launch` | 所有模組加載完畢（默認） | 大多數業務邏輯 |
| `postLaunch` | Minecraft 主菜單加載後 | UI 注入、延遲初始化 |

### 模組 Manifest (`francium-mod.json`)

```json
{
    "modId": "my-mod",
    "version": "1.0.0",
    "name": "My Mod",
    "description": "Description",
    "mainClass": "com.example.MyMod",
    "authors": ["author"],
    "mcVersionRange": ["1.20.4", "1.21"],
    "aiBridgeEnabled": true,
    "dependencies": {
        "fabric-api": ">=0.90.0"
    },
    "optionalDependencies": {},
    "conflicts": {},
    "loadPriority": 0,
    "entryPointType": "launch",
    "mixinConfigs": ["mixins.my-mod.json"]
}
```

完整字段參考請見 [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md#francium-modjson-完整參考)。

---

## 構建與分發 / Build & Distribution

```bash
# 完整構建（含測試）
./gradlew build

# 獨立編譯核心模組（無 Gradle）
bash build.sh        # Linux/macOS
build.bat            # Windows

# 創建分發 JAR
./gradlew shadowJar

# 創建分發包（含依賴）
bash build_dist.sh   # Linux/macOS
build_dist.bat       # Windows

# 發布到 JitPack
# 推送 tag 到 GitHub 即可自動發布
git tag v1.2.3
git push origin v1.2.3
```

### JitPack 配置

`jitpack.yml` 已預置，推送 tag 後可通過以下方式引用：

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.stanley-1028:francium-loader:v1.2.3'
}
```

---

## 代碼規範 / Code Style

- **Java 21** 優先使用 record、switch expressions、pattern matching、text blocks
- **無外部依賴**：核心模組（francium-core, francium-resolver）不應依賴第三方庫
- **可測試性**：每個 public 方法應可獨立測試
- **文檔**：所有 public API 需有 Javadoc
- **日誌**：使用 `System.out.println` 或 SLF4J（Gradle 構建時）
- **線程安全**：DAG 並行加載需注意線程安全

---

*"不要等待模組更新，讓 AI 替你橋接未來。"*
