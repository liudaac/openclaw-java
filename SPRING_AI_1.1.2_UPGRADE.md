# Spring AI 1.1.2 升级任务清单

## 已完成 ✅

### 1. 依赖更新
- [x] 更新 Spring AI 版本到 1.1.2
- [x] 更新 bucket4j 依赖 (bucket4j_jdk17-core:8.17.0)
- [x] 更新 Spring AI OpenAI (spring-ai-starter-model-openai)
- [x] 添加 Prometheus Metrics 依赖 (micrometer-registry-prometheus)
- [x] 添加 AspectJ 依赖 (spring-boot-starter-aop)
- [x] 添加 Caffeine Cache 依赖 (caffeine)

### 2. 核心服务修复
- [x] 修复 LlmService - 适配 Spring AI 1.1.2 API
- [x] 修复 AcpProtocolImpl - 移除 ChatResponse 依赖
- [x] 修复 MetricsConfig - 更新 Prometheus 包路径 (prometheusmetrics)
- [x] 修复 MetricsController - 更新 Prometheus 包路径

### 3. Controller 层修复
- [x] 修复 ConfigController - 修复重复 validateConfig 方法名
- [x] 创建 GatewayProperties 配置类
- [x] 修复 ChatController - 返回类型 ObjectNode
- [x] 修复 StatusController - 6 个方法的返回类型 ObjectNode
- [x] 修复 ToolController - ToolExecuteContext 构造器使用 builder

### 4. SDK 层修复
- [x] 修复 ChannelId - 添加 name() 和 valueOf() 方法
- [x] 创建 ToolCall 类
- [x] 创建 OutboundMessage 类
- [x] 创建 AgentMessage 类
- [x] 创建 LlmService 接口

### 5. 接口定义修复
- [x] 修复 WorkQueue - 添加 getPendingCount(), getCompletedCount()
- [x] 修复 NodeRegistry - 添加 getNodeCount(), isNodeHealthy()
- [x] 修复 SessionStateMachine - 添加 transition(), getCurrentState()
- [x] 修复 CronService - 添加 getExecutionStats()

### 6. Service 层修复
- [x] 修复 StreamingResponseHandler - AtomicLong 类型转换 (.get())
- [x] 修复 GatewayWebSocketHandler - 添加 getConnectionCount()
- [x] 修复 RetryPolicyManager - 使用 IntervalFunction 替代 multiplier
- [x] 修复 GatewayServiceImpl - 内部类实现所有接口方法

## 待修复 🔧

### 7. Controller 层 (剩余)
- [ ] GatewayController - 返回类型和缺少方法
- [ ] ChannelController - ChannelId 方法调用
- [ ] CronController - 检查 getExecutionStats 调用
- [ ] StreamingController - ChannelAdapter 抽象类问题

### 8. Service 层 (剩余)
- [ ] StreamingMessageService - StreamingMessageRequest 构造器
- [ ] AcpProtocolImpl - 检查 ChatResponse 引用
- [ ] 其他 Service 类检查

### 9. 类型转换问题 (剩余)
- [ ] 检查所有 ObjectNode 到 JsonNode 的返回类型
- [ ] 检查所有 Mono/Flux 返回类型

### 10. 其他问题
- [ ] 检查 AspectJ 注解是否正常工作
- [ ] 检查所有 Spring AI 导入是否正确
- [ ] 运行完整编译测试

## 当前状态

**已修复**: ~25 个文件
**待修复**: ~15-20 个文件
**预计完成时间**: 还需 1-2 小时

## 主要变更

### Spring AI API 变更
- ChatClient 从 `org.springframework.ai.chat.ChatClient` → `org.springframework.ai.chat.client.ChatClient`
- 移除 ChatResponse, ChatClient 直接返回 String
- 使用 `chatClient.prompt().call().content()` 替代 `chatClient.call(new Prompt())`

### Resilience4j API 变更
- RetryConfig.Builder 移除 multiplier 方法
- 使用 IntervalFunction.ofExponentialBackoff() 替代

### Micrometer Prometheus 变更
- 包路径从 `io.micrometer.prometheus` → `io.micrometer.prometheusmetrics`

## 下一步计划

1. 继续修复 GatewayController
2. 修复 ChannelController
3. 修复 StreamingMessageService
4. 运行编译测试
5. 修复剩余问题
