package openclaw.agent.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for reading workspace context files.
 *
 * <p>Provides access to AGENTS.md, SOUL.md, USER.md, MEMORY.md, HEARTBEAT.md, etc.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
@Service
public class WorkspaceFileService {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceFileService.class);

    // Default workspace directory
    private static final String DEFAULT_WORKSPACE_DIR = System.getProperty("user.home") + "/.openclaw/workspace";

    // Context file names
    public static final String FILE_AGENTS = "AGENTS.md";
    public static final String FILE_SOUL = "SOUL.md";
    public static final String FILE_USER = "USER.md";
    public static final String FILE_MEMORY = "MEMORY.md";
    public static final String FILE_HEARTBEAT = "HEARTBEAT.md";
    public static final String FILE_TOOLS = "TOOLS.md";
    public static final String FILE_IDENTITY = "IDENTITY.md";
    public static final String FILE_BOOTSTRAP = "BOOTSTRAP.md";

    private final String workspaceDir;

    public WorkspaceFileService() {
        this(DEFAULT_WORKSPACE_DIR);
    }

    public WorkspaceFileService(String workspaceDir) {
        this.workspaceDir = workspaceDir != null ? workspaceDir : DEFAULT_WORKSPACE_DIR;
    }

    /**
     * Read a file from the workspace.
     *
     * @param filename the file name
     * @return the file content, or empty if not found
     */
    public Optional<String> readFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return Optional.empty();
        }

        Path path = Paths.get(workspaceDir, filename);
        try {
            if (Files.exists(path) && Files.isReadable(path)) {
                String content = Files.readString(path);
                return Optional.of(content);
            }
        } catch (IOException e) {
            logger.warn("Failed to read {}: {}", filename, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Check if a file exists in the workspace.
     *
     * @param filename the file name
     * @return true if exists
     */
    public boolean exists(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        Path path = Paths.get(workspaceDir, filename);
        return Files.exists(path);
    }

    /**
     * Get file last modified time.
     *
     * @param filename the file name
     * @return the last modified time, or empty if not found
     */
    public Optional<Instant> getLastModified(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return Optional.empty();
        }

        Path path = Paths.get(workspaceDir, filename);
        try {
            if (Files.exists(path)) {
                return Optional.of(Files.getLastModifiedTime(path).toInstant());
            }
        } catch (IOException e) {
            logger.warn("Failed to get last modified time for {}: {}", filename, e.getMessage());
        }
        return Optional.empty();
    }

    // Convenience methods for specific files

    /**
     * Read AGENTS.md.
     *
     * @return the content, or empty if not found
     */
    public Optional<String> readAgentsMd() {
        return readFile(FILE_AGENTS);
    }

    /**
     * Read SOUL.md.
     *
     * @return the content, or empty if not found
     */
    public Optional<String> readSoulMd() {
        return readFile(FILE_SOUL);
    }

    /**
     * Read USER.md.
     *
     * @return the content, or empty if not found
     */
    public Optional<String> readUserMd() {
        return readFile(FILE_USER);
    }

    /**
     * Read MEMORY.md.
     *
     * @return the content, or empty if not found
     */
    public Optional<String> readMemoryMd() {
        return readFile(FILE_MEMORY);
    }

    /**
     * Read HEARTBEAT.md.
     *
     * @return the content, or empty if not found
     */
    public Optional<String> readHeartbeatMd() {
        return readFile(FILE_HEARTBEAT);
    }

    /**
     * Read TOOLS.md.
     *
     * @return the content, or empty if not found
     */
    public Optional<String> readToolsMd() {
        return readFile(FILE_TOOLS);
    }

    /**
     * Read IDENTITY.md.
     *
     * @return the content, or empty if not found
     */
    public Optional<String> readIdentityMd() {
        return readFile(FILE_IDENTITY);
    }

    /**
     * Read BOOTSTRAP.md.
     *
     * @return the content, or empty if not found
     */
    public Optional<String> readBootstrapMd() {
        return readFile(FILE_BOOTSTRAP);
    }

    /**
     * Get the workspace directory.
     *
     * @return the workspace directory path
     */
    public String getWorkspaceDir() {
        return workspaceDir;
    }

    /**
     * Read all context files and assemble them.
     *
     * @param includeHeartbeat whether to include HEARTBEAT.md
     * @return the assembled context
     */
    public String assembleContext(boolean includeHeartbeat) {
        StringBuilder sb = new StringBuilder();

        // AGENTS.md - workspace conventions
        readAgentsMd().ifPresent(content -> {
            sb.append("## AGENTS.md - Workspace Conventions\n\n");
            sb.append(content).append("\n\n");
        });

        // SOUL.md - who the agent is
        readSoulMd().ifPresent(content -> {
            sb.append("## SOUL.md - Who You Are\n\n");
            sb.append(content).append("\n\n");
        });

        // USER.md - who the user is
        readUserMd().ifPresent(content -> {
            sb.append("## USER.md - About Your Human\n\n");
            sb.append(content).append("\n\n");
        });

        // MEMORY.md - long-term memory
        readMemoryMd().ifPresent(content -> {
            sb.append("## MEMORY.md - Long-term Memory\n\n");
            sb.append(content).append("\n\n");
        });

        // TOOLS.md - local tool notes
        readToolsMd().ifPresent(content -> {
            sb.append("## TOOLS.md - Local Tool Notes\n\n");
            sb.append(content).append("\n\n");
        });

        // IDENTITY.md - agent identity
        readIdentityMd().ifPresent(content -> {
            sb.append("## IDENTITY.md - Your Identity\n\n");
            sb.append(content).append("\n\n");
        });

        // HEARTBEAT.md (only if requested)
        if (includeHeartbeat) {
            readHeartbeatMd().ifPresent(content -> {
                sb.append("## HEARTBEAT.md - Periodic Tasks\n\n");
                sb.append(content).append("\n\n");
            });
        }

        return sb.toString();
    }
}
