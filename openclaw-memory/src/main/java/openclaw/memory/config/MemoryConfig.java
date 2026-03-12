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
}
