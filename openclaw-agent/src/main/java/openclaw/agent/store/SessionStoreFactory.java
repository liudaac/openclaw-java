package openclaw.agent.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating session store instances.
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
public class SessionStoreFactory {

    private static final Logger logger = LoggerFactory.getLogger(SessionStoreFactory.class);

    private final StoreConfig config;
    private Object redisTemplate;

    public SessionStoreFactory(StoreConfig config) {
        this.config = config;
    }

    public SessionStoreFactory(StoreConfig config, Object redisTemplate) {
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
                throw new IllegalStateException(
                    "Redis store type requires spring-data-redis dependency. " +
                    "Please add the dependency or switch to MEMORY/FILE store type.");
                
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
                throw new IllegalStateException(
                    "Redis store type requires spring-data-redis dependency. " +
                    "Please add the dependency or switch to MEMORY/FILE store type.");
                
            default:
                throw new IllegalArgumentException("Unknown store type: " + type);
        }
    }
}
