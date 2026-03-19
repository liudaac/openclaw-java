# Java版 OpenClaw 迭代计划

基于原版 OpenClaw (2026.3.11 - 2026.3.13) 更新分析

---

## 📊 当前状态对比

| 模块 | Node.js 原版 | Java 版 | 优先级 |
|------|-------------|---------|--------|
| **Gateway V3 认证** | ✅ 完整 | ✅ 90% | P1 |
| **Cron 定时任务** | ✅ 完整 | ✅ 100% | - |
| **Browser 浏览器** | ✅ 完整 | ✅ 100% | - |
| **Session 持久化** | ✅ 完整 | ✅ 100% | - |
| **流式消息** | ✅ 完整 | ✅ 90% | P2 |
| **Memory 记忆** | ✅ 完整 | ✅ 85% | P2 |
| **Security 安全** | ✅ 完整 | ⚠️ 需更新 | **P0** |
| **Plugin SDK** | ✅ 完整 | ⚠️ 需对齐 | P1 |
| **Dashboard v2** | ✅ 完整 | ❌ 缺失 | P2 |

---

## 🔴 P0 - 安全更新（紧急）

### 1. Exec 审批安全加固

**原版更新**:
- 修复 Ruby `-r`, `--require`, `-I` 审批流程
- 修复 Perl `-M` 和 `-I` 审批流程
- 修复 PowerShell `-File` 和 `-f` 包装器
- 修复 `env` 调度包装器 (macOS)
- 修复反斜杠换行符作为 shell 行继续符
- 修复 macOS 技能自动允许信任绑定

**Java版行动**:
```java
// openclaw-security/src/main/java/openclaw/security/exec/
- ExecApprovalValidator.java (新增)
- ShellCommandParser.java (增强)
- ScriptRunnerDetector.java (新增)
```

### 2. Webhook 安全加固

**原版更新**:
- Feishu webhook: 需要 `encryptKey` + `verificationToken`
- LINE webhook: 空事件 POST 探测也需要签名
- Zalo webhook: 速率限制无效密钥猜测
- Telegram webhook: 验证密钥在读取请求体之前

**Java版行动**:
```java
// 各 channel 的 webhook 处理器
- FeishuWebhookController: 添加 encryptKey 验证
- LineWebhookController: 空事件签名验证
- ZaloWebhookController: 添加速率限制
- TelegramWebhookController: 提前验证 secret
```

### 3. 输入验证强化

**原版更新**:
- 剥离零宽和软连字符标记分割字符
- 规范化兼容性 Unicode
- 剥离不可见格式化码点

**Java版行动**:
```java
// openclaw-security/src/main/java/openclaw/security/guard/
- InputValidator.java (增强)
  - stripZeroWidthChars()
  - stripSoftHyphens()
  - normalizeUnicode()
  - stripInvisibleFormatting()
```

### 4. 设备配对安全

**原版更新**:
- 设置码改为短期引导令牌
- 设备令牌范围限制在批准的基线
- 引导设置码单次使用

**Java版行动**:
```java
// openclaw-gateway/src/main/java/openclaw/gateway/auth/
- DevicePairingService.java (修改)
  - 短期令牌 (5分钟过期)
  - 单次使用验证
  - 范围限制 enforcement
```

---

## 🟠 P1 - 核心功能对齐

### 5. Gateway 改进

**原版更新**:
- 会话重置保留 `lastAccountId` 和 `lastThreadId`
- 网关重启时保持 LaunchAgent 注册
- 添加 `openclaw gateway status --require-rpc`
- 改进 Linux 非交互式守护进程安装失败报告

**Java版行动**:
```java
// openclaw-gateway
- SessionManager.java: 重置时保留路由信息
- GatewayLifecycleService.java: 改进重启逻辑
- GatewayStatusController.java: 添加 requireRpc 参数
```

### 6. Agent 功能增强

**原版更新**:
- 添加 `/fast` 快速模式 (OpenAI/Anthropic)
- Agent 现在最多加载一个根记忆引导文件
- 压缩后触发溢出恢复
- 压缩后重新运行转录修复

**Java版行动**:
```java
// openclaw-agent
- AgentSession.java: 添加 fastMode 支持
- MemoryBootstrapLoader.java: 单文件限制
- CompactionService.java: 溢出恢复 + 转录修复
```

### 7. Plugin SDK 对齐

**原版更新**:
- 移除公共 `openclaw/extension-api` 表面
- 添加 `ChannelMessageActionAdapter.describeMessageTool()`
- 添加上下文引擎的 `delegateCompactionToRuntime()`

**Java版行动**:
```java
// openclaw-plugin-sdk
- 移除 ExtensionApi 类
- MessageActionAdapter.java: 添加 describeMessageTool()
- ContextEngine.java: 添加 delegateCompactionToRuntime()
```

---

## 🟡 P2 - 功能增强

### 8. 流式消息改进

**原版更新**:
- Telegram: 长 HTML 消息分块
- Telegram: 最终预览交付优化
- Discord: 自动线程归档时长配置
- Mattermost: 回复线程模式

**Java版行动**:
```java
// 各 channel 的 streaming 包
- TelegramStreamingService: HTML 分块
- DiscordChannelService: autoArchiveDuration
- MattermostReplyService: replyToMode
```

### 9. Memory 记忆系统

**原版更新**:
- 多模态图像和音频索引 (Gemini)
- `gemini-embedding-2-preview` 支持
- 压缩后内存搜索同步

**Java版行动**:
```java
// openclaw-memory
- MultimodalMemoryIndexer.java (新增)
- GeminiEmbeddingProvider.java: 添加 embedding-2
- MemorySyncService.java: 压缩后同步
```

### 10. Dashboard v2

**原版更新**:
- 全新网关仪表板 (模块化概览、聊天、配置、Agent、会话视图)
- 命令面板
- 移动底部标签
- 丰富聊天工具 (斜杠命令、搜索、导出、固定消息)

**Java版行动**:
```java
// openclaw-server/src/main/java/openclaw/server/dashboard/
- DashboardV2Controller.java (新增)
- CommandPaletteService.java (新增)
- ChatExportService.java (新增)
- PinnedMessageService.java (新增)
```

---

## 📋 实施计划

### 阶段 1: 安全加固 (1-2周)
- [ ] Exec 审批安全更新
- [ ] Webhook 安全加固
- [ ] 输入验证强化
- [ ] 设备配对安全

### 阶段 2: 核心对齐 (2-3周)
- [ ] Gateway 改进
- [ ] Agent 功能增强
- [ ] Plugin SDK 对齐

### 阶段 3: 功能增强 (3-4周)
- [ ] 流式消息改进
- [ ] Memory 记忆系统增强
- [ ] Dashboard v2 开发

---

## 🔗 参考文档

- 原版 CHANGELOG: `/root/openclaw/CHANGELOG.md`
- Java版 README: `/root/openclaw-java/README.md`
- 安全公告: `GHSA-` 系列 (在 CHANGELOG 中标注)

---

*计划创建时间: 2026-03-19*
*基于原版版本: 2026.3.11 - 2026.3.13*
