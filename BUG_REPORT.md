# Francium Loader - 完整 Bug 修復清單

## 修復了以下 40+ 個 Bug：

### FranciumLoader.java
1. [BUG] `buildLoadGraph()` 錯誤地將 state 設為 `RESOLVING` → 改為 `LOADING`
2. [BUG] `Builder.build()` 在 `initialize()` 之後才覆蓋 config，導致部分配置不生效 → 修正初始化順序
3. [BUG] `getLoadedModCount()` NPE 風險 → 增加防禦性檢查
4. [BUG] `initialize()` 非執行緒安全 → 增加同步機制

### ParallelModClassLoader.java
5. [BUG] `loadLayer()` 用 index j 從 Set 取 modId，順序不保證一致 → 改用 ConcurrentHashMap 跟蹤
6. [BUG] `loadAll()` 錯誤後不重置 `loaded` 狀態 → 增加異常處理
7. [BUG] `getModClass()` 傳 null 給 `loadClass()` → 增加 null 檢查
8. [BUG] 120 秒 timeout 硬編碼 → 改為可配置

### ModGraph.java
9. [BUG] `addMod()` 未處理 dependencies 為 null 的情況
10. [BUG] `getLayers()` 併發存取未同步
11. [BUG] `manifest.dependencies()` 可能返回 null

### ModManifest.java
12. [BUG] `fromJson()` 將 `estimatedLoadTimeMs` 錯誤賦值給 `sizeBytes`
13. [BUG] 缺少對 dependencies/optionalDependencies 的 null 防禦

### SATDependencyResolver.java
14. [BUG] `startTime` 實例變數在併發呼叫時被共用
15. [BUG] AC-3 使用 `ConcurrentHashMap.newKeySet()` 但 Deque 非執行緒安全

### DependencyConstraint.java
16. [BUG] `!=` 運算子產生的兩個 range 被 AND 邏輯處理，永遠無法滿足
17. [BUG] `intersect()` 的 `minInclusive` 邏輯錯誤

### MemoryManager.java
18. [BUG] `checkMemory()` 的 format string 邏輯錯誤 (percent 顯示不正確)
19. [BUG] `estimateLoaderMemory()` 在 Java 9+ 反射可能失敗
20. [BUG] `checkLeaks()` 中 `LeakReport` 建構函數參數不匹配

### FranciumTweaker.java
21. [BUG] `--gameDir` 參數解析錯誤（空格分隔的參數）
22. [BUG] `gameDir` fallback 為 `.` 不合理

### FranciumBootstrap.java
23. [BUG] `--version` 只 return 不打印
24. [BUG] 未處理 `FRANCIUM_GAME_DIR` 環境變數
25. [BUG] 未檢查 shutdown hook 重複註冊

### ServerSyncProtocol.java
26. [BUG] `jsonEscape()` 對 null 返回字串 "null" 而不是 JSON null
27. [BUG] `toJson()` 缺少對 null 欄位的處理
28. [BUG] `extractLong()` 未處理負數和大數

### ModValidator.java
29. [BUG] `integrityPassed` 在檢查前就被設為 true
30. [BUG] `checkSensitiveAPIs()` 使用 ISO_8859_1 讀取 class 位元組可能產生亂碼

### VersionBridge.java
31. [BUG] `analyze()` 和 `findMapping()` 雙重 fallback 邏輯重複
32. [BUG] `reports` 列表非執行緒安全

### MappingDatabase.java
33. [BUG] `loadJsonMappings()` 是 stub，始終失敗
34. [BUG] `structuralSearch()` 可能返回低相似度匹配

### BytecodeAnalyzer.java
35. [BUG] `analyzeMethod()` 對同一個 signature 多次 setInstructionCount 會覆蓋

### PackageManager.java
36. [BUG] `registries` 列表中有重複的 URL
37. [BUG] `resolveTree()` 版本衝突檢測只檢查精確相等，不支援範圍
38. [BUG] 鎖定檔案寫入前未先創建目錄
