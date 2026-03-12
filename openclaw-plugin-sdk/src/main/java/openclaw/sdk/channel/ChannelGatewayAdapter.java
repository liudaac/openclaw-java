package openclaw.sdk.channel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Channel gateway adapter for gateway integration.
 *
 * @param <ResolvedAccount> the type of resolved account
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelGatewayAdapter<ResolvedAccount> {

    /**
     * Gets gateway methods provided by this channel.
     *
     * @return list of method names
     */
    java.util.List<String> getGatewayMethods();

    /**
     * Handles a gateway method call.
     *
     * @param account the account
     * @param method the method name
     * @param params the method parameters
     * @return the method result
     */
    CompletableFuture<GatewayResult> handleGatewayMethod(
            ResolvedAccount account,
            String method,
            Map<String, Object> params
    );

    /**
     * Gets the gateway URL for this channel.
     *
     * @param account the account
     * @return the gateway URL
     */
    CompletableFuture<String> getGatewayUrl(ResolvedAccount account);

    /**
     * Gateway result.
     *
     * @param success whether the call succeeded
     * @param result the result data
     * @param error error message if failed
     */
    record GatewayResult(
            boolean success,
            Object result,
            String error
    ) {

        /**
         * Creates a successful gateway result.
         *
         * @param result the result
         * @return the gateway result
         */
        public static GatewayResult success(Object result) {
            return new GatewayResult(true, result, null);
        }

        /**
         * Creates a failed gateway result.
         *
         * @param error the error message
         * @return the gateway result
         */
        public static GatewayResult failure(String error) {
            return new GatewayResult(false, null, error);
        }
    }
}
