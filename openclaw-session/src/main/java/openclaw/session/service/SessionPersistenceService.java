package openclaw.session.service;

import openclaw.session.model.Message;
import openclaw.session.model.Session;
import openclaw.session.model.SessionStatus;
import openclaw.session.store.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session persistence service with memory cache and SQLite storage.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
@Service
public class SessionPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionPersistenceService.class);
    
    private final SessionStore store;
    private final Map<String, Session> sessionCache;
    
    public SessionPersistenceService(SessionStore store) {
        this.store = store;
        this.sessionCache = new ConcurrentHashMap<>();
    }
    
    @PostConstruct
    public void initialize() {
        store.initialize().join();
        loadActiveSessions();
        logger.info("Session persistence service initialized");
    }
    
    @PreDestroy
    public void shutdown() {
        // Save all cached sessions
        sessionCache.values().forEach(session -> {
            try {
                store.saveSession(session).join();
            } catch (Exception e) {
                logger.error("Failed to save session on shutdown: {}", session.getId(), e);
            }
        });
        store.close().join();
        logger.info("Session persistence service shutdown");
    }
    
    /**
     * Create a new session.
     */
    public CompletableFuture<Session> createSession(String sessionKey, String model) {
        return CompletableFuture.supplyAsync(() -> {
            Session session = new Session(sessionKey, model);
            session.setStatus(SessionStatus.PENDING);
            
            // Save to database
            store.saveSession(session).join();
            
            // Cache in memory
            sessionCache.put(session.getId(), session);
            
            logger.info("Created session: {} (key: {})", session.getId(), sessionKey);
            return session;
        });
    }
    
    /**
     * Get session by ID.
     */
    public CompletableFuture<Optional<Session>> getSession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            Session cached = sessionCache.get(sessionId);
            if (cached != null) {
                return Optional.of(cached);
            }
            
            // Load from database
            Optional<Session> session = store.findSessionById(sessionId).join();
            session.ifPresent(s -> sessionCache.put(sessionId, s));
            
            return session;
        });
    }
    
    /**
     * Get session by key.
     */
    public CompletableFuture<Optional<Session>> getSessionByKey(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache
            Optional<Session> cached = sessionCache.values().stream()
                .filter(s -> s.getSessionKey().equals(sessionKey))
                .findFirst();
            if (cached.isPresent()) {
                return cached;
            }
            
            // Load from database
            return store.findSessionByKey(sessionKey).join();
        });
    }
    
    /**
     * Update session status.
     */
    public CompletableFuture<Void> updateStatus(String sessionId, SessionStatus status) {
        return CompletableFuture.runAsync(() -> {
            Session session = sessionCache.get(sessionId);
            if (session != null) {
                session.setStatus(status);
                store.saveSession(session).join();
            } else {
                store.updateSessionStatus(sessionId, status).join();
            }
            logger.debug("Updated session {} status to {}", sessionId, status);
        });
    }
    
    /**
     * Add message to session.
     */
    public CompletableFuture<Message> addMessage(String sessionId, String role, String content) {
        return CompletableFuture.supplyAsync(() -> {
            Message message = new Message(role, content);
            message.setSessionId(sessionId);
            
            // Save message
            store.saveMessage(message).join();
            
            // Update session
            Session session = sessionCache.get(sessionId);
            if (session != null) {
                session.addMessage(message);
                store.updateSessionActivity(sessionId).join();
            }
            
            logger.debug("Added message to session {}: {}", sessionId, role);
            return message;
        });
    }
    
    /**
     * Get session messages.
     */
    public CompletableFuture<List<Message>> getMessages(String sessionId) {
        return store.findMessagesBySession(sessionId);
    }
    
    /**
     * Get session messages with limit.
     */
    public CompletableFuture<List<Message>> getMessages(String sessionId, int limit) {
        return store.findMessagesBySession(sessionId, limit);
    }
    
    /**
     * Archive session.
     */
    public CompletableFuture<Void> archiveSession(String sessionId) {
        return updateStatus(sessionId, SessionStatus.ARCHIVED)
            .thenRun(() -> sessionCache.remove(sessionId));
    }
    
    /**
     * Delete session.
     */
    public CompletableFuture<Boolean> deleteSession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            sessionCache.remove(sessionId);
            return store.deleteSession(sessionId).join();
        });
    }
    
    /**
     * Search sessions.
     */
    public CompletableFuture<List<Session>> searchSessions(String keyword) {
        return store.searchSessions(keyword);
    }
    
    /**
     * Get recent sessions.
     */
    public CompletableFuture<List<Session>> getRecentSessions(int limit) {
        return store.findRecentSessions(limit);
    }
    
    /**
     * Get session statistics.
     */
    public CompletableFuture<SessionStore.SessionStats> getStats(String sessionId) {
        return store.getSessionStats(sessionId);
    }
    
    /**
     * Update token counts.
     */
    public CompletableFuture<Void> updateTokenCounts(String sessionId, int inputTokens, int outputTokens) {
        return CompletableFuture.runAsync(() -> {
            Session session = sessionCache.get(sessionId);
            if (session != null) {
                session.addInputTokens(inputTokens);
                session.addOutputTokens(outputTokens);
            }
        });
    }
    
    private void loadActiveSessions() {
        try {
            List<Session> activeSessions = store.findActiveSessions().join();
            activeSessions.forEach(s -> sessionCache.put(s.getId(), s));
            logger.info("Loaded {} active sessions from storage", activeSessions.size());
        } catch (Exception e) {
            logger.error("Failed to load active sessions", e);
        }
    }
}
