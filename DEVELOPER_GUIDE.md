# Francium Mod Loader — 开发者指南

> 从零开始写一个 Francium 模组。15 分钟上手。

---

## 快速开始

### 1. 创建项目

```
my-awesome-mod/
├── src/main/java/com/example/MyMod.java
├── src/main/resources/francium-mod.json
└── build.gradle
```

### 2. 编写 mod 入口

```java
package com.example;

import com.francium.loader.FranciumLoader;

public class MyMod {
    public static void onPreLaunch(FranciumLoader loader) {
        System.out.println("[MyMod] Francium 正在启动！");
    }

    public static void onLaunch(FranciumLoader loader) {
        System.out.println("[MyMod] 所有模组已加载，准备进入游戏！");
    }
}
```

### 3. 创建 francium-mod.json

```json
{
  "modId": "my-awesome-mod",
  "version": "1.0.0",
  "name": "My Awesome Mod",
  "description": "一个很棒的 Francium 模组",
  "mainClass": "com.example.MyMod",
  "authors": ["你的名字"],
  "mcVersionRange": ["1.20.4", "1.21"],
  "aiBridgeEnabled": true,
  "dependencies": {},
  "entryPointType": "launch"
}
```

### 4. 构建 & 安装

```bash
./gradlew build
cp build/libs/my-awesome-mod-1.0.0.jar ~/.minecraft/mods/
```

然后使用 Nova Launcher 启动 Minecraft——Francium 会自动发现并加载你的模组。

---

## francium-mod.json 完整参考

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `modId` | string | ✅ | 唯一标识符，只能用小写字母和连字符 |
| `version` | string | ✅ | 语义化版本 (SemVer)，如 `1.0.0` |
| `name` | string | ✅ | 显示名称 |
| `mainClass` | string | ✅ | 入口类全限定名 |
| `description` | string | | 模组描述 |
| `authors` | string[] | | 作者列表 |
| `mcVersionRange` | [min, max] | | 支持的 MC 版本范围 |
| `dependencies` | object | | 依赖: `{ "modId": "版本约束" }` |
| `optionalDependencies` | object | | 可选依赖 |
| `conflicts` | object | | 冲突声明 |
| `aiBridgeEnabled` | boolean | | 是否允许 AI 自动跨版本适配，默认 `true` |
| `loadPriority` | int | | 加载优先级，负数=提前，正数=延后 |
| `entryPointType` | string | | `"preLaunch"` / `"launch"` / `"postLaunch"` |
| `mixinConfigs` | string[] | | Mixin 配置文件路径 |

### 依赖版本约束

```
"fabric-api": ">=0.90.0"           // 最低版本
"sodium": ">=1.2.0 <2.0.0"        // 范围
"iris": "^1.6.0"                   // 兼容版本 (>=1.6.0 <2.0.0)
"lithium": "~0.12.0"               // 补丁兼容 (>=0.12.0 <0.13.0)
```

---

## 生命周期

Francium 按以下顺序触发模组入口：

```
INIT → DISCOVERING → RESOLVING → BRIDGING → LOADING → READY
                                                      ↓
                                              preLaunch 回调
                                                      ↓
                                              进入 Minecraft
                                                      ↓
                                              postLaunch 回调
```

### 入口类型

| entryPointType | 触发时机 | 适用场景 |
|----------------|----------|----------|
| `"preLaunch"` | 所有模组加载完毕后，进入游戏前 | 初始化数据、注册监听器 |
| `"launch"` | 与 preLaunch 同级（默认） | 大多数模组逻辑 |
| `"postLaunch"` | 游戏主菜单加载后 | UI 注入、延迟初始化 |

---

## 进阶特性

### AI 版本桥接

设置 `aiBridgeEnabled: true` 后，Francium 会自动分析你的模组字节码，在 Minecraft 版本更新时自动适配 API 变化。

```
你的模组 (MC 1.20.4)           →    Francium AI Bridge    →    MC 1.21
  调用 Block.m_12345_()               自动映射                      Block.getBlockState()
                                      生成适配器字节码
```

**最佳实践：**
- 尽可能使用 Mojang Mappings 减少混淆名依赖
- 在 `mcVersionRange` 中声明你测试过的版本范围
- 设置 `aiBridgeEnabled: false` 如果你的模组用了大量底层反射

