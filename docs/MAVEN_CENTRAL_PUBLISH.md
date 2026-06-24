# Maven Central 发布指南

本文档介绍如何将 Francium Mod Loader 发布到 Maven Central 仓库。

## 前置条件

### 1. 注册 Maven Central 账号
- 访问 [https://central.sonatype.com/](https://central.sonatype.com/)
- 使用 GitHub 账号登录或注册新账号
- 验证你的邮箱地址

### 2. 验证命名空间
- 在 Maven Central Portal 中申请 `com.francium` 命名空间
- 按照提示完成域名验证（通过 DNS TXT 记录或 GitHub 仓库验证）
- 等待审核通过（通常 1-2 个工作日）

### 3. 生成部署令牌
- 登录 Maven Central Portal
- 进入 **Account → Deployment Tokens**
- 点击 **Generate Token**
- 保存生成的用户名和密码（只会显示一次）

### 4. 生成 GPG 签名密钥

```bash
# 生成新的 GPG 密钥对
gpg --gen-key

# 查看密钥 ID（最后 8 位十六进制数字）
gpg --list-secret-keys --keyid-format=SHORT

# 导出私钥（用于 Gradle 签名）
gpg --export-secret-keys -o ~/.gnupg/secring.gpg

# 将公钥上传到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

## 本地配置

### 1. 配置 Gradle 属性

将 `gradle.properties.example` 复制到 `~/.gradle/gradle.properties` 并填入你的凭据：

```properties
# Maven Central 部署令牌
mavenCentralUsername=your-deployment-token-username
mavenCentralPassword=your-deployment-token-password

# GPG 签名密钥
signing.keyId=YOUR_KEY_ID          # GPG 密钥 ID（最后 8 位）
signing.password=your-gpg-passphrase
signing.secretKeyRingFile=~/.gnupg/secring.gpg
```

### 2. 本地测试发布

```bash
# 发布到本地 Maven 仓库进行测试
./gradlew publishToMavenLocal

# 验证发布结果
ls -la ~/.m2/repository/com/francium/
```

## 手动发布到 Maven Central

### 发布所有模块

```bash
# 发布根项目
./gradlew publish

# 发布所有子项目
./gradlew :francium-api:publish
./gradlew :francium-core:publish
./gradlew :francium-resolver:publish
./gradlew :francium-manager:publish
./gradlew :francium-ai-bridge:publish
./gradlew :francium-profiler:publish
./gradlew :francium-server:publish
```

### 验证发布

1. 登录 [Maven Central Portal](https://central.sonatype.com/)
2. 进入 **Publishing → Publications**
3. 查看发布状态
4. 验证通过后，组件将自动同步到 Maven Central
5. 通常需要 10-30 分钟才能在 Maven Central 搜索到

## GitHub Actions 自动发布

项目已配置自动发布 workflow，在推送 tag 时自动触发。

### 配置 GitHub Secrets

在 GitHub 仓库的 **Settings → Secrets and variables → Actions** 中添加以下 secrets：

| Secret 名称 | 说明 |
|------------|------|
| `MAVEN_CENTRAL_USERNAME` | Maven Central 部署令牌用户名 |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central 部署令牌密码 |
| `MAVEN_GPG_PRIVATE_KEY` | GPG 私钥（ASCII armor 格式） |
| `MAVEN_GPG_KEY_ID` | GPG 密钥 ID（最后 8 位） |
| `MAVEN_GPG_PASSPHRASE` | GPG 密钥密码 |

### 导出 GPG 私钥用于 GitHub Secrets

```bash
# 导出私钥为 ASCII armor 格式
gpg --armor --export-secret-keys YOUR_KEY_ID

# 复制输出内容到 MAVEN_GPG_PRIVATE_KEY secret
```

### 触发自动发布

推送一个版本 tag 即可触发自动发布：

```bash
git tag -a v2.4.0 -m "Release v2.4.0"
git push origin v2.4.0
```

## 发布的 Artifacts

发布后，以下 artifacts 将可用：

| GroupId | ArtifactId | 说明 |
|---------|------------|------|
| `com.francium` | `francium-loader` | 完整加载器（包含所有模块） |
| `com.francium` | `francium-api` | 公共 API |
| `com.francium` | `francium-core` | 核心加载器 |
| `com.francium` | `francium-resolver` | SAT 依赖解析器 |
| `com.francium` | `francium-manager` | 套件管理器 |
| `com.francium` | `francium-ai-bridge` | AI 版本桥接 |
| `com.francium` | `francium-profiler` | 内存分析器 |
| `com.francium` | `francium-server` | 服务器同步 |

## 使用示例

### Gradle

```groovy
repositories {
    mavenCentral()
}

dependencies {
    // 完整加载器
    implementation 'com.francium:francium-loader:2.4.0'
    
    // 或者只使用特定模块
    implementation 'com.francium:francium-api:2.4.0'
    implementation 'com.francium:francium-resolver:2.4.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.francium</groupId>
    <artifactId>francium-loader</artifactId>
    <version>2.4.0</version>
</dependency>
```

## 常见问题

### 1. 发布失败：签名验证失败
- 确保 GPG 密钥已正确上传到密钥服务器
- 检查 `signing.keyId`、`signing.password` 和 `signing.secretKeyRingFile` 是否正确

### 2. 发布失败：401 Unauthorized
- 检查部署令牌是否正确
- 确保令牌未过期

### 3. 发布失败：403 Forbidden
- 确保你已验证 `com.francium` 命名空间
- 检查 groupId 是否与验证的命名空间匹配

### 4. 发布后找不到 artifact
- 等待 10-30 分钟让 Maven Central 同步
- 检查 [https://search.maven.org/](https://search.maven.org/)

## 相关链接

- [Maven Central Portal](https://central.sonatype.com/)
- [Maven Central 发布指南](https://central.sonatype.org/publish/publish-guide/)
- [GPG 密钥生成](https://central.sonatype.org/publish/requirements/gpg/)
- [OSSRH 指南](https://central.sonatype.org/publish/publish-guide/)
