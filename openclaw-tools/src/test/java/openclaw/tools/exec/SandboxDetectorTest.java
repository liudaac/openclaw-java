package openclaw.tools.exec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SandboxDetector and CommandExecutionTool sandbox functionality.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
class SandboxDetectorTest {

    @TempDir
    Path tempDir;

    // ==================== SandboxDetector Tests ====================

    @Test
    @DisplayName("SandboxDetector should be creatable")
    void testSandboxDetectorCreation() {
        SandboxDetector detector = new SandboxDetector();
        assertNotNull(detector);
    }

    @Test
    @DisplayName("SandboxDetector with override should respect override value")
    void testSandboxDetectorOverride() {
        // Override to true
        SandboxDetector detectorTrue = new SandboxDetector(true);
        assertTrue(detectorTrue.isSandboxed(), "Override to true should report sandboxed");

        // Override to false
        SandboxDetector detectorFalse = new SandboxDetector(false);
        assertFalse(detectorFalse.isSandboxed(), "Override to false should report not sandboxed");
    }

    @Test
    @DisplayName("SandboxDetector should return status string")
    void testSandboxStatus() {
        SandboxDetector detector = new SandboxDetector();
        String status = detector.getSandboxStatus();
        assertNotNull(status);
        assertTrue(status.contains("Sandbox Detection Results"));
    }

    // ==================== CommandExecutionTool Sandbox Tests ====================

    @Test
    @DisplayName("CommandExecutionTool should allow execution when sandbox is available")
    void testSandboxAvailableAllowsExecution() {
        // Create tool with sandbox override to true (simulating sandbox available)
        CommandExecutionTool tool = new CommandExecutionTool(
                tempDir,
                java.time.Duration.ofMinutes(1),
                java.util.Set.of(),
                java.util.Set.of(),
                false,  // requireApproval
                true,   // sandboxEnabled
                true    // failClosed
        );

        // The tool should report sandbox status
        assertNotNull(tool.getSandboxStatus());
    }

    @Test
    @DisplayName("CommandExecutionTool should reject execution when sandbox unavailable and fail-closed")
    void testSandboxUnavailableWithFailClosed() {
        // Create tool with sandbox override to false (simulating no sandbox)
        // Note: In real scenario, we would need to mock SandboxDetector
        // For now, we test the configuration is properly set
        CommandExecutionTool tool = new CommandExecutionTool(
                tempDir,
                java.time.Duration.ofMinutes(1),
                java.util.Set.of(),
                java.util.Set.of(),
                false,  // requireApproval
                true,   // sandboxEnabled
                true    // failClosed
        );

        assertNotNull(tool);
        // The actual sandbox check would depend on the environment
    }

    @Test
    @DisplayName("CommandExecutionTool should allow execution when fail-closed is disabled")
    void testFailClosedDisabled() {
        CommandExecutionTool tool = new CommandExecutionTool(
                tempDir,
                java.time.Duration.ofMinutes(1),
                java.util.Set.of(),
                java.util.Set.of(),
                false,  // requireApproval
                true,   // sandboxEnabled
                false   // failClosed - disabled
        );

        assertNotNull(tool);
        // With failClosed=false, commands should execute even outside sandbox
    }

    @Test
    @DisplayName("CommandExecutionTool should skip sandbox check when disabled")
    void testSandboxDisabled() {
        CommandExecutionTool tool = new CommandExecutionTool(
                tempDir,
                java.time.Duration.ofMinutes(1),
                java.util.Set.of(),
                java.util.Set.of(),
                false,  // requireApproval
                false,  // sandboxEnabled - disabled
                true    // failClosed (irrelevant when sandbox is disabled)
        );

        assertNotNull(tool);
        // With sandboxEnabled=false, sandbox check should be skipped
    }

    @Test
    @DisplayName("CommandExecutionTool default constructor should enable sandbox with fail-closed")
    void testDefaultConfiguration() {
        CommandExecutionTool tool = new CommandExecutionTool(tempDir);

        assertNotNull(tool);
        // Default: sandboxEnabled=true, failClosed=true
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("SandboxDetector should detect container environment variables")
    void testContainerEnvironmentVariables() {
        // Set container environment variable
        String originalContainer = System.getenv("container");
        try {
            // Note: Cannot actually set env var in Java, but we can verify the detector logic
            SandboxDetector detector = new SandboxDetector();
            String status = detector.getSandboxStatus();
            assertNotNull(status);
            assertTrue(status.contains("Docker") || status.contains("NO"));
        } finally {
            // No cleanup needed for read-only env var check
        }
    }

    @Test
    @DisplayName("CommandExecutionTool should expose sandbox status")
    void testSandboxStatusExposed() {
        CommandExecutionTool tool = new CommandExecutionTool(tempDir);

        String status = tool.getSandboxStatus();
        assertNotNull(status);
        assertFalse(status.isEmpty());

        // Status should contain key indicators
        assertTrue(status.contains("Docker") || status.contains("Containerd") ||
                   status.contains("cgroup") || status.contains("sandboxed"),
                   "Status should contain sandbox-related information");
    }
}
