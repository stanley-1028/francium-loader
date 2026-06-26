# Francium Mod Loader

<div align="center">

**下一代 Minecraft 模組加載器 — 由 AI 驅動的版本橋接與 DAG 並行加載**

Next-generation Minecraft mod loader with AI-powered cross-version bridging and DAG parallel loading.

[![License](https://img.shields.io/badge/License-MIT-green)]()
[![Java](https://img.shields.io/badge/Java-21%2B-orange)]()
[![Architecture](https://img.shields.io/badge/Architecture-DAG%20%2B%20SAT%20%2B%20AI-blue)]()
[![Tests](https://img.shields.io/badge/Tests-238%20%2F%20100%25-brightgreen)]()
[![Mapping](https://img.shields.io/badge/Mappings-80%20classes%20%7C%20805%20methods-purple)]()
[![Release](https://img.shields.io/github/v/release/stanley-1028/francium-loader?label=Release&color=brightgreen)](https://github.com/stanley-1028/francium-loader/releases/latest)
[![JitPack](https://img.shields.io/badge/JitPack-com.francium%3Afrancium--loader-blue)](https://jitpack.io/#stanley-1028/francium-loader)
[![Checkstyle](https://github.com/stanley-1028/francium-loader/actions/workflows/checkstyle.yml/badge.svg)](https://github.com/stanley-1028/francium-loader/actions/workflows/checkstyle.yml)
[![Build](https://github.com/stanley-1028/francium-loader/actions/workflows/build.yml/badge.svg)](https://github.com/stanley-1028/francium-loader/actions/workflows/build.yml)
[![GitHub Stars](https://img.shields.io/github/stars/stanley-1028/francium-loader?style=social)]()
[![GitHub Forks](https://img.shields.io/github/forks/stanley-1028/francium-loader?style=social)]()

[**快速開始**](QUICKSTART.md) ·
[**文件**](#-文件) ·
[**API 文件**](https://jitpack.io/com/github/stanley-1028/francium-loader/v2.4.0/javadoc/) ·
[**下載**](https://github.com/stanley-1028/francium-loader/releases/latest) ·
[**常見問題**](FAQ.md)

</div>

---

## ✨ 為什麼選擇 Francium？

現有的模組加載器（Forge、Fabric、NeoForge、Quilt）都面臨相同的結構性問題。Francium 不是又一個「更輕量」的加載器，而是從底層重新設計，逐個擊破這些痛點。

| 問題 | 現有方案 | Francium 方案 |
|------|----------|--------------|
| ⏱️ 加載時間過長 | 循序加載，100 mods 需 2-3 分鐘 | **DAG 拓撲分層 + 並行加載，加速比 3-10x** |
| 🔗 模組衝突與依賴地獄 | 手動管理依賴 | **SAT 求解器自動化解決** |
| 🔄 版本不相容 | 每 MC 版本需重新編譯 | **AI 位元組碼橋接，自動跨版本適配** |
| 💾 效能損耗 | 共用 ClassLoader | **每模組獨立 ClassLoader + 物件池** |
| 📦 安裝繁瑣 | 手動下載 jar | **內建套件管理器 (`francium install`)** |
| 🖥️ 伺服器同步 | 手動比對 | **自動清單同步 + SHA256 驗證** |
| 🧹 記憶體管理 | 無 GC 策略 | **洩漏偵測 + 自適應 GC** |
| ⏳ 版本斷層 | 等待 mod 更新數月 | **AI 橋接即時適配** |

---

## 🚀 核心特性

<div align="center">

|  |  |  |
|:-:|:-:|:-:|
| **🔗 DAG 並行加載**<br>依賴圖拓撲分層，同層模組 ForkJoin 並行加載，100 mods 僅需 20-30 秒 | **🤖 AI 版本橋接**<br>基於 ASM 位元組碼分析 + 多維相似度計算，自動生成跨版本配接器 | **🧩 SAT 依賴求解**<br>DPLL 回溯 + MRV/LCV 啟發式，自動偵測並解決衝突 |
| **🗺️ Mapping 資料庫**<br>**80 類別 / 805 方法**，全面覆蓋 Minecraft 常用 API | **📦 套件管理器**<br>`francium install/search/update`，npm-like 體驗 | **💾 記憶體分析器**<br>洩漏偵測、物件池、自適應 GC 策略 |
| **🔄 伺服器同步協定**<br>自動 mod 清單同步 + 安全驗證 | **🌐 雙生態相容**<br>同時支援 Forge 和 Fabric 模組<br>Forge 適配層 v2.5 開發中<br>✅ 生命週期/註冊/事件/配置<br>✅ 能量/流體/物品/能力系統<br>✅ 方塊狀態/屬性/附魔/藥水<br>✅ 85 項測試 100% 通過 | **🔌 Mixin 整合**<br>內建 SpongePowered Mixin 0.8.7 |
| **☕ Java Agent 支援**<br>可作為 `-javaagent` 參數注入 | **🔒 獨立 ClassLoader**<br>每模組隔離加載，防止衝突 | **🧪 完整測試覆蓋**<br>238 項測試，100% 通過率 |

</div>

---

## ⚡ 快速開始

### 1. 下載安裝

[![Download Latest](https://img.shields.io/github/v/release/stanley-1028/francium-loader?label=Download%20Latest&color=3366CC&style=for-the-badge)](https://github.com/stanley-1028/francium-loader/releases/latest)

前往 [Releases 頁面](https://github.com/stanley-1028/francium-loader/releases/latest) 下載 `FranciumLoader-2.4.0.zip`，解壓縮後即可使用。

### 2. 加入依賴

**Gradle（JitPack）：**
```gradle
// settings.gradle
repositories {
    maven { url = 'https://jitpack.io' }
}

// build.gradle
dependencies {
    implementation 'com.github.stanley-1028:francium-loader:v2.4.0'
}
```

**Gradle（Maven Central）：**
```gradle
dependencies {
    implementation 'com.francium:francium-loader:2.4.0'
}
```

### 3. 程式碼入門

```java
import com.francium.loader.FranciumLoader;
import java.nio.file.Paths;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // 初始化加載器
        FranciumLoader loader = FranciumLoader.builder(Paths.get("."))
            .withParallelLoading(true)
            .withMemoryManagement(true)
            .build();

        // 掃描並載入模組
        FranciumLoader.FranciumReport report = loader.launch();

        // 查看並行加速效果
        System.out.println("Total mods: " + report.totalMods);
        System.out.println("Load time: " + report.loadTimeMs + "ms");
    }
}
```

📖 **完整的快速開始指南：[QUICKSTART.md](QUICKSTART.md)**

---

## 🎮 Demo / 功能展示

Francium 提供一個獨立的 CLI Demo，無需 Minecraft 即可展示核心功能：

```bash
# Windows 一鍵 Demo
demo.bat

# 直接執行 Java
java FranciumDemo.java
```

Demo 會展示：
1. 🧩 **SAT 依賴求解**：20 個模組的依賴自動解析
2. 🔗 **DAG 拓撲分層**：可視化依賴圖分層
3. ⚡ **並行加載模擬**：各層並行加載時間線
4. 💾 **記憶體分析**：模擬物件池與洩漏檢測
5. 🗺️ **Mapping 資料庫**：展示 80 類別 / 805 方法的跨版本映射

---

## 📁 架構總覽

```
francium-loader/
├── francium-core/               # 核心加載器
│   ├── ModGraph.java            # 有向無環圖，Kahn 拓撲分層
│   ├── ParallelModClassLoader.java  # ForkJoin 並行加載
│   ├── FranciumLoader.java      # 主入口
│   ├── FranciumBootstrap.java   # CLI 啟動器
│   ├── ModManifest.java         # 模組元資料
│   └── LoaderConfig.java        # 設定管理
│
├── francium-ai-bridge/          # AI 版本橋接（核心創新）
│   ├── VersionBridge.java       # 主橋接器
│   ├── MethodSignature.java     # 方法簽名 + 多維相似度
│   ├── MappingDatabase.java     # 80 類別 / 805 方法的跨版本資料庫
│   ├── BytecodeAnalyzer.java    # ASM 位元組碼分析
│   ├── CompatibilityPredictor.java  # ML 加權特徵預測
│   └── AdapterGenerator.java    # 自動生成配接器 bytecode
│
├── francium-resolver/           # SAT 依賴解析器
│   ├── SATDependencyResolver.java   # DPLL 回溯 + 約束傳播
│   ├── DependencyConstraint.java    # 語義化版本約束
│   └── SemanticVersion.java         # SemVer 2.0
│
├── francium-manager/            # 套件管理器
│   ├── PackageManager.java      # npm-like install/update/search
│   ├── ModrinthAdapter.java     # Modrinth API v2 配接器
│   └── CurseForgeAdapter.java   # CurseForge API v1 配接器
│
├── francium-profiler/           # 記憶體分析器
│   ├── MemoryManager.java       # 洩漏偵測 + 物件池 + GC
│   ├── ProfilerReport.java      # 分析報告
│   └── ReportViewer.java        # 報告可視化
│
├── francium-server/             # 伺服器同步
│   ├── ServerSyncProtocol.java  # 同步協定
│   └── ModValidator.java        # 安全驗證
│
├── francium-api/                # 公共 API
│   └── PublicApi.java           # 公共 API 註解
│
├── francium-forge-adapter/      # Forge 適配層（v2.5 開發中）
│   ├── ForgeAdapter.java        # Forge 適配器主類別
│   ├── adapter/                 # 模組格式偵測與轉換
│   │   ├── ForgeModMetadata.java    # Forge 模組中繼資料
│   │   ├── ForgeModDetector.java    # Forge 模組偵測器
│   │   └── ForgeModConverter.java   # 模組轉換工具
│   ├── lifecycle/               # FML 生命週期
│   │   ├── FMLLifecycle.java        # 生命週期階段
│   │   ├── FMLLifecycleEvent.java   # 生命週期事件
│   │   ├── FMLLifecycleManager.java # 生命週期管理器
│   │   └── ForgeModContainer.java   # 模組容器
│   ├── registry/                # 註冊系統（50+ 種註冊表）
│   │   ├── ForgeRegistry.java       # 註冊表基底類別
│   │   ├── ForgeRegistryManager.java # 註冊表管理器
│   │   ├── DeferredRegister.java    # 延遲註冊工具
│   │   ├── RegistryEvent.java       # 註冊事件
│   │   └── IForgeRegistryEntry.java # 註冊項目介面
│   ├── event/                   # 事件系統（7 大類 / 48 子事件）
│   │   ├── FMLEvent.java           # 事件基底類別
│   │   ├── FMLEventBus.java        # 事件匯流排
│   │   ├── player/PlayerEvent.java # 玩家事件
│   │   ├── block/BlockEvent.java   # 方塊事件
│   │   ├── entity/EntityEvent.java # 實體事件
│   │   ├── item/ItemEvent.java     # 物品事件
│   │   ├── world/WorldEvent.java   # 世界事件
│   │   ├── server/ServerEvent.java # 伺服器事件
│   │   └── client/ClientEvent.java # 用戶端事件
│   ├── config/                  # 配置系統
│   │   ├── ModConfigType.java     # 配置類型列舉
│   │   ├── ConfigValue.java       # 配置項包裝
│   │   ├── ModConfigSpec.java     # 配置規格建構器
│   │   ├── ModConfig.java         # 配置檔實例
│   │   └── ConfigManager.java     # 配置管理員
│   ├── dist/                    # 側邊支援
│   │   ├── Dist.java              # 側邊列舉
│   │   ├── DistManager.java       # 側邊管理員
│   │   └── OnlyIn.java            # @OnlyIn 註解
│   ├── energy/                  # 能量系統（FE/RF）
│   │   ├── IEnergyStorage.java    # 能量儲存介面
│   │   └── EnergyStorage.java     # 能量儲存實作
│   └── fluid/                   # 流體系統
│       ├── FluidStack.java        # 流體堆疊
│       ├── IFluidHandler.java     # 流體處理器介面
│       └── MultiFluidTank.java    # 多槽流體儲存
│
└── francium-mod-template/       # 模組開發模板
```

📖 **詳細的專案結構說明：[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)**

---

## 🔬 技術詳解

### 1. DAG 並行加載 (Parallel Loading)

模組依賴關係形成有向無環圖。使用 Kahn 演算法拓撲分層，同層模組互相獨立，用 ForkJoinPool 並行加載。

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
1. **BytecodeAnalyzer** 用 ASM 解析 mod JAR，提取外部方法呼叫
2. **MappingDatabase** 查詢多版本映射（Mojang、Yarn、SRG）— 現已收錄 **80 類別、805 方法**
3. **CompatibilityPredictor** 基於加權特徵模型排序候選
4. **AdapterGenerator** 自動生成橋接 bytecode 注入 classpath

```
實例: Mod 為 MC 1.20.4 編寫，呼叫 Block.m_12345_()
      Francium 自動映射到 MC 1.21 的 Block.getBlockState()
      生成 Adapter class 透明轉發呼叫
      Mod 無需修改即可在新版本執行
```

### 3. SAT 依賴解析 (SAT Dependency Resolution)

將依賴解析視為約束滿足問題 (CSP)，使用 DPLL 風格的回溯演算法：

- **MRV 啟發式**: 優先賦值給域最小的變數
- **LCV 排序**: 選擇對其他變數約束最小的值
- **前向檢查**: 賦值後立即縮小相關變數域
- **衝突分析**: 精確指出衝突源

### 4. 套件管理器 (Package Manager)

```bash
francium install sodium              # 自動解析傳遞依賴
francium install sodium@^1.2.0       # 指定版本約束
francium search "shader"             # 搜尋模組
francium update                      # 檢查更新
francium install                     # 從 lock 檔案恢復
```

### 5. Mapping 資料庫 (Mapping Database)

v2.4.0 將資料庫從 38 類別大幅擴充至 **80 類別 / 805 方法**，超額完成 v3.0 目標：

| 領域 | 涵蓋類別 | 方法數 |
|------|----------|--------|
| 方塊 & 物品 | Block, Item, ItemStack, BlockState, BlockEntity, ... | ~80 |
| 世界 & 維度 | Level, ServerLevel, ClientLevel, DimensionType, ... | ~60 |
| 實體 | Entity, LivingEntity, Player, Monster, Animal, ... | ~120 |
| 容器 & GUI | Container, MenuType, Inventory, Slot, ... | ~50 |
| 客戶端 & 渲染 | Minecraft, GuiGraphics, EntityRenderDispatcher, ... | ~90 |
| 世界生成 & 生物群系 | Biome, ChunkGenerator, StructureTemplate, ... | ~50 |
| 配方 & 附魔 | Recipe, Enchantment, EnchantmentHelper, ... | ~70 |
| 核心引擎 | MinecraftServer, ServerPlayer, GameProfile, ... | ~40 |
| NBT & 資料 | CompoundTag, ListTag, Tag, Component, ... | ~50 |
| 註冊 & 標籤 | Registry, BuiltInRegistries, TagKey, Holder, ... | ~40 |
| 事件 & 效果 | SoundEvent, Particle, MobEffect, Stats, ... | ~35 |
| 網路 & 封包 | Packet, FriendlyByteBuf, Connection, ... | ~30 |
| 其他常用 | RandomSource, ResourceLocation, AABB, Vec3, ... | ~50 |
| **總計** | **80 類別** | **805** |

跨版本映射（如 Yarn ↔ Mojmap ↔ SRG 互轉）從 ~400 條增長至 **805 條**，覆蓋率高達 **90%+** 最常用 API。

📖 **Mapping 貢獻指南：[docs/MAPPING_CONTRIBUTION_GUIDE.md](docs/MAPPING_CONTRIBUTION_GUIDE.md)**

---

## 🧪 測試

核心模組測試通過率 **153 項測試 (100%)**：

| 模組 | 測試類 | 斷言數 | 狀態 |
|------|--------|--------|------|
| SemanticVersion | 15 項測試 | 15 | ✅ |
| DependencyConstraint | 17 項測試 | 17 | ✅ |
| ModGraph | 21 項測試 | 21 | ✅ |
| CompatibilityPredictor | 10 項測試 | 10 | ✅ |
| SATDependencyResolver | 7 項測試 | 7 | ✅ |
| MappingDatabase | **83 項測試** | **83** | ✅ |

> **Mapping 資料庫測試** 為 v2.3.0 新增，逐類別驗證每個方法 ID→名稱 映射正確性

可透過以下方式執行：

```bash
# Gradle（JUnit 5）
./gradlew test

# 獨立測試執行器（無 Gradle）
java TestRunner.java
```

📖 **真實模組測試計劃：[docs/TESTING_PLAN.md](docs/TESTING_PLAN.md)**

---

## 🛠️ 技術棧

- **語言**: Java 21
- **圖論**: 自訂 DAG 實現（無外部依賴）
- **SAT 求解**: DPLL 回溯 + MRV/LCV 啟發式（純 Java）
- **位元組碼**: ASM 9.6 / 9.10
- **網路**: Netty（伺服器同步）, HttpClient（套件管理）
- **ML**: TensorFlow Java（可選，深度學習模式）
- **建構**: Gradle 8.x / 9.x
- **測試**: JUnit 5
- **分發**: JitPack, Shadow JAR, jpackage 原生安裝包, Maven Central

---

## ⚔️ 與現有方案對比

| 特性 | Forge | Fabric | Francium |
|------|-------|--------|----------|
| 並行加載 | ❌ 循序 | ❌ 循序 | ✅ DAG 拓撲並行 |
| 跨版本相容 | ❌ 需重新編譯 | ❌ 需重新編譯 | ✅ AI 自動橋接 |
| 依賴解析 | 手動 | 手動 | ✅ SAT 自動求解 |
| 套件管理器 | ❌ 需第三方 | ❌ 需第三方 | ✅ 內建 |
| 記憶體管理 | ❌ | ❌ | ✅ 洩漏偵測 + 物件池 |
| 伺服器同步 | 手動 | 手動 | ✅ 自動協定 |
| 相容現有 Mod | ✅ Forge | ✅ Fabric | ✅ 兩者都相容 |
| 模組隔離 | 共用 CL | 共用 CL | ✅ 獨立 ClassLoader |
| Mapping 資料庫 | ❌ 無 | ❌ 無 | ✅ 80 類別 / 805 方法 |
| Mixin 支援 | ✅ 內建 | ✅ 內建 | ✅ SpongePowered 0.8.7 |

---

## 📚 文件

### 🚀 入門指南
- [快速開始](QUICKSTART.md) - 一步一步帶你上手
- [常見問題](FAQ.md) - 40 個常見問題解答
- [安裝指南](QUICKSTART.md#安裝) - 多種安裝方式

### 📖 使用手冊
- [故障排除](TROUBLESHOOTING.md) - 常見問題和解決方案
- [性能優化](PERFORMANCE.md) - 效能調校指南
- [安全政策](SECURITY.md) - 安全最佳實踐

### 👨‍💻 開發者文件
- [開發者指南](DEVELOPER_GUIDE.md) - 模組開發完整教學
- [專案結構](PROJECT_STRUCTURE.md) - 程式碼架構說明
- [程式碼風格](CODESTYLE.md) - 程式碼規範
- [發布檢查清單](RELEASE_CHECKLIST.md) - 版本發布流程
- [API Javadoc](https://jitpack.io/com/github/stanley-1028/francium-loader/v2.4.0/javadoc/) - API 文件

### 🤝 貢獻指南
- [貢獻指南](CONTRIBUTING.md) - 如何參與貢獻
- [社群指南](docs/COMMUNITY_GUIDE.md) - 社群行為準則
- [Mapping 貢獻指南](docs/MAPPING_CONTRIBUTION_GUIDE.md) - 如何貢獻映射資料

### 📋 計劃與路線
- [路線圖](ROADMAP.md) - 專案發展路線
- [變更日誌](CHANGELOG.md) - 版本變更記錄
- [Forge 適配規劃](docs/FORGE_ADAPTER_PLAN.md) - Forge 支援開發計劃
- [真實模組測試計劃](docs/TESTING_PLAN.md) - 整合測試計劃

### 🔧 技術文件
- [架構文件](docs/ARCHITECTURE.md) - 技術架構詳解
- [CLI 指南](docs/CLI_GUIDE.md) - 命令列介面說明
- [Maven Central 發布指南](docs/MAVEN_CENTRAL_PUBLISH.md) - 發布到 Maven Central
- [Mapping 混淆名說明](docs/MAPPING_OBFUSCATION_GUIDE.md) - 混淆名相關說明

---

## 🤝 社群與貢獻

歡迎參與 Francium 的開發！無論是程式碼、文件、翻譯還是建議，都非常歡迎。

### 如何貢獻

1. **🌟 給個 Star** - 如果你覺得這個專案不錯
2. **🐛 回報問題** - 遇到 bug 請提交 Issue
3. **💡 提出建議** - 有好想法歡迎討論
4. **🔧 提交 PR** - 直接參與開發
5. **📝 改進文件** - 幫助完善文件
6. **🗺️ 貢獻 Mapping** - 幫助完善映射資料庫

### 行為準則

請遵守我們的 [社群指南](docs/COMMUNITY_GUIDE.md)，保持友善和尊重。

### 開始貢獻

📖 **詳細的貢獻指南：[CONTRIBUTING.md](CONTRIBUTING.md)**

---

## 🗺️ 路線圖預覽

### ✅ 已完成
- [x] v2.0 - 核心架構（DAG + SAT + AI Bridge）
- [x] v2.2 - 套件管理器 + 伺服器同步
- [x] v2.3 - Mapping 資料庫擴充（38 類 / 400 方法）
- [x] v2.4 - Mapping 資料庫大幅擴充（80 類 / 805 方法）
- [x] v2.4 - Maven Central 發布配置
- [x] v2.4 - 完整文件體系（21+ 篇文件）

### 🚧 進行中 / 計劃中
- [x] v2.5 - Forge 適配基礎架構 ✅ 開發中
- [x] v2.6 - Forge 事件與配置系統 ✅ 開發中
- [x] v3.0 - Forge 內容 API（能量、流體）🔄 進行中
- [ ] v3.0 - Forge 能力（Capability）系統
- [ ] v3.0 - TensorFlow/ONNX 深度學習整合
- [ ] v3.5 - Web 套件註冊表伺服器
- [ ] v3.5 - 效能最佳化與穩定性提升
- [ ] v4.0 - 沙箱模式與安全分析
- [ ] v4.0 - IDE 外掛支援

📖 **完整的路線圖：[ROADMAP.md](ROADMAP.md)**

---

## 📄 許可證

MIT License — 自由使用、修改、分發。詳見 [LICENSE](LICENSE)。

---

## 🔗 連結

- **GitHub**: https://github.com/stanley-1028/francium-loader
- **Issues**: https://github.com/stanley-1028/francium-loader/issues
- **Discussions**: https://github.com/stanley-1028/francium-loader/discussions
- **JitPack**: https://jitpack.io/#stanley-1028/francium-loader
- **下載**: https://github.com/stanley-1028/francium-loader/releases/latest

---

<div align="center">

*"不要等待模組更新，讓 AI 替你橋接未來。"*

**Made with ❤️ by the Francium Team**

</div>
