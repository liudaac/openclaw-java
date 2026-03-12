package openclaw.memory.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * DataSource configuration for memory module
 * 
 * <p>Configures data source based on storage type:</p>
 * <ul>
 *   <li>SQLite (default): File-based, minimal dependencies</li>
 *   <li>PostgreSQL: Production-grade with pgvector</li>
 * </ul>
 */
@Configuration
public class DataSourceConfig {

    /**
     * SQLite DataSource (default)
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "openclaw.memory.storage-type", 
                          havingValue = "sqlite", 
                          matchIfMissing = true)
    public DataSource sqliteDataSource(MemoryConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getSqlite().getUrl());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setMaximumPoolSize(1); // SQLite supports limited concurrency
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        // SQLite optimizations
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
        hikariConfig.addDataSourceProperty("cache_size", "10000");
        
        return new HikariDataSource(hikariConfig);
    }

    /**
     * PostgreSQL DataSource (for pgvector)
     */
    @Bean
    @ConditionalOnProperty(name = "openclaw.memory.storage-type", 
                          havingValue = "pgvector")
    public DataSource postgresDataSource(MemoryConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getPgvector().getUrl());
        hikariConfig.setUsername(config.getPgvector().getUsername());
        hikariConfig.setPassword(config.getPgvector().getPassword());
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        
        return new HikariDataSource(hikariConfig);
    }

    /**
     * JdbcTemplate for PostgreSQL
     */
    @Bean
    @ConditionalOnProperty(name = "openclaw.memory.storage-type", 
                          havingValue = "pgvector")
    public JdbcTemplate pgvectorJdbcTemplate(DataSource postgresDataSource) {
        return new JdbcTemplate(postgresDataSource);
    }
}
