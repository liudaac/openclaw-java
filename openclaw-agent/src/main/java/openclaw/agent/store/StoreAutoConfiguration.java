package openclaw.agent.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Auto-configuration for session store.
 *
 * <p>Automatically configures the appropriate store based on 'openclaw.store.type' property.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
@Configuration
public class StoreAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(StoreAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public StoreConfig storeConfig() {
        return new StoreConfig();
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionStore sessionStore(StoreConfig config, 
                                     @Autowired(required = false) StringRedisTemplate redisTemplate) {
        SessionStoreFactory factory = new SessionStoreFactory(config, redisTemplate);
        SessionStore store = factory.createStore();
        
        // Initialize store
        store.initialize().join();
        
        logger.info("Session store initialized: {}", store.getStoreType());
        return store;
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionStoreFactory sessionStoreFactory(StoreConfig config,
                                                   @Autowired(required = false) StringRedisTemplate redisTemplate) {
        return new SessionStoreFactory(config, redisTemplate);
    }

    /**
     * Store lifecycle manager.
     */
    @Configuration
    static class StoreLifecycleManager {

        private static final Logger logger = LoggerFactory.getLogger(StoreLifecycleManager.class);

        private final SessionStore sessionStore;

        StoreLifecycleManager(SessionStore sessionStore) {
            this.sessionStore = sessionStore;
        }

        @PostConstruct
        public void init() {
            logger.info("Session store type: {}", sessionStore.getStoreType());
        }

        @PreDestroy
        public void destroy() {
            logger.info("Shutting down session store");
            sessionStore.shutdown().join();
        }
    }
}
