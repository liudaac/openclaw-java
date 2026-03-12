package openclaw.tools.exec;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;

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
 * Command execution tool with safety controls.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class CommandExecutionTool implements AgentTool {

    private final Path workingDir;
    private final Duration timeout;
    private final Set<String> allowedCommands;
    private final Set<String> blockedCommands;
    private final boolean requireApproval;

    public CommandExecutionTool(Path workingDir) {
        this(workingDir, Duration.ofMinutes(1), Set.of(), Set.of(), true);
    }

    public CommandExecutionTool(
            Path workingDir,
            Duration timeout,
            Set<String> allowedCommands,
            Set<String> blockedCommands,
            boolean requireApproval) {
        this.workingDir = workingDir;
        this.timeout = timeout;
        this.allowedCommands = allowedCommands;
        this.blockedCommands = blockedCommands;
        this.requireApproval = requireApproval;
    }

    @Override
    public String getName() {
        return "command_execution";
    }

    @Override
    public String getDescription() {
        return "Execute shell commands with safety controls";
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
                Map<String, Object> args = context.arguments();
                String command = args.get("command").toString();

                // Security check
                ToolResult securityCheck = checkSecurity(command);
                if (securityCheck != null) {
                    return securityCheck;
                }

                // Build command
                List<String> argList = (List<String>) args.getOrDefault("args", List.of());
                int timeoutSeconds = (int) args.getOrDefault("timeout_seconds", 60);

                return executeCommand(command, argList, timeoutSeconds);
            } catch (Exception e) {
                return ToolResult.failure("Command execution failed: " + e.getMessage());
            }
        });
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
