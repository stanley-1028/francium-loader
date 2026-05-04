# francium CLI 使用指南

**「不要再手动管理模组了，让 Francium 替你打工。」**

---

## 这是什么？

`francium` 是 Francium Mod Loader 自带的面板线套件管理器。设计哲学：**像 npm 一样管理你的 Minecraft mod**。

如果你用过 `npm install react`，那你已经会用了：
```
francium install sodium    ← 就像 npm install react
francium search shader     ← 就像 npm search express
francium update            ← 就像 npm update
```

---

## 安装

Francium CLI 随 Francium Mod Loader 一起分发。安装 Francium 后即可使用：

```bash
# 确认安装成功
francium --version
# Francium Mod Loader v1.0.0-alpha

# 查看帮助
francium --help
```

---

## 常用命令速查

| 命令 | 作用 | 类比 npm |
|------|------|-----------|
| `francium install <mod>` | 安装模组（含所有传递依赖） | `npm install <pkg>` |
| `francium install` | 从 lock 文件恢复安装 | `npm ci` |
| `francium search <keyword>` | 搜索模组 | `npm search` |
| `francium update` | 检查所有模组更新 | `npm outdated` |
| `francium update <mod>` | 更新指定模组 | `npm update <pkg>` |
| `francium remove <mod>` | 卸载模组 | `npm uninstall <pkg>` |
| `francium list` | 列出已安装模组 | `npm ls --depth=0` |
| `francium info <mod>` | 查看模组详情 | `npm info <pkg>` |
| `francium publish` | 发布你的模组到 registry | `npm publish` |
| `francium sync` | 同步服务器模组清单 | — |

---

## 命令详解

### `francium install` — 安装模组

```bash
# 安装最新版
francium install sodium

# 指定版本
francium install sodium@0.5.0

# 使用 SemVer 约束
francium install "sodium@>=0.5.0 <1.0.0"
francium install "iris@^1.7.0"          # 脱字符：1.x.x 兼容
francium install "fabric-api@~0.100.0"   # 波浪号：0.100.x 兼容
```

**安装过程中 Francium 做的事：**

1. 查询 registry，找到符合条件的版本
2. SAT 求解器递归解析所有传递依赖
3. 自动下载所有需要的 jar
4. SHA256 校验（如果 registry 提供了）
5. 生成 `francium-lock.json` 锁定版本
6. 输出安装报告

**输出示例：**
```
✓ Installed sodium@0.5.1 (4 mods)
  Downloads: sodium-0.5.1.jar, fabric-api-0.100.1.jar, ...
  SAT resolve: 12ms | Backtracks: 0
▸ sodium         0.5.1   ─── 渲染引擎
▸ fabric-api     0.100.1 ─── 核心 API
▸ indium         1.0.30  ─── Fabric 渲染兼容
▸ iris           1.7.2   ─── 光影加载器
```

---

### `francium install`（无参数）— 从 lock 文件恢复

```bash
# 类似 npm ci，严格按 francium-lock.json 恢复
francium install

# 输出：
✓ Restored 42 mods from lock file (3.2s)
  All SHA256 checks passed.
```

这在你 clone 了朋友的整合包仓库后特别有用。不需要手动整理 mod 清單，一条命令全部到位。

---

### `francium search` — 搜索模组

```bash
francium search shader

# 结果：
Found 12 mods matching "shader":
▸ iris           1.7.2     ⭐ 45.2K   现代光影引擎
▸ oculus         1.7.3     ⭐ 38.1K   Iris 的 Forge 移植
▸ canvas         2.1.0     ⭐ 12.4K   轻量级渲染器
▸ shadercore     0.3.1     ⭐ 5.2K    光影核心库
...
```

结果按下载量排序，每个结果包含最新版本和简介。

---

### `francium update` — 检查更新

```bash
# 检查所有模组
francium update

# 输出：
Checked 42 mods. 3 updates available:
▸ sodium        0.5.1 → 0.5.2  [Minor: bug fixes]
▸ iris          1.7.2 → 1.8.0  [Major: new features]
▸ fabric-api    0.100.1 → 0.102.0 [Minor]
```

```bash
# 更新指定模组
francium update sodium
# ✓ Updated sodium: 0.5.1 → 0.5.2

# 更新全部
francium update --all
```

---

### `francium remove` — 卸载模组

```bash
francium remove sodium

# 输出：
✓ Removed sodium (0.5.2)
  Warning: iris 1.7.2 depends on sodium. 
  Run 'francium fix' to resolve orphan dependencies.
```

不会自动卸载被其他模组依赖的传递依赖。如果你想清理，运行 `francium fix`。

---

### `francium list` — 列出已安装

```bash
francium list

# 输出：
Installed mods (42):
  模组名             版本      类型
  ───────────────────────────────────────
  sodium           0.5.2    [渲染]
  iris             1.7.2    [光影]
  fabric-api       0.100.1  [API]
  lithium          0.12.1   [优化]
  create           0.5.1h   [机械]     
  ...
  ───────────────────────────────────────
  总计: 42 个模组 | 占 384MB
```

```bash
# 仅列出顶层（手动安装的）
francium list --depth=0

# JSON 格式输出（给 CI/脚本用）
francium list --json
```

---

### `francium info` — 模组详情

