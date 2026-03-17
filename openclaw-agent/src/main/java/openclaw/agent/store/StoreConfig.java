package openclaw.agent.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Store configuration properties.
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
@Component
@ConfigurationProperties(prefix = "openclaw.store")
public class StoreConfig {

    /**
     * Store type: memory, file, redis
     */
    private SessionStore.StoreType type = SessionStore.StoreType.MEMORY;

    /**
     * File store configuration.
     */
    private FileConfig file = new FileConfig();

    /**
     * Redis store configuration.
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * Session TTL configuration.
     */
    private SessionConfig session = new SessionConfig();

    public SessionStore.StoreType getType() {
        return type;
    }

    public void setType(SessionStore.StoreType type) {
        this.type = type;
    }

    public void setType(String type) {
        this.type = SessionStore.StoreType.valueOf(type.toUpperCase());
    }

    public FileConfig getFile() {
        return file;
    }

    public void setFile(FileConfig file) {
        this.file = file;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public SessionConfig getSession() {
        return session;
    }

    public void setSession(SessionConfig session) {
        this.session = session;
    }

    /**
     * File store configuration.
     */
    public static class FileConfig {
        /**
         * Base directory for file storage.
         */
        private String baseDir = System.getProperty("user.home") + "/.openclaw/store";

        /**
         * File extension.
         */
        private String extension = ".json";

        /**
         * Auto compact interval.
         */
        private Duration compactInterval = Duration.ofHours(1);

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public Duration getCompactInterval() {
            return compactInterval;
        }

        public void setCompactInterval(Duration compactInterval) {
            this.compactInterval = compactInterval;
        }
    }

    /**
     * Redis store configuration.
     */
    public static class RedisConfig {
        /**
         * Redis host.
         */
        private String host = "localhost";

        /**
         * Redis port.
         */
        private int port = 6379;

        /**
         * Redis password (optional).
         */
        private String password;

        /**
         * Redis database.
         */
        private int database = 0;

        /**
         * Connection timeout.
         */
        private Duration timeout = Duration.ofSeconds(5);

        /**
         * Key prefix.
         */
        private String keyPrefix = "openclaw:";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    /**
     * Session configuration.
     */
    public static class SessionConfig {
        /**
         * Session TTL (time to live).
         */
        private Duration ttl = Duration.ofHours(24);

        /**
         * Maximum messages per session.
         */
        private int maxMessages = 1000;

        /**
         * Auto cleanup inactive sessions.
         */
        private boolean autoCleanup = true;

        /**
         * Cleanup interval.
         */
        private Duration cleanupInterval = Duration.ofMinutes(30);

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
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
    }
}
