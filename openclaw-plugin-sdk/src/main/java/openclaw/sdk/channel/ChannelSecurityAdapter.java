package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel security adapter for authentication and authorization.
 *
 * @param <ResolvedAccount> the type of resolved account
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelSecurityAdapter<ResolvedAccount> {

    /**
     * Authenticates a request.
     *
     * @param credentials the credentials
     * @return the authenticated account if valid
     */
    CompletableFuture<Optional<ResolvedAccount>> authenticate(Map<String, Object> credentials);

    /**
     * Authorizes an action.
     *
     * @param account the account
     * @param action the action
     * @param resource the resource
     * @return true if authorized
     */
    CompletableFuture<Boolean> authorize(
            ResolvedAccount account,
            String action,
            String resource
    );

    /**
     * Validates a webhook request.
     *
     * @param headers the request headers
     * @param body the request body
     * @return validation result
     */
    CompletableFuture<WebhookValidationResult> validateWebhook(
            Map<String, String> headers,
            String body
    );

    /**
     * Webhook validation result.
     *
     * @param valid whether the webhook is valid
     * @param account the associated account if valid
     * @param error error message if invalid
     */
    record WebhookValidationResult<T>(
            boolean valid,
            Optional<T> account,
            Optional<String> error
    ) {

        /**
         * Creates a valid webhook result.
         *
         * @param account the account
         * @return the result
         */
        public static <T> WebhookValidationResult<T> valid(T account) {
            return new WebhookValidationResult<>(true, Optional.ofNullable(account), Optional.empty());
        }

        /**
         * Creates an invalid webhook result.
         *
         * @param error the error message
         * @return the result
         */
        public static <T> WebhookValidationResult<T> invalid(String error) {
            return new WebhookValidationResult<>(false, Optional.empty(), Optional.of(error));
        }
    }
}