```bash
francium info sodium

# 输出：
▸ sodium 0.5.2
  Modern rendering engine for Minecraft
  Author: jellysquid3
  License: LGPL-3.0
  Downloads: 5,200,000+ 
  MC Versions: 1.19.4 ~ 1.21
  Registry: https://registry.francium.dev/v1/mod/sodium
  
  Available versions:
    0.5.2  (latest)   → MC 1.21
    0.5.1             → MC 1.20.4
    0.4.10            → MC 1.19.4
    0.3.2 (legacy)   → MC 1.18.2
  
  Dependencies: 
    fabric-api >= 0.100.0
    indium (optional)
```

---

### `francium sync` — 服务器同步

```bash
# 同步当前服务器（需要在服务器配置中设置）
francium sync

# 输出：
Connected to server: Hypixel-like SMP (mc.francium.dev:25565)
Comparing mod lists...
  ✓ matched:  38 mods
  ↓ download: 2 mods (twilightforest, biomesoplenty)
  ✗ extra:    1 mod (minimap — client-side only)
  
Download missing mods? [Y/n]: y
✓ Downloaded twilightforest-4.4.0.jar (23MB)
✓ Downloaded biomesoplenty-19.0.0.jar (18MB)
✓ SHA256 verified. Ready to join!
```

服务器同步的意义：再也不用在 Discord 里问「服务器要装什么 mod？」。Francium 在连接时自动比对你的 `mods/` 文件夹和服务器清单，精确告诉你少了什么、多了什么、版本对不对。

---

### `francium publish` — 发布你的模组

```bash
# 在模组项目目录下
cd my-francium-mod
francium publish

# 输出：
Publishing my-awesome-mod@1.0.0...
✓ Built: build/libs/my-awesome-mod-1.0.0.jar (156KB)
✓ Uploaded to Francium Registry
✓ Published at: https://registry.francium.dev/mod/my-awesome-mod
📢 Tweet this: "Just published my Francium mod! francium install my-awesome-mod"
```

**前置条件：**
- 已配置 `francium-mod.json`（填写完整的 metadata）
- 已配置 registry API key（`francium login`）
- 版本号尚未被注册

---

## 配置文件

### francium-lock.json

类似于 `package-lock.json` 或 `Cargo.lock`。记录每个模组的精确版本和下载源。

```json
{
  "version": 1,
  "generatedAt": 1712345678000,
  "mods": [
    {
      "modId": "sodium",
      "version": "0.5.2",
      "downloadUrl": "https://registry.francium.dev/v1/mod/sodium/0.5.2",
      "sha256": "a1b2c3..."
    }
  ]
}
```

**应该提交到 Git。** 这样你的团队和 CI 都能复现完全相同的模组环境。

### francium-config.toml

全局配置文件，位于 `~/.francium/config.toml`：

```toml
[registry]
url = "https://registry.francium.dev/v1"
# 备用 registry（类似 apt sources.list）
mirrors = ["https://registry.francium.cn/v1"]
cache_ttl_minutes = 60

[download]
max_concurrent = 4
timeout_seconds = 300
verify_sha256 = true

[install]
auto_resolve = true
auto_sync_server = true
```

---

## 高级用法

### 批量安装

```bash
# 从文件批量安装（每行一个 modId@version）
francium install --from modlist.txt

# modlist.txt:
# sodium@>=0.5.0
# iris@^1.7.0
# create
# fabric-api@~0.100.0
```

### 仅下载不安装

```bash
francium download sodium@0.5.2 --to ./mods/
```

### 离线安装（从本地缓存）

```bash
francium install sodium --offline
# 仅从本地缓存安装，不联网
```

### 检查依赖树

```bash
francium why iris
# iris@1.7.2 is required by:
# └─ my-awesome-mod@1.0.0 (hard dependency)
```

### 清理缓存

```bash
francium clean cache
# 清理 ~/.francium/pkg-cache/
```

---

## 与现有方案的对比

| 操作 | Forge/Fabric 现状 | Francium |
|------|-------------------|----------|
| 安装模组 | 打开浏览器 → CurseForge/Modrinth → 下载 → 拖进 mods/ | `francium install sodium` |
| 安装模组+依赖 | 报错 → 谷歌 → 下载 → 再报错 → 循环 | SAT 自动解析 |
| 查模组更新 | 一个个打开网页看 | `francium update` |
| 团队同步 | 「你少了 iris 1.7.2」→ 传文件 | `francium sync` |
| 换电脑 | 重新下载 87 个 mod | `francium install`（从 lock） |
| 发布模组 | GitHub Releases + 论坛发帖 | `francium publish` |

---

## FAQ

**Q: 可以从 CurseForge/Modrinth 安装模组吗？**
A: 正在开发中。目前 Fransium Registry 是主源，未来会支持多 registry（类似 apt 的多源）。

**Q: lock 文件冲突了怎么解决？**
A: 重新生成：删除 `francium-lock.json`，运行 `francium install`，SAT 求解器会重新计算最优依赖组合。

**Q: 我是模组开发者，如何把我的模组加入 Francium Registry？**
A: 运行 `francium publish` 自动发布，或者在 registry 网站上手动提交。

**Q: CLI 可以在没有 Minecraft 的时候用吗？**
A: 完全可以。`francium` 是独立工具，只管 mod 文件管理。游戏启动是 Francium Loader 的事。

---

*「生活已经够复杂了，安装 mod 不应该是。」*
