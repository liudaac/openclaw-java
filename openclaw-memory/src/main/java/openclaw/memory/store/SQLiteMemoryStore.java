package openclaw.memory.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.memory.Embedding;
import openclaw.memory.MemoryEntry;
import openclaw.memory.MemorySearchResult;
import openclaw.memory.config.MemoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

@Service
@ConditionalOnProperty(name = "openclaw.memory.storage-type", havingValue = "sqlite", matchIfMissing = true)
public class SQLiteMemoryStore {
    
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
    public CompletableFuture<Void> store(MemoryEntry entry, Embedding embedding) {
        MemoryEntry entryWithVector = new MemoryEntry(
            entry.id(),
            entry.text(),
            embedding.vector(),
            entry.metadata(),
            entry.timestamp(),
            entry.sessionKey()
        );
        return store(entryWithVector);
    }
    
    @Override
    public CompletableFuture<Optional<MemoryEntry>> retrieve(String id) {
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
                logger.error("Failed to retrieve memory: {}", id, e);
                throw new RuntimeException("Failed to retrieve memory", e);
            }
            
            return Optional.empty();
        });
    }
    
    @Override
    public CompletableFuture<List<MemorySearchResult>> search(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM memories ORDER BY timestamp DESC LIMIT ?";
            List<MemorySearchResult> results = new ArrayList<>();
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, limit);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        MemoryEntry entry = mapResultSetToEntry(rs);
                        results.add(new MemorySearchResult(entry.id(), 1.0, entry));
                    }
                }
                
            } catch (SQLException e) {
                logger.error("Failed to search memories", e);
                throw new RuntimeException("Failed to search memories", e);
            }
            
            return results;
        });
    }
    
    @Override
    public CompletableFuture<List<MemorySearchResult>> search(Embedding embedding, int limit, double minScore) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM memories";
            List<ScoredEntry> scoredEntries = new ArrayList<>();
            
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    MemoryEntry entry = mapResultSetToEntry(rs);
                    double score = cosineSimilarity(embedding.vector(), entry.vector());
                    
                    if (score >= minScore) {
                        scoredEntries.add(new ScoredEntry(entry, score));
                    }
                }
                
            } catch (SQLException e) {
                logger.error("Failed to search memories by embedding", e);
                throw new RuntimeException("Failed to search memories", e);
            }
            
            return scoredEntries.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(limit)
                .map(se -> new MemorySearchResult(se.entry().id(), se.score(), se.entry()))
                .toList();
        });
    }
    
    @Override
    public CompletableFuture<Boolean> delete(String id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM memories WHERE id = ?";
            
            try (Connection