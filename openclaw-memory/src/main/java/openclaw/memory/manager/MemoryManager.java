package openclaw.memory.manager;

import openclaw.memory.embedding.EmbeddingProvider;
import openclaw.memory.search.MemorySearchEngine;
import openclaw.memory.search.MemorySearchResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Memory manager for coordinating embedding and search operations.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface MemoryManager {

    /**
     * Initializes the memory manager.
     *
     * @param config the configuration
     * @return completion future
     */
    CompletableFuture<Void> initialize(MemoryConfig config);

    /**
     * Shuts down the memory manager.
     *
     * @return completion future
     */
    CompletableFuture<Void> shutdown();

    /**
     * Gets the embedding provider.
     *
     * @return the provider
     */
    EmbeddingProvider getEmbeddingProvider();

    /**
     * Gets the search engine.
     *
     * @return the search engine
     */
    MemorySearchEngine getSearchEngine();

    /**
     * Searches memories.
     *
     * @param query the query
     * @param limit the result limit
     * @return the results
     */
    default CompletableFuture<List<MemorySearchResult>> search(String query, int limit) {
        return getSearchEngine().search(query, 
                MemorySearchEngine.SearchOptions.builder().limit(limit).build());
    }

    /**
     * Adds a memory.
     *
     * @param content the content
     * @return the entry ID
     */
    default CompletableFuture<String> add(String content) {
        return getSearchEngine().addMemory(content, Map.of());
    }

    /**
     * Adds a memory with metadata.
     *
     * @param content the content
     * @param metadata the metadata
     * @return the entry ID
     */
    default CompletableFuture<String> add(String content, Map<String, Object> metadata) {
        return getSearchEngine().addMemory(content, metadata);
    }

    /**
     * Deletes a memory.
     *
     * @param entryId the entry ID
     * @return completion future
     */
    default CompletableFuture<Void> delete(String entryId) {
        return getSearchEngine().deleteMemory(entryId);
    }

    /**
     * Gets memory statistics.
     *
     * @return the statistics
     */
    CompletableFuture<MemoryStats> getStats();

    /**
     * Reindexes all memories.
     *
     * @return completion future
     */
    CompletableFuture<Void> reindex();

    /**
     * Memory configuration.
     *
     * @param dataDir the data directory
     * @param provider the embedding provider
     * @param maxEntries the maximum entries
     * @param autoEmbed whether to auto-embed on add
     */
    record MemoryConfig(
            Path dataDir,
            EmbeddingProvider provider,
            int maxEntries,
            boolean autoEmbed
    ) {

        /**
         * Creates a builder for MemoryConfig.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for MemoryConfig.
         */
        public static class Builder {
            private Path dataDir;
            private EmbeddingProvider provider;
            private int maxEntries = 100000;
            private boolean autoEmbed = true;

            public Builder dataDir(Path dataDir) {
                this.dataDir = dataDir;
                return this;
            }

            public Builder provider(EmbeddingProvider provider) {
                this.provider = provider;
                return this;
            }

            public Builder maxEntries(int maxEntries) {
                this.maxEntries = maxEntries;
                return this;
            }

            public Builder autoEmbed(boolean autoEmbed) {
                this.autoEmbed = autoEmbed;
                return this;
            }

            public MemoryConfig build() {
                return new MemoryConfig(dataDir, provider, maxEntries, autoEmbed);
            }
        }
    }

    /**
     * Memory statistics.
     *
     * @param totalEntries total entries
     * @param totalEmbeddings total embeddings
     * @param lastIndexed last indexed timestamp
     * @param indexSize index size in bytes
     */
    record MemoryStats(
            int totalEntries,
            int totalEmbeddings,
            Optional<Long> lastIndexed,
            long indexSize
    ) {
    }
}
