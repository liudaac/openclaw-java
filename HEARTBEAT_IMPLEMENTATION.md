# OpenClaw Java Heartbeat Implementation

## 概述

本文档描述了 OpenClaw Java 版本中 Heartbeat 机制的完整实现，与 Node.js 原版功能对齐。

## 实现组件

### 1. 核心模块 (openclaw-agent)

#### 1.1 HeartbeatConfig.java
- **位置**: `openclaw-agent/src/main/java/openclaw/agent/heartbeat/HeartbeatConfig.java`
- **功能**: 配置属性类，支持 `openclaw.agents.defaults.heartbeat` 前缀
- **配置项**:
  - `enabled`: 启用/禁用 heartbeat
  - `every`: 触发间隔 (ISO 8601 格式，如 PT30M)
  - `ackMaxChars`: HEARTBEAT_OK 最大字符数
  - `prompt`: 自定义 heartbeat prompt
  - `target`: 交付目标 ("last", "none", 或特定频道)

#### 1.2 HeartbeatProcessor.java
- **位置**: `openclaw-agent/src/main/java/openclaw/agent/heartbeat/HeartbeatProcessor.java`
- **功能**: 处理 HEARTBEAT_OK token 检测和消息过滤
- **特性**:
  - HTML/Markdown 包裹检测
  - 前后空格和标点处理
  - 空内容检测 (跳过 API 调用)
  - NO_REPLY token 支持

#### 1.3 HeartbeatService.java
- **位置**: `openclaw-agent/src/main/java/openclaw/agent/heartbeat/HeartbeatService.java`
- **功能**: 定时触发 Agent 执行 heartbeat 任务
- **特性**:
  - 定时调度 (@Scheduled)
  - 手动触发支持
  - 唤醒请求队列 (优先级)
  - 重试机制
  - 统计信息

### 2. 工作空间文件服务 (openclaw-agent)

#### 2.1 WorkspaceFileService.java
- **位置**: `openclaw-agent/src/main/java/openclaw/agent/workspace/WorkspaceFileService.java`
- **功能**: 读取工作空间上下文文件
- **支持文件**:
  - AGENTS.md
  - SOUL.md
  - USER.md
  - MEMORY.md
  - HEARTBEAT.md
  - TOOLS.md
  - IDENTITY.md
  - BOOTSTRAP.md

### 3. 系统 Prompt 构建 (openclaw-agent)

#### 3.1 SystemPromptBuilder.java
- **位置**: `openclaw-agent/src/main/java/openclaw/agent/prompt/SystemPromptBuilder.java`
- **功能**: 构建包含 heartbeat 指导的系统 Prompt
- **包含内容**:
  - 运行时信息
  - 上下文文件
  - Heartbeat 指导
  - Silent replies 规则
  - Group chat 指导

### 4. Cron 模块增强 (openclaw-cron)

#### 4.1 CronJob.java
- **新增字段**:
  - `wakeMode`: "now" 或 "next-heartbeat"
  - `sessionTarget`: "main" 或 "isolated"
  - `agentId`: 目标 Agent ID
  - `sessionKey`: 目标 Session Key
  - `deliveryChannel`: 交付频道
  - `deliveryTo`: 交付目标

#### 4.2 CronService.java
- **增强**: 支持通过 heartbeat 机制唤醒 Agent
- **逻辑**: 当 `wakeMode == NEXT_HEARTBEAT` 时，请求 heartbeat 服务执行

### 5. REST API (openclaw-server)

#### 5.1 HeartbeatController.java
- **位置**: `openclaw-server/src/main/java/openclaw/server/controller/HeartbeatController.java`
- **端点**:
  - `POST /api/v1/heartbeat/trigger` - 手动触发 heartbeat
  - `GET /api/v1/heartbeat/stats` - 获取统计信息
  - `GET /api/v1/heartbeat/config` - 获取配置
  - `POST /api/v1/heartbeat/config` - 更新配置
  - `POST /api/v1/heartbeat/request` - 请求立即执行

