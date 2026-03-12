# OpenClaw Java

[![Version](https://img.shields.io/badge/version-2026.3.9-blue.svg)](https://github.com/openclaw/openclaw-java)
[![Java](https://img.shields.io/badge/java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

OpenClaw Java 是 [OpenClaw](https://github.com/openclaw/openclaw) 的 Java 实现版本，提供 AI Agent 平台的核心功能。

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.9+
- Docker (可选)

### 1. 克隆项目
```bash
git clone https://github.com/openclaw/openclaw-java.git
cd openclaw-java
```

### 2. 配置环境变量
```bash
export OPENAI_API_KEY=sk-your-api-key
export OPENCLAW_GATEWAY_PORT=8080
```

### 3. 构建项目
```bash
mvn clean install -DskipTests
```

### 4. 运行服务
```bash
cd openclaw-server
mvn spring-boot:run
```

### 5. 验证
```bash
curl http://localhost:8080/api/v1/gateway/health
```

## 📦 Docker 部署

```bash
# 构建并启动
docker-compose up -d

# 查看日志
docker-compose logs -f openclaw-server

# 停止
docker-compose down
```

## 📡 API 文档

启动后访问: http://localhost:8080/swagger-ui.html

### 核心端点

| 端点 | 描述 |
|------|------|
| `/api/v1/gateway/health` | 健康检查 |
| `/api/v1/agent/spawn` | 创建 Agent |
| `/api/v1/channels` | 通道列表 |
| `/api/v1/tools` | 工具列表 |
| `/api/v1/metrics/prometheus` | Prometheus 指标 |

## 🏗️ 架构

```
┌─────────────────────────────────────────────────────────┐
│                    OpenClaw Server                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │
│  │   Gateway   │ │    Agent    │ │   Metrics   │       │
│  │  Controller │ │  Controller │ │  Controller │       │
│  └─────────────┘ └─────────────┘ └─────────────┘       │
├─────────────────────────────────────────────────────────┤
│  Spring Boot + WebFlux + Spring AI + Resilience4j      │
├─────────────────────────────────────────────────────────┤
│  Gateway │ Agent │ Channels │ Tools │ Memory │ Security │
└─────────────────────────────────────────────────────────┘
```

## 📊 功能特性

### Phase 1: 核心基础设施 ✅
- HTTP/WebSocket Server
- LLM Client (OpenAI)
- Gateway API
- Agent API (ACP Protocol)

### Phase 2: 通道基础设施 ✅
- Telegram Webhook
- Feishu Webhook
- Inbound/Outbound Adapters

### Phase 3: 工具系统 ✅
- Browser Tool (Playwright)
- Image Tool (DALL-E)
- Cron Tool (Scheduler)
- Media Handler

### Phase 4: 生产就绪 ✅
- 测试覆盖 (~60%)
- Prometheus 监控
- 性能优化
- Docker 部署

## 📈 监控

### Prometheus 指标
```bash
curl http://localhost:8080/api/v1/metrics/prometheus
```

### Grafana 仪表板
```bash
open http://localhost:3000
# 默认账号: admin/admin
```

## 🧪 测试

```bash
# 运行测试
mvn test

# 生成覆盖率报告
mvn jacoco:report
```

## 📚 文档

- [Phase 1 README](./PHASE1_README.md)
- [Phase 2 Summary](./PHASE2_SUMMARY.md)
- [Phase 3 Summary](./PHASE3_SUMMARY.md)
- [Phase 4 Summary](./PHASE4_SUMMARY.md)
- [Architecture Analysis](./openclaw架构分析报告.md)

## 🤝 贡献

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/xxx`)
3. 提交更改 (`git commit -am 'Add feature'`)
4. 推送分支 (`git push origin feature/xxx`)
5. 创建 Pull Request

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🙏 致谢

- [OpenClaw](https://github.com/openclaw/openclaw) - 原版 Node.js 实现
- [Spring Boot](https://spring.io/projects/spring-boot) - Web 框架
- [Spring AI](https://spring.io/projects/spring-ai) - AI 集成
- [Resilience4j](https://resilience4j.readme.io/) - 可靠性模式

---

**Made with ❤️ by OpenClaw Team**
