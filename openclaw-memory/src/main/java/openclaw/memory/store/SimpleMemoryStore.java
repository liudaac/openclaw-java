package openclaw.memory.store;

import openclaw.memory.Embedding;
import openclaw.memory.MemoryEntry;
import openclaw.memory.MemorySearchResult;
import openclaw.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple in-memory implementation of MemoryStore.
 * Used for testing and as a fallback when no persistent store is configured.
 */
@Service
public class SimpleMemoryStore implements MemoryStore {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleMemoryStore.class);
    
    private final Map<String, MemoryEntry> entries = new ConcurrentHashMap<>();
    private final Map<String, Embedding> embeddings = new ConcurrentHashMap<>();
    
    @Override
    public CompletableFuture<Void> store(MemoryEntry entry) {
        return CompletableFuture.runAsync(() -> {
            entries.put(entry.id(), entry);
            logger.debug("Stored memory: {}", entry.id());
        });
    }
    
    @Override
    public CompletableFuture<Void> store(MemoryEntry entry, Embedding embedding) {
        return CompletableFuture.runAsync(() -> {
            entries.put(entry.id(), entry);
            embeddings.put(entry.id(), embedding);
            logger.debug("Stored memory with embedding: {}", entry.id());
        });
    }
    
    @Override
    public CompletableFuture<Optional<MemoryEntry>> retrieve(String id) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(entries.get(id)));
    }
    
    @Override
    public CompletableFuture<List<MemorySearchResult>> search(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            return entries.values().stream()
                .limit(limit)
                .map(entry -> new MemorySearchResult(entry.id(), 1.0, entry))
                .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<List<MemorySearchResult>> search(Embedding embedding, int limit, double minScore) {
        return CompletableFuture.supplyAsync(() -> {
            List<ScoredEntry> scored = new ArrayList<>();
            
            for (Map.Entry<String, Embedding> e : embeddings.entrySet()) {
                double score = embedding.cosineSimilarity(e.getValue());
                if (score >= minScore) {
                    MemoryEntry entry = entries.get(e.getKey());
                    if (entry != null) {
                        scored.add(new ScoredEntry(entry, score));
                    }
                }
            }
            
            return scored.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(limit)
                .map(se -> new MemorySearchResult(se.entry().id(), se.score(), se.entry()))
                .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<Boolean> delete(String id) {
        return CompletableFuture.supplyAsync(() -> {
            entries.remove(id);
            embeddings.remove(id);
            logger.debug("Deleted memory: {}", id);
            return true;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> update(String id, MemoryEntry entry) {
        return CompletableFuture.supplyAsync(() -> {
            entries.put(id, entry);
            logger.debug("Updated memory: {}", id);
            return true;
        });
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> getAll() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(entries.values()));
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> getByMetadata(Map<String, String> metadataFilter) {
        return CompletableFuture.supplyAsync(() -> {
            return entries.values().stream()
                .filter(entry -> {
                    Map<String, Object> metadata = entry.metadata();
                    return metadataFilter.entrySet().stream()
                        .allMatch(e -> metadata.getOrDefault(e.getKey(), "").equals(e.getValue()));
                })
                .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<StoreStats> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            long totalSize = entries.values().stream()
                .mapToLong(e -> e.text().length())
                .sum();
            return new StoreStats(entries.size(), totalSize, System.currentTimeMillis());
        });
    }
    
    @Override
    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> {
            entries.clear();
            embeddings.clear();
            logger.info("Cleared all memories");
        });
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void close() {
        entries.clear();
        embeddings.clear();
        logger.info("SimpleMemoryStore closed");
    }
    
    private record ScoredEntry(MemoryEntry entry, double score) {}
}
