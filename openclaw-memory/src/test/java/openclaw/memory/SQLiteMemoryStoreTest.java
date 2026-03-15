package openclaw.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.memory.config.MemoryConfig;
import openclaw.memory.store.SQLiteMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SQLiteMemoryStore
 */
class SQLiteMemoryStoreTest {

    private SQLiteMemoryStore memoryStore;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create temporary SQLite database
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("test.db").toString();
        DataSource dataSource = new DataSource() {
            @Override public Connection getConnection() throws SQLException { return DriverManager.getConnection(dbUrl); }
            @Override public Connection getConnection(String username, String password) throws SQLException { return DriverManager.getConnection(dbUrl, username, password); }
            @Override public PrintWriter getLogWriter() { return null; }
            @Override public void setLogWriter(PrintWriter out) {}
            @Override public void setLoginTimeout(int seconds) {}
            @Override public int getLoginTimeout() { return 0; }
            @Override public Logger getParentLogger() { return Logger.getLogger(""); }
            @Override public <T> T unwrap(Class<T> iface) { return null; }
            @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        };
        
        objectMapper = new ObjectMapper();
        MemoryConfig config = new MemoryConfig();
        
        memoryStore = new SQLiteMemoryStore(dataSource, objectMapper, config);
        memoryStore.init();
    }

    @Test
    void testStoreAndRetrieve() throws Exception {
        // Create test entry
        String id = UUID.randomUUID().toString();
        float[] vector = new float[1536];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) Math.random();
        }
        
        MemoryEntry entry = MemoryEntry.builder()
            .id(id)
            .text("Test memory content")
            .vector(vector)
            .metadata(Map.of("key", "value"))
            .timestamp(System.currentTimeMillis())
            .sessionKey("session-123")
            .build();

        // Store
        CompletableFuture<Void> storeFuture = memoryStore.store(entry);
        storeFuture.get();

        // Retrieve
        CompletableFuture<Optional<MemoryEntry>> retrieveFuture = memoryStore.get(id);
        Optional<MemoryEntry> retrieved = retrieveFuture.get();

        assertTrue(retrieved.isPresent());
        assertEquals(id, retrieved.get().id());
        assertEquals("Test memory content", retrieved.get().text());
        assertEquals("session-123", retrieved.get().sessionKey());
    }

    @Test
    void testSearchByVector() throws Exception {
        // Store multiple entries
        for (int i = 0; i < 5; i++) {
            float[] vector = new float[1536];
            vector[0] = (float) i / 5; // Create different vectors
            
            MemoryEntry entry = MemoryEntry.builder()
                .id(UUID.randomUUID().toString())
                .text("Memory " + i)
                .vector(vector)
                .metadata(Map.of("index", i))
                .timestamp(System.currentTimeMillis())
                .build();
            
            memoryStore.store(entry).get();
        }

        // Search with query vector similar to entry 3
        float[] queryVector = new float[1536];
        queryVector[0] = 0.6f; // Close to entry 3 (0.6)

        CompletableFuture<List<MemorySearchResult>> searchFuture = 
            memoryStore.searchByVector(queryVector, 3, 0.0);
        List<MemorySearchResult> results = searchFuture.get();

        assertFalse(results.isEmpty());
        // Results should be sorted by similarity
        assertTrue(results.get(0).score() >= results.get(1).score());
    }

    @Test
    void testDelete() throws Exception {
        String id = UUID.randomUUID().toString();
        MemoryEntry entry = MemoryEntry.builder()
            .id(id)
            .text("To be deleted")
            .vector(new float[1536])
            .timestamp(System.currentTimeMillis())
            .build();

        // Store
        memoryStore.store(entry).get();

        // Verify exists
        assertTrue(memoryStore.get(id).get().isPresent());

        // Delete
        memoryStore.delete(id).get();

        // Verify deleted
        assertFalse(memoryStore.get(id).get().isPresent());
    }

    @Test
    void testLoadAll() throws Exception {
        // Store entries
        for (int i = 0; i < 3; i++) {
            MemoryEntry entry = MemoryEntry.builder()
                .id(UUID.randomUUID().toString())
                .text("Entry " + i)
                .vector(new float[1536])
                .timestamp(System.currentTimeMillis())
                .build();
            memoryStore.store(entry).get();
        }

        // Load all
        List<MemoryEntry> all = memoryStore.loadAll().get();
        assertEquals(3, all.size());
    }
}