#### 5.2 OpenClawConfig.java
- **增强**: 添加 Heartbeat 相关 Bean 配置
- **注解**: 添加 `@EnableScheduling` 启用 Spring 调度

## 配置示例

### application.yml

```yaml
openclaw:
  agents:
    defaults:
      heartbeat:
        enabled: true
        every: PT30M
        ackMaxChars: 300
        target: last
        prompt: "Read HEARTBEAT.md if it exists..."

spring:
  task:
    scheduling:
      pool:
        size: 10
```

### Cron Job with Wake Mode

```java
CronJob job = new CronJob();
job.setName("daily-report");
job.setSchedule("0 0 9 * * *"); // 每天 9:00
job.setCommand("Generate daily report");
job.setWakeMode(CronJob.WakeMode.NEXT_HEARTBEAT);
job.setSessionTarget("main");
job.setAgentId("default");
```

## API 使用示例

### 手动触发 Heartbeat

```bash
curl -X POST http://localhost:8080/api/v1/heartbeat/trigger \
  -d "reason=manual" \
  -d "agentId=default"
```

### 获取统计信息

```bash
curl http://localhost:8080/api/v1/heartbeat/stats
```

### 更新配置

```bash
curl -X POST http://localhost:8080/api/v1/heartbeat/config \
  -d "enabled=true" \
  -d "every=PT15M" \
  -d "ackMaxChars=500"
```

## 与 Node.js 原版对比

| 功能 | Node.js 原版 | Java 实现 |
|------|-------------|-----------|
| HEARTBEAT.md 读取 | ✅ | ✅ |
| HEARTBEAT_OK 处理 | ✅ | ✅ |
| NO_REPLY 支持 | ✅ | ✅ |
| 定时触发 | ✅ | ✅ |
| 手动触发 | ✅ | ✅ |
| 唤醒队列 | ✅ | ✅ |
| 优先级处理 | ✅ | ✅ |
| 重试机制 | ✅ | ✅ |
| 空内容跳过 | ✅ | ✅ |
| Cron wakeMode | ✅ | ✅ |
| 系统 Prompt 集成 | ✅ | ✅ |
| REST API | ❌ | ✅ (新增) |
| 配置热更新 | ❌ | ✅ (新增) |

## 文件清单

### 新建文件

1. `openclaw-agent/src/main/java/openclaw/agent/heartbeat/HeartbeatConfig.java`
2. `openclaw-agent/src/main/java/openclaw/agent/heartbeat/HeartbeatProcessor.java`
3. `openclaw-agent/src/main/java/openclaw/agent/heartbeat/HeartbeatService.java`
4. `openclaw-agent/src/main/java/openclaw/agent/heartbeat/package-info.java`
5. `openclaw-agent/src/main/java/openclaw/agent/workspace/WorkspaceFileService.java`
6. `openclaw-agent/src/main/java/openclaw/agent/workspace/package-info.java`
7. `openclaw-agent/src/main/java/openclaw/agent/prompt/SystemPromptBuilder.java`
8. `openclaw-agent/src/main/java/openclaw/agent/prompt/package-info.java`
9. `openclaw-server/src/main/java/openclaw/server/controller/HeartbeatController.java`
10. `openclaw-server/src/main/resources/application-heartbeat.yml`

### 修改文件

1. `openclaw-cron/src/main/java/openclaw/cron/model/CronJob.java`
2. `openclaw-cron/src/main/java/openclaw/cron/service/CronService.java`
3. `openclaw-server/src/main/java/openclaw/server/config/OpenClawConfig.java`

## 后续建议

1. **测试**: 编写单元测试和集成测试
2. **文档**: 更新主 README 添加 Heartbeat 模块说明
3. **监控**: 添加 metrics 和监控端点
4. **WebSocket**: 实现实时通知功能
5. **分布式**: 支持 Redis 分布式调度
