# Francium 性能優化指南 / Performance Optimization Guide

本指南將幫助你優化 Francium Mod Loader 的性能，獲得最佳的加載速度和運行體驗。

---

## 📋 目錄 / Table of Contents

1. [加載速度優化](#加載速度優化)
2. [記憶體優化](#記憶體優化)
3. [CPU 優化](#cpu-優化)
4. [AI Bridge 優化](#ai-bridge-優化)
5. [硬體建議](#硬體建議)
6. [基準測試](#基準測試)

---

## ⚡ 加載速度優化

### 1. 啟用並行加載（最重要）

Francium 最大的優勢就是 DAG 並行加載，確保它已啟用：

```toml
[general]
parallelLoading = true
```

**效果：** 3-10x 加載速度提升（取決於 CPU 核心數和模組依賴關係）

---

### 2. 調整並行執行緒數

根據你的 CPU 核心數調整並行執行緒數：

```toml
[general]
# 0 = 自動偵測（推薦）
parallelThreads = 0

# 或手動指定
# parallelThreads = 8
```

**建議：**
- 一般情況：使用自動偵測（0）
- CPU 核心數 ≤ 4：設為核心數
- CPU 核心數 > 4：設為核心數的 1-1.5 倍
- 筆記型電腦：適當減少，避免過熱

---

### 3. 使用 SSD

硬碟速度對加載時間影響很大：

| 儲存裝置 | 典型讀取速度 | 100 個模組估計加載時間 |
|----------|-------------|----------------------|
| HDD（機械硬碟） | 50-100 MB/s | 60-120 秒 |
| SATA SSD | 500-550 MB/s | 20-30 秒 |
| NVMe SSD | 3000-7000 MB/s | 10-20 秒 |

**建議：**
- 將 Minecraft 和模組放在 SSD 上
- 如果可能，使用 NVMe SSD
- 避免使用 USB 隨身碟或網路硬碟

---

### 4. 優化模組順序

雖然 Francium 會自動計算依賴關係，但合理的模組配置仍有幫助：

- **減少深層依賴**：依賴鏈越長，並行度越低
- **使用輕量級替代**：有些模組功能重疊，選擇更輕量的
- **移除不需要的模組**：這是最直接的優化方式

查看依賴圖：
```bash
francium graph --show-deps
```

---

### 5. 預生成適配器

如果你使用 AI Bridge，可以預生成適配器，避免執行時生成：

```toml
[ai-bridge]
preGenerateAdapters = true
```

**效果：** 第一次加載慢一些，但後續加載更快。

---

## 🧠 記憶體優化

### 1. 合理設置 JVM 記憶體

根據你的模組數量調整記憶體：

| 模組數量 | 建議最小記憶體 | 建議最大記憶體 |
|----------|--------------|--------------|
| < 20 | 2 GB | 4 GB |
| 20-50 | 3 GB | 6 GB |
| 50-100 | 4 GB | 8 GB |
| 100+ | 6 GB | 12 GB |

**設置方式：**
```bash
java -Xms2G -Xmx6G -jar francium-loader.jar
```

- `-Xms`：初始記憶體
- `-Xmx`：最大記憶體

**建議：**
- 不要設置過大的記憶體，反而可能降低效能
- Xms 和 Xmx 可以設為相同值，避免記憶體調整開銷
- 留出足夠的記憶體給作業系統和其他程式

---

### 2. 啟用記憶體管理

Francium 內建了記憶體管理功能：

```toml
[general]
memoryManagement = true
```

**功能包括：**
- 物件池（減少 GC 壓力）
- 記憶體洩漏偵測
- 自適應 GC 策略

---

### 3. 調整物件池大小

根據你的使用情況調整物件池：

```toml
[memory]
# 物件池大小（每種類型）
objectPoolSize = 1000

# 是否啟用物件池
objectPoolEnabled = true
```

**建議：**
- 記憶體充足：增大物件池
- 記憶體緊張：減小或關閉物件池

---

### 4. 啟用 G1 GC（推薦）

G1 GC 是現代 JVM 的預設垃圾回收器，適合大多數場景：

```bash
java -XX:+UseG1GC -Xmx6G -jar francium-loader.jar
```

**進階優化：**
```bash
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=50 \
     -XX:G1HeapRegionSize=16M \
     -Xmx6G \
     -jar francium-loader.jar
```

---

### 5. 監控記憶體使用

使用內建的記憶體分析器：

```bash
# 查看記憶體使用情況
francium profiler --memory

# 偵測記憶體洩漏
francium profiler --leak-detect

# 生成記憶體報告
francium profiler --report
```

---

## 🖥️ CPU 優化

### 1. 選擇合適的並行度

並行不是越多越好，找到最佳平衡點：

| CPU 核心數 | 建議並行執行緒數 |
|-----------|----------------|
| 2 核心 | 2-3 |
| 4 核心 | 4-6 |
| 6 核心 | 6-8 |
| 8 核心 | 8-12 |
| 16+ 核心 | 12-16 |

**測試最佳值：**
```bash
# 測試不同執行緒數的性能
francium benchmark --threads 4
francium benchmark --threads 8
francium benchmark --threads 12
```

---

### 2. 禁用不必要的功能

如果你不需要某些功能，可以禁用它們以節省 CPU：

```toml
# 關閉效能分析（如果不需要）
[profiler]
enabled = false

# 關閉 AI Bridge（如果不需要跨版本）
[ai-bridge]
enabled = false

# 關閉記憶體管理（如果不需要）
[memory]
enabled = false
```

---

### 3. 使用 JVM 編譯器優化

```bash
# 使用 C2 編譯器（預設，適合長時間運行）
java -XX:+TieredCompilation -XX:TieredStopAtLevel=4 -jar francium-loader.jar

# 快速啟動模式（犧牲一些長期性能換取更快的啟動）
java -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -jar francium-loader.jar
```

---

### 4. CPU 親和性（進階）

在 Linux 上，可以設置 CPU 親和性：

```bash
# 綁定到特定 CPU 核心
taskset -c 0-7 java -jar francium-loader.jar
```

---

## 🤖 AI Bridge 優化

### 1. 調整相似度閾值

根據你的需求調整相似度閾值：

```toml
[ai-bridge]
# 更高的閾值 = 更準確但可能匹配不到
# 更低的閾值 = 匹配更多但可能不準確
minConfidence = 0.7  # 預設值
```

| 閾值 | 準確率 | 覆蓋率 | 適用場景 |
|------|--------|--------|----------|
| 0.9 | 很高 | 低 | 生產環境、追求穩定 |
| 0.7 | 不錯 | 中等 | 平衡（預設） |
| 0.5 | 一般 | 高 | 測試、追求覆蓋率 |

---

### 2. 預熱 Mapping 快取

第一次使用 AI Bridge 時會比較慢，因為需要載入映射資料。可以預熱快取：

```bash
francium mapping --warmup
```

---

### 3. 使用本地 Mapping 檔案

如果你有自定義的映射檔案，可以放在本地：

```toml
[ai-bridge]
# 本地映射檔案目錄
localMappingsDir = "config/mappings"
```

---

### 4. 禁用 AI Bridge（如果不需要）

如果你只玩單一版本的 Minecraft，可以禁用 AI Bridge 來節省資源：

```toml
[ai-bridge]
enabled = false
```

---

## 💻 硬體建議

### 最低配置

| 配備 | 規格 |
|------|------|
| CPU | 雙核心處理器 |
| 記憶體 | 4 GB RAM |
| 儲存 | 1 GB 可用空間 |
| Java | Java 21+ |

**適合：** 少量模組（<20 個），體驗為主

---

### 推薦配置

| 配備 | 規格 |
|------|------|
| CPU | 四核心處理器（Intel i5 / AMD Ryzen 5） |
| 記憶體 | 8 GB RAM |
| 儲存 | SSD，5 GB 可用空間 |
| Java | Java 21+ |

**適合：** 中等模組包（20-50 個模組），良好體驗

---

### 旗艦配置

| 配備 | 規格 |
|------|------|
| CPU | 六核心或更多（Intel i7/i9 / AMD Ryzen 7/9） |
| 記憶體 | 16 GB+ RAM |
| 儲存 | NVMe SSD，10 GB+ 可用空間 |
| Java | Java 21+ |

**適合：** 大型模組包（50-100+ 個模組），最佳體驗

---

## 📊 基準測試

### 運行基準測試

Francium 內建了基準測試工具：

```bash
# 執行完整基準測試
francium benchmark

# 只測試加載速度
francium benchmark --load

# 只測試記憶體
francium benchmark --memory

# 測試不同執行緒數
francium benchmark --threads 4
```

---

### 基準測試結果範例

**測試環境：**
- CPU: AMD Ryzen 7 5800X（8 核心 16 執行緒）
- 記憶體: 32 GB DDR4 3600 MHz
- 儲存: Samsung 980 Pro NVMe SSD
- Java: OpenJDK 21

**測試結果（100 個模組）：**

| 指標 | 循序加載 | Francium 並行 | 提升 |
|------|---------|-------------|------|
| 加載時間 | 125 秒 | 18 秒 | **6.9x** |
| 峰值記憶體 | 2.8 GB | 3.1 GB | +11% |
| CPU 峰值 | 15% | 75% | 並行利用 |

---

### 如何比較

如果你想和其他加載器比較，可以使用以下方法：

1. **相同的模組集合**
2. **相同的硬體環境**
3. **相同的 Java 版本和參數**
4. **多次測試取平均值**

---

## 🎯 快速優化清單

如果你只想快速優化，按照這個清單來：

### ✅ 必做

1. [ ] 確保 `parallelLoading = true`
2. [ ] 使用 SSD 儲存 Minecraft 和模組
3. [ ] 合理設置記憶體（`-Xmx4G` 或更多）
4. [ ] 移除不需要的模組

### ⭐ 建議做

5. [ ] 調整 `parallelThreads` 找到最佳值
6. [ ] 啟用 G1 GC
7. [ ] 啟用記憶體管理
8. [ ] 預生成 AI Bridge 適配器

### 🔧 進階優化

9. [ ] 調整物件池大小
10. [ ] 優化 JVM 參數
11. [ ] 禁用不必要的功能
12. [ ] 使用本地 Mapping 檔案

---

## ❓ 常見問題

### Q: 為什麼我的加載速度沒有明顯提升？

**可能原因：**
1. 模組之間的依賴關係太複雜，並行度不高
2. 硬碟速度太慢，成為瓶頸
3. CPU 核心數太少
4. 並行加載沒有正確啟用

**解決方法：**
- 執行 `francium graph --show-layers` 查看並行度
- 檢查硬碟速度
- 確保 `parallelLoading = true`

---

### Q: 增加記憶體就一定會變快嗎？

不一定。記憶體不足會變慢，但記憶體過多也不會更快，反而可能增加 GC 時間。

**建議：** 根據模組數量合理設置，參考 [記憶體優化](#-記憶體優化) 章節。

---

### Q: 為什麼有時候加載快，有時候慢？

可能的原因：
- 作業系統的檔案快取（第一次慢，後續快）
- 背景程式占用資源
- CPU 降頻（溫度過高或省電模式）
- 防毒軟體掃描

**建議：**
- 多次測試取平均值
- 關閉不必要的背景程式
- 確保電腦在高效能模式

---

### Q: AI Bridge 會影響效能嗎？

有一點影響，但通常很小：
- 第一次加載：需要生成適配器，會慢一些
- 後續加載：幾乎沒有影響
- 運行時：幾乎沒有影響（直接轉發呼叫）

如果很在意效能，可以預生成適配器或禁用 AI Bridge。

---

## 📚 更多資源

- 📖 [快速開始指南](QUICKSTART.md)
- 🛠️ [故障排除指南](TROUBLESHOOTING.md)
- 📖 [開發者指南](DEVELOPER_GUIDE.md)
- 🤖 [AI Bridge 技術細節](docs/ARCHITECTURE.md)

---

**祝你獲得最佳性能！🚀**

如果有其他性能優化的建議，歡迎提交 PR 或在社群分享！
