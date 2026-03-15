package openclaw.secrets;

import openclaw.secrets.audit.AuditLog;
// SecretConfig does not exist, using SecretManagerConfig instead
import openclaw.secrets.credential.Credential;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Secret manager interface for secure credential storage and retrieval.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface SecretManager {

    /**
     * Initializes the secret manager.
     *
     * @param config the configuration
     * @return completion future
     */
    CompletableFuture<Void> initialize(SecretManagerConfig config);

    /**
     * Shuts down the secret manager.
     *
     * @return completion future
     */
    CompletableFuture<Void> shutdown();

    /**
     * Stores a secret.
     *
     * @param key the secret key
     * @param value the secret value
     * @param metadata optional metadata
     * @return the secret ID
     */
    CompletableFuture<String> storeSecret(
            String key,
            String value,
            Optional<Map<String, Object>> metadata
    );

    /**
     * Retrieves a secret.
     *
     * @param key the secret key
     * @return the secret value if found
     */
    CompletableFuture<Optional<String>> retrieveSecret(String key);

    /**
     * Retrieves a secret (alias for retrieveSecret).
     *
     * @param key the secret key
     * @return the secret value if found
     */
    default CompletableFuture<Optional<String>> retrieve(String key) {
        return retrieveSecret(key);
    }

    /**
     * Deletes a secret.
     *
     * @param key the secret key
     * @return completion future
     */
    CompletableFuture<Void> deleteSecret(String key);

    /**
     * Checks if a secret exists.
     *
     * @param key the secret key
     * @return true if exists
     */
    CompletableFuture<Boolean> hasSecret(String key);

    /**
     * Lists all secret keys.
     *
     * @return list of keys
     */
    CompletableFuture<List<String>> listSecrets();

    /**
     * Rotates a secret (updates with new value).
     *
     * @param key the secret key
     * @param newValue the new value
     * @return the old value
     */
    CompletableFuture<Optional<String>> rotateSecret(String key, String newValue);

    /**
     * Gets a credential by profile.
     *
     * @param profileId the profile ID
     * @return the credential if found
     */
    CompletableFuture<Optional<Credential>> getCredential(String profileId);

    /**
     * Stores a credential.
     *
     * @param credential the credential
     * @return the credential ID
     */
    CompletableFuture<String> storeCredential(Credential credential);

    /**
     * Deletes a credential.
     *
     * @param profileId the profile ID
     * @return completion future
     */
    CompletableFuture<Void> deleteCredential(String profileId);

    /**
     * Lists all credentials.
     *
     * @return list of credentials
     */
    CompletableFuture<List<Credential>> listCredentials();

    /**
     * Gets the audit log.
     *
     * @return the audit log
     */
    AuditLog getAuditLog();

    /**
     * Secret manager configuration.
     *
     * @param dataDir the data directory
     * @param masterKey the master encryption key
     * @param enableAudit whether to enable audit logging
     * @param auditRetentionDays audit log retention in days
     */
    record SecretManagerConfig(
            Path dataDir,
            Optional<String> masterKey,
            boolean enableAudit,
            int auditRetentionDays
    ) {

        /**
         * Creates a builder for SecretManagerConfig.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for SecretManagerConfig.
         */
        public static class Builder {
            private Path dataDir;
            private String masterKey;
            private boolean enableAudit = true;
            private int auditRetentionDays = 90;

            public Builder dataDir(Path dataDir) {
                this.dataDir = dataDir;
                return this;
            }

            public Builder masterKey(String masterKey) {
                this.masterKey = masterKey;
                return this;
            }

            public Builder enableAudit(boolean enableAudit) {
                this.enableAudit = enableAudit;
                return this;
            }

            public Builder auditRetentionDays(int auditRetentionDays) {
                this.auditRetentionDays = auditRetentionDays;
                return this;
            }

            public SecretManagerConfig build() {
                return new SecretManagerConfig(
                        dataDir,
                        Optional.ofNullable(masterKey),
                        enableAudit,
                        auditRetentionDays
                );
            }
        }
    }
}
