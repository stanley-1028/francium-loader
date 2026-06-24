# Francium 快速開始指南 / Quick Start Guide

本指南將幫助你在 5 分鐘內開始使用 Francium Mod Loader。

---

## 📋 前置需求 / Prerequisites

- **Java 21 或更高版本**
- **Minecraft 1.20.4 或更高版本**（理論支援任意版本，需 AI Bridge 輔助）
- **大約 100 MB 可用磁碟空間**

---

## 🚀 快速安裝 / Quick Install

### 方法一：發行版套件（推薦普通用戶）

1. **下載發行版**
   - 前往 [GitHub Releases](https://github.com/stanley-1028/francium-loader/releases/latest)
   - 下載最新的 `FranciumLoader-2.4.0.zip`

2. **解壓縮**
   - 解壓縮到任意目錄，例如 `C:\FranciumLoader` 或 `~/FranciumLoader`

3. **一鍵安裝（Windows）**
   - 以**系統管理員身份**執行 `install_windows.bat`
   - 腳本會自動：
     - 檢測 Java 版本
     - 檢測 .minecraft 目錄
     - 加入 PATH 環境變數
     - 創建桌面捷徑

4. **驗證安裝**
   ```bash
   francium --version
   ```

### 方法二：便攜版（適合進階用戶）

1. 下載 `francium-loader.jar`
2. 放在任意位置
3. 直接運行：
   ```bash
   java -jar francium-loader.jar
   ```

### 方法三：MultiMC / Prism Launcher 匯入

1. 下載發行版套件
2. 在啟動器中**新增實例**
3. 選擇 **「從 ZIP 匯入」**
4. 選取發行版中的 `mmc-pack.json`
5. 啟動器自動配置 Java 參數與模組目錄

---

## 🎮 安裝你的第一個模組 / Install Your First Mod

### 使用套件管理器（推薦）

Francium 內建了類似 npm 的套件管理器，可以一鍵安裝模組。

```bash
# 搜索模組
francium search "shader"

# 安裝模組（自動解析依賴）
francium install sodium

# 安裝指定版本
francium install sodium@^1.2.0

# 查看已安裝模組
francium list

# 檢查更新
francium update

# 卸載模組
francium uninstall sodium
```

### 手動安裝

1. 下載模組 JAR 檔
2. 放入 `mods/` 目錄
3. 執行 `francium scan` 重新掃描

---

## 🛠️ 開發者快速開始 / Developer Quick Start

### 加入依賴

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

**Maven：**

```xml
<dependency>
    <groupId>com.francium</groupId>
    <artifactId>francium-loader</artifactId>
    <version>2.4.0</version>
</dependency>
```

### 基本使用範例

```java
import com.francium.loader.FranciumLoader;
import java.nio.file.Paths;

public class MyModLoader {
    public static void main(String[] args) throws Exception {
        // 1. 初始化加載器
        FranciumLoader loader = FranciumLoader.builder(Paths.get("."))
            .withParallelLoading(true)      // 啟用並行加載
            .withMemoryManagement(true)     // 啟用記憶體管理
            .withAiBridge(true)             // 啟用 AI 版本橋接
            .withMixin(true)                // 啟用 Mixin 支援
            .build();

        // 2. 掃描並載入模組
        FranciumLoader.FranciumReport report = loader.launch();

        // 3. 查看結果
        System.out.println("載入模組數: " + report.totalMods);
        System.out.println("分層數: " + report.layers);
        System.out.println("載入時間: " + report.loadTimeMs + "ms");
        System.out.println("加速比: " + report.speedupRatio + "x");
    }
}
```

### 創建你的第一個模組

#### 1. 創建 francium.json

在模組 JAR 的 `META-INF/francium.json` 中描述模組資訊：

```json
{
    "modId": "my-first-mod",
    "version": "1.0.0",
    "name": "My First Mod",
    "description": "我的第一個 Francium 模組",
    "authors": ["Your Name"],
    "mainClass": "com.example.mymod.MyMod",
    "mcVersionRange": ["1.20.4", "1.21"],
    "aiBridgeEnabled": true,
    "dependencies": {
        "fabric-api": ">=0.90.0"
    },
    "mixins": [
        "mixins.mymod.json"
    ]
}
```

#### 2. 創建主類

```java
package com.example.mymod;

import com.francium.api.FranciumMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyMod implements FranciumMod {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyMod.class);

    @Override
    public void onInitialize() {
        LOGGER.info("My First Mod 已載入！");
        // 在這裡初始化你的模組
    }

    @Override
    public String getModId() {
        return "my-first-mod";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

#### 3. 使用 Gradle 模板

你也可以使用 Francium 提供的 Gradle 模板專案：

```bash
# 複製模板
cp -r francium-mod-template my-mod

# 修改配置
cd my-mod
# 編輯 build.gradle 和 src/main/resources/META-INF/francium.json

# 構建
./gradlew build
```

---

## ⚙️ 配置說明 / Configuration

### 基本配置

Francium 的配置檔位於 `config/francium.toml`：

```toml
[general]
# 是否啟用並行加載
parallelLoading = true

# 並行執行緒數（0 = 自動偵測）
parallelThreads = 0

# 是否啟用 AI 版本橋接
aiBridgeEnabled = true

# 是否啟用記憶體管理
memoryManagement = true

[logging]
# 日誌級別：TRACE, DEBUG, INFO, WARN, ERROR
level = "INFO"

# 是否輸出到檔案
fileOutput = true

[mixin]
# 是否啟用 Mixin
enabled = true

# Mixin 設定檔目錄
configDir = "config/mixin"
```

---

## 🧪 嘗試 Demo / Try the Demo

Francium 提供了一個獨立的 Demo，無需 Minecraft 即可展示核心功能：

```bash
# Windows
demo.bat

# Linux/Mac
./demo.sh

# 或直接運行
java FranciumDemo.java
```

Demo 會展示：
1. SAT 依賴求解：20 個模組的依賴自動解析
2. DAG 拓撲分層：可視化依賴圖分層
3. 並行加載模擬：各層並行加載時間線
4. 記憶體分析：模擬物件池與洩漏檢測
5. Mapping 數據庫：展示 80 類別 / 805 方法的跨版本映射

---

## 📚 下一步 / Next Steps

- 📖 [完整開發者指南](DEVELOPER_GUIDE.md)
- 🎯 [路線圖](ROADMAP.md)
- 📝 [變更日誌](CHANGELOG.md)
- 🤝 [貢獻指南](CONTRIBUTING.md)
- 🌐 [社群指南](docs/COMMUNITY_GUIDE.md)
- 🗺️ [Mapping 貢獻指南](docs/MAPPING_CONTRIBUTION_GUIDE.md)

---

## ❓ 常見問題 / FAQ

### Q: Francium 和 Forge/Fabric 有什麼不同？

A: Francium 是一個全新設計的模組加載器，具有以下獨特功能：
- **DAG 並行加載**：比傳統循序加載快 3-10 倍
- **AI 版本橋接**：自動跨版本適配，無需等待模組更新
- **SAT 依賴求解**：自動解決依賴衝突
- **內建套件管理器**：類似 npm 的體驗
- **雙生態相容**：同時支援 Forge 和 Fabric 模組

### Q: 現有的 Forge/Fabric 模組可以直接用嗎？

A: 大部分基本功能的模組可以透過 AI Bridge 自動適配。但複雜的模組可能需要一些調整。我們正在持續優化 Mapping 數據庫來提高相容性。

### Q: 效能如何？會不會比較慢？

A: 不會。實際上，由於 DAG 並行加載，Francium 的加載速度比傳統加載器快 3-10 倍。100 個模組通常只需要 20-30 秒。

### Q: 安全嗎？會不會損壞我的存檔？

A: Francium 採用了多層安全機制：
- 每個模組獨立 ClassLoader，防止衝突
- 伺服器同步協議自動校驗模組完整性
- 記憶體分析器可以檢測潛在問題

但建議在使用前備份你的存檔，特別是在測試新模組時。

### Q: 如何回報 Bug 或提出建議？

A: 請在 [GitHub Issues](https://github.com/stanley-1028/francium-loader/issues) 中提交，我們會盡快回應。

---

## 🆘 獲得幫助 / Get Help

如果你遇到問題，可以透過以下管道獲得幫助：

1. 📖 先閱讀本文檔和 [常見問題](#-常見問題--faq)
2. 🔍 搜尋 [GitHub Issues](https://github.com/stanley-1028/francium-loader/issues)
3. 💬 加入社群討論（詳見 [社群指南](docs/COMMUNITY_GUIDE.md)）
4. 🐛 提交新的 [Issue](https://github.com/stanley-1028/francium-loader/issues/new)

---

**祝你使用愉快！🎉**

*"不要等待模組更新，讓 AI 替你橋接未來。"*