### SAT 依赖解析

Francium 把模组依赖视为一个 [约束满足问题](https://zh.wikipedia.org/wiki/约束满足问题)，使用 DPLL 回溯算法自动求解。

你只需声明依赖，Francium 会：
1. 自动检测版本冲突
2. 尝试所有可能的版本组合
3. 精确报告哪两个模组冲突（不再靠猜）

```
[Francium] Dependencies resolved for 47 mods (12ms, 89 nodes explored)
```

### DAG 并行加载

模组按依赖关系分层，同层模组并行加载。100 个模组的加载时间从 2-3 分钟降到 20-30 秒。

```
Layer 0: [A, B]      ← 无依赖，并行
Layer 1: [C, D, E]   ← 依赖 Layer 0，并行
Layer 2: [F]         ← 依赖 Layer 1
```

### 内存管理

Francium 自动监控每个模组的内存占用：

```java
FranciumLoader loader = /* ... */;
MemoryManager.MemorySnapshot snap = loader.getMemorySnapshot();
System.out.println("总内存: " + snap.totalMB + "MB");
System.out.println("泄露风险: " + snap.leakRiskMods);
```

---

## 示例模组

### Hello World

```java
package com.example.helloworld;

import com.francium.loader.FranciumLoader;

public class HelloWorldMod {
    public static void onLaunch(FranciumLoader loader) {
        System.out.println("Hello from Francium mod!");
        System.out.println("加载了 " + loader.getLoadedModCount() + " 个模组");

        // 查看加载性能
        var timings = loader.phaseTimings();
        System.out.println("发现阶段: " + timings.get("discovery") + "ms");
        System.out.println("加载阶段: " + timings.get("loading") + "ms");
    }
}
```

### 带依赖的模组

```json
{
  "modId": "my-addon",
  "version": "1.0.0",
  "name": "My Addon",
  "mainClass": "com.example.MyAddon",
  "dependencies": {
    "sodium": ">=1.0.0",
    "my-awesome-mod": ">=1.0.0"
  }
}
```

### 模组间通信

```java
// Mod A: 暴露 API
public class ModA {
    public static final String API_VERSION = "1.0";

    public static void doSomething() {
        System.out.println("Mod A is doing something!");
    }
}

// Mod B: 调用 Mod A
public class ModB {
    public static void onLaunch(FranciumLoader loader) {
        // 通过反射或直接引用检查 Mod A 是否加载
        try {
            Class<?> modA = Class.forName("com.example.ModA");
            modA.getMethod("doSomething").invoke(null);
        } catch (Exception e) {
            System.err.println("Mod A 未加载!");
        }
    }
}
```

---

## 配置参考 (config/francium/loader.toml)

```toml
# 并行加载
maxParallelMods = 8            # 同时加载的模组数
layerTimeoutSeconds = 120      # 单层超时

# AI 桥接
aiBridgeEnabled = true         # 启用 AI 版本适配
aiConfidenceThreshold = 0.85   # 最低置信度 (0.0-1.0)
aiBridgeReportOnly = false     # 仅报告不修改（调试用）

# 内存管理
memoryLeakDetection = true     # 内存泄漏检测
memoryWarningThresholdMB = 512 # 告警阈值
aggressiveGC = false           # 激进 GC（低配电脑推荐开启）

# 安全
modValidation = "integrity"    # none / basic / integrity / strict
allowUnsignedMods = true       # 允许未签名模组
```

---

## 从 Fabric/Forge 迁移

### Fabric → Francium

1. `fabric.mod.json` 重命名为 `francium-mod.json`
2. `entrypoints.main` 映射到 `mainClass`
3. 依赖格式完全兼容
4. Mixin 配置保持不变

### Forge → Francium

1. 创建 `francium-mod.json`
2. 从 `mods.toml` 中提取 `modId`、`version`、`displayName`
3. `@Mod` 注解改为实现 Francium 回调
4. Forge 事件系统需手动适配

---

## 发布你的模组

```
1. ./gradlew build
2. 上传到 GitHub Releases
3. (即将支持) `francium publish` 一键发布到 Francium Registry
```

---

*"不要等待模组更新，让 AI 替你桥接未来。"*
