package openclaw.server.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced Rate Limiter
 * 
 * <p>Multi-dimensional rate limiting:</p>
 * <ul>
 *   <li>Per user</li>
 *   <li>Per channel</li>
 *   <li>Per model</li>
 *   <li>Per endpoint</li>
 *   <li>Dynamic adjustment</li>
 * </ul>
 */
@Service
public class AdvancedRateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedRateLimiter.class);
    
    private final RateLimiterRegistry registry;
    private final Map<String, RateLimiter> userLimiters;
    private final Map<String, RateLimiter> channelLimiters;
    private final Map<String, RateLimiter> modelLimiters;
    private final Map<String, RateLimiter> endpointLimiters;
    
    // Metrics
    private final Map<String, AtomicLong> requestCounts;
    private final Map<String, AtomicLong> blockedCounts;
    
    // Default configurations
    private static final RateLimiterConfig DEFAULT_USER_CONFIG = RateLimiterConfig.custom()
        .limitForPeriod(100)        // 100 requests
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ofMillis(100))
        .build();
    
    private static final RateLimiterConfig DEFAULT_CHANNEL_CONFIG = RateLimiterConfig.custom()
        .limitForPeriod(1000)       // 1000 requests per channel
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ofMillis(100))
        .build();
    
    private static final RateLimiterConfig DEFAULT_MODEL_CONFIG = RateLimiterConfig.custom()
        .limitForPeriod(60)         // 60 requests per model per minute
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ofMillis(500))
        .build();
    
    private static final RateLimiterConfig DEFAULT_ENDPOINT_CONFIG = RateLimiterConfig.custom()
        .limitForPeriod(10000)      // 10000 requests per endpoint
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ofMillis(50))
        .build();
    
    public AdvancedRateLimiter() {
        this.registry = RateLimiterRegistry.ofDefaults();
        this.userLimiters = new ConcurrentHashMap<>();
        this.channelLimiters = new ConcurrentHashMap<>();
        this.modelLimiters = new ConcurrentHashMap<>();
        this.endpointLimiters = new ConcurrentHashMap<>();
        this.requestCounts = new ConcurrentHashMap<>();
        this.blockedCounts = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if user request is allowed
     * 
     * @param userId user identifier
     * @return true if allowed
     */
    public boolean isUserAllowed(String userId) {
        RateLimiter limiter = userLimiters.computeIfAbsent(userId, 
            id -> registry.rateLimiter("user-" + id, DEFAULT_USER_CONFIG));
        
        boolean allowed = limiter.acquirePermission();
        
        if (allowed) {
            incrementCounter("user-" + userId + "-requests");
        } else {
            incrementCounter("user-" + userId + "-blocked");
            logger.warn("Rate limit exceeded for user: {}", userId);
        }
        
        return allowed;
    }
    
    /**
     * Check if channel request is allowed
     * 
     * @param channelId channel identifier
     * @return true if allowed
     */
    public boolean isChannelAllowed(String channelId) {
        RateLimiter limiter = channelLimiters.computeIfAbsent(channelId,
            id -> registry.rateLimiter("channel-" + id, DEFAULT_CHANNEL_CONFIG));
        
        boolean allowed = limiter.acquirePermission();
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for channel: {}", channelId);
        }
        
        return allowed;
    }
    
    /**
     * Check if model request is allowed
     * 
     * @param model model name (e.g., "gpt-4")
     * @return true if allowed
     */
    public boolean isModelAllowed(String model) {
        RateLimiter limiter = modelLimiters.computeIfAbsent(model,
            m -> registry.rateLimiter("model-" + m, DEFAULT_MODEL_CONFIG));
        
        boolean allowed = limiter.acquirePermission();
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for model: {}", model);
        }
        
        return allowed;
    }
    
    /**
     * Check if endpoint request is allowed
     * 
     * @param endpoint endpoint path
     * @return true if allowed
     */
    public boolean isEndpointAllowed(String endpoint) {
        RateLimiter limiter = endpointLimiters.computeIfAbsent(endpoint,
            e -> registry.rateLimiter("endpoint-" + e, DEFAULT_ENDPOINT_CONFIG));
        
        boolean allowed = limiter.acquirePermission();
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for endpoint: {}", endpoint);
        }
        
        return allowed;
    }
    
    /**
     * Check all dimensions
     * 
     * @param userId user identifier
     * @param channelId channel identifier
     * @param model model name
     * @param endpoint endpoint path
     * @return true if all allowed
     */
    public boolean checkAll(String userId, String channelId, String model, String endpoint) {
        return isUserAllowed(userId) &&
               isChannelAllowed(channelId) &&
               isModelAllowed(model) &&
               isEndpointAllowed(endpoint);
    }
    
    /**
     * Update user rate limit dynamically
     * 
     * @param userId user identifier
     * @param limit new limit
     * @param period refresh period
     */
    public void updateUserLimit(String userId, int limit, Duration period) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(limit)
            .limitRefreshPeriod(period)
            .timeoutDuration(Duration.ofMillis(100))
            .build();
        
        RateLimiter limiter = registry.rateLimiter("user-" + userId, config);
        userLimiters.put(userId, limiter);
        
        logger.info("Updated rate limit for user {}: {}/{}s", 
            userId, limit, period.getSeconds());
    }
    
    /**
     * Update model rate limit dynamically
     * 
     * @param model model name
     * @param limit new limit
     * @param period refresh period
     */
    public void updateModelLimit(String model, int limit, Duration period) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(limit)
            .limitRefreshPeriod(period)
            .timeoutDuration(Duration.ofMillis(500))
            .build();
        
        RateLimiter limiter = registry.rateLimiter("model-" + model, config);
        modelLimiters.put(model, limiter);
        
        logger.info("Updated rate limit for model {}: {}/{}s",
            model, limit, period.getSeconds());
    }
    
    /**
     * Get rate limit metrics
     */
    public RateLimitMetrics getMetrics(String dimension, String id) {
        RateLimiter limiter = switch (dimension) {
            case "user" -> userLimiters.get(id);
            case "channel" -> channelLimiters.get(id);
            case "model" -> modelLimiters.get(id);
            case "endpoint" -> endpointLimiters.get(id);
            default -> null;
        };
        
        if (limiter == null) {
            return null;
        }
        
        return new RateLimitMetrics(
            id,
            dimension,
            limiter.getMetrics().getAvailablePermissions(),
            limiter.getMetrics().getNumberOfWaitingThreads(),
            requestCounts.getOrDefault(dimension + "-" + id + "-requests", new AtomicLong(0)).get(),
            blockedCounts.getOrDefault(dimension + "-" + id + "-blocked", new AtomicLong(0)).get()
        );
    }
    
    /**
     * Reset rate limiter
     */
    public void reset(String dimension, String id) {
        RateLimiter limiter = switch (dimension) {
            case "user" -> userLimiters.get(id);
            case "channel" -> channelLimiters.get(id);
            case "model" -> modelLimiters.get(id);
            case "endpoint" -> endpointLimiters.get(id);
            default -> null;
        };
        
        if (limiter != null) {
            limiter.changeLimitForPeriod(limiter.getRateLimiterConfig().getLimitForPeriod());
            logger.info("Reset rate limiter for {}:{}", dimension, id);
        }
    }
    
    private void incrementCounter(String key) {
        requestCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Rate limit metrics
     */
    public record RateLimitMetrics(
        String id,
        String dimension,
        int availablePermissions,
        int waitingThreads,
        long totalRequests,
        long blockedRequests
    ) {
        public double getBlockedRate() {
            if (totalRequests == 0) return 0.0;
            return (double) blockedRequests / totalRequests;
        }
    }
}
