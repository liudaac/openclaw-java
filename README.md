# OpenClaw Java

OpenClaw Java Edition - 基于 Spring Boot 的 AI Agent 平台

## 项目简介

OpenClaw Java 是 OpenClaw 的 Java 实现版本，提供完整的 AI Agent 功能，包括：

- 🤖 多通道支持 (Telegram, Feishu, Discord, Slack)
- 🛠️ 丰富的工具生态 (Web Search, File Operations, Email, Calendar 等)
- 🧠 记忆系统 (向量搜索, SQLite/pgvector 存储)
- 🔒 企业级安全 (SSRF 防护, 输入验证, Secrets 管理)
- 📊 Dashboard/Control UI (Web 管理界面)

## 技术栈

- **框架**: Spring Boot 3.2, Spring WebFlux
- **AI 集成**: Spring AI (OpenAI, Anthropic, Ollama)
- **数据库**: SQLite, PostgreSQL (pgvector)
- **构建工具**: Maven 3.9+
- **Java 版本**: 21

## 项目结构

```
openclaw-java/
├── openclaw-plugin-sdk      # 插件 SDK
├── openclaw-gateway         # Gateway 核心
├── openclaw-server          # HTTP/WebSocket 服务器
├── openclaw-agent           # Agent 核心
├── openclaw-channel-telegram   # Telegram 通道
├── openclaw-channel-feishu     # 飞书通道
├── openclaw-channel-discord    # Discord 通道
├── openclaw-channel-slack      # Slack 通道
├── openclaw-tools           # 工具集
├── openclaw-memory          # 记忆系统
├── openclaw-security        # 安全模块
├── openclaw-secrets         # Secrets 管理
└── openclaw-cli             # CLI 工具
```

## 编译

### 环境要求

- JDK 21+
- Maven 3.9+
- (可选) Docker & Docker Compose

### 编译命令

```bash
# 克隆项目
git clone https://github.com/liudaac/openclaw-java.git
cd openclaw-java

# 编译所有模块
mvn clean install

# 跳过测试编译
mvn clean install -DskipTests

# 指定版本编译
mvn clean install -Drevision=2026.3.9
```

### 编译输出

编译完成后，可执行 JAR 文件位于：
- `openclaw-gateway/target/openclaw-gateway-*.jar`
- `openclaw-server/target/openclaw-server-*.jar`
- `openclaw-cli/target/openclaw-cli-*.jar`

## 部署

### 方式一：直接运行

```bash
# 运行 Gateway
java -jar openclaw-gateway/target/openclaw-gateway-2026.3.9-SNAPSHOT.jar

# 运行 Server
java -jar openclaw-server/target/openclaw-server-2026.3.9-SNAPSHOT.jar
```

### 方式二：Docker 部署

```bash
# 构建 Docker 镜像
docker build -t openclaw-java:latest .

# 运行容器
docker run -p 18789:18789 openclaw-java:latest
```

### 方式三：Docker Compose

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 配置

### 配置文件位置

- 主配置: `~/.openclaw/openclaw.json`
- 环境变量配置: `application.yml`

### 基本配置示例

```json
{
  "gateway": {
    "bind": "0.0.0.0",
    "port": 18789,
    "auth": {
      "mode": "token",
      "token": "your-secure-token"
    },
    "controlUi": {
      "enabled": true,
      "basePath": "/"
    }
  },
  "llm": {
    "provider": "openai",
    "model": "gpt-4",
    "apiKey": "your-api-key"
  },
  "channels": {
    "telegram": {
      "enabled": true,
      "botToken": "your-bot-token"
    },
    "feishu": {
      "enabled": true,
      "appId": "your-app-id",
      "appSecret": "your-app-secret"
    }
  }
}
```

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `OPENCLAW_GATEWAY_BIND` | 绑定地址 | `0.0.0.0` |
| `OPENCLAW_GATEWAY_PORT` | 端口 | `18789` |
| `OPENCLAW_GATEWAY_AUTH_TOKEN` | 认证 Token | - |
| `OPENCLAW_LLM_PROVIDER` | LLM 提供商 | `openai` |
| `OPENCLAW_LLM_API_KEY` | API Key | - |

## 运行

### 启动 Gateway

```bash
# 默认启动
java -jar openclaw-gateway/target/openclaw-gateway-*.jar

# 指定配置文件
java -jar openclaw-gateway/target/openclaw-gateway-*.jar --spring.config.location=file:/path/to/config.yml

# 指定环境变量
OPENCLAW_GATEWAY_PORT=8080 java -jar openclaw-gateway/target/openclaw-gateway-*.jar
```

### 访问 Dashboard

启动后访问：`http://localhost:18789/`

Dashboard 功能：
- 💬 Chat: 与 AI 对话
- 📊 Status: 查看系统状态
- ⚙️ Config: 管理配置
- 📜 Logs: 查看日志
- 🔧 Tools: 管理工具

## 使用说明

### CLI 命令

```bash
# 查看帮助
java -jar openclaw-cli/target/openclaw-cli-*.jar --help

# 启动 TUI 交互界面
java -jar openclaw-cli/target/openclaw-cli-*.jar tui

# 发送消息
java -jar openclaw-cli/target/openclaw-cli-*.jar send --channel telegram --to user_id --message "Hello"

# 创建备份
java -jar openclaw-cli/target/openclaw-cli-*.jar backup create

# 验证备份
java -jar openclaw-cli/target/openclaw-cli-*.jar backup verify /path/to/backup.zip
```

### API 使用

```bash
# 发送消息
curl -X POST http://localhost:18789/api/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "channel": "telegram",
    "to": "user_id",
    "message": "Hello from OpenClaw!"
  }'

# 获取状态
curl http://localhost:18789/api/status

# 获取配置
curl http://localhost:18789/api/config
```

### WebSocket 连接

```javascript
const ws = new WebSocket('ws://localhost:18789/ws');

ws.onopen = () => {
  console.log('Connected to OpenClaw');
};

ws.onmessage = (event) => {
  console.log('Received:', event.data);
};

ws.send(JSON.stringify({
  type: 'message',
  channel: 'telegram',
  to: 'user_id',
  content: 'Hello'
}));
```

## 开发

### 添加新通道

1. 创建新模块 `openclaw-channel-{name}`
2. 实现 `ChannelAdapter` 接口
3. 在 `pom.xml` 中添加依赖
4. 注册到 Gateway

### 添加新工具

1. 在 `openclaw-tools` 模块中创建工具类
2. 实现 `Tool` 接口
3. 添加 `@Component` 注解
4. 工具会自动注册

## 版本管理

本项目使用 Maven Flatten Plugin 进行 CI-friendly 版本管理：

```bash
# 构建指定版本
mvn clean install -Drevision=2026.3.10-SNAPSHOT

# 发布版本
mvn clean deploy -Drevision=2026.3.9
```

版本号只需在父 POM 的 `<revision>` 属性中修改即可。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 相关链接

- [OpenClaw Node.js 版本](https://github.com/liudaac/openclaw)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
