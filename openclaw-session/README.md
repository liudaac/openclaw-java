# OpenClaw Session Module

Session persistence and management for OpenClaw.

## 特性

- ✅ **SQLite 持久化** - 会话和消息持久化存储
- ✅ **内存缓存** - 热数据缓存，提升性能
- ✅ **会话恢复** - 重启后自动恢复活跃会话
- ✅ **历史搜索** - 支持会话和消息搜索
- ✅ **统计信息** - Token 使用统计
- ✅ **状态机** - 完整的状态转换管理

## 架构

```
openclaw-session/
├── model/
│   ├── Session.java              # 会话实体
│   ├── Message.java              # 消息实体
│   ├── SessionStatus.java        # 状态枚举
│   └── SessionStatusMachine.java # 状态机
├── store/
│   ├── SessionStore.java         # 存储接口
│   └── SQLiteSessionStore.java   # SQLite 实现
└── service/
    └── SessionPersistenceService.java # 主服务
```

## 状态机

```
PENDING -> ACTIVE -> COMPLETED -> ARCHIVED
    |         |
    |         v
    |      PAUSED
    |         |
    v      ERROR
```

## 使用示例

```java
@Autowired
private SessionPersistenceService sessionService;

// 创建会话
Session session = sessionService.createSession("user-123", "gpt-4").join();

// 添加消息
sessionService.addMessage(session.getId(), "user", "Hello").join();
sessionService.addMessage(session.getId(), "assistant", "Hi there!").join();

// 更新状态
sessionService.updateStatus(session.getId(), SessionStatus.ACTIVE).join();

// 获取会话
Optional<Session> opt = sessionService.getSession(session.getId()).join();

// 获取消息历史
List<Message> messages = sessionService.getMessages(session.getId()).join();

// 搜索会话
List<Session> results = sessionService.searchSessions("keyword").join();

// 归档会话
sessionService.archiveSession(session.getId()).join();
```

## 与原版对比

| 功能 | Node.js | Java (新) |
|------|---------|-----------|
| 存储格式 | JSONL | SQLite ✅ |
| 会话恢复 | 完整 | 完整 ✅ |
| 历史搜索 | 完整 | 完整 ✅ |
| 内存缓存 | 无 | 有 ✅ |
| 自动归档 | 有 | 有 ✅ |

## 待实现

- [ ] 自动归档策略
- [ ] 会话压缩/摘要
- [ ] 导出功能
