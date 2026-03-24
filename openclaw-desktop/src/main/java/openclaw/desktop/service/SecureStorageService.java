package openclaw.desktop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Secure Storage Service for API Keys.
 *
 * <p>Encrypts and stores sensitive data using AES-GCM.</p>
 */
@Service
public class SecureStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SecureStorageService.class);

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    private final Path storagePath;
    private final Properties properties;

    public SecureStorageService() {
        this.storagePath = Paths.get(System.getProperty("user.home"), ".openclaw", "secure", "api-keys.enc");
        this.properties = new Properties();
        loadStorage();
    }

    /**
     * Store API key securely.
     */
    public void storeApiKey(String provider, String apiKey, String password) {
        try {
            String encrypted = encrypt(apiKey, password);
            properties.setProperty("apikey." + provider, encrypted);
            saveStorage();
            logger.info("Stored API key for provider: {}", provider);
        } catch (Exception e) {
            logger.error("Failed to store API key", e);
            throw new RuntimeException("Failed to store API key", e);
        }
    }

    /**
     * Retrieve API key.
     */
    public String retrieveApiKey(String provider, String password) {
        try {
            String encrypted = properties.getProperty("apikey." + provider);
            if (encrypted == null) {
                return null;
            }
            return decrypt(encrypted, password);
        } catch (Exception e) {
            logger.error("Failed to retrieve API key", e);
            throw new RuntimeException("Failed to retrieve API key", e);
        }
    }

    /**
     * Check if API key exists.
     */
    public boolean hasApiKey(String provider) {
        return properties.containsKey("apikey." + provider);
    }

    /**
     * Delete API key.
     */
    public void deleteApiKey(String provider) {
        properties.remove("apikey." + provider);
        saveStorage();
        logger.info("Deleted API key for provider: {}", provider);
    }

    /**
     * Get all stored providers.
     */
    public Map<String, Boolean> getStoredProviders() {
        Map<String, Boolean> result = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("apikey.")) {
                String provider = key.substring(7);
                result.put(provider, true);
            }
        }
        return result;
    }

    /**
     * Encrypt data.
     */
    private String encrypt(String plaintext, String password) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        SecretKey key = deriveKey(password);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
        buffer.put(iv);
        buffer.put(encrypted);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    /**
     * Decrypt data.
     */
    private String decrypt(String ciphertext, String password) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(ciphertext);

        ByteBuffer buffer = ByteBuffer.wrap(decoded);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        SecretKey key = deriveKey(password);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Derive key from password.
     */
    private SecretKey deriveKey(String password) throws Exception {
        // Use a fixed salt based on the password hash for simplicity
        // In production, use a unique salt stored alongside the encrypted data
        byte[] salt = password.getBytes(StandardCharsets.UTF_8);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }

    /**
     * Load storage from file.
     */
    private void loadStorage() {
        try {
            if (Files.exists(storagePath)) {
                properties.load(Files.newInputStream(storagePath));
                logger.info("Loaded secure storage from: {}", storagePath);
            }
        } catch (Exception e) {
            logger.warn("Failed to load secure storage", e);
        }
    }

    /**
     * Save storage to file.
     */
    private void saveStorage() {
        try {
            Files.createDirectories(storagePath.getParent());
            properties.store(Files.newOutputStream(storagePath), "OpenClaw Secure Storage");
        } catch (Exception e) {
            logger.error("Failed to save secure storage", e);
            throw new RuntimeException("Failed to save secure storage", e);
        }
    }
}
