# 🎬 Francium Mod Loader — 演示视频脚本

**目标时长**: 4-6 分钟（B站版）/ 2-3 分钟（YouTube Shorts/TikTok 版）
**目标观众**: MC 模组玩家、整合包作者、模组开发者
**风格**: 技术展示 + MC 梗 + 不废话

---

## 场景 0：片头（5 秒）

**画面**: Francium LOGO 渐入，背景是 Minecraft 像素方块随机掉落
**BGM**: 8-bit 电子乐，轻快有节奏感
**字幕**: 「你的模组，Francium 替你桥接未来。」

---

## 场景 1：痛点暴击（30 秒）

**画面**: 分屏对比

| 左边（现状） | 右边（Francium） |
|---|---|
| 玩家对着启动器发呆，Java 进度条卡在 47% | Francium 进度条飞速跑完 |
| 弹出 `NoClassDefFoundError`，红字刷屏 | 绿色 `✓ 42 mods loaded` |
| Discord 聊天：「少了啥前置？」「版本不对」「你 iris 太旧了」 | 终端输出：「SAT 求解器：sodium 0.5.2 + iris 1.7.2，完美兼容 ✅」 |

**旁白**: 「你知道 Minecraft 模组加载最痛苦的是什么吗？不是模组太少，是模组之间的架打不完。Forge 和 Fabric 打架，sodium 和 iris 打架，你和你的 sanity 也在打架。」

**字幕弹出**: 「😭 装个模组，半小时在 Google，五分钟在玩游戏」

---

## 场景 2：Francium 是什么（45 秒）

**画面**: 三张「技能卡」依次滑入

**技能卡 1 — AI 版本桥接**
- 动画：MC 1.20.4 → Francium → MC 1.21
- 字幕：「Mojang 改了方法名？Francium 自动找到对应，你连代码都不用碰。」

**技能卡 2 — SAT 依赖解析**  
- 动画：一堆模组图标乱飞，SAT 求解器（显示为电路板/逻辑门动画）把它们排成整齐的一列
- 字幕：「200 个模组的依赖关系，数学替你解。精确告诉你谁和谁在打架，而不是甩一个 stack trace。」

**技能卡 3 — DAG 并行加载**
- 动画：三个箭头同时向下，模组图标分层排列
- 字幕：「你的 CPU 有 16 个核。Forge 只用 1 个。Francium 全用。」

**旁白**: 「Francium 是一个全新的 Minecraft Mod Loader，三个核心创新：AI 自动桥接跨版本 API、SAT 求解器数学级精确解依赖、DAG 拓扑分层并行加载。翻译成人话：装模组不下 Google，MC 更新不用等作者，100 个模组 20 秒进游戏。」

---

## 场景 3：核心功能演示（90 秒）

### 3.1 AI 桥接演示（30 秒）

**画面**: 终端运行 `java AIBridgeDemo.java`
**重点镜头**: 推近到输出结果

```
Phase 3: 兼容性预测 (CompatibilityPredictor)

  API调用                      源混淆名→目标混淆名       置信度      方式
  ──────────────────────────────────────────────────────────────────────
  Block.getBlockState()        m_12345_ → m_99999_     95%      🧠 AI语义映射
  Entity.getPos()              m_67890_ → m_88888_     95%      🧠 AI语义映射
  World.setBlock()             m_11111_ → m_77777_     95%      🧠 AI语义映射
  ...

  综合兼容性: 100%  ✅ 几乎完美兼容
  是否可立即运行: ✅ 模组可直接在 MC 1.21 上运行！
```

**旁白**: 「这是一个为 MC 1.20.4 编写的钻石探测器模组。Mojang 在 1.21 把所有方法名全改了——`getBlockState` 变成了 `m_99999_`。Francium 自动分析字节码，找到语义对应，生成桥接适配器。模组代码一行不改，直接在新版本上跑。」

### 3.2 CLI 包管理演示（30 秒）

**画面**: 终端快速敲命令
```
$ francium search shader
▸ iris           1.7.2     ⭐ 45.2K   现代光影引擎
▸ oculus         1.7.3     ⭐ 38.1K   Iris 的 Forge 移植
▸ canvas         2.1.0     ⭐ 12.4K   轻量级渲染器

$ francium install sodium@^0.5.0
✓ Installed sodium@0.5.2 (4 mods)
  SAT resolve: 12ms | Backtracks: 0
▸ sodium         0.5.2   ─── 渲染引擎
▸ fabric-api     0.100.1 ─── 核心 API
▸ indium         1.0.30  ─── Fabric 渲染兼容
▸ iris           1.7.2   ─── 光影加载器
```

**旁白**: 「安装模组不用开浏览器、不用上 CurseForge、不用手动下载。跟 `npm install` 一样——一条命令，SAT 求解器自动解析所有传递依赖，SHA256 校验，搞定。」

### 3.3 并行加载对比（30 秒）

**画面**: 分屏对比
- 左：Forge/Fabric 进度条，数字一个一个往上跳
- 右：Francium 的 DAG 分层动画，每层多个模组同时加载
- 最终字幕弹出：

```
100 个模组加载耗时对比：
Forge:  ~2 分 30 秒
Fabric: ~1 分 45 秒
Francium: ~25 秒
加速比: 4.2x
```

