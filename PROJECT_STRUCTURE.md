# 項目結構說明 / Project Structure

本文檔介紹 Francium Mod Loader 的項目結構，幫助你快速了解各個模組和文件的作用。

---

## 📁 頂層目錄結構

```
francium-loader/
├── francium-api/              # 公共 API
├── francium-core/             # 核心加載器
├── francium-ai-bridge/        # AI 版本橋接
├── francium-resolver/         # SAT 依賴解析器
├── francium-manager/          # 套件管理器
├── francium-profiler/         # 記憶體分析器
├── francium-server/           # 伺服器同步
├── francium-mod-template/     # 模組開發模板
├── src/                       # 額外原始碼（Java Agent）
├── test-mods/                 # 測試用 mod JAR
├── examples/                  # 示例專案
├── docs/                      # 技術文檔
├── .github/                   # GitHub 配置
├── docker/                    # Docker 支援
├── gradle/                    # Gradle wrapper
├── jpackage-resources/        # jpackage 資源
├── build-stubs/               # 輕量級構建樁實現
└── 根目錄文件...
```

---

## 🧩 核心模組詳解

### 1. francium-api（公共 API）

**路徑：** `francium-api/`

**作用：** 定義對外公開的 API 介面和註解。

**主要文件：**
- `PublicApi.java` - 公共 API 標註
- `FranciumMod.java` - 模組主介面

**何時使用：** 開發模組時需要依賴此模組。

---

### 2. francium-core（核心加載器）

**路徑：** `francium-core/`

**作用：** 核心加載邏輯，包括 DAG 圖、並行加載、類加載器等。

**主要文件：**

| 文件 | 作用 |
|------|------|
| `FranciumLoader.java` | 主入口，整合所有子系統 |
| `FranciumBootstrap.java` | CLI 啟動器 |
| `ModGraph.java` | 有向無環圖，Kahn 拓撲分層 |
| `ParallelModClassLoader.java` | ForkJoin 並行加載 |
| `ModManifest.java` | 模組元數據 |
| `LoaderConfig.java` | 配置管理 |

**關鍵功能：**
- 模組發現與掃描
- 依賴圖構建
- 拓撲排序與分層
- 並行加載
- 類加載隔離

---

### 3. francium-ai-bridge（AI 版本橋接）

**路徑：** `francium-ai-bridge/`

**作用：** 核心創新功能，實現跨版本自動適配。

**主要文件：**

| 文件 | 作用 |
|------|------|
| `VersionBridge.java` | 主橋接器 |
| `MethodSignature.java` | 方法簽名 + 多維相似度 |
| `MappingDatabase.java` | 映射數據庫（80 類 / 805 方法） |
| `BytecodeAnalyzer.java` | ASM 字節碼分析 |
| `CompatibilityPredictor.java` | ML 加權特徵預測 |
| `AdapterGenerator.java` | 自動生成適配器 bytecode |

**資源文件：**
```
src/main/resources/mappings/
├── seed-mappings-v1_20_4.json    (80 類 / 805 方法)
├── seed-mappings-v1_21.json      (80 類 / 805 方法)
└── cross-version-v1_20_4-to-v1_21.json  (805 條)
```

**工作流程：**
1. 分析模組字節碼，提取外部方法調用
2. 在映射數據庫中查找對應版本的方法
3. 使用相似度算法排序候選
4. 自動生成橋接適配器

---

### 4. francium-resolver（SAT 依賴解析器）

**路徑：** `francium-resolver/`

**作用：** 基於 SAT 算法的依賴版本求解器。

**主要文件：**

| 文件 | 作用 |
|------|------|
| `SATDependencyResolver.java` | DPLL 回溯 + 約束傳播 |
| `DependencyConstraint.java` | 語義化版本約束 |
| `SemanticVersion.java` | SemVer 2.0 實現 |

**關鍵特性：**
- DPLL 回溯算法
- MRV/LCV 啟發式
- 自動檢測並解決衝突
- 語義化版本約束

---

### 5. francium-manager（套件管理器）

**路徑：** `francium-manager/`

**作用：** npm-like 的套件管理器，支援 Modrinth 和 CurseForge。

**主要文件：**

