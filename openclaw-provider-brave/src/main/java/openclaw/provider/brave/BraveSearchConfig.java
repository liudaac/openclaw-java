package openclaw.provider.brave;

import java.util.Map;

/**
 * Brave Search configuration with improved onboarding.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class BraveSearchConfig {

    private String apiKey;
    private String baseUrl = "https://api.search.brave.com/res/v1";
    private int timeoutMs = 30000;
    private int maxResults = 10;

    /**
     * Creates config from map.
     *
     * @param config the config map
     * @return the config
     */
    public static BraveSearchConfig fromMap(Map<String, Object> config) {
        BraveSearchConfig result = new BraveSearchConfig();
        if (config != null) {
            result.apiKey = getString(config, "apiKey");
            result.baseUrl = getString(config, "baseUrl", result.baseUrl);
            result.timeoutMs = getInt(config, "timeoutMs", result.timeoutMs);
            result.maxResults = getInt(config, "maxResults", result.maxResults);
        }
        return result;
    }

    /**
     * Validates the configuration.
     *
     * @return validation result
     */
    public ValidationResult validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        if (apiKey == null || apiKey.isBlank()) {
            builder.addError("apiKey", "API key is required. Get your API key from https://api.search.brave.com/app/keys");
        } else if (!apiKey.startsWith("BSA")) {
            builder.addWarning("apiKey", "API key should start with 'BSA'. Please check your key.");
        }

        return builder.build();
    }

    /**
     * Gets the onboarding help text.
     *
     * @return the help text
     */
    public static String getOnboardingHelp() {
        return """
                Brave Search API Setup:
                
                1. Visit https://api.search.brave.com/app/keys
                2. Sign in or create a free account
                3. Click "Create API Key"
                4. Copy the key (starts with "BSA")
                5. Paste it in the API Key field above
                
                Note: The free tier includes 2,000 queries per month.
                """;
    }

    // Getters
    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public int getTimeoutMs() { return timeoutMs; }
    public int getMaxResults() { return maxResults; }

    // Setters
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }

    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);
        return value != null ? value : defaultValue;
    }

    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Validation result.
     */
    public record ValidationResult(
            java.util.List<ValidationError> errors,
            java.util.List<ValidationError> warnings
    ) {
        public boolean isValid() {
            return errors.isEmpty();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final java.util.List<ValidationError> errors = new java.util.ArrayList<>();
            private final java.util.List<ValidationError> warnings = new java.util.ArrayList<>();

            public Builder addError(String field, String message) {
                errors.add(new ValidationError(field, message));
                return this;
            }

            public Builder addWarning(String field, String message) {
                warnings.add(new ValidationError(field, message));
                return this;
            }

            public ValidationResult build() {
                return new ValidationResult(
                        java.util.List.copyOf(errors),
                        java.util.List.copyOf(warnings)
                );
            }
        }
    }

    public record ValidationError(String field, String message) {}
}
