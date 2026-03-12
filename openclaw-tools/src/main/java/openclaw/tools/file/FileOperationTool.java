package openclaw.tools.file;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * File operation tool for reading, writing, and managing files.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class FileOperationTool implements AgentTool {

    private final Path workspaceDir;
    private final boolean allowWrite;

    public FileOperationTool(Path workspaceDir) {
        this(workspaceDir, true);
    }

    public FileOperationTool(Path workspaceDir, boolean allowWrite) {
        this.workspaceDir = workspaceDir;
        this.allowWrite = allowWrite;
    }

    @Override
    public String getName() {
        return "file_operation";
    }

    @Override
    public String getDescription() {
        return "Read, write, and manage files in the workspace";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "operation", PropertySchema.enum_("The operation to perform",
                                List.of("read", "write", "list", "delete", "exists")),
                        "path", PropertySchema.string("The file path (relative to workspace)"),
                        "content", PropertySchema.string("Content to write (for write operation)"),
                        "create_dirs", PropertySchema.boolean_("Create parent directories if missing")
                ))
                .required(List.of("operation", "path"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String operation = args.get("operation").toString();
                String pathStr = args.get("path").toString();

                // Security: resolve against workspace
                Path targetPath = workspaceDir.resolve(pathStr).normalize();

                // Ensure path is within workspace
                if (!targetPath.startsWith(workspaceDir)) {
                    return ToolResult.failure("Path escapes workspace: " + pathStr);
                }

                switch (operation) {
                    case "read":
                        return readFile(targetPath);
                    case "write":
                        if (!allowWrite) {
                            return ToolResult.failure("Write operations not allowed");
                        }
                        String content = args.getOrDefault("content", "").toString();
                        boolean createDirs = (boolean) args.getOrDefault("create_dirs", false);
                        return writeFile(targetPath, content, createDirs);
                    case "list":
                        return listDirectory(targetPath);
                    case "delete":
                        if (!allowWrite) {
                            return ToolResult.failure("Delete operations not allowed");
                        }
                        return deleteFile(targetPath);
                    case "exists":
                        return checkExists(targetPath);
                    default:
                        return ToolResult.failure("Unknown operation: " + operation);
                }
            } catch (Exception e) {
                return ToolResult.failure("File operation failed: " + e.getMessage());
            }
        });
    }

    private ToolResult readFile(Path path) {
        try {
            if (!Files.exists(path)) {
                return ToolResult.failure("File not found: " + path.getFileName());
            }
            if (Files.isDirectory(path)) {
                return ToolResult.failure("Path is a directory: " + path.getFileName());
            }

            // Check file size (max 1MB)
            long size = Files.size(path);
            if (size > 1024 * 1024) {
                return ToolResult.failure("File too large (>1MB)");
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            return ToolResult.success(content, Map.of(
                    "size", size,
                    "lines", content.lines().count()
            ));
        } catch (IOException e) {
            return ToolResult.failure("Failed to read file: " + e.getMessage());
        }
    }

    private ToolResult writeFile(Path path, String content, boolean createDirs) {
        try {
            if (createDirs) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return ToolResult.success("File written successfully", Map.of(
                    "path", path.toString(),
                    "size", content.getBytes(StandardCharsets.UTF_8).length
            ));
        } catch (IOException e) {
            return ToolResult.failure("Failed to write file: " + e.getMessage());
        }
    }

    private ToolResult listDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                return ToolResult.failure("Directory not found: " + path.getFileName());
            }
            if (!Files.isDirectory(path)) {
                return ToolResult.failure("Path is not a directory: " + path.getFileName());
            }

            StringBuilder sb = new StringBuilder();
            try (var stream = Files.list(path)) {
                stream.forEach(entry -> {
                    String type = Files.isDirectory(entry) ? "[DIR]" : "[FILE]";
                    sb.append(type).append(" ").append(entry.getFileName()).append("\n");
                });
            }
            return ToolResult.success(sb.toString());
        } catch (IOException e) {
            return ToolResult.failure("Failed to list directory: " + e.getMessage());
        }
    }

    private ToolResult deleteFile(Path path) {
        try {
            if (!Files.exists(path)) {
                return ToolResult.failure("File not found: " + path.getFileName());
            }
            Files.delete(path);
            return ToolResult.success("Deleted: " + path.getFileName());
        } catch (IOException e) {
            return ToolResult.failure("Failed to delete: " + e.getMessage());
        }
    }

    private ToolResult checkExists(Path path) {
        boolean exists = Files.exists(path);
        boolean isFile = Files.isRegularFile(path);
        boolean isDir = Files.isDirectory(path);
        return ToolResult.success(exists ? "exists" : "not found", Map.of(
                "exists", exists,
                "is_file", isFile,
                "is_directory", isDir
        ));
    }
}
