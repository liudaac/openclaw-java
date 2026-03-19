# 2026-03-19 原版 OpenClaw 更新分析

## 今日关键提交

### 🔴 高优先级更新

#### 1. fix(macos): align exec command parity (#50386)
**提交**: c4a4050ce4
**影响**: macOS 执行命令一致性修复
**Java版行动**: 检查 `openclaw-tools` 中的 exec 实现

#### 2. fix(agents): strip prompt cache for non-OpenAI responses endpoints (#49877)
**提交**: bcc725ffe2
**影响**: 非 OpenAI 兼容端点的 prompt cache 处理
**Java版行动**: 更新 Agent 请求构建逻辑

#### 3. fix(pairing): include shared auth in setup codes
**提交**: 1d3e596021
**影响**: 设备配对设置码包含共享认证
**Java版行动**: 更新 DevicePairingService

#### 4. Session management improvements and dashboard API (#50101)
**提交**: 7b61ca1b06
**影响**: 会话管理改进和 Dashboard API
**Java版行动**: 同步会话管理改进

#### 5. fix: persist outbound sends and skip stale cron deliveries (#50092)
**提交**: a290f5e50f
**影响**: 持久化出站发送，跳过过期 Cron 交付
**Java版行动**: 更新 Cron 交付队列逻辑

#### 6. fix(whatsapp): use globalThis singleton for active-listener Map (#47433)
**提交**: 6ae68faf5f
**影响**: WhatsApp 活跃监听器单例模式
**Java版行动**: 检查 WhatsApp 通道实现

#### 7. Discord: enforce strict DM component allowlist auth (#49997)
**提交**: 0f0cecd2e8
**影响**: Discord DM 组件严格允许列表认证
**Java版行动**: 更新 Discord 安全策略

#### 8. MiniMax: add M2.7 models and update default to M2.7 (#49691)
**提交**: b64f4e313d
**影响**: 添加 MiniMax M2.7 模型，更新默认模型
**Java版行动**: 更新模型配置

---

## 📋 Java 版迭代任务

### 任务 1: Agent Prompt Cache 修复
**文件**: `openclaw-agent/src/main/java/openclaw/agent/core/AgentRequestBuilder.java`

```java
// 修改 buildRequest 方法
public ChatRequest buildRequest(List<Message> messages, List<Tool> tools) {
    ChatRequest.Builder builder = ChatRequest.builder()
        .messages(messages)
        .tools(tools);
    
    // 对于非 OpenAI 兼容端点，移除 prompt cache 相关字段
    if (!isOpenAICompatibleEndpoint()) {
        // 不设置 promptCacheKey 和 promptCacheRetention
        builder.promptCacheKey(null)
               .promptCacheRetention(null);
    }
    
    return builder.build();
}
```

### 任务 2: 设备配对共享认证
**文件**: `openclaw-gateway/src/main/java/openclaw/gateway/auth/DevicePairingService.java`

```java
// 修改 generateSetupCode 方法
public SetupCode generateSetupCode(Device device) {
    String code = generateSecureCode();
    
    // 包含共享认证信息
    SetupCode setupCode = SetupCode.builder()
        .code(code)
        .sharedAuthToken(generateSharedAuthToken(device))
        .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
        .build();
    
    storeSetupCode(setupCode);
    return setupCode;
}
```

### 任务 3: Cron 交付持久化
**文件**: `openclaw-cron/src/main/java/openclaw/cron/delivery/CronDeliveryService.java`

```java
// 新增 Cron 交付持久化逻辑
@Service
public class CronDeliveryService {
    
    @Autowired
    private DeliveryQueueStore queueStore;
    
    public void persistOutboundSend(CronJob job, OutboundMessage message) {
        // 持久化出站发送
        DeliveryRecord record = DeliveryRecord.builder()
            .jobId(job.getId())
            .message(message)
            .status(DeliveryStatus.PENDING)
            .createdAt(Instant.now())
            .build();
        
        queueStore.save(record);
    }
    
    public boolean shouldSkipStaleDelivery(CronJob job) {
        // 检查是否过期（超过 1 小时）
        Instant scheduledTime = job.getScheduledTime();
        return Duration.between(scheduledTime, Instant.now()).toHours() > 1;
    }
}
```

### 任务 4: MiniMax 模型更新
**文件**: `openclaw-server/src/main/resources/models/minimax-models.json`

```json
{
  "models": [
    {
      "id": "minimax-m2.7",
      "name": "MiniMax-M2.7",
      "description": "MiniMax M2.7 模型"
    },
    {
      "id": "minimax-m2.7-highspeed",
      "name": "MiniMax-M2.7-HighSpeed",
      "description": "MiniMax M2.7 高速版"
    }
  ],
  "defaultModel": "minimax-m2.7"
}
```

### 任务 5: Discord DM 允许列表
**文件**: `openclaw-channel-discord/src/main/java/openclaw/channel/discord/security/DMAllowlistService.java`

```java
// 新增 DM 允许列表服务
@Service
public class DMAllowlistService {
    
    public boolean isAllowed(User user, Component component) {
        // 严格检查 DM 组件允许列表
        if (!isDMAllowlistEnabled()) {
            return false;
        }
        
        Set<String> allowedUsers = getDMAllowlist();
        return allowedUsers.contains(user.getId());
    }
    
    private boolean isDMAllowlistEnabled() {
        return discordConfig.isDmAllowlistEnabled();
    }
}
```

---

## 🔧 实施计划

### Phase 1: 关键修复 (今日完成) ✅
1. [x] Agent Prompt Cache 修复 - `OpenAICompatibleProvider.java`
2. [x] 设备配对共享认证 - `DevicePairingService.java`
3. [x] MiniMax 模型更新 - `MiniMaxProvider.java`

### Phase 2: 功能增强 (今日完成) ✅
4. [x] Cron 交付持久化 - `CronDeliveryService.java`
5. [x] Discord DM 允许列表 - `DMAllowlistService.java`
6. [ ] 会话管理改进 (待实施)

### Phase 3: 测试验证 (明日)
7. [ ] 单元测试
8. [ ] 集成测试
9. [ ] 回归测试

## ✅ 今日完成

### 提交 1: fix(agents): strip prompt cache for non-OpenAI responses endpoints
- **文件**: `openclaw-tools/src/main/java/openclaw/tools/llm/OpenAICompatibleProvider.java`
- **改动**: 添加 `isOpenAICompatibleEndpoint()` 检查，为非 OpenAI 端点移除 prompt cache 字段
- **原版**: bcc725ffe2 (#49877)

### 提交 2: feat(gateway): add device pairing service with shared auth
- **文件**: `openclaw-gateway/src/main/java/openclaw/gateway/auth/DevicePairingService.java` (新增)
- **改动**: 创建设备配对服务，包含共享认证令牌、单次使用验证、自动过期清理
- **原版**: 1d3e596021

### 提交 3: feat(models): add MiniMax M2.7 models and update default
- **文件**: `openclaw-tools/src/main/java/openclaw/tools/llm/MiniMaxProvider.java`
- **改动**: 添加 minimax-m2.7 和 minimax-m2.7-highspeed 模型，更新默认模型
- **原版**: b64f4e313d (#49691)

### 提交 4: feat(cron): persist outbound sends and skip stale deliveries
- **文件**: 
  - `openclaw-cron/src/main/java/openclaw/cron/delivery/CronDeliveryService.java` (新增)
  - `openclaw-cron/src/main/java/openclaw/cron/model/CronJob.java`
- **改动**: 添加 Cron 交付持久化服务，跳过超过1小时的过期交付
- **原版**: a290f5e50f (#50092)

### 提交 5: feat(discord): enforce strict DM component allowlist auth
- **文件**: `openclaw-channel-discord/src/main/java/openclaw/channel/discord/security/DMAllowlistService.java` (新增)
- **改动**: 添加 Discord DM 组件允许列表服务，严格验证用户权限
- **原版**: 0f0cecd2e8 (#49997)

---

*分析时间: 2026-03-19*
*基于原版提交: c4a4050ce4 及之前*
