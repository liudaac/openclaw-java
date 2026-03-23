package openclaw.session.store;

import openclaw.session.model.Message;
import openclaw.session.model.Session;
import openclaw.session.model.SessionStatus;
import openclaw.session.config.SessionConfig;
import openclaw.session.store.SessionStore.SessionStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SQLiteSessionStore.
 *
 * @author OpenClaw Team
 * @version 2026.3.20
 */
class SQLiteSessionStoreTest {

    @TempDir
    Path tempDir;

    private SQLiteSessionStore store;

    @BeforeEach
    void setUp() {
        String dbPath = tempDir.resolve("test.db").toString();
        DataSource dataSource = createDataSource(dbPath);
        SessionConfig config = new SessionConfig();
        store = new SQLiteSessionStore(dataSource, config);
        store.initialize().join();
    }

    private DataSource createDataSource(String dbPath) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + dbPath);
        return dataSource;
    }

    @Test
    void testSaveAndFindSession() {
        // Create session
        Session session = new Session("test-key-1", "gpt-4");
        session.setStatus(SessionStatus.ACTIVE);
        
        // Save
        store.saveSession(session).join();
        
        // Find by ID
        Optional<Session> found = store.findSessionById(session.getId()).join();
        assertTrue(found.isPresent());
        assertEquals("test-key-1", found.get().getSessionKey());
        assertEquals("gpt-4", found.get().getModel());
        assertEquals(SessionStatus.ACTIVE, found.get().getStatus());
        
        // Find by key
        Optional<Session> foundByKey = store.findSessionByKey("test-key-1").join();
        assertTrue(foundByKey.isPresent());
        assertEquals(session.getId(), foundByKey.get().getId());
    }

    @Test
    void testFindAllSessions() {
        // Create multiple sessions
        Session session1 = new Session("key-1", "gpt-4");
        Session session2 = new Session("key-2", "gpt-3.5");
        
        store.saveSession(session1).join();
        store.saveSession(session2).join();
        
        List<Session> sessions = store.findAllSessions().join();
        assertEquals(2, sessions.size());
    }

    @Test
    void testFindSessionsByStatus() {
        Session activeSession = new Session("active-key", "gpt-4");
        activeSession.setStatus(SessionStatus.ACTIVE);
        
        Session completedSession = new Session("completed-key", "gpt-4");
        completedSession.setStatus(SessionStatus.COMPLETED);
        
        store.saveSession(activeSession).join();
        store.saveSession(completedSession).join();
        
        List<Session> activeSessions = store.findSessionsByStatus(SessionStatus.ACTIVE).join();
        assertEquals(1, activeSessions.size());
        assertEquals("active-key", activeSessions.get(0).getSessionKey());
    }

    @Test
    void testFindActiveSessions() {
        Session activeSession = new Session("active-key", "gpt-4");
        activeSession.setStatus(SessionStatus.ACTIVE);
        
        Session pausedSession = new Session("paused-key", "gpt-4");
        pausedSession.setStatus(SessionStatus.PAUSED);
        
        Session completedSession = new Session("completed-key", "gpt-4");
        completedSession.setStatus(SessionStatus.COMPLETED);
        
        store.saveSession(activeSession).join();
        store.saveSession(pausedSession).join();
        store.saveSession(completedSession).join();
        
        List<Session> activeSessions = store.findActiveSessions().join();
        assertEquals(2, activeSessions.size());
    }

    @Test
    void testUpdateSessionStatus() {
        Session session = new Session("test-key", "gpt-4");
        session.setStatus(SessionStatus.PENDING);
        store.saveSession(session).join();
        
        store.updateSessionStatus(session.getId(), SessionStatus.ACTIVE).join();
        
        Optional<Session> updated = store.findSessionById(session.getId()).join();
        assertTrue(updated.isPresent());
        assertEquals(SessionStatus.ACTIVE, updated.get().getStatus());
    }

    @Test
    void testDeleteSession() {
        Session session = new Session("test-key", "gpt-4");
        store.saveSession(session).join();
        
        // Add a message
        Message message = new Message("user", "Hello");
        message.setSessionId(session.getId());
        store.saveMessage(message).join();
        
        // Delete session
        Boolean deleted = store.deleteSession(session.getId()).join();
        assertTrue(deleted);
        
        // Verify deleted
        Optional<Session> found = store.findSessionById(session.getId()).join();
        assertFalse(found.isPresent());
        
        // Verify messages also deleted (cascade)
        List<Message> messages = store.findMessagesBySession(session.getId()).join();
        assertTrue(messages.isEmpty());
    }

    @Test
    void testSaveAndFindMessage() {
        // Create session
        Session session = new Session("test-key", "gpt-4");
        store.saveSession(session).join();
        
        // Create message
        Message message = new Message("user", "Hello, world!");
        message.setSessionId(session.getId());
        message.setTokenCount(10);
        
        store.saveMessage(message).join();
        
        // Find messages
        List<Message> messages = store.findMessagesBySession(session.getId()).join();
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals("Hello, world!", messages.get(0).getContent());
        assertEquals(10, messages.get(0).getTokenCount());
    }

    @Test
    void testFindMessagesWithLimit() {
        Session session = new Session("test-key", "gpt-4");
        store.saveSession(session).join();
        
        // Add multiple messages
        for (int i = 0; i < 5; i++) {
            Message message = new Message("user", "Message " + i);
            message.setSessionId(session.getId());
            store.saveMessage(message).join();
        }
        
        // Find with limit
        List<Message> messages = store.findMessagesBySession(session.getId(), 3).join();
        assertEquals(3, messages.size());
    }

    @Test
    void testSearchSessions() {
        Session session1 = new Session("chat-123", "gpt-4");
        Session session2 = new Session("chat-456", "gpt-4");
        Session session3 = new Session("other-789", "gpt-4");
        
        store.saveSession(session1).join();
        store.saveSession(session2).join();
        store.saveSession(session3).join();
        
        List<Session> results = store.searchSessions("chat").join();
        assertEquals(2, results.size());
    }

    @Test
    void testSearchMessages() {
        Session session = new Session("test-key", "gpt-4");
        store.saveSession(session).join();
        
        Message msg1 = new Message("user", "Hello world");
        msg1.setSessionId(session.getId());
        
        Message msg2 = new Message("assistant", "Hi there");
        msg2.setSessionId(session.getId());
        
        Message msg3 = new Message("user", "Goodbye");
        msg3.setSessionId(session.getId());
        
        store.saveMessage(msg1).join();
        store.saveMessage(msg2).join();
        store.saveMessage(msg3).join();
        
        List<Message> results = store.searchMessages(session.getId(), "Hello").join();
        assertEquals(1, results.size());
        assertEquals("Hello world", results.get(0).getContent());
    }

    @Test
    void testGetSessionStats() {
        Session session = new Session("test-key", "gpt-4");
        store.saveSession(session).join();
        
        // Add messages
        Message msg1 = new Message("user", "Hello");
        msg1.setSessionId(session.getId());
        msg1.setTokenCount(5);
        
        Message msg2 = new Message("assistant", "Hi!");
        msg2.setSessionId(session.getId());
        msg2.setTokenCount(3);
        
        store.saveMessage(msg1).join();
        store.saveMessage(msg2).join();
        
        SessionStats stats = store.getSessionStats(session.getId()).join();
        assertEquals(2, stats.totalMessages());
        assertEquals(8, stats.totalTokens());
        assertNotNull(stats.firstMessageAt());
        assertNotNull(stats.lastMessageAt());
    }

    @Test
    void testFindRecentSessions() {
        Session session1 = new Session("key-1", "gpt-4");
        session1.setLastActivityAt(Instant.now().minusSeconds(3600));
        
        Session session2 = new Session("key-2", "gpt-4");
        session2.setLastActivityAt(Instant.now());
        
        Session session3 = new Session("key-3", "gpt-4");
        session3.setLastActivityAt(Instant.now().minusSeconds(7200));
        
        store.saveSession(session1).join();
        store.saveSession(session2).join();
        store.saveSession(session3).join();
        
        List<Session> recent = store.findRecentSessions(2).join();
        assertEquals(2, recent.size());
        assertEquals("key-2", recent.get(0).getSessionKey()); // Most recent
    }

    @Test
    void testFindSessionsByDateRange() {
        Instant now = Instant.now();
        
        Session oldSession = new Session("old-key", "gpt-4");
        oldSession.setCreatedAt(now.minusSeconds(86400 * 2)); // 2 days ago
        
        Session recentSession = new Session("recent-key", "gpt-4");
        recentSession.setCreatedAt(now.minusSeconds(3600)); // 1 hour ago
        
        store.saveSession(oldSession).join();
        store.saveSession(recentSession).join();
        
        List<Session> results = store.findSessionsByDateRange(now.minusSeconds(86400), now).join();
        assertEquals(1, results.size());
        assertEquals("recent-key", results.get(0).getSessionKey());
    }
}