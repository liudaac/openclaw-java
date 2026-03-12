package openclaw.secrets.storage;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/**
 * Encrypted file storage for secrets.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class EncryptedStorage implements AutoCloseable {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final Path dataDir;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptedStorage(Path dataDir, String masterKey) {
        this.dataDir = dataDir;
        this.secretKey = deriveKey(masterKey);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Initializes the storage.
     */
    public void initialize() {
        try {
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }

    /**
     * Stores a value.
     *
     * @param key the key
     * @param value the value
     * @param metadata the metadata
     * @return the storage ID
     */
    public String store(String key, String value, Map<String, Object> metadata) {
        try {
            String id = UUID.randomUUID().toString();
            Path filePath = dataDir.resolve(encodeKey(key) + ".enc");
            
            // Encrypt value
            byte[] encrypted = encrypt(value.getBytes(StandardCharsets.UTF_8));
            
            // Store metadata
            StorageEntry entry = new StorageEntry(
                    id,
                    key,
                    Base64.getEncoder().encodeToString(encrypted),
                    System.currentTimeMillis(),
                    metadata
            );
            
            // Write to file
            String json = serialize(entry);
            Files.writeString(filePath, json);
            
            return id;
        } catch (Exception e) {
            throw new StorageException("Failed to store secret", e);
        }
    }

    /**
     * Retrieves a value.
     *
     * @param key the key
     * @return the value if found
     */
    public Optional<String> retrieve(String key) {
        try {
            Path filePath = dataDir.resolve(encodeKey(key) + ".enc");
            
            if (!Files.exists(filePath)) {
                return Optional.empty();
            }
            
            // Read file
            String json = Files.readString(filePath);
            StorageEntry entry = deserialize(json);
            
            // Decrypt value
            byte[] encrypted = Base64.getDecoder().decode(entry.encryptedValue());
            byte[] decrypted = decrypt(encrypted);
            
            return Optional.of(new String(decrypted, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve secret", e);
        }
    }

    /**
     * Deletes a value.
     *
     * @param key the key
     */
    public void delete(String key) {
        try {
            Path filePath = dataDir.resolve(encodeKey(key) + ".enc");
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            throw new StorageException("Failed to delete secret", e);
        }
    }

    /**
     * Checks if a key exists.
     *
     * @param key the key
     * @return true if exists
     */
    public boolean exists(String key) {
        Path filePath = dataDir.resolve(encodeKey(key) + ".enc");
        return Files.exists(filePath);
    }

    /**
     * Lists all keys.
     *
     * @return list of keys
     */
    public List<String> listKeys() {
        try {
            return Files.list(dataDir)
                    .filter(p -> p.toString().endsWith(".enc"))
                    .map(p -> {
                        String filename = p.getFileName().toString();
                        return decodeKey(filename.substring(0, filename.length() - 4));
                    })
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }

    private byte[] encrypt(byte[] plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        
        byte[] ciphertext = cipher.doFinal(plaintext);
        
        // Combine IV + ciphertext
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        
        return buffer.array();
    }

    private byte[] decrypt(byte[] encrypted) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encrypted);
        
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        
        return cipher.doFinal(ciphertext);
    }

    private SecretKey deriveKey(String masterKey) {
        try {
            if (masterKey == null || masterKey.isEmpty()) {
                // Generate random key if no master key provided
                byte[] keyBytes = new byte[32];
                new SecureRandom().nextBytes(keyBytes);
                return new SecretKeySpec(keyBytes, ALGORITHM);
            }
            
            // Derive key from master key using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(masterKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive key", e);
        }
    }

    private String encodeKey(String key) {
        // Base64 encode to make filesystem-safe
        return Base64.getUrlEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeKey(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private String serialize(StorageEntry entry) {
        // Simple JSON serialization
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(entry.id()).append("\",");
        sb.append("\"key\":\"").append(entry.key()).append("\",");
        sb.append("\"encryptedValue\":\"").append(entry.encryptedValue()).append("\",");
        sb.append("\"timestamp\":").append(entry.timestamp()).append(",");
        sb.append("\"metadata\":").append(mapToJson(entry.metadata()));
        sb.append("}");
        return sb.toString();
    }

    private StorageEntry deserialize(String json) {
        // Simple JSON parsing - in production use Jackson
        return new StorageEntry("", "", "", 0, Map.of());
    }

    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append("\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Storage entry.
     *
     * @param id the ID
     * @param key the key
     * @param encryptedValue the encrypted value
     * @param timestamp the timestamp
     * @param metadata the metadata
     */
    private record StorageEntry(
            String id,
            String key,
            String encryptedValue,
            long timestamp,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Storage exception.
     */
    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
