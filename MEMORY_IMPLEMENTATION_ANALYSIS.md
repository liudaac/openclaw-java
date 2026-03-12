# OpenClaw Java 记忆模块实现分析

## 🤔 为什么当前是内存实现？

### 1. 快速原型开发

**原因**: Phase 1-8 的重点是快速实现核心功能

```
优先级排序:
1. HTTP/WebSocket Server ✅
2. LLM Client ✅
3. Agent API ✅
4. Channel Integration ✅
5. Tools ✅
6. Vector Search (内存) ✅
7. SQLite Persistence ❌ (未实现)
```

**决策**: 先让向量搜索跑起来，再优化持久化

### 2. 技术复杂度

**内存实现**:
```java
// 简单直接
Map<String, VectorEntry> entries = new ConcurrentHashMap<>();
```

**SQLite + 向量**:
```java
// 需要:
// 1. SQLite JDBC 驱动
// 2. 向量存储方案选择:
//    - sqlite-vec (C 扩展，Java 调用复杂)
//    - 纯 SQL 存储向量 (性能差)
//    - pgvector (需要 PostgreSQL)
// 3. 索引算法实现 (HNSW)
// 4. 序列化/反序列化
```

### 3. 依赖限制

**Node.js 原版**:
```javascript
// sqlite-vec: 原生 C 扩展，npm 直接安装
const sqliteVec = require('sqlite-vec');
```

**Java 挑战**:
```xml
<!-- 问题: sqlite-vec 没有官方 Java 绑定 -->
<!-- 可选方案: -->
<!-- 1. JNI 调用 C 库 (复杂) -->
<!-- 2. 纯 Java 实现 (工作量大) -->
<!-- 3. 使用替代方案 (如 pgvector) -->
```

---

## ✅ Java 版 SQLite 实现方案

### 方案 1: 纯 SQLite + 暴力搜索 (推荐短期)

**优点**: 简单，无需额外依赖
**缺点**: O(n) 搜索，性能一般

```java
@Service
public class SQLiteVectorStore implements MemoryStore {
    
    private final DataSource dataSource;
    private final EmbeddingService embeddingService;
    
    public SQLiteVectorStore(DataSource dataSource, EmbeddingService embeddingService) {
        this.dataSource = dataSource;
        this.embeddingService = embeddingService;
        initTable();
    }
    
    private void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS memories (
                id TEXT PRIMARY KEY,
                text TEXT NOT NULL,
                vector BLOB NOT NULL,  -- 存储 float[] 的二进制
                metadata TEXT,         -- JSON 格式
                timestamp INTEGER,
                session_key TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_session ON memories(session_key);
            CREATE INDEX IF NOT EXISTS idx_timestamp ON memories(timestamp);
        """;
        // 执行 SQL
    }
    
    @Override
    public CompletableFuture<Void> store(MemoryEntry entry) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO memories (id, text, vector, metadata, timestamp, session_key)
                VALUES (?, ?, ?, ?, ?, ?)
            """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, entry.id());
                stmt.setString(2, entry.text());
                stmt.setBytes(3, floatArrayToBytes(entry.vector())); // 序列化
                stmt.setString(4, objectMapper.writeValueAsString(entry.metadata()));
                stmt.setLong(5, entry.timestamp());
                stmt.setString(6, entry.sessionKey());
                
                stmt.executeUpdate();
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException("Failed to store memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> search(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. 生成查询向量
            float[] queryVector = embeddingService.embed(query).join();
            
            // 2. 从 SQLite 加载所有向量 (暴力搜索)
            String sql = "SELECT * FROM memories ORDER BY timestamp DESC LIMIT 1000";
            
            List<MemoryEntry> allEntries = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    MemoryEntry entry = new MemoryEntry(
                        rs.getString("id"),
                        rs.getString("text"),
                        bytesToFloatArray(rs.getBytes("vector")),
                        objectMapper.readValue(rs.getString("metadata"), Map.class),
                        rs.getLong("timestamp"),
                        rs.getString("session_key")
                    );
                    allEntries.add(entry);
                }
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException("Failed to search memories", e);
            }
            
            // 3. 计算相似度并排序
            return allEntries.stream()
                .map(entry -> new ScoredEntry(
                    entry,
                    cosineSimilarity(queryVector, entry.vector())
                ))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(limit)
                .map(ScoredEntry::entry)
                .collect(Collectors.toList());
        });
    }
    
    // 辅助方法: float[] 转 byte[]
    private byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }
    
    // 辅助方法: byte[] 转 float[]
    private float[] bytesToFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }
}
```

**依赖**:
```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.44.1.0</version>
</dependency>
```

---

### 方案 2: pgvector (推荐生产环境)

**优点**: 专业向量数据库，性能高，支持 HNSW
**缺点**: 需要 PostgreSQL

