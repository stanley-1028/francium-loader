# 🧠 Francium AI Bridge — 跨版本兼容性演示

**「你的模组不必重写，Francium 替你桥接未来。」**

这是一个**完全独立、可直接运行**的 Java 程序，演示 Francium 的核心杀手功能：AI 跨版本字节码桥接。

不需要 Minecraft、不需要 Gradle、不需要编译模组。单个 `java` 命令。

---

## 5 秒跑起来

```bash
cd examples/ai-bridge-demo
java AIBridgeDemo.java
```

输出会展示一个完整流程：

```
Phase 1: 字节码分析 (ASM)
  → 从模组 JAR 中提取所有 Minecraft API 调用

Phase 2: 多版本映射查询 (MappingDatabase)
  → 查找每个 API 调用在目标 MC 版本中的对应

Phase 3: 兼容性预测 (CompatibilityPredictor)
  → 按特征相似度排序，逐个标注置信度

Phase 4: 适配器生成 (AdapterGenerator)
  → 自动生成桥接字节码，注入 ClassLoader

Phase 5: 最终报告
  → 综合兼容性评分 + 运行建议
```

## 它演示了什么

模拟一个**为 MC 1.20.4 编写的模组 "DiamondFinder"**，想在 MC 1.21 上运行：

| MC 版本 | Block.getBlockState 的混淆名 |
|---------|------------------------------|
| 1.20.4  | `m_12345_()`                 |
| 1.21    | `m_99999_()`                 |
| 1.22    | `m_ABCDE_()`                 |

Mojang 每个版本都重新混淆，但语义不变。Francium 的 AI 桥接引擎理解这种语义，自动找到对应关系。

你甚至可以看到**跨两个大版本**（1.20.4 → 1.22）的效果。

## 核心原理（一句话版）

```
你的模组 → ASM 提取 API 调用 → Mapping 数据库找对应 → 
特征相似度排序 → 生成适配器字节码 → 注入 ClassLoader → 
你的模组在新版本上运行，原封不动
```

## 真实 Francium 的额外能力

这个 Demo 是纯 Java 模拟。真实的 `francium-ai-bridge` 模块还会：

- **真正的 ASM 字节码解析**：直接读取 `.class` 文件，不是模拟
- **Mojang → Yarn → SRG 三层映射**：覆盖所有混淆方案
- **ML 加权预测器**：基于历史成功率的自适应模型
- **运行时字节码注入**：动态生成并加载适配器类
- **增量缓存**：分析结果持久化，第二次加载秒过

## 文件

| 文件 | 说明 |
|------|------|
| `AIBridgeDemo.java` | 独立可运行的演示程序 |
| `run.sh` | Linux/macOS 一键运行 |
| `run.bat` | Windows 一键运行 |

## 在真实项目中

```java
// 在你的模组 francium-mod.json 中：
{
    "aiBridgeEnabled": true    // ← 就这一行
}

// Francium 在加载时自动完成所有桥接。
// 你不需要写任何适配代码。
```

---

*「不要等到模组更新，让 AI 替你桥接未来。」*
