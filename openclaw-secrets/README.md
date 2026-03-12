# OpenClaw Secrets Manager (Java)

Secrets and configuration management for OpenClaw Java Edition, compatible with Node.js version 2026.3.9.

## Features

- **Encrypted Storage**: AES-256-GCM encryption for secrets
- **Audit Logging**: Comprehensive audit trail for all operations
- **Credential Matrix**: Multi-credential management
- **Key Rotation**: Automatic and manual secret rotation
- **Secure Key Derivation**: SHA-256 based key derivation

## Architecture

```
openclaw-secrets/
├── SecretManager          # Main interface
├── DefaultSecretManager   # Default implementation
├── credential/            # Credential management
├── audit/                 # Audit logging
└── storage/               # Encrypted storage
```

## Usage

### Initialize

```java
SecretManager manager = new DefaultSecretManager();

SecretManagerConfig config = SecretManagerConfig.builder()
    .dataDir(Path.of("/data/secrets"))
    .masterKey(System.getenv("OPENCLAW_MASTER_KEY"))
    .enableAudit(true)
    .auditRetentionDays(90)
    .build();

manager.initialize(config).join();
```

### Store Secret

```java
String secretId = manager.storeSecret(
    "api-key",
    "sk-abc123",
    Optional.of(Map.of("service", "openai"))
).join();
```

### Retrieve Secret

```java
Optional<String> secret = manager.retrieveSecret("api-key").join();
secret.ifPresent(value -> System.out.println("Secret: " + value));
```

### Rotate Secret

```java
Optional<String> oldValue = manager.rotateSecret("api-key", "sk-new456").join();
oldValue.ifPresent(old -> System.out.println("Old value: " + old));
```

### Credential Management

```java
// Create credential
Credential credential = Credential.builder()
    .profileId("openai-profile")
    .type(Credential.CredentialType.API_KEY)
    .data(Map.of("apiKey", "sk-abc123"))
    .build();

// Store credential
manager.storeCredential(credential).join();

// Retrieve credential
Optional<Credential> cred = manager.getCredential("openai-profile").join();
```

### Audit Log

```java
AuditLog auditLog = manager.getAuditLog();

// Get recent entries
List<AuditEntry> entries = auditLog.getEntries(100);

// Get entries for specific target
List<AuditEntry> targetEntries = auditLog.getEntriesForTarget("api-key", 50);

// Clear old entries
auditLog.clearOldEntries(Instant.now().minusSeconds(30 * 24 * 60 * 60));
```

## Security Features

1. **AES-256-GCM Encryption**: Industry-standard authenticated encryption
2. **Random IV**: Each encryption uses unique IV
3. **Key Derivation**: SHA-256 based key derivation
4. **Filesystem-safe Keys**: Base64 encoding for filesystem compatibility
5. **Audit Trail**: All operations logged with timestamps

## Credential Types

- `API_KEY` - API keys
- `OAUTH_TOKEN` - OAuth tokens
- `USERNAME_PASSWORD` - Username/password pairs
- `CERTIFICATE` - TLS certificates
- `TOKEN` - Generic tokens
- `CUSTOM` - Custom credential types

## Statistics

- **Total Java files**: 7
- **Encryption**: AES-256-GCM
- **Key Derivation**: SHA-256
- **Storage**: Filesystem-based with JSON

## Dependencies

- Bouncy Castle (cryptography)
- Jackson (JSON)

## Next Steps

1. Add support for hardware security modules (HSM)
2. Implement secret versioning
3. Add distributed secret storage
4. Integrate with cloud secret managers (AWS Secrets Manager, etc.)
