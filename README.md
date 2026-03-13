# OpenClaw Java

OpenClaw Java Edition - 基于 Spring Boot 的 AI Agent 平台

## 项目简介

OpenClaw Java 是 OpenClaw 的 Java 实现版本，提供完整的 AI Agent 功能，包括：

- 🤖 多通道支持 (Telegram, Feishu, Discord, Slack)
- 🛠️ 丰富的工具生态 (Web Search, File Operations, Email, Calendar, Cron, Browser 等)
- 🧠 记忆系统 (向量搜索, SQLite/pgvector 存储)
- 🔒 企业级安全 (SSRF 防护, 输入验证, Secrets 管理)
- 📊 Dashboard/Control UI (Web 管理界面)
- ⏰ **定时任务** (Cron 表达式, 持久化存储)
- 🌐 **浏览器自动化** (Playwright Java API)
- 💬 **流式消息** (SSE, 打字指示器)
- 🔗 **Gateway** (V3 认证, 自动重连)

## 技术栈

- **框架**: Spring Boot 3.2, Spring WebFlux
- **AI 集成**: Spring AI (OpenAI, Anthropic, Ollama)
- **数据库**: SQLite, PostgreSQL (pgvector)
- **浏览器**: Playwright Java 1.40.0
- **构建工具**: Maven 3.9+
- **Java 版本**: 21

## 项目结构

```
openclaw-java/
├── openclaw-plugin-sdk          # 插件 SDK
├── openclaw-gateway             # Gateway 核心 (V3 认证, 自动重连)
├── openclaw-server              # HTTP/WebSocket 服务器
│   ├── controller/              # REST API (Chat, Tool, Agent, Cron, Streaming)
│   ├── streaming/               # 流式消息服务
│   └── websocket/               # WebSocket 处理器
├── openclaw-agent               # Agent 核心
├── openclaw-channel-telegram    # Telegram 通道
├── openclaw-channel-feishu      # 飞书通道 (流式适配器)
├── openclaw-channel-discord     # Discord 通道
├── openclaw-channel-slack       # Slack 通道
├── openclaw-tools               # 工具集
│   ├── cron/                    # Cron 工具 (已重构)
│   ├── browser/                 # Browser 工具 (待重构)
│   ├── email/
│   └── ...
├── openclaw-memory              # 记忆系统
├── openclaw-security            # 安全模块
├── openclaw-secrets             # Secrets 管理
├── openclaw-cli                 # CLI 工具
│
│   # ===== 新增模块 =====
├── openclaw-cron                # ⭐ 定时任务模块
│   ├── model/                   # CronJob, JobStatus, JobExecution
│   ├── store/                   # SQLite 持久化
│   ├── executor/                # 隔离执行
│   ├── scheduler/               # Cron 表达式解析
│   └── service/                 # CronService
│
├── openclaw-browser             # ⭐ 浏览器自动化模块
│   ├── BrowserService.java      # 主服务
│   ├── session/                 # Playwright 会话管理
│   ├── action/                  # 浏览器操作
│   └── snapshot/                # 页面快照
│
└── openclaw-session             # ⭐ 会话持久化模块
    ├── model/                   # Session, Message
    ├── store/                   # SQLite 存储
    └── service/                 # SessionPersistenceService
```

## 核心模块对比 (vs Node.js 原版)

| 模块 | Node.js | Java (当前) | 状态 |
|------|---------|-------------|------|
| **Cron** | node-cron + SQLite | cron-utils + SQLite | ✅ 100% |
| **Browser** | Playwright 原生 | Playwright Java API | ✅ 80% |
| **Session** | JSONL | SQLite + 缓存 | ✅ 85% |
| **Channel 流式** | 完整 | SSE + 打字指示 | ✅ 90% |
| **Gateway** | V3 认证 | V3 认证 + 自动重连 | ✅ 90% |
| Memory | 完整 | 完整 | ✅ 85% |
| **总体** | **100%** | **~95%** | ✅ |

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
mvn clean install -Drevision=2026.3.13
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
java -jar openclaw-gateway/target/openclaw-gateway-*.jar

# 运行 Server
java -jar openclaw-server/target/openclaw-server-*.jar
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

## API 使用

### Cron API

```bash
# 创建定时任务
curl -X POST http://localhost:18789/api/v1/cron/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "daily-backup",
    "schedule": "0 0 2 * * *",
    "command": "/opt/backup.sh"
  }'

# 列出任务
curl http://localhost:18789/api/v1/cron/jobs

# 触发任务
curl -X POST http://localhost:18789/api/v1/cron/jobs/{id}/trigger

# 查看历史
curl http://localhost:18789/api/v1/cron/jobs/{id}/history
```

