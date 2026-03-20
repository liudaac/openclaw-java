# OpenClaw Session 模块

实时会话管理模块，提供会话持久化和消息存储功能。

## 功能特性

### 核心功能
- ✅ SQLite 持久化存储
- ✅ 内存缓存（热数据）
- ✅ 会话状态机管理（6种状态）
- ✅ 消息历史记录
- ✅ 会话搜索
- ✅ 统计信息
- ✅ 自动清理过期会话

### 会话状态
```
PENDING -> ACTIVE -> COMPLETED -> ARCHIVED
    |         |
    |         v
    |      PAUSED
    |         |
    v      ERROR
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>ai.openclaw</groupId>
    <artifactId>openclaw-session</artifactId>
</dependency>
```

### 2. 配置（可选）

```yaml
openclaw:
  session:
    enabled: true
    storage-type: sqlite  # sqlite | memory | redis
    db-path: ${user.home}/.openclaw/sessions.db
    max-messages: 1000
    ttl: 30d
    auto-cleanup: true
    cleanup-interval: 1h
```

### 3. 使用服务

```java
@Autowired
private SessionPersistenceService sessionService;

// 创建会话
Session session = sessionService.createSession("user-123", "gpt-4").join();

// 添加消息
sessionService.addMessage(session.getId(), "user", "Hello!").join();

// 获取历史
List<Message> messages = sessionService.getMessages(session.getId()).join();

// 搜索会话
List<Session> results = sessionService.searchSessions("keyword").join();

// 获取统计
SessionStats stats = sessionService.getStats(session.getId()).join();
```

## 架构设计

### 类图
```
SessionStore (interface)
    ├── SQLiteSessionStore
    └── InMemorySessionStore

SessionPersistenceService
    ├── SessionStore (存储层)
    └── sessionCache (内存缓存)
```

### 数据模型

**Session 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | TEXT | 主键 |
| session_key | TEXT | 会话标识（唯一） |
| model | TEXT | 使用的模型 |
| status | TEXT | 状态 |
| metadata | TEXT | JSON 元数据 |
| created_at | INTEGER | 创建时间 |
| updated_at | INTEGER | 更新时间 |
| last_activity_at | INTEGER | 最后活动时间 |
| total_input_tokens | INTEGER | 输入 token 数 |
| total_output_tokens | INTEGER | 输出 token 数 |
| error_message | TEXT | 错误信息 |

**Message 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | TEXT | 主键 |
| session_id | TEXT | 外键（关联 sessions） |
| role | TEXT | 角色（system/user/assistant/tool） |
| content | TEXT | 内容 |
| tool_name | TEXT | 工具名称 |
| tool_call_id | TEXT | 工具调用 ID |
| tool_result | TEXT | 工具结果（JSON） |
| metadata | TEXT | 元数据（JSON） |
| created_at | INTEGER | 创建时间 |
| token_count | INTEGER | Token 数 |

## 性能优化

1. **索引优化**
   - session_key 索引
   - status 索引
   - last_activity_at 索引
   - session_id + created_at 复合索引

2. **缓存策略**
   - 活跃会话内存缓存
   - 懒加载非活跃会话
   - 写入时同步更新缓存

3. **批量操作**
   - 支持批量插入消息
   - 异步保存减少延迟

## 测试

```bash
# 运行测试
mvn test -pl openclaw-session

# 运行特定测试
mvn test -pl openclaw-session -Dtest=SQLiteSessionStoreTest
```

## 更新日志

### 2026.3.20
- ✅ 实现 SQLiteSessionStore 完整功能
- ✅ 添加 InMemorySessionStore 备选实现
- ✅ 添加 SessionConfig 配置类
- ✅ 添加 Spring Boot 自动配置
- ✅ 添加单元测试
- ✅ 完善文档

## 待办事项

- [ ] Redis 存储实现
- [ ] 会话压缩/摘要
- [ ] 分布式会话同步
- [ ] 会话导出/导入