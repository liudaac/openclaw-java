package openclaw.security;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SSH sandbox security for preventing path traversal attacks.
 */
public class SshSandboxSecurity {

    /**
     * Validate upload path to prevent escaping sandbox.
     */
    public void validateUploadPath(String path, String baseDir) throws SecurityException {
        if (path == null || path.isBlank()) {
            throw new SecurityException("Path is null or empty");
        }
        if (baseDir == null || baseDir.isBlank()) {
            throw new SecurityException("Base directory is null or empty");
        }

        Path normalizedPath = Paths.get(baseDir, path).normalize();
        Path normalizedBase = Paths.get(baseDir).normalize();

        // Check if path escapes base directory
        if (!normalizedPath.startsWith(normalizedBase)) {
            throw new SecurityException("Path escapes sandbox: " + path);
        }

        // Check for symbolic link traversal
        if (path.contains("..") || path.contains("~")) {
            throw new SecurityException("Path contains traversal characters: " + path);
        }
    }

    /**
     * Check if path is within sandbox.
     */
    public boolean isWithinSandbox(String path, String baseDir) {
        try {
            validateUploadPath(path, baseDir);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
