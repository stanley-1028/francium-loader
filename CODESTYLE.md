# 代碼風格指南 / Code Style Guide

本文檔定義 Francium Mod Loader 的代碼風格和最佳實踐。所有貢獻者都應遵循這些規範，以保持代碼庫的一致性和可讀性。

---

## 📋 目錄

1. [總則](#總則)
2. [命名規範](#命名規範)
3. [代碼格式](#代碼格式)
4. [註釋規範](#註釋規範)
5. [Java 特定規範](#java-特定規範)
6. [設計原則](#設計原則)
7. [工具配置](#工具配置)
8. [提交規範](#提交規範)

---

## 🎯 總則

### 核心原則

1. **可讀性優先** - 代碼是寫給人看的，順便給機器執行
2. **一致性** - 遵循現有代碼的風格
3. **簡潔性** - 避免過度設計和不必要的抽象
4. **可維護性** - 易於理解、修改和除錯

### 黃金法則

> 如果你在修改現有程式碼，請遵循現有程式碼的風格。
> 如果你在撰寫新程式碼，請遵循本指南。
> 如果你不確定，請尋求指導。

---

## 🏷️ 命名規範

### 套件命名

- 使用全小寫字母
- 使用反向網域名稱
- 使用有意義的名稱

```java
// ✅ 正確
package com.francium.loader;
package com.francium.ai.bridge;

// ❌ 錯誤
package com.Francium.Loader;  // 大小寫混亂
package francium;              // 缺少反域名
```

### 類別命名

- 使用大寫駝峰式（PascalCase）
- 使用名詞或名詞片語
- 介面使用形容詞或名詞

```java
// ✅ 正確
public class ModLoader { ... }
public class ParallelClassLoader { ... }
public interface Loadable { ... }

// ❌ 錯誤
public class modLoader { ... }      // 小寫開頭
public class Mod_Loading { ... }    // 底線分隔
public class Load { ... }           // 動詞開頭（類別應該是名詞）
```

### 方法命名

- 使用小寫駝峰式（camelCase）
- 使用動詞或動詞片語
- 布林值方法使用 is/has/can 開頭

```java
// ✅ 正確
public void loadMods() { ... }
public boolean isLoaded() { ... }
public boolean hasDependencies() { ... }
public ModManifest getManifest() { ... }

// ❌ 錯誤
public void LoadMods() { ... }      // 大寫開頭
public boolean loaded() { ... }     // 缺少 is/has
public ModManifest manifest() { ... } // 缺少 get
```

### 變數命名

- 使用小寫駝峰式（camelCase）
- 使用有意義的名稱
- 避免縮寫（除非是廣為人知的）

```java
// ✅ 正確
private int modCount;
private String modId;
private List<ModManifest> loadedMods;

// ❌ 錯誤
private int mc;                     // 不明確的縮寫
private String mid;                 // 不明確的縮寫
private List<ModManifest> lm;       // 毫無意義的縮寫
```

### 常數命名

- 使用全大寫字母
- 使用底線分隔單字
- 使用 `static final` 修飾

```java
// ✅ 正確
public static final int MAX_MODS = 1000;
public static final String DEFAULT_CONFIG_PATH = "config/francium.toml";

// ❌ 錯誤
public static final int maxMods = 1000;           // 小寫
public static final String defaultConfigPath;      // 小寫，不是 final
```

### 泛型型別參數

- 使用單一大寫字母
- 常用：T（Type）、E（Element）、K（Key）、V（Value）

```java
// ✅ 正確
public class ModContainer<T> { ... }
public interface Loader<K, V> { ... }

// ❌ 錯誤
public class ModContainer<Type> { ... }  // 太長
public class ModContainer<t> { ... }     // 小寫
```

---

## 📝 代碼格式

### 縮排

- 使用 4 個空格縮排
- 不要使用 Tab 字元
- 連續行使用 8 個空格（雙倍縮排）

```java
// ✅ 正確
public void example() {
    if (condition) {
        doSomething();
    }
}

// 方法呼叫換行
someObject.methodWithManyParameters(
        firstParameter,
        secondParameter,
        thirdParameter);
```

### 大括號

- 使用埃及風格（K&R style）
- 左大括號與敘述同一行
- 右大括號單獨一行

```java
// ✅ 正確
if (condition) {
    doSomething();
} else {
    doSomethingElse();
}

// ❌ 錯誤
if (condition)
{                          // 左大括號單獨一行
    doSomething();
}
```

### 行長度

- 目標：80-120 字元
- 最大：150 字元
- 超過時適當換行

### 空行

- 方法之間空一行
- 邏輯區塊之間空一行
- 不要有多個連續空行

```java
// ✅ 正確
public void method1() {
    // 第一個邏輯區塊
    doSomething();
    doAnotherThing();

    // 第二個邏輯區塊
    doMoreThings();
}

public void method2() {
    // ...
}
```

### 匯入排序

- 靜態匯入在前
- 其他匯入按字母排序
- 不同套件群組之間空一行

```java
// ✅ 正確
import static java.util.Objects.requireNonNull;

import com.francium.loader.ModManifest;
import com.francium.loader.ModContainer;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

---

## 💬 註釋規範

### 一般原則

- 註釋「為什麼」，而不是「做什麼」
- 保持註釋更新，過時的註釋比沒有註釋更糟
- 避免顯而易見的註釋

```java
// ✅ 好的註釋
// 使用 DAG 並行加載以提高效能（傳統循序加載在 100 mods 時需要 2 分鐘）
loadModsParallel();

// ❌ 壞的註釋
// 載入模組
loadMods();  // 這是顯而易見的
```

### Javadoc

所有公開的類別、方法和常數都應該有 Javadoc。

#### 類別 Javadoc

```java
/**
 * 核心模組加載器，負責發現、解析和載入所有模組。
 *
 * <p>使用 DAG 拓撲排序實現並行加載，比傳統循序加載快 3-10 倍。
 * 支援 Forge 和 Fabric 雙生態，並透過 AI Bridge 實現跨版本相容。
 *
 * <p>使用範例：
 * <pre>{@code
 * FranciumLoader loader = FranciumLoader.builder()
 *     .modsDir(Paths.get("mods"))
 *     .parallelLoading(true)
 *     .build();
 * loader.load();
 * }</pre>
 *
 * @see ModGraph
 * @see ParallelModClassLoader
 * @since 1.0.0
 */
public class FranciumLoader { ... }
```

#### 方法 Javadoc

```java
/**
 * 載入所有模組。
 *
 * <p>此方法會執行以下步驟：
 * <ol>
 *   <li>發現 mods 目錄中的所有模組</li>
 *   <li>解析依賴關係並構建 DAG</li>
 *   <li>按拓撲順序並行載入模組</li>
 *   <li>初始化所有已載入的模組</li>
 * </ol>
 *
 * @param modsDir 模組目錄路徑
 * @return 載入結果，包含成功和失敗的模組
 * @throws IOException 如果讀取模組檔案失敗
 * @throws DependencyException 如果依賴解析失敗
 * @since 1.0.0
 */
public LoadResult loadMods(Path modsDir) throws IOException { ... }
```

### 行內註釋

- 使用 `//` 進行行內註釋
- 放在程式碼上方或右側
- 保持簡潔

```java
// 計算並行度（CPU 核心數的 1.5 倍）
int parallelism = Runtime.getRuntime().availableProcessors() * 3 / 2;
```

### TODO 註釋

使用 `TODO` 標記待完成的工作，並附上說明和（可選的）日期。

```java
// TODO: 優化這個算法，目前是 O(n²)，可以改進到 O(n log n)
// TODO(2026-06-24): 添加更多錯誤處理
// FIXME: 這個方法在邊界情況下會崩潰
```

---

## ☕ Java 特定規範

### 可見性

- 儘量使用最小的可見性
- 欄位預設為 `private`
- 只公開必要的 API

```java
// ✅ 正確
public class ModLoader {
    private final List<ModContainer> mods;  // private 欄位
    protected void initialize() { ... }     // 子類別可見
    public void load() { ... }              // 公開 API
}
```

### final 關鍵字

- 不可變的欄位使用 `final`
- 不打算被覆寫的方法使用 `final`
- 不打算被繼承的類別使用 `final`

```java
// ✅ 正確
public final class ImmutableConfig {
    private final String modId;
    private final int version;

    public final String getModId() {
        return modId;
    }
}
```

### 異常處理

- 不要捕獲 `Exception` 或 `Throwable`
- 捕獲具體的異常類型
- 不要忽略異常（空的 catch 區塊）
- 適當記錄異常

```java
// ✅ 正確
try {
    loadModFile(path);
} catch (IOException e) {
    logger.error("Failed to load mod: {}", path, e);
    throw new ModLoadException("Failed to load mod: " + path, e);
}

// ❌ 錯誤
try {
    loadModFile(path);
} catch (Exception e) {
    // 什麼都不做
}
```

### 日誌記錄

- 使用 SLF4J 記錄器
- 使用適當的日誌級別
- 不要記錄敏感資訊

```java
// ✅ 正確
private static final Logger logger = LoggerFactory.getLogger(ModLoader.class);

logger.info("Loading {} mods", modCount);
logger.debug("Mod {} loaded in {}ms", modId, duration);
logger.warn("Mod {} has unresolved dependencies", modId);
logger.error("Failed to load mod {}", modId, exception);
```

**日誌級別使用指南：**

| 級別 | 使用時機 |
|------|----------|
| ERROR | 嚴重錯誤，可能導致程式崩潰 |
| WARN | 潛在問題，但程式可以繼續執行 |
| INFO | 重要的資訊訊息（預設級別） |
| DEBUG | 除錯用的詳細資訊 |
| TRACE | 非常詳細的追蹤資訊 |

### 集合使用

- 介面宣告，實例創用具體類別
- 使用鑽石運算子（`<>`）
- 考慮執行緒安全

```java
// ✅ 正確
List<String> mods = new ArrayList<>();
Map<String, ModContainer> modMap = new HashMap<>();

// ❌ 錯誤
ArrayList<String> mods = new ArrayList<String>();  // 具體類型宣告 + 冗餘型別參數
```

### 選用式（Optional）

- 用於可能為 null 的傳回值
- 不要用於欄位或方法參數
- 優先使用 `orElse()`、`orElseGet()`、`ifPresent()`

```java
// ✅ 正確
public Optional<ModContainer> getMod(String modId) {
    return Optional.ofNullable(modMap.get(modId));
}

// 使用方式
getMod("example").ifPresent(mod -> {
    // 處理模組
});

// ❌ 錯誤
public ModContainer getMod(String modId) {
    return modMap.get(modId);  // 可能回傳 null
}
```

---

## 🏗️ 設計原則

### SOLID 原則

1. **單一職責原則（SRP）** - 一個類別應該只有一個改變的理由
2. **開放封閉原則（OCP）** - 對擴充開放，對修改封閉
3. **里氏替換原則（LSP）** - 子類別可以替換父類別
4. **介面隔離原則（ISP）** - 多個小介面比一個大介面好
5. **依賴反轉原則（DIP）** - 依賴抽象，不依賴具體實作

### 其他原則

- **DRY**（Don't Repeat Yourself）- 不要重複自己
- **KISS**（Keep It Simple, Stupid）- 保持簡單
- **YAGNI**（You Aren't Gonna Need It）- 你不會需要它
- **最少驚訝原則** - 程式碼的行為應該符合預期

---

## 🔧 工具配置

### EditorConfig

專案已配置 `.editorconfig`，大部分 IDE 會自動套用：

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space
indent_size = 4

[*.{md,txt,yml,yaml}]
indent_size = 2
```

### Checkstyle

專案使用 Checkstyle 進行代碼風格檢查，設定檔位於 `config/checkstyle/`。

執行檢查：
```bash
./gradlew checkstyleMain
./gradlew checkstyleTest
```

### IDE 設定

#### IntelliJ IDEA

1. 安裝 Checkstyle 外掛
2. 匯入專案的程式碼風格設定
3. 啟用 EditorConfig 支援
4. 設定儲存時自動格式化

#### VS Code

1. 安裝 Java 擴充套件包
2. 安裝 Checkstyle for Java 擴充
3. 安裝 EditorConfig for VS Code

---

## 📝 提交規範

### 提交訊息格式

使用約定式提交（Conventional Commits）格式：

```
<類型>(<範圍>): <主題>

<內文>

<頁尾>
```

### 類型

| 類型 | 說明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修復 bug |
| `docs` | 文件更新 |
| `style` | 代碼風格調整（不影響功能） |
| `refactor` |重構（不是新功能也不是修 bug） |
| `perf` | 效能優化 |
| `test` | 新增或更新測試 |
| `build` | 建構系統或依賴更新 |
| `ci` | CI/CD 設定更新 |
| `chore` | 其他雜項 |

### 範例

```
feat(ai-bridge): add cross-version mapping for 1.21

新增 Minecraft 1.21 的種子映射，包含 80 個類別和 805 個方法。
同時更新了跨版本映射，支援 1.20.4 到 1.21 的自動轉換。

Closes #123
```

```
fix(core): fix null pointer exception in ModGraph

當模組沒有依賴時，ModGraph 會拋出 NullPointerException。
新增了空值檢查和單元測試。

Fixes #456
```

---

## 📚 更多資源

- 🤝 [貢獻指南](CONTRIBUTING.md)
- 📖 [開發者指南](DEVELOPER_GUIDE.md)
- 🗺️ [項目結構說明](PROJECT_STRUCTURE.md)
- 🛡️ [安全政策](SECURITY.md)

---

**感謝你幫助保持 Francium 的代碼品質！✨**

如果你對代碼風格有任何建議，歡迎提交 Issue 或 PR 討論。
