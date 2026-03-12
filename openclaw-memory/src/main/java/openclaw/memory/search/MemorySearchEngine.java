package openclaw.memory.search;

import openclaw.memory.embedding.EmbeddingProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Memory search engine interface.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface MemorySearchEngine {

    /**
     * Searches for relevant memories.
     *
     * @param query the search query
     * @param options the search options
     * @return the search results
     */
    CompletableFuture<List<MemorySearchResult>> search(String query, SearchOptions options);

    /**
     * Searches with vector similarity.
     *
     * @param queryVector the query vector
     * @param options the search options
     * @return the search results
     */
    CompletableFuture<List<MemorySearchResult>> searchVector(float[] queryVector, SearchOptions options);

    /**
     * Adds a memory entry.
     *
     * @param content the content
     * @param metadata the metadata
     * @return the entry ID
     */
    CompletableFuture<String> addMemory(String content, java.util.Map<String, Object> metadata);

    /**
     * Deletes a memory entry.
     *
     * @param entryId the entry ID
     * @return completion future
     */
    CompletableFuture<Void> deleteMemory(String entryId);

    /**
     * Updates a memory entry.
     *
     * @param entryId the entry ID
     * @param content the new content
     * @param metadata the new metadata
     * @return completion future
     */
    CompletableFuture<Void> updateMemory(String entryId, String content, java.util.Map<String, Object> metadata);

    /**
     * Search options.
     *
     * @param limit the result limit
     * @param minScore the minimum score threshold
     * @param filters additional filters
     * @param hybridWeight the weight for hybrid search (0.0-1.0)
     */
    record SearchOptions(
            int limit,
            double minScore,
            java.util.Map<String, Object> filters,
            double hybridWeight
    ) {

        /**
         * Default search options.
         */
        public static final SearchOptions DEFAULT = new SearchOptions(10, 0.0, java.util.Map.of(), 0.5);

        /**
         * Creates a builder for SearchOptions.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for SearchOptions.
         */
        public static class Builder {
            private int limit = 10;
            private double minScore = 0.0;
            private java.util.Map<String, Object> filters = java.util.Map.of();
            private double hybridWeight = 0.5;

            public Builder limit(int limit) {
                this.limit = limit;
                return this;
            }

            public Builder minScore(double minScore) {
                this.minScore = minScore;
                return this;
            }

            public Builder filters(java.util.Map<String, Object> filters) {
                this.filters = filters != null ? filters : java.util.Map.of();
                return this;
            }

            public Builder hybridWeight(double hybridWeight) {
                this.hybridWeight = hybridWeight;
                return this;
            }

            public SearchOptions build() {
                return new SearchOptions(limit, minScore, filters, hybridWeight);
            }
        }
    }
}
