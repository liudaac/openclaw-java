package openclaw.tools.exec;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Command execution tool with safety controls and sandbox detection.
 *
 * Features:
 * - Sandbox environment detection (Docker, Containerd, etc.)
 * - Fail-closed mode: rejects execution when sandbox is unavailable
 * - Configurable security policies
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
public class CommandExecutionTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutionTool.class);

    private final Path workingDir;
    private final Duration timeout;
    private final Set<String> allowedCommands;
    private final Set<String> blockedCommands;
    private final boolean requireApproval;

    // Sandbox settings
    private final boolean sandboxEnabled;
    private final boolean failClosed;
    private final SandboxDetector sandboxDetector;

    /**
     * Creates a CommandExecutionTool with default settings.
     * Sandbox is enabled with fail-closed mode by default.
     */
    public CommandExecutionTool(Path workingDir) {
        this(workingDir, Duration.ofMinutes(1), Set.of(), Set.of(), true, true, true);
    }

    /**
     * Creates a CommandExecutionTool with full configuration.
     *
     * @param workingDir the working directory for command execution
     * @param timeout the default timeout for commands
     * @param allowedCommands whitelist of allowed commands (empty = allow all)
     * @param blockedCommands blacklist of blocked commands
     * @param requireApproval whether to require user approval for commands
     * @param sandboxEnabled whether to enable sandbox detection
     * @param failClosed if true, reject execution when sandbox is unavailable
     */
    public CommandExecutionTool(
            Path workingDir,
            Duration timeout,
            Set<String> allowedCommands,
            Set<String> blockedCommands,
            boolean requireApproval,
            boolean sandboxEnabled,
            boolean failClosed) {
        this.workingDir = workingDir;
        this.timeout = timeout;
        this.allowedCommands = allowedCommands != null ? allowedCommands : Set.of();
        this.blockedCommands = blockedCommands != null ? blockedCommands : Set.of();
        this.requireApproval = requireApproval;
        this.sandboxEnabled = sandboxEnabled;
        this.failClosed = failClosed;
        this.sandboxDetector = new SandboxDetector();

        logger.info("CommandExecutionTool initialized: sandboxEnabled={}, failClosed={}",
                sandboxEnabled, failClosed);
    }

    @Override
    public String getName() {
        return "command_execution";
    }

    @Override
    public String getDescription() {
        return "Execute shell commands with safety controls and sandbox detection";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "command", PropertySchema.string("The command to execute"),
                        "args", PropertySchema.array("Command arguments", PropertySchema.string("Argument")),
                        "timeout_seconds", PropertySchema.integer("Timeout in seconds (default 60)")
                ))
                .required(List.of("command"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Check sandbox availability (fail-closed)
                ToolResult sandboxCheck = checkSandboxAvailability();
                if (sandboxCheck != null) {
                    return sandboxCheck;
                }

                Map<String, Object> args = context.arguments();
                String command = args.get("command").toString();

                // Step 2: Security check
                ToolResult securityCheck = checkSecurity(command);
                if (securityCheck != null) {
                    return securityCheck;
                }

                // Build command
                List<String> argList = (List<String>) args.getOrDefault("args", List.of());
                int timeoutSeconds = (int) args.getOrDefault("timeout_seconds", 60);

                return executeCommand(command, argList, timeoutSeconds);
            } catch (Exception e) {
                logger.error("Command execution failed", e);
                return ToolResult.failure("Command execution failed: " + e.getMessage());
            }
        });
    }

    /**
     * Checks if the sandbox is available when sandbox mode is enabled.
     * In fail-closed mode, execution is rejected if sandbox is not detected.
     *
     * @return ToolResult if sandbox check fails, null if passed
     */
    private ToolResult checkSandboxAvailability() {
        if (!sandboxEnabled) {
            logger.debug("Sandbox detection disabled, skipping check");
            return null;
        }

        boolean isSandboxed = sandboxDetector.isSandboxed();

        if (!isSandboxed && failClosed) {
            logger.warn("Sandbox not detected and fail-closed mode is enabled. Command execution rejected.");
            String status = sandboxDetector.getSandboxStatus();
            return ToolResult.failure(
                    "SECURITY: Command execution blocked - sandbox environment not detected.\n" +
                    "Fail-closed mode is enabled. Commands can only execute within a containerized sandbox.\n\n" +
                    status + "\n\n" +
                    "To disable this protection, set sandboxEnabled=false or failClosed=false in configuration."
            );
        }

        if (isSandboxed) {
            logger.debug("Sandbox environment confirmed, proceeding with execution");
        } else {
            logger.warn("Sandbox not detected but fail-closed is disabled, proceeding with caution");
        }

        return null; // Passed sandbox check
    }

    /**
     * Checks if the current environment is sandboxed.
     * Public method for external sandbox status queries.
     *
     * @return true if running in a sandbox
     */
    public boolean isSandboxed() {
        return sandboxDetector.isSandboxed();
    }

    /**
     * Gets the current sandbox status as a descriptive string.
     *
     * @return sandbox status description
     */
    public String getSandboxStatus() {
        return sandboxDetector.getSandboxStatus();
    }

    private ToolResult checkSecurity(String command) {
        // Check blocked commands
        String lowerCmd = command.toLowerCase();
        for (String blocked : blockedCommands) {
            if (lowerCmd.contains(blocked.toLowerCase())) {
                return ToolResult.failure("Command contains blocked pattern: " + blocked);
            }
        }

        // Check allowed commands (if whitelist is defined)
        if (!allowedCommands.isEmpty()) {
            boolean allowed = false;
            for (String allowedCmd : allowedCommands) {
                if (lowerCmd.startsWith(allowedCmd.toLowerCase())) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return ToolResult.failure("Command not in allowed list");
            }
        }

        // Dangerous patterns
        Set<String> dangerous = Set.of(
                "rm -rf /", "rm -rf /*", "dd if=/dev/zero",
                ":(){ :|:& };:", "mkfs", "fdisk",
                "format", "del /f /s /q", "rd /s /q"
        );
        for (String pattern : dangerous) {
            if (lowerCmd.contains(pattern)) {
                return ToolResult.failure("Dangerous command detected");
            }
        }

        return null; // Passed security check
    }

    private ToolResult executeCommand(String command, List<String> args, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command().add(command);
            pb.command().addAll(args);
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Limit output size
                    if (output.length() > 100000) {
                        output.append("... (output truncated)\n");
                        break;
                    }
                }
            }

            // Wait for completion
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolResult.failure("Command timed out after " + timeoutSeconds + " seconds");
            }

            int exitCode = process.exitValue();
            String result = output.toString();

            if (exitCode == 0) {
                return ToolResult.success(result, Map.of(
                        "exit_code", exitCode,
                        "command", command
                ));
            } else {
                return ToolResult.failure("Command failed with exit code " + exitCode + ":\n" + result);
            }
        } catch (IOException e) {
            return ToolResult.failure("Failed to execute command: " + e.getMessage());
        } catch (InterruptedException e) {
            return ToolResult.failure("Command interrupted");
        }
    }
}
