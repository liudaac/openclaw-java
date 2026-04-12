package openclaw.gateway.provider.attribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for resolving provider attribution policies and headers.
 *
 * <p>Following the TypeScript provider-attribution.ts pattern with
 * resolveProviderRequestPolicy and resolveProviderAttributionHeaders functions.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public class ProviderAttributionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProviderAttributionService.class);
    
    // Cache for provider policies
    private final Map<String, ProviderAttributionPolicy> policyCache;
    
    public ProviderAttributionService() {
        this.policyCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Resolves the attribution policy for a provider request.
     *
     * @param providerId the provider ID
     * @param requestType the type of request
     * @param config optional configuration
     * @return the resolved attribution policy
     */
    public ProviderAttributionPolicy resolveProviderRequestPolicy(
            String providerId,
            RequestType requestType,
            Optional<Map<String, Object>> config) {
        
        // Check cache first
        String cacheKey = buildCacheKey(providerId, requestType);
        ProviderAttributionPolicy cached = policyCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Resolve policy based on provider characteristics
        ProviderAttributionPolicy policy = resolvePolicy(providerId, requestType, config);
        
        // Cache the resolved policy
        policyCache.put(cacheKey, policy);
        
        return policy;
    }
    
    /**
     * Resolves attribution headers for a provider request.
     *
     * @param providerId the provider ID
     * @param policy the attribution policy
     * @param requestContext the request context
     * @return the resolved attribution headers
     */
    public Map<String, String> resolveProviderAttributionHeaders(
            String providerId,
            ProviderAttributionPolicy policy,
            RequestContext requestContext) {
        
        Map<String, String> headers = new ConcurrentHashMap<>();
        
        if (!policy.includeAttributionHeaders()) {
            return headers;
        }
        
        // Add standard attribution headers
        headers.put("X-OpenClaw-Provider", providerId);
        headers.put("X-OpenClaw-Version", getOpenClawVersion());
        
        // Add endpoint class header
        headers.put("X-OpenClaw-Endpoint-Class", policy.endpointClass().name().toLowerCase());
        
        // Add request context headers
        if (requestContext.sessionKey() != null) {
            headers.put("X-OpenClaw-Session", requestContext.sessionKey());
        }
        
        if (requestContext.agentId() != null) {
            headers.put("X-OpenClaw-Agent", requestContext.agentId());
        }
        
        // Add custom headers from policy
        headers.putAll(policy.customHeaders());
        
        return Map.copyOf(headers);
    }
    
    /**
     * Clears the policy cache.
     */
    public void clearCache() {
        policyCache.clear();
        logger.debug("Provider attribution policy cache cleared");
    }
    
    /**
     * Invalidates the cache for a specific provider.
     */
    public void invalidateCache(String providerId) {
        policyCache.entrySet().removeIf(entry -> entry.getKey().startsWith(providerId + ":"));
        logger.debug("Provider attribution policy cache invalidated for: {}", providerId);
    }
    
    // Private helper methods
    
    private ProviderAttributionPolicy resolvePolicy(
            String providerId,
            RequestType requestType,
            Optional<Map<String, Object>> config) {
        
        // Check if provider has explicit configuration
        if (config.isPresent()) {
            Map<String, Object> cfg = config.get();
            
            // Check for explicit endpoint class
            Object endpointClass = cfg.get("endpointClass");
            if (endpointClass instanceof String) {
                try {
                    ProviderAttributionPolicy.ProviderEndpointClass ec = 
                        ProviderAttributionPolicy.ProviderEndpointClass.valueOf(
                            ((String) endpointClass).toUpperCase()
                        );
                    
                    return switch (ec) {
                        case FIRST_PARTY -> ProviderAttributionPolicy.firstParty(providerId);
                        case THIRD_PARTY -> ProviderAttributionPolicy.thirdParty(providerId);
                        case ENTERPRISE -> createEnterprisePolicy(providerId, cfg);
                        case CUSTOM -> createCustomPolicy(providerId, cfg);
                        default -> ProviderAttributionPolicy.defaults(providerId);
                    };
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid endpoint class: {}", endpointClass);
                }
            }
        }
        
        // Infer policy from provider ID patterns
        if (isFirstPartyProvider(providerId)) {
            return ProviderAttributionPolicy.firstParty(providerId);
        }
        
        if (isThirdPartyProvider(providerId)) {
            return ProviderAttributionPolicy.thirdParty(providerId);
        }
        
        return ProviderAttributionPolicy.defaults(providerId);
    }
    
    private boolean isFirstPartyProvider(String providerId) {
        String lower = providerId.toLowerCase();
        return lower.contains("openai") || 
               lower.contains("anthropic") || 
               lower.contains("google") ||
               lower.contains("azure") ||
               lower.contains("aws");
    }
    
    private boolean isThirdPartyProvider(String providerId) {
        String lower = providerId.toLowerCase();
        return lower.contains("proxy") || 
               lower.contains("gateway") || 
               lower.contains("bridge");
    }
    
    private ProviderAttributionPolicy createEnterprisePolicy(
            String providerId, 
            Map<String, Object> config) {
        
        return new ProviderAttributionPolicy(
            providerId,
            ProviderAttributionPolicy.ProviderEndpointClass.ENTERPRISE,
            getBoolean(config, "includeAttributionHeaders", true),
            getMap(config, "customHeaders"),
            Optional.of(getLong(config, "requestTimeoutMs", 60000L)),
            ProviderAttributionPolicy.RetryPolicy.defaults(),
            ProviderAttributionPolicy.RateLimitPolicy.relaxed()
        );
    }
    
    private ProviderAttributionPolicy createCustomPolicy(
            String providerId, 
            Map<String, Object> config) {
        
        return new ProviderAttributionPolicy(
            providerId,
            ProviderAttributionPolicy.ProviderEndpointClass.CUSTOM,
            getBoolean(config, "includeAttributionHeaders", false),
            getMap(config, "customHeaders"),
            Optional.of(getLong(config, "requestTimeoutMs", 30000L)),
            ProviderAttributionPolicy.RetryPolicy.defaults(),
            ProviderAttributionPolicy.RateLimitPolicy.defaults()
        );
    }
    
    private String buildCacheKey(String providerId, RequestType requestType) {
        return providerId + ":" + requestType.name();
    }
    
    private String getOpenClawVersion() {
        // In real implementation, read from version.properties or manifest
        return "2026.4.12";
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, String> getMap(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Map) {
            try {
                return (Map<String, String>) value;
            } catch (ClassCastException e) {
                return Map.of();
            }
        }
        return Map.of();
    }
    
    private boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    private long getLong(Map<String, Object> config, String key, long defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }
    
    // Records
    
    /**
     * Request type enumeration.
     */
    public enum RequestType {
        CHAT_COMPLETION,
        EMBEDDING,
        IMAGE_GENERATION,
        AUDIO_TRANSCRIPTION,
        TOOL_CALL,
        HEALTH_CHECK
    }
    
    /**
     * Request context for attribution.
     */
    public record RequestContext(
            String sessionKey,
            String agentId,
            String requestId,
            Optional<String> userId
    ) {
        public static RequestContext of(String sessionKey) {
            return new RequestContext(sessionKey, null, generateRequestId(), Optional.empty());
        }
        
        public static RequestContext of(String sessionKey, String agentId) {
            return new RequestContext(sessionKey, agentId, generateRequestId(), Optional.empty());
        }
        
        private static String generateRequestId() {
            return java.util.UUID.randomUUID().toString().substring(0, 8);
        }
    }
    
    /**
     * Routing summary for logging.
     */
    public record RoutingSummary(
            String provider,
            String api,
            String endpointClass,
            String route,
            String policy
    ) {
        @Override
        public String toString() {
            return String.format("provider=%s api=%s endpoint=%s route=%s policy=%s",
                provider, api, endpointClass, route, policy);
        }
    }
    
    /**
     * Describes the routing policy for logging.
     */
    private String describeRoutingPolicy(ProviderAttributionPolicy policy) {
        if (!policy.includeAttributionHeaders()) {
            return "none";
        }
        return switch (policy.endpointClass()) {
            case FIRST_PARTY -> "documented";
            case THIRD_PARTY -> "hidden";
            default -> "none";
        };
    }
    
    /**
     * Describes the route class for logging.
     */
    private String describeRouteClass(ProviderAttributionPolicy.ProviderEndpointClass endpointClass) {
        return switch (endpointClass) {
            case STANDARD -> "default";
            case FIRST_PARTY, ENTERPRISE -> "native";
            case THIRD_PARTY, CUSTOM -> "proxy-like";
            default -> "default";
        };
    }
    
    /**
     * Generates a routing summary for logging.
     *
     * @param providerId the provider ID
     * @param api the API type
     * @param policy the attribution policy
     * @return the routing summary
     */
    public RoutingSummary generateRoutingSummary(
            String providerId,
            String api,
            ProviderAttributionPolicy policy) {
        
        String route = describeRouteClass(policy.endpointClass());
        String routingPolicy = describeRoutingPolicy(policy);
        
        RoutingSummary summary = new RoutingSummary(
            providerId != null ? providerId : "unknown",
            api != null ? api : "unknown",
            policy.endpointClass().name().toLowerCase(),
            route,
            routingPolicy
        );
        
        logger.info("Provider routing: {}", summary);
        
        return summary;
    }
}