package openclaw.session.store;

import openclaw.session.model.Message;
import openclaw.session.model.Session;
import openclaw.session.model.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for session persistence.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public interface SessionStore {
    
    CompletableFuture<Void> initialize();
    
    // Session operations
    CompletableFuture<Void> saveSession(Session session);
    CompletableFuture<Optional<Session>> findSessionById(String sessionId);
    CompletableFuture<Optional<Session>> findSessionByKey(String sessionKey);
    CompletableFuture<List<Session>> findAllSessions();
    CompletableFuture<List<Session>> findSessionsByStatus(SessionStatus status);
    CompletableFuture<List<Session>> findActiveSessions();
    CompletableFuture<Boolean> deleteSession(String sessionId);
    CompletableFuture<Integer> deleteSessions(List<String> sessionIds);
    CompletableFuture<Void> updateSessionStatus(String sessionId, SessionStatus status);
    CompletableFuture<Void> updateSessionActivity(String sessionId);
    
    // Message operations
    CompletableFuture<Void> saveMessage(Message message);
    CompletableFuture<List<Message>> findMessagesBySession(String sessionId);
    CompletableFuture<List<Message>> findMessagesBySession(String sessionId, int limit);
    CompletableFuture<Optional<Message>> findMessageById(String messageId);
    CompletableFuture<Void> deleteMessagesBySession(String sessionId);
    
    // Search
    CompletableFuture<List<Session>> searchSessions(String keyword);
    CompletableFuture<List<Message>> searchMessages(String sessionId, String keyword);
    
    // Statistics
    CompletableFuture<SessionStats> getSessionStats(String sessionId);
    CompletableFuture<List<Session>> findRecentSessions(int limit);
    CompletableFuture<List<Session>> findSessionsByDateRange(Instant start, Instant end);
    
    CompletableFuture<Void> close();
    
    record SessionStats(
        int totalMessages,
        int totalTokens,
        Instant firstMessageAt,
        Instant lastMessageAt
    ) {}
}
