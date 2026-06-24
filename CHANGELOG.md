# Changelog

All notable changes to francium-loader will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [2.4.0] — 2026-06-24

### 🚀 重大更新

#### 🗺️ Mapping 數據庫超額完成 v3.0 目標
- 種子映射從 38 類別 / 400 方法 → **80 類別 / 805 方法** (2x 增長)
- 跨版本映射從 400 條 → **805 條** (2x 增長)
- 超額完成 v3.0 目標 (60 類 / 800+ 方法)
- 新增 42 個 Minecraft 常用類，涵蓋：
  - **客戶端 & 渲染**：Minecraft、GuiGraphics、EntityRenderDispatcher、BlockRenderDispatcher、ItemRenderer、RenderType、LocalPlayer、Screen
  - **世界生成 & 生物群系**：Biome、StructureTemplate、ChunkGenerator、NoiseGeneratorSettings、StructureFeature、BiomeSpecialEffects
  - **配方 & 附魔**：Recipe、CraftingRecipe、RecipeManager、Ingredient、Enchantment、EnchantmentHelper
  - **遊戲規則 & 戰利品**：GameRules、LootTable
  - **命令 & 進度**：CommandSourceStack、Advancement、AdvancementProgress
  - **工具 & 崩潰報告**：CrashReport、Util
  - **物品 & 食物**：FoodProperties、Rarity、EquipmentSlot
  - **核心 & 數據**：Holder、BlockEntityType
  - **實體 AI**：GoalSelector、PathNavigation
  - **網絡 & 傷害**：Packet、DamageSource、Container
  - **玩家 & 食物**：Abilities、FoodData
  - **統計 & 遊戲模式**：Stats、GameType
  - **拋射物**：AbstractArrow

#### 📦 Maven Central 發布完整設定
- 所有子項目統一配置發布（francium-api、core、resolver、manager、ai-bridge、profiler、server）
- 新增 `maven-publish` 和 `signing` 插件到所有子項目
- 新增 GitHub Actions workflow：`maven-central-publish.yml`
  - 支援 tag 推送自動發布
  - GPG 簽名自動化
  - 完整的日誌輸出
- 新增完整發布指南：`docs/MAVEN_CENTRAL_PUBLISH.md`
  - 前置條件說明
  - 本地配置步驟
  - GitHub Secrets 配置
  - 常見問題解答
  - 使用示例（Gradle/Maven）

#### 👥 社群文檔與貢獻指南
- 新增 `docs/COMMUNITY_GUIDE.md` 社群指南
  - 官方渠道介紹
  - 社區規範
  - 獲取幫助指南
  - 常見問題解答
  - 分享作品指南
- 新增 `docs/MAPPING_CONTRIBUTION_GUIDE.md` 映射貢獻指南
  - 映射格式詳細說明
  - 四種貢獻方式
  - 完整貢獻工作流
  - 質量標準與優先級
  - 數據來源參考
  - 常見問題解答
- 更新 `CONTRIBUTING.md`
  - 新增 Mapping 貢獻章節
  - 新增 Mapping Database 幫助領域
  - 連結到詳細貢獻指南

### 📄 文件
- 新增 `docs/MAVEN_CENTRAL_PUBLISH.md` - Maven Central 發布指南
- 新增 `docs/COMMUNITY_GUIDE.md` - 社群指南
- 新增 `docs/MAPPING_CONTRIBUTION_GUIDE.md` - Mapping 貢獻指南
- 新增 `QUICKSTART.md` - 快速開始指南（6000+ 字入門文檔）
- 新增 `TROUBLESHOOTING.md` - 故障排除指南（11500+ 字，8 大類別）
- 新增 `PERFORMANCE.md` - 性能優化指南（6300+ 字，6 大類別）
- 新增 `.github/workflows/maven-central-publish.yml` - Maven Central 發布 workflow
- 更新 `README.md` - 全面更新，新增更多文檔鏈接
- 更新 `ROADMAP.md` - 標記已完成項目，更新執行紀錄
- 更新 `CONTRIBUTING.md` - 新增 Mapping 貢獻章節
- 更新 `.gitignore` - 從 52 行擴展到 120+ 行，新增更多忽略項

