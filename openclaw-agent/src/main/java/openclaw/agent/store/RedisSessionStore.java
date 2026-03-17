package openclaw.agent.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openclaw.agent.AcpProtocol.AgentMessage;
import openclaw.agent.AcpProtocol.AgentSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Redis-based session store implementation.
 *
 * <p>High-performance distributed storage. Suitable for production deployments.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
public class RedisSessionStore implements SessionStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisSessionStore.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final StoreConfig.RedisConfig redisConfig;
    private final StoreConfig.SessionConfig sessionConfig;

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String MESSAGES_KEY_PREFIX = "messages:";
    private static final String SESSION_INDEX_KEY = "sessions:index";

    public RedisSessionStore(StringRedisTemplate redisTemplate, 
                             StoreConfig.RedisConfig redisConfig,
                             StoreConfig.SessionConfig sessionConfig) {
        this.redisTemplate = redisTemplate;
        this.redisConfig = redisConfig;
        this.sessionConfig = sessionConfig;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Initializing Redis session store at {}:{}", 
                redisConfig.getHost(), redisConfig.getPort());
            
            // Test connection
            try {
                redisTemplate.getConnectionFactory().getConnection().ping();
                logger.info("Redis connection established");
            } catch (Exception e) {
                throw new RuntimeException("Failed to connect to Redis", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Redis session store shutdown");
        });
    }

    @Override
    public CompletableFuture<Void> saveSession(String sessionKey, AgentSession session) {
        return CompletableFuture.runAsync(() -> {
            try {
                String key = getSessionKey(sessionKey);
                String json = objectMapper.writeValueAsString(session);
                
                redisTemplate.opsForValue().set(key, json, sessionConfig.getTtl());
                
                // Add to index
                redisTemplate.opsForSet().add(SESSION_INDEX_KEY, sessionKey);
                redisTemplate.expire(SESSION_INDEX_KEY, sessionConfig.getTtl());
                
                logger.debug("Saved session: {}", sessionKey);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize session: " + sessionKey, e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<AgentSession>> getSession(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = getSessionKey(sessionKey);
                String json = redisTemplate.opsForValue().get(key);
                
                if (json == null) {
                    return Optional.empty();
                }
                
                AgentSession session = objectMapper.readValue(json, AgentSession.class);
                
                // Refresh TTL
                redisTemplate.expire(key, sessionConfig.getTtl());
                
                return Optional.of(session);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to deserialize session: {}", sessionKey, e);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteSession(String sessionKey) {
        return CompletableFuture.runAsync(() -> {
            String sessionKeyFull = getSessionKey(sessionKey);
            String messagesKeyFull = getMessagesKey(sessionKey);
            
            redisTemplate.delete(sessionKeyFull);
            redisTemplate.delete(messagesKeyFull);
            redisTemplate.opsForSet().remove(SESSION_INDEX_KEY, sessionKey);
            
            logger.debug("Deleted session: {}", sessionKey);
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            String key = getSessionKey(sessionKey);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        });
    }

    @Override
    public CompletableFuture<Void> appendMessage(String sessionKey, AgentMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                String key = getMessagesKey(sessionKey);
                String json = objectMapper.writeValueAsString(message);
                
                // Push to list
                redisTemplate.opsForList().rightPush(key, json);
                
                // Trim to max messages
                int maxMessages = sessionConfig.getMaxMessages();
                redisTemplate.opsForList().trim(key, -maxMessages, -1);
                
                // Set TTL
                redisTemplate.expire(key, sessionConfig.getTtl());
                
                logger.debug("Appended message to session: {}", sessionKey);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize message", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<AgentMessage>> getMessages(String sessionKey, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = getMessagesKey(sessionKey);
                
                // Get last N messages
                List<String> jsonList = redisTemplate.opsForList().range(key, -limit, -1);
                
                if (jsonList == null || jsonList.isEmpty()) {
                    return List.of();
                }
                
                List<AgentMessage> messages = new ArrayList<>();
                for (String json : jsonList) {
                    try {
                        AgentMessage message = objectMapper.readValue(json, AgentMessage.class);
                        messages.add(message);
                    } catch (JsonProcessingException e) {
                        logger.warn("Failed to deserialize message", e);
                    }
                }
                
                // Refresh TTL
                redisTemplate.expire(key, sessionConfig.getTtl());
                
                return messages;
            } catch (Exception e) {
                logger.warn("Failed to get messages: {}", sessionKey, e);
                return List.of();
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> listSessionKeys() {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> keys = redisTemplate.opsForSet().members(SESSION_INDEX_KEY);
            if (keys == null) {
                return List.of();
            }
            return new ArrayList<>(keys);
        });
    }

    @Override
    public StoreType getStoreType() {
        return StoreType.REDIS;
    }

    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                redisTemplate.getConnectionFactory().getConnection().ping();
                return true;
            } catch (Exception e) {
                logger.warn("Redis health check failed", e);
                return false;
            }
        });
    }

    // Private methods

    private String getSessionKey(String sessionKey) {
        return redisConfig.getKeyPrefix() + SESSION_KEY_PREFIX + sessionKey;
    }

    private String getMessagesKey(String sessionKey) {
        return redisConfig.getKeyPrefix() + MESSAGES_KEY_PREFIX + sessionKey;
    }

    @Override
    public String toString() {
        return String.format("RedisSessionStore{host=%s:%d, database=%d}",
            redisConfig.getHost(), redisConfig.getPort(), redisConfig.getDatabase());
    }
}
