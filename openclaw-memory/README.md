# OpenClaw Memory System (Java)

Memory and embedding system for OpenClaw Java Edition, compatible with Node.js version 2026.3.9.

## Features

- **Multiple Embedding Providers**: OpenAI, Mistral, Ollama (local)
- **Batch Processing**: Concurrent embedding with rate limiting
- **Vector Search**: SQLite-based with cosine similarity
- **Hybrid Search**: Combine vector and keyword search
- **Memory Management**: Add, update, delete, reindex operations

## Architecture

```
openclaw-memory/
├── embedding/      # Embedding providers
├── batch/          # Batch processing
├── search/         # Search engine interfaces
└── manager/        # Memory manager
```

## Embedding Providers

### OpenAI
```java
EmbeddingProvider provider = new OpenAIEmbeddingProvider(apiKey);
EmbeddingVector vector = provider.embed("Hello world").join();
```

### Mistral
```java
EmbeddingProvider provider = new MistralEmbeddingProvider(apiKey);
```

### Ollama (Local)
```java
EmbeddingProvider provider = new OllamaEmbeddingProvider("nomic-embed-text");
```

## Batch Processing

```java
BatchEmbeddingProcessor processor = new BatchEmbeddingProcessor(provider, 5, 100);

List<String> texts = List.of("text1", "text2", "text3");
BatchResult result = processor.processBatch(texts).join();

System.out.println("Success: " + result.successCount());
System.out.println("Errors: " + result.errorCount());
```

## Memory Manager

```java
MemoryManager manager = new DefaultMemoryManager();

MemoryConfig config = MemoryConfig.builder()
    .dataDir(Path.of("/data/memory"))
    .provider(provider)
    .maxEntries(100000)
    .build();

manager.initialize(config).join();

// Add memory
String id = manager.add("Important information", Map.of("source", "user")).join();

// Search
List<MemorySearchResult> results = manager.search("query", 10).join();

// Get stats
MemoryStats stats = manager.getStats().join();
```

## Search Options

```java
SearchOptions options = SearchOptions.builder()
    .limit(10)
    .minScore(0.7)
    .hybridWeight(0.5)
    .filters(Map.of("source", "user"))
    .build();

List<MemorySearchResult> results = searchEngine.search("query", options).join();
```

## Statistics

- **Total Java files**: 11
- **Embedding providers**: 3 (OpenAI, Mistral, Ollama)
- **Batch processing**: Concurrent with semaphore control
- **Storage**: SQLite with vector support

## Dependencies

- SQLite JDBC
- sqlite-vec (vector extension)
- OkHttp (HTTP client)
- Jackson (JSON)

## Next Steps

1. Add more embedding providers (Gemini, Voyage, etc.)
2. Implement MMR (Maximal Marginal Relevance) for diversity
3. Add temporal decay for relevance scoring
4. Implement query expansion
