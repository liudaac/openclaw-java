package openclaw.agent.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Factory for creating session store instances.
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
public class SessionStoreFactory {

    private static final Logger logger = LoggerFactory.getLogger(SessionStoreFactory.class);

    private final StoreConfig config;
    private StringRedisTemplate redisTemplate;

    public SessionStoreFactory(StoreConfig config) {
        this.config = config;
    }

    public SessionStoreFactory(StoreConfig config, StringRedisTemplate redisTemplate) {
        this.config = config;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create a session store based on configuration.
     *
     * @return the session store
     */
    public SessionStore createStore() {
        SessionStore.StoreType type = config.getType();
        
        logger.info("Creating session store of type: {}", type);
        
        switch (type) {
            case MEMORY:
                return new MemorySessionStore(config.getSession());
                
            case FILE:
                return new FileSessionStore(config.getFile(), config.getSession());
                
            case REDIS:
                if (redisTemplate == null) {
                    throw new IllegalStateException(
                        "RedisTemplate is required for REDIS store type. " +
                        "Please configure Redis or switch to MEMORY/FILE store type.");
                }
                return new RedisSessionStore(redisTemplate, config.getRedis(), config.getSession());
                
            default:
                throw new IllegalArgumentException("Unknown store type: " + type);
        }
    }

    /**
     * Create a specific store type.
     *
     * @param type the store type
     * @return the session store
     */
    public SessionStore createStore(SessionStore.StoreType type) {
        logger.info("Creating session store of type: {}", type);
        
        switch (type) {
            case MEMORY:
                return new MemorySessionStore(config.getSession());
                
            case FILE:
                return new FileSessionStore(config.getFile(), config.getSession());
                
            case REDIS:
                if (redisTemplate == null) {
                    throw new IllegalStateException("RedisTemplate is required for REDIS store type");
                }
                return new RedisSessionStore(redisTemplate, config.getRedis(), config.getSession());
                
            default:
                throw new IllegalArgumentException("Unknown store type: " + type);
        }
    }
}
