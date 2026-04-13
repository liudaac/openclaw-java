package openclaw.tools;

import openclaw.browser.BrowserService;
import openclaw.sdk.tool.AgentTool;
import openclaw.tools.cron.CronTool;
import openclaw.tools.db.DatabaseQueryTool;
import openclaw.tools.exec.CommandExecutionTool;
import openclaw.tools.exec.ExecToolConfiguration;
import openclaw.tools.exec.OpenShellTool;
import openclaw.tools.exec.PythonInterpreterTool;
import openclaw.tools.file.FileOperationTool;
import openclaw.tools.image.ImageTool;
import openclaw.tools.search.WebSearchAutoConfiguration;
import openclaw.tools.translate.TranslateTool;
import openclaw.tools.web.FetchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Set;

/**
 * OpenClaw Tools Auto Configuration.
 * Automatically configures all OpenClaw tools when Spring Boot starts.
 *
 * @author OpenClaw Team
 * @version 2026.4.13
 */
@Configuration
@AutoConfigureAfter(WebSearchAutoConfiguration.class)
public class ToolsAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ToolsAutoConfiguration.class);

    /**
     * Command Execution Tool.
     */
    @Bean
    @ConditionalOnMissingBean(name = "commandExecutionTool")
    public AgentTool commandExecutionTool(ExecToolConfiguration config) {
        logger.info("Initializing CommandExecutionTool");
        Path workingDir = Paths.get(System.getProperty("user.home"), ".openclaw", "workspace");
        return new CommandExecutionTool(
            workingDir,
            config.getTimeout() != null ? config.getTimeout() : Duration.ofMinutes(1),
            config.getAllowedCommands() != null ? config.getAllowedCommands() : Set.of(),
            config.getBlockedCommands() != null ? config.getBlockedCommands() : Set.of(
                "rm -rf /", "mkfs", "dd if=/dev/zero", ":(){ :|:& };:", "> /dev/sda"
            ),
            config.isRequireApproval(),
            config.isSandboxEnabled(),
            config.isFailClosed()
        );
    }

    /**
     * Python Interpreter Tool.
     */
    @Bean
    @ConditionalOnMissingBean(name = "pythonInterpreterTool")
    public AgentTool pythonInterpreterTool() {
        logger.info("Initializing PythonInterpreterTool");
        Path workingDir = Paths.get(System.getProperty("user.home"), ".openclaw", "workspace");
        return new PythonInterpreterTool(workingDir);
    }

    /**
     * File Operation Tool.
     */
    @Bean
    @ConditionalOnMissingBean(name = "fileOperationTool")
    public AgentTool fileOperationTool() {
        logger.info("Initializing FileOperationTool");
        Path workingDir = Paths.get(System.getProperty("user.home"), ".openclaw", "workspace");
        return new FileOperationTool(workingDir);
    }

    /**
     * Cron Tool.
     */
    @Bean
    @ConditionalOnMissingBean(name = "cronTool")
    public AgentTool cronTool() {
        logger.info("Initializing CronTool");
        return new CronTool();
    }

    /**
     * Database Query Tool.
     * Note: Requires database configuration. Disabled by default.
     */
    // @Bean
    // @ConditionalOnMissingBean(name = "databaseQueryTool")
    // public AgentTool databaseQueryTool(@Value("${openclaw.tools.db.url:}") String jdbcUrl,
    //                                    @Value("${openclaw.tools.db.username:}") String username,
    //                                    @Value("${openclaw.tools.db.password:}") String password) {
    //     logger.info("Initializing DatabaseQueryTool");
    //     return new DatabaseQueryTool(jdbcUrl, username, password);
    // }

    /**
     * Open Shell Tool.
     */
    @Bean
    @ConditionalOnMissingBean(name = "openShellTool")
    public AgentTool openShellTool() {
        logger.info("Initializing OpenShellTool");
        return new OpenShellTool();
    }

    /**
     * Image Tool.
     */
    @Bean
    @ConditionalOnMissingBean(name = "imageTool")
    public AgentTool imageTool() {
        logger.info("Initializing ImageTool");
        return new ImageTool();
    }

    /**
     * Translate Tool.
     */
    @Bean
    @ConditionalOnMissingBean(name = "translateTool")
    public AgentTool translateTool() {
        logger.info("Initializing TranslateTool");
        return new TranslateTool();
    }

    /**
     * Fetch Tool.
     */
    @Bean
    @ConditionalOnMissingBean(name = "fetchTool")
    public AgentTool fetchTool() {
        logger.info("Initializing FetchTool");
        return new FetchTool();
    }
}
