# Francium 故障排除指南 / Troubleshooting Guide

本指南將幫助你解決使用 Francium Mod Loader 時可能遇到的常見問題。

---

## 📋 目錄 / Table of Contents

1. [安裝問題](#安裝問題)
2. [運行問題](#運行問題)
3. [模組加載問題](#模組加載問題)
4. [AI Bridge 問題](#ai-bridge-問題)
5. [性能問題](#性能問題)
6. [構建問題](#構建問題)
7. [開發問題](#開發問題)
8. [獲取幫助](#獲取幫助)

---

## 🔧 安裝問題

### 問題 1：Java 版本不正確

**症狀：**
- 啟動時出現 `UnsupportedClassVersionError`
- 提示 `Java version is too old`

**可能原因：**
- 系統安裝的 Java 版本低於 21
- 有多個 Java 版本，預設使用了舊版本

**解決方案：**

1. **檢查 Java 版本**
   ```bash
   java -version
   ```
   確保版本是 21 或更高。

2. **下載並安裝 Java 21+**
   - 推薦：[Eclipse Temurin](https://adoptium.net/)
   - 或：[Oracle JDK](https://www.oracle.com/java/technologies/downloads/)

3. **設置 JAVA_HOME 環境變數**
   - Windows：
     ```bash
     setx JAVA_HOME "C:\Program Files\Java\jdk-21"
     setx PATH "%JAVA_HOME%\bin;%PATH%"
     ```
   - Linux/Mac：
     ```bash
     export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
     export PATH=$JAVA_HOME/bin:$PATH
     ```

4. **重新啟動命令列 / 終端機**

---

### 問題 2：找不到 .minecraft 目錄

**症狀：**
- 啟動時提示 `Cannot find .minecraft directory`
- 模組目錄不存在

**可能原因：**
- Minecraft 安裝在非預設位置
- 從未啟動過 Minecraft
- 使用了 MultiMC / Prism Launcher 等第三方啟動器

**解決方案：**

1. **手動指定 .minecraft 路徑**
   ```bash
   francium --minecraft-dir "C:\Users\YourName\AppData\Roaming\.minecraft"
   ```

2. **在配置文件中設置**
   編輯 `config/francium.toml`：
   ```toml
   [general]
   minecraftDir = "C:/Users/YourName/AppData/Roaming/.minecraft"
   ```

3. **MultiMC / Prism Launcher 用戶**
   - 使用「從 ZIP 匯入」功能
   - 或手動指定實例目錄

---

### 問題 3：安裝腳本權限不足（Windows）

**症狀：**
- 執行 `install_windows.bat` 時出現「存取被拒」
- 無法寫入 PATH 環境變數

**可能原因：**
- 沒有系統管理員權限
- 防毒軟體封鎖了腳本

**解決方案：**

1. **以系統管理員身分執行**
   - 右鍵點擊 `install_windows.bat`
   - 選擇「以系統管理員身分執行」

2. **手動添加到 PATH**
   - 按 `Win + X`，選擇「系統」
   - 點擊「進階系統設定」
   - 點擊「環境變數」
   - 在「系統變數」中找到 `Path`，點擊「編輯」
   - 點擊「新增」，輸入 Francium 的安裝路徑
   - 點擊「確定」儲存

3. **暫時停用防毒軟體**
   - 部分防毒軟體可能會封鎖批次檔
   - 安裝完成後再重新啟用

---

## 🚀 運行問題

### 問題 1：啟動後閃退

**症狀：**
- 執行 `francium` 後視窗閃一下就關閉
- 沒有任何錯誤訊息

**可能原因：**
- Java 版本不正確
- 配置文件損壞
- 模組衝突導致崩潰

**解決方案：**

1. **查看日誌文件**
   日誌文件位於 `logs/francium.log`

2. **在命令列中運行**
   打開命令提示字元或 PowerShell，手動執行：
   ```bash
   java -jar francium-loader.jar
   ```
   這樣可以看到錯誤訊息。

3. **重置配置**
   刪除 `config/` 目錄，重新生成預設配置。

4. **安全模式啟動**
   ```bash
   francium --safe-mode
   ```
   這會跳過所有模組，只加載核心功能。

---

### 問題 2：記憶體不足（OutOfMemoryError）

**症狀：**
- 出現 `java.lang.OutOfMemoryError: Java heap space`
- 加載大量模組時崩潰
- 遊戲運行緩慢

**可能原因：**
- 預設記憶體配置不足
- 加載了過多的模組
- 有記憶體洩漏

**解決方案：**

1. **增加 JVM 記憶體**
   ```bash
   java -Xmx4G -Xms2G -jar francium-loader.jar
   ```
   - `-Xmx4G`：最大記憶體 4GB
   - `-Xms2G`：初始記憶體 2GB

2. **在配置文件中設置**
   編輯啟動腳本或配置：
   ```bash
   JAVA_OPTS="-Xmx6G -Xms2G"
   ```

3. **減少模組數量**
   - 移除不需要的模組
   - 使用 `francium list` 查看已安裝的模組
   - 使用 `francium uninstall <mod-id>` 卸載不需要的模組

4. **啟用記憶體管理**
   確保配置中啟用了記憶體管理：
   ```toml
   [general]
   memoryManagement = true
   ```

---

### 問題 3：配置文件錯誤

**症狀：**
- 啟動時出現 `Config parse error`
- 配置選項不生效

**可能原因：**
- 手動編輯配置時語法錯誤
- 使用了錯誤的配置格式
- 配置文件損壞

**解決方案：**

1. **檢查 TOML 語法**
   - TOML 對縮排和引號很敏感
   - 使用 [TOML 驗證工具](https://toml.io/) 檢查

2. **重置配置文件**
   刪除 `config/francium.toml`，重新生成預設配置。

3. **使用正確的配置格式**
   ✅ 正確：
   ```toml
   [general]
   parallelLoading = true
   parallelThreads = 0
   ```

   ❌ 錯誤：
   ```toml
   [general]
   parallelLoading: true  # 應該用 = 而不是 :
   parallelThreads = "0"  # 數字不需要引號
   ```

---

## 📦 模組加載問題

### 問題 1：模組無法加載

**症狀：**
- 模組出現在清單中但未啟用
- 日誌中出現 `Failed to load mod`
- 模組功能未生效

**可能原因：**
- 模組版本不相容
- 缺少依賴模組
- 模組文件損壞
- 模組格式不支援

**解決方案：**

1. **檢查日誌**
   查看 `logs/francium.log` 中的錯誤訊息。

2. **驗證模組完整性**
   ```bash
   francium verify <mod-id>
   ```

3. **檢查依賴**
   ```bash
   francium deps <mod-id>
   ```
   確保所有依賴都已安裝。

4. **重新下載模組**
   - 模組文件可能損壞
   - 從官方來源重新下載
   - 使用 `francium install --force <mod-id>` 重新安裝

5. **檢查 Minecraft 版本**
   確保模組支援你的 Minecraft 版本。

---

### 問題 2：依賴衝突

**症狀：**
- 出現 `Dependency conflict` 錯誤
- SAT 求解器無法找到可行解
- 模組之間版本不相容

**可能原因：**
- 多個模組需要同一依賴的不同版本
- 版本約束過於嚴格
- 循環依賴

**解決方案：**

1. **查看衝突詳情**
   ```bash
   francium resolve --show-conflicts
   ```

2. **放寬版本約束**
   編輯 `francium.json` 或使用 `--relax` 選項：
   ```bash
   francium install --relax <mod-id>
   ```

3. **手動指定版本**
   ```bash
   francium install <mod-id>@<version>
   ```

4. **移除衝突的模組**
   如果兩個模組功能重疊，考慮只保留一個。

5. **使用最新版本**
   ```bash
   francium update
   ```
   更新所有模組到最新版本，可能會解決衝突。

---

### 問題 3：模組加載順序錯誤

**症狀：**
- 模組初始化順序不正確
- 出現 `ClassNotFoundException`
- 依賴的模組還沒初始化就被呼叫

**可能原因：**
- 模組的依賴關係聲明不正確
- DAG 分層算法有問題
- 模組使用了非標準的加載方式

**解決方案：**

1. **檢查依賴聲明**
   確保模組的 `francium.json` 中正確聲明了所有依賴：
   ```json
   {
     "dependencies": {
       "fabric-api": ">=0.90.0",
       "another-mod": ">=1.0.0"
     }
   }
   ```

2. **查看加載順序**
   ```bash
   francium graph --show-layers
   ```

3. **手動調整順序**
   在配置中指定加載優先級：
   ```toml
   [loading]
   loadOrder = ["mod-a", "mod-b", "mod-c"]
   ```

4. **回報問題**
   如果確定是 DAG 算法的問題，請在 GitHub 上提交 Issue。

---

## 🤖 AI Bridge 問題

### 問題 1：AI Bridge 無法匹配方法

**症狀：**
- 出現 `Method not found` 錯誤
- 跨版本模組無法正常運行
- 日誌中出現 `Low confidence match` 警告

**可能原因：**
- Mapping 數據庫中沒有對應的映射
- 方法特徵變化太大
- 相似度閾值設置過高

**解決方案：**

1. **檢查 Mapping 覆蓋率**
   ```bash
   francium mapping --coverage
   ```

2. **降低相似度閾值**
   編輯配置：
   ```toml
   [ai-bridge]
   minConfidence = 0.6  # 預設 0.7，降低可匹配更多但可能不準確
   ```

3. **手動添加映射**
   參考 [Mapping 貢獻指南](docs/MAPPING_CONTRIBUTION_GUIDE.md) 手動添加映射。

4. **回報問題**
   如果是常用方法但缺失，請在 GitHub 上提交 Issue 或 PR。

---

### 問題 2：AI Bridge 匹配錯誤

**症狀：**
- 模組行為異常
- 出現奇怪的錯誤
- 方法呼叫了錯誤的目標

**可能原因：**
- 相似度計算不準確
- 有多個相似的方法
- 方法語義發生了變化

**解決方案：**

1. **提高相似度閾值**
   ```toml
   [ai-bridge]
   minConfidence = 0.85  # 提高閾值，只匹配高置信度的
   ```

2. **禁用 AI Bridge**
   如果某個模組問題嚴重，可以針對該模組禁用：
   ```json
   {
     "modId": "problem-mod",
     "aiBridgeEnabled": false
   }
   ```

3. **手動修正映射**
   參考 Mapping 貢獻指南手動修正錯誤的映射。

4. **回報問題**
   請在 GitHub 上提交 Issue，包含：
   - 模組名稱和版本
   - Minecraft 版本
   - 錯誤的方法匹配
   - 正確的應該是什麼

---

### 問題 3：適配器生成失敗

**症狀：**
- 出現 `Adapter generation failed` 錯誤
- 字節碼生成異常
- 類別定義錯誤

**可能原因：**
- ASM 版本不相容
- 方法特徵過於複雜
- 記憶體不足

**解決方案：**

1. **增加記憶體**
   參考 [記憶體不足問題](#問題-2記憶體不足outofmemoryerror)

2. **禁用即時生成**
   使用預生成的適配器：
   ```toml
   [ai-bridge]
   preGenerateAdapters = true
   ```

3. **回報問題**
   請在 GitHub 上提交 Issue，包含完整的錯誤堆疊。

---

## ⚡ 性能問題

### 問題 1：加載速度慢

**症狀：**
- 模組加載時間很長
- 沒有體驗到並行加速
- 感覺比 Forge/Fabric 還慢

**可能原因：**
- 並行加載未啟用
- CPU 核心數不足
- 硬碟速度太慢
- 模組數量太多

**解決方案：**

1. **確保並行加載已啟用**
   ```toml
   [general]
   parallelLoading = true
   ```

2. **調整執行緒數**
   ```toml
   [general]
   parallelThreads = 0  # 0 = 自動偵測，可手動設置
   ```
   建議設為 CPU 核心數的 1-2 倍。

3. **使用 SSD**
   - 機械硬碟會大幅減慢加載速度
   - 建議將 Minecraft 和模組放在 SSD 上

4. **減少模組數量**
   - 移除不需要的模組
   - 使用 `francium list` 查看已安裝的模組

5. **查看加載時間分析**
   ```bash
   francium profile --load-time
   ```
   找出哪些模組加載最慢。

---

### 問題 2：記憶體使用過高

**症狀：**
- 記憶體占用持續上升
- 遊戲運行越來越慢
- 出現記憶體不足錯誤

**可能原因：**
- 記憶體洩漏
- 模組品質不佳
- 物件池配置不合理

**解決方案：**

1. **啟用記憶體管理**
   ```toml
   [general]
   memoryManagement = true
   ```

2. **調整物件池大小**
   ```toml
   [memory]
   objectPoolSize = 1000
   ```

3. **執行記憶體分析**
   ```bash
   francium profiler --memory
   ```
   找出記憶體使用最多的地方。

4. **檢查洩漏**
   ```bash
   francium profiler --leak-detect
   ```

5. **更新模組**
   部分模組可能有記憶體洩漏問題，更新到最新版本可能會修復。

---

### 問題 3：CPU 占用過高

**症狀：**
- 閒置時 CPU 占用也很高
- 風扇轉速很快
- 電腦變得很卡

**可能原因：**
- 後台執行緒過多
- 過度並行
- 某些模組有問題

**解決方案：**

1. **減少並行執行緒**
   ```toml
   [general]
   parallelThreads = 4  # 根據 CPU 核心數調整
   ```

2. **禁用不必要的功能**
   ```toml
   [profiler]
   enabled = false  # 如果不需要效能分析
   ```

3. **找出耗 CPU 的模組**
   ```bash
   francium profiler --cpu
   ```

4. **更新或移除問題模組**

---

## 🔨 構建問題

### 問題 1：Gradle 構建失敗

**症狀：**
- 執行 `./gradlew build` 失敗
- 出現依賴下載錯誤
- 編譯錯誤

**可能原因：**
- 網路問題導致依賴下載失敗
- Gradle 版本不相容
- Java 版本不正確

**解決方案：**

1. **檢查 Java 版本**
   確保使用 Java 21+。

2. **清理並重新構建**
   ```bash
   ./gradlew clean
   ./gradlew build --refresh-dependencies
   ```

3. **使用國內鏡像**
   如果在中國大陸，建議使用 Gradle 鏡像：
   ```gradle
   // settings.gradle
   pluginManagement {
       repositories {
           maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
           gradlePluginPortal()
       }
   }
   ```

4. **檢查網路連線**
   - 確保可以訪問 Maven 倉庫
   - 嘗試使用代理

---

### 問題 2：測試失敗

**症狀：**
- 執行 `./gradlew test` 有測試失敗
- 單元測試不通過

**可能原因：**
- 程式碼變更導致測試失敗
- 環境問題
- 測試本身有問題

**解決方案：**

1. **查看失敗的測試**
   ```bash
   ./gradlew test --info
   ```

2. **執行特定測試**
   ```bash
   ./gradlew test --tests "com.francium.SomeTest"
   ```

3. **跳過測試構建**
   如果緊急需要構建，可以暫時跳過測試：
   ```bash
   ./gradlew build -x test
   ```
   ⚠️ 不建議這麼做，除非你確定測試失敗不影響功能。

4. **回報問題**
   如果是官方程式碼的測試失敗，請在 GitHub 上提交 Issue。

---

### 問題 3：輕量級構建失敗（build.bat / build.sh）

**症狀：**
- 執行 `build.bat` 失敗
- 出現 `package does not exist` 錯誤

**可能原因：**
- 缺少依賴庫
- 輕量級構建不包含所有功能
- 檔案路徑錯誤

**解決方案：**

1. **理解輕量級構建的限制**
   - 輕量級構建只包含核心功能
   - 不包含外部依賴（如 ASM、Gson、Netty 等）
   - 主要用於快速驗證核心邏輯

2. **使用 Gradle 構建**
   如果需要完整功能，請使用 Gradle：
   ```bash
   ./gradlew build
   ```

3. **檢查檔案是否存在**
   確保所有原始檔案都在正確的位置。

---

## 💻 開發問題

### 問題 1：IDE 無法辨識專案

**症狀：**
- IntelliJ IDEA / VS Code 無法正確載入專案
- 出現大量紅色錯誤
- 無法自動補全

**可能原因：**
- 沒有正確匯入 Gradle 專案
- IDE 快取損壞
- 缺少外掛

**解決方案：**

1. **重新匯入 Gradle 專案**
   - IntelliJ IDEA：`File → Open → 選擇 build.gradle`
   - VS Code：安裝 Gradle for Java 擴充功能

2. **重新整理快取**
   - IntelliJ IDEA：`File → Invalidate Caches / Restart`
   - VS Code：`Java: Clean Java Language Server Workspace`

3. **安裝必要的外掛**
   - IntelliJ IDEA：確保已安裝 Gradle 外掛
   - VS Code：安裝 Extension Pack for Java

---

### 問題 2：除錯（Debug）無法連接

**症狀：**
- 無法設定中斷點
- 除錯器無法連接
- 程式碼執行順序不對

**可能原因：**
- 沒有以除錯模式啟動
- 類別被優化或混淆
- 原始碼與執行的位元組碼不匹配

**解決方案：**

1. **以除錯模式啟動**
   ```bash
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar francium-loader.jar
   ```

2. **禁用優化**
   ```toml
   [debug]
   disableOptimizations = true
   ```

3. **使用 Gradle 除錯任務**
   ```bash
   ./gradlew run --debug-jvm
   ```

---

### 問題 3：Mixin 不生效

**症狀：**
- Mixin 設定檔沒有被載入
- 注入的程式碼沒有執行
- 出現 Mixin 錯誤

**可能原因：**
- Mixin 設定檔格式錯誤
- 目標類別不存在
- Mixin 版本不相容

**解決方案：**

1. **檢查 Mixin 設定檔**
   確保 JSON 格式正確：
   ```json
   {
     "required": true,
     "package": "com.example.mixins",
     "compatibilityLevel": "JAVA_21",
     "mixins": [
       "ExampleMixin"
     ],
     "client": [
       "ClientMixin"
     ]
   }
   ```

2. **檢查 francium.json**
   確保在 `francium.json` 中聲明了 Mixin：
   ```json
   {
     "mixins": [
       "mixins.example.json"
     ]
   }
   ```

3. **查看 Mixin 日誌**
   啟用 Mixin 除錯日誌：
   ```toml
   [mixin]
   debug = true
   ```

---

## 🆘 獲取幫助

如果以上方法都無法解決你的問題，你可以透過以下管道獲得幫助：

### 1. 🔍 先自查

- 📖 閱讀 [快速開始指南](QUICKSTART.md)
- 📖 閱讀 [開發者指南](DEVELOPER_GUIDE.md)
- 🔍 搜尋 [GitHub Issues](https://github.com/stanley-1028/francium-loader/issues)
- 📋 查看 [常見問題](QUICKSTART.md#-常見問題--faq)

### 2. 📝 提交 Issue

如果確定是 Bug 或有新功能建議，請在 GitHub 上提交 Issue：

**提交前請準備：**
- 📋 問題描述（盡量詳細）
- 🔄 重現步驟
- 💻 系統環境（作業系統、Java 版本、Minecraft 版本）
- 📜 錯誤日誌（完整的堆疊追蹤）
- 📦 模組清單（如果與模組相關）

**提交位置：**
[GitHub Issues](https://github.com/stanley-1028/francium-loader/issues/new)

### 3. 💬 社群討論

- 加入社群（詳見 [社群指南](docs/COMMUNITY_GUIDE.md)）
- 在討論區提問
- 和其他使用者交流

### 4. 📧 聯絡開發者

如果是敏感問題或需要私下聯絡，可以透過 GitHub 聯絡開發者。

---

## 💡 除錯小技巧

### 啟用除錯日誌

在配置中啟用除錯日誌可以獲得更多資訊：

```toml
[logging]
level = "DEBUG"  # 或 TRACE
```

### 匯出系統資訊

```bash
francium doctor
```
這會輸出系統環境、Java 版本、記憶體使用等資訊，方便除錯。

### 建立最小重現範例

如果要回報 Bug，盡量建立一個最小的可重現範例：
- 只保留必要的模組
- 提供清晰的重現步驟
- 附上相關的設定檔和日誌

---

**祝你使用愉快！有問題不要害怕提問，我們很樂意幫助你。** 🎉
