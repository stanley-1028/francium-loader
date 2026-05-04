# Francium 开发者指南（中文）

**构建能跨 Minecraft 版本存活的模组。是的，你没看错。**

---

## 目录

1. [五分钟光速上手](#五分钟光速上手)
2. [项目结构解剖](#项目结构解剖)
3. [模组清单 francium-mod.json](#模组清单)
4. [生命周期与入口点](#生命周期与入口点)
5. [依赖管理：让 SAT 替你打工](#依赖管理)
6. [AI 跨版本桥接：Francium 的杀手锏](#ai-跨版本桥接)
7. [Mixin 支持](#mixin-支持)
8. [API 参考](#api-参考)
9. [不启动 Minecraft 也能测试](#不启动-minecraft-也能测试)
10. [从 Fabric/Forge 迁移](#从-fabricforge-迁移)
11. [发布你的模组](#发布你的模组)
12. [疑难解答](#疑难解答)

---

## 五分钟光速上手

```bash
# 1. 克隆模板仓库
git clone https://github.com/stanley-1028/francium-loader.git
cp -r francium-loader/francium-mod-template ./my-mod
cd my-mod

# 2. 编辑 gradle.properties
#    填入你的 modId、modName、mcVersion、mainClass

# 3. 打开 ExampleMod.java，替换成你的代码

# 4. 构建
./gradlew build

# 5. 把 build/libs/my-mod-1.0.0.jar 丢进 Minecraft 的 mods/ 文件夹
#    启动游戏，享受 Francium 为你自动处理的一切
```

没有 Gradle 插件大战。没有 Loom。没有 ForgeGradle。就是一个标准 Java 项目，加一行 `compileOnly` 依赖。你甚至不需要装 Minecraft 就能测试——后面会讲。

---

## 项目结构解剖

```
my-mod/
├── build.gradle              # 标准 Java + Francium 依赖
├── settings.gradle           # rootProject.name
├── gradle.properties         # ← 在这里填你的元数据
├── src/
│   ├── main/java/            # 你的代码
│   ├── main/resources/
│   │   └── francium-mod.json # 自动从 gradle.properties 生成
│   └── test/java/            # JUnit 测试（不需要 Minecraft！）
└── .gitignore
```

**你不需要的东西（对比 Fabric/Forge 开发体验）：**

| 你不再需要的 | 以前为什么要 |
|---|---|
| Access Widener 文件 | 因为 Mojang 混淆名每版本都在变 |
| Mixin 配置文件 | 除非你真的需要 Mixin |
| 七层 Gradle 插件嵌套 | 因为 Loom/ForgeGradle 的依赖地狱 |
| 手动管理混淆映射表 | Francium 替你搞定了 |
| 在 Discord 里问「为什么编译不过」 | SAT 求解器精确告诉你哪个模组冲突 |

说白了：**以前你要折腾半天才能开始写代码的事情，现在一行命令搞定。剩下的时间，去写你真正想写的模组逻辑。**

---

## 模组清单

Francium 兼容三种清单格式，无缝读取：

| 格式 | 文件名 | 兼容度 |
|---|---|---|
| Francium 原生 | `francium-mod.json` | ✅ 完整 |
| Fabric | `fabric.mod.json` | ✅ 完整（含 Mixin） |
| Forge/NeoForge | `META-INF/mods.toml` | ⚠️ 部分 |

### francium-mod.json（推荐）

```json
{
  "schemaVersion": 1,
  "id": "my-mod",
  "name": "我的超棒模组",
  "version": "1.0.0",
  "description": "一句话说清楚你的模组干什么。不要写「一个 Minecraft 模组」——这不是废话文学大赛。",
  "authors": ["你的名字"],
  "license": "MIT",
  "mcVersion": "1.21",
  "mainClass": "com.yourname.YourMod",
  "dependencies": {
    "fabric-api": ">=0.100.0"
  },
  "optionalDependencies": {
    "sodium": ">=0.5.0"
  },
  "entrypoints": {
    "main": "com.yourname.YourMod::onInitialize",
    "client": "com.yourname.YourMod::onInitializeClient"
  },
  "aiBridgeEnabled": true,
  "mixins": ["my-mod.mixins.json"]
}
```

**字段说明：**

| 字段 | 必填 | 说明 |
|---|---|---|
| `id` | ✅ | 唯一标识符。小写，连字符分隔。请像 `sodium` 一样克制，别像 `SuperAwesomeEpicModFinal_v2_real` 一样放飞。 |
| `name` | ✅ | 人类可读的名字 |
| `version` | ✅ | 语义化版本（1.0.0） |
| `mcVersion` | ✅ | 目标 Minecraft 版本 |
| `mainClass` | ✅ | 入口类，需要有 `onInitialize()` 方法 |
| `dependencies` | - | 硬依赖。找不到 = 加载失败 |
| `optionalDependencies` | - | 软依赖。找不到 = 警告但不崩 |
| `entrypoints` | ✅ | 生命周期钩子（见下文） |
| `aiBridgeEnabled` | - | `true` = Francium 自动适配新版 MC，你不改代码就能在 1.22 上跑 |
| `mixins` | - | Mixin 配置文件（Fabric 风格） |

---

## 生命周期与入口点

Francium 有五阶段生命周期。你的代码在 LOADING 阶段运行：

```
INIT → DISCOVERING → RESOLVING → BRIDGING → LOADING → READY
                                                     ↑
                                               你的代码在这里
```

### 入口方法

你的主类可以实现这些方法：

```java
public class YourMod {
    // LOADING 阶段调用，在游戏渲染之前
    public void onInitialize() {
        // 注册方块、物品、实体等
        Registry.register(Blocks.MY_BLOCK);
        System.out.println("我的模组已就绪，将军！");
    }

    // 客户端渲染就绪后调用
    public void onInitializeClient() {
        // 注册渲染器、快捷键、HUD元素
    }

    // 可选：服务端初始化
    public void onInitializeServer() {
        // 注册命令、服务端 tick 处理器
    }
}
```

**不需要 `@Mod` 注解。** Francium 通过 `francium-mod.json` → `mainClass` → 反射找到你的类。你只管写代码，它帮你找路。

### 扩展点（进阶）

如果需要钩入 Francium 本身：

```java
FranciumLoader loader = FranciumLoader.getInstance();

loader.onPreLaunch(() -> {
    // 所有模组加载完毕，游戏即将开始
});

loader.onModLoaded("sodium", () -> {
    // 特定模组加载完毕后的回调
    // 如果 sodium 没装或者炸了，这个回调不会被触发
});
```

---

## 依赖管理

在 `francium-mod.json` 中声明依赖：

```json
"dependencies": {
  "sodium": ">=0.5.0 <1.0.0",   // 范围约束：0.5.0 以上，1.0 以下
  "iris": "^1.7.0",               // 脱字符：1.x.x 任意版本
  "fabric-api": ">=0.90.0"        // 最低版本
}
```

Francium 使用 **SAT 求解器**而不是「试试看，崩了再说」策略。当依赖冲突时：

| 加载器 | 遇到冲突的表现 |
|---|---|
| Forge/Fabric | `NoClassDefFoundError`，然后你对着日志发呆十分钟 |
| Francium | *「sodium 需要 fabric-api >=0.90，iris 需要 fabric-api >=0.85。已选择 fabric-api 0.92，同时满足两者。」* |

如果真无解，Francium 会精确告诉你**哪几个模组在打架、为什么打、建议怎么办**，而不是甩一个神秘的 stack trace。

---

## AI 跨版本桥接

**这是 Francium 最有杀伤力的功能。** 当 `aiBridgeEnabled: true` 时：

1. Francium 用 ASM 分析你模组的字节码
2. 提取所有 Minecraft API 调用
3. 与目标 MC 版本的映射表比对
4. 如果方法名变了，Francium 自动生成**桥接适配器**
5. 你的模组在新 MC 版本上运行，原封不动

**实例：**

```
你的模组调用：Block.m_12345_()      (1.20.4 混淆名)
目标版本：    Block.getBlockState()  (1.21 映射名)
Francium：    "交给我。" → 生成适配器 → 你的模组正常运行
```

> 换个角度理解：Francium 的 AI 桥接就像你雇了一个**懂所有 Minecraft 版本的翻译官**。你的模组说「我要调用那个获取方塊状态的方法」，翻译官看一眼当前版本说「在 1.21 里它叫 `getBlockState`，我帮你转过去」。

**信心等级：**
- ≥ 0.95：精确映射，100% 可靠
- 0.85–0.95：结构匹配，高置信度
- 0.60–0.85：结构搜索，可能需要验证
- < 0.60：标记为需要人工审查

在 `config/francium/loader.toml` 中设置 `aiConfidenceThreshold` 来控制触发阈值。

---

## Mixin 支持

Francium 支持 Fabric 风格的 Mixin。在 `francium-mod.json` 中添加：

```json
{
  "mixins": ["my-mod.mixins.json"]
}
```

你的 `my-mod.mixins.json`：

```json
{
  "required": true,
  "package": "com.yourname.mixin",
  "compatibilityLevel": "JAVA_17",
  "mixins": ["MixinLivingEntity"],
  "client": ["MixinTitleScreen"]
}
```

Francium 在 BRIDGING 阶段加载 Mixin，在 AI 分析之后、类加载之前。这意味着 AI 桥接可以和 Mixin 共存。

---

## API 参考

### FranciumLoader（核心）

```java
// 获取加载器实例
FranciumLoader loader = FranciumLoader.getInstance();

// 检查当前状态
LoaderState state = loader.state();

// 获取已加载模组数量
int count = loader.getLoadedModCount();

// 获取各阶段耗时
Map<String, Long> timings = loader.phaseTimings();
// 输出：{discovery: 45ms, resolution: 12ms, bridging: 230ms, loading: 340ms}

// 获取内存快照
MemorySnapshot mem = loader.getMemorySnapshot();
System.out.println("堆内存使用：" + mem.heapUsed() / 1024 / 1024 + "MB");
```

### ModGraph（DAG）

```java
ModGraph graph = loader.modGraph();
List<Set<String>> layers = graph.getLayers();
double speedup = graph.getSpeedupRatio();  // 例如 4.2x
```

### PackageManager（CLI）

```java
PackageManager pm = new PackageManager(modsDir, cacheDir);

// 安装模组
InstallReport report = pm.install("sodium", "^0.5.0");

// 搜索模组
List<RegistryMod> results = pm.search("shader");

// 检查更新
List<UpdateInfo> updates = pm.checkUpdates();
```

### VersionBridge（AI）

```java
VersionBridge bridge = new VersionBridge("1.20.4", "1.21");
BridgeReport report = bridge.analyze(Path.of("mods/my-mod.jar"));
System.out.println("兼容性：" + (report.compatibilityScore * 100) + "%");
```

---

## 不启动 Minecraft 也能测试

Francium 的设计哲学：**模组逻辑应当独立于游戏引擎可测试。**

```java
class ExampleModTest {
    @Test
    void testModInitialization() {
        ExampleMod mod = new ExampleMod();
        mod.onInitialize();
        // 断言你的注册逻辑
    }

    @Test
    void testYourGameLogic() {
        // 纯逻辑测试，没有 Minecraft 依赖
        assertEquals(64, new ItemStack(Items.DIRT, 64).getCount());
    }
}
```

运行：`./gradlew test`

> 说实话，能在 CI 里跑模组测试而不是「本地能跑就行了」，这本身就是一种革命。

---

## 从 Fabric/Forge 迁移

### 从 Fabric 迁移

1. 保留你的 `fabric.mod.json`（Francium 原生支持）
2. 把 `fabric-loader` 依赖换成 `francium-loader`
3. 从 `build.gradle` 中移除 Fabric Loom
4. 设置 `aiBridgeEnabled: true` 获取跨版本支持
5. `./gradlew build`

你的 Mixin 配置、入口点、注解全部原封不动。

### 从 Forge 迁移

1. 在 `mods.toml` 旁边新建 `francium-mod.json`
2. 把 Forge 事件总线调用替换成 Francium 入口点
3. 用 `mainClass` 字段替代 `@Mod` 注解
4. 把 Forge 特有 API 移植到 Mojang 映射（或开启 AI 桥接）
5. `./gradlew build`

### 兼容性一览

| 特性 | Fabric → Francium | Forge → Francium |
|---|---|---|
| 入口点 | ✅ 自动 | ⚠️ 手动移植 |
| Mixin | ✅ 完整支持 | ❌ 建议用 ASM 转换器 |
| 注册系统 | ✅ 标准 | ⚠️ 替换为原版 Registry |
| 网络 | ✅ 标准 | ⚠️ 替换为原版数据包 |
| 事件 | ✅ Fabric API 事件 | ⚠️ 降级为原版事件 |
| Capabilities | ❌ 不适用 | ❌ 不支持 |

---

## 发布你的模组

### 方式一：GitHub Releases（推荐）

```bash
./gradlew build
# 把 build/libs/my-mod-1.0.0.jar 上传到 GitHub Releases
```

### 方式二：JitPack

在你的 `build.gradle` 中添加：

```groovy
publishing {
    publications {
        maven(MavenPublication) {
            groupId = 'com.yourname'
            artifactId = 'my-mod'
            version = project.version
            from components.java
        }
    }
}
```

然后在 GitHub 上打 tag，JitPack 自动构建发布。

### 方式三：Francium Registry（即将上线）

未来你可以直接 `francium publish` 推到官方 registry。

---

## 疑难解答

### 「NoClassDefFoundError: com.francium.loader.FranciumLoader」

确保 francium-loader 是 `compileOnly`，不是 `implementation`。Francium 在运行时由加载器本身提供。

### 「我的 Fabric 模组在 Francium 上跑不起来」

Francium 读取 `fabric.mod.json` 但不实现完整的 Fabric API。请把 Fabric API 作为依赖，或者只使用原版 API 调用。

### 「AI 桥接说我模组只有 40% 兼容」

意味着你 40% 的 API 调用有精确映射，剩下 60% 需要结构搜索或人工审查。在 loader.toml 中开启 `aiBridgeReportOnly: true` 可以看到详细报告。

### 「SAT 求解器跑太久了」

罕见。如果你的模组依赖特别复杂（200+ 模组且有大量冲突约束），增大 loader.toml 中的 `layerTimeoutSeconds`。

### 「模组加载了但没反应」

检查 `francium-mod.json` 中 `entrypoints.main` 是否指向 `com.your.Class::onInitialize`（冒号后面的方法名必须写全）。

### 「我不要 AI 桥接，我想自己处理兼容性」

完全OK。把 `aiBridgeEnabled` 设为 `false`，Francium 就不会动你的字节码。不想用时关掉，需要时再打开。Francium 尊重你的选择。

---

*还有问题？去 [github.com/stanley-1028/francium-loader](https://github.com/stanley-1028/francium-loader) 提 issue。Francium 社区随时欢迎你，不问你「搜过了没」。*

*记住——「不要手动管理模组了，让 Francium 替你打工。」因为你的时间应该花在写酷模组上，而不是跟 Gradle 插件斗智斗勇。*