**旁白**: 「并行的魔力。你的 CPU 不是单核的，凭啥加载器只用一个核？Francium 把模组依赖关系建模成 DAG，同一层并行加载。100 个模组的整合包，从等两分钟变成等二十秒。」

---

## 场景 4：模组开发者体验（45 秒）

**画面**: IDE 分屏，左边是 Fabric 项目的 build.gradle（200 行），右边是 Francium 项目的 build.gradle（15 行）

**镜头推近到 Francium 的 build.gradle**：
```groovy
plugins { id 'java' }
dependencies {
    compileOnly 'com.github.stanley-1028:francium-loader:1.0.0'
}
```

**旁白**: 「如果你是模组开发者——Fabric 的环境配置可能是你噩梦的开始。Loom、AccessWidener、Mixin 配置、Mappings 管理——在你写第一行模组代码之前，你已经跟 Gradle 打了半小时的架。」

**画面切换**: 终端运行
```bash
git clone https://github.com/stanley-1028/francium-loader.git
cp -r francium-loader/francium-mod-template ./my-mod
cd my-mod
# 编辑 ExampleMod.java...
./gradlew build
# 完成。丢进 mods/。
```

**旁白**: 「Francium 的开发体验：Fork 模板仓库，写 Java，build。没有 Gradle 插件大战，没有七层依赖嵌套。你写的是模组逻辑，不是构建配置。」

**画面**: 测试截图
```
$ ./gradlew test
BUILD SUCCESSFUL in 2s
70 tests completed, 70 passed ✅
```

**旁白**: 「而且你可以在 CI 里跑模组测试。不需要启动 Minecraft。JUnit 直接跑。」

---

## 场景 5：Docker 服务器部署（30 秒）

**画面**: 终端快速演示
```bash
git clone https://github.com/stanley-1028/francium-loader.git
cd francium-loader/docker
docker compose up -d

# 3 秒后...
✓ Francium Server ready on 0.0.0.0:25566
✓ Health check: {"status":"UP","mods":42}
```

**旁白**: 「架模组服的痛苦之一：每个人都在 Discord 问『要装什么模组』。Francium Server 一条 Docker 命令部署，客户端连上自动同步模组清单。SHA256 校验，Ed25519 签名。少了自动下，版本不对自动提示。」

---

## 场景 6：收尾 + CTA（30 秒）

**画面**: Francium LOGO + GitHub 链接居中
**字幕**:
```
⭐ GitHub: github.com/stanley-1028/francium-loader
📦 JitPack: com.github.stanley-1028:francium-loader:1.0.0-alpha
📖 开发者文档（中/英）: README 里有完整指南
```

**旁白**: 「Francium 目前是 1.0.0-alpha，核心测试 70/70 全部通过。MIT 开源，随便用随便改。如果你受够了手动管理模组依赖、受够了每次 MC 更新就挂一堆模组、受够了三分钟的启动等待——来试试 Francium。」

**画面**: 最后弹出 Francium tagline
> **「不要再手动管理模组了，让 Francium 替你打工。」**

---

## 场景 7：彩蛋（可选，10 秒）

**画面**: 终端输入 `francium install everything`
```
$ francium install everything
⚠️  SAT求解器: 检测到 847 个冲突约束
    这可能需要几分钟... 或者你可以重新考虑你的人生选择。
    (Just kidding. SAT solver found a solution in 1.2s. All 847 mods installed.)
✓ Installed 847 mods in 1.2s
  Your Minecraft now takes longer to load than Windows.
```

**字幕**: 「不要真的装 847 个模组。我们只是证明 SAT 求解器能处理。」

---

## 制作备注

### 所需素材
- Francium LOGO（已有像素风格）
- MC 游戏画面（启动过程、模组内容）
- 终端录屏（AI Bridge Demo、CLI 命令）
- 分屏对比动画（Forge vs Francium 加载速度）
- DAG 动画（拓扑分层可视化）

### BGM 建议
- 主 BGM：C418 风格的环境电子乐，不要直接用 Minecraft OST（版权）
- 场景切换：8-bit 音效（类似 Undertale 风格）
- 结尾：渐弱的钢琴

### 配色
- 背景：深空蓝黑（#0a0a1a）
- 主色：Francium 蓝（#4a9eff）
- 强调：霓虹紫（#c084fc）、像素绿（#4ade80）
- 终端：黑底绿字（经典 hacker 风格）

### B站特供版本
- 时长可延长到 6-8 分钟，加入更多技术细节
- 开投币/收藏引导（B站特有的互动话术）
- 弹幕互动：「在评论区告诉我你最想 Francium 支持的功能！」
- 加一条「学生开发者」叙事线（15 岁澳门独立开发者，展示架构图手稿等）

### YouTube Shorts / TikTok 版本
- 压缩到 60 秒以内
- 开头 3 秒钩子：`NoClassDefFoundError` 红字刷屏 + 玩家崩溃表情
- 快速切 3 个核心功能（各 15 秒）
- 结尾：GitHub 链接 + MIT 标签

### Reddit 版本
- 使用场景 1（痛点）+ 场景 3（演示）的剪辑
- 不需要旁白，用文字叠加
- 目标 subreddit：r/feedthebeast, r/Minecraft, r/programming

---

*「好的演示视频，像好的模组——不废话，直接展示。」*
