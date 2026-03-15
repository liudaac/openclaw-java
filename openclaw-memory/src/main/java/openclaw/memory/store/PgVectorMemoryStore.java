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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
        int dimension = config.getVectorSearch().getDimension();
        
        String sql = String.format("""
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
        """, dimension);
        
        jdbcTemplate.execute(sql);
        
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
        
        logger.info("pgvector memory tables created");
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
                
                jdbcTemplate.update(sql, entry.id(), entry.text(), vectorStr, metadataStr, entry.timestamp(), entry.sessionKey());
                logger.debug("Stored memory in pgvector: {}", entry.id());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to store memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> store(MemoryEntry entry, Embedding embedding) {
        return store(new MemoryEntry(entry.id(), entry.text(), embedding.vector(), entry.metadata(), entry.timestamp(), entry.sessionKey()));
    }
    
    @Override
    public CompletableFuture<Optional<MemoryEntry>> retrieve(String id) {
        return CompletableFuture.supplyAsync(() -> {
            List<MemoryEntry> results = jdbcTemplate.query("SELECT * FROM memories WHERE id = ?", entryRowMapper, id);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        });
    }
    
    @Override
    public CompletableFuture<List<MemorySearchResult>> search(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM memories ORDER BY timestamp DESC LIMIT ?";
            return jdbcTemplate.query(sql, entryRowMapper, limit).stream()
                .map(e -> new MemorySearchResult(e.id(), 1.0, e))
                .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<List<MemorySearchResult>> search(Embedding embedding, int limit, double minScore) {
        return CompletableFuture.supplyAsync(() -> {
            String vectorStr = floatArrayToVectorString(embedding.vector());
            String sql = "SELECT *, vector <=> ?::vector AS distance FROM memories ORDER BY vector <=> ?::vector LIMIT ?";
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                try {
                    double score = 1.0 - rs.getDouble("distance");
                    if (score < minScore) return null;
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
                    throw new SQLException("Parse failed", e);
                }
            }, vectorStr, vectorStr, limit).stream().filter(r -> r != null).collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<Boolean> delete(String id) {
        return CompletableFuture.supplyAsync(() -> {
            int rows = jdbcTemplate.update("DELETE FROM memories WHERE id = ?", id);
            logger.debug("Deleted memory from pgvector: {}", id);
            return rows > 0;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> update(String id, MemoryEntry entry) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String vectorStr = floatArrayToVectorString(entry.vector());
                String metadataStr = objectMapper.writeValueAsString(entry.metadata());
                int rows = jdbcTemplate.update(
                    "UPDATE memories SET text = ?, vector = ?::vector, metadata = ?::jsonb, timestamp = ?, session_key = ? WHERE id = ?",
                    entry.text(), vectorStr, metadataStr, entry.timestamp(), entry.sessionKey(), id);
                return rows > 0;
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Update failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> getAll() {
        return CompletableFuture.supplyAsync(() -> jdbcTemplate.query("SELECT * FROM memories ORDER BY timestamp DESC", entryRowMapper));
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> getByMetadata(Map<String, String> metadataFilter) {
        return CompletableFuture.supplyAsync(() -> {
            return getAll().join().stream().filter(entry -> {
                Map<String, Object> metadata = entry.metadata();
                return metadataFilter.entrySet().stream().allMatch(e -> metadata.getOrDefault(e.getKey(), "").toString().equals(e.getValue()));
            }).collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<StoreStats> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM memories", Integer.class);
            return new StoreStats(count != null ? count : 0, 0, System.currentTimeMillis());
        });
    }
    
    @Override
    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> jdbcTemplate.update("DELETE FROM memories"));
    }
    
    @Override
    public boolean isAvailable() { return true; }
    
    @Override
    public CompletableFuture<Void> initialize() { return CompletableFuture.completedFuture(null); }
    
    @Override
    public void close() { logger.info("PgVectorMemoryStore closed"); }
    
    private String floatArrayToVectorString(float[] floats) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < floats.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(floats[i]);
        }
        return sb.append("]").toString();
    }
    
    private float[] vectorStringToFloatArray(String vectorStr) {
        String[] parts = vectorStr.substring(1, vectorStr.length() - 1).split(",");
        float[] floats = new float[parts.length];
        for (int i = 0; i < parts.length; i++) floats[i] = Float.parseFloat(parts[i].trim());
        return floats;
    }
}