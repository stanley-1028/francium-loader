# Francium Mod Loader

**下一代 Minecraft 模組加載器 — 由 AI 驅動的版本橋接與 DAG 並行加載**

Next-generation Minecraft mod loader with AI-powered cross-version bridging and DAG parallel loading.

[![License](https://img.shields.io/badge/License-MIT-green)]()
[![Java](https://img.shields.io/badge/Java-21%2B-orange)]()
[![Architecture](https://img.shields.io/badge/Architecture-DAG%20%2B%20SAT%20%2B%20AI-blue)]()
[![Tests](https://img.shields.io/badge/Tests-70%2F70%20(100%25)-brightgreen)]()
[![JitPack](https://img.shields.io/badge/JitPack-com.francium%3Afrancium--loader-blue)](https://jitpack.io/#stanley-1028/francium-loader)

---

## 為什麼需要 Francium？/ Why Francium?

現有的模組加載器（Forge、Fabric、NeoForge、Quilt）都面臨相同的結構性問題。Francium 不是又一個「更輕量」的加載器，而是從底層重新設計，逐個擊破這些痛點。

| 問題 | 現有方案 | Francium 方案 |
|------|----------|--------------|
| 加載時間過長 | 循序加載，100 mods 需 2-3 分鐘 | DAG 拓撲分層 + 並行加載，加速比 3-10x |
| 模組衝突與依賴地獄 | 手動管理依賴 | SAT 求解器自動化解決 |
| 版本不相容 | 每 MC 版本需重新編譯 | AI 字節碼橋接，自動跨版本適配 |
| 效能損耗 | 共用 ClassLoader | 每模組獨立 ClassLoader + 物件池 |
| 安裝繁瑣 | 手動下載 jar | 內建套件管理器 (`francium install`) |
| 伺服器同步 | 手動比對 | 自動清單同步 + SHA256 驗證 |
| 記憶體管理 | 無 GC 策略 | 洩漏偵測 + 自適應 GC |
| 版本斷層 | 等待 mod 更新數月 | AI 橋接即時適配 |

---

## 核心特性 / Features

- **DAG 並行加載** — 依賴圖拓撲分層，同層模組 ForkJoin 並行加載，100 mods 僅需 20-30 秒
- **AI 版本橋接** — 基於 ASM 字節碼分析 + 多維相似度計算，自動生成跨版本適配器
- **SAT 依賴求解** — DPLL 回溯 + MRV/LCV 啟發式，自動檢測並解決衝突
- **套件管理器** — `francium install/search/update`，npm-like 體驗
- **記憶體分析器** — 洩漏偵測、物件池、自適應 GC 策略
- **伺服器同步協議** — 自動 mod 清單同步 + 安全驗證
- **雙生態相容** — 同時支援 Forge 和 Fabric 模組
- **獨立 ClassLoader** — 每模組隔離加載，防止衝突

---

## 快速引入 / Quick Start

### 加入依賴 / Add Dependency

**Gradle（JitPack，適合目前版本）：**

```gradle
// settings.gradle
repositories {
    maven { url = 'https://jitpack.io' }
}

// build.gradle
dependencies {
    implementation 'com.github.stanley-1028:francium-loader:v2.2.0'
}
```

**Gradle（Maven Central，發布後可用）：**

```gradle
dependencies {
    implementation 'com.francium:francium-loader:2.2.1'
}
```

### 程式碼入門 / Code Example

```java
import com.francium.loader.FranciumLoader;
import com.francium.graph.ModGraph;
import com.francium.resolver.sat.SATDependencyResolver;
import java.util.List;

public class QuickStart {
    public static void main(String[] args) {
        // 1. 初始化加載器
        FranciumLoader loader = FranciumLoader.builder()
            .modsDirectory("./mods")
            .parallel(true)
            .build();

        // 2. 掃描並載入模組
        FranciumLoader.FranciumReport report = loader.bootstrap();

        // 3. 查看並行加速效果
        ModGraph graph = loader.getModGraph();
        System.out.println("Layers: " + graph.getLayerCount());
        System.out.println("Speedup: " + graph.getSpeedupRatio() + "x");

        // 4. 使用 SAT 求解器自行解析依賴
        SATDependencyResolver resolver = new SATDependencyResolver();
        SATDependencyResolver.ResolveResult result =
            resolver.resolve(allMods, allDependencies, timeout);

        if (result.success) {
            System.out.println("Resolved " + result.modCount + " mods");
        }
    }
}
```

完整的 API 文檔請參閱 [Javadoc](https://jitpack.io/com/github/stanley-1028/francium-loader/v2.2.0/javadoc/)。

---

## Demo / 功能展示

Francium 提供一個獨立的 CLI Demo，無需 Minecraft 即可展示核心功能：，無需 Minecraft 即可展示核心功能：

```bash
# Gradle 方式（推薦）
./gradlew :francium-demo:run

# 直接執行 Java
java FranciumDemo.java

# Windows 一鍵 Demo
demo.bat
```

Demo 會展示：
1. SAT 依賴求解：20 個模組的依賴自動解析
2. DAG 拓撲分層：可視化依賴圖分層
3. 並行加載模擬：各層並行加載時間線
4. 記憶體分析：模擬物件池與洩漏檢測

---

## 架構總覽 / Architecture

```
francium-loader/
├── francium-core/           # 核心加載器
│   ├── ModGraph.java        # 有向無環圖，Kahn 拓撲分層
│   ├── ParallelModClassLoader.java  # ForkJoin 並行加載
│   ├── FranciumLoader.java  # 主入口
│   ├── FranciumBootstrap.java      # CLI 啟動器
│   ├── ModManifest.java     # 模組元數據
│   └── LoaderConfig.java    # 配置管理
│
├── francium-ai-bridge/      # AI 版本橋接（核心創新）
│   ├── VersionBridge.java   # 主橋接器
│   ├── MethodSignature.java # 方法簽名 + 多維相似度
│   ├── MappingDatabase.java # 多版本映射數據庫
│   ├── BytecodeAnalyzer.java # ASM 字節碼分析
│   ├── CompatibilityPredictor.java  # ML 加權特徵預測
│   └── AdapterGenerator.java       # 自動生成適配器 bytecode
│
├── francium-resolver/       # SAT 依賴解析器
│   ├── SATDependencyResolver.java  # DPLL 回溯 + 約束傳播
│   ├── DependencyConstraint.java   # 語義化版本約束
│   └── SemanticVersion.java        # SemVer 2.0
│
├── francium-manager/        # 套件管理器
│   └── PackageManager.java  # npm-like install/update/search
│
├── francium-profiler/       # 記憶體分析器
│   ├── MemoryManager.java   # 洩漏偵測 + 物件池 + GC
│   ├── ProfilerReport.java  # 分析報告
│   └── ReportViewer.java    # 報告可視化
│
├── francium-server/         # 伺服器同步
│   ├── ServerSyncProtocol.java  # 同步協議
│   └── ModValidator.java        # 安全驗證
│
├── francium-mod-template/   # 模組開發模板
│
├── FranciumDemo.java        # 獨立 Demo 入口
└── TestRunner.java          # 測試運行器
```

---

## 技術詳解 / Technical Deep Dive

### 1. DAG 並行加載 (Parallel Loading)

模組依賴關係形成有向無環圖。使用 Kahn 算法拓撲分層，同層模組互相獨立，用 ForkJoinPool 並行加載。

```
依賴圖:         拓撲分層:
A → B → D      Layer 0: {C, F}     (無依賴，可並行)
A → C          Layer 1: {B, E}     (依賴 Layer 0)
B → E          Layer 2: {A, D}     (依賴 Layer 1)
F → E

循序: 6 步 → 並行: 3 層 → 2x 加速
100 mods 實測: 3-10x 加速
```

### 2. AI 版本橋接 (AI Version Bridge)

Francium 最具革命性的功能。Minecraft 每次改版後 method/field 名稱會混淆變化，但語義結構一致。

**工作流程:**
1. **BytecodeAnalyzer** 用 ASM 解析 mod JAR，提取外部方法調用
2. **MappingDatabase** 查詢多版本映射（Mojang、Yarn、SRG）
3. **CompatibilityPredictor** 基於加權特徵模型排序候選
4. **AdapterGenerator** 自動生成橋接 bytecode 注入 classpath

```
實例: Mod 為 MC 1.20.4 編寫，調用 Block.m_12345_()
      Francium 自動映射到 MC 1.21 的 Block.getBlockState()
      生成 Adapter class 透明轉發調用
      Mod 無需修改即可在新版本運行
```

### 3. SAT 依賴解析 (SAT Dependency Resolution)

將依賴解析視為約束滿足問題 (CSP)，使用 DPLL 風格的回溯算法：

- **MRV 啟發式**: 優先賦值給域最小的變數
- **LCV 排序**: 選擇對其他變數約束最小的值
- **前向檢查**: 賦值後立即縮小相關變數域
- **衝突分析**: 精確指出衝突源

### 4. 套件管理器 (Package Manager)

```bash
francium install sodium              # 自動解析傳遞依賴
francium install sodium@^1.2.0       # 指定版本約束
francium search "shader"             # 搜索模組
francium update                      # 檢查更新
francium install                     # 從 lock 文件恢復
```

---

## 快速安裝 / Quick Install 🚀

### 一鍵安裝 (Windows)

[![Download Latest](https://img.shields.io/github/v/release/stanley-1028/francium-loader?label=Download%20Francium%20Loader&color=3366CC&style=for-the-badge)](https://github.com/stanley-1028/francium-loader/releases/latest)

1. 前往 [Releases 頁面](https://github.com/stanley-1028/francium-loader/releases/latest)
2. 下載 `FranciumLoader-x.x.x.exe`
3. **雙擊安裝** — 自動加入 PATH 環境變數，打開終端即可用 `francium` 命令
4. 安裝完成後在命令提示字元或 PowerShell 中輸入 `francium --help`

### 便攜版

下載 `FranciumLoader-x.x.x-portable.zip`，解壓後使用 `bin/francium` 命令。

### macOS / Linux

下載對應 DMG / AppImage，或直接使用 shadow JAR。

<details>
<summary>從源碼構建 / Build from Source</summary>

```bash
git clone https://github.com/stanley-1028/francium-loader.git
cd francium-loader
./gradlew build
```

</details>

### 前置需求 / Prerequisites
- Java 21+
- Minecraft 1.20.4+（理論支援任意版本）

### 運行 Demo / Run Demo

```bash
# 使用 Gradle
./gradlew :francium-demo:run

# 直接執行
java FranciumDemo.java

# Windows
demo.bat
```

### 安裝 Francium / Install

```bash
# 使用 Installer
java -jar francium-installer.jar --install ~/.minecraft

# 或手動添加到啟動參數
-javaagent:francium-loader.jar
```

### 創建模組 / Create a Mod

在 mod JAR 中創建 `francium-mod.json`：

```json
{
    "modId": "my-awesome-mod",
    "version": "1.0.0",
    "name": "My Awesome Mod",
    "mainClass": "com.example.MyMod",
    "mcVersionRange": ["1.20.4", "1.21"],
    "aiBridgeEnabled": true,
    "dependencies": {
        "fabric-api": ">=0.90.0"
    }
}
```

詳見 [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) 和 [francium-mod-template/](francium-mod-template/)。

---

## 測試 / Tests

核心模組測試通過率 **70/70 (100%)**：

| 模組 | 測試類 | 斷言數 | 狀態 |
|------|--------|--------|------|
| SemanticVersion | 15 項測試 | 15 | ✅ |
| DependencyConstraint | 17 項測試 | 17 | ✅ |
| ModGraph | 21 項測試 | 21 | ✅ |
| CompatibilityPredictor | 10 項測試 | 10 | ✅ |
| SATDependencyResolver | 7 項測試 | 7 | ✅ |

測試使用手寫 `main()` 方法格式（見 [TestRunner.java](TestRunner.java)），可通過以下方式運行：

```bash
# Gradle（JUnit 5）
./gradlew test

# 獨立測試運行器（無 Gradle）
java TestRunner.java
```

核心測試不依賴任何外部庫，可通過 `build.sh` / `build.bat` 獨立編譯運行。

---

## 技術棧 / Tech Stack

- **語言**: Java 21
- **圖論**: 自訂 DAG 實現（無外部依賴）
- **SAT 求解**: DPLL 回溯 + MRV/LCV 啟發式（純 Java）
- **字節碼**: ASM 9.6（francium-ai-bridge）
- **網路**: Netty（伺服器同步）, HttpClient（套件管理）
- **ML**: TensorFlow Java（可選，深度學習模式）
- **構建**: Gradle 8.x
- **測試**: JUnit 5（Gradle 構建）/ 純 Java `main()`（獨立構建）
- **分發**: JitPack, Shadow JAR

---

## 與現有方案對比 / Comparison

| 特性 | Forge | Fabric | Francium |
|------|-------|--------|----------|
| 並行加載 | ❌ 循序 | ❌ 循序 | ✅ DAG 拓撲並行 |
| 跨版本相容 | ❌ 需重新編譯 | ❌ 需重新編譯 | ✅ AI 自動橋接 |
| 依賴解析 | 手動 | 手動 | ✅ SAT 自動求解 |
| 套件管理器 | ❌ 需第三方 | ❌ 需第三方 | ✅ 內建 |
| 記憶體管理 | ❌ | ❌ | ✅ 洩漏偵測 + 物件池 |
| 伺服器同步 | 手動 | 手動 | ✅ 自動協議 |
| 相容現有 Mod | ✅ Forge | ✅ Fabric | ✅ 兩者都相容 |
| 模組隔離 | 共用 CL | 共用 CL | ✅ 獨立 ClassLoader |

---

## 許可證 / License

MIT License — 自由使用、修改、分發。詳見 [LICENSE](LICENSE)。

---

## 鏈接 / Links

- GitHub: https://github.com/stanley-1028/francium-loader
- Issues: https://github.com/stanley-1028/francium-loader/issues
- Changelog: CHANGELOG.md
- 路線圖: ROADMAP.md
- 開發者指南: DEVELOPER_GUIDE.md
- JitPack: https://jitpack.io/#stanley-1028/francium-loader

---

*"不要等待模組更新，讓 AI 替你橋接未來。"*
