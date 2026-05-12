# 🧪 Francium Mod Template

**Fork this. Edit one file. Ship your mod. 不要再手動管理模組了，讓 Francium 替你打工。**

[![Francium](https://img.shields.io/badge/Francium-1.0.0--alpha-blue)]()
[![Java](https://img.shields.io/badge/Java-21%2B-orange)]()
[![License](https://img.shields.io/badge/License-MIT-green)]()

---

## 五分鐘快速開始

```bash
# 1. Fork 這個 repo 或直接 clone
git clone https://github.com/YOU/my-francium-mod.git
cd my-francium-mod

# 2. 改個名字
#    編輯 gradle.properties，把 modId / modName / mainClass 改成你的

# 3. 寫程式
#    打開 src/main/java/com/example/ExampleMod.java
#    在 onInitialize() 裡面註冊你的方塊/物品/實體

# 4. Build
./gradlew build        # macOS / Linux
gradlew.bat build      # Windows

# 5. 產物在 build/libs/my-francium-mod-1.0.0.jar
#    丟進 Minecraft 的 mods/ 資料夾，啟動！
```

就是這麼簡單。不需要學 Mixin、不需要配 AccessWidener、不需要跟 Fabric Loom 打架。你寫 Java，Francium 搞定剩下的。

---

## 目錄結構

```
my-francium-mod/
├── build.gradle              # Francium 依賴 + 打包設定
├── settings.gradle           # 專案名稱
├── gradle.properties          # ← 改這個：modId, 版本號, MC 版本
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   └── ExampleMod.java      # ← 你的程式起點
│   │   └── resources/
│   │       └── francium-mod.json    # ← 模組清單（自動從 gradle.properties 填入）
│   └── test/
│       └── java/com/example/
│           └── ExampleModTest.java  # 測試（不需要開 Minecraft 就能跑！）
└── .gitignore
```

---

## francium-mod.json 說明

這是 Francium 的模組清單檔，類似 Fabric 的 `fabric.mod.json` 或 Forge 的 `mods.toml`。Francium 也**向後相容**這兩種格式——所以如果你已經有 Fabric mod，直接把 jar 丟進 Francium 的 `mods/` 就能跑。

```json
{
  "schemaVersion": 1,
  "id": "my-awesome-mod",         // 唯一識別碼，只能用小寫字母和連字號
  "name": "My Awesome Mod",       // 人類可讀的名稱
  "version": "1.0.0",             // SemVer 版本號
  "mcVersion": "1.21",            // 目標 Minecraft 版本
  "mainClass": "com.example.ExampleMod",  // 入口類別
  "dependencies": {               // 硬依賴（缺了就無法載入）
    "fabric-api": ">=0.100.0"
  },
  "optionalDependencies": {       // 軟依賴（有的話就載入，沒有也能跑）
    "sodium": ">=0.5.0"
  },
  "entrypoints": {                // 生命週期入口點
    "main": "com.example.ExampleMod::onInitialize",
    "client": "com.example.ExampleMod::onInitializeClient"
  },
  "mixins": [],                   // Mixin 設定（可選，Francium 支援 Fabric Mixin）
  "aiBridgeEnabled": true         // ← 讓 Francium 的 AI 自動幫你跨版本適配！
}
```

---

## 為什麼選 Francium 而不是 Forge/Fabric？

| 你想要的 | Forge | Fabric | Francium |
|----------|-------|--------|----------|
| 寫一個方塊 | ✅ 可行 | ✅ 可行 | ✅ 可行 |
| Build 時間 < 5 秒 | ❌ 等吧 | ❌ Loom 很慢 | ✅ `./gradlew build` |
| 不需要學 Gradle 外掛 | ❌ ForgeGradle | ❌ Loom | ✅ 標準 Java 專案 |
| 跨 MC 版本自動適配 | ❌ 重新編譯 | ❌ 重新編譯 | ✅ AI 橋接幫你搞定 |
| 模組衝突自動解決 | ❌ 手動 | ❌ 手動 | ✅ SAT 求解器 |
| 跑測試不需要開 Minecraft | ❌ | ❌ | ✅ JUnit 直接跑 |

> **Francium 的哲學**：模組開發者應該專注在「我的模組做什麼」，而不是「Gradle plugin 的 87 個配置選項」。

---

## 相依性管理

Francium 使用 **SAT 求解器** 自動解決依賴衝突。你只需要在 `francium-mod.json` 的 `dependencies` 區塊宣告你需要什麼，Francium 會：

1. 找到滿足所有約束的版本組合
2. 如果有多個解，自動選最優版本
3. 如果真的無解，它會**告訴你哪些模組在打架**，而不是丟一個 `NoClassDefFoundError` 讓你通靈

```json
"dependencies": {
  "sodium": ">=0.5.0 <1.0.0",       // SemVer 範圍
  "iris": "^1.7.0",                   // Caret: 相容版本
  "fabric-api": ">=0.90.0"            // 大於等於
}
```

---

## AI 版本橋接（Beta）

在 `francium-mod.json` 設定 `"aiBridgeEnabled": true`，Francium 會在載入時自動檢測你的模組呼叫的 Minecraft API，並在目標 MC 版本不同的情況下自動生成橋接代碼。

```
你的模組呼叫 → Block.m_12345_() (1.20.4 混淆名)
Francium 偵測到目標是 MC 1.21 →
  自動映射到 Block.getBlockState() →
  生成 Adapter class 透明轉發 →
  你的模組不用改就能跑！
```

是的，這聽起來像魔法。它其實是 ASM 字節碼分析 + 多版本映射資料庫 + 加權相似度匹配。詳見 [francium-ai-bridge 文檔](../docs/ARCHITECTURE.md#ai-版本橋接詳解)。

---

## 常用指令

```bash
# 編譯
./gradlew build

# 只跑測試
./gradlew test

# 打包成 fat jar（含所有依賴）
./gradlew shadowJar

# 清理
./gradlew clean
```

---

## FAQ

**Q: 我可以用 Kotlin 寫 Francium 模組嗎？**

A: 可以。Francium 是純 JVM 加載器，任何 JVM 語言都能用。加個 `kotlin` plugin 到 build.gradle 就好。

**Q: 現有的 Fabric mod 可以跑在 Francium 上嗎？**

A: 可以。Francium 讀取 `fabric.mod.json` 和 `mods.toml`，也支援 Mixin。

**Q: Francium 支援哪些 Minecraft 版本？**

A: 理論上支援 1.14+，目前測試過 1.20.4 和 1.21。AI 橋接支援跨版本適配。

**Q: 我的模組只是一個 Data Pack，也要用 Francium 嗎？**

A: Data Pack 直接用就好。Francium 是給需要 Java 程式碼的模組用的。

---

## 下一步

- 📖 [Francium 架構文檔](../docs/ARCHITECTURE.md)
- 📦 [Francium Loader 主專案](https://github.com/stanley-1028/francium-loader)
- 🤖 [AI 橋接技術細節](../docs/ARCHITECTURE.md#ai-版本橋接詳解)
- 🧪 [範例模組](../examples/example-mod/)

---

*Fork → Edit → Build → Ship. 不要再把時間花在跟 Gradle 打架了。*
