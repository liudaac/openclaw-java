# Java版 OpenClaw 第三阶段迭代计划 - 2026-04-08

## 目标

完成剩余待处理任务，包括控制器集成和基础功能完善。

## 任务列表

### 任务1: 集成 ClientDisconnectHandler 到控制器
**优先级**: 高
**说明**: 将 HTTP 客户端断开检测集成到 AgentController 和 ChatController

#### 1.1 更新 AgentController
- 注入 ClientDisconnectHandler
- 在 spawn/wait 操作中检测客户端断开
- 中止长时间运行的操作

#### 1.2 更新 ChatController
- 注入 ClientDisconnectHandler
- 在流式响应中检测客户端断开

### 任务2: HTTP 404 错误分类支持
**优先级**: 中
**说明**: 为模型回退链添加 HTTP 404 错误分类

#### 2.1 创建错误分类器
- 识别 HTTP 404 错误
- 分类为可回退错误类型

#### 2.2 更新 Agent 错误处理
- 在 Pi Embedded Runner 中集成

### 任务3: 单元测试编写
**优先级**: 中
**说明**: 为本次迭代的变更编写单元测试

#### 3.1 Agent 模块测试
- AgentConfigTest: systemPromptOverride
- HeartbeatConfigTest: includeSystemPromptSection
- SpawnRequestTest: lightContext

#### 3.2 Memory 模块测试
- MemoryConfigTest: SlotConfig, DreamingConfig

#### 3.3 Cron 模块测试
- JobIdentityNormalizerTest

#### 3.4 Telegram 模块测试
- TelegramOutboundAdapterTest: 长消息分割

#### 3.5 Server 模块测试
- ClientDisconnectHandlerTest

---

## 实施顺序

1. 集成 ClientDisconnectHandler 到控制器
2. 创建 HTTP 404 错误分类支持
3. 编写单元测试

---

*计划创建时间: 2026-04-08*
