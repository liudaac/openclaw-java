package openclaw.gateway.provider.attribution;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provider attribution policy for request routing and attribution.
 *
 * <p>Following the TypeScript provider-attribution.ts pattern.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public record ProviderAttributionPolicy(
        /**
         * The provider ID this policy applies to.
         */
        String providerId,
        
        /**
         * The endpoint classification for this provider.
         */
        ProviderEndpointClass endpointClass,
        
        /**
         * Whether to include attribution headers in requests.
         */
        boolean includeAttributionHeaders,
        
        /**
         * Custom attribution headers to include.
         */
        Map<String, String> customHeaders,
        
        /**
         * Request timeout in milliseconds.
         */
        Optional<Long> requestTimeoutMs,
        
        /**
         * Retry policy configuration.
         */
        RetryPolicy retryPolicy,
        
        /**
         * Rate limiting configuration.
         */
        RateLimitPolicy rateLimitPolicy
) {
    
    /**
     * Creates a default policy for a provider.
     */
    public static ProviderAttributionPolicy defaults(String providerId) {
        return new ProviderAttributionPolicy(
            providerId,
            ProviderEndpointClass.STANDARD,
            true,
            Map.of(),
            Optional.of(30000L),
            RetryPolicy.defaults(),
            RateLimitPolicy.defaults()
        );
    }
    
    /**
     * Creates a policy for first-party endpoints.
     */
    public static ProviderAttributionPolicy firstParty(String providerId) {
        return new ProviderAttributionPolicy(
            providerId,
            ProviderEndpointClass.FIRST_PARTY,
            true,
            Map.of("X-Attribution-Source", "openclaw"),
            Optional.of(30000L),
            RetryPolicy.defaults(),
            RateLimitPolicy.defaults()
        );
    }
    
    /**
     * Creates a policy for third-party endpoints.
     */
    public static ProviderAttributionPolicy thirdParty(String providerId) {
        return new ProviderAttributionPolicy(
            providerId,
            ProviderEndpointClass.THIRD_PARTY,
            false,
            Map.of(),
            Optional.of(60000L),
            RetryPolicy.aggressive(),
            RateLimitPolicy.strict()
        );
    }
    
    /**
     * Provider endpoint classification.
     */
    public enum ProviderEndpointClass {
        /**
         * Standard API endpoints.
         */
        STANDARD,
        
        /**
         * First-party owned endpoints (e.g., OpenAI's own API).
         */
        FIRST_PARTY,
        
        /**
         * Third-party endpoints (e.g., proxy services).
         */
        THIRD_PARTY,
        
        /**
         * Enterprise endpoints with special handling.
         */
        ENTERPRISE,
        
        /**
         * Custom or self-hosted endpoints.
         */
        CUSTOM
    }
    
    /**
     * Retry policy configuration.
     */
    public record RetryPolicy(
            int maxRetries,
            long baseDelayMs,
            long maxDelayMs,
            double backoffMultiplier,
            Set<Integer> retryableStatusCodes
    ) {
        public static RetryPolicy defaults() {
            return new RetryPolicy(
                3,
                1000L,
                30000L,
                2.0,
                Set.of(408, 429, 500, 502, 503, 504)
            );
        }
        
        public static RetryPolicy aggressive() {
            return new RetryPolicy(
                5,
                500L,
                60000L,
                1.5,
                Set.of(408, 429, 500, 502, 503, 504, 520, 521, 522, 523, 524)
            );
        }
        
        public static RetryPolicy none() {
            return new RetryPolicy(
                0,
                0L,
                0L,
                1.0,
                Set.of()
            );
        }
    }
    
    /**
     * Rate limit policy configuration.
     */
    public record RateLimitPolicy(
            int maxRequestsPerMinute,
            int maxRequestsPerHour,
            long burstWindowMs,
            boolean enableAdaptiveThrottling
    ) {
        public static RateLimitPolicy defaults() {
            return new RateLimitPolicy(
                60,
                1000,
                1000L,
                true
            );
        }
        
        public static RateLimitPolicy strict() {
            return new RateLimitPolicy(
                30,
                500,
                2000L,
                true
            );
        }
        
        public static RateLimitPolicy relaxed() {
            return new RateLimitPolicy(
                120,
                2000,
                500L,
                false
            );
        }
    }
}
