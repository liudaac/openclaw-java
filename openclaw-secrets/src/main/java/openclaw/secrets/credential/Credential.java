package openclaw.secrets.credential;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Credential representation.
 *
 * @param profileId the profile ID
 * @param type the credential type
 * @param data the credential data
 * @param createdAt creation timestamp
 * @param expiresAt expiration timestamp if applicable
 * @param metadata additional metadata
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record Credential(
        String profileId,
        CredentialType type,
        Map<String, Object> data,
        Instant createdAt,
        Optional<Instant> expiresAt,
        Map<String, Object> metadata
) {

    /**
     * Creates a credential with current timestamp.
     *
     * @param profileId the profile ID
     * @param type the type
     * @param data the data
     */
    public Credential(String profileId, CredentialType type, Map<String, Object> data) {
        this(profileId, type, data, Instant.now(), Optional.empty(), Map.of());
    }

    /**
     * Creates a builder for Credential.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for Credential.
     */
    public static class Builder {
        private String profileId;
        private CredentialType type;
        private Map<String, Object> data = Map.of();
        private Instant createdAt = Instant.now();
        private Instant expiresAt;
        private Map<String, Object> metadata = Map.of();

        public Builder profileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder type(CredentialType type) {
            this.type = type;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data != null ? data : Map.of();
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : Map.of();
            return this;
        }

        public Credential build() {
            return new Credential(
                    profileId,
                    type,
                    data,
                    createdAt,
                    Optional.ofNullable(expiresAt),
                    metadata
            );
        }
    }

    /**
     * Credential type.
     */
    public enum CredentialType {
        API_KEY,
        OAUTH_TOKEN,
        USERNAME_PASSWORD,
        CERTIFICATE,
        TOKEN,
        CUSTOM
    }

    /**
     * Checks if the credential is expired.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return expiresAt.map(exp -> Instant.now().isAfter(exp)).orElse(false);
    }

    /**
     * Gets a value from the credential data.
     *
     * @param key the key
     * @return the value if present
     */
    public Optional<Object> get(String key) {
        return Optional.ofNullable(data.get(key));
    }

    /**
     * Gets a string value from the credential data.
     *
     * @param key the key
     * @return the value if present
     */
    public Optional<String> getString(String key) {
        return get(key).map(Object::toString);
    }
}
