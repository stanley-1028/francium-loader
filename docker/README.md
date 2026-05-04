# 🐳 Francium Server — Docker 部署指南

**一条命令部署 Francium 模组同步服务器。**

---

## 五分钟部署

```bash
# 1. 进入 docker 目录
cd francium-loader/docker

# 2. 构建镜像（需要先构建 francium-server JAR）
cp ../francium-server/build/libs/francium-server-*.jar .

# 3. 启动
docker compose up -d

# 4. 验证
docker compose logs francium-server
# 看到 "Francium Server ready on 0.0.0.0:25566" 就成功了
```

---

## 镜像总览

| 项目 | 详情 |
|------|------|
| 基础镜像 | `eclipse-temurin:21-jre-alpine` (~200MB) |
| JVM | ZGC 低延迟 GC |
| 内存 | 256MB ~ 1GB（可配） |
| 用户 | 非 root（`francium` 用户） |
| 端口 | 25566 (mod sync) + 25567 (registry API) |

---

## 配置

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `FRANCIUM_DATA_DIR` | `/app/data` | 数据目录 |
| `FRANCIUM_CONFIG_DIR` | `/app/config` | 配置目录 |
| `FRANCIUM_MODS_DIR` | `/app/mods` | 模组目录 |
| `JAVA_OPTS` | `-Xms256M -Xmx1G -XX:+UseZGC` | JVM 参数 |

### 自定义配置

创建 `docker/config/server.toml`：

```toml
[server]
host = "0.0.0.0"
modSyncPort = 25566

[sync]
requireSignature = true
autoDownloadClientMods = true
syncIntervalMinutes = 60

[security]
verifySha256 = true
minimumSigningKeyBits = 256
allowUnsignedMods = false
```

---

## 常用命令

```bash
# 查看日志
docker compose logs -f francium-server

# 重启
docker compose restart

# 停止
docker compose down

# 清理（含数据卷）
docker compose down -v

# 进入容器
docker compose exec francium-server sh
```

---

## 部署到服务器

```bash
# 方式一：直接在服务器上 build
git clone https://github.com/stanley-1028/francium-loader.git
cd francium-loader/docker
docker compose up -d

# 方式二：推送到 registry 后 pull
docker tag francium-server:1.0.0-alpha your-registry/francium-server:latest
docker push your-registry/francium-server:latest
# 在服务器上 docker pull && docker compose up

# 方式三：单容器运行（不用 compose）
docker run -d \
  --name francium-server \
  -p 25566:25566 \
  -v francium-mods:/app/mods \
  -e "FRANCIUM_DATA_DIR=/app/data" \
  francium-server:1.0.0-alpha
```

---

## 客户端连接

在 Minecraft 的 Francium Loader 配置中：

```toml
# config/francium/loader.toml
serverSyncEnabled = true
syncServerAddress = "your-server:25566"
autoDownloadMods = true
```

或者用 CLI：

```bash
francium sync --server your-server:25566
```

---

## 健康检查

```bash
# 检查服务状态
curl http://localhost:25566/health

# 返回:
# {"status":"UP","mods":42,"players":0,"uptime":"2h 15m"}
```

---

## 性能调优

| 场景 | 建议 JAVA_OPTS |
|------|---------------|
| 小服 (≤10 玩家) | `-Xms128M -Xmx512M` |
| 中服 (10-50 玩家) | `-Xms256M -Xmx1G` |
| 大服 (50+ 玩家) | `-Xms512M -Xmx2G -XX:+UseZGC` |

---

*Docker 让你不用在服务器上配 Java 环境。一条 `docker compose up`，Francium 开箱即用。*
