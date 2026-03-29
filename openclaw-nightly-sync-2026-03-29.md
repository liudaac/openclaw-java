# OpenClaw Nightly Sync Analysis Report

**分析日期**: 2026-03-29  
**原版提交范围**: 2026-03-25 至 2026-03-29  
**原版仓库**: `/root/openclaw/`  
**Java版仓库**: `/root/openclaw-java/`  

---

## 执行摘要

本次分析覆盖了原版OpenClaw从2026-03-25至2026-03-29期间的所有提交。共分析了 **~100个提交**，识别出 **15个关键变更** 需要评估同步到Java版。

### 关键发现

| 类别 | 数量 | 说明 |
|------|------|------|
| P0 关键变更 | 3 | Gateway任务生命周期、背景任务跟踪、安全修复 |
| P1 重要功能增强 | 5 | Memory FTS5支持、CJK优化、子代理工具策略 |
| P2 用户体验 | 4 | 状态反应、媒体支持、流式响应 |
| P3-P4 维护性 | 3 | 文档、测试、配置优化 |

### 同步建议概览

| 优先级 | 建议同步 | 待评估 | 跳过 |
|--------|----------|--------|------|
| P0 | 2 | 1 | 0 |
| P1 | 3 | 2 | 0 |
| P2 | 2 | 2 | 0 |
| P3-P4 | 1 | 2 | 0 |

---

## 详细变更分析

### P0: 关键功能修复/架构变更

#### 1. Gateway: 背景任务生命周期跟踪 (#52518)

**提交**: `17c36b5093dadf0abffa9d492c61db65394fde36`  
**影响范围**: 
- `src/tasks/task-registry.ts` - 任务注册表核心重构
- `src/acp/control-plane/manager.core.ts` - ACP控制平面管理器
- `src/agents/subagent-registry-lifecycle.ts` - 子代理生命周期
- `src/cli/program/register.status-health-sessions.ts` - CLI状态健康检查

**变更概要**: 
- 新增38个文件，约3500行代码
- 重构任务注册表存储层，抽象存储接口
- 新增背景任务生命周期跟踪
- 新增子代理注册表生命周期管理
- 新增任务维护与协调机制

**Java版评估**: ⚠️ **待评估**
- Java版有 `SubagentSpawner` 接口，但无完整的任务注册表系统
- 需要评估是否已存在等效功能
- **建议**: 检查 `openclaw-agent/src/main/java/openclaw/agent/spawn/` 模块

**同步决策**: 🔶 **待评估** - 需确认Java版任务管理现状

---

#### 2. Gateway: 任务注册表存储抽象 (#56927)

**提交**: `92d0b3a557bccd1a64591b345903a32792fc0e31`  
**影响范围**: 
- `src/tasks/task-registry.store.ts` - 存储层重构
- `src/tasks/task-registry.store.json.ts` - JSON存储实现
- `src/tasks/task-registry.ts` - 注册表核心

**变更概要**: 
- 将存储层抽象为独立接口
- 支持多种存储后端（JSON、内存等）
- 新增存储层单元测试

**Java版评估**: ⚠️ **待评估**
- Java版无独立的任务存储模块
- 需要评估现有会话/任务存储机制
- **建议**: 检查 `openclaw-session` 和 `openclaw-store` 模块

**同步决策**: 🔶 **待评估** - 需评估现有存储架构

---

#### 3. Exec: 安全沙箱失败关闭修复 (#56800)

**提交**: `5d81b64343394bc7b0057131d92bfd9bbd91bf3e`  
**影响范围**: 
- `src/agents/bash-tools.exec.ts` - 执行工具核心
- `src/agents/bash-tools.exec-approval-followup.ts` - 审批后续处理
- `src/security/audit.ts` - 安全审计

**变更概要**: 
- 当沙箱不可用时执行"失败关闭"策略
- 强化审批后续处理的安全检查
- 14个文件变更，约116行代码

**Java版评估**: ✅ **需要同步**
- Java版有 `CommandExecutionTool`，但无沙箱隔离机制
- 当前实现直接执行命令，存在安全风险
- **建议**: 添加沙箱检测和失败关闭逻辑

**同步决策**: 🔴 **需要同步** - 安全关键修复

**实施建议**:
```java
// 在 CommandExecutionTool.java 中添加
private ToolResult checkSandboxAvailability() {
    if (!sandboxAvailable && failClosed) {
        return ToolResult.failure("Sandbox unavailable - execution denied");
    }
    return null;
}
```

---

### P1: 重要功能增强

#### 4. Memory: 可配置FTS5分词器支持CJK (#56707)

