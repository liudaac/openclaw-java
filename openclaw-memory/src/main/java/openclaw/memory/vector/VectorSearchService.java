package openclaw.memory.vector;

import openclaw.memory.Embedding;
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
 * Vector Search Service - High Priority Improvement
 *
 * <p>Provides vector similarity search for memory and embeddings.</p>
 */
@Service
public class VectorSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);

    private final MemoryStore memoryStore;
    private final Map<String, float[]> embeddingCache;
    private final VectorIndex vectorIndex;

    public VectorSearchService(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
        this.embeddingCache = new ConcurrentHashMap<>();
        this.vectorIndex = new InMemoryVectorIndex();
    }

    /**
     * Search for similar embeddings
     */
    public CompletableFuture<List<VectorSearchResult>> search(
            float[] queryVector,
            int topK,
            double minScore) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Searching for {} similar vectors with min score {}", topK, minScore);

                // Search in vector index
                List<VectorSearchResult> results = vectorIndex.search(queryVector, topK * 2);

                // Filter by score and limit
                return results.stream()
                        .filter(r -> r.score() >= minScore)
                        .limit(topK)
                        .collect(Collectors.toList());

            } catch (Exception e) {
                logger.error("Vector search failed: {}", e.getMessage(), e);
                return List.of();
            }
        });
    }

    /**
     * Search by text (auto-embed)
     */
    public CompletableFuture<List<VectorSearchResult>> searchByText(
            String queryText,
            int topK,
            double minScore) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate embedding for query text
                float[] queryVector = generateEmbedding(queryText);

                if (queryVector == null) {
                    logger.warn("Failed to generate embedding for query: {}", queryText);
                    return List.of();
                }

                return search(queryVector, topK, minScore).join();

            } catch (Exception e) {
                logger.error("Text search failed: {}", e.getMessage(), e);
                return List.of();
            }
        });
    }

    /**
     * Add embedding to index
     */
    public CompletableFuture<Boolean> addEmbedding(String id, float[] vector, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                vectorIndex.add(id, vector, metadata);
                embeddingCache.put(id, vector);
                logger.debug("Added embedding: {}", id);
                return true;

            } catch (Exception e) {
                logger.error("Failed to add embedding: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Remove embedding from index
     */
    public CompletableFuture<Boolean> removeEmbedding(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                vectorIndex.remove(id);
                embeddingCache.remove(id);
                logger.debug("Removed embedding: {}", id);
                return true;

            } catch (Exception e) {
                logger.error("Failed to remove embedding: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Update embedding
     */
    public CompletableFuture<Boolean> updateEmbedding(String id, float[] vector, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                vectorIndex.update(id, vector, metadata);
                embeddingCache.put(id, vector);
                logger.debug("Updated embedding: {}", id);
                return true;

            } catch (Exception e) {
                logger.error("Failed to update embedding: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Batch add embeddings
     */
    public CompletableFuture<Integer> batchAddEmbeddings(
            List<String> ids,
            List<float[]> vectors,
            List<Map<String, Object>> metadataList) {

        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;

            for (int i = 0; i < ids.size(); i++) {
                try {
                    String id = ids.get(i);
                    float[] vector = vectors.get(i);
                    Map<String, Object> metadata = metadataList.get(i);

                    if (addEmbedding(id, vector, metadata).join()) {
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.error("Failed to add embedding at index {}: {}", i, e.getMessage());
                }
            }

            logger.info("Batch added {} embeddings", successCount);
            return successCount;
        });
    }

    /**
     * Get embedding by ID
     */
    public Optional<float[]> getEmbedding(String id) {
        return Optional.ofNullable(embeddingCache.get(id));
    }

    /**
     * Get index statistics
     */
    public VectorIndexStats getStats() {
        return new VectorIndexStats(
                vectorIndex.size(),
                embeddingCache.size(),
                vectorIndex.dimension()
        );
    }

    /**
     * Clear all embeddings
     */
    public CompletableFuture<Boolean> clear() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                vectorIndex.clear();
                embeddingCache.clear();
                logger.info("Cleared all embeddings");
                return true;

            } catch (Exception e) {
                logger.error("Failed to clear embeddings: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Generate embedding for text
     * (Simplified - in production, use OpenAI or similar)
     */
    private float[] generateEmbedding(String text) {
        // Simplified embedding generation
        // In production, call OpenAI API or use local model

        // Create a simple hash-based embedding for demo
        int dimension = 1536; // OpenAI embedding dimension
        float[] embedding = new float[dimension];

        // Use text hash to generate pseudo-embedding
        int hash = text.hashCode();
        Random random = new Random(hash);

        for (int i = 0; i < dimension; i++) {
            embedding[i] = (float) (random.nextGaussian() * 0.1);
        }

        // Normalize
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                embedding[i] /= norm;
            }
        }

        return embedding;
    }

    /**
     * Calculate cosine similarity
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }

        double dotProduct = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // Records
    public record VectorSearchResult(
            String id,
            double score,
            float[] vector,
            Map<String, Object> metadata
    ) {}

    public record VectorIndexStats(
            int indexSize,
            int cacheSize,
            int dimension
    ) {}

    /**
     * Vector index interface
     */
    public interface VectorIndex {
        void add(String id, float[] vector, Map<String, Object> metadata);
        void remove(String id);
        void update(String id, float[] vector, Map<String, Object> metadata);
        List<VectorSearchResult> search(float[] query, int topK);
        int size();
        int dimension();
        void clear();
    }

    /**
     * In-memory vector index (simplified HNSW or brute force)
     */
    private static class InMemoryVectorIndex implements VectorIndex {

        private final Map<String, VectorEntry> entries = new ConcurrentHashMap<>();
        private static final int DIMENSION = 1536;

        @Override
        public void add(String id, float[] vector, Map<String, Object> metadata) {
            entries.put(id, new VectorEntry(id, vector, metadata));
        }

        @Override
        public void remove(String id) {
            entries.remove(id);
        }

        @Override
        public void update(String id, float[] vector, Map<String, Object> metadata) {
            entries.put(id, new VectorEntry(id, vector, metadata));
        }

        @Override
        public List<VectorSearchResult> search(float[] query, int topK) {
            return entries.values().stream()
                    .map(entry -> {
                        double score = cosineSimilarity(query, entry.vector());
                        return new VectorSearchResult(
                                entry.id(),
                                score,
                                entry.vector(),
                                entry.metadata()
                        );
                    })
                    .sorted((a, b) -> Double.compare(b.score(), a.score()))
                    .limit(topK)
                    .collect(Collectors.toList());
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public int dimension() {
            return DIMENSION;
        }

        @Override
        public void clear() {
            entries.clear();
        }

        private record VectorEntry(
                String id,
                float[] vector,
                Map<String, Object> metadata
        ) {}
    }
}
