# 🎯 Francium Mod Loader — 完善計劃

> 目標：從「能跑的核心庫」變成「一裝即可用的模組載入器」

---

## 📊 現狀速覽

| 項目 | 狀態 |
|------|------|
| 核心 DAG 並行加載 | ✅ |
| SAT 依賴解析 | ✅ |
| AI 版本橋接 | ✅ |
| 套件管理器 | ✅ |
| 記憶體分析器 | ✅ |
| 伺服器同步 | ✅ |
| 測試覆蓋 | ✅ 70/70 (100%) |
| CI | ✅ |
| 一鍵安裝 | ✅ `install-francium.bat` |
| 一鍵 Demo | ✅ `demo.bat` |
| 文件 | ✅ |
| Maven Central 發布 | ❌ |
| Nova Launcher 整合 | ❌ 代碼橋接未驗證 |

---

## 🔴 P0：一裝即可用

**標準：雙擊一個檔案就能完成安裝，無需終端機**

- [x] Windows 一鍵安裝腳本 (`install-francium.bat`) ✅
- [x] 功能展示 Demo (`demo.bat`) ✅
- [x] 自動 Java 檢測 ✅
- [x] 自動 .minecraft 檢測 ✅
- [x] 自動建置 (無 Gradle 時用 javac) ✅
- [x] 基本設定檔自動生成 ✅

---

## 🟡 P1：開發者體驗

- [ ] Nova Launcher 整合端到端驗證
- [ ] 範例模組 (Hello World Francium Mod)
- [ ] Gradle 模板專案
- [ ] 模組開發者文件
- [ ] API Javadoc

---

## 🟢 P2：生態與分發

- [ ] JitPack 發布 (`com.github.stanley-1028:francium-loader`)
- [ ] Maven Central 發布
- [ ] GitHub Releases
- [ ] 社群文檔 (Discord/Reddit)

---

## 📋 執行紀錄

```
已完成 (2026-05-10):
  ├── install-francium.bat — Windows 一鍵安裝器
  ├── demo.bat — 一鍵功能展示
  ├── 自動 Java/.minecraft 檢測
  └── 自動建置 + 設定檔生成

下一步:
  ├── Nova Launcher 整合驗證
  ├── 開發者文件
  └── JitPack 發布
```
