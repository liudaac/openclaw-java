package openclaw.memory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Memory module configuration
 */
@Component
@ConfigurationProperties(prefix = "openclaw.memory")
public class MemoryConfig {

    /**
     * Storage type: memory, sqlite, pgvector
     * Default: sqlite (short-term solution with minimal dependencies)
     */
    private String storageType = "sqlite";

    /**
     * SQLite configuration
     */
    private SqliteConfig sqlite = new SqliteConfig();

    /**
     * PostgreSQL/pgvector configuration
     */
    private PgvectorConfig pgvector = new PgvectorConfig();

    /**
     * Vector search configuration
     */
    private VectorSearchConfig vectorSearch = new VectorSearchConfig();

    /**
     * Full-text search (FTS5) configuration
     */
    private FtsConfig fts = new FtsConfig();

    /**
     * Memory slot configuration for slot-aware operations
     */
    private SlotConfig slot = new SlotConfig();

    /**
     * Dreaming configuration for session ingestion
     */
    private DreamingConfig dreaming = new DreamingConfig();

    // Getters and Setters

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public SqliteConfig getSqlite() {
        return sqlite;
    }

    public void setSqlite(SqliteConfig sqlite) {
        this.sqlite = sqlite;
    }

    public PgvectorConfig getPgvector() {
        return pgvector;
    }

    public void setPgvector(PgvectorConfig pgvector) {
        this.pgvector = pgvector;
    }

    public VectorSearchConfig getVectorSearch() {
        return vectorSearch;
    }

    public void setVectorSearch(VectorSearchConfig vectorSearch) {
        this.vectorSearch = vectorSearch;
    }

    public FtsConfig getFts() {
        return fts;
    }

    public void setFts(FtsConfig fts) {
        this.fts = fts;
    }

    public SlotConfig getSlot() {
        return slot;
    }

    public void setSlot(SlotConfig slot) {
        this.slot = slot;
    }

    public DreamingConfig getDreaming() {
        return dreaming;
    }

    public void setDreaming(DreamingConfig dreaming) {
        this.dreaming = dreaming;
    }

    /**
     * SQLite configuration
     */
    public static class SqliteConfig {
        private String url = "jdbc:sqlite:openclaw.db";
        private boolean enabled = true;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * PostgreSQL/pgvector configuration
     */
    public static class PgvectorConfig {
        private String url = "jdbc:postgresql://localhost:5432/openclaw";
        private String username = "openclaw";
        private String password = "password";
        private boolean enabled = false;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Vector search configuration
     */
    public static class VectorSearchConfig {
        private int dimension = 1536;
        private double minScore = 0.7;
        private int defaultLimit = 5;
        private boolean enabled = true;

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        public double getMinScore() {
            return minScore;
        }

        public void setMinScore(double minScore) {
            this.minScore = minScore;
        }

        public int getDefaultLimit() {
            return defaultLimit;
        }

        public void setDefaultLimit(int defaultLimit) {
            this.defaultLimit = defaultLimit;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * FTS5 full-text search configuration
     */
    public static class FtsConfig {
        private String tokenizer = "porter";
        private boolean ftsOnly = false;
        private boolean enabled = true;

        public String getTokenizer() {
            return tokenizer;
        }

        public void setTokenizer(String tokenizer) {
            this.tokenizer = tokenizer;
        }

        public boolean isFtsOnly() {
            return ftsOnly;
        }

        public void setFtsOnly(boolean ftsOnly) {
            this.ftsOnly = ftsOnly;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Memory slot configuration for slot-aware operations
     */
    public static class SlotConfig {
        private String defaultSlot = "default";
        private boolean slotAwarePaths = true;

        public String getDefaultSlot() {
            return defaultSlot;
        }

        public void setDefaultSlot(String defaultSlot) {
            this.defaultSlot = defaultSlot;
        }

        public boolean isSlotAwarePaths() {
            return slotAwarePaths;
        }

        public void setSlotAwarePaths(boolean slotAwarePaths) {
            this.slotAwarePaths = slotAwarePaths;
        }
    }

    /**
     * Dreaming configuration for session ingestion
     */
    public static class DreamingConfig {
        private boolean enabled = true;
        private boolean respectMemorySlot = true;
        private String ingestionMode = "daily"; // "daily", "realtime", "disabled"

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRespectMemorySlot() {
            return respectMemorySlot;
        }

        public void setRespectMemorySlot(boolean respectMemorySlot) {
            this.respectMemorySlot = respectMemorySlot;
        }

        public String getIngestionMode() {
            return ingestionMode;
        }

        public void setIngestionMode(String ingestionMode) {
            this.ingestionMode = ingestionMode;
        }
    }
}
