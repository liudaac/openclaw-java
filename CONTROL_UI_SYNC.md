# Control UI 同步指南

## 概述

原版 OpenClaw 的 Control UI 是一个独立的 Vite + Lit 项目，位于 `openclaw/ui/`。
Java 版将其构建后的静态资源放在 `openclaw-server/src/main/resources/static/control-ui/`。

## 同步方法

### 方法 1: 使用同步脚本（推荐）

```bash
# 从默认路径 (/root/openclaw) 同步
./scripts/sync-control-ui.sh

# 或指定原版路径
./scripts/sync-control-ui.sh /path/to/openclaw
```

### 方法 2: 手动同步

```bash
# 1. 在原版中构建 UI
cd /path/to/openclaw/ui
npm install
npm run build

# 2. 备份旧的静态资源
mv openclaw-server/src/main/resources/static/control-ui \
   openclaw-server/src/main/resources/static/control-ui.backup.$(date +%Y%m%d)

# 3. 复制新的静态资源
mkdir -p openclaw-server/src/main/resources/static/control-ui
cp -r /path/to/openclaw/ui/dist/* openclaw-server/src/main/resources/static/control-ui/

# 4. 修复路径
sed -i 's|src="/|src="./|g' openclaw-server/src/main/resources/static/control-ui/index.html
sed -i 's|href="/|href="./|g' openclaw-server/src/main/resources/static/control-ui/index.html
```

## 近期重要变更 (2026.3.20 - 2026.3.23)

### 1. Gateway 连接流程重构 (44bbd2d)
- **文件**: `ui/src/ui/gateway.ts`
- **变更**: 拆分 Gateway 连接流程，改进错误处理
- **影响**: Java 版 Gateway 服务端需要相应更新

### 2. 多会话选择和删除 (36c6d44)
- **文件**: `ui/src/ui/controllers/sessions.ts`, `ui/src/ui/views/sessions.ts`
- **变更**: 
  - 新增 `deleteSessionsAndRefresh` 函数
  - 支持批量删除会话
  - 改进移动端响应式布局
- **Java 版 API**: 需要实现 `sessions.delete` 批量删除接口

### 3. 会话标签去重 (1ad3893)
- **文件**: `ui/src/ui/controllers/sessions.ts`
- **变更**: 处理重复的 Agent 会话标签
- **影响**: 会话列表显示逻辑

### 4. 使用统计改进 (14237aa, a5309b6)
- **文件**: `ui/src/ui/app-render-usage-tab.ts`
- **变更**: 
  - 改进使用概览样式和本地化
  - 移除空的详情占位状态
- **影响**: 仅 UI 样式，无需 Java 端变更

### 5. 上下文使用统计修复 (7c520cc, 6db6e1)
- **文件**: `ui/src/ui/views/chat.ts`
- **变更**: 修复聊天通知中的 Token 统计
- **Java 版 API**: 确保 `sessions.context` 返回正确的 usage 数据

## Java 版需要同步的 API 变更

### 新增/修改的 Gateway 方法

| 方法 | 变更 | Java 版状态 |
|------|------|-------------|
| `sessions.delete` | 支持批量删除（数组参数） | ❓ 需检查 |
| `sessions.context` | 返回 usage 统计 | ❓ 需检查 |
| `gateway.connect` | 流程重构 | ✅ 已同步 DI |

### 需要检查的 Java 类

```java
// 会话管理
openclaw-server/src/main/java/openclaw/server/controller/SessionController.java

// Gateway 处理
openclaw-server/src/main/java/openclaw/server/websocket/GatewayWebSocketHandler.java
```

## 版本兼容性

| UI 版本 | Java 后端要求 | 状态 |
|---------|--------------|------|
| 2026.3.23 | Gateway DI + 批量删除 API | 🔄 需同步 |
| 2026.3.22 | Gateway V3 认证 | ✅ 兼容 |
| 2026.3.20 | Session 持久化 | ✅ 兼容 |

## 同步检查清单

- [ ] 运行 `./scripts/sync-control-ui.sh` 构建并复制 UI
- [ ] 检查 `index.html` 路径是否正确（相对路径 `./`）
- [ ] 启动 Java 服务器测试 UI 功能
- [ ] 测试会话列表加载
- [ ] 测试批量删除功能（如果 UI 支持）
- [ ] 测试 Gateway 连接
- [ ] 提交静态资源变更

## 注意事项

1. **路径修复**: 构建后的 `index.html` 使用绝对路径 `/`，需要改为相对路径 `./`
2. **API 兼容性**: UI 可能依赖新的 Gateway API，需要同步实现
3. **缓存问题**: 浏览器可能缓存旧版本，建议强制刷新或清缓存测试
4. **版本匹配**: 建议 UI 和 Java 后端版本保持一致

## 相关文件

- 原版 UI: `/root/openclaw/ui/`
- Java 静态资源: `openclaw-server/src/main/resources/static/control-ui/`
- 同步脚本: `scripts/sync-control-ui.sh`
