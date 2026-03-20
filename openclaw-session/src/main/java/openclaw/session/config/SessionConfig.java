package openclaw.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration properties for session management.
 *
 * @author OpenClaw Team
 * @version 2026.3.20
 */
@Configuration
@ConfigurationProperties(prefix = "openclaw.session")
public class SessionConfig {

    /**
     * SQLite database file path.
     */
    private String dbPath = "${user.home}/.openclaw/sessions.db";

    /**
     * Maximum number of messages to keep per session.
     */
    private int maxMessages = 1000;

    /**
     * Session time-to-live (TTL).
     */
    private Duration ttl = Duration.ofDays(30);

    /**
     * Whether to auto-cleanup expired sessions.
     */
    private boolean autoCleanup = true;

    /**
     * Cleanup interval.
     */
    private Duration cleanupInterval = Duration.ofHours(1);

    /**
     * Whether to enable session persistence.
     */
    private boolean enabled = true;

    /**
     * Storage type: sqlite, memory, or redis.
     */
    private StorageType storageType = StorageType.SQLITE;

    public enum StorageType {
        SQLITE, MEMORY, REDIS
    }

    // Getters and Setters

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public boolean isAutoCleanup() {
        return autoCleanup;
    }

    public void setAutoCleanup(boolean autoCleanup) {
        this.autoCleanup = autoCleanup;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    @Override
    public String toString() {
        return String.format(
            "SessionConfig{dbPath='%s', maxMessages=%d, ttl=%s, autoCleanup=%s, storageType=%s}",
            dbPath, maxMessages, ttl, autoCleanup, storageType
        );
    }
}