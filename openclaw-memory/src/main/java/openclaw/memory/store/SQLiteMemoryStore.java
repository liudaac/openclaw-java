package openclaw.memory.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.memory.Embedding;
import openclaw.memory.MemoryEntry;
import openclaw.memory.MemorySearchResult;
import openclaw.memory.MemoryStore;
import openclaw.memory.config.MemoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * SQLite-based memory store implementation
 * 
 * <p>This is the default (short-term) implementation with minimal dependencies.
 * Uses brute-force search (O(n)) suitable for small-scale deployments.</p>
 */
@Service
@ConditionalOnProperty(name = "openclaw.memory.storage-type", havingValue = "sqlite", matchIfMissing = true)
public class SQLiteMemoryStore implements MemoryStore {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLiteMemoryStore.class);
    
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final MemoryConfig config;
    
    public SQLiteMemoryStore(DataSource dataSource, ObjectMapper objectMapper, MemoryConfig config) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.config = config;
    }
    
    @PostConstruct
    public void init() {
        logger.info("Initializing SQLite memory store");
        createTables();
    }
    
    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS memories (
                id TEXT PRIMARY KEY,
                text TEXT NOT NULL,
                vector BLOB NOT NULL,
                metadata TEXT,
                timestamp INTEGER NOT NULL,
                session_key TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_memories_session ON memories(session_key);
            CREATE INDEX IF NOT EXISTS idx_memories_timestamp ON memories(timestamp);
        """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("SQLite memory tables created successfully");
        } catch (SQLException e) {
            logger.error("Failed to create SQLite tables", e);
            throw new RuntimeException("Failed to initialize SQLite memory store", e);
        }
    }
    
    @Override
    public CompletableFuture<Void> store(MemoryEntry entry) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO memories (id, text, vector, metadata, timestamp, session_key)
                VALUES (?, ?, ?, ?, ?, ?)
            """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, entry.id());
                stmt.setString(2, entry.text());
                stmt.setBytes(3, floatArrayToBytes(entry.vector()));
                stmt.setString(4, objectMapper.writeValueAsString(entry.metadata()));
                stmt.setLong(5, entry.timestamp());
                stmt.setString(6, entry.sessionKey());
                
                stmt.executeUpdate();
                logger.debug("Stored memory: {}", entry.id());
                
            } catch (SQLException | JsonProcessingException e) {
                logger.error("Failed to store memory: {}", entry.id(), e);
                throw new RuntimeException("Failed to store memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> search(String query, int limit) {
        // This method is for text-based search
        // For vector search, use searchByVector
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM memories ORDER BY timestamp DESC LIMIT ?";
            List<MemoryEntry> results = new ArrayList<>();
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, limit * 10); // Load more for filtering
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapResultSetToEntry(rs));
                    }
                }
                
            } catch (SQLException e) {
                logger.error("Failed to search memories", e);
                throw new RuntimeException("Failed to search memories", e);
            }
            
            return results.stream().limit(limit).toList();
        });
    }
    
    /**
     * Vector-based search using brute-force cosine similarity
     */
    public CompletableFuture<List<MemorySearchResult>> searchByVector(float[] queryVector, int limit, double minScore) {
        return CompletableFuture.supplyAsync(() -> {
            // Load all entries (brute-force approach)
            String sql = "SELECT * FROM memories";
            List<ScoredEntry> scoredEntries = new ArrayList<>();
            
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    MemoryEntry entry = mapResultSetToEntry(rs);
                    double score = cosineSimilarity(queryVector, entry.vector());
                    
                    if (score >= minScore) {
                        scoredEntries.add(new ScoredEntry(entry, score));
                    }
                }
                
            } catch (SQLException e) {
                logger.error("Failed to search memories by vector", e);
                throw new RuntimeException("Failed to search memories", e);
            }
            
            // Sort by score and limit
            return scoredEntries.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(limit)
                .map(se -> new MemorySearchResult(se.entry().id(), se.score(), se.entry()))
                .toList();
        });
    }
    
    @Override
    public CompletableFuture<Optional<MemoryEntry>> get(String id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM memories WHERE id = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToEntry(rs));
                    }
                }
                
            } catch (SQLException e) {
                logger.error("Failed to get memory: {}", id, e);
                throw new RuntimeException("Failed to get memory", e);
            }
            
            return Optional.empty();
        });
    }
    
    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM memories WHERE id = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, id);
                stmt.executeUpdate();
                logger.debug("Deleted memory: {}", id);
                
            } catch (SQLException e) {
                logger.error("Failed to delete memory: {}", id, e);
                throw new RuntimeException("Failed to delete memory", e);
            }
        });
    }
    
    /**
     * Load all memories (for cache warmup)
     */
    public CompletableFuture<List<MemoryEntry>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM memories ORDER BY timestamp DESC";
            List<MemoryEntry> results = new ArrayList<>();
            
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    results.add(mapResultSetToEntry(rs));
                }
                
            } catch (SQLException e) {
                logger.error("Failed to load all memories", e);
                throw new RuntimeException("Failed to load memories", e);
            }
            
            return results;
        });
    }
    
    private MemoryEntry mapResultSetToEntry(ResultSet rs) throws SQLException {
        try {
            return new MemoryEntry(
                rs.getString("id"),
                rs.getString("text"),
                bytesToFloatArray(rs.getBytes("vector")),
                objectMapper.readValue(rs.getString("metadata"), Map.class),
                rs.getLong("timestamp"),
                rs.getString("session_key")
            );
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to parse metadata", e);
        }
    }
    
    private byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }
    
    private float[] bytesToFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
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
    
    private record ScoredEntry(MemoryEntry entry, double score) {}
}
