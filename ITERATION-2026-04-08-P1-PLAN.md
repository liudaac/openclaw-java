# Java版 OpenClaw P1 迭代计划 - 2026-04-08

基于原版 OpenClaw 近三天更新分析，继续处理 P1 优先级任务。

## P1 优先级任务

### 任务1: HTTP 404 错误分类用于模型回退链
**目标**: 同步原版 HTTP 404 错误分类功能
**提交**: `de2182877a`

#### 1.1 更新错误分类逻辑
- 在 Pi Embedded Runner 中识别 HTTP 404 错误
- 将 404 错误分类为可回退错误类型
- 保留上下文溢出时的 404 处理

#### 1.2 影响模块
- `openclaw-agent` - 错误处理

---

### 任务2: Cron JobId 加载路径规范化
**目标**: 同步原版 Cron JobId 规范化
**提交**: `242b2e66f2`

#### 2.1 更新 Cron Store
- 规范化 jobId 到 id
- 遗留 jobId 警告
- 测试覆盖

#### 2.2 影响模块
- `openclaw-cron` - Store 服务

---

### 任务3: Telegram 长消息分割恢复
**目标**: 同步原版 Telegram 长消息分割
**提交**: `e79e25667a`

#### 3.1 更新 Telegram Outbound
- 恢复长消息自动分割
- 保持消息完整性

#### 3.2 影响模块
- `openclaw-channel-telegram`

---

### 任务4: Matrix 邀请自动加入提示
**目标**: 同步原版 Matrix 邀请处理
**提交**: `9fd47a5aed`

#### 4.1 更新 Matrix Onboarding
- 引导期间提示邀请自动加入
- 配置更新支持

#### 4.2 影响模块
- `openclaw-channel-matrix`

---

### 任务5: HTTP Gateway 客户端断开中止
**目标**: 同步原版 HTTP Gateway 客户端断开处理
**提交**: `aad3bbebdd`

#### 5.1 更新 Gateway HTTP 处理
- 客户端断开时中止正在运行的 Agent 命令
- 支持 /v1/chat/completions 和 /v1/responses
- 信号处理优化

#### 5.2 影响模块
- `openclaw-gateway` - HTTP 处理
- `openclaw-server` - 控制器

---

## 实施步骤

1. **第一步**: HTTP 404 错误分类（任务1）
2. **第二步**: Cron JobId 规范化（任务2）
3. **第三步**: Telegram 长消息分割（任务3）
4. **第四步**: Matrix 邀请处理（任务4）
5. **第五步**: HTTP Gateway 断开处理（任务5）

---

## 变更影响评估

| 模块 | 变更类型 | 影响程度 |
|------|----------|----------|
| openclaw-agent | 错误处理 | 中 |
| openclaw-cron | 数据兼容 | 中 |
| openclaw-channel-telegram | 消息处理 | 低 |
| openclaw-channel-matrix | 邀请处理 | 低 |
| openclaw-gateway | HTTP 处理 | 中 |
| openclaw-server | 控制器 | 中 |

---

*计划创建时间: 2026-04-08*
