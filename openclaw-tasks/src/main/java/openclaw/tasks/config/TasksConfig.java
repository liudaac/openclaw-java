package openclaw.tasks.config;

import openclaw.tasks.registry.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

/**
 * Configuration for OpenClaw Tasks module.
 */
@Configuration
public class TasksConfig {

    @Value("${openclaw.tasks.db.path:${user.home}/.openclaw/tasks.db}")
    private String dbPath;

    @Bean
    public FlowRegistry flowRegistry() throws SQLException {
        return new SqliteFlowRegistry(dbPath);
    }

    @Bean
    public TaskRegistry taskRegistry() throws SQLException {
        return new SqliteTaskRegistry(dbPath);
    }
}
