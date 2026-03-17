package openclaw.agent.store;

import openclaw.agent.AcpProtocol.AgentMessage;
import openclaw.agent.AcpProtocol.AgentSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store implementation.
 *
 * <p>Simple and fast, but not persistent. Suitable for development and testing.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
public class MemorySessionStore implements SessionStore {

    private static final Logger logger = LoggerFactory.getLogger(MemorySessionStore.class);

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<AgentMessage>> messages = new ConcurrentHashMap<>();
    private final StoreConfig.SessionConfig config;

    public MemorySessionStore(StoreConfig.SessionConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        logger.info("Initializing memory session store");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        logger.info("Shutting down memory session store");
        sessions.clear();
        messages.clear();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> saveSession(String sessionKey, AgentSession session) {
        sessions.put(sessionKey, session);
        if (!messages.containsKey(sessionKey)) {
            messages.put(sessionKey, new ArrayList<>());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<AgentSession>> getSession(String sessionKey) {
        return CompletableFuture.completedFuture(Optional.ofNullable(sessions.get(sessionKey)));
    }

    @Override
    public CompletableFuture<Void> deleteSession(String sessionKey) {
        sessions.remove(sessionKey);
        messages.remove(sessionKey);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> exists(String sessionKey) {
        return CompletableFuture.completedFuture(sessions.containsKey(sessionKey));
    }

    @Override
    public CompletableFuture<Void> appendMessage(String sessionKey, AgentMessage message) {
        List<AgentMessage> sessionMessages = messages.computeIfAbsent(sessionKey, k -> new ArrayList<>());
        sessionMessages.add(message);
        
        // Trim to max messages
        int maxMessages = config.getMaxMessages();
        if (sessionMessages.size() > maxMessages) {
            sessionMessages.subList(0, sessionMessages.size() - maxMessages).clear();
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<AgentMessage>> getMessages(String sessionKey, int limit) {
        List<AgentMessage> sessionMessages = messages.getOrDefault(sessionKey, List.of());
        int start = Math.max(0, sessionMessages.size() - limit);
        List<AgentMessage> result = sessionMessages.subList(start, sessionMessages.size());
        return CompletableFuture.completedFuture(new ArrayList<>(result));
    }

    @Override
    public CompletableFuture<List<String>> listSessionKeys() {
        return CompletableFuture.completedFuture(new ArrayList<>(sessions.keySet()));
    }

    @Override
    public StoreType getStoreType() {
        return StoreType.MEMORY;
    }

    @Override
    public String toString() {
        return String.format("MemorySessionStore{sessions=%d, messages=%d}", 
            sessions.size(), messages.size());
    }
}
