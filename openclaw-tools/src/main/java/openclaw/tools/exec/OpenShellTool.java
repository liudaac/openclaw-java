package openclaw.tools.exec;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.AgentTool.PropertySchema;
import openclaw.sdk.tool.AgentTool.ToolParameters;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OpenShell - Enhanced command execution with SSH support and interactive sessions.
 *
 * Features:
 * - Local command execution with safety controls
 * - Remote SSH command execution
 * - Interactive shell sessions
 * - Session management
 * - Enhanced security policies
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public class OpenShellTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(OpenShellTool.class);

    // Session management
    private final Map<String, ShellSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, RemoteHostConfig> remoteHosts = new ConcurrentHashMap<>();

    // Security settings
    private final Set<String> blockedCommands;
    private final Set<String> allowedCommands;
    private final boolean requireApproval;
    private final Duration defaultTimeout;
    private final Path workingDir;

    // SSH settings
    private final String sshKeyPath;
    private final boolean strictHostKeyChecking;

    public OpenShellTool() {
        this(Path.of(System.getProperty("user.dir")), Duration.ofMinutes(5),
                Set.of(), Set.of(), false, null, true);
    }

    public OpenShellTool(
            Path workingDir,
            Duration defaultTimeout,
            Set<String> allowedCommands,
            Set<String> blockedCommands,
            boolean requireApproval,
            String sshKeyPath,
            boolean strictHostKeyChecking) {
        this.workingDir = workingDir;
        this.defaultTimeout = defaultTimeout;
        this.allowedCommands = allowedCommands != null ? new HashSet<>(allowedCommands) : new HashSet<>();
        this.blockedCommands = blockedCommands != null ? new HashSet<>(blockedCommands) : new HashSet<>();
        this.requireApproval = requireApproval;
        this.sshKeyPath = sshKeyPath;
        this.strictHostKeyChecking = strictHostKeyChecking;

        // Default blocked commands
        this.blockedCommands.addAll(Set.of(
                "rm -rf /", "rm -rf /*", "dd if=/dev/zero",
                ":(){ :|:& };:", "mkfs", "fdisk",
                "format", "del /f /s /q", "rd /s /q",
                "shutdown", "reboot", "halt", "poweroff",
                "mkfs.ext", "mkfs.ntfs", "mkfs.fat"
        ));
    }

    @Override
    public String getName() {
        return "openshell";
    }

    @Override
    public String getDescription() {
        return "Execute shell commands locally or remotely via SSH with interactive session support";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.ofEntries(
                        Map.entry("action", PropertySchema.enum_("Action to perform", List.of(
                                "exec", "exec_remote", "create_session", "close_session",
                                "list_sessions", "send_input", "add_host"
                        ))),
                        Map.entry("command", PropertySchema.string("Command to execute (for exec actions)")),
                        Map.entry("args", PropertySchema.array("Command arguments", PropertySchema.string("Argument"))),
                        Map.entry("session_id", PropertySchema.string("Session ID (for session actions)")),
                        Map.entry("host", PropertySchema.string("Remote host (for exec_remote)")),
                        Map.entry("port", PropertySchema.integer("SSH port (default: 22)")),
                        Map.entry("username", PropertySchema.string("SSH username")),
                        Map.entry("password", PropertySchema.string("SSH password (optional if using key)")),
                        Map.entry("timeout_seconds", PropertySchema.integer("Timeout in seconds (default: 300)")),
                        Map.entry("working_dir", PropertySchema.string("Working directory for command")),
                        Map.entry("input", PropertySchema.string("Input to send to interactive session")),
                        Map.entry("host_alias", PropertySchema.string("Alias for remote host configuration")),
                        Map.entry("private_key_path", PropertySchema.string("Path to SSH private key")),
                        Map.entry("env", PropertySchema.array("Environment variables", PropertySchema.string("KEY=VALUE")))
                ))
                .required(List.of("action"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String action = args.get("action").toString();

                switch (action) {
                    case "exec":
                        return executeLocalCommand(args);
                    case "exec_remote":
                        return executeRemoteCommand(args);
                    case "create_session":
                        return createSession(args);
                    case "close_session":
                        return closeSession(args);
                    case "list_sessions":
                        return listSessions();
                    case "send_input":
                        return sendInput(args);
                    case "add_host":
                        return addHost(args);
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }
            } catch (Exception e) {
                logger.error("OpenShell execution failed", e);
                return ToolResult.failure("Execution failed: " + e.getMessage());
            }
        });
    }

    /**
     * Execute a local command with security checks.
     */
    private ToolResult executeLocalCommand(Map<String, Object> args) {
        String command = (String) args.get("command");
        if (command == null || command.isEmpty()) {
            return ToolResult.failure("Command is required");
        }

        // Security check
        ToolResult securityCheck = checkSecurity(command);
        if (securityCheck != null) {
            return securityCheck;
        }

        List<String> argList = (List<String>) args.getOrDefault("args", List.of());
        int timeoutSeconds = (int) args.getOrDefault("timeout_seconds", (int) defaultTimeout.getSeconds());
        String workingDirStr = (String) args.get("working_dir");
        Path cmdWorkingDir = workingDirStr != null ? Path.of(workingDirStr) : workingDir;

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command().add(command);
            pb.command().addAll(argList);
            pb.directory(cmdWorkingDir.toFile());
            pb.redirectErrorStream(true);

            // Set environment variables
            List<String> envVars = (List<String>) args.getOrDefault("env", List.of());
            Map<String, String> env = pb.environment();
            for (String envVar : envVars) {
                String[] parts = envVar.split("=", 2);
                if (parts.length == 2) {
                    env.put(parts[0], parts[1]);
                }
            }

            Process process = pb.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (output.length() > 100000) {
                        output.append("... (output truncated)\n");
                        break;
                    }
                }
            }

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
                        "command", command,
                        "working_dir", cmdWorkingDir.toString()
                ));
            } else {
                return ToolResult.failure("Command failed with exit code " + exitCode + ":\n" + result);
            }

        } catch (IOException | InterruptedException e) {
            return ToolResult.failure("Command execution failed: " + e.getMessage());
        }
    }

    /**
     * Execute a command on a remote host via SSH.
     */
    private ToolResult executeRemoteCommand(Map<String, Object> args) {
        String host = (String) args.get("host");
        String command = (String) args.get("command");
        String username = (String) args.get("username");

        if (host == null || command == null || username == null) {
            return ToolResult.failure("host, command, and username are required for remote execution");
        }

        // Security check
        ToolResult securityCheck = checkSecurity(command);
        if (securityCheck != null) {
            return securityCheck;
        }

        int port = (int) args.getOrDefault("port", 22);
        int timeoutSeconds = (int) args.getOrDefault("timeout_seconds", (int) defaultTimeout.getSeconds());
        String password = (String) args.get("password");
        String privateKeyPath = (String) args.get("private_key_path");

        try {
            return executeSshCommand(host, port, username, password, privateKeyPath, command, timeoutSeconds);
        } catch (Exception e) {
            return ToolResult.failure("SSH execution failed: " + e.getMessage());
        }
    }

    /**
     * Execute SSH command using JSch or system ssh command.
     */
    private ToolResult executeSshCommand(String host, int port, String username,
                                          String password, String privateKeyPath,
                                          String command, int timeoutSeconds) {
        try {
            // Build SSH command
            List<String> sshArgs = new ArrayList<>();
            sshArgs.add("ssh");

            // Port
            if (port != 22) {
                sshArgs.add("-p");
                sshArgs.add(String.valueOf(port));
            }

            // Strict host key checking
            if (!strictHostKeyChecking) {
                sshArgs.add("-o");
                sshArgs.add("StrictHostKeyChecking=no");
                sshArgs.add("-o");
                sshArgs.add("UserKnownHostsFile=/dev/null");
            }

            // Timeout
            sshArgs.add("-o");
            sshArgs.add("ConnectTimeout=" + Math.min(timeoutSeconds, 30));

            // Private key
            String keyPath = privateKeyPath != null ? privateKeyPath : sshKeyPath;
            if (keyPath != null && !keyPath.isEmpty()) {
                sshArgs.add("-i");
                sshArgs.add(keyPath);
            }

            // Target
            sshArgs.add(username + "@" + host);

            // Remote command
            sshArgs.add(command);

            ProcessBuilder pb = new ProcessBuilder(sshArgs);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (output.length() > 100000) {
                        output.append("... (output truncated)\n");
                        break;
                    }
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolResult.failure("SSH command timed out after " + timeoutSeconds + " seconds");
            }

            int exitCode = process.exitValue();
            String result = output.toString();

            if (exitCode == 0) {
                return ToolResult.success(result, Map.of(
                        "exit_code", exitCode,
                        "host", host,
                        "command", command
                ));
            } else {
                return ToolResult.failure("SSH command failed with exit code " + exitCode + ":\n" + result);
            }

        } catch (IOException | InterruptedException e) {
            return ToolResult.failure("SSH execution failed: " + e.getMessage());
        }
    }

    /**
     * Create an interactive shell session.
     */
    private ToolResult createSession(Map<String, Object> args) {
        String sessionId = (String) args.getOrDefault("session_id", UUID.randomUUID().toString());
        String host = (String) args.get("host");

        try {
            ProcessBuilder pb;
            if (host != null) {
                // Remote session via SSH
                String username = (String) args.get("username");
                if (username == null) {
                    return ToolResult.failure("username is required for remote session");
                }
                int port = (int) args.getOrDefault("port", 22);
                pb = buildSshProcessBuilder(host, port, username, args);
            } else {
                // Local shell session
                pb = new ProcessBuilder(getShellCommand());
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            ShellSession session = new ShellSession(sessionId, host, process);
            sessions.put(sessionId, session);

            // Start output reader thread
            Thread readerThread = new Thread(() -> readSessionOutput(session));
            readerThread.setDaemon(true);
            readerThread.start();

            return ToolResult.success("Session created: " + sessionId, Map.of(
                    "session_id", sessionId,
                    "host", host != null ? host : "localhost",
                    "type", host != null ? "remote" : "local"
            ));

        } catch (IOException e) {
            return ToolResult.failure("Failed to create session: " + e.getMessage());
        }
    }

    /**
     * Close a shell session.
     */
    private ToolResult closeSession(Map<String, Object> args) {
        String sessionId = (String) args.get("session_id");
        if (sessionId == null) {
            return ToolResult.failure("session_id is required");
        }

        ShellSession session = sessions.remove(sessionId);
        if (session == null) {
            return ToolResult.failure("Session not found: " + sessionId);
        }

        session.close();
        return ToolResult.success("Session closed: " + sessionId);
    }

    /**
     * List active sessions.
     */
    private ToolResult listSessions() {
        List<Map<String, Object>> sessionList = sessions.values().stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("session_id", s.getId());
                    map.put("host", s.getHost());
                    map.put("active", s.isActive());
                    map.put("created_at", s.getCreatedAt().toString());
                    return map;
                })
                .toList();

        return ToolResult.success("Found " + sessionList.size() + " session(s)",
                Map.of("sessions", sessionList));
    }

    /**
     * Send input to an interactive session.
     */
    private ToolResult sendInput(Map<String, Object> args) {
        String sessionId = (String) args.get("session_id");
        String input = (String) args.get("input");

        if (sessionId == null || input == null) {
            return ToolResult.failure("session_id and input are required");
        }

        ShellSession session = sessions.get(sessionId);
        if (session == null) {
            return ToolResult.failure("Session not found: " + sessionId);
        }

        if (!session.isActive()) {
            return ToolResult.failure("Session is not active: " + sessionId);
        }

        try {
            session.sendInput(input);
            Thread.sleep(100); // Wait for output
            String output = session.getRecentOutput();
            return ToolResult.success(output, Map.of(
                    "session_id", sessionId,
                    "input_sent", input
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to send input: " + e.getMessage());
        }
    }

    /**
     * Add a remote host configuration.
     */
    private ToolResult addHost(Map<String, Object> args) {
        String alias = (String) args.get("host_alias");
        String host = (String) args.get("host");
        String username = (String) args.get("username");

        if (alias == null || host == null || username == null) {
            return ToolResult.failure("host_alias, host, and username are required");
        }

        RemoteHostConfig config = new RemoteHostConfig(
                host,
                (int) args.getOrDefault("port", 22),
                username,
                (String) args.get("password"),
                (String) args.get("private_key_path")
        );

        remoteHosts.put(alias, config);
        return ToolResult.success("Host added: " + alias, Map.of(
                "alias", alias,
                "host", host,
                "username", username
        ));
    }

    /**
     * Security check for commands.
     */
    private ToolResult checkSecurity(String command) {
        if (command == null || command.isEmpty()) {
            return ToolResult.failure("Command cannot be empty");
        }

        String lowerCmd = command.toLowerCase().trim();

        // Check blocked commands
        for (String blocked : blockedCommands) {
            if (lowerCmd.contains(blocked.toLowerCase())) {
                return ToolResult.failure("Command contains blocked pattern: " + blocked);
            }
        }

        // Check allowed commands whitelist
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

        return null; // Passed security check
    }

    /**
     * Build SSH ProcessBuilder for interactive session.
     */
    private ProcessBuilder buildSshProcessBuilder(String host, int port, String username,
                                                   Map<String, Object> args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ssh");

        if (port != 22) {
            cmd.add("-p");
            cmd.add(String.valueOf(port));
        }

        if (!strictHostKeyChecking) {
            cmd.add("-o");
            cmd.add("StrictHostKeyChecking=no");
        }

        String keyPath = (String) args.get("private_key_path");
        if (keyPath == null) {
            keyPath = sshKeyPath;
        }
        if (keyPath != null && !keyPath.isEmpty()) {
            cmd.add("-i");
            cmd.add(keyPath);
        }

        cmd.add("-t"); // Force pseudo-terminal allocation
        cmd.add(username + "@" + host);

        return new ProcessBuilder(cmd);
    }

    /**
     * Get the appropriate shell command for the current OS.
     */
    private List<String> getShellCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return List.of("cmd.exe");
        } else {
            return List.of("/bin/bash", "-i");
        }
    }

    /**
     * Read output from a session.
     */
    private void readSessionOutput(ShellSession session) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(session.getProcess().getInputStream()))) {
            String line;
            while (session.isActive() && (line = reader.readLine()) != null) {
                session.appendOutput(line + "\n");
            }
        } catch (IOException e) {
            logger.error("Error reading session output", e);
        }
    }

    // ==================== Inner Classes ====================

    /**
     * Represents an interactive shell session.
     */
    private static class ShellSession {
        private final String id;
        private final String host;
        private final Process process;
        private final StringBuilder output;
        private final java.time.Instant createdAt;
        private volatile boolean active;

        public ShellSession(String id, String host, Process process) {
            this.id = id;
            this.host = host;
            this.process = process;
            this.output = new StringBuilder();
            this.createdAt = java.time.Instant.now();
            this.active = true;
        }

        public String getId() { return id; }
        public String getHost() { return host != null ? host : "localhost"; }
        public Process getProcess() { return process; }
        public java.time.Instant getCreatedAt() { return createdAt; }
        public boolean isActive() {
            return active && process.isAlive();
        }

        public synchronized void appendOutput(String text) {
            output.append(text);
            // Keep last 10000 chars
            if (output.length() > 10000) {
                output.delete(0, output.length() - 10000);
            }
        }

        public synchronized String getRecentOutput() {
            return output.toString();
        }

        public void sendInput(String input) throws IOException {
            if (!process.isAlive()) {
                throw new IOException("Process is not alive");
            }
            OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
            writer.write(input);
            writer.write("\n");
            writer.flush();
        }

        public void close() {
            active = false;
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Remote host configuration.
     */
    private static class RemoteHostConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final String privateKeyPath;

        public RemoteHostConfig(String host, int port, String username,
                                String password, String privateKeyPath) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.privateKeyPath = privateKeyPath;
        }
    }
}
