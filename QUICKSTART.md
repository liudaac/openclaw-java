# OpenClaw Java 快速上手指南

> 本文档帮助你快速完成 OpenClaw Java 版本的本地搭建和运行。

---

## 📋 目录

1. [环境准备](#环境准备)
2. [获取代码](#获取代码)
3. [编译构建](#编译构建)
4. [配置运行](#配置运行)
5. [验证安装](#验证安装)
6. [常见问题](#常见问题)

---

## 环境准备

### 必需环境

| 组件 | 版本要求 | 验证命令 |
|------|----------|----------|
| **JDK** | 21+ | `java -version` |
| **Maven** | 3.9+ | `mvn -version` |
| **Git** | 任意 | `git --version` |

### 可选环境

- **Docker** 20.10+ (用于容器化部署)
- **PostgreSQL** 14+ (可选，用于向量存储)

### 快速检查环境

```bash
# 检查 Java
java -version
# 应显示: openjdk version "21.xxx"

# 检查 Maven
mvn -version
# 应显示: Apache Maven 3.9.x

# 检查 Git
git --version
```

---

## 获取代码

### 方式一：克隆 GitHub 仓库

```bash
# 克隆项目
git clone https://github.com/liudaac/openclaw-java.git

# 进入项目目录
cd openclaw-java
```

### 方式二：下载源码包

```bash
# 下载最新 release
curl -L -o openclaw-java.zip https://github.com/liudaac/openclaw-java/archive/refs/heads/main.zip

# 解压
unzip openclaw-java.zip
cd openclaw-java-main
```

---

## 编译构建

### 完整编译（推荐）

```bash
# 编译所有模块（首次编译需要 5-10 分钟）
mvn clean install
```

### 跳过测试编译（快速）

```bash
# 跳过测试，加快编译速度
mvn clean install -DskipTests
```

### 编译特定模块

```bash
# 只编译 Feishu 通道模块
mvn clean install -pl openclaw-channel-feishu -am

# 只编译 Server 模块
mvn clean install -pl openclaw-server -am
```

### 编译输出

编译成功后，各模块 JAR 文件位于：

```
openclaw-gateway/target/openclaw-gateway-*.jar
openclaw-server/target/openclaw-server-*.jar
openclaw-cli/target/openclaw-cli-*.jar
```

---

## 配置运行

### 步骤 1：创建配置目录

```bash
# 创建 OpenClaw 配置目录
mkdir -p ~/.openclaw
```

### 步骤 2：创建主配置文件

创建文件 `~/.openclaw/openclaw.json`：

```json
{
  "gateway": {
    "enabled": true,
    "bind": "0.0.0.0",
    "port": 18789,
    "auth": {
      "mode": "token",
      "token": "your-secure-token-change-me"
    }
  },
  "llm": {
    "provider": "openai",
    "model": "gpt-4o",
    "apiKey": "sk-your-openai-api-key"
  },
  "channels": {
    "feishu": {
      "enabled": true,
      "appId": "cli-your-feishu-app-id",
      "appSecret": "your-feishu-app-secret"
    }
  },
  "memory": {
    "enabled": true,
    "storageType": "sqlite",
    "dbPath": "${user.home}/.openclaw/memory.db"
  }
}
```

### 步骤 3：配置说明

#### LLM 配置选项

| 提供商 | provider 值 | 需要配置 |
|--------|-------------|----------|
| OpenAI | `openai` | apiKey |
| Anthropic | `anthropic` | apiKey |
| Ollama | `ollama` | baseUrl |
| Moonshot | `moonshot` | apiKey |
| MiniMax | `minimax` | apiKey |

#### 飞书通道配置

```json
{
  "channels": {
    "feishu": {
      "enabled": true,
      "appId": "cli_xxxxxxxxxxxxxxxx",
      "appSecret": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
      "encryptKey": "optional-encrypt-key",
      "verificationToken": "optional-verification-token",
      "groupPolicy": "open",
      "requireMention": false
    }
  }
}
```

**配置项说明：**
- `appId` / `appSecret`: 从飞书开发者后台获取
- `groupPolicy`: `open` | `allowlist` | `disabled` - 群组消息处理策略
- `requireMention`: 是否需要 @机器人 才响应

### 步骤 4：运行 Gateway

```bash
# 运行 Gateway 服务
java -jar openclaw-gateway/target/openclaw-gateway-*.jar

# 或使用 Maven 直接运行
mvn -pl openclaw-gateway spring-boot:run
```

Gateway 启动后：
- 管理界面: http://localhost:18789
- WebSocket: ws://localhost:18789/ws

### 步骤 5：运行 Server（可选）

```bash
# 运行 HTTP Server
java -jar openclaw-server/target/openclaw-server-*.jar

# 或使用 Maven
mvn -pl openclaw-server spring-boot:run
```

---

## 验证安装

### 1. 检查 Gateway 是否运行

```bash
# 查看 Gateway 状态
curl http://localhost:18789/status

# 预期响应
{"status":"ok","version":"2026.3.x"}
```

### 2. 检查通道连接

```bash
# 查看已配置的通道
curl http://localhost:18789/channels
```

### 3. 飞书通道验证

在飞书群中 @机器人，发送测试消息：

```
@机器人 /status
```

预期响应：机器人返回状态信息。

### 4. 查看日志

```bash
# Gateway 日志
tail -f ~/.openclaw/logs/gateway.log

# Server 日志
tail -f ~/.openclaw/logs/server.log
```

---

## Docker 部署（可选）

### 使用 Docker Compose

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f gateway

# 停止服务
docker-compose down
```

### 构建 Docker 镜像

```bash
# 构建镜像
docker build -t openclaw-java:latest .

# 运行容器
docker run -d \
  -p 18789:18789 \
  -v ~/.openclaw:/root/.openclaw \
  --name openclaw \
  openclaw-java:latest
```

---

## 常见问题

### Q1: 编译失败，提示 "release version 21 not supported"

**原因**: JDK 版本不正确

**解决**:
```bash
# 检查 JDK 版本
java -version

# 安装 JDK 21
# Ubuntu/Debian:
sudo apt install openjdk-21-jdk

# macOS:
brew install openjdk@21
```

### Q2: Maven 依赖下载失败

**解决**:
```bash
# 清除 Maven 缓存
rm -rf ~/.m2/repository

# 重新编译
mvn clean install -U
```

### Q3: 飞书通道无法接收消息

**检查清单**:
1. [ ] 飞书 App ID 和 Secret 正确
2. [ ] 机器人已在飞书群中添加
3. [ ] 订阅了 `im.message.receive_v1` 事件
4. [ ] 配置了正确的 Webhook URL
5. [ ] 检查 Gateway 日志中的错误信息

### Q4: 如何切换 LLM 提供商

修改 `~/.openclaw/openclaw.json`：

```json
{
  "llm": {
    "provider": "anthropic",
    "model": "claude-3-opus-20240229",
    "apiKey": "sk-ant-api03-your-key"
  }
}
```

重启 Gateway 生效。

### Q5: 如何启用本地 Ollama

```json
{
  "llm": {
    "provider": "ollama",
    "model": "llama2",
    "baseUrl": "http://localhost:11434"
  }
}
```

---

## 下一步

- 📖 [完整配置文档](README.md)
- 🔧 [API 文档](docs/API.md) (如有)
- 💬 [加入社区](https://github.com/liudaac/openclaw-java/discussions)

---

**最后更新**: 2026-03-25
**版本**: 2026.3.25
