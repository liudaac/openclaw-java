# Java版 OpenClaw 迭代计划 - 2026-04-08

基于原版 OpenClaw 近三天更新分析，制定以下迭代计划。

## P0 优先级任务

### 任务1: Agent 配置新增字段支持
**目标**: 同步原版 Agents Prompt Override 和 Heartbeat 控制功能
**提交**: `a3b2fdf7d6`

#### 1.1 更新 AgentConfig
- 新增 `systemPromptOverride` 字段
- 支持完整系统提示覆盖

#### 1.2 更新 HeartbeatConfig
- 新增 `includeSystemPromptSection` 字段
- 控制是否包含 Heartbeats 系统提示部分

### 任务2: Subagent LightContext 支持
**目标**: 同步原版 Subagent LightContext 支持
**提交**: `f1b7dd6c0a`

#### 2.1 更新 AcpProtocol.SpawnRequest
- 新增 `lightContext` 字段
- 防止 ACP 误用 lightContext

#### 2.2 更新 SubagentSpawner
- 支持 lightContext 选项

### 任务3: Memory 模块基础结构准备
**目标**: 为后续 Memory Wiki 功能做准备
**提交**: `947a43dae3`, `733063e31c`, `9dda94c0f7`

#### 3.1 新增 MemorySlot 配置支持
- 在 MemoryConfig 中新增 slot 相关配置
- 支持 dreaming slot-aware 配置路径

### 任务4: ACP Discord 恢复流程准备
**目标**: 为 ACP Discord 恢复和重置流程做准备
**提交**: `f6124f3e17`

#### 4.1 更新 ACP 模块结构
- 准备支持 ACPX 0.5.2 的接口变更
- 支持绑定重置通过 Gateway 服务路由

---

## 实施步骤

1. **第一步**: 更新 AgentConfig 和 HeartbeatConfig（任务1）
2. **第二步**: 更新 AcpProtocol 和 SubagentSpawner（任务2）
3. **第三步**: 准备 Memory 模块结构（任务3）
4. **第四步**: 更新 ACP 模块接口（任务4）

---

## 变更影响评估

| 模块 | 变更类型 | 影响程度 |
|------|----------|----------|
| openclaw-agent | 配置扩展 | 中 |
| openclaw-session | 协议扩展 | 中 |
| openclaw-memory | 结构准备 | 低 |
| openclaw-acp | 接口扩展 | 中 |

---

## 测试要求

1. AgentConfig 新增字段序列化/反序列化测试
2. HeartbeatConfig 新增字段配置加载测试
3. SubagentSpawner lightContext 支持测试
4. AcpProtocol SpawnRequest 扩展测试

---

*计划创建时间: 2026-04-08*
