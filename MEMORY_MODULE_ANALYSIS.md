# OpenClaw Java 记忆模块实现分析

## 📋 概述

OpenClaw Java 的记忆模块提供了向量存储、语义搜索和上下文管理功能，支持 Agent 的长期记忆和智能检索。

---

## 🏗️ 架构设计

### 模块结构

```
openclaw-memory/
├── src/main/java/openclaw/memory/
│   ├── MemoryStore.java              # 内存存储接口
│   ├── Embedding.java                # 嵌入向量
│   ├── MemorySearchResult.java       # 搜索结果
│   ├── SQLiteMemoryStore.java        # SQLite 实现
│   └── vector/
│       ├── VectorSearchService.java  # 向量搜索服务
│       └── EmbeddingService.java     # OpenAI 嵌入服务
```

---

## 💾 核心组件

### 1. MemoryStore - 内存存储接口

**功能**: 定义内存存储的标准接口

```java
public interface MemoryStore {
    // 存储记忆
    CompletableFuture<Void> store(MemoryEntry entry);
    
    // 搜索记忆
    CompletableFuture<List<MemoryEntry>> search(String query, int limit);
    
    // 获取记忆
    CompletableFuture<Optional<MemoryEntry>> get(String id);
    
    // 删除记忆
    CompletableFuture<Void> delete(String id);
}
```

**设计模式**: 接口隔离，支持多种存储实现

### 2. Embedding - 嵌入向量

**功能**: 表示文本的向量嵌入

```java
public record Embedding(
    String id,
    float[] vector,
    String text,
    Map<String, Object> metadata,
    long timestamp
) {}
```

**关键特性**:
- 1536 维向量 (OpenAI ada-002)
- 元数据支持
- 时间戳记录

### 3. VectorSearchService - 向量搜索

**功能**: 提供语义相似度搜索

```java
@Service
public class VectorSearchService {
    // 向量相似度搜索
    public CompletableFuture<List<VectorSearchResult>> search(
        float[] queryVector,
        int topK,
        double minScore
    );
    
    // 文本搜索 (自动嵌入)
    public CompletableFuture<List<VectorSearchResult>> searchByText(
        String queryText,
        int topK,
        double minScore
    );
    
    // 添加嵌入
    public CompletableFuture<Boolean> addEmbedding(
        String id,
        float[] vector,
        Map<String, Object> metadata
    );
}
```

**算法**: 余弦相似度

```java
public static double cosineSimilarity(float[] a, float[] b) {
    double dotProduct = 0;
    double normA = 0;
    double normB = 0;
    
    for (int i = 0; i < a.length; i++) {
        dotProduct += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}
```

### 4. EmbeddingService - OpenAI 嵌入

**功能**: 调用 OpenAI API 生成文本嵌入

```java
@Service
public class EmbeddingService {
    @Value("${openai.api-key:}")
    private String apiKey;
    
    @Value("${openai.embedding.model:text-embedding-ada-002}")
    private String model;
    
    // 单文本嵌入
    public CompletableFuture<float[]> embed(String text);
    
    // 批量嵌入
    public CompletableFuture<List<float[]>> embedBatch(List<String> texts);
}
```

**API 调用**:
```http
POST https://api.openai.com/v1/embeddings
Authorization: Bearer {api_key}
Content-Type: application/json

{
  "model": "text-embedding-ada-002",
  "input": "要嵌入的文本"
}
```

---

## 🔍 向量索引实现

### InMemoryVectorIndex

**类型**: 内存向量索引 (简化实现)

```java
private static class InMemoryVectorIndex implements VectorIndex {
    private final Map<String, VectorEntry> entries = new ConcurrentHashMap<>();
    
    // 暴力搜索 (O(n))
    public List<VectorSearchResult> search(float[] query, int topK) {
        return entries.values().stream()
            .map(entry -> {
                double score = cosineSimilarity(query, entry.vector());
                return new VectorSearchResult(entry.id(), score, ...);
            })
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .limit(topK)
            .collect(Collectors.toList());
    }
}
```

**特点**:
- ✅ 简单实现，易于理解
- ✅ 无需外部依赖
- ⚠️ O(n) 复杂度，不适合大规模数据
- ⚠️ 内存存储，重启丢失

### 与 Node.js 对比

