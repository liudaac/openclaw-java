# Spring AI 1.1.2 升级任务清单

## 已完成 ✅

### 1. 依赖更新
- [x] 更新 Spring AI 版本到 1.1.2
- [x] 更新 bucket4j 依赖
- [x] 添加 Prometheus Metrics 依赖
- [x] 添加 AspectJ 依赖
- [x] 添加 Caffeine Cache 依赖

### 2. 核心服务修复
- [x] 修复 LlmService - 适配新 API
- [x] 修复 AcpProtocolImpl - 移除 ChatResponse 依赖
- [x] 修复 MetricsConfig - 更新 Prometheus 包路径
- [x] 修复 MetricsController - 更新 Prometheus 包路径
- [x] 修复 ConfigController - 修复重复方法名
- [x] 创建 GatewayProperties 配置类

## 待修复 🔧

### 3. Controller 层
- [ ] ChatController - 返回类型不匹配 (ObjectNode vs JsonNode)
- [ ] StatusController - 返回类型不匹配 + 缺少方法
- [ ] GatewayController - 缺少 getNodeCount, getPendingCount, getCompletedCount 方法
- [ ] ChannelController - ChannelId 缺少 name() 和 valueOf() 方法
- [ ] ToolController - ToolExecuteContext 构造器参数不匹配
- [ ] CronController - 缺少 getExecutionStats 方法
- [ ] StreamingController - ChannelAdapter 不是抽象的

### 4. Service 层
- [ ] StreamingMessageService - StreamingMessageRequest 构造器参数不匹配
- [ ] StreamingResponseHandler - AtomicLong 类型转换错误
- [ ] RetryPolicyManager - multiplier 方法不存在
- [ ] GatewayServiceImpl - 内部类未实现接口方法

### 5. Config 层
- [ ] 检查所有配置类是否完整

### 6. 其他模块接口问题
- [ ] WorkQueue 接口 - 缺少 getPendingCount, getCompletedCount, getStats 方法
- [ ] NodeRegistry 接口 - 缺少 getNodeCount, isNodeHealthy 方法
- [ ] SessionStateMachine - 缺少 transition, getCurrentState 方法
- [ ] CronService - 缺少 getExecutionStats 方法
- [ ] GatewayWebSocketHandler - 缺少 getConnectionCount 方法
- [ ] ChannelId - 缺少 name(), valueOf() 方法
- [ ] ToolExecuteContext - 构造器参数不匹配
- [ ] ToolResult - 缺少 output() 方法

### 7. 类型转换问题
- [ ] 修复所有 ObjectNode 到 JsonNode 的返回类型
- [ ] 修复 AtomicLong 到 long 的转换

## 修复优先级

1. **高优先级**: 接口定义（WorkQueue, NodeRegistry, SessionStateMachine 等）
2. **中优先级**: Controller 层修复
3. **低优先级**: Service 层实现细节
