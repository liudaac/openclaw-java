package openclaw.plugin.sdk.websearch.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache utilities for web search.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
public final class CacheUtils {

    private static final Logger logger = LoggerFactory.getLogger(CacheUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();

    // Cache directory (can be configured)
    private static Path cacheDir = Paths.get(System.getProperty("user.home"), ".openclaw", "cache", "web-search");

    private CacheUtils() {
        // Utility class
    }

    /**
     * Set cache directory.
     *
     * @param dir the cache directory
     */
    public static void setCacheDir(Path dir) {
        cacheDir = dir;
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            logger.warn("Failed to create cache directory: {}", dir, e);
        }
    }

    /**
     * Build cache key from components.
     *
     * @param components the components
     * @return the cache key
     */
    public static String buildSearchCacheKey(Object... components) {
        StringBuilder sb = new StringBuilder();
        for (Object component : components) {
            if (component != null) {
                if (sb.length() > 0) {
                    sb.append("|");
                }
                sb.append(component.toString());
            }
        }
        return sha256(sb.toString());
    }

    /**
     * Read cached search payload.
     *
     * @param key the cache key
     * @return optional cached payload
     */
    public static Optional<Map<String, Object>> readCachedSearchPayload(String key) {
        // Check memory cache first
        CacheEntry memoryEntry = memoryCache.get(key);
        if (memoryEntry != null && !memoryEntry.isExpired()) {
            logger.debug("Cache hit (memory): {}", key);
            return Optional.of(memoryEntry.getData());
        }

        // Check disk cache
        Path cacheFile = cacheDir.resolve(key + ".json");
        if (Files.exists(cacheFile)) {
            try {
                String content = Files.readString(cacheFile);
                CacheEntry entry = objectMapper.readValue(content, CacheEntry.class);
                if (!entry.isExpired()) {
                    logger.debug("Cache hit (disk): {}", key);
                    // Promote to memory cache
                    memoryCache.put(key, entry);
                    return Optional.of(entry.getData());
                } else {
                    // Delete expired cache
                    Files.deleteIfExists(cacheFile);
                    logger.debug("Cache expired: {}", key);
                }
            } catch (IOException e) {
                logger.warn("Failed to read cache file: {}", cacheFile, e);
            }
        }

        return Optional.empty();
    }

    /**
     * Write cached search payload.
     *
     * @param key the cache key
     * @param payload the payload
     * @param ttlMs TTL in milliseconds
     */
    public static void writeCachedSearchPayload(String key, Map<String, Object> payload, long ttlMs) {
        CacheEntry entry = new CacheEntry(payload, System.currentTimeMillis() + ttlMs);

        // Write to memory cache
        memoryCache.put(key, entry);

        // Write to disk cache
        try {
            Files.createDirectories(cacheDir);
            Path cacheFile = cacheDir.resolve(key + ".json");
            String content = objectMapper.writeValueAsString(entry);
            Files.writeString(cacheFile, content);
        } catch (IOException e) {
            logger.warn("Failed to write cache file: {}", key, e);
        }
    }

    /**
     * Clear all caches.
     */
    public static void clearCache() {
        memoryCache.clear();
        try {
            if (Files.exists(cacheDir)) {
                Files.list(cacheDir)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                logger.warn("Failed to delete cache file: {}", p, e);
                            }
                        });
            }
        } catch (IOException e) {
            logger.warn("Failed to clear cache directory", e);
        }
    }

    /**
     * Clean expired entries from memory cache.
     */
    public static void cleanExpiredMemoryCache() {
        memoryCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Compute SHA-256 hash.
     *
     * @param input the input string
     * @return the hash
     */
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * Cache entry.
     */
    private static class CacheEntry {
        private Map<String, Object> data;
        private long expiresAt;

        public CacheEntry() {
        }

        public CacheEntry(Map<String, Object> data, long expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
