package openclaw.agent.store;

import openclaw.agent.AcpProtocol.AgentMessage;
import openclaw.agent.AcpProtocol.AgentSession;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Session store interface for agent session persistence.
 *
 * <p>Supports multiple implementations: memory, file, redis.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
public interface SessionStore {

    /**
     * Store type.
     */
    enum StoreType {
        MEMORY,
        FILE,
        REDIS
    }

    /**
     * Initialize the store.
     *
     * @return completion future
     */
    CompletableFuture<Void> initialize();

    /**
     * Shutdown the store.
     *
     * @return completion future
     */
    CompletableFuture<Void> shutdown();

    /**
     * Save a session.
     *
     * @param sessionKey the session key
     * @param session the session
     * @return completion future
     */
    CompletableFuture<Void> saveSession(String sessionKey, AgentSession session);

    /**
     * Get a session.
     *
     * @param sessionKey the session key
     * @return the session if found
     */
    CompletableFuture<Optional<AgentSession>> getSession(String sessionKey);

    /**
     * Delete a session.
     *
     * @param sessionKey the session key
     * @return completion future
     */
    CompletableFuture<Void> deleteSession(String sessionKey);

    /**
     * Check if session exists.
     *
     * @param sessionKey the session key
     * @return true if exists
     */
    CompletableFuture<Boolean> exists(String sessionKey);

    /**
     * Append a message to session.
     *
     * @param sessionKey the session key
     * @param message the message
     * @return completion future
     */
    CompletableFuture<Void> appendMessage(String sessionKey, AgentMessage message);

    /**
     * Get messages for a session.
     *
     * @param sessionKey the session key
     * @param limit the maximum number of messages
     * @return the messages
     */
    CompletableFuture<List<AgentMessage>> getMessages(String sessionKey, int limit);

    /**
     * Get all session keys.
     *
     * @return the session keys
     */
    CompletableFuture<List<String>> listSessionKeys();

    /**
     * Get store type.
     *
     * @return the store type
     */
    StoreType getStoreType();

    /**
     * Get store health status.
     *
     * @return true if healthy
     */
    default CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.completedFuture(true);
    }
}
