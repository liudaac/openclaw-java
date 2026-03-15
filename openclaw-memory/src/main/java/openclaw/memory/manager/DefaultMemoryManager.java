package openclaw.memory.manager;

import openclaw.memory.embedding.EmbeddingProvider;
import openclaw.memory.search.MemorySearchEngine;

import java.util.concurrent.CompletableFuture;

/**
 * Default memory manager implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class DefaultMemoryManager implements MemoryManager {

    private MemoryConfig config;
    private EmbeddingProvider embeddingProvider;
    private MemorySearchEngine searchEngine;
    private boolean initialized = false;

    @Override
    public CompletableFuture<Void> initialize(MemoryConfig config) {
        return CompletableFuture.runAsync(() -> {
            this.config = config;
            this.embeddingProvider = config.provider();
            
            // Initialize SQLite-based search engine
            this.searchEngine = new SQLiteMemorySearchEngine(embeddingProvider, config.dataDir());
            
            // Initialize the search engine
            searchEngine.initialize().join();
            
            this.initialized = true;
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            if (searchEngine != null) {
                searchEngine.close();
            }
            initialized = false;
        });
    }

    @Override
    public EmbeddingProvider getEmbeddingProvider() {
        ensureInitialized();
        return embeddingProvider;
    }

    @Override
    public MemorySearchEngine getSearchEngine() {
        ensureInitialized();
        return searchEngine;
    }

    @Override
    public CompletableFuture<MemorySearchEngine.MemoryStats> getStats() {
        ensureInitialized();
        return searchEngine.getStats();
    }

    @Override
    public CompletableFuture<Void> reindex() {
        ensureInitialized();
        return searchEngine.reindex();
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("MemoryManager not initialized");
        }
    }
}
