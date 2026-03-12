package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel configuration adapter.
 *
 * @param <ResolvedAccount> the type of resolved account
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelConfigAdapter<ResolvedAccount> {

    /**
     * Validates the configuration.
     *
     * @param config the raw configuration
     * @return validation result
     */
    CompletableFuture<ConfigValidationResult> validate(Map<String, Object> config);

    /**
     * Resolves the account from configuration.
     *
     * @param config the configuration
     * @return the resolved account if valid
     */
    CompletableFuture<Optional<ResolvedAccount>> resolveAccount(Map<String, Object> config);

    /**
     * Gets the configuration schema.
     *
     * @return the schema
     */
    ChannelConfigSchema getSchema();

    /**
     * Gets default configuration values.
     *
     * @return default values
     */
    Map<String, Object> getDefaults();

    /**
     * Configuration validation result.
     *
     * @param valid whether the config is valid
     * @param errors validation errors if invalid
     */
    record ConfigValidationResult(boolean valid, Optional<java.util.List<String>> errors) {

        /**
         * Creates a successful validation result.
         *
         * @return the result
         */
        public static ConfigValidationResult success() {
            return new ConfigValidationResult(true, Optional.empty());
        }

        /**
         * Creates a failed validation result.
         *
         * @param errors the error messages
         * @return the result
         */
        public static ConfigValidationResult failure(java.util.List<String> errors) {
            return new ConfigValidationResult(false, Optional.of(errors));
        }
    }
}
