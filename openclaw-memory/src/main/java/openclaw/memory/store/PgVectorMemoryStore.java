package openclaw.memory.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.memory.MemoryEntry;
import openclaw.memory.MemorySearchResult;
import openclaw.memory.MemoryStore;
import openclaw.memory.config.MemoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * PostgreSQL with pgvector extension memory store implementation
 * 
 * <p>This is the long-term solution for production deployments.
 * Uses HNSW index for efficient vector search (O(log n)).</p>
 * 
 * <p>Requires PostgreSQL with pgvector extension:</p>
 * <pre>CREATE EXTENSION vector;</pre>
 */
@Service
@ConditionalOnProperty(name = "openclaw.memory.storage-type", havingValue = "pgvector")
public class PgVectorMemoryStore implements MemoryStore {
    
    private static final Logger logger = LoggerFactory.getLogger(PgVectorMemoryStore.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final MemoryConfig config;
    
    private final RowMapper<MemoryEntry> entryRowMapper = (rs, rowNum) -> {
        try {
            return new MemoryEntry(
                rs.getString("id"),
                rs.getString("text"),
                vectorStringToFloatArray(rs.getString("vector")),
                objectMapper.readValue(rs.getString("metadata"), Map.class),
                rs.getLong("timestamp"),
                rs.getString("session_key")
            );
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to parse metadata", e);
        }
    };
    
    public PgVectorMemoryStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, MemoryConfig config) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.config = config;
    }
    
    @PostConstruct
    public void init() {
        logger.info("Initializing pgvector memory store");
        createExtension();
        createTables();
    }
    
    private void createExtension() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            logger.info("pgvector extension created/verified");
        } catch (Exception e) {
            logger.error("Failed to create pgvector extension. Make sure pgvector is installed.", e);
            throw new RuntimeException("pgvector extension not available", e);
        }
    }
    
    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS memories (
                id TEXT PRIMARY KEY,
                text TEXT NOT NULL,
                vector vector(%d),
                metadata JSONB,
                timestamp BIGINT NOT NULL,
                session_key TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_memories_session ON memories(session_key);
            CREATE INDEX IF NOT EXISTS idx_memories_timestamp ON memories(timestamp);
        """.formatted(config.getVectorSearch().getDimension());
        
        jdbcTemplate.execute(sql);
        
        // Create HNSW index for vector search
        String hnswSql = """
            CREATE INDEX IF NOT EXISTS idx_memories_vector_hnsw 
            ON memories USING hnsw (vector vector_cosine_ops)
            WITH (m = 16, ef_construction = 64);
        """;
        
        try {
            jdbcTemplate.execute(hnswSql);
            logger.info("HNSW index created for vector search");
        } catch (Exception e) {
            logger.warn("Failed to create HNSW index, falling back to exact search", e);
        }
        
        logger.info("pgvector memory tables created successfully");
    }
    
    @Override
    public CompletableFuture<Void> store(MemoryEntry entry) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO memories (id, text, vector, metadata, timestamp, session_key)
                VALUES (?, ?, ?::vector, ?::jsonb, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    text = EXCLUDED.text,
                    vector = EXCLUDED.vector,
                    metadata = EXCLUDED.metadata,
                    timestamp = EXCLUDED.timestamp,
                    session_key = EXCLUDED.session_key
            """;
            
            try {
                String vectorStr = floatArrayToVectorString(entry.vector());
                String metadataStr = objectMapper.writeValueAsString(entry.metadata());
                
                jdbcTemplate.update(sql,
                    entry.id(),
                    entry.text(),
                    vectorStr,
                    metadataStr,
                    entry.timestamp(),
                    entry.sessionKey()
                );
                
                logger.debug("Stored memory in pgvector: {}", entry.id());
                
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize metadata for memory: {}", entry.id(), e);
                throw new RuntimeException("Failed to store memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> search(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT * FROM memories
                ORDER BY timestamp DESC
                LIMIT ?
            """;
            
            return jdbcTemplate.query(sql, entryRowMapper, limit);
        });
    }
    
    /**
     * Vector-based search using pgvector's HNSW index
     */
    public CompletableFuture<List<MemorySearchResult>> searchByVector(float[] queryVector, int limit, double minScore) {
        return CompletableFuture.supplyAsync(() -> {
            String vectorStr = floatArrayToVectorString(queryVector);
            
            // Use HNSW index for approximate nearest neighbor search
            String sql = """
                SELECT *, vector <=> ?::vector AS distance
                FROM memories
                ORDER BY vector <=> ?::vector
                LIMIT ?
            """;
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                try {
                    double distance = rs.getDouble("distance");
                    double score = 1.0 - distance; // Convert distance to similarity
                    
                    if (score < minScore) {
                        return null;
                    }
                    
                    MemoryEntry entry = new MemoryEntry(
                        rs.getString("id"),
                        rs.getString("text"),
                        vectorStringToFloatArray(rs.getString("vector")),
                        objectMapper.readValue(rs.getString("metadata"), Map.class),
                        rs.getLong("timestamp"),
                        rs.getString("session_key")
                    );
                    
                    return new MemorySearchResult(entry.id(), score, entry);
                } catch (JsonProcessingException e) {
                    throw new SQLException("Failed to parse metadata", e);
                }
            }, vectorStr, vectorStr, limit)
            .stream()
            .filter(result -> result != null)
            .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<Optional<MemoryEntry>> get(String id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM memories WHERE id = ?";
            
            List<MemoryEntry> results = jdbcTemplate.query(sql, entryRowMapper, id);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        });
    }
    
    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM memories WHERE id = ?";
            jdbcTemplate.update(sql, id);
            logger.debug("Deleted memory from pgvector: {}", id);
        });
    }
    
    /**
     * Load all memories for cache warmup
     */
    public CompletableFuture<List<MemoryEntry>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM memories ORDER BY timestamp DESC";
            return jdbcTemplate.query(sql, entryRowMapper);
        });
    }
    
    /**
     * Get memory statistics
     */
    public CompletableFuture<Map<String, Object>> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) as count FROM memories";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            
            return Map.of(
                "totalMemories", count != null ? count : 0,
                "storageType", "pgvector",
                "dimension", config.getVectorSearch().getDimension()
            );
        });
    }
    
    private String floatArrayToVectorString(float[] floats) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < floats.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(floats[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    private float[] vectorStringToFloatArray(String vectorStr) {
        // Remove brackets and split
        String content = vectorStr.substring(1, vectorStr.length() - 1);
        String[] parts = content.split(",");
        
        float[] floats = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            floats[i] = Float.parseFloat(parts[i].trim());
        }
        return floats;
    }
}