### 🎮 範例與演示
- 更新 `FranciumDemo.java` - 新增第 5 部分：Mapping 數據庫展示
  - 展示 80 類 / 805 方法的統計數據
  - 13 個領域的覆蓋情況（帶可視化條形圖）
  - 6 個熱門映射示例
  - 更新頁腳，添加 Mapping DB 和 AI Bridge 指標

### 🔧 構建與 CI
- 子項目統一添加 `maven-publish` 和 `signing` 插件
- 子項目統一配置 sourcesJar 和 javadocJar
- 所有子項目 POM 元數據自動生成
- GPG 簽名配置統一管理
- 修復輕量級構建腳本 `build.bat`
  - 新增 `build-stubs/` 目錄，包含 SLF4J 樁實現
  - 新增 `francium-api/PublicApi.java` 到編譯列表
  - 核心代碼可編譯成功

### 🧹 代碼質量
- 修復 `MappingDatabase.java` 中的 import 語句
- 修復 `VersionBridge.java` 中的 import 語句
- 統一使用簡單類名而非完全限定名

## [2.3.0] — 2026-06-18

### 🚀 重大更新

#### 📦 發行版包裝 (Distribution Packaging)
- 建立完整的 `FranciumLoader-2.3.0.zip` 發行版（19.6 MB）
- 包含：`francium-loader.jar`、設定檔、安裝腳本、範例 Mod、MultiMC 匯入 JSON
- 提供 Windows 一鍵安裝 (`install_windows.bat`)
- 支援 MultiMC / Prism Launcher 匯入 (`mmc-pack.json`)

#### 🧩 範例 Mod (Example Mod)
- 新增完整 Gradle 範例模組專案 `build/examples/example-mod/`
- 預編譯 JAR (`example-mod-1.0.0.jar`) 含 `META-INF/francium.json` 描述檔
- 展示 Mixin 設定 (`mixins.example.json`) 與 Framcium Loader 整合
- 提供三種安裝方式：直接放 JAR、Gradle 建置、原始碼修改
- 附完整 `francium.json` 欄位說明文件

#### 🗺️ Mapping 數據庫大擴充 (Mapping Database)
- 種子映射從 20 類別 / ~80 方法 → **38 類別 / 400 方法** (5x 增長)
- 跨版本映射從 ~50 條 → **400 條** (8x 增長)
- 新增領域：ServerLevel/ClientLevel、NBT、Registry、Tags、
  BlockEntity、MenuType、Entity 子類、SoundEvent、Particle、Stats、MobEffect
- 覆蓋 Minecraft 開發最常用的 API 面

### 🐛 Bug Fixes
- **System.exit 移除**: FranciumBootstrap 改用 `throw RuntimeException`
- **異常處理強化**: 多處異常加入 LOGGER 日誌記錄
- **PrintStackTrac 移除**: 全部改為 SLF4J LOGGER

### 📄 文件
- 新增 Checkstyle CI (`.github/workflows/checkstyle.yml`)
- README 全面重寫：更新測試計數 (70→153)、Mapping 統計、發行版說明
- CI 流程更新：checkstyle action 版本升級 v4→v6
- build.gradle 版本號 v2.2.0 → v2.3.0

## [2.1.0] — 2026-05-17

### Changed
- Updated JUnit Jupiter to 6.0.3 (compatible with Gradle 9.x)
- Updated JUnit Platform Launcher to 1.12.3
- Version bumped to 2.1.0

## [2.0.0] — 2026-05-17

### Added
- **Java Agent support** — Francium-loader can now be used as a `-javaagent` JVM argument. The `FranciumAgent.premain()` hooks into the JVM at startup to inject mod JARs into the system classpath and process Mixin configurations before any Minecraft classes are loaded
- **Mixin 0.8.7 integration** — `org.spongepowered:mixin` is shaded into the JAR. Fabric mods with `mixins.json` configs are automatically detected and their mixin classes registered. Compatible with Fabric mod format (`fabric.mod.json`)
- **MixinConfigProcessor** — scans mod JARs for `mixins.json`, `francium.mixins.json`, and `modid.mixins.json`, registers all `mixins/client/server` entries for runtime class transformation
- **Mixin manifest entries**: `Premain-Class`, `Agent-Class`, `Can-Retransform-Classes`, `Can-Set-Native-Method-Prefix`