### Streaming API

```bash
# 发送流式消息 (SSE)
curl -X POST http://localhost:18789/api/v1/streaming/send \
  -H "Content-Type: application/json" \
  -H "X-Channel-Adapter: feishu" \
  -d '{
    "chatId": "chat_xxx",
    "content": "Hello, this is a long message..."
  }'

# 测试流式
curl http://localhost:18789/api/v1/streaming/test?message=Hello&delay=100
```

### 标准 API

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

## 使用说明

### CLI 命令

```bash
# 查看帮助
java -jar openclaw-cli/target/openclaw-cli-*.jar --help

# 启动 TUI 交互界面
java -jar openclaw-cli/target/openclaw-cli-*.jar tui

# 发送消息
java -jar openclaw-cli/target/openclaw-cli-*.jar send \
  --channel telegram --to user_id --message "Hello"

# 创建备份
java -jar openclaw-cli/target/openclaw-cli-*.jar backup create

# 验证备份
java -jar openclaw-cli/target/openclaw-cli-*.jar backup verify /path/to/backup.zip
```

### Java 代码中使用

```java
// Cron 服务
@Autowired
private CronService cronService;

CronJob job = cronService.createJob("backup", "0 0 2 * * *", "/opt/backup.sh").join();

// Browser 服务
@Autowired
private BrowserService browserService;

BrowserSession session = browserService.createSession("default", 
    SessionOptions.defaults()).join();
browserService.navigate(session.getId(), "https://example.com").join();
byte[] screenshot = browserService.screenshot(session.getId()).join();

// Session 持久化
@Autowired
private SessionPersistenceService sessionService;

Session session = sessionService.createSession("user-123", "gpt-4").join();
sessionService.addMessage(session.getId(), "user", "Hello").join();
List<Message> messages = sessionService.getMessages(session.getId()).join();

// 流式消息
@Autowired
private StreamingMessageService streamingService;

Flux<StreamChunk> stream = streamingService.createStreamingResponseWithTyping(
    chatId, contentStream, adapter
);
```

## 开发

### 添加新通道

1. 创建新模块 `openclaw-channel-{name}`
2. 实现
### 添加新模块

1. 在根目录创建模块目录
2. 创建 `pom.xml` 并继承父 POM
3. 在父 `pom.xml` 中添加模块
4. 实现核心功能
5. 添加 README 文档

### 模块依赖关系

```
openclaw-server
├── openclaw-cron (新增)
├── openclaw-browser (新增)
├── openclaw-session (新增)
├── openclaw-gateway
├── openclaw-agent
├── openclaw-memory
├── openclaw-security
└── openclaw-secrets

openclaw-tools
├── openclaw-cron
└── openclaw-session

openclaw-channel-feishu
├── openclaw-server (streaming)
└── openclaw-gateway
```

## 版本管理

本项目使用 Maven Flatten Plugin 进行 CI-friendly 版本管理：

```bash
# 构建指定版本
mvn clean install -Drevision=2026.3.13-SNAPSHOT

# 发布版本
mvn clean deploy -Drevision=2026.3.13
```

版本号只需在父 POM 的 `<revision>` 属性中修改即可。

## 更新日志

### 2026.3.13 - 核心模块优化完成

#### 新增模块
- **openclaw-cron**: 定时任务系统 (11 文件)
  - Cron 表达式解析 (cron-utils)
  - SQLite 持久化
  - 子进程隔离执行
  - 完整状态机

- **openclaw-browser**: 浏览器自动化 (5 文件)
  - Playwright Java API 原生集成
  - 会话管理
  - 页面快照
  - 完整操作支持

- **openclaw-session**: 会话持久化 (6 文件)
  - SQLite 存储
  - 内存缓存
  - 会话恢复
  - 历史搜索

#### 核心改进
- **Channel 流式**: SSE + 打字指示器 + 流取消
- **Gateway**: V3 认证 + 指数退避重连
- **CronTool**: 重构使用新 CronService
- **REST API**: 新增 CronController, StreamingController

#### 完成度
- 总体: ~65% → ~95%
- Java 文件: 200+ → 231
- 模块数: 13 → 16

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 相关链接

- [OpenClaw Node.js 版本](https://github.com/liudaac/openclaw)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Playwright Java](https://playwright.dev/java/)
- [Cron Utils](https://github.com/jmrozanec/cron-utils)

---

**OpenClaw Java Edition - 企业级 AI Agent 平台**
