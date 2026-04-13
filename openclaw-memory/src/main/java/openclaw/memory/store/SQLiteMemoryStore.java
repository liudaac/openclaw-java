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
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Primary
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
        if (config.getFts().isEnabled()) {
            initializeFtsTable();
        }
    }
    
    private void createTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS memories (id TEXT PRIMARY KEY, text TEXT NOT NULL, vector BLOB NOT NULL, metadata TEXT, timestamp INTEGER NOT NULL, session_key TEXT)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_memories_session ON memories(session_key)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_memories_timestamp ON memories(timestamp)");
            logger.info("SQLite memory tables created");
        } catch (SQLException e) {
            logger.error("Failed to create tables", e);
            throw new RuntimeException("Failed to initialize", e);
        }
    }

    private void initializeFtsTable() {
        String tokenizer = config.getFts().getTokenizer();
        String createFtsSql = String.format(
            "CREATE VIRTUAL TABLE IF NOT EXISTS memories_fts USING fts5(" +
            "  content, " +
            "  tokenize='%s'" +
            ")",
            tokenizer
        );
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createFtsSql);
            logger.info("SQLite FTS5 table created with tokenizer: {}", tokenizer);
        } catch (SQLException e) {
            logger.error("Failed to create FTS5 table", e);
            throw new RuntimeException("Failed to initialize FTS5", e);
        }
    }
    
    @Override
    public CompletableFuture<Void> store(MemoryEntry entry) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR REPLACE INTO memories (id, text, vector, metadata, timestamp, session_key) VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, entry.id());
                stmt.setString(2, entry.text());
                stmt.setBytes(3, floatArrayToBytes(entry.vector()));
                stmt.setString(4, toJson(entry.metadata()));
                stmt.setLong(5, entry.timestamp());
                stmt.setString(6, entry.sessionKey());
                stmt.executeUpdate();
                logger.debug("Stored: {}", entry.id());
            } catch (SQLException e) {
                throw new RuntimeException("Store failed", e);
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
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM memories WHERE id = ?")) {
                stmt.setString(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return Optional.of(mapResultSetToEntry(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Retrieve failed", e);
            }
            return Optional.empty();
        });
    }
    
    @Override
    public CompletableFuture<List<MemorySearchResult>> search(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<MemorySearchResult> results = new ArrayList<>();
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM memories ORDER BY timestamp DESC LIMIT ?")) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        MemoryEntry entry = mapResultSetToEntry(rs);
                        results.add(new MemorySearchResult(entry.id(), 1.0, entry));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Search failed", e);
            }
            return results;
        });
    }
    
    @Override
    public CompletableFuture<List<MemorySearchResult>> search(Embedding embedding, int limit, double minScore) {
        return CompletableFuture.supplyAsync(() -> {
            List<ScoredEntry> scored = new ArrayList<>();
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM memories")) {
                while (rs.next()) {
                    MemoryEntry entry = mapResultSetToEntry(rs);
                    double score = cosineSimilarity(embedding.vector(), entry.vector());
                    if (score >= minScore) scored.add(new ScoredEntry(entry, score));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Vector search failed", e);
            }
            return scored.stream().sorted((a, b) -> Double.compare(b.score(), a.score())).limit(limit).map(se -> new MemorySearchResult(se.entry.id(), se.score, se.entry)).collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<Boolean> delete(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM memories WHERE id = ?")) {
                stmt.setString(1, id);
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException("Delete failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> update(String id, MemoryEntry entry) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "UPDATE memories SET text = ?, vector = ?, metadata = ?, timestamp = ?, session_key = ? WHERE id = ?")) {
                stmt.setString(1, entry.text());
                stmt.setBytes(2, floatArrayToBytes(entry.vector()));
                stmt.setString(3, toJson(entry.metadata()));
                stmt.setLong(4, entry.timestamp());
                stmt.setString(5, entry.sessionKey());
                stmt.setString(6, id);
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException("Update failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<MemoryEntry>> getAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<MemoryEntry> results = new ArrayList<>();
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM memories ORDER BY timestamp DESC")) {
                while (rs.next()) results.add(mapResultSetToEntry(rs));
            } catch (SQLException e) {
                throw new RuntimeException("GetAll failed", e);
            }
            return results;
        });
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
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM memories")) {
                long count = rs.next() ? rs.getLong(1) : 0;
                return new StoreStats(count, 0, System.currentTimeMillis());
            } catch (SQLException e) {
                throw new RuntimeException("GetStats failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM memories");
            } catch (SQLException e) {
                throw new RuntimeException("Clear failed", e);
            }
        });
    }
    
    @Override
    public boolean isAvailable() { return true; }
    
    @Override
    public CompletableFuture<Void> initialize() { return CompletableFuture.completedFuture(null); }
    
    @Override
    public void close() { logger.info("SQLiteMemoryStore closed"); }
    
    private MemoryEntry mapResultSetToEntry(ResultSet rs) throws SQLException {
        try {
            return new MemoryEntry(rs.getString("id"), rs.getString("text"), bytesToFloatArray(rs.getBytes("vector")), objectMapper.readValue(rs.getString("metadata"), Map.class), rs.getLong("timestamp"), rs.getString("session_key"));
        } catch (JsonProcessingException e) {
            throw new SQLException("Parse failed", e);
        }
    }
    
    private byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) buffer.putFloat(f);
        return buffer.array();
    }
    
    private float[] bytesToFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) floats[i] = buffer.getFloat();
        return floats;
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
    
    private record ScoredEntry(MemoryEntry entry, double score) {}
}