| 文件 | 作用 |
|------|------|
| `PackageManager.java` | 套件管理器主類 |
| `ModrinthAdapter.java` | Modrinth API v2 適配器 |
| `CurseForgeAdapter.java` | CurseForge API v1 適配器 |
| `MixinConfigProcessor.java` | Mixin 配置處理器 |

**支援功能：**
- 搜索模組
- 安裝模組（含傳遞依賴）
- 更新模組
- 版本鎖定
- 多 registry 支援

---

### 6. francium-profiler（記憶體分析器）

**路徑：** `francium-profiler/`

**作用：** 記憶體管理和效能分析。

**主要文件：**

| 文件 | 作用 |
|------|------|
| `MemoryManager.java` | 記憶體管理（洩漏偵測 + 物件池 + GC） |
| `ProfilerReport.java` | 分析報告 |
| `ReportViewer.java` | 報告可視化 |

**功能：**
- 記憶體洩漏偵測
- 物件池管理
- 自適應 GC 策略
- 效能報告生成

---

### 7. francium-server（伺服器同步）

**路徑：** `francium-server/`

**作用：** 伺服器端模組清單同步和安全驗證。

**主要文件：**

| 文件 | 作用 |
|------|------|
| `ServerSyncProtocol.java` | 同步協定 |
| `ModValidator.java` | 安全驗證器 |

**功能：**
- 自動 mod 清單同步
- SHA-256 文件校驗
- ECDSA 數位簽章
- 版本一致性檢查

---

### 8. francium-mod-template（模組開發模板）

**路徑：** `francium-mod-template/`

**作用：** 新模組開發的起始模板。

**使用方式：**
1. 複製此目錄
2. 修改包名和配置
3. 開始開發你的模組

---

## 📁 其他重要目錄

### src/（額外原始碼）

**路徑：** `src/main/java/com/francium/loader/`

**內容：**
- `FranciumAgent.java` - Java Agent 支援

---

### test-mods/（測試用模組）

**路徑：** `test-mods/`

**內容：**
- 各種測試用的 mod JAR 檔案
- 用於整合測試和驗證

---

### examples/（示例專案）

**路徑：** `examples/`

**內容：**
- `example-mod/` - 示例模組
- `ai-bridge-demo/` - AI 橋接演示

---

### docs/（技術文檔）

**路徑：** `docs/`

**主要文檔：**

| 文檔 | 說明 |
|------|------|
| `ARCHITECTURE.md` | 架構文檔 |
| `CLI_GUIDE.md` | CLI 指南 |
| `DEVELOPER_GUIDE.md` | 開發者指南 |
| `DEVELOPER_GUIDE_CN.md` | 中文版開發者指南 |
| `DEVELOPER_GUIDE_EN.md` | 英文版開發者指南 |
| `MAVEN_CENTRAL_PUBLISH.md` | Maven Central 發布指南 |
| `COMMUNITY_GUIDE.md` | 社群指南 |
| `MAPPING_CONTRIBUTION_GUIDE.md` | Mapping 貢獻指南 |
| `loader.toml` | 配置示例 |
| `promo/` | 宣傳文案 |
| `video/` | 視頻腳本 |

---

### .github/（GitHub 配置）

**路徑：** `.github/`

**內容：**
```
.github/
├── workflows/              # GitHub Actions
│   ├── build.yml           # 構建 + 測試
│   ├── checkstyle.yml      # 代碼風格檢查
│   ├── dependabot-auto-merge.yml  # 依賴自動更新
│   └── maven-central-publish.yml  # Maven Central 發布
└── ISSUE_TEMPLATE/         # Issue 模板
```

---

### build-stubs/（輕量級構建樁）

**路徑：** `build-stubs/`

**作用：** 輕量級構建時使用的樁實現，無需外部依賴。

**內容：**
```
build-stubs/
└── org/slf4j/
    ├── Logger.java          # SLF4J Logger 介面樁
    └── LoggerFactory.java   # SLF4J LoggerFactory 樁
```

**使用場景：**
- 沒有 Gradle 時使用 `build.bat` 構建
- 快速驗證核心邏輯
- 離線環境下的構建

---

## 📄 根目錄重要文件

### 構建相關

| 文件 | 作用 |
|------|------|
| `build.gradle` | 根 Gradle 配置 |
| `settings.gradle` | Gradle 設定 |
| `gradle.properties` | Gradle 屬性（本地配置） |
| `gradle.properties.example` | Gradle 屬性範本 |
| `gradlew` / `gradlew.bat` | Gradle wrapper |
| `build.bat` / `build.sh` | 輕量級構建腳本 |
| `build_dist.bat` / `build_dist.sh` | 發行版構建腳本 |

### 演示與測試

| 文件 | 作用 |
|------|------|
| `FranciumDemo.java` | 獨立 Demo 程序 |
| `demo.bat` | Demo 啟動腳本 |
| `TestRunner.java` | 測試運行器 |
| `RealModIntegrationTest.java` | 真實模組整合測試 |

### 文檔

| 文件 | 作用 |
|------|------|
| `README.md` | 項目介紹 |
| `QUICKSTART.md` | 快速開始指南 |
| `TROUBLESHOOTING.md` | 故障排除指南 |
| `PERFORMANCE.md` | 性能優化指南 |
| `SECURITY.md` | 安全政策 |
| `ROADMAP.md` | 路線圖 |
| `CHANGELOG.md` | 變更日誌 |
| `DEVELOPER.md` | 開發者指南 |
| `DEVELOPER_GUIDE.md` | 模組開發指南 |
| `CONTRIBUTING.md` | 貢獻指南 |
| `BUG_REPORT.md` | Bug 報告模板 |

### 其他

| 文件 | 作用 |
|------|------|
| `LICENSE` | MIT 許可證 |
| `.gitignore` | Git 忽略規則 |
| `.editorconfig` | 編輯器配置 |
| `.gitattributes` | Git 屬性 |
| `jitpack.yml` | JitPack 配置 |
| `jreleaser.yml` | JReleaser 配置 |
| `install-francium.bat` | Windows 安裝腳本 |

---

## 🔄 資料流程圖

```
使用者輸入
    ↓
┌─────────────────┐
│  PackageManager │  套件管理
└────────┬────────┘
         ↓
┌─────────────────┐
│  FranciumLoader │  主入口
└────────┬────────┘
         ↓
┌─────────────────┐
│  ModGraph       │  DAG 依賴圖
└────────┬────────┘
         ↓
┌─────────────────┐
│  SAT Resolver   │  依賴求解
└────────┬────────┘
         ↓
┌─────────────────┐
│  AI Bridge      │  版本橋接
└────────┬────────┘
         ↓
┌─────────────────┐
│  ParallelLoader │  並行加載
└────────┬────────┘
         ↓
┌─────────────────┐
│  Minecraft      │  遊戲運行
└─────────────────┘
```

---

## 🎯 從哪裡開始？

### 如果你是使用者

1. 閱讀 [QUICKSTART.md](QUICKSTART.md) - 快速開始
2. 查看 [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - 解決問題
3. 參考 [PERFORMANCE.md](PERFORMANCE.md) - 性能優化

### 如果你是模組開發者

1. 閱讀 [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) - 開發指南
2. 查看 `francium-mod-template/` - 開發模板
3. 參考 `examples/example-mod/` - 示例模組

### 如果你是貢獻者

1. 閱讀 [CONTRIBUTING.md](CONTRIBUTING.md) - 貢獻指南
2. 查看 [ROADMAP.md](ROADMAP.md) - 路線圖
3. 了解項目結構（就是本文檔！）

### 如果你想研究核心技術

1. `francium-core/` - DAG 並行加載
2. `francium-ai-bridge/` - AI 版本橋接
3. `francium-resolver/` - SAT 依賴求解

---

## 📚 更多資源

- 📖 [架構文檔](docs/ARCHITECTURE.md)
- 📖 [開發者指南](DEVELOPER_GUIDE.md)
- 📖 [貢獻指南](CONTRIBUTING.md)
- 🎯 [路線圖](ROADMAP.md)

---

**希望這份文檔能幫助你快速了解 Francium 的項目結構！** 🚀

如果有任何問題，歡迎提交 Issue 或在社群中提問。
