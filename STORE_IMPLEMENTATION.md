# OpenClaw Java Session Store Implementation

## 概述

实现了可插拔的会话存储架构，支持三种存储后端：
- **Memory**: 内存存储，适合开发和测试
- **File**: 文件存储，适合简单部署
- **Redis**: Redis 存储，适合生产环境

## 快速开始

### 1. 内存存储 (默认)

无需配置，开箱即用：

```yaml
openclaw:
  store:
    type: memory
```

### 2. 文件存储

适合单机部署，数据持久化到本地文件：

```yaml
openclaw:
  store:
    type: file
    file:
      base-dir: /var/openclaw/store
      extension: .json
```

### 3. Redis 存储

适合分布式部署，支持多节点共享：

```yaml
openclaw:
  store:
    type: redis
    redis:
      host: redis.example.com
      port: 6379
      password: your-password
      database: 0

spring:
  data:
    redis:
      host: redis.example.com
      port: 6379
```

## 配置详解

### 存储类型选择

| 类型 | 持久化 | 分布式 | 性能 | 适用场景 |
|------|--------|--------|------|----------|
| memory | ❌ | ❌ | ⭐⭐⭐ | 开发/测试 |
| file | ✅ | ❌ | ⭐⭐ | 单机部署 |
| redis | ✅ | ✅ | ⭐⭐⭐ | 生产环境 |

### 会话配置

```yaml
openclaw:
  store:
    session:
      # 会话有效期 (ISO 8601 格式)
      ttl: PT24H
      
      # 每个会话最大消息数
      max-messages: 1000
      
      # 自动清理过期会话
      auto-cleanup: true
      
      # 清理间隔
      cleanup-interval: PT30M
```

### 文件存储配置

```yaml
openclaw:
  store:
    file:
      # 存储目录
      base-dir: ${user.home}/.openclaw/store
      
      # 文件扩展名
      extension: .json
      
      # 自动压缩间隔
      compact-interval: PT1H
```

### Redis 配置

```yaml
openclaw:
  store:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
      timeout: PT5S
      key-prefix: openclaw:
```

## 实现架构

```
┌─────────────────────────────────────────────────────────┐
│                    SessionStore                         │
│                    (Interface)                          │
└─────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────┐
│ MemorySession   │ │ FileSession  │ │ RedisSession │
│ Store           │ │ Store        │ │ Store        │
├─────────────────┤ ├──────────────┤ ├──────────────┤
│ ConcurrentHash  │ │ JSON Files   │ │ Redis        │
│ Map             │ │              │ │              │
│                 │ │              │ │              │
│ • Fast          │ │ • Persistent │ │ • Distributed│
│ • Non-persistent│ │ • Simple     │ │ • Scalable   │
│ • Development   │ │ • Single-node│ │ • Production │
└─────────────────┘ └──────────────┘ └──────────────┘
```

## 代码使用示例

### 注入 SessionStore

```java
@Service
public class MyService {
    
    @Autowired
    private SessionStore sessionStore;
    
    public void saveSession(String key, AgentSession session) {
        sessionStore.saveSession(key, session).join();
    }
    
    public Optional<AgentSession> getSession(String key) {
        return sessionStore.getSession(key).join();
    }
}
```

### 切换存储类型

```java
// 通过配置切换
StoreConfig config = new StoreConfig();
config.setType(SessionStore.StoreType.REDIS);

SessionStoreFactory factory = new SessionStoreFactory(config, redisTemplate);
SessionStore store = factory.createStore();
```

### 健康检查

```java
// 检查存储健康状态
boolean healthy = sessionStore.isHealthy().join();
if (!healthy) {
    logger.error("Session store is unhealthy");
}
```

## 文件清单

| 文件 | 说明 |
|------|------|
| `SessionStore.java` | 存储接口 |
| `StoreConfig.java` | 配置类 |
| `SessionStoreFactory.java` | 工厂类 |
| `StoreAutoConfiguration.java` | 自动配置 |
| `MemorySessionStore.java` | 内存实现 |
| `FileSessionStore.java` | 文件实现 |
| `RedisSessionStore.java` | Redis 实现 |
| `application-store.yml` | 配置示例 |

## 演进路径

```
Phase 1: 开发阶段
    └── type: memory
        
Phase 2: 简单部署
    └── type: file
        └── base-dir: /var/openclaw/store
        
Phase 3: 生产部署
    └── type: redis
        └── Redis Cluster
        
Phase 4: 混合部署
    └── 热数据: redis
    └── 冷数据: file/postgresql
```

## 注意事项

1. **Memory Store**: 重启后数据丢失，仅用于开发
2. **File Store**: 需要确保目录有写权限
3. **Redis Store**: 需要配置 spring.data.redis
4. **TTL**: 过期会话会自动清理
5. **Max Messages**: 超过限制会丢弃旧消息

## 后续扩展

- [ ] PostgreSQL 存储实现
- [ ] 存储迁移工具
- [ ] 混合存储策略
- [ ] 存储监控指标
