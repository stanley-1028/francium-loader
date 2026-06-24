# 發布檢查清單 / Release Checklist

本文檔是 Francium Mod Loader 發布新版本前的檢查清單，確保每次發布的品質和一致性。

---

## 📋 目錄

1. [發布前準備](#發布前準備)
2. [程式碼檢查](#程式碼檢查)
3. [建置與測試](#建置與測試)
4. [文件更新](#文件更新)
5. [版本發布步驟](#版本發布步驟)
6. [發布後驗證](#發布後驗證)
7. [熱修復發布](#熱修復發布)

---

## 🎯 發布前準備

### 計劃確認

- [ ] 確認本次發布的目標和範圍
- [ ] 確認版本號（遵循 [SemVer 2.0](https://semver.org/lang/zh-TW/)）
  - 主要版本（Major）：不相容的 API 修改
  - 次要版本（Minor）：向下相容的功能性新增
  - 修訂版本（Patch）：向下相容的問題修正
- [ ] 確認發布日期
- [ ] 確認所有規劃的功能都已完成
- [ ] 確認所有已知的嚴重 Bug 都已修復

### 里程碑檢查

- [ ] 關閉對應的 GitHub Milestone
- [ ] 確認所有指派的 Issue 都已處理
- [ ] 確認所有 Pull Request 都已合併
- [ ] 檢查是否有遺漏的重要變更

---

## 🔍 程式碼檢查

### 程式碼品質

- [ ] 執行 Checkstyle 檢查，確保沒有違規
  ```bash
  ./gradlew checkstyleMain checkstyleTest
  ```
- [ ] 檢查是否有遺留的 TODO/FIXME
  ```bash
  grep -r "TODO" src/ --include="*.java"
  grep -r "FIXME" src/ --include="*.java"
  ```
- [ ] 檢查是否有除錯程式碼（System.out.println 等）
- [ ] 檢查是否有未使用的匯入和變數
- [ ] 確認所有公開 API 都有 Javadoc

### 安全性檢查

- [ ] 檢查是否有已知的安全漏洞
  ```bash
  ./gradlew dependencyCheckAnalyze
  ```
- [ ] 更新有安全漏洞的依賴
- [ ] 檢查設定檔是否包含敏感資訊
- [ ] 確認沒有硬編碼的密碼或金鑰

### 依賴檢查

- [ ] 檢查是否有過期的依賴
  ```bash
  ./gradlew dependencyUpdates
  ```
- [ ] 評估是否需要更新依賴
- [ ] 確認所有依賴的授權都相容
- [ ] 更新 gradle-wrapper 到最新穩定版本（如需要）

---

## 🔨 建置與測試

### 建置測試

- [ ] 清理並重新建置
  ```bash
  ./gradlew clean build
  ```
- [ ] 確認建置成功，沒有錯誤
- [ ] 確認沒有警告（或確認警告是可接受的）
- [ ] 檢查產生的 JAR 檔案大小是否合理

### 單元測試

- [ ] 執行所有單元測試
  ```bash
  ./gradlew test
  ```
- [ ] 確認所有測試都通過
- [ ] 檢查測試覆蓋率
  ```bash
  ./gradlew jacocoTestReport
  ```
- [ ] 確認測試覆蓋率沒有下降太多

### 整合測試

- [ ] 執行整合測試
  ```bash
  ./gradlew integrationTest
  ```
- [ ] 測試真實模組載入
- [ ] 測試跨版本橋接功能
- [ ] 測試伺服器同步功能

### 平台測試

在至少以下平台測試：

- [ ] Windows 10/11
- [ ] macOS 最新版本
- [ ] Linux（Ubuntu 最新 LTS）

每個平台測試項目：
- [ ] 建置成功
- [ ] 單元測試通過
- [ ] Demo 程式正常執行
- [ ] 範例模組正常載入

---

## 📄 文件更新

### 版本文件

- [ ] 更新 `CHANGELOG.md`
  - [ ] 添加新版本標題和日期
  - [ ] 分類列出所有變更（新增、修正、文件等）
  - [ ] 標記重大變更和不相容變更
  - [ ] 參考相關的 Issue 和 PR
- [ ] 更新 `ROADMAP.md`
  - [ ] 標記已完成的項目
  - [ ] 更新執行紀錄
  - [ ] 更新下一步計劃

### 程式碼文件

- [ ] 更新 `README.md`
  - [ ] 更新版本號
  - [ ] 更新功能介紹（如有新功能）
  - [ ] 更新下載連結
  - [ ] 更新文件連結
- [ ] 更新 `DEVELOPER_GUIDE.md`（如有 API 變更）
- [ ] 更新 `QUICKSTART.md`（如有安裝流程變更）
- [ ] 更新其他相關文件

### 版本號更新

- [ ] 更新 `build.gradle` 中的版本號
- [ ] 更新 `gradle.properties` 中的版本號（如有）
- [ ] 更新其他檔案中的版本號參考

---

## 🚀 版本發布步驟

### 準備發布分支

- [ ] 確保 main 分支是最新的
  ```bash
  git checkout main
  git pull origin main
  ```
- [ ] 建立發布分支（選用，適用於大版本）
  ```bash
  git checkout -b release/v2.5.0
  ```
- [ ] 提交版本號更新和文件更新
  ```bash
  git add -A
  git commit -m "chore: prepare release v2.5.0"
  ```

### 建立 Git Tag

- [ ] 建立帶註解的 tag
  ```bash
  git tag -a v2.5.0 -m "Release v2.5.0"
  ```
- [ ] 確認 tag 建立成功
  ```bash
  git tag -l v2.5.0
  ```
- [ ] 推送 tag 到遠端
  ```bash
  git push origin v2.5.0
  ```

### GitHub Release

- [ ] 前往 GitHub Releases 頁面
- [ ] 建立新的 Release
- [ ] 選擇剛建立的 tag
- [ ] 填寫 Release 標題（例如：v2.5.0）
- [ ] 填寫 Release 說明
  - [ ] 重大更新摘要
  - [ ] 完整變更日誌連結
  - 下載連結和安裝說明
- [ ] 上傳發行版檔案
  - [ ] francium-loader.jar
  - [ ] FranciumLoader-2.5.0.zip
  - [ ] 原始碼（自動包含）
- [ ] 如果是預發布版本，勾選「This is a pre-release」
- [ ] 發布！

### Maven Central 發布

- [ ] 確認所有子專案都能正常發布
- [ ] 觸發 Maven Central 發布 workflow
  - 或手動執行：`./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository`
- [ ] 確認發布成功
- [ ] 等待 Maven Central 同步（通常 10-30 分鐘）
- [ ] 驗證可以從 Maven Central 下載

### JitPack 發布

- [ ] JitPack 會自動偵測新 tag
- [ ] 確認 JitPack 建置成功
- [ ] 驗證可以從 JitPack 下載

### 原生安裝包

- [ ] 建置 Windows 安裝包（.exe）
- [ ] 建置 macOS 安裝包（.dmg）
- [ ] 建置 Linux 安裝包（.deb / .rpm）
- [ ] 測試安裝包可以正常安裝
- [ ] 上傳到 GitHub Release

---

## ✅ 發布後驗證

### 基本驗證

- [ ] 確認 GitHub Release 頁面正常顯示
- [ ] 確認可以下載所有發行版檔案
- [ ] 確認檔案大小和校驗和正確
- [ ] 測試下載的檔案可以正常執行

### 分散式平台驗證

- [ ] 確認 JitPack 可以正常解析
- [ ] 確認 Maven Central 可以搜尋到
- [ ] 測試可以從 Maven/Gradle 新增依賴

### 文件驗證

- [ ] 確認 README 中的連結都正常
- [ ] 確認文件網站（如有）已更新
- [ ] 確認 API 文件已更新

### 社群通知

- [ ] 在 Discord 發布公告
- [ ] 在 Reddit 發布更新說明
- [ ] 在其他社群平台發布通知
- [ ] 感謝貢獻者

---

## 🔥 熱修復發布

如果需要緊急修復嚴重 Bug，使用熱修復流程：

### 熱修復檢查清單

- [ ] 確認問題的嚴重性和影響範圍
- [ ] 評估是否需要熱修復
- [ ] 建立熱修復分支
  ```bash
  git checkout v2.5.0
  git checkout -b hotfix/2.5.1
  ```
- [ ] 修復問題
- [ ] 撰寫測試確保問題已修復
- [ ] 執行完整測試
- [ ] 更新 CHANGELOG
- [ ] 更新版本號（修訂版本 +1）
- [ ] 建立 tag 並發布
- [ ] 合併修復到 main 分支

### 熱修復發布後

- [ ] 確認修復有效
- [ ] 通知受影響的使用者
- [ ] 記錄問題和解決方案
- [ ] 評估是否需要長期改進

---

## 📝 發布範本

### CHANGELOG 範本

```markdown
## [2.5.0] — 2026-06-25

### 🚀 重大更新

#### 新功能名稱
- 功能描述
- 更多細節

### ✨ 新功能

- 新功能 1
- 新功能 2

### 🐛 Bug 修復

- 修復問題 1 (#123)
- 修復問題 2 (#456)

### 📄 文件

- 更新文件 1
- 更新文件 2

### 🔧 建置與 CI

- 建置改進
- CI 更新

### ⚠️ 不相容變更

- 變更 1：說明和遷移指南
- 變更 2：說明和遷移指南
```

### GitHub Release 範本

```markdown
## 🎉 Francium v2.5.0 發布！

### 📝 本次更新重點

- ✨ 新功能 A
- 🐛 修復了多個 Bug
- ⚡ 性能優化
- 📄 文件更新

### 📥 下載

| 平台 | 檔案 |
|------|------|
| 通用 | [francium-loader-2.5.0.jar](連結) |
| Windows | [FranciumLoader-2.5.0.exe](連結) |
| macOS | [FranciumLoader-2.5.0.dmg](連結) |
| Linux | [FranciumLoader-2.5.0.deb](連結) |

### 🚀 快速開始

1. 下載對應平台的安裝檔
2. 按照 [QUICKSTART.md](QUICKSTART.md) 進行安裝
3. 享受更快的模組加載體驗！

### 📖 完整變更日誌

詳細變更請參考 [CHANGELOG.md](CHANGELOG.md)。

### ❓ 遇到問題？

- 查看 [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- 提交 [Issue](https://github.com/stanley-1028/francium-loader/issues)
- 在社群中提問

感謝所有貢獻者！💖
```

---

## 📚 相關文件

- 📖 [版本變更日誌](CHANGELOG.md)
- 🗺️ [路線圖](ROADMAP.md)
- 🤝 [貢獻指南](CONTRIBUTING.md)
- 🛡️ [安全政策](SECURITY.md)

---

**每次發布前請仔細檢查這份清單，確保發布品質！✅**

如果有任何遺漏的項目，歡迎更新這份文件。
