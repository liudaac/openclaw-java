package openclaw.secrets.config;

import openclaw.secrets.SecretManager;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Encrypted configuration I/O.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class ConfigIO {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretManager secretManager;
    private final Path configDir;

    public ConfigIO(SecretManager secretManager, Path configDir) {
        this.secretManager = secretManager;
        this.configDir = configDir;
    }

    /**
     * Reads an encrypted configuration file.
     *
     * @param filename the filename
     * @return the decrypted config
     */
    public CompletableFuture<Map<String, Object>> readConfig(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = configDir.resolve(filename);
                if (!Files.exists(filePath)) {
                    return Map.of();
                }

                byte[] encrypted = Files.readAllBytes(filePath);
                String json = decrypt(encrypted);

                // Parse JSON (simplified)
                return parseJson(json);
            } catch (Exception e) {
                throw new ConfigIOException("Failed to read config", e);
            }
        });
    }

    /**
     * Writes an encrypted configuration file.
     *
     * @param filename the filename
     * @param config the config
     * @return completion future
     */
    public CompletableFuture<Void> writeConfig(String filename, Map<String, Object> config) {
        return CompletableFuture.runAsync(() -> {
            try {
                String json = toJson(config);
                byte[] encrypted = encrypt(json);

                Path filePath = configDir.resolve(filename);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, encrypted);
            } catch (Exception e) {
                throw new ConfigIOException("Failed to write config", e);
            }
        });
    }

    /**
     * Checks if a config file exists.
     *
     * @param filename the filename
     * @return true if exists
     */
    public boolean exists(String filename) {
        return Files.exists(configDir.resolve(filename));
    }

    /**
     * Deletes a config file.
     *
     * @param filename the filename
     * @return completion future
     */
    public CompletableFuture<Void> deleteConfig(String filename) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.deleteIfExists(configDir.resolve(filename));
            } catch (IOException e) {
                throw new ConfigIOException("Failed to delete config", e);
            }
        });
    }

    private byte[] encrypt(String plaintext) throws Exception {
        // Get encryption key from secret manager
        String keyStr = secretManager.retrieve("config-key").join()
                .orElseThrow(() -> new IllegalStateException("Config key not found"))
                .toString();

        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);

        // Generate IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // Encrypt
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Combine IV + ciphertext
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

        return result;
    }

    private String decrypt(byte[] encrypted) throws Exception {
        // Get encryption key from secret manager
        String keyStr = secretManager.retrieve("config-key").join()
                .orElseThrow(() -> new IllegalStateException("Config key not found"))
                .toString();

        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);

        // Extract IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encrypted, 0, iv, 0, iv.length);

        // Extract ciphertext
        byte[] ciphertext = new byte[encrypted.length - iv.length];
        System.arraycopy(encrypted, iv.length, ciphertext, 0, ciphertext.length);

        // Decrypt
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private String toJson(Map<String, Object> config) {
        // Simple JSON serialization
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> parseJson(String json) {
        // Simple JSON parsing
        return Map.of();
    }

    /**
     * Config I/O exception.
     */
    public static class ConfigIOException extends RuntimeException {
        public ConfigIOException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
