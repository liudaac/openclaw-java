package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel authentication adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelAuthAdapter {

    /**
     * Gets supported authentication methods.
     *
     * @return list of auth methods
     */
    java.util.List<AuthMethod> getAuthMethods();

    /**
     * Authenticates with a method.
     *
     * @param methodId the method ID
     * @param credentials the credentials
     * @return authentication result
     */
    CompletableFuture<AuthResult> authenticate(
            String methodId,
            Map<String, Object> credentials
    );

    /**
     * Refreshes authentication.
     *
     * @param authToken the current auth token
     * @return new auth result
     */
    CompletableFuture<AuthResult> refresh(String authToken);

    /**
     * Revokes authentication.
     *
     * @param authToken the auth token to revoke
     * @return completion future
     */
    CompletableFuture<Void> revoke(String authToken);

    /**
     * Authentication method.
     *
     * @param id the method ID
     * @param name the method name
     * @param type the method type
     * @param description the description
     */
    record AuthMethod(
            String id,
            String name,
            AuthType type,
            String description
    ) {
    }

    /**
     * Authentication type.
     */
    enum AuthType {
        TOKEN,
        API_KEY,
        OAUTH2,
        USERNAME_PASSWORD,
        CERTIFICATE,
        CUSTOM
    }

    /**
     * Authentication result.
     *
     * @param success whether authentication succeeded
     * @param token the auth token if successful
     * @param expiresAt expiration timestamp if applicable
     * @param error error message if failed
     * @param metadata additional metadata
     */
    record AuthResult(
            boolean success,
            Optional<String> token,
            Optional<Long> expiresAt,
            Optional<String> error,
            Map<String, Object> metadata
    ) {

        /**
         * Creates a successful auth result.
         *
         * @param token the auth token
         * @return the result
         */
        public static AuthResult success(String token) {
            return new AuthResult(
                    true,
                    Optional.of(token),
                    Optional.empty(),
                    Optional.empty(),
                    Map.of()
            );
        }

        /**
         * Creates a successful auth result with expiration.
         *
         * @param token the auth token
         * @param expiresAt expiration timestamp
         * @return the result
         */
        public static AuthResult success(String token, long expiresAt) {
            return new AuthResult(
                    true,
                    Optional.of(token),
                    Optional.of(expiresAt),
                    Optional.empty(),
                    Map.of()
            );
        }

        /**
         * Creates a failed auth result.
         *
         * @param error the error message
         * @return the result
         */
        public static AuthResult failure(String error) {
            return new AuthResult(
                    false,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(error),
                    Map.of()
            );
        }
    }
}