### Changed
- Version bumped to 2.0.0 (major — breaking new features for Fabric mod compatibility)
- Shadow JAR now includes SpongePowered Mixin 0.8.7
- Added SpongePowered Maven repository for dependency resolution

## [1.6.0] — 2026-05-16

### Fixed
- **Robustness: Invalid/empty JARs no longer crash the loader** — corrupted JAR files are now gracefully skipped with a warning instead of throwing ZipException to the top-level
- **Duplicate mod registration** when JAR contains both `francium-mod.json` and `fabric.mod.json` — now only the first found manifest format is used per JAR
- **JAR path discovery for validator and AI Bridge** — `discoverPhase()` and `bridgePhase()` now use the actual JAR path from discovery instead of guessing `modId-version.jar` which never existed
- **AI Bridge NaN% displayed** when mod has zero external API calls — compatibility score defaults to 100% for mods that don't need bridging
- Shadow JAR now includes all transitive dependencies (SLF4J, Logback, Gson, ASM, Netty) — was only 88 entries, now bundles 5,492+ entries with full runtime classpath
- Converted hand-written `main()` test in ModGraphTest to proper JUnit 5 `@Test` methods (Gradle 9.x fails on empty test suites)
- Linux CI: changed jpackage from `app-image` (directory) to `deb` installer with menu shortcut and package metadata

### Changed
- Version bumped to 1.6.0
- `FranciumBootstrap.VERSION` now reads from JAR manifest (`Implementation-Version`) instead of hardcoded `"1.2.0"`

## [1.5.0] — 2026-05-16

### Changed
- Updated shadow plugin to `com.gradleup.shadow` 9.4.1 (migrated from unmaintained `com.github.johnrengelman.shadow`)
- Updated Gradle wrapper to 9.5.1 (via Dependabot)
- Updated JUnit Jupiter to 5.11.4 (compatible with Gradle 8.x+ embedded launcher)
- Updated SLF4J to 2.0.18 (consistent across all submodules, via Dependabot)
- Updated Logback to 1.5.32 (consistent across all submodules, via Dependabot)
- Updated ASM to 9.10 (bytecode manipulation, via Dependabot)
- Updated Gson to 2.14.0 (JSON parsing, via Dependabot)
- Updated Netty to 4.2.13.Final (server communication, via Dependabot)
- Updated CI workflow actions: checkout@v6, setup-java@v5, setup-gradle@v6, upload-artifact@v7, download-artifact@v8, action-gh-release@v3
- Version bumped to 1.5.0

### Fixed
- Linux jpackage app-image: removed invalid `--linux-package-name` and `--linux-app-category` flags (only valid for --type deb/rpm)
- Version consistency: build.gradle version now matches release tag

## [1.4.0] — 2026-05-15

### Added
- Modern installer experience with desktop shortcut, start menu, per-user install

### Added
- End-to-end integration test (FranciumE2ETest): SAT resolution, DAG scheduling, parallel loading simulation, profiler metrics, AI bridge — 4 tests covering the full pipeline
- Cycle detection test: verifies CircularDependencyException on mod A→B→C→A
- Conflict detection test: verifies SAT returns `success=false` on unsolvable constraints
- Performance gate test: ensures pipeline completes under 5 seconds
- Full README.md and DEVELOPER.md documentation

### Changed
- JUnit 5 migration completed (all 71 tests now use JUnit 5)

## [1.2.3] — 2026-05-15

### Fixed
- Cross-platform builds: jpackage now produces .exe (Windows) and .dmg (macOS)
- CI matrix: build + test across all platforms before packaging
- macOS icon handling: moved nova_icon_256.png to jpackage input dir
