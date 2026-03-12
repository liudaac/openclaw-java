package openclaw.tools.exec;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Python interpreter tool for executing Python code.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class PythonInterpreterTool implements AgentTool {

    private final Path workingDir;
    private final long timeoutSeconds;
    private final Set<String> blockedModules;

    public PythonInterpreterTool(Path workingDir) {
        this(workingDir, 30);
    }

    public PythonInterpreterTool(Path workingDir, long timeoutSeconds) {
        this.workingDir = workingDir;
        this.timeoutSeconds = timeoutSeconds;
        this.blockedModules = Set.of(
                "os.system", "subprocess", "eval", "exec",
                "__import__", "open", "file", "input"
        );
    }

    @Override
    public String getName() {
        return "python_interpreter";
    }

    @Override
    public String getDescription() {
        return "Execute Python code safely";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "code", PropertySchema.string("Python code to execute"),
                        "timeout_seconds", PropertySchema.integer("Timeout in seconds")
                ))
                .required(List.of("code"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String code = args.get("code").toString();
                long timeout = (long) args.getOrDefault("timeout_seconds", timeoutSeconds);

                // Security check
                ToolResult securityCheck = checkSecurity(code);
                if (securityCheck != null) {
                    return securityCheck;
                }

                return executePython(code, timeout);
            } catch (Exception e) {
                return ToolResult.failure("Python execution failed: " + e.getMessage());
            }
        });
    }

    private ToolResult checkSecurity(String code) {
        String lowerCode = code.toLowerCase();

        // Check blocked modules/functions
        for (String blocked : blockedModules) {
            if (lowerCode.contains(blocked.toLowerCase())) {
                return ToolResult.failure("Blocked pattern detected: " + blocked);
            }
        }

        // Check for file operations
        if (lowerCode.contains("open(") || lowerCode.contains("with open")) {
            return ToolResult.failure("File operations not allowed. Use file_operation tool instead.");
        }

        // Check for network operations
        if (lowerCode.contains("urllib") || lowerCode.contains("http.client") ||
            lowerCode.contains("socket") || lowerCode.contains("requests")) {
            return ToolResult.failure("Network operations not allowed. Use fetch tool instead.");
        }

        return null;
    }

    private ToolResult executePython(String code, long timeout) {
        try {
            // Create temp file for code
            Path tempFile = Files.createTempFile(workingDir, "script_", ".py");
            Files.writeString(tempFile, code);

            try {
                ProcessBuilder pb = new ProcessBuilder("python3", tempFile.toString());
                pb.directory(workingDir.toFile());
                pb.redirectErrorStream(true);

                Process process = pb.start();

                // Read output
                StringBuilder output = new StringBuilder();
                StringBuilder error = new StringBuilder();

                try (BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                     BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                    // Read stdout
                    String line;
                    while ((line = outReader.readLine()) != null) {
                        output.append(line).append("\n");
                        if (output.length() > 50000) {
                            output.append("... (output truncated)\n");
                            break;
                        }
                    }

                    // Read stderr
                    while ((line = errReader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                }

                // Wait for completion
                boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return ToolResult.failure("Python execution timed out after " + timeout + " seconds");
                }

                int exitCode = process.exitValue();

                if (exitCode == 0) {
                    return ToolResult.success(output.toString(), Map.of(
                            "exit_code", exitCode
                    ));
                } else {
                    return ToolResult.failure("Python error (exit " + exitCode + "):\n" + error + "\n" + output);
                }
            } finally {
                // Cleanup
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            return ToolResult.failure("Failed to execute Python: " + e.getMessage());
        } catch (InterruptedException e) {
            return ToolResult.failure("Python execution interrupted");
        }
    }
}
