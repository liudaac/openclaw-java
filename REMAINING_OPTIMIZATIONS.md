# OpenClaw Java 版待优化清单

**更新时间**: 2026-03-13  
**已完成**: Cron ✅, Browser (核心) ✅  
**总体完成度**: ~75% → ~85%

---

## 🔴 P0 - 重要待优化 (影响生产使用)

### 1. Session 持久化 ⏳

| 功能 | Node.js | Java 现状 | 差距 |
|------|---------|-----------|------|
| 会话持久化 | ✅ JSONL | ⚠️ 内存 | 重启丢失 |
| 会话恢复 | ✅ 完整 | ❌ 缺失 | 无法恢复 |
| 历史搜索 | ✅ 完整 | ⚠️ 基础 | 功能受限 |

**需要实现**:
- SQLite 会话存储
- 会话恢复机制
- 历史搜索接口

**文件位置**: `openclaw-agent/src/main/java/openclaw/agent/session/`

---

### 2. Channel 流式消息 ⏳

| 功能 | Node.js | Java 现状 | 差距 |
|------|---------|-----------|------|
| 流式响应 | ✅ 完整 | ⚠️ 基础 | 体验降级 |
| 消息分块 | ✅ 自动 | ⚠️ 手动 | 需优化 |
| 打字指示器 | ✅ 支持 | ❌ 缺失 | 交互缺失 |

**需要实现**:
- `ChannelStreamingAdapter` 接口
- WebFlux 流式响应
- 消息自动分块

**文件位置**: `openclaw-server/src/main/java/openclaw/server/channels/`

---

## 🟡 P1 - 建议优化 (提升体验)

### 3. Gateway 协议完善

| 功能 | Node.js | Java 现状 | 差距 |
|------|---------|-----------|------|
| 设备认证 V3 | ✅ 完整 | ⚠️ 基础 | 安全降级 |
| TLS 指纹验证 | ✅ 完整 | ❌ 缺失 | 安全缺失 |
| 自动重连 | ✅ 指数退避 | ⚠️ 基础 | 稳定性差 |
| 协议协商 | ✅ 完整 | ⚠️ 基础 | 兼容性差 |

**文件位置**: `openclaw-gateway/src/main/java/openclaw/gateway/`

---

### 4. Browser 高级功能 (可选)

| 功能 | Node.js | Java 现状 | 优先级 |
|------|---------|-----------|--------|
| Chrome 扩展中继 | ✅ 完整 | ❌ 缺失 | P2 |
| 网络拦截 | ✅ 完整 | ❌ 缺失 | P2 |
| 移动端模拟 | ✅ 完整 | ❌ 缺失 | P3 |
| 视频录制 | ✅ 支持 | ❌ 缺失 | P3 |

**文件位置**: `openclaw-browser/src/main/java/openclaw/browser/`

---

### 5. 工具完善

| 工具 | Node.js | Java 现状 | 差距 |
|------|---------|-----------|------|
| Web Search | ✅ 多提供商 | ⚠️ 接口 | 需实现 |
| Browser Tool | ✅ 完整 | ⚠️ CLI | 需重构 |
| Email | ✅ 完整 | ✅ 完整 | ✅ 完成 |

**文件位置**: `openclaw-tools/src/main/java/openclaw/tools/`

---

## 🟢 P2 - 可选优化

### 6. 高级功能

| 功能 | Node.js | Java 现状 | 说明 |
|------|---------|-----------|------|
| Cron WebSocket 通知 | ✅ 有 | ❌ 无 | 实时状态 |
| Cron 分布式调度 | ✅ Redis | ❌ 无 | 集群支持 |
| 子代理管理增强 | ✅ 完整 | ⚠️ 基础 | 资源控制 |
| 记忆关联图 | ✅ 完整 | ⚠️ 基础 | 关系记忆 |

---

## 📊 优先级矩阵

```
影响度
    高 │  Session持久化    Channel流式
       │  ⭐⭐⭐⭐⭐         ⭐⭐⭐⭐⭐
       │
       │  Gateway协议      Browser高级
       │  ⭐⭐⭐⭐           ⭐⭐⭐
       │
       │  工具完善         高级功能
       │  ⭐⭐⭐             ⭐⭐
       └─────────────────────────────────
         低              高    实现难度
```

---

## 🎯 推荐优化顺序

### Phase 1 (本周) - 核心稳定
1. ✅ **Cron 模块** - 已完成
2. ✅ **Browser 核心** - 已完成
3. ⏳ **Session 持久化** - 建议下一步

### Phase 2 (下周) - 体验提升
4. ⏳ **Channel 流式消息**
5. ⏳ **Gateway 协议完善**

### Phase 3 (可选) - 功能增强
6. Browser 扩展/拦截
7. Cron WebSocket/分布式
8. 其他高级功能

---

## 💡 关键决策点

### Session 持久化方案选择

**方案 A: SQLite (推荐)**
- ✅ 与 Cron 一致
- ✅ 轻量，无需额外服务
- ✅ 适合单机部署

**方案 B: Redis**
- ✅ 支持分布式
- ⚠️ 需要额外服务
- ⚠️ 复杂度增加

### Channel 流式实现

**技术选型**:
- Spring WebFlux `Flux<String>`
- Server-Sent Events (SSE)
- WebSocket 流式

---

## 📁 相关文件位置

```
# Session 持久化
openclaw-agent/src/main/java/openclaw/agent/session/
├── SessionManager.java
├── SessionPersistence.java      # 待创建
└── SQLiteSessionStore.java      # 待创建

# Channel 流式
openclaw-server/src/main/java/openclaw/server/channels/
├── ChannelStreamingAdapter.java # 待创建
└── StreamingMessageService.java # 待创建

# Gateway 完善
openclaw-gateway/src/main/java/openclaw/gateway/
├── protocol/
│   └── ProtocolNegotiator.java  # 待完善
├── auth/
│   └── DeviceAuthV3.java        # 待实现
└── reconnect/
    └── ExponentialBackoff.java  # 待实现
```

---

## ✅ 当前状态总结

| 模块 | 完成度 | 状态 |
|------|--------|------|
| Cron | 100% | ✅ 生产就绪 |
| Browser 核心 | 80% | ✅ 可用 |
| Session 管理 | 60% | ⏳ 需持久化 |
| Channel 路由 | 70% | ⏳ 需流式 |
| Gateway | 75% | ⏳ 需完善 |
| Memory | 85% | ✅ 基本可用 |
| Tools | 70% | ⏳ 需完善 |

**总体**: 从 ~65% 提升到 ~85%，核心功能已具备生产条件。
