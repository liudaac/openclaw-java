# Spring AI 1.1.2 升级修复计划

## 当前状态

### 已完成 ✅ (约 35+ 文件)
- Spring AI 版本升级到 1.1.2
- 所有依赖更新（bucket4j, micrometer, aspectj, caffeine）
- 核心服务修复（LlmService, AcpProtocolImpl, MetricsConfig, MetricsController）
- Controller 层修复（ConfigController, ChatController, StatusController, ToolController, GatewayController, ChannelController, CronController）
- SDK 层修复（ChannelId, ToolCall, OutboundMessage, AgentMessage, LlmService 接口, ChannelMessage.Builder）
- 接口定义修复（WorkQueue, NodeRegistry, SessionStateMachine, CronService）
- Service 层修复（StreamingResponseHandler, GatewayWebSocketHandler, RetryPolicyManager, GatewayServiceImpl, StreamingMessageService）

## 分批修复计划

### 第一批：GatewayServiceImpl 修复 🔧
**状态**: 进行中
**问题**:
- [ ] NodeInfo.id() vs getId() 方法调用
- [ ] QueueStats 构造器参数类型 (float vs double)
- [ ] 内部类缺少接口方法（heartbeat, getItem, getStrategy）
- [ ] 导入问题

**预计时间**: 15 分钟

### 第二批：类型转换修复 🔧
**状态**: 待开始
**问题**:
- [ ] JsonNode/ObjectNode 返回类型不匹配
- [ ] CompletableFuture<Long> 转 long
- [ ] AtomicLong 转 long
- [ ] int/long 类型转换

**文件**:
- ChatController.java
- ConfigController.java
- GatewayController.java
- StreamingResponseHandler.java
- CronController.java

**预计时间**: 20 分钟

### 第三批：Channel 相关修复 🔧
**状态**: 待开始
**问题**:
- [ ] ChannelMessage.Builder.target() 方法
- [ ] ChannelOutboundAdapter.send() 方法
- [ ] StreamingController 的 ChannelAdapter 抽象方法
- [ ] StreamingMessageRequest 构造器

**文件**:
- ChannelController.java
- StreamingController.java
- StreamingMessageService.java

**预计时间**: 20 分钟

### 第四批：Resilience4j 修复 🔧
**状态**: 待开始
**问题**:
- [ ] RetryPolicyManager 的 multiplier 方法

**文件**:
- RetryPolicyManager.java

**预计时间**: 10 分钟

### 第五批：最终检查和测试 🔧
**状态**: 待开始
**任务**:
- [ ] 运行完整编译
- [ ] 修复剩余问题
- [ ] 测试关键功能

**预计时间**: 30 分钟

## 总预计时间
约 1.5-2 小时

## 当前批次
正在修复：第一批 - GatewayServiceImpl
