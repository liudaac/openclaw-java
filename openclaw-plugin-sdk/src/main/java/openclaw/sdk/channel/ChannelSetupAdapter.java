package openclaw.sdk.channel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Channel setup adapter for initial configuration.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelSetupAdapter {

    /**
     * Checks if setup is required.
     *
     * @return true if setup is needed
     */
    CompletableFuture<Boolean> isSetupRequired();

    /**
     * Performs initial setup.
     *
     * @param config the initial configuration
     * @return setup result
     */
    CompletableFuture<SetupResult> setup(Map<String, Object> config);

    /**
     * Setup result.
     *
     * @param success whether setup succeeded
     * @param message result message
     * @param config the updated configuration
     */
    record SetupResult(
            boolean success,
            String message,
            Map<String, Object> config
    ) {

        /**
         * Creates a successful setup result.
         *
         * @param message the success message
         * @param config the config
         * @return the result
         */
        public static SetupResult success(String message, Map<String, Object> config) {
            return new SetupResult(true, message, config);
        }

        /**
         * Creates a failed setup result.
         *
         * @param message the error message
         * @return the result
         */
        public static SetupResult failure(String message) {
            return new SetupResult(false, message, Map.of());
        }
    }
}
