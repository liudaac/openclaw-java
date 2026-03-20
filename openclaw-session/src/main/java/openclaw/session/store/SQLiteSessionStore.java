package openclaw.session.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openclaw.session.model.Message;
import openclaw.session.model.Session;
import openclaw.session.model.SessionStatus;
import openclaw.session.config.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * SQLite implementation of SessionStore.
 *
 * @author OpenClaw Team
 * @version 2026.3.20
 */
@Service
public class SQLiteSessionStore implements SessionStore {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteSessionStore.class);

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final SessionConfig config;

    public SQLiteSessionStore(DataSource dataSource, SessionConfig config) {
        this.dataSource = dataSource;
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing SQLite session store");
        initialize().join();
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down SQLite session store");
        close().join();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        id TEXT PRIMARY KEY,
                        session_key TEXT NOT NULL UNIQUE,
                        model TEXT,
                        status TEXT NOT NULL,
                        metadata TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        last_activity_at INTEGER NOT NULL,
                        total_input_tokens INTEGER DEFAULT 0,
                        total_output_tokens INTEGER DEFAULT 0,
                        error_message TEXT
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id TEXT PRIMARY KEY,
                        session_id TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT,
                        tool_name TEXT,
                        tool_call_id TEXT,
                        tool_result TEXT,
                        metadata TEXT,
                        created_at INTEGER NOT NULL,
                        token_count INTEGER DEFAULT 0,
                        FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
                    )
                """);

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_key ON sessions(session_key)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions(status)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_activity ON sessions(last_activity_at)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_session ON messages(session_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at)");
                stmt.execute("PRAGMA foreign_keys = ON");

                logger.info("SQLite session store initialized");
            } catch (SQLException e) {
                logger.error("Failed to initialize SQLite session store", e);
                throw new RuntimeException("Failed to initialize session store", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveSession(Session session) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO sessions 
                (id, session_key, model, status, metadata, created_at, updated_at, 
                 last_activity_at, total_input_tokens, total_output_tokens, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, session.getId());
                stmt.setString(2, session.getSessionKey());
                stmt.setString(3, session.getModel());
                stmt.setString(4, session.getStatus().name());
                stmt.setString(5, toJson(session.getMetadata()));
                stmt.setLong(6, session.getCreatedAt().toEpochMilli());
                stmt.setLong(7, session.getUpdatedAt().toEpochMilli());
                stmt.setLong(8, session.getLastActivityAt().toEpochMilli());
                stmt.setInt(9, session.getTotalInputTokens());
                stmt.setInt(10, session.getTotalOutputTokens());
                stmt.setString(11, session.getErrorMessage());
                
                stmt.executeUpdate();
                logger.debug("Saved session: {}", session.getId());
            } catch (SQLException e) {
                logger.error("Failed to save session: {}", session.getId(), e);
                throw new RuntimeException("Failed to save session", e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Session>> findSessionById(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM sessions WHERE id = ?";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToSession(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to find session by id: {}", sessionId, e);
                throw new RuntimeException("Failed to find session", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<Session>> findSessionByKey(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM sessions WHERE session_key = ?";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionKey);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToSession(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to find session by key: {}", sessionKey, e);
                throw new RuntimeException("Failed to find session", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<List<Session>> findAllSessions() {
        return CompletableFuture.supplyAsync(() -> {
            List<Session> sessions = new ArrayList<>();
            String sql = "SELECT * FROM sessions ORDER BY created_at DESC";
            
            try (Connection conn = dataSource.getConnection(); 
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            } catch (SQLException e) {
                logger.error("Failed to find all sessions", e);
                throw new RuntimeException("Failed to find sessions", e);
            }
            return sessions;
        });
    }

    @Override
    public CompletableFuture<List<Session>> findSessionsByStatus(SessionStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            List<Session> sessions = new ArrayList<>();
            String sql = "SELECT * FROM sessions WHERE status = ? ORDER BY created_at DESC";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status.name());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        sessions.add(mapResultSetToSession(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to find sessions by status: {}", status, e);
                throw new RuntimeException("Failed to find sessions", e);
            }
            return sessions;
        });
    }

    @Override
    public CompletableFuture<List<Session>> findActiveSessions() {
        return CompletableFuture.supplyAsync(() -> {
            List<Session> sessions = new ArrayList<>();
            String sql = "SELECT * FROM sessions WHERE status IN (?, ?) ORDER BY last_activity_at DESC";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, SessionStatus.ACTIVE.name());
                stmt.setString(2, SessionStatus.PAUSED.name());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        sessions.add(mapResultSetToSession(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to find active sessions", e);
                throw new RuntimeException("Failed to find sessions", e);
            }
            return sessions;
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteSession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM sessions WHERE id = ?";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                int rows = stmt.executeUpdate();
                logger.debug("Deleted session: {} (rows: {})", sessionId, rows);
                return rows > 0;
            } catch (SQLException e) {
                logger.error("Failed to delete session: {}", sessionId, e);
                throw new RuntimeException("Failed to delete session", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateSessionStatus(String sessionId, SessionStatus status) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE sessions SET status = ?, updated_at = ? WHERE id = ?";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status.name());
                stmt.setLong(2, System.currentTimeMillis());
                stmt.setString(3, sessionId);
                stmt.executeUpdate();
                logger.debug("Updated session {} status to {}", sessionId, status);
            } catch (SQLException e) {
                logger.error("Failed to update session status: {}", sessionId, e);
                throw new RuntimeException("Failed to update session status", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateSessionActivity(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE sessions SET last_activity_at = ?, updated_at = ? WHERE id = ?";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                long now = System.currentTimeMillis();
                stmt.setLong(1, now);
                stmt.setLong(2, now);
                stmt.setString(3, sessionId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.error("Failed to update session activity: {}", sessionId, e);
                throw new RuntimeException("Failed to update session activity", e);
            }
        });
    }

    // Message operations
    @Override
    public CompletableFuture<Void> saveMessage(Message message) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO messages 
                (id, session_id, role, content, tool_name, tool_call_id, tool_result, metadata, created_at, token_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, message.getId());
                stmt.setString(2, message.getSessionId());
                stmt.setString(3, message.getRole());
                stmt.setString(4, message.getContent());
                stmt.setString(5, message.getToolName());
                stmt.setString(6, message.getToolCallId());
                stmt.setString(7, toJson(message.getToolResult()));
                stmt.setString(8, toJson(message.getMetadata()));
                stmt.setLong(9, message.getCreatedAt().toEpochMilli());
                stmt.setInt(10, message.getTokenCount());
                
                stmt.executeUpdate();
                logger.debug("Saved message: {} for session: {}", message.getId(), message.getSessionId());
            } catch (SQLException e) {
                logger.error("Failed to save message: {}", message.getId(), e);
                throw new RuntimeException("Failed to save message", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<Message>> findMessagesBySession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Message> messages = new ArrayList<>();
            String sql = "SELECT * FROM messages WHERE session_id = ? ORDER BY created_at ASC";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        messages.add(mapResultSetToMessage(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to find messages for session: {}", sessionId, e);
                throw new RuntimeException("Failed to find messages", e);
            }
            return messages;
        });
    }

    @Override
    public CompletableFuture<List<Message>> findMessagesBySession(String sessionId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Message> messages = new ArrayList<>();
            String sql = "SELECT * FROM messages WHERE session_id = ? ORDER BY created_at DESC LIMIT ?";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                stmt.setInt(2, limit);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        messages.add(0, mapResultSetToMessage(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to find messages for session: {}", sessionId, e);
                throw new RuntimeException("Failed to find messages", e);
            }
            return messages;
        });
    }

    @Override
    public CompletableFuture<Optional<Message>> findMessageById(String messageId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM messages WHERE id = ?";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, messageId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToMessage(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to find message by id: {}", messageId, e);
                throw new RuntimeException("Failed to find message", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Void> deleteMessagesBySession(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM messages WHERE session_id = ?";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                stmt.executeUpdate();
                logger.debug("Deleted messages for session: {}", sessionId);
            } catch (SQLException e) {
                logger.error("Failed to delete messages for session: {}", sessionId, e);
                throw new RuntimeException("Failed to delete messages", e);
            }
        });
    }

    // Search operations
    @Override
    public CompletableFuture<List<Session>> searchSessions(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            List<Session> sessions = new ArrayList<>();
            String sql = "SELECT * FROM sessions WHERE session_key LIKE ? OR metadata LIKE ? ORDER BY created_at DESC";
            String pattern = "%" + keyword + "%";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        sessions.add(mapResultSetToSession(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to search sessions: {}", keyword, e);
                throw new RuntimeException("Failed to search sessions", e);
            }
            return sessions;
        });
    }

    @Override
    public CompletableFuture<List<Message>> searchMessages(String sessionId, String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            List<Message> messages = new ArrayList<>();
            String sql = "SELECT * FROM messages WHERE session_id = ? AND content LIKE ? ORDER BY created_at ASC";
            String pattern = "%" + keyword + "%";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                stmt.setString(2, pattern);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        messages.add(mapResultSetToMessage(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to search messages: {}", keyword, e);
                throw new RuntimeException("Failed to search messages", e);
            }
            return messages;
        });
    }

    // Statistics
    @Override
    public CompletableFuture<SessionStats> getSessionStats(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT 
                    COUNT(*) as total_messages,
                    COALESCE(SUM(token_count), 0) as total_tokens,
                    MIN(created_at) as first_message_at,
                    MAX(created_at) as last_message_at
                FROM messages 
                WHERE session_id = ?
            """;
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int totalMessages = rs.getInt("total_messages");
                        int totalTokens = rs.getInt("total_tokens");
                        long firstMsgMs = rs.getLong("first_message_at");
                        long lastMsgMs = rs.getLong("last_message_at");
                        
                        Instant firstMessageAt = firstMsgMs > 0 ? Instant.ofEpochMilli(firstMsgMs) : null;
                        Instant lastMessageAt = lastMsgMs > 0 ? Instant.ofEpochMilli(lastMsgMs) : null;
                        
                        return new SessionStats(totalMessages, totalTokens, firstMessageAt, lastMessageAt);
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to get session stats: {}", sessionId, e);
                throw new RuntimeException("Failed to get session stats", e);
            }
            return new SessionStats(0, 0, null, null);
        });
    }

    @Override
    public CompletableFuture<List<Session>> findRecentSessions(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Session> sessions = new ArrayList<>();
            String sql = "SELECT * FROM sessions ORDER BY last_activity_at DESC LIMIT ?";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        sessions.add(mapResultSetToSession(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to find recent sessions", e);
                throw new RuntimeException("Failed to find sessions", e);
            }
            return sessions;
        });
    }

    @Override
    public CompletableFuture<List<Session>> findSessionsByDateRange(Instant start, Instant end) {
        return CompletableFuture.supplyAsync(() -> {
            List<Session> sessions = new ArrayList<>();
            String sql = "SELECT * FROM sessions WHERE created_at BETWEEN ? AND ? ORDER BY created_at DESC";
            
            try (Connection conn = dataSource.getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, start.toEpochMilli());
                stmt.setLong(2, end.toEpochMilli());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        sessions.add(mapResultSetToSession(rs));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to find sessions by date range", e);
                throw new RuntimeException("Failed to find sessions", e);
            }
            return sessions;
        });
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            logger.info("SQLite session store closed");
        });
    }

    // Helper methods
    private Session mapResultSetToSession(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setId(rs.getString("id"));
        session.setSessionKey(rs.getString("session_key"));
        session.setModel(rs.getString("model"));
        session.setStatus(SessionStatus.valueOf(rs.getString("status")));
        session.setMetadata(fromJson(rs.getString("metadata")));
        session.setCreatedAt(Instant.ofEpochMilli(rs.getLong("created_at")));
        session.setUpdatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")));
        session.setLastActivityAt(Instant.ofEpochMilli(rs.getLong("last_activity_at")));
        session.setTotalInputTokens(rs.getInt("total_input_tokens"));
        session.setTotalOutputTokens(rs.getInt("total_output_tokens"));
        session.setErrorMessage(rs.getString("error_message"));
        return session;
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getString("id"));
        message.setSessionId(rs.getString("session_id"));
        message.setRole(rs.getString("role"));
        message.setContent(rs.getString("content"));
        message.setToolName(rs.getString("tool_name"));
        message.setToolCallId(rs.getString("tool_call_id"));
        message.setToolResult(fromJson(rs.getString("tool_result")));
        message.setMetadata(fromJson(rs.getString("metadata")));
        message.setCreatedAt(Instant.ofEpochMilli(rs.getLong("created_at")));
        message.setTokenCount(rs.getInt("token_count"));
        return message;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize to JSON", e);
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize JSON: {}", json, e);
            return null;
        }
    }
}