**提交**: `3ce48aff660a0dca487fb195132d53e6e0e404ed`  
**影响范围**: 
- `extensions/memory-core/src/memory/manager-search.ts` - 搜索管理器
- `packages/memory-host-sdk/src/host/query-expansion.ts` - 查询扩展
- `src/config/schema.base.generated.ts` - 配置Schema

**变更概要**: 
- 新增FTS5分词器配置选项
- 支持CJK（中日韩）文本的全文搜索
- 12个文件变更，约310行代码

**Java版评估**: ✅ **需要同步**
- Java版有 `MemoryManager` 和搜索功能
- 当前使用SQLite，但未配置FTS5分词器
- **建议**: 在 `openclaw-memory` 模块中添加FTS5配置

**同步决策**: 🔴 **需要同步** - CJK用户关键功能

**实施建议**:
```java
// 在 MemoryConfig.java 中添加
private String ftsTokenizer = "porter"; // 或 "icu" 支持CJK

// 在 SQLiteMemoryStore 中初始化FTS5时
CREATE VIRTUAL TABLE IF NOT EXISTS memories_fts USING fts5(
    content, 
    tokenize='${ftsTokenizer}'
);
```

---

#### 5. Memory: CJK字符Token计数修复 (#39985)

**提交**: `1c8758fbd51d2af197834dc2afdef2bac88f2b93`  
**影响范围**: 
- `src/agents/pi-extensions/context-pruning/pruner.ts` - 上下文修剪
- `src/utils/cjk-chars.ts` - CJK字符工具（新增）

**变更概要**: 
- 新增CJK字符检测和Token估算工具
- 修复CJK文本在上下文修剪时的Token计算错误
- 3个文件变更，约38行代码

**Java版评估**: ✅ **需要同步**
- Java版无CJK字符处理工具
- 上下文长度计算可能不准确
- **建议**: 创建 `CjkCharUtils` 工具类

**同步决策**: 🔴 **需要同步** - CJK用户体验关键

**实施建议**:
```java
// 新增 openclaw.utils.CjkCharUtils
public class CjkCharUtils {
    public static boolean isCjk(char c) {
        return (c >= '\u4e00' && c <= '\u9fff') || // CJK统一汉字
               (c >= '\u3040' && c <= '\u309f') || // 平假名
               (c >= '\u30a0' && c <= '\u30ff');   // 片假名
    }
    
    public static int estimateTokens(String text) {
        // CJK字符约1 token，其他约0.25 token
        return (int) text.chars()
            .mapToDouble(c -> isCjk((char) c) ? 1.0 : 0.25)
            .sum();
    }
}
```

---

#### 6. Memory: CJK字符分块优化 (#40271)

**提交**: `971ecabe80b632180edddb81f5652436de13ffed`  
**影响范围**: 
- `packages/memory-host-sdk/src/host/internal.ts` - 内存分块
- `src/utils/cjk-chars.ts` - CJK工具

**变更概要**: 
- 优化QMD内存系统的分块算法
- 使用加权字符长度进行分块边界决策
- 5个文件变更，约299行代码

**Java版评估**: ✅ **需要同步**
- 与#39985相关，需同时实施
- 影响内存搜索质量

**同步决策**: 🔴 **需要同步** - 与#39985配套

---

#### 7. Agents: 子代理静默回合失败关闭 (#52593)

**提交**: `af694def5b8e1b6ef7a80c2f4c550f9c5bad594c`  
**影响范围**: 
- `src/agents/pi-embedded-subscribe.ts` - 嵌入式订阅
- `src/auto-reply/reply/agent-runner-execution.ts` - 代理执行
- `src/agents/pi-embedded-runner/run/attempt.ts` - 运行尝试

**变更概要**: 
- 当子代理产生静默回合时执行失败关闭
- 防止无限等待无响应的子代理
- 17个文件变更，约156行代码

**Java版评估**: ⚠️ **待评估**
- Java版有子代理支持，但需检查静默处理逻辑
- **建议**: 检查 `SubagentSpawner` 和 `DefaultSubagentSpawner`

**同步决策**: 🔶 **待评估** - 需确认现有子代理行为

---

#### 8. Agents: 允许子代理使用memory_search和memory_get (#55385)

**提交**: `6c85c82ba31843b8cffebd86f3a79176696cda07`  
**影响范围**: 
- `src/agents/pi-tools.policy.ts` - 工具策略

**变更概要**: 
- 将 `memory_search` 和 `memory_get` 从子代理拒绝列表移除
- 这些只读工具对多代理共享内存至关重要
- 1个文件变更，约1行代码

