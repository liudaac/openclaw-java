package openclaw.session.config;

import openclaw.session.store.SessionStore;
import openclaw.session.store.SQLiteSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Auto-configuration for session persistence.
 *
 * @author OpenClaw Team
 * @version 2026.3.20
 */
@Configuration
@EnableConfigurationProperties(SessionConfig.class)
@ConditionalOnProperty(prefix = "openclaw.session", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SessionAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SessionAutoConfiguration.class);

    /**
     * Creates SQLite DataSource for session storage.
     */
    @Bean
    @ConditionalOnProperty(prefix = "openclaw.session", name = "storage-type", havingValue = "sqlite", matchIfMissing = true)
    public DataSource sessionDataSource(SessionConfig config, Environment env) {
        String dbPath = resolveDbPath(config.getDbPath(), env);
        
        try {
            Path path = Paths.get(dbPath);
            Files.createDirectories(path.getParent());
            logger.info("Session database path: {}", dbPath);
        } catch (Exception e) {
            logger.warn("Failed to create session database directory: {}", dbPath, e);
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + dbPath);
        
        return dataSource;
    }

    /**
     * Creates SQLite session store.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "openclaw.session", name = "storage-type", havingValue = "sqlite", matchIfMissing = true)
    public SessionStore sessionStore(DataSource sessionDataSource, SessionConfig config) {
        logger.info("Creating SQLite session store");
        return new SQLiteSessionStore(sessionDataSource, config);
    }

    /**
     * Resolves database path with environment variable substitution.
     */
    private String resolveDbPath(String path, Environment env) {
        if (path.startsWith("${user.home}")) {
            String userHome = System.getProperty("user.home");
            path = path.replace("${user.home}", userHome);
        }
        
        // Additional environment variable resolution
        if (path.contains("${")) {
            path = env.resolvePlaceholders(path);
        }
        
        return path;
    }
}