| 特性 | Node.js | Java |
|------|---------|------|
| 向量数据库 | sqlite-vec + LanceDB | 内存索引 |
| 索引算法 | HNSW | 暴力搜索 |
| 持久化 | ✅ SQLite | ❌ 内存 |
| 性能 | O(log n) | O(n) |

---

## 🔄 数据流

### 存储记忆

```
Agent 消息
    ↓
EmbeddingService.embed(text)
    ↓
OpenAI API → float[1536]
    ↓
VectorSearchService.addEmbedding()
    ↓
InMemoryVectorIndex.entries.put(id, entry)
    ↓
SQLiteMemoryStore.store() (可选)
    ↓
持久化到 SQLite
```

### 检索记忆

```
用户查询
    ↓
EmbeddingService.embed(query)
    ↓
OpenAI API → queryVector
    ↓
VectorSearchService.search(queryVector, topK=5)
    ↓
InMemoryVectorIndex.search()
    ↓
计算余弦相似度 (所有条目)
    ↓
排序并返回 Top 5
    ↓
Agent 上下文增强
```

---

## 💡 使用示例

### 存储记忆

```java
// 创建记忆
MemoryEntry entry = MemoryEntry.builder()
    .id(UUID.randomUUID().toString())
    .text("用户喜欢 Java 编程语言")
    .metadata(Map.of(
        "userId", "user_123",
        "type", "preference",
        "topic", "programming"
    ))
    .build();

// 生成嵌入
float[] embedding = embeddingService.embed(entry.text()).join();

// 存储到向量索引
vectorSearchService.addEmbedding(
    entry.id(),
    embedding,
    entry.metadata()
).join();

// 持久化到 SQLite (可选)
memoryStore.store(entry).join();
```

### 检索记忆

```java
// 用户查询
String query = "用户喜欢什么编程语言？";

// 搜索相关记忆
List<VectorSearchResult> results = vectorSearchService
    .searchByText(query, 5, 0.7)
    .join();

// 使用结果增强 Agent 上下文
for (VectorSearchResult result : results) {
    System.out.println("找到记忆: " + result.id());
    System.out.println("相似度: " + result.score());
    System.out.println("内容: " + result.metadata().get("text"));
}
```

---

## 🔧 配置

### application.yml

```yaml
openclaw:
  memory:
    enabled: true
    vector-search:
      enabled: true
      dimension: 1536
      min-score: 0.7
    embedding:
      provider: openai
      model: text-embedding-ada-002
      api-key: ${OPENAI_API_KEY}
```

---

## 📊 性能分析

### 时间复杂度

| 操作 | 复杂度 | 说明 |
|------|--------|------|
| 嵌入生成 | O(1) | API 调用 |
| 添加向量 | O(1) | HashMap 插入 |
| 向量搜索 | O(n) | 暴力搜索 |
| 相似度计算 | O(d) | d=1536 维度 |

### 空间复杂度

| 存储 | 大小 | 说明 |
|------|------|------|
| 单个向量 | 6KB | 1536 × 4 bytes |
| 1000 条记忆 | 6MB | 不含元数据 |
| 10000 条记忆 | 60MB | 不含元数据 |

---

## 🚀 优化建议

### 短期优化
1. **添加 HNSW 索引** - 提升搜索性能到 O(log n)
2. **向量量化** - 减少内存占用
3. **批量操作** - 减少 API 调用次数

### 长期优化
1. **专用向量数据库** - 集成 pgvector 或 Milvus
2. **分布式存储** - 支持大规模记忆
3. **增量索引** - 避免全量重建

---

## 📝 总结

OpenClaw Java 的记忆模块实现了基础的向量存储和语义搜索功能：

**优势**:
- ✅ 简单易用，无外部依赖
- ✅ 集成 OpenAI 嵌入
- ✅ 支持元数据

**局限**:
- ⚠️ 内存存储，重启丢失
- ⚠️ 暴力搜索，性能受限
- ⚠️ 无持久化向量索引

**适用场景**:
- 小规模 Agent 会话
- 短期记忆存储
- 原型开发

**生产建议**:
- 使用 pgvector 替代内存索引
- 添加 Redis 缓存层
- 实现增量索引更新

---

*分析时间: 2026-03-11*  
*版本: 2026.3.9*
