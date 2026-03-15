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
}
