package openclaw.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Memory store interface for persisting and retrieving memories.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface MemoryStore {

    /**
     * Store a memory entry.
     *
     * @param entry the memory entry to store
     * @return future that completes when the entry is stored
     */
    CompletableFuture<Void> store(MemoryEntry entry);

    /**
     * Store a memory entry with embedding.
     *
     * @param entry the memory entry
     * @param embedding the embedding vector
     * @return future that completes when stored
     */
    CompletableFuture<Void> store(MemoryEntry entry, Embedding embedding);

    /**
     * Retrieve a memory entry by ID.
     *
     * @param id the memory entry ID
     * @return future with the optional memory entry
     */
    CompletableFuture<Optional<MemoryEntry>> retrieve(String id);

    /**
     * Search for memories by text similarity.
     *
     * @param query the search query
     * @param limit maximum number of results
     * @return future with search results
     */
    CompletableFuture<List<MemorySearchResult>> search(String query, int limit);

    /**
     * Search for memories by vector similarity.
     *
     * @param embedding the query embedding
     * @param limit maximum number of results
     * @param minScore minimum similarity score
     * @return future with search results
     */
    CompletableFuture<List<MemorySearchResult>> search(Embedding embedding, int limit, double minScore);

    /**
     * Delete a memory entry by ID.
     *
     * @param id the memory entry ID
     * @return future that completes when deleted
     */
    CompletableFuture<Boolean> delete(String id);

    /**
     * Update a memory entry.
     *
     * @param id the memory entry ID
     * @param entry the updated entry
     * @return future that completes when updated
     */
    CompletableFuture<Boolean> update(String id, MemoryEntry entry);

    /**
     * Get all memory entries.
     *
     * @return future with list of all entries
     */
    CompletableFuture<List<MemoryEntry>> getAll();

    /**
     * Get memory entries by metadata filter.
     *
     * @param metadataFilter metadata key-value pairs to filter by
     * @return future with filtered entries
     */
    CompletableFuture<List<MemoryEntry>> getByMetadata(Map<String, String> metadataFilter);

    /**
     * Get store statistics.
     *
     * @return future with store statistics
     */
    CompletableFuture<StoreStats> getStats();

    /**
     * Clear all memories.
     *
     * @return future that completes when cleared
     */
    CompletableFuture<Void> clear();

    /**
     * Check if the store is available.
     *
     * @return true if available
     */
    boolean isAvailable();

    /**
     * Initialize the store.
     *
     * @return future that completes when initialized
     */
    CompletableFuture<Void> initialize();

    /**
     * Close the store and release resources.
     */
    void close();

    /**
     * Store statistics record.
     */
    record StoreStats(
            long totalEntries,
            long totalSizeBytes,
            long lastAccessTime
    ) {}
}