**Java版评估**: ✅ **需要同步**
- Java版需检查工具策略配置
- 当前可能限制子代理使用内存工具

**同步决策**: 🔴 **需要同步** - 多代理功能关键

**实施建议**:
```java
// 在工具策略配置中
public static final Set<String> SUBAGENT_ALLOWED_TOOLS = Set.of(
    "memory_search", "memory_get",  // 允许只读内存工具
    "read", "web_search", "web_fetch"
);
```

---

### P2: 用户体验改进

#### 9. Slack: 状态反应生命周期 (#56430)

**提交**: `cea7162490f4569f1ff210d39f92fd4ba07e29e3`  
**影响范围**: 
- `extensions/slack/src/monitor/message-handler/dispatch.ts` - 消息分发
- `src/channels/status-reactions.ts` - 状态反应接口

**变更概要**: 
- 新增工具/思考进度指示器的状态反应
- 7个文件变更，约617行代码

**Java版评估**: ⚠️ **待评估**
- Java版有Slack通道，但需检查状态反应支持
- **建议**: 检查 `openclaw-channel-slack` 模块

**同步决策**: 🔶 **待评估** - 需确认Slack功能覆盖

---

#### 10. LINE: 出站媒体支持 (#56700)

**提交**: `9449e54f4ffc03df7c7775522b0dfd844249fbff`  
**影响范围**: 
- `extensions/line/src/outbound-media.ts` - 出站媒体（新增）
- `extensions/line/src/send.ts` - 发送逻辑

**变更概要**: 
- 支持图片、视频、音频的出站发送
- 9个文件变更，约727行代码

**Java版评估**: ❌ **跳过**
- Java版无LINE通道模块

**同步决策**: ⚪ **跳过** - 无LINE模块

---

#### 11. Matrix: 草稿流式编辑 (#56387)

**提交**: `7e7e45c2f3db5a86e9be406fca9659d1f864de49`  
**影响范围**: 
- `extensions/matrix/src/matrix/draft-stream.ts` - 草稿流（新增）
- `extensions/matrix/src/matrix/monitor/handler.ts` - 监控处理器
- `extensions/matrix/src/matrix/send.ts` - 发送逻辑

**变更概要**: 
- 支持编辑到位的部分回复（草稿流式）
- 17个文件变更，约1561行代码

**Java版评估**: ⚠️ **待评估**
- Java版有Matrix通道，但功能可能不完整
- **建议**: 检查 `openclaw-channel-matrix` 模块

**同步决策**: 🔶 **待评估** - 需确认Matrix功能覆盖

---

#### 12. TTS: Edge-TTS CJK语音支持 (#52355)

**提交**: `69a0a0edc53ac055ff5c8de06ae422db85cdc007`  
**影响范围**: 
- `extensions/microsoft/speech-provider.ts` - 语音提供商

**变更概要**: 
- 为CJK文本自动选择中文语音
- 3个文件变更，约130行代码

**Java版评估**: ⚠️ **待评估**
- Java版需检查TTS提供商实现
- **建议**: 检查 `openclaw-tools` 中的TTS相关工具

**同步决策**: 🔶 **待评估** - 需确认TTS实现

---

### P3-P4: 维护性/测试/文档

#### 13. Memory: 保持FTS-only索引在重建时 (#42714)

**提交**: `598f539be5e1585e450a324c506ced28d458d0a7`  
**影响范围**: 
- `extensions/memory-core/src/memory/manager-embedding-ops.ts` - 嵌入操作

**变更概要**: 
- 重建索引时保持FTS-only模式
- 4个文件变更，约194行代码

**Java版评估**: ✅ **需要同步**
- 与FTS5支持相关，应一并实施

**同步决策**: 🔴 **需要同步** - 与#56707配套

---

#### 14. Memory: FTS-only搜索无需Provider (#56473)

**提交**: `41c30f0c59012635220bde685468af009da6a60a`  
**影响范围**: 
- `extensions/memory-core/src/memory/manager-embedding-ops.ts` - 嵌入操作
- `extensions/memory-core/src/memory/manager.ts` - 管理器

**变更概要**: 
- 当无嵌入提供商时仍支持FTS搜索
- 5个文件变更，约224行代码

**Java版评估**: ✅ **需要同步**
- 与FTS5支持相关，应一并实施

**同步决策**: 🔴 **需要同步** - 与#56707配套

---

## 同步建议总结

### 高优先级同步 (P0-P1)

| # | 变更 | 优先级 | 影响模块 | 预估工作量 |
|---|------|--------|----------|------------|
| 1 | Exec安全沙箱失败关闭 | P0 | openclaw-tools | 2-3天 |
| 2 | Memory FTS5分词器 | P1 | openclaw-memory | 2-3天 |
| 3 | CJK字符Token计数 | P1 | openclaw-utils (新建) | 1-2天 |
| 4 | CJK字符分块优化 | P1 | openclaw-memory | 1-2天 |
| 5 | 子代理内存工具策略 | P1 | openclaw-agent | 0.5天 |
| 6 | Memory FTS-only重建 | P1 | openclaw-memory | 1天 |
| 7 | Memory FTS-only无Provider | P1 | openclaw-memory | 1天 |

### 待评估变更 (需进一步分析)

| # | 变更 | 优先级 | 需确认内容 |
|---|------|--------|------------|
| 1 | Gateway任务生命周期 | P0 | Java版任务管理现状 |
| 2 | Gateway存储抽象 | P0 | 现有存储架构 |
| 3 | 子代理静默回合 | P1 | 现有子代理行为 |
| 4 | Slack状态反应 | P2 | Slack功能覆盖 |
| 5 | Matrix草稿流式 | P2 | Matrix功能覆盖 |
| 6 | TTS CJK语音 | P2 | TTS实现状态 |

### 跳过变更

| # | 变更 | 原因 |
|---|------|------|
| 1 | LINE出站媒体 | Java版无LINE模块 |

---

## 风险评估

### 高风险

1. **Exec安全沙箱**: Java版当前直接执行命令，无沙箱隔离。需尽快实施失败关闭机制。
2. **Gateway任务系统**: 架构差异大，需仔细评估现有实现。

### 中风险

1. **Memory FTS5**: 需要SQLite FTS5扩展支持，可能涉及原生库依赖。
2. **CJK处理**: 需要全面测试CJK文本的各种场景。

### 低风险

1. **子代理工具策略**: 简单配置变更，风险低。
2. **文档/测试**: 无运行时风险。

---

## 实施建议

### 第一阶段 (本周)

1. **实施Exec安全沙箱失败关闭** - 安全关键
2. **实施子代理内存工具策略** - 简单快速

### 第二阶段 (下周)

1. **实施Memory FTS5分词器支持**
2. **实施CJK字符处理工具**
3. **评估Gateway任务系统**

### 第三阶段 (后续)

1. **评估Slack/Matrix增强功能**
2. **评估TTS CJK支持**

---

## 附录: 完整提交列表

### 2026-03-29 提交

```
5f85c4e Docs: add runtime semantics guidance to AGENTS
ee701d6 build: bump Android version to 2026.3.29
92d0b3a Gateway: abstract task registry storage (#56927)
17c36b5 Gateway: track background task lifecycle (#52518)
270d0c5 fix: avoid telegram plugin self-recursive sdk imports
88ca0b2 fix(status): handle node-only hosts on current main (#56718)
571da81 fix: keep openai-codex on HTTP responses transport
e06069c fix(sandbox): add CJK fonts to browser image (#56905)
4432954 Track ACP sessions_spawn runs and emit ACP lifecycle events (#40885)
a7a89fb fix(ci): retry actionlint release downloads
8119333 fix(acpx): read ACPX_PINNED_VERSION from package.json
5adc50c docs(changelog): add slack status reactions entry
7c50138 fix(plugins): keep built cli metadata scans lightweight
cea7162 feat(slack): status reaction lifecycle (#56430)
e28fdb0 docs: add LINE ACP support and plugin requireApproval hook docs
2899ce5 Update CHANGELOG.md
af694de fix(agents): fail closed on silent turns (#52593)
f897aba docs: add missing feature docs for Matrix E2EE thumbnails
3aac43e docs: remove stale MiniMax M2.5 refs
57882f0 fix(web-search): localize shared search cache (#54040)
4d54376 Tests: stabilize shard-2 queue and channel state
9c185fa Agents: cover subagent memory tool policy
6c85c82 fix: allow memory_search and memory_get in sub-agent sessions
```

### 2026-03-28 提交

```
341e617 docs(plugins): refresh bundled plugin runtime docs
caeeecf refactor(test): centralize bundled channel test roots
8e0ab35 refactor(plugins): decouple bundled plugin runtime loading
1738d54 fix(gateway/auth): local trusted-proxy fallback
9777781 fix(subagents): preserve requester agent for inline announces
...
```

*(完整列表见原版仓库 git log)*

---

*报告生成时间: 2026-03-29 22:45*  
*分析者: OpenClaw Nightly Sync Subagent*  
*基于原版提交范围: 2026-03-25 至 2026-03-29*