package openclaw.tools.exec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Set;

/**
 * Configuration properties for command execution tools.
 *
 * <p>Properties are prefixed with "openclaw.tools.exec".</p>
 *
 * Example YAML configuration:
 * <pre>
 * openclaw:
 *   tools:
 *     exec:
 *       sandbox-enabled: true
 *       fail-closed: true
 *       require-approval: true
 *       timeout: 60s
 *       blocked-commands:
 *         - rm -rf /
 *         - mkfs
 * </pre>
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
@Configuration
@ConfigurationProperties(prefix = "openclaw.tools.exec")
public class ExecToolConfiguration {

    /**
     * Whether to enable sandbox detection.
     * When enabled, the tool will check if running in a containerized environment.
     * Default: true
     */
    private boolean sandboxEnabled = true;

    /**
     * Whether to use fail-closed mode.
     * When true and sandbox is not detected, command execution will be rejected.
     * When false, commands will execute even outside sandbox (with warning).
     * Default: true
     */
    private boolean failClosed = true;

    /**
     * Whether to require user approval before executing commands.
     * Default: true
     */
    private boolean requireApproval = true;

    /**
     * Default timeout for command execution.
     * Default: 60 seconds
     */
    private Duration timeout = Duration.ofMinutes(1);

    /**
     * List of blocked command patterns.
     * Commands containing these patterns will be rejected.
     */
    private Set<String> blockedCommands = Set.of();

    /**
     * List of allowed command patterns (whitelist).
     * If non-empty, only commands starting with these patterns are allowed.
     */
    private Set<String> allowedCommands = Set.of();

    // Getters and Setters

    public boolean isSandboxEnabled() {
        return sandboxEnabled;
    }

    public void setSandboxEnabled(boolean sandboxEnabled) {
        this.sandboxEnabled = sandboxEnabled;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    public void setFailClosed(boolean failClosed) {
        this.failClosed = failClosed;
    }

    public boolean isRequireApproval() {
        return requireApproval;
    }

    public void setRequireApproval(boolean requireApproval) {
        this.requireApproval = requireApproval;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Set<String> getBlockedCommands() {
        return blockedCommands;
    }

    public void setBlockedCommands(Set<String> blockedCommands) {
        this.blockedCommands = blockedCommands != null ? blockedCommands : Set.of();
    }

    public Set<String> getAllowedCommands() {
        return allowedCommands;
    }

    public void setAllowedCommands(Set<String> allowedCommands) {
        this.allowedCommands = allowedCommands != null ? allowedCommands : Set.of();
    }

    @Override
    public String toString() {
        return "ExecToolConfiguration{" +
                "sandboxEnabled=" + sandboxEnabled +
                ", failClosed=" + failClosed +
                ", requireApproval=" + requireApproval +
                ", timeout=" + timeout +
                ", blockedCommands=" + blockedCommands +
                ", allowedCommands=" + allowedCommands +
                '}';
    }
}
