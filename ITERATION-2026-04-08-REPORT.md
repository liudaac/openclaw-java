# Java版 OpenClaw 迭代报告 - 2026-04-08

## 迭代概述

本次迭代同步了原版 OpenClaw 近三天（2026-04-05 至 2026-04-08）的关键更新，重点处理 P0 优先级的功能修复。

---

## 已完成的变更

### 1. Agent 配置扩展 ✅

#### 1.1 AgentConfig 新增字段
**文件**: `openclaw-agent/src/main/java/openclaw/agent/config/AgentConfig.java`

新增字段：
- `systemPromptOverride`: 可选的完整系统提示覆盖，用于提示调试和受控实验

**变更详情**:
```java
// 新增字段
String systemPromptOverride

// Builder 新增方法
public Builder systemPromptOverride(String systemPromptOverride)
```

#### 1.2 HeartbeatConfig 新增字段
**文件**: `openclaw-agent/src/main/java/openclaw/agent/heartbeat/HeartbeatConfig.java`

新增字段：
- `includeSystemPromptSection`: 控制是否在系统提示中包含 Heartbeats 部分

**变更详情**:
```java
// 新增字段
private boolean includeSystemPromptSection = true;

// 新增方法
public boolean isIncludeSystemPromptSection()
public void setIncludeSystemPromptSection(boolean includeSystemPromptSection)
```

**配置说明**:
- 当 `includeSystemPromptSection = true` 时，包含默认的 ## Heartbeats 系统提示部分
- 当 `includeSystemPromptSection = false` 时，保留 Heartbeat 运行时行为，但省略 Heartbeat 提示指令

---

### 2. Subagent LightContext 支持 ✅

#### 2.1 AcpProtocol.SpawnRequest 扩展
**文件**: `openclaw-agent/src/main/java/openclaw/agent/AcpProtocol.java`

新增字段：
- `lightContext`: 是否使用轻量级上下文模式（省略完整对话历史）

**变更详情**:
```java
// SpawnRequest 新增字段
boolean lightContext

// Builder 新增方法
public Builder lightContext(boolean lightContext)
```

#### 2.2 SubagentSpawner 更新
**文件**: `openclaw-agent/src/main/java/openclaw/agent/spawn/SubagentSpawner.java`

- 扩展 `SpawnOptions` 支持 `lightContext` 字段
- 添加 `SpawnOptions.Builder` 构建器模式

**变更详情**:
```java
// SpawnOptions 新增字段
boolean lightContext

// 新增 Builder
public static Builder builder()
```

#### 2.3 DefaultSubagentSpawner 更新
**文件**: `openclaw-agent/src/main/java/openclaw/agent/spawn/DefaultSubagentSpawner.java`

- 在创建 SpawnRequest 时传递 lightContext 选项

---

### 3. Memory 模块配置扩展 ✅

#### 3.1 MemoryConfig 新增配置
**文件**: `openclaw-memory/src/main/java/openclaw/memory/config/MemoryConfig.java`

新增配置类：

**SlotConfig** - 内存槽配置：
```java
public static class SlotConfig {
    private String defaultSlot = "default";
    private boolean slotAwarePaths = true;
}
```

**DreamingConfig** - Dreaming 配置：
```java
public static class DreamingConfig {
    private boolean enabled = true;
    private boolean respectMemorySlot = true;
    private String ingestionMode = "daily"; // "daily", "realtime", "disabled"
}
```

**配置说明**:
- `slotAwarePaths`: 启用 slot-aware 配置路径
- `respectMemorySlot`: Dreaming 配置尊重内存槽设置
- `ingestionMode`: 会话摄取模式（daily/realtime/disabled）

---

## 同步的原版提交

| 提交 | 描述 | 同步状态 |
|------|------|----------|
| `a3b2fdf7d6` | feat(agents): add prompt override and heartbeat controls | ✅ 已同步 |
| `f1b7dd6c0a` | fix: honor lightContext in spawned subagents | ✅ 已同步 |
| `733063e31c` | fix: slot-aware dreaming config paths | ✅ 已同步 (配置准备) |
| `9dda94c0f7` | fix(memory): respect memory slot in dreaming config | ✅ 已同步 (配置准备) |

---

## 待后续处理的任务

### P0 剩余任务

1. **ACP Discord 恢复和重置流程** (`f6124f3e17`)
   - 需要更深入的 ACP 模块重构
   - 涉及 Gateway 服务路由变更
   - 建议单独迭代处理

2. **Memory Wiki Belief-Layer Digests** (`947a43dae3`)
   - 需要完整的 Memory Wiki 功能实现
   - 涉及大量新功能开发
   - 建议单独迭代处理

### P1 任务（建议后续迭代）

- HTTP 404 错误分类用于模型回退链
- Cron JobId 加载路径规范化
- Telegram 长消息分割恢复
- Matrix 邀请自动加入提示
- Discord 语音接收恢复
- Slack 媒体传输保护

---

## 测试建议

### 单元测试

1. **AgentConfigTest**: 验证 systemPromptOverride 字段序列化/反序列化
2. **HeartbeatConfigTest**: 验证 includeSystemPromptSection 配置加载
3. **SpawnRequestTest**: 验证 lightContext 字段传递
4. **MemoryConfigTest**: 验证 SlotConfig 和 DreamingConfig 配置加载

### 集成测试

1. **SubagentSpawnerIntegrationTest**: 验证 lightContext 在子代理创建中的行为
2. **AgentHeartbeatIntegrationTest**: 验证 Heartbeat 配置对系统提示的影响

---

## 配置示例

### application.yml 配置

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
```

---

## 变更统计

| 模块 | 文件数 | 变更类型 |
|------|--------|----------|
| openclaw-agent | 4 | 配置扩展、协议扩展 |
| openclaw-memory | 1 | 配置扩展 |
| **总计** | **5** | - |

---

## 兼容性说明

- 所有新增字段都有默认值，向后兼容
- `systemPromptOverride` 默认为 null，不影响现有行为
- `includeSystemPromptSection` 默认为 true，保持现有行为
- `lightContext` 默认为 false，保持现有行为
- `respectMemorySlot` 默认为 true，启用新功能

---

## 提交信息

建议 Git 提交信息：
```
feat: sync OpenClaw updates - Agent config, Subagent lightContext, Memory slot config

- Add systemPromptOverride to AgentConfig
- Add includeSystemPromptSection to HeartbeatConfig
- Add lightContext support to SubagentSpawner and AcpProtocol
- Add SlotConfig and DreamingConfig to MemoryConfig

Syncs from upstream:
- a3b2fdf7d6: feat(agents): add prompt override and heartbeat controls
- f1b7dd6c0a: fix: honor lightContext in spawned subagents
- 733063e31c: fix: slot-aware dreaming config paths
- 9dda94c0f7: fix(memory): respect memory slot in dreaming config
```

---

*报告生成时间: 2026-04-08 11:30 GMT+8*
