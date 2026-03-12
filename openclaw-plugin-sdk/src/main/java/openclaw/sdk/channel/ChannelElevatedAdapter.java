package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel elevated adapter for privileged operations.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelElevatedAdapter {

    /**
     * Checks if elevated permissions are available.
     *
     * @param account the account
     * @return true if elevated
     */
    CompletableFuture<Boolean> isElevated(Object account);

    /**
     * Requests elevated permissions.
     *
     * @param account the account
     * @param reason the reason for elevation
     * @return elevation result
     */
    CompletableFuture<ElevationResult> requestElevation(
            Object account,
            String reason
    );

    /**
     * Performs an elevated operation.
     *
     * @param account the account
     * @param operation the operation to perform
     * @return operation result
     */
    CompletableFuture<OperationResult> performElevatedOperation(
            Object account,
            ElevatedOperation operation
    );

    /**
     * Elevation result.
     *
     * @param granted whether elevation was granted
     * @param token the elevation token if granted
     * @param expiresAt expiration timestamp
     * @param error error message if denied
     */
    record ElevationResult(
            boolean granted,
            Optional<String> token,
            Optional<Long> expiresAt,
            Optional<String> error
    ) {

        /**
         * Creates a granted elevation result.
         *
         * @param token the token
         * @param expiresAt expiration
         * @return the result
         */
        public static ElevationResult granted(String token, long expiresAt) {
            return new ElevationResult(true, Optional.of(token), Optional.of(expiresAt), Optional.empty());
        }

        /**
         * Creates a denied elevation result.
         *
         * @param error the error message
         * @return the result
         */
        public static ElevationResult denied(String error) {
            return new ElevationResult(false, Optional.empty(), Optional.empty(), Optional.of(error));
        }
    }

    /**
     * Elevated operation.
     *
     * @param type the operation type
     * @param params the operation parameters
     */
    record ElevatedOperation(
            String type,
            Map<String, Object> params
    ) {
    }

    /**
     * Operation result.
     *
     * @param success whether the operation succeeded
     * @param result the result
     * @param error error message if failed
     */
    record OperationResult(
            boolean success,
            Optional<Object> result,
            Optional<String> error
    ) {

        /**
         * Creates a successful operation result.
         *
         * @param result the result
         * @return the result
         */
        public static OperationResult success(Object result) {
            return new OperationResult(true, Optional.of(result), Optional.empty());
        }

        /**
         * Creates a failed operation result.
         *
         * @param error the error message
         * @return the result
         */
        public static OperationResult failure(String error) {
            return new OperationResult(false, Optional.empty(), Optional.of(error));
        }
    }
}
