# 🎯 Francium Mod Loader — 完善計劃

> 目標：從「能跑的核心庫」變成「一裝即可用的模組載入器」

---

## 📊 現狀速覽 (v2.4.0)

| 項目 | 狀態 |
|------|------|
| 核心 DAG 並行加載 | ✅ |
| SAT 依賴解析 | ✅ |
| AI 版本橋接 | ✅ |
| Mapping 數據庫 (80 類 / 805 方法) | ✅ v2.4.0 大幅擴充 |
| 套件管理器 | ✅ |
| 記憶體分析器 | ✅ |
| 伺服器同步 | ✅ |
| 測試覆蓋 | ✅ 153 項 (100%) |
| CI (Build + Checkstyle + Release) | ✅ 全矩陣含 Win/Mac/Linux |
| 一鍵安裝 (`install_windows.bat`) | ✅ |
| 一鍵 Demo (`demo.bat`) | ✅ v2.4.0 更新 |
| 發行版套件 (ZIP) | ✅ v2.3.0 新增 |
| 範例模組 (Example Mod) | ✅ v2.3.0 新增 |
| MultiMC/Prism 匯入 | ✅ v2.3.0 新增 |
| 文件 (DEVELOPER_GUIDE.md) | ✅ |
| 快速開始指南 (QUICKSTART.md) | ✅ v2.4.0 新增 |
| 故障排除指南 (TROUBLESHOOTING.md) | ✅ v2.4.0 新增 |
| 性能優化指南 (PERFORMANCE.md) | ✅ v2.4.0 新增 |
| Maven Central 發布 | ✅ 設定檔 + Workflow + 文件 |
| 社群文檔 | ✅ COMMUNITY_GUIDE.md |
| Mapping 貢獻指南 | ✅ MAPPING_CONTRIBUTION_GUIDE.md |
| Nova Launcher 整合 | ✅ |

---

## 🔴 P0：一裝即可用

**標準：雙擊一個檔案就能完成安裝，無需終端機**

- [x] Windows 一鍵安裝腳本 (`install_windows.bat`) ✅
- [x] 發行版 ZIP 套件 (含 JAR、設定、範例) ✅
- [x] MultiMC / Prism Launcher 匯入支援 (`mmc-pack.json`) ✅
- [x] 功能展示 Demo (`demo.bat`) ✅
- [x] 自動 Java 檢測 ✅
- [x] 自動 .minecraft 檢測 ✅
- [x] 自動建置 (無 Gradle 時用 javac) ✅
- [x] 基本設定檔自動生成 ✅

---

## 🟡 P1：開發者體驗

- [x] Nova Launcher 整合端到端驗證 ✅
- [x] 範例模組 (Hello World Francium Mod) ✅ v2.3.0
- [x] 預編譯 example-mod JAR ✅ v2.3.0
- [x] Gradle 模板專案 (francium-mod-template) ✅
- [x] 模組開發者文件 (DEVELOPER_GUIDE.md) ✅
- [x] `francium.json` 完整欄位說明 ✅
- [x] API Javadoc 上線 ✅

---

## 🟢 P2：生態與分發

- [x] JitPack 發布 (`com.github.stanley-1028:francium-loader`)
- [x] GitHub Releases (v2.3.0)
- [x] 原生安裝包 (exe / dmg / deb)
- [x] Maven Central 發布 ✅ v2.4.0 設定完成
  - 子項目發布設定
  - GitHub Actions workflow
  - 完整發布指南文件
- [x] 社群文檔 (Discord/Reddit) ✅ v2.4.0
  - COMMUNITY_GUIDE.md 社群指南
  - REDDIT_POST.md 發布文案
  - MCBBS_POST.md 中文社區文案

---

## 🔵 P3：Mapping 數據庫持續擴充

- [x] 種子映射: 20 類 → 38 類 ✅ v2.3.0
- [x] 方法映射: ~80 → ~400 ✅ v2.3.0
- [x] 跨版本映射: ~50 → ~400 ✅ v2.3.0
- [x] 目標: 60 類 / 800+ 方法 (v3.0) ✅ v2.4.0 超額完成 (80 類 / 805 方法)
  - 新增 42 個 Minecraft 常用類
  - 涵蓋客戶端渲染、世界生成、配方附魔、命令進度等領域
  - 跨版本映射同步擴充至 805 條
- [x] Community contribution workflow 開放社群貢獻映射 ✅ v2.4.0
  - MAPPING_CONTRIBUTION_GUIDE.md 完整貢獻指南
  - CONTRIBUTING.md 新增 Mapping 貢獻章節
  - 品質標準與優先級說明

---

## 📋 執行紀錄

```
v2.4.0 (2026-06-24):
  ├── Mapping 數據庫 2x 擴充 (80 類 / 805 方法)
  │   ├── 新增 42 個 Minecraft 常用類
  │   ├── 涵蓋客戶端渲染、世界生成、配方附魔等領域
  │   └── 跨版本映射同步擴充至 805 條
  ├── Maven Central 發布完整設定
  │   ├── 所有子項目發布配置
  │   ├── GitHub Actions 自動發布 workflow
  │   └── MAVEN_CENTRAL_PUBLISH.md 完整指南
  ├── 社群文檔與貢獻指南
  │   ├── COMMUNITY_GUIDE.md 社群指南
  │   ├── MAPPING_CONTRIBUTION_GUIDE.md 映射貢獻指南
  │   └── CONTRIBUTING.md 更新
  ├── 用戶體驗優化
  │   ├── QUICKSTART.md 快速開始指南（6000+ 字）
  │   ├── TROUBLESHOOTING.md 故障排除指南（11500+ 字）
  │   ├── PERFORMANCE.md 性能優化指南（6300+ 字）
  │   ├── FranciumDemo.java 更新（新增 Mapping 展示）
  │   ├── README.md 全面更新
  │   └── .gitignore 完善（52 行 → 120+ 行）
  ├── 構建與代碼質量
  │   ├── 輕量級構建腳本修復（build.bat）
  │   ├── 新增 build-stubs/ 目錄（SLF4J 樁實現）
  │   └── 代碼 import 語句統一優化
  └── 超額完成 v3.0 Mapping 目標 (60類/800方法 → 80類/805方法)

v2.3.0 (2026-06-18):
  ├── Mapping 數據庫 5x 擴充 (38 類 / 400 方法)
  ├── 發行版 ZIP 套件 (19.6 MB)
  ├── 範例模組 (example-mod-1.0.0.jar)
  ├── MultiMC / Prism Launcher 匯入支援
  ├── 測試擴充至 153 項 (Mapping 83 項)
  ├── CI action 版本升級
  └── README / CHANGELOG 全面更新

下一步:
  ├── 真實模組整合測試與優化
  ├── Forge/NeoForge 適配層完善
  └── 官方網站與文件站
