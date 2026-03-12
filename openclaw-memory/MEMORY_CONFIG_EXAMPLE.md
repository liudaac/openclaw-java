# OpenClaw Java 记忆模块配置示例

## 📋 配置说明

记忆模块支持两种存储方案，通过配置切换：

1. **SQLite** (默认) - 短期方案，依赖少
2. **pgvector** - 长期方案，高性能

---

## ⚙️ 配置文件

### 默认配置 (SQLite)

```yaml
# application.yml
openclaw:
  memory:
    # 存储类型: sqlite (默认) | pgvector
    storage-type: sqlite
    
    # SQLite 配置
    sqlite:
      url: jdbc:sqlite:openclaw.db
      enabled: true
    
    # 向量搜索配置
    vector-search:
      dimension: 1536        # OpenAI ada-002 维度
      min-score: 0.7         # 最小相似度阈值
      default-limit: 5       # 默认返回结果数
      enabled: true
```

### 切换到 pgvector

```yaml
# application.yml
openclaw:
  memory:
    storage-type: pgvector
    
    # pgvector 配置
    pgvector:
      url: jdbc:postgresql://localhost:5432/openclaw
      username: openclaw
      password: password
      enabled: true
    
    vector-search:
      dimension: 1536
      min-score: 0.7
      default-limit: 5
      enabled: true
```

---

## 🚀 快速开始

### 1. SQLite (默认)

**依赖**: 已包含 sqlite-jdbc

**启动**: 直接运行，自动创建数据库文件

```bash
# 自动创建 openclaw.db 文件
mvn spring-boot:run
```

### 2. pgvector

**步骤 1**: 启动 PostgreSQL with pgvector

```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: ankane/pgvector:latest
    environment:
      POSTGRES_USER: openclaw
      POSTGRES_PASSWORD: password
      POSTGRES_DB: openclaw
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

```bash
docker-compose up -d postgres
```

**步骤 2**: 修改配置

```yaml
openclaw:
  memory:
    storage-type: pgvector
    pgvector:
      url: jdbc:postgresql://localhost:5432/openclaw
      username: openclaw
      password: password
```

**步骤 3**: 运行

```bash
mvn spring-boot:run
```

---

## 📊 方案对比

| 特性 | SQLite | pgvector |
|------|--------|----------|
| **部署复杂度** | ⭐ 简单 | ⭐⭐⭐ 复杂 |
| **依赖** | sqlite-jdbc | PostgreSQL + pgvector |
| **搜索性能** | O(n) 暴力 | O(log n) HNSW |
| **数据持久化** | ✅ 文件 | ✅ 数据库 |
| **并发支持** | ⚠️ 有限 | ✅ 优秀 |
| **扩展性** | ⚠️ 单机 | ✅ 分布式 |
| **适用规模** | < 10万条 | > 10万条 |

---

## 🔧 高级配置

### SQLite 高级配置

```yaml
openclaw:
  memory:
    storage-type: sqlite
    sqlite:
      url: jdbc:sqlite:file:/data/openclaw.db?cache=shared&mode=rwc
      enabled: true
    
    vector-search:
      dimension: 1536
      min-score: 0.7
      default-limit: 5
```

### pgvector 高级配置

```yaml
openclaw:
  memory:
    storage-type: pgvector
    pgvector:
      url: jdbc:postgresql://localhost:5432/openclaw?sslmode=require
      username: openclaw
      password: ${POSTGRES_PASSWORD}
      enabled: true
    
    vector-search:
      dimension: 1536
      min-score: 0.7
      default-limit: 10
```

### 连接池配置 (pgvector)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/openclaw
    username: openclaw
    password: password
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 🔄 迁移指南

### 从内存迁移到 SQLite

**步骤 1**: 修改配置
```yaml
openclaw:
  memory:
    storage-type: sqlite
```

**步骤 2**: 重启服务，自动创建表

### 从 SQLite 迁移到 pgvector

**步骤 1**: 导出 SQLite 数据
```bash
# 使用 sqlite3 导出
sqlite3 openclaw.db ".dump memories" > memories.sql
```

**步骤 2**: 导入 PostgreSQL
```bash
# 转换并导入
psql -U openclaw -d openclaw -f memories.sql
```

**步骤 3**: 修改配置并重启
```yaml
openclaw:
  memory:
    storage-type: pgvector
```

---

## 📝 环境变量配置

### SQLite

```bash
export OPENCLAW_MEMORY_STORAGE_TYPE=sqlite
export OPENCLAW_MEMORY_SQLITE_URL=jdbc:sqlite:openclaw.db
```

### pgvector

```bash
export OPENCLAW_MEMORY_STORAGE_TYPE=pgvector
export OPENCLAW_MEMORY_PGVECTOR_URL=jdbc:postgresql://localhost:5432/openclaw
export OPENCLAW_MEMORY_PGVECTOR_USERNAME=openclaw
export OPENCLAW_MEMORY_PGVECTOR_PASSWORD=password
```

---

## ✅ 验证配置

### 检查当前存储类型

```bash
curl http://localhost:8080/api/v1/admin/memory/stats
```

**响应**:
```json
{
  "storageType": "sqlite",
  "totalMemories": 1000,
  "dimension": 1536
}
```

### 检查 pgvector 是否可用

```bash
# 检查 PostgreSQL 连接
docker exec postgres pg_isready -U openclaw

# 检查 pgvector 扩展
docker exec postgres psql -U openclaw -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
```

---

## 🎯 推荐配置

### 开发环境
```yaml
openclaw:
  memory:
    storage-type: sqlite
```

### 测试环境
```yaml
openclaw:
  memory:
    storage-type: sqlite
    sqlite:
      url: jdbc:sqlite::memory:  # 内存数据库，测试后自动清理
```

### 生产环境 (小规模)
```yaml
openclaw:
  memory:
    storage-type: sqlite
    sqlite:
      url: jdbc:sqlite:/data/openclaw.db
```

### 生产环境 (大规模)
```yaml
openclaw:
  memory:
    storage-type: pgvector
    pgvector:
      url: jdbc:postgresql://prod-db:5432/openclaw
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
```

---

*配置文档版本: 2026.3.9*