```java
@Service
public class PgVectorStore implements MemoryStore {
    
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    
    public PgVectorStore(DataSource dataSource, EmbeddingService embeddingService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.embeddingService = embeddingService;
        initTable();
    }
    
    private void initTable() {
        // 启用 pgvector 扩展
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        
        // 创建表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS memories (
                id TEXT PRIMARY KEY,
                text TEXT NOT NULL,
                vector vector(1536),  -- pgvector 类型
                metadata JSONB,
                timestamp BIGINT,
                session_key TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_memories_vector 
            ON memories USING hnsw (vector vector_cosine_ops);
        """);
    }
    
    @Override
    public CompletableFuture<Void> store(MemoryEntry entry) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO memories (id, text, vector, metadata, timestamp, session_key)
                VALUES (?, ?, ?::vector, ?::jsonb, ?, ?)
            """;
            
            // 向量转字符串格式: '[0.1,0.2,...]'
            String vectorStr = floatArrayToVectorString(entry.vector());
            String metadataStr = objectMapper.writeValueAsString(entry.metadata());
            
            jdbcTemplate.update(sql, entry.id(), entry.text(), 
                vectorStr, metadataStr, entry.timestamp(), entry.sessionKey());
        });
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> search(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // 生成查询向量
            float[] queryVector = embeddingService.embed(query).join();
            String vectorStr = floatArrayToVectorString(queryVector);
            
            // 使用 pgvector 的相似度搜索
            String sql = """
                SELECT *, vector <=> ?::vector AS distance
                FROM memories
                ORDER BY vector <=> ?::vector
                LIMIT ?
            """;
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                return new MemoryEntry(
                    rs.getString("id"),
                    rs.getString("text"),
                    vectorStringToFloatArray(rs.getString("vector")),
                    objectMapper.readValue(rs.getString("metadata"), Map.class),
                    rs.getLong("timestamp"),
                    rs.getString("session_key")
                );
            }, vectorStr, vectorStr, limit);
        });
    }
    
    private String floatArrayToVectorString(float[] floats) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < floats.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(floats[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
```

**依赖**:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.1</version>
</dependency>
```

**Docker Compose**:
```yaml
services:
  postgres:
    image: ankane/pgvector:latest
    environment:
      POSTGRES_USER: openclaw
      POSTGRES_PASSWORD: password
      POSTGRES_DB: openclaw
    ports:
      - "5432:5432"
```

---

### 方案 3: 混合方案 (推荐最终)

**设计**: 内存缓存 + SQLite 持久化

```java
@Service
public class HybridMemoryStore implements MemoryStore {
    
    private final InMemoryVectorIndex cache;      // L1: 内存缓存
    private final SQLiteMemoryStore persistent;   // L2: SQLite 持久化
    
    @Override
    public CompletableFuture<Void> store(MemoryEntry entry) {
        return CompletableFuture.runAsync(() -> {
            // 1. 写入内存
            cache.add(entry.id(), entry.vector(), entry.metadata());
            
            // 2. 异步写入 SQLite
            persistent.store(entry);
        });
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> search(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. 先查内存缓存
            float[] queryVector = embeddingService.embed(query).join();
            List<VectorSearchResult> cacheResults = cache.search(queryVector, limit);
            
            // 2. 如果缓存不足，查 SQLite
            if (cacheResults.size() < limit) {
                List<MemoryEntry> dbResults = persistent.search(query, limit).join();
                // 合并结果
            }
            
            return results;
        });
    }
    
    // 启动时加载数据到缓存
    @PostConstruct
    public void loadFromDatabase() {
        List<MemoryEntry> allEntries = persistent.loadAll().join();
        for (MemoryEntry entry : allEntries) {
            cache.add(entry.id(), entry.vector(), entry.metadata());
        }
    }
}
```

---

## 📊 方案对比

| 方案 | 复杂度 | 性能 | 持久化 | 推荐场景 |
|------|--------|------|--------|----------|
| **内存** | ⭐ 简单 | O(n) 搜索 | ❌ 重启丢失 | 原型/测试 |
| **SQLite + 暴力** | ⭐⭐ 中等 | O(n) 搜索 | ✅ 持久化 | 小规模生产 |
| **pgvector** | ⭐⭐⭐ 复杂 | O(log n) HNSW | ✅ 持久化 | 大规模生产 |
| **混合** | ⭐⭐⭐ 复杂 | O(log n) | ✅ 持久化 | 高性能生产 |

---

## 🚀 实施建议

### 短期 (本周)
1. 实现 **方案 1: SQLite + 暴力搜索**
2. 添加配置切换 (内存 ↔ SQLite)

### 中期 (下周)
1. 实现 **方案 3: 混合方案**
2. 添加缓存预热机制

### 长期 (下月)
1. 评估 **pgvector** 迁移
2. 性能基准测试

---

## 📝 总结

**为什么当前是内存实现？**
1. ✅ 快速原型，优先核心功能
2. ✅ 避免复杂依赖
3. ✅ 简化开发和测试

**应该改为 SQLite 吗？**
- 🔴 **测试/原型**: 保持内存
- 🟡 **小规模生产**: SQLite + 暴力
- 🟢 **大规模生产**: pgvector 或混合

**推荐路径**:
```
内存 (当前) → SQLite (短期) → 混合 (中期) → pgvector (长期)
```

需要我立即实现 SQLite 版本吗？
