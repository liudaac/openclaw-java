package openclaw.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WorkspacePathGuard}.
 *
 * @author OpenClaw Team
 * @version 2026.4.11
 */
class WorkspacePathGuardTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAllowPathWithinWorkspace() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir);

        WorkspacePathGuard.PathValidationResult result = guard.validatePath("file.txt");

        assertTrue(result.isValid());
        assertNotNull(result.getPath());
        assertEquals(tempDir.resolve("file.txt").toAbsolutePath().normalize(), result.getPath());
    }

    @Test
    void shouldAllowNestedPathWithinWorkspace() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir);

        WorkspacePathGuard.PathValidationResult result = guard.validatePath("subdir/nested/file.txt");

        assertTrue(result.isValid());
        assertNotNull(result.getPath());
    }

    @Test
    void shouldBlockPathEscapingWorkspace() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir);

        WorkspacePathGuard.PathValidationResult result = guard.validatePath("../escape.txt");

        assertFalse(result.isValid());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("escapes workspace"));
    }

    @Test
    void shouldBlockAbsolutePathOutsideWorkspace() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir);

        WorkspacePathGuard.PathValidationResult result = guard.validatePath("/etc/passwd");

        assertFalse(result.isValid());
    }

    @Test
    void shouldBlockPathWithNull() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir);

        WorkspacePathGuard.PathValidationResult result = guard.validatePath(null);

        assertFalse(result.isValid());
        assertTrue(result.getError().contains("null"));
    }

    @Test
    void shouldBlockPathWithEmptyString() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir);

        WorkspacePathGuard.PathValidationResult result = guard.validatePath("");

        assertFalse(result.isValid());
    }

    @Test
    void shouldHandleContainerPathMapping() {
        Path workspaceRoot = Paths.get("/host/workspace").toAbsolutePath().normalize();
        Path containerWorkdir = Paths.get("/app/workspace").toAbsolutePath().normalize();

        WorkspacePathGuard guard = new WorkspacePathGuard(
                workspaceRoot,
                containerWorkdir,
                true,
                Set.of("outPath")
        );

        // Container absolute path should be mapped to host workspace
        Path resolved = guard.resolvePath("/app/workspace/output.txt");

        assertEquals(workspaceRoot.resolve("output.txt").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void shouldRejectContainerPathOutsideWorkdir() {
        Path workspaceRoot = Paths.get("/host/workspace").toAbsolutePath().normalize();
        Path containerWorkdir = Paths.get("/app/workspace").toAbsolutePath().normalize();

        WorkspacePathGuard guard = new WorkspacePathGuard(
                workspaceRoot,
                containerWorkdir,
                true,
                Set.of("outPath")
        );

        assertThrows(SecurityException.class, () -> {
            guard.resolvePath("/etc/passwd");
        });
    }

    @Test
    void shouldNormalizePathParam() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir);

        String normalized = guard.normalizePathParam("subdir/file.txt");

        assertEquals("subdir/file.txt", normalized);
    }

    @Test
    void shouldGuardPathParam() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir, null, true, Set.of("outPath", "path"));

        // Valid path
        Path validPath = guard.guardPathParam("outPath", "file.txt");
        assertNotNull(validPath);

        // Invalid path
        assertThrows(SecurityException.class, () -> {
            guard.guardPathParam("outPath", "../escape.txt");
        });

        // Unguarded parameter
        Path unguarded = guard.guardPathParam("otherParam", "../any.txt");
        assertNotNull(unguarded);
    }

    @Test
    void shouldHandleComplexTraversalAttempts() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir);

        // Various traversal attempts
        assertFalse(guard.validatePath("../../etc/passwd").isValid());
        assertFalse(guard.validatePath("subdir/../../../etc/passwd").isValid());
        assertFalse(guard.validatePath("./../escape.txt").isValid());
        assertFalse(guard.validatePath("subdir/../../../../escape.txt").isValid());
    }

    @Test
    void shouldAllowValidDotPaths() {
        WorkspacePathGuard guard = new WorkspacePathGuard(tempDir);

        assertTrue(guard.validatePath("./file.txt").isValid());
        assertTrue(guard.validatePath("subdir/./file.txt").isValid());
        assertTrue(guard.validatePath("subdir/../other/file.txt").isValid());
    }
}
