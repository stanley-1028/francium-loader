# Francium Mod Loader — 下一代 Minecraft 模组加载器

> **不要让模组更新和依赖地狱浪费你的生命。让 Francium 替你打工。**

---

## 太长不看版

Francium 是一个全新的 Minecraft Mod Loader，解决三个困扰全体模组玩家的终极问题：

| 😭 你的痛苦 | 🧠 Francium 的方案 |
|---|---|
| 「MC 更新了，我的 87 个模组挂了 83 个」 | **AI 自动桥接**：Mojang 改了方法名？Francium 自动适配，你啥也不用改 |
| 「装个 Sodium，报错说缺 4 个前置，装了前置又说版本冲突」 | **SAT 求解器**：数学级精确解依赖，再也不猜谜 |
| 「200 个模组的整合包，开游戏先泡杯面」 | **DAG 并行加载**：100 个模组从 2 分钟降到 20 秒 |

---

## 这玩意儿和 Forge/Fabric 有什么区别？

Forge 和 Fabric 是伟大的项目，但它们的架构诞生于 2013 年。十年过去了，Minecraft 变了，模组生态变了，但加载器的核心逻辑还是那样。

Francium 从三个维度重新思考了「加载器应该怎么工作」：

### 1. AI 版本桥接 —— 模组不再绑定 MC 版本

**原理**：Minecraft 每个版本 Mojang 都会重混淆方法名（`getBlockState()` → `m_12345_()`），但方法的语义结构永远不变。Francium 用 ASM 分析你的模组字节码，自动在多版本 Mappings 中找到对应方法，生成桥接适配器。

**效果**：你为 1.20.4 写的模组，在 1.21 上可能 85-90% 的 API 调用都能自动适配。不需要你自己改代码，不需要等作者更新。

```
你的模组 (1.20.4):  调用 Block.m_12345_()
                            ↓ Francium AI Bridge 自动映射
MC 1.21:           路由到 Block.getBlockState()
                            ↓
你的模组正常运行 ✅
```

### 2. SAT 依赖解析 —— 让数学替你解依赖

**原理**：把模组依赖关系建模为「约束满足问题」(Constraint Satisfaction Problem)，用 DPLL 回溯算法自动求解。

**效果**：不再对着 `NoClassDefFoundError` 猜是哪个模组出了问题。SAT 求解器直接告诉你：

```
Fr Resolver: sodium 需要 fabric-api >=0.90
             iris 需要 fabric-api >=0.85
             选择了 fabric-api 0.92，两边都满足 ✅
```

如果真解不开，告诉你是哪两个模组打架、为什么打架、你能怎么办。

### 3. DAG 并行加载 —— 你的 CPU 不止一个核

**原理**：模组依赖关系自然形成有向无环图 (DAG)。同层模组互不依赖，用 ForkJoinPool 并行加载。

**效果**：
- 100 个模组：从 2-3 分钟降到 20-30 秒（实测 4.2x 加速）
- 加载失败的模组不阻塞同层其他模组
- 独立 ClassLoader 隔离每个模组，减少类冲突

---

## 还有啥？

| 功能 | 说明 | 类比 |
|---|---|---|
| `francium install sodium` | 命令行安装模组（自动解析所有传递依赖） | 像 `npm install react` |
| `francium update` | 一键检查所有模组更新 | 像 `npm outdated` |
| `francium sync` | 连接服务器时自动同步模组清单 | 再也不用在群聊问「要装什么模组」 |
| 内存泄漏检测 | 独立 ClassLoader + WeakReference 追踪 | 知道是哪个模组在吃内存 |
| 服务器同步协议 | SHA256 校验 + Ed25519 签名 | 模组文件不会被篡改 |
| Docker 一键部署 | `docker compose up -d` 部署模组同步服务器 | 服务器主不用手动配 Java 环境 |

---

## 兼容性

| 模组类型 | 支持程度 |
|---|---|
| Fabric 模组 | ✅ 完全支持（直接读 fabric.mod.json，支持 Mixin） |
| Forge 模组 | ⚠️ 部分支持（读 mods.toml，部分 API 需适配） |
| Francium 原生模组 | ✅ 完全支持 |
| Quilt 模组 | ✅ 基于 Fabric |

---

## 15 分钟写一个 Francium 模组

```bash
git clone https://github.com/stanley-1028/francium-loader.git
cp -r francium-loader/francium-mod-template ./my-awesome-mod
cd my-awesome-mod
```

然后改 `ExampleMod.java`：

```java
public class MyMod {
    public void onInitialize() {
        System.out.println("Hello Francium!");
        // 不需要 @Mod 注解，不需要 Gradle 插件套娃
    }
}
```

`./gradlew build`，丢进 `mods/`，完了。

---

## 当前状态

**1.0.0-alpha**。核心功能已完成，测试通过率 100%。正在征集早期测试者和模组开发者。

| 模块 | 测试 | 状态 |
|---|---|---|
| SAT 解析器 | 7/7 | ✅ |
| DAG 并行加载 | 21/21 | ✅ |
| AI 桥接 | 10/10 | ✅ |
| 版本约束 | 32/32 | ✅ |

---

## 开发这个故事

我是一个 15 岁的澳门开发者，从 0 写了这个项目。

了解 MC 模组加载器架构的人都懂，这不是「用 Spring Boot 写个 Todo List」的难度。SAT 求解器、DAG 拓扑排序、ASM 字节码分析、多版本 Mappings 数据库——这些东西通常在研究生课程里才出现。

我不是在炫耀。我是想说：**当一个高中生能独立做出这些的时候，说明 MC 社区的工具链确实有巨大的改进空间。** 现存的加载器在架构上欠了十年的技术债，Francium 试图从头还这笔债。

---

## 链接

- **GitHub**: [github.com/stanley-1028/francium-loader](https://github.com/stanley-1028/francium-loader)
- **许可证**: MIT（随便改，随便用，随便分发）
- **Issues**: 提 Bug、提建议、提 PR，全部欢迎

---

## FAQ

**Q: 真的免费吗？**
A: MIT 许可证。你甚至可以拿去卖钱，不欠我一分。

**Q: 和 Forge/Fabric 冲突吗？**
A: 不冲突。Francium 是独立加载器，你可以同时安装在 Minecraft 里（虽然没必要）。

**Q: 能跑在我的低配电脑上吗？**
A: 并行加载反而降低总启动时间。另外 Francium 的内存管理对低配机友好——支持激进 GC 模式和物件池。

**Q: AI 桥接要联网吗？**
A: 不要。Mapping 数据库在本地，AI 指的是本地特征匹配算法，不是调 OpenAI API。

**Q: 什么时候能正式用？**
A: 核心功能已经能跑。如果你愿意帮忙测试，现在就可以。去 GitHub 提 Issue 报告任何问题。

---

*「不要等待模组更新，让 AI 替你桥接未来。」*

*—— stanley1028，2026 年 5 月*
