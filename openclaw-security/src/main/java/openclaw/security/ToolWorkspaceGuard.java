package openclaw.security;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Tool workspace guard for wrapping tools with workspace boundary protection.
 * <p>
 * This class wraps an existing tool to add workspace path validation for specified
 * path parameters, preventing the tool from writing outside the workspace.
 * <p>
 * Based on TypeScript implementation: src/agents/pi-tools.read.ts (wrapToolWorkspaceRootGuardWithOptions)
 *
 * @author OpenClaw Team
 * @version 2026.4.11
 */
public class ToolWorkspaceGuard implements AgentTool {

    private final AgentTool wrappedTool;
    private final WorkspacePathGuard pathGuard;
    private final Set<String> guardedParamKeys;

    /**
     * Creates a tool workspace guard.
     *
     * @param wrappedTool the tool to wrap
     * @param workspaceRoot the workspace root directory
     */
    public ToolWorkspaceGuard(AgentTool wrappedTool, Path workspaceRoot) {
        this(wrappedTool, workspaceRoot, null, Set.of("outPath", "path", "filePath"));
    }

    /**
     * Creates a tool workspace guard with options.
     *
     * @param wrappedTool the tool to wrap
     * @param workspaceRoot the workspace root directory
     * @param containerWorkdir the container working directory (for sandboxed environments)
     * @param guardedParamKeys the parameter keys to guard
     */
    public ToolWorkspaceGuard(
            AgentTool wrappedTool,
            Path workspaceRoot,
            Path containerWorkdir,
            Set<String> guardedParamKeys) {
        this.wrappedTool = wrappedTool;
        this.pathGuard = new WorkspacePathGuard(
                workspaceRoot,
                containerWorkdir,
                true,
                guardedParamKeys
        );
        this.guardedParamKeys = guardedParamKeys != null ? guardedParamKeys : Set.of();
    }

    @Override
    public String getName() {
        return wrappedTool.getName();
    }

    @Override
    public String getDescription() {
        return wrappedTool.getDescription();
    }

    @Override
    public ToolParameters getParameters() {
        return wrappedTool.getParameters();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        // Validate and guard path parameters
        Map<String, Object> originalArgs = context.arguments();
        Map<String, Object> guardedArgs = new java.util.HashMap<>(originalArgs);

        for (String paramKey : guardedParamKeys) {
            if (guardedArgs.containsKey(paramKey)) {
                Object value = guardedArgs.get(paramKey);
                if (value instanceof String pathValue) {
                    WorkspacePathGuard.PathValidationResult result = pathGuard.validatePath(pathValue);
                    if (!result.isValid()) {
                        return CompletableFuture.completedFuture(
                                ToolResult.failure(
                                        "SECURITY: Path validation failed for parameter '" + paramKey + "': " + result.getError()
                                )
                        );
                    }

                    // Normalize the path parameter
                    String normalizedPath = pathGuard.normalizePathParam(pathValue);
                    guardedArgs.put(paramKey, normalizedPath);
                }
            }
        }

        // Create new context with guarded arguments
        ToolExecuteContext guardedContext = ToolExecuteContext.builder()
                .toolName(context.toolName())
                .arguments(guardedArgs)
                .config(context.config().orElse(null))
                .workspaceDir(context.workspaceDir().orElse(null))
                .agentDir(context.agentDir().orElse(null))
                .agentId(context.agentId().orElse(null))
                .sessionKey(context.sessionKey().orElse(null))
                .sessionId(context.sessionId().orElse(null))
                .messageChannel(context.messageChannel().orElse(null))
                .agentAccountId(context.agentAccountId().orElse(null))
                .requesterSenderId(context.requesterSenderId().orElse(null))
                .senderIsOwner(context.senderIsOwner())
                .sandboxed(context.sandboxed())
                .build();

        // Execute the wrapped tool with guarded arguments
        return wrappedTool.execute(guardedContext);
    }

    /**
     * Creates a guarded tool if workspace-only policy is enabled.
     *
     * @param tool the tool to guard
     * @param fsPolicy the file system policy
     * @param workspaceDir the workspace directory
     * @return the guarded tool or the original tool
     */
    public static AgentTool applyGuard(
            AgentTool tool,
            ToolFsPolicy fsPolicy,
            Path workspaceDir) {
        if (fsPolicy == null || !fsPolicy.isWorkspaceOnly()) {
            return tool;
        }

        Path sandboxRoot = fsPolicy.getSandboxRoot() != null
                ? fsPolicy.getSandboxRoot()
                : workspaceDir;

        return new ToolWorkspaceGuard(
                tool,
                sandboxRoot,
                fsPolicy.getContainerWorkdir(),
                fsPolicy.getGuardedPathKeys()
        );
    }

    /**
     * File system policy for tool guarding.
     */
    public static class ToolFsPolicy {
        private final boolean workspaceOnly;
        private final Path sandboxRoot;
        private final Path containerWorkdir;
        private final Set<String> guardedPathKeys;

        /**
         * Creates a file system policy.
         *
         * @param workspaceOnly whether to restrict to workspace only
         * @param sandboxRoot the sandbox root directory
         * @param containerWorkdir the container working directory
         * @param guardedPathKeys the path keys to guard
         */
        public ToolFsPolicy(
                boolean workspaceOnly,
                Path sandboxRoot,
                Path containerWorkdir,
                Set<String> guardedPathKeys) {
            this.workspaceOnly = workspaceOnly;
            this.sandboxRoot = sandboxRoot;
            this.containerWorkdir = containerWorkdir;
            this.guardedPathKeys = guardedPathKeys != null ? guardedPathKeys : Set.of("outPath", "path", "filePath");
        }

        /**
         * Creates a workspace-only policy.
         *
         * @param workspaceRoot the workspace root
         * @return the policy
         */
        public static ToolFsPolicy workspaceOnly(Path workspaceRoot) {
            return new ToolFsPolicy(true, workspaceRoot, null, Set.of("outPath", "path", "filePath"));
        }

        /**
         * Creates a permissive policy (no restrictions).
         *
         * @return the policy
         */
        public static ToolFsPolicy permissive() {
            return new ToolFsPolicy(false, null, null, Set.of());
        }

        public boolean isWorkspaceOnly() {
            return workspaceOnly;
        }

        public Path getSandboxRoot() {
            return sandboxRoot;
        }

        public Path getContainerWorkdir() {
            return containerWorkdir;
        }

        public Set<String> getGuardedPathKeys() {
            return guardedPathKeys;
        }
    }
}
