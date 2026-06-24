# 常見問題 / FAQ

本文檔彙總了 Francium Mod Loader 最常見的問題和解答。如果你在這裡找不到答案，可以參考其他文檔或在社群提問。

---

## 📋 目錄

1. [一般問題](#一般問題)
2. [安裝與配置](#安裝與配置)
3. [使用問題](#使用問題)
4. [模組相關](#模組相關)
5. [AI Bridge 相關](#ai-bridge-相關)
6. [性能相關](#性能相關)
7. [開發者相關](#開發者相關)
8. [故障排除](#故障排除)

---

## ❓ 一般問題

### Q1: Francium 是什麼？

**A:** Francium 是一個 AI 驅動的 Minecraft 跨版本模組加載器。它的核心特色是：
- 🤖 AI 版本橋接：自動適配不同 Minecraft 版本的模組
- ⚡ DAG 並行加載：比傳統加載器快 3-10 倍
- 🧩 雙生態相容：同時支援 Forge 和 Fabric 模組
- 📦 套件管理器：npm-like 的模組管理體驗

---

### Q2: Francium 和 Forge/Fabric 有什麼不同？

**A:** 主要區別在於：

| 特性 | Francium | Forge | Fabric |
|------|----------|-------|--------|
| 跨版本支援 | ✅ AI 自動橋接 | ❌ 每版本重新編譯 | ❌ 每版本重新編譯 |
| 並行加載 | ✅ DAG 並行 | ❌ 循序加載 | ❌ 循序加載 |
| 雙生態相容 | ✅ Forge + Fabric | ❌ 僅 Forge | ❌ 僅 Fabric |
| 套件管理器 | ✅ 內建 | ⚠️ 需第三方 | ⚠️ 需第三方 |
| 成熟度 | ⚠️ 開發中 | ✅ 非常成熟 | ✅ 成熟 |

**簡單來說：** Francium 是一個實驗性的下一代加載器，有很多創新功能，但還在發展中。

---

### Q3: Francium 支援哪些 Minecraft 版本？

**A:** 目前主要測試和支援：
- ✅ Minecraft 1.20.4
- ✅ Minecraft 1.21
- ⚠️ 其他版本理論上可以透過 AI Bridge 支援，但可能需要更多測試

Mapping 數據庫目前包含 1.20.4 和 1.21 的種子映射。

---

### Q4: Francium 是免費的嗎？

**A:** 是的，完全免費且開源！
- 📜 授權：MIT License
- 💻 原始碼：完全開源
- 🎯 商業使用：允許（MIT 授權）

---

### Q5: Francium 會影響遊戲性能嗎？

**A:** 一般來說，Francium 對遊戲運行時性能影響很小：
- ⚡ 加載速度：更快（並行加載）
- 🧠 記憶體使用：略高一些（隔離類加載器）
- 🎮 遊戲運行：幾乎沒有影響

如果遇到性能問題，可以參考 [PERFORMANCE.md](PERFORMANCE.md) 進行優化。

---

## 🔧 安裝與配置

### Q6: 如何安裝 Francium？

**A:** 有幾種安裝方式：

**方式一：發行版套件（推薦新手）**
1. 從 [GitHub Releases](https://github.com/stanley-1028/francium-loader/releases/latest) 下載最新的 ZIP
2. 解壓縮到任意目錄
3. 執行 `install_windows.bat`（Windows）或按照說明手動安裝

**方式二：MultiMC / Prism Launcher 匯入**
1. 下載發行版 ZIP
2. 在 MultiMC 中選擇「匯入實例」
3. 選擇 ZIP 檔案

**方式三：手動安裝**
1. 下載 `francium-loader.jar`
2. 當作 Java Agent 使用：`java -javaagent:francium-loader.jar -jar minecraft.jar`

詳細教學請參考 [QUICKSTART.md](QUICKSTART.md)。

---

### Q7: 安裝 Francium 需要 Java 嗎？

**A:** 是的，需要 Java 21 或更高版本。

**檢查 Java 版本：**
```bash
java -version
```

**如果沒有 Java：**
- Windows：從 [Adoptium](https://adoptium.net/) 下載 Temurin JDK 21
- macOS：`brew install openjdk@21`
- Linux：使用套件管理器安裝 openjdk-21-jdk

---

### Q8: Francium 會自動找到我的 .minecraft 嗎？

**A:** 是的，Francium 會自動偵測常見的 .minecraft 位置：

- Windows：`%APPDATA%\.minecraft`
- macOS：`~/Library/Application Support/minecraft`
- Linux：`~/.minecraft`

如果你的 Minecraft 安裝在非預設位置，可以手動指定：
```toml
[general]
minecraftDir = "自訂路徑/.minecraft"
```

---

### Q9: 配置檔案在哪裡？

**A:** 配置檔案通常在：
- `config/francium.toml`（相對於遊戲目錄）
- 或 `~/.config/francium/francium.toml`

如果找不到，可以參考 [TROUBLESHOOTING.md](TROUBLESHOOTING.md)。

---

### Q10: 如何備份我的設定？

**A:** 備份以下檔案和目錄：
- `config/francium.toml` - 主要設定
- `mods/` - 模組目錄
- `francium-lock.json` - 版本鎖定檔案

建議定期備份，特別是在更新之前。

---

## 🎮 使用問題

### Q11: 如何安裝模組？

**A:** 有幾種方式：

**方式一：套件管理器（推薦）**
```bash
# 搜尋模組
francium search 模組名稱

# 安裝模組
francium install 模組名稱

# 安裝指定版本
francium install 模組名稱@1.0.0
```

**方式二：手動安裝**
1. 下載模組 JAR 檔案
2. 放到 `mods/` 目錄
3. 重新啟動遊戲

---

### Q12: 如何查看已安裝的模組？

**A:** 使用指令：
```bash
francium list
```

或者直接查看 `mods/` 目錄。

---

### Q13: 如何更新模組？

**A:** 使用套件管理器：
```bash
# 檢查更新
francium update --check

# 更新所有模組
francium update

# 更新特定模組
francium update 模組名稱
```

---

### Q14: 如何解除安裝模組？

**A:** 使用指令：
```bash
francium remove 模組名稱
```

或者手動從 `mods/` 目錄刪除對應的 JAR 檔案。

---

### Q15: Francium 支援哪些模組來源？

**A:** 目前支援：
- ✅ Modrinth（完整支援）
- ⚠️ CurseForge（基本支援，需要 API Key）
- 🔜 Francium Registry（未來計畫）

---

## 📦 模組相關

### Q16: 所有 Forge/Fabric 模組都能執行嗎？

**A:** 不一定。雖然 Francium 支援雙生態，但：
- ✅ 大多數簡單模組可以直接執行
- ⚠️ 複雜模組可能需要調整
- ❌ 深度修改遊戲的模組可能無法執行

建議在測試環境中先試用。

---

### Q17: 為什麼有些模組無法載入？

**A:** 可能的原因：
1. 模組依賴缺失 → 安裝對應的依賴模組
2. 版本不相容 → 檢查模組支援的 Minecraft 版本
3. 模組有 bug → 嘗試更新或回歸模組版本
4. Francium 的相容性問題 → 回報給開發團隊

詳細除錯請參考 [TROUBLESHOOTING.md](TROUBLESHOOTING.md)。

---

### Q18: 如何確認模組是否正確載入？

**A:** 檢查日誌檔案：
- 日誌位置：`logs/francium.log`
- 搜尋模組名稱，查看是否有載入成功的訊息

或者使用指令：
```bash
francium list --loaded
```

---

### Q19: 模組之間會衝突嗎？

**A:** 可能會。Francium 有 SAT 依賴求解器，可以自動解決部分衝突，但不是所有情況都能解決。

如果遇到衝突：
1. 查看日誌中的衝突資訊
2. 嘗試更新其中一個模組
3. 移除其中一個衝突的模組
4. 回報給模組作者或 Francium 團隊

---

### Q20: 可以同時使用 Forge 和 Fabric 模組嗎？

**A:** 是的，這是 Francium 的特色之一！

Francium 可以同時載入 Forge 和 Fabric 模組。但請注意：
- ⚠️ 某些模組可能不相容
- ⚠️ 效能可能會略受影響
- ✅ 大多數情況下可以正常執行

---

## 🤖 AI Bridge 相關

### Q21: AI Bridge 是什麼？

**A:** AI Bridge 是 Francium 的核心創新功能，它可以：
- 🔍 分析模組的位元組碼
- 🧠 比對不同版本的 Minecraft API
- 🔧 自動生成跨版本適配器
- 🎯 讓舊版本的模組在新版本上執行

簡單來說：它就像是模組的「翻譯官」。

---

### Q22: AI Bridge 需要連網嗎？

**A:** 不需要。AI Bridge 的所有功能都在本地執行：
- ✅ Mapping 數據庫內建在 JAR 中
- ✅ 相似度計算在本地完成
- ✅ 適配器生成在本地完成
- ✅ 不需要連接任何伺服器

---

### Q23: AI Bridge 的準確率如何？

**A:** 取決於多種因素：
- ✅ 常用 API：90%+ 準確率
- ⚠️ 冷門 API：70-80% 準確率
- ❌ 非常罕見的 API：可能匹配不到

Mapping 數據庫越豐富，準確率越高。你也可以貢獻映射來幫助改進！

---

### Q24: 可以關閉 AI Bridge 嗎？

**A:** 可以。如果你只玩單一版本的 Minecraft，可以關閉 AI Bridge 來節省資源：

```toml
[ai-bridge]
enabled = false
```

---

### Q25: 如何貢獻 Mapping 數據？

**A:** 歡迎貢獻！請參考 [docs/MAPPING_CONTRIBUTION_GUIDE.md](docs/MAPPING_CONTRIBUTION_GUIDE.md)。

貢獻方式：
1. 添加新的類別和方法映射
2. 修正錯誤的映射
3. 添加新的 Minecraft 版本支援
4. 改進跨版本映射

---

## ⚡ 性能相關

### Q26: Francium 的加載速度真的比較快嗎？

**A:** 是的！根據基準測試：

| 模組數量 | 傳統加載器 | Francium | 提升 |
|----------|-----------|----------|------|
| 20 個 | 15 秒 | 5 秒 | 3x |
| 50 個 | 45 秒 | 10 秒 | 4.5x |
| 100 個 | 125 秒 | 18 秒 | 6.9x |

實際速度取決於你的 CPU 和硬碟。

---

### Q27: 為什麼我的加載速度沒有明顯提升？

**A:** 可能的原因：
1. 模組依賴太複雜，並行度不高
2. 硬碟速度太慢（建議使用 SSD）
3. CPU 核心數太少
4. 並行加載沒有正確啟用

優化建議請參考 [PERFORMANCE.md](PERFORMANCE.md)。

---

### Q28: Francium 會用比較多記憶體嗎？

**A:** 會稍微多一點，大約多 10-20%：
- 每個模組有獨立的 ClassLoader
- Mapping 數據庫需要一些記憶體
- 物件池需要一些預留空間

如果記憶體緊張，可以參考 [PERFORMANCE.md](PERFORMANCE.md) 進行優化。

---

### Q29: 如何優化 Francium 的性能？

**A:** 快速優化清單：
1. ✅ 確保並行加載已啟用
2. ✅ 使用 SSD 儲存遊戲和模組
3. ✅ 合理設置記憶體（-Xmx4G 或更多）
4. ✅ 移除不需要的模組
5. ⭐ 調整並行執行緒數
6. ⭐ 啟用 G1 GC

詳細優化指南請參考 [PERFORMANCE.md](PERFORMANCE.md)。

---

### Q30: 如何執行基準測試？

**A:** 使用內建的基準測試工具：
```bash
francium benchmark
```

測試結果包括：
- 加載時間
- 記憶體使用
- CPU 使用率
- 並行效率

---

## 👨‍💻 開發者相關

### Q31: 如何開發 Francium 模組？

**A:** 基本步驟：
1. 設定開發環境（Java 21 + Gradle）
2. 建立新的 Gradle 專案
3. 新增 Francium API 依賴
4. 實作 `FranciumMod` 介面
5. 建立 `francium.json` 描述檔
6. 編譯和測試

詳細教學請參考 [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)。

---

### Q32: Francium 有開發模板嗎？

**A:** 有的！
- `francium-mod-template/` - 模組開發模板
- `examples/example-mod/` - 示例模組

你可以複製模板目錄，修改配置後開始開發。

---

### Q33: francium.json 有哪些欄位？

**A:** 主要欄位：

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `modId` | string | ✅ | 唯一識別碼 |
| `version` | string | ✅ | 版本號（SemVer） |
| `name` | string | ✅ | 顯示名稱 |
| `mainClass` | string | ✅ | 入口類別 |
| `description` | string | ❌ | 模組介紹 |
| `authors` | string[] | ❌ | 作者列表 |
| `dependencies` | object | ❌ | 依賴約束 |
| `mixins` | string[] | ❌ | Mixin 設定檔 |

詳細說明請參考 [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)。

---

### Q34: Francium 支援 Mixin 嗎？

**A:** 是的，內建支援 SpongePowered Mixin 0.8.7。

在 `francium.json` 中配置：
```json
{
  "mixins": [
    "mixins.mymod.json"
  ]
}
```

---

### Q35: 如何除錯我的模組？

**A:** 幾種方式：
1. 查看日誌：`logs/francium.log`
2. 啟用除錯模式：`francium --debug`
3. 使用 IDE 的除錯功能
4. 連接遠端除錯

---

## 🔧 故障排除

### Q36: Francium 無法啟動怎麼辦？

**A:** 按照以下步驟檢查：

1. **檢查 Java 版本**
   ```bash
   java -version
   ```
   需要 Java 21 或更高版本。

2. **檢查日誌**
   - 查看 `logs/francium.log`
   - 找到錯誤訊息

3. **嘗試安全模式**
   ```bash
   francium --safe-mode
   ```

4. **重新安裝**
   - 備份 `mods/` 和 `config/`
   - 重新下載 Francium
   - 還原備份

如果還是不行，請參考 [TROUBLESHOOTING.md](TROUBLESHOOTING.md)。

---

### Q37: 出現「依賴衝突」錯誤怎麼辦？

**A:** 幾種解決方法：
1. 更新其中一個模組到最新版本
2. 移除其中一個衝突的模組
3. 手動指定版本：`francium install mod@版本號`
4. 查看日誌了解詳細的衝突資訊

---

### Q38: 模組載入後遊戲閃退怎麼辦？

**A:** 除錯步驟：
1. 查看崩潰報告（`crash-reports/` 目錄）
2. 檢查是否是特定模組導致的
3. 嘗試單獨載入該模組
4. 更新模組到最新版本
5. 如果是 AI Bridge 的問題，可以嘗試關閉它

---

### Q39: 忘記密碼/帳號怎麼辦？

**A:** Francium 本身不需要帳號密碼。

如果你是指 Minecraft 帳號，請聯絡 Mojang 支援。

如果你是指 Modrinth/CurseForge 帳號，請到對應網站找回。

---

### Q40: 在哪裡可以獲得更多幫助？

**A:** 幾個管道：

1. **先自查**
   - [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - 故障排除指南
   - [PERFORMANCE.md](PERFORMANCE.md) - 性能優化指南
   - [QUICKSTART.md](QUICKSTART.md) - 快速開始指南

2. **GitHub Issues**
   - 提交 Bug 報告或功能請求
   - 先搜尋是否已有相同問題

3. **社群討論**
   - GitHub Discussions
   - Discord（詳見 [docs/COMMUNITY_GUIDE.md](docs/COMMUNITY_GUIDE.md)）

---

## 📚 更多資源

- 🚀 [快速開始指南](QUICKSTART.md)
- 📖 [項目結構說明](PROJECT_STRUCTURE.md)
- 🔧 [故障排除指南](TROUBLESHOOTING.md)
- ⚡ [性能優化指南](PERFORMANCE.md)
- 🛡️ [安全政策](SECURITY.md)
- 👨‍💻 [開發者指南](DEVELOPER_GUIDE.md)
- 🤝 [貢獻指南](CONTRIBUTING.md)

---

**還有問題？歡迎在社群提問！💬**

如果你的問題很常見，我們會把它加到這份 FAQ 中。
