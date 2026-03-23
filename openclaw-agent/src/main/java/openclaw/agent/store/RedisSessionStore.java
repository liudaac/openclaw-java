package openclaw.agent.store;

import openclaw.agent.AcpProtocol.AgentMessage;
import openclaw.agent.AcpProtocol.AgentSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Redis-based session store implementation.
 *
 * <p>High-performance distributed storage. Suitable for production deployments.</p>
 * <p><strong>Note:</strong> This implementation requires spring-data-redis dependency.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
public class RedisSessionStore implements SessionStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisSessionStore.class);

    private final StoreConfig.RedisConfig redisConfig;
    private final StoreConfig.SessionConfig sessionConfig;

    public RedisSessionStore(Object redisTemplate,
                             StoreConfig.RedisConfig redisConfig,
                             StoreConfig.SessionConfig sessionConfig) {
        this.redisConfig = redisConfig;
        this.sessionConfig = sessionConfig;
        throw new IllegalStateException(
            "RedisSessionStore requires spring-data-redis dependency. " +
            "Please add the dependency to use Redis store type.");
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.failedFuture(new UnsupportedOperationException(
            "RedisSessionStore requires spring-data-redis dependency"));
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> saveSession(String sessionKey, AgentSession session) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException(
            "RedisSessionStore requires spring-data-redis dependency"));
    }

    @Override
    public CompletableFuture<Optional<AgentSession>> getSession(String sessionKey) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException(
            "RedisSessionStore requires spring-data-redis dependency"));
    }

    @Override
    public CompletableFuture<Void> deleteSession(String sessionKey) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> exists(String sessionKey) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Void> appendMessage(String sessionKey, AgentMessage message) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException(
            "RedisSessionStore requires spring-data-redis dependency"));
    }

    @Override
    public CompletableFuture<List<AgentMessage>> getMessages(String sessionKey, int limit) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException(
            "RedisSessionStore requires spring-data-redis dependency"));
    }

    @Override
    public CompletableFuture<List<String>> listSessionKeys() {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public StoreType getStoreType() {
        return StoreType.REDIS;
    }
}
