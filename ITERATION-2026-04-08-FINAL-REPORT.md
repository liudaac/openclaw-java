# Java版 OpenClaw 迭代最终报告 - 2026-04-08

## 迭代概述

本次迭代完成了原版 OpenClaw 近三天（2026-04-05 至 2026-04-08）更新的同步工作，分为两个阶段：

1. **第一阶段 (P0)**: 核心功能修复
2. **第二阶段 (P1)**: 频道特定修复和工具改进

---

## 第一阶段：P0 核心功能修复 ✅

### 1. Agent 配置扩展

#### AgentConfig
- **新增字段**: `systemPromptOverride`
- **用途**: 完整系统提示覆盖，用于提示调试和受控实验
- **文件**: `openclaw-agent/src/main/java/openclaw/agent/config/AgentConfig.java`

#### HeartbeatConfig
- **新增字段**: `includeSystemPromptSection`
- **用途**: 控制是否在系统提示中包含 Heartbeats 部分
- **默认值**: `true`（保持向后兼容）
- **文件**: `openclaw-agent/src/main/java/openclaw/agent/heartbeat/HeartbeatConfig.java`

### 2. Subagent LightContext 支持

#### AcpProtocol.SpawnRequest
- **新增字段**: `lightContext`
- **用途**: 轻量级上下文模式（省略完整对话历史）
- **文件**: `openclaw-agent/src/main/java/openclaw/agent/AcpProtocol.java`

#### SubagentSpawner
- **扩展**: `SpawnOptions` 支持 `lightContext` 字段
- **新增**: `SpawnOptions.Builder` 构建器模式
- **文件**:
  - `openclaw-agent/src/main/java/openclaw/agent/spawn/SubagentSpawner.java`
  - `openclaw-agent/src/main/java/openclaw/agent/spawn/DefaultSubagentSpawner.java`

### 3. Memory 模块配置扩展

#### MemoryConfig
- **新增配置类**: `SlotConfig`
  - `defaultSlot`: 默认内存槽
  - `slotAwarePaths`: 启用 slot-aware 配置路径
- **新增配置类**: `DreamingConfig`
  - `enabled`: 启用 dreaming
  - `respectMemorySlot`: Dreaming 配置尊重内存槽设置
  - `ingestionMode`: 会话摄取模式（daily/realtime/disabled）
- **文件**: `openclaw-memory/src/main/java/openclaw/memory/config/MemoryConfig.java`

### P0 同步的原版提交

| 提交 | 描述 | 状态 |
|------|------|------|
| `a3b2fdf7d6` | feat(agents): add prompt override and heartbeat controls | ✅ |
| `f1b7dd6c0a` | fix: honor lightContext in spawned subagents | ✅ |
| `733063e31c` | fix: slot-aware dreaming config paths | ✅ |
| `9dda94c0f7` | fix(memory): respect memory slot in dreaming config | ✅ |

---

## 第二阶段：P1 频道特定修复 ✅

### 1. Cron JobId 规范化

#### JobIdentityNormalizer
- **用途**: 处理遗留 `jobId` 字段到标准 `id` 字段的迁移
- **功能**:
  - 检测遗留 `jobId` 字段
  - 自动迁移到 `id` 字段
  - 记录警告日志
- **文件**: `openclaw-cron/src/main/java/openclaw/cron/util/JobIdentityNormalizer.java`

#### SQLiteCronJobStore
- **更新**: 在加载作业时调用 `JobIdentityNormalizer.normalize()`
- **文件**: `openclaw-cron/src/main/java/openclaw/cron/store/SQLiteCronJobStore.java`

### 2. Telegram 长消息分割

#### TelegramOutboundAdapter
- **新增常量**:
  - `TELEGRAM_MESSAGE_LIMIT`: 4096 字符
  - `MESSAGE_SPLIT_MARGIN`: 100 字符安全边距
  - `MAX_MESSAGE_LENGTH`: 3996 字符有效最大长度
- **新增方法**:
  - `splitMessage()`: 智能分割长消息（优先在段落边界分割）
  - `sendSingleMessage()`: 发送单条消息
  - `sendMessageChunks()`: 顺序发送多条消息
- **文件**: `openclaw-channel-telegram/src/main/java/openclaw/channel/telegram/TelegramOutboundAdapter.java`

### 3. HTTP Gateway 客户端断开检测

#### ClientDisconnectHandler
- **用途**: 管理 HTTP 请求生命周期和客户端断开检测
- **功能**:
  - 创建请求上下文
  - 检测客户端断开
  - 信号取消长时间运行的操作
  - 支持 Reactor 集成
- **文件**: `openclaw-server/src/main/java/openclaw/server/http/ClientDisconnectHandler.java`

