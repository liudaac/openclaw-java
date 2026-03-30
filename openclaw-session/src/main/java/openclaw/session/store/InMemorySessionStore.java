package openclaw.session.store;

import openclaw.session.model.Message;
import openclaw.session.model.Session;
import openclaw.session.model.SessionStatus;
import openclaw.session.config.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of SessionStore.
 *
 * <p>Non-persistent storage suitable for development and testing.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.20
 */
@Service
@ConditionalOnProperty(prefix = "openclaw.session", name = "storage-type", havingValue = "memory")
public class InMemorySessionStore implements SessionStore {

    private static final Logger logger = LoggerFactory.getLogger(InMemorySessionStore.class);

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<Message>> messages = new ConcurrentHashMap<>();
    private final SessionConfig config;

    public InMemorySessionStore(SessionConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Initializing in-memory session store");
        });
    }

    @Override
    public CompletableFuture<Void> saveSession(Session session) {
        return CompletableFuture.runAsync(() -> {
            sessions.put(session.getId(), session);
            logger.debug("Saved session: {}", session.getId());
        });
    }

    @Override
    public CompletableFuture<Optional<Session>> findSessionById(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            return Optional.ofNullable(sessions.get(sessionId));
        });
    }

    @Override
    public CompletableFuture<Optional<Session>> findSessionByKey(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            return sessions.values().stream()
                .filter(s -> s.getSessionKey().equals(sessionKey))
                .findFirst();
        });
    }

    @Override
    public CompletableFuture<List<Session>> findAllSessions() {
        return CompletableFuture.supplyAsync(() -> {
            return new ArrayList<>(sessions.values());
        });
    }

    @Override
    public CompletableFuture<List<Session>> findSessionsByStatus(SessionStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            return sessions.values().stream()
                .filter(s -> s.getStatus() == status)
                .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<List<Session>> findActiveSessions() {
        return CompletableFuture.supplyAsync(() -> {
            return sessions.values().stream()
                .filter(s -> s.getStatus().isActive())
                .sorted(Comparator.comparing(Session::getLastActivityAt).reversed())
                .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteSession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            sessions.remove(sessionId);
            messages.remove(sessionId);
            logger.debug("Deleted session: {}", sessionId);
            return true;
        });
    }

    @Override
    public CompletableFuture<Integer> deleteSessions(List<String> sessionIds) {
        return CompletableFuture.supplyAsync(() -> {
            int count = 0;
            for (String sessionId : sessionIds) {
                sessions.remove(sessionId);
                messages.remove(sessionId);
                count++;
            }
            logger.debug("Deleted {} sessions", count);
            return count;
        });
    }

    @Override
    public CompletableFuture<Void> updateSessionStatus(String sessionId, SessionStatus status) {
        return CompletableFuture.runAsync(() -> {
            Session session = sessions.get(sessionId);
            if (session != null) {
                session.setStatus(status);
                session.setUpdatedAt(Instant.now());
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateSessionActivity(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            Session session = sessions.get(sessionId);
            if (session != null) {
                session.setLastActivityAt(Instant.now());
                session.setUpdatedAt(Instant.now());
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveMessage(Message message) {
        return CompletableFuture.runAsync(() -> {
            messages.computeIfAbsent(message.getSessionId(), k -> new ArrayList<>()).add(message);
            
            // Trim if exceeds max messages
            List<Message> sessionMessages = messages.get(message.getSessionId());
            if (sessionMessages.size() > config.getMaxMessages()) {
                messages.put(message.getSessionId(), 
                    sessionMessages.subList(sessionMessages.size() - config.getMaxMessages(), sessionMessages.size()));
            }
            
            logger.debug("Saved message: {} for session: {}", message.getId(), message.getSessionId());
        });
    }

    @Override
    public CompletableFuture<List<Message>> findMessagesBySession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            return new ArrayList<>(messages.getOrDefault(sessionId, new ArrayList<>()));
        });
    }

    @Override
    public CompletableFuture<List<Message>> findMessagesBySession(String sessionId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Message> sessionMessages = messages.getOrDefault(sessionId, new ArrayList<>());
            int start = Math.max(0, sessionMessages.size() - limit);
            return new ArrayList<>(sessionMessages.subList(start, sessionMessages.size()));
        });
    }

    @Override
    public CompletableFuture<Optional<Message>> findMessageById(String messageId) {
        return CompletableFuture.supplyAsync(() -> {
            return messages.values().stream()
                .flatMap(List::stream)
                .filter(m -> m.getId().equals(messageId))
                .findFirst();
        });
    }

    @Override
    public CompletableFuture<Void> deleteMessagesBySession(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            messages.remove(sessionId);
        });
    }

    @Override
    public CompletableFuture<List<Session>> searchSessions(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            return sessions.values().stream()
                .filter(s -> s.getSessionKey().contains(keyword))
                .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<List<Message>> searchMessages(String sessionId, String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            return messages.getOrDefault(sessionId, new ArrayList<>()).stream()
                .filter(m -> m.getContent() != null && m.getContent().contains(keyword))
                .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<SessionStats> getSessionStats(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Message> sessionMessages = messages.getOrDefault(sessionId, new ArrayList<>());
            int totalTokens = sessionMessages.stream().mapToInt(Message::getTokenCount).sum();
            
            Instant firstMessageAt = sessionMessages.stream()
                .min(Comparator.comparing(Message::getCreatedAt))
                .map(Message::getCreatedAt)
                .orElse(null);
            
            Instant lastMessageAt = sessionMessages.stream()
                .max(Comparator.comparing(Message::getCreatedAt))
                .map(Message::getCreatedAt)
                .orElse(null);
            
            return new SessionStats(sessionMessages.size(), totalTokens, firstMessageAt, lastMessageAt);
        });
    }

    @Override
    public CompletableFuture<List<Session>> findRecentSessions(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            return sessions.values().stream()
                .sorted(Comparator.comparing(Session::getLastActivityAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<List<Session>> findSessionsByDateRange(Instant start, Instant end) {
        return CompletableFuture.supplyAsync(() -> {
            return sessions.values().stream()
                .filter(s -> !s.getCreatedAt().isBefore(start) && !s.getCreatedAt().isAfter(end))
                .sorted(Comparator.comparing(Session::getCreatedAt).reversed())
                .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            sessions.clear();
            messages.clear();
            logger.info("In-memory session store closed");
        });
    }
}