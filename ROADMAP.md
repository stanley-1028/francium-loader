# 🎯 Francium Mod Loader — 完善計劃

> 目標：從「能跑的核心庫」變成「一裝即可用的模組載入器」

---

## 📊 現狀速覽 (v2.3.0)

| 項目 | 狀態 |
|------|------|
| 核心 DAG 並行加載 | ✅ |
| SAT 依賴解析 | ✅ |
| AI 版本橋接 | ✅ |
| Mapping 數據庫 (38 類 / 400 方法) | ✅ v2.3.0 大幅擴充 |
| 套件管理器 | ✅ |
| 記憶體分析器 | ✅ |
| 伺服器同步 | ✅ |
| 測試覆蓋 | ✅ 153 項 (100%) |
| CI (Build + Checkstyle + Release) | ✅ 全矩陣含 Win/Mac/Linux |
| 一鍵安裝 (`install_windows.bat`) | ✅ |
| 一鍵 Demo (`demo.bat`) | ✅ |
| 發行版套件 (ZIP) | ✅ v2.3.0 新增 |
| 範例模組 (Example Mod) | ✅ v2.3.0 新增 |
| MultiMC/Prism 匯入 | ✅ v2.3.0 新增 |
| 文件 (DEVELOPER_GUIDE.md) | ✅ |
| Maven Central 發布 | ❌ |
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
- [ ] Maven Central 發布
- [ ] 社群文檔 (Discord/Reddit)

---

## 🔵 P3：Mapping 數據庫持續擴充

- [x] 種子映射: 20 類 → 38 類 ✅ v2.3.0
- [x] 方法映射: ~80 → ~400 ✅ v2.3.0
- [x] 跨版本映射: ~50 → ~400 ✅ v2.3.0
- [ ] 目標: 60 類 / 800+ 方法 (v3.0)
- [ ] Community contribution workflow 開放社群貢獻映射

---

## 📋 執行紀錄

```
v2.3.0 (2026-06-18):
  ├── Mapping 數據庫 5x 擴充 (38 類 / 400 方法)
  ├── 發行版 ZIP 套件 (19.6 MB)
  ├── 範例模組 (example-mod-1.0.0.jar)
  ├── MultiMC / Prism Launcher 匯入支援
  ├── 測試擴充至 153 項 (Mapping 83 項)
  ├── CI action 版本升級
  └── README / CHANGELOG 全面更新

下一步:
  ├── Mapping 數據庫繼續擴充至 60 類 / 800+ 方法
  └── Maven Central 發布
```