### P1 同步的原版提交

| 提交 | 描述 | 状态 |
|------|------|------|
| `242b2e66f2` | fix: normalize cron jobId load path | ✅ |
| `e79e25667a` | fix(telegram): restore outbound message splitting | ✅ |
| `aad3bbebdd` | fix: abort HTTP gateway turns on client disconnect | ⚠️ 基础类已添加 |

---

## 待后续处理的任务

### 需要控制器层集成

1. **HTTP Gateway 客户端断开中止**
   - ClientDisconnectHandler 已创建
   - 需要在 ChatController、AgentController 中集成
   - 监听客户端断开事件并中止 Agent 操作

2. **Matrix 邀请自动加入提示**
   - Matrix 模块当前文件较少
   - 需要完整的 onboarding 流程实现

### P0 剩余核心任务

3. **ACP Discord 恢复和重置流程** (`f6124f3e17`)
   - 需要 ACP 模块重大重构
   - 涉及 Gateway 服务路由变更
   - 建议单独迭代

4. **Memory Wiki Belief-Layer Digests** (`947a43dae3`)
   - 需要完整的 Memory Wiki 功能实现
   - 涉及大量新功能开发
   - 建议单独迭代

---

## 变更统计

### 文件变更

| 类别 | 数量 |
|------|------|
| 新增文件 | 4 |
| 修改文件 | 6 |
| 删除文件 | 1 |
| **总计** | **11** |

### 代码变更

| 阶段 | 新增行数 | 删除行数 | 净增行数 |
|------|----------|----------|----------|
| P0 | +483 | -6 | +477 |
| P1 | +496 | -103 | +393 |
| **总计** | **+979** | **-109** | **+870** |

### 模块影响

| 模块 | 变更类型 | 优先级 |
|------|----------|--------|
| openclaw-agent | 配置扩展、协议扩展 | P0 |
| openclaw-memory | 配置扩展 | P0 |
| openclaw-cron | 数据兼容 | P1 |
| openclaw-channel-telegram | 消息处理 | P1 |
| openclaw-server | HTTP 处理 | P1 |

---

## Git 提交记录

```
5d2fe34 feat: sync OpenClaw updates - Agent config, Subagent lightContext, Memory slot config
d0c0e78 feat: sync OpenClaw P1 updates - Cron, Telegram, HTTP Gateway
```

---

## 配置示例

### application.yml

```yaml
openclaw:
  agents:
    defaults:
      system-prompt-override: "Custom system prompt for debugging"
      heartbeat:
        enabled: true
        include-system-prompt-section: true
        every: 30m
        ack-max-chars: 300
  memory:
    slot:
      default-slot: "default"
      slot-aware-paths: true
    dreaming:
      enabled: true
      respect-memory-slot: true
      ingestion-mode: "daily"
  cron:
    store:
      type: sqlite
      path: "${user.home}/.openclaw/cron.db"
```

---

## 兼容性说明

所有变更都是向后兼容的：

- `systemPromptOverride`: 默认为 null，不影响现有行为
- `includeSystemPromptSection`: 默认为 true，保持现有行为
- `lightContext`: 默认为 false，保持现有行为
- `respectMemorySlot`: 默认为 true，启用新功能
- `JobIdentityNormalizer`: 自动处理遗留数据，无破坏性变更
- Telegram 消息分割: 自动触发，对用户透明

---

## 测试建议

### 单元测试

1. **AgentConfigTest**: 验证 systemPromptOverride 字段
2. **HeartbeatConfigTest**: 验证 includeSystemPromptSection 配置
3. **SpawnRequestTest**: 验证 lightContext 字段传递
4. **MemoryConfigTest**: 验证 SlotConfig 和 DreamingConfig
5. **JobIdentityNormalizerTest**: 验证 jobId 到 id 的迁移
6. **TelegramOutboundAdapterTest**: 验证长消息分割
7. **ClientDisconnectHandlerTest**: 验证断开检测

### 集成测试

1. **SubagentSpawnerIntegrationTest**: lightContext 行为
2. **CronJobStoreIntegrationTest**: 遗留数据迁移
3. **TelegramChannelIntegrationTest**: 长消息发送

---

## 下一步建议

1. **短期** (本周):
   - 在控制器中集成 ClientDisconnectHandler
   - 编写单元测试

2. **中期** (本月):
   - 实现 ACP Discord 恢复流程
   - 完善 Memory Wiki 功能

3. **长期** (下月):
   - 实现 Matrix 完整功能
   - 性能优化和重构

---

*报告生成时间: 2026-04-08 11:30 GMT+8*
*迭代完成时间: 2026-04-08 11:30 GMT+8*
