# Mapping 贡献指南

感谢你考虑为 Francium Mod Loader 贡献映射数据！映射数据库是 AI 版本桥接功能的核心，你的贡献将帮助更多模组跨版本运行。

## 目录

- [开始之前](#开始之前)
- [映射格式说明](#映射格式说明)
- [如何贡献映射](#如何贡献映射)
- [贡献工作流](#贡献工作流)
- [质量标准](#质量标准)
- [常见问题](#常见问题)

## 开始之前

### 你需要知道的

- Francium 使用 JSON 格式存储种子映射（seed mappings）
- 每个 Minecraft 版本有独立的映射文件
- 映射包含类名、方法名、方法描述符和混淆名
- 我们优先覆盖最常用的 Minecraft API

### 你不需要的

- 不需要 Java 编程经验
- 不需要深入了解 Minecraft 源码
- 不需要安装开发环境（虽然推荐）

## 映射格式说明

### 文件位置

映射文件位于 `francium-ai-bridge/src/main/resources/mappings/` 目录：

```
mappings/
├── seed-mappings-v1_20_4.json    # 1.20.4 种子映射
├── seed-mappings-v1_21.json      # 1.21 种子映射
└── cross-version-v1_20_4-to-v1_21.json  # 跨版本映射
```

### Seed Mapping 格式

```json
{
  "version": "1.20.4",
  "description": "Francium Seed Mappings — Minecraft 1.20.4",
  "classes": {
    "net/minecraft/world/level/block/Block": [
      {
        "name": "getDescriptionId",
        "desc": "()Ljava/lang/String;",
        "obf": "m_6057_"
      }
    ]
  }
}
```

#### 字段说明

| 字段 | 说明 | 示例 |
|------|------|------|
| `version` | Minecraft 版本号 | `"1.20.4"` |
| `description` | 映射文件描述 | `"Francium Seed Mappings"` |
| `classes` | 类映射对象 | 键为类名，值为方法数组 |
| `name` | 方法名（Mojang 映射） | `"getDescriptionId"` |
| `desc` | 方法描述符（ASM 格式） | `"()Ljava/lang/String;"` |
| `obf` | 混淆方法名 | `"m_6057_"` |

### 方法描述符格式

方法描述符遵循 JVM 规范（ASM 格式）：

| 类型 | 描述符 |
|------|--------|
| `void` | `V` |
| `boolean` | `Z` |
| `byte` | `B` |
| `char` | `C` |
| `short` | `S` |
| `int` | `I` |
| `long` | `J` |
| `float` | `F` |
| `double` | `D` |
| 对象 | `L全限定类名;` |
| 数组 | `[元素类型` |

**示例：**
- `void method()` → `()V`
- `int method(String s)` → `(Ljava/lang/String;)I`
- `String[] method(int[] arr)` → `([I)[Ljava/lang/String;`

### Cross-Version Mapping 格式

```json
{
  "version": "cross-version",
  "from": "1.20.4",
  "to": "1.21",
  "mappings": [
    {
      "from": "net/minecraft/world/level/block/Block#getDescriptionId:()Ljava/lang/String;",
      "to": "net/minecraft/world/level/block/Block#getDescriptionId:()Ljava/lang/String;"
    }
  ]
}
```

格式：`类名#方法名:描述符`

## 如何贡献映射

### 方式一：添加新的类映射

1. 确定你要添加的 Minecraft 类
2. 查找该类的所有公共方法
3. 在对应的 seed mapping 文件中添加类条目
4. 添加每个方法的 `name`、`desc` 和 `obf`

**示例：添加 Item 类**

```json
"net/minecraft/world/item/Item": [
  {
    "name": "getDescriptionId",
    "desc": "()Ljava/lang/String;",
    "obf": "m_41457_"
  },
  {
    "name": "getDefaultInstance",
    "desc": "()Lnet/minecraft/world/item/ItemStack;",
    "obf": "m_41458_"
  }
]
```

### 方式二：为现有类添加方法

1. 找到对应的类条目
2. 在方法数组中添加新方法
3. 确保方法按字母顺序排列（可选，但推荐）

### 方式三：添加跨版本映射

如果某个方法在不同版本中有不同的名称或描述符，需要添加跨版本映射：

```json
{
  "from": "net/minecraft/world/item/Item#oldMethodName:()V",
  "to": "net/minecraft/world/item/Item#newMethodName:()V"
}
```

**注意：** 如果方法名和描述符在两个版本中相同，通常不需要显式添加跨版本映射，AI Bridge 会自动匹配。

### 方式四：修正错误的映射

如果你发现某个映射有误（方法名、描述符或混淆名错误），请提交 PR 修正。

## 贡献工作流

### 1. Fork 仓库

访问 [https://github.com/stanley-1028/francium-loader](https://github.com/stanley-1028/francium-loader) 并点击 Fork。

### 2. 创建分支

```bash
git checkout -b mapping/add-block-methods
```

分支命名建议：
- `mapping/add-<类名>` - 添加新类
- `mapping/fix-<类名>` - 修复映射错误
- `mapping/update-<版本>` - 更新特定版本映射

### 3. 修改映射文件

使用你喜欢的文本编辑器编辑 JSON 文件。

### 4. 验证 JSON 格式

```bash
# 使用 Python 验证
python -m json.tool seed-mappings-v1_20_4.json > /dev/null

# 或者使用 jq
jq . seed-mappings-v1_20_4.json > /dev/null
```

### 5. 运行测试（可选但推荐）

如果你有 Java 开发环境，可以运行 MappingDatabase 测试：

```bash
./gradlew :francium-ai-bridge:test
```

### 6. 提交更改

```bash
git add francium-ai-bridge/src/main/resources/mappings/
git commit -m "mapping: add Item class methods

- add 15 methods for Item class
- include getDefaultInstance, getDescriptionId, etc.
- verified JSON format"
```

### 7. 提交 Pull Request

在 GitHub 上提交 PR，描述你的更改内容。

## 质量标准

为了确保映射质量，请遵循以下标准：

### ✅ 应该做的

- [ ] 只添加公共（public）方法
- [ ] 确保方法描述符准确
- [ ] 混淆名（obf）尽量准确
- [ ] 按字母顺序排列方法（可选）
- [ ] 添加有意义的 commit message
- [ ] 在 PR 描述中说明更改内容

### ❌ 不应该做的

- 不要添加私有（private）或保护（protected）方法
- 不要添加合成（synthetic）方法
- 不要猜测方法名或描述符
- 不要修改不相关的映射
- 不要删除现有的映射（除非确认错误）

### 优先级

我们优先接受以下映射贡献：

1. **常用 API 类**：Block, Item, Entity, Level, Player 等
2. **频繁使用的方法**：getters, setters, 常用操作方法
3. **模组开发常用类**：BlockEntity, Menu, Recipe 等

## 从哪里获取映射数据？

如果你不确定方法名或混淆名，可以参考以下来源：

### 1. Fabric Yarn Mappings
- 仓库：[FabricMC/yarn](https://github.com/FabricMC/yarn)
- 格式：`.tiny` 文件
- 包含：类名、方法名、字段名

### 2. Forge MCP / Official Mappings
- 官方映射：Mojang 提供的官方映射
- MCP：Mod Coder Pack

### 3. Minecraft 源码
- 使用反编译工具查看 Minecraft 源码
- 推荐：FernFlower, CFR, Procyon

### 4. Mod 源码
- 参考 popular mods 的源码
- 查看它们如何使用 Minecraft API

## 常见问题

### Q: 我需要添加 obf（混淆名）吗？

**A:** 推荐添加，但不是必须的。obf 字段用于混淆名匹配，如果没有，AI Bridge 仍然可以通过方法名和描述符进行匹配。

### Q: 我需要同时更新所有版本的映射吗？

**A:** 不需要。你可以只更新一个版本的映射。但如果你知道其他版本的对应方法，也欢迎一起添加。

### Q: 如何验证我的映射是否正确？

**A:** 
1. 运行 MappingDatabase 测试：`./gradlew :francium-ai-bridge:test`
2. 使用真实模组进行测试
3. 在 PR 中描述你是如何验证的

### Q: 我可以添加自己的模组使用的方法吗？

**A:** 当然可以！如果你的模组使用了某些不在映射数据库中的方法，欢迎贡献这些映射。这将帮助其他使用相同 API 的模组。

### Q: 映射会影响 AI Bridge 的准确率吗？

**A:** 是的！更多、更准确的映射会显著提高 AI 版本桥接的准确率。每个新增的方法都能帮助 AI Bridge 更好地理解代码结构。

### Q: 我贡献的映射会被如何使用？

**A:** 你的贡献将：
- 包含在 Francium 的下一个版本中
- 帮助所有使用 AI 版本桥接功能的用户
- 提高跨版本兼容性
- 被 MIT 许可证覆盖

## 获得帮助

如果你在贡献过程中遇到问题：

1. 查看 [GitHub Issues](https://github.com/stanley-1028/francium-loader/issues) 中是否有类似问题
2. 提交新的 Issue，标签使用 `mapping`
3. 在 Discord 社区中提问（如果有）

## 致谢

感谢所有为 Francium 映射数据库做出贡献的人！你的每一个贡献都在让 Minecraft 模组生态变得更好。

---

**最后更新：** 2026-06-24  
**版本：** v2.3.0
