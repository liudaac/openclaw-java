package openclaw.server.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Cache Configuration - Phase 4
 *
 * <p>Configures Caffeine caching for improved performance.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineConfig());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats();
    }

    /**
     * Cache names
     */
    public static class CacheNames {
        public static final String AGENT_SESSIONS = "agentSessions";
        public static final String TOOL_RESULTS = "toolResults";
        public static final String CHANNEL_INFO = "channelInfo";
        public static final String LLM_RESPONSES = "llmResponses";
    }
}
