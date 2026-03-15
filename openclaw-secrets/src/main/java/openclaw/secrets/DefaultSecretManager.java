package openclaw.secrets;

import openclaw.secrets.audit.AuditLog;
import openclaw.secrets.audit.DefaultAuditLog;
import openclaw.secrets.credential.Credential;
import openclaw.secrets.storage.EncryptedStorage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Default secret manager implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class DefaultSecretManager implements SecretManager {

    private SecretManagerConfig config;
    private EncryptedStorage storage;
    private AuditLog auditLog;
    private boolean initialized = false;

    @Override
    public CompletableFuture<Void> initialize(SecretManagerConfig config) {
        return CompletableFuture.runAsync(() -> {
            this.config = config;
            
            // Initialize encrypted storage
            this.storage = new EncryptedStorage(
                    config.dataDir(),
                    config.masterKey().orElse(null)
            );
            this.storage.initialize();
            
            // Initialize audit log
            if (config.enableAudit()) {
                this.auditLog = new DefaultAuditLog(
                        config.dataDir().resolve("audit"),
                        config.auditRetentionDays()
                );
            } else {
                this.auditLog = AuditLog.noop();
            }
            
            this.initialized = true;
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            if (storage != null) {
                storage.close();
            }
            if (auditLog != null) {
                try {
                    auditLog.close();
                } catch (Exception e) {
                    // Ignore close exceptions during shutdown
                }
            }
            initialized = false;
        });
    }

    @Override
    public CompletableFuture<String> storeSecret(
            String key,
            String value,
            Optional<Map<String, Object>> metadata) {
        ensureInitialized();
        
        return CompletableFuture.supplyAsync(() -> {
            String secretId = storage.store(key, value, metadata.orElse(Map.of()));
            auditLog.log(AuditLog.Action.STORE, key, Map.of("secretId", secretId));
            return secretId;
        });
    }

    @Override
    public CompletableFuture<Optional<String>> retrieveSecret(String key) {
        ensureInitialized();
        
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> value = storage.retrieve(key);
            auditLog.log(AuditLog.Action.RETRIEVE, key, Map.of("found", value.isPresent()));
            return value;
        });
    }

    @Override
    public CompletableFuture<Void> deleteSecret(String key) {
        ensureInitialized();
        
        return CompletableFuture.runAsync(() -> {
            storage.delete(key);
            auditLog.log(AuditLog.Action.DELETE, key, Map.of());
        });
    }

    @Override
    public CompletableFuture<Boolean> hasSecret(String key) {
        ensureInitialized();
        return CompletableFuture.supplyAsync(() -> storage.exists(key));
    }

    @Override
    public CompletableFuture<List<String>> listSecrets() {
        ensureInitialized();
        return CompletableFuture.supplyAsync(storage::listKeys);
    }

    @Override
    public CompletableFuture<Optional<String>> rotateSecret(String key, String newValue) {
        ensureInitialized();
        
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> oldValue = storage.retrieve(key);
            storage.store(key, newValue, Map.of("rotated", true));
            auditLog.log(AuditLog.Action.ROTATE, key, Map.of("hadOldValue", oldValue.isPresent()));
            return oldValue;
        });
    }

    @Override
    public CompletableFuture<Optional<Credential>> getCredential(String profileId) {
        ensureInitialized();
        
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> json = storage.retrieve("credential:" + profileId);
            return json.map(this::parseCredential);
        });
    }

    @Override
    public CompletableFuture<String> storeCredential(Credential credential) {
        ensureInitialized();
        
        return CompletableFuture.supplyAsync(() -> {
            String json = serializeCredential(credential);
            String key = "credential:" + credential.profileId();
            storage.store(key, json, Map.of("type", credential.type().name()));
            auditLog.log(AuditLog.Action.STORE_CREDENTIAL, credential.profileId(), Map.of());
            return credential.profileId();
        });
    }

    @Override
    public CompletableFuture<Void> deleteCredential(String profileId) {
        ensureInitialized();
        
        return CompletableFuture.runAsync(() -> {
            storage.delete("credential:" + profileId);
            auditLog.log(AuditLog.Action.DELETE_CREDENTIAL, profileId, Map.of());
        });
    }

    @Override
    public CompletableFuture<List<Credential>> listCredentials() {
        ensureInitialized();
        
        return CompletableFuture.supplyAsync(() -> {
            return storage.listKeys().stream()
                    .filter(k -> k.startsWith("credential:"))
                    .map(k -> storage.retrieve(k).map(this::parseCredential))
                    .flatMap(Optional::stream)
                    .toList();
        });
    }

    @Override
    public AuditLog getAuditLog() {
        ensureInitialized();
        return auditLog;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("SecretManager not initialized");
        }
    }

    private Credential parseCredential(String json) {
        // Simple parsing - in production use Jackson
        return new Credential("unknown", Credential.CredentialType.API_KEY, Map.of());
    }

    private String serializeCredential(Credential credential) {
        // Simple serialization - in production use Jackson
        return "{}";
    }
}
