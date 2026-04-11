package openclaw.security;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ToolWorkspaceGuard}.
 *
 * @author OpenClaw Team
 * @version 2026.4.11
 */
class ToolWorkspaceGuardTest {

    @TempDir
    Path tempDir;

    /**
     * Mock tool for testing.
     */
    static class MockTool implements AgentTool {
        private final String name;
        private Map<String, Object> lastArguments;

        MockTool(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "Mock tool for testing";
        }

        @Override
        public ToolParameters getParameters() {
            return ToolParameters.builder()
                    .properties(Map.of(
                            "outPath", PropertySchema.string("Output file path"),
                            "content", PropertySchema.string("Content to write")
                    ))
                    .required(java.util.List.of("outPath"))
                    .build();
        }

        @Override
        public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
            this.lastArguments = context.arguments();
            return CompletableFuture.completedFuture(ToolResult.success("Executed"));
        }

        Map<String, Object> getLastArguments() {
            return lastArguments;
        }
    }

    @Test
    void shouldAllowValidPath() {
        MockTool mockTool = new MockTool("testTool");
        ToolWorkspaceGuard guard = new ToolWorkspaceGuard(mockTool, tempDir);

        ToolExecuteContext context = ToolExecuteContext.builder()
                .toolName("testTool")
                .arguments(Map.of("outPath", "output.txt", "content", "test"))
                .workspaceDir(tempDir)
                .build();

        CompletableFuture<ToolResult> result = guard.execute(context);

        assertFalse(result.isCompletedExceptionally());
        ToolResult toolResult = result.join();
        assertTrue(toolResult.success());
    }

    @Test
    void shouldBlockPathEscapingWorkspace() {
        MockTool mockTool = new MockTool("testTool");
        ToolWorkspaceGuard guard = new ToolWorkspaceGuard(mockTool, tempDir);

        ToolExecuteContext context = ToolExecuteContext.builder()
                .toolName("testTool")
                .arguments(Map.of("outPath", "../escape.txt", "content", "test"))
                .workspaceDir(tempDir)
                .build();

        CompletableFuture<ToolResult> result = guard.execute(context);

        ToolResult toolResult = result.join();
        assertFalse(toolResult.success());
        assertTrue(toolResult.error().isPresent());
        assertTrue(toolResult.error().get().contains("SECURITY"));
        assertTrue(toolResult.error().get().contains("escapes workspace"));
    }

    @Test
    void shouldApplyPolicyToCreateGuardedTool() {
        MockTool mockTool = new MockTool("testTool");

        // With workspace-only policy
        ToolWorkspaceGuard.ToolFsPolicy policy = ToolWorkspaceGuard.ToolFsPolicy.workspaceOnly(tempDir);
        AgentTool guardedTool = ToolWorkspaceGuard.applyGuard(mockTool, policy, tempDir);

        assertTrue(guardedTool instanceof ToolWorkspaceGuard);

        // With permissive policy
        ToolWorkspaceGuard.ToolFsPolicy permissivePolicy = ToolWorkspaceGuard.ToolFsPolicy.permissive();
        AgentTool unguardedTool = ToolWorkspaceGuard.applyGuard(mockTool, permissivePolicy, tempDir);

        assertSame(mockTool, unguardedTool);
    }

    @Test
    void shouldNormalizePathParameter() {
        MockTool mockTool = new MockTool("testTool");
        ToolWorkspaceGuard guard = new ToolWorkspaceGuard(mockTool, tempDir, null, Set.of("outPath"));

        ToolExecuteContext context = ToolExecuteContext.builder()
                .toolName("testTool")
                .arguments(Map.of("outPath", "./subdir/../output.txt", "content", "test"))
                .workspaceDir(tempDir)
                .build();

        CompletableFuture<ToolResult> result = guard.execute(context);

        assertTrue(result.join().success());
        // The path should be normalized in the arguments passed to the wrapped tool
        Map<String, Object> args = mockTool.getLastArguments();
        assertNotNull(args);
        String outPath = (String) args.get("outPath");
        assertNotNull(outPath);
        // Path should be normalized
        assertFalse(outPath.contains("./"));
        assertFalse(outPath.contains("../"));
    }

    @Test
    void shouldPreserveToolMetadata() {
        MockTool mockTool = new MockTool("testTool");
        ToolWorkspaceGuard guard = new ToolWorkspaceGuard(mockTool, tempDir);

        assertEquals("testTool", guard.getName());
        assertEquals("Mock tool for testing", guard.getDescription());
        assertNotNull(guard.getParameters());
    }

    @Test
    void shouldHandleNonStringPathParams() {
        MockTool mockTool = new MockTool("testTool");
        ToolWorkspaceGuard guard = new ToolWorkspaceGuard(mockTool, tempDir);

        // Pass a non-string value for outPath
        ToolExecuteContext context = ToolExecuteContext.builder()
                .toolName("testTool")
                .arguments(Map.of("outPath", 12345, "content", "test"))
                .workspaceDir(tempDir)
                .build();

        // Should not throw, just skip validation for non-string values
        CompletableFuture<ToolResult> result = guard.execute(context);
        assertTrue(result.join().success());
    }

    @Test
    void shouldHandleMissingPathParams() {
        MockTool mockTool = new MockTool("testTool");
        ToolWorkspaceGuard guard = new ToolWorkspaceGuard(mockTool, tempDir);

        // No outPath in arguments
        ToolExecuteContext context = ToolExecuteContext.builder()
                .toolName("testTool")
                .arguments(Map.of("content", "test"))
                .workspaceDir(tempDir)
                .build();

        CompletableFuture<ToolResult> result = guard.execute(context);
        assertTrue(result.join().success());
    }
}
