package openclaw.security;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Workspace path guard for restricting file operations within a workspace.
 * <p>
 * This class implements the security boundary check for tools that output files,
 * ensuring they cannot write outside the designated workspace directory.
 * <p>
 * Based on TypeScript implementation: src/agents/openclaw-tools.nodes-workspace-guard.ts
 *
 * @author OpenClaw Team
 * @version 2026.4.11
 */
public class WorkspacePathGuard {

    private final Path workspaceRoot;
    private final Path containerWorkdir;
    private final boolean normalizeGuardedPathParams;
    private final Set<String> pathParamKeys;

    /**
     * Creates a workspace path guard.
     *
     * @param workspaceRoot the workspace root directory
     */
    public WorkspacePathGuard(Path workspaceRoot) {
        this(workspaceRoot, null, true, Set.of("outPath", "path", "filePath"));
    }

    /**
     * Creates a workspace path guard with options.
     *
     * @param workspaceRoot the workspace root directory
     * @param containerWorkdir the container working directory (for sandboxed environments)
     * @param normalizeGuardedPathParams whether to normalize path parameters
     * @param pathParamKeys the parameter keys to guard
     */
    public WorkspacePathGuard(
            Path workspaceRoot,
            Path containerWorkdir,
            boolean normalizeGuardedPathParams,
            Set<String> pathParamKeys) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.containerWorkdir = containerWorkdir != null ? containerWorkdir.toAbsolutePath().normalize() : null;
        this.normalizeGuardedPathParams = normalizeGuardedPathParams;
        this.pathParamKeys = pathParamKeys != null ? pathParamKeys : Set.of();
    }

    /**
     * Validates that a path is within the workspace.
     *
     * @param path the path to validate
     * @return the validation result
     */
    public PathValidationResult validatePath(String path) {
        if (path == null || path.isBlank()) {
            return PathValidationResult.invalid("Path is null or empty");
        }

        try {
            Path targetPath = resolvePath(path);
            Path normalizedTarget = targetPath.toAbsolutePath().normalize();

            // Check if path is within workspace
            if (!normalizedTarget.startsWith(workspaceRoot)) {
                return PathValidationResult.invalid(
                        "Path escapes workspace: " + path + " (resolved to: " + normalizedTarget + ")"
                );
            }

            return PathValidationResult.valid(normalizedTarget);
        } catch (Exception e) {
            return PathValidationResult.invalid("Invalid path: " + path + " - " + e.getMessage());
        }
    }

    /**
     * Resolves a path against the workspace root.
     *
     * @param path the path to resolve
     * @return the resolved path
     */
    public Path resolvePath(String path) {
        Path targetPath;

        // Handle absolute paths in container environments
        if (containerWorkdir != null && path.startsWith("/")) {
            // Convert absolute container path to host workspace path
            Path containerPath = Paths.get(path).toAbsolutePath().normalize();
            if (containerPath.startsWith(containerWorkdir)) {
                Path relativePath = containerWorkdir.relativize(containerPath);
                targetPath = workspaceRoot.resolve(relativePath);
            } else {
                // Absolute path outside container workdir - reject
                throw new SecurityException("Absolute path outside container workdir: " + path);
            }
        } else {
            // Relative path - resolve against workspace
            targetPath = workspaceRoot.resolve(path).normalize();
        }

        return targetPath;
    }

    /**
     * Guards a path parameter, returning the validated path or throwing.
     *
     * @param paramName the parameter name
     * @param paramValue the parameter value
     * @return the validated path
     * @throws SecurityException if path is invalid
     */
    public Path guardPathParam(String paramName, String paramValue) {
        if (!pathParamKeys.contains(paramName)) {
            // Not a guarded parameter
            return Paths.get(paramValue);
        }

        PathValidationResult result = validatePath(paramValue);
        if (!result.isValid()) {
            throw new SecurityException("Path validation failed for parameter '" + paramName + "': " + result.getError());
        }

        return result.getPath();
    }

    /**
     * Normalizes a path parameter if normalization is enabled.
     *
     * @param path the path to normalize
     * @return the normalized path
     */
    public String normalizePathParam(String path) {
        if (!normalizeGuardedPathParams) {
            return path;
        }

        try {
            Path resolved = resolvePath(path);
            Path relative = workspaceRoot.relativize(resolved);
            return relative.toString();
        } catch (Exception e) {
            // Return original if normalization fails
            return path;
        }
    }

    /**
     * Gets the workspace root.
     *
     * @return the workspace root
     */
    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    /**
     * Path validation result.
     */
    public static class PathValidationResult {
        private final boolean valid;
        private final Path path;
        private final String error;

        private PathValidationResult(boolean valid, Path path, String error) {
            this.valid = valid;
            this.path = path;
            this.error = error;
        }

        /**
         * Creates a valid result.
         *
         * @param path the validated path
         * @return the result
         */
        public static PathValidationResult valid(Path path) {
            return new PathValidationResult(true, path, null);
        }

        /**
         * Creates an invalid result.
         *
         * @param error the error message
         * @return the result
         */
        public static PathValidationResult invalid(String error) {
            return new PathValidationResult(false, null, error);
        }

        /**
         * Checks if the path is valid.
         *
         * @return true if valid
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Gets the validated path.
         *
         * @return the path, or null if invalid
         */
        public Path getPath() {
            return path;
        }

        /**
         * Gets the error message.
         *
         * @return the error, or null if valid
         */
        public String getError() {
            return error;
        }
    }

    /**
     * Exception thrown when path validation fails.
     */
    public static class PathValidationException extends SecurityException {
        public PathValidationException(String message) {
            super(message);
        }
    }
}
