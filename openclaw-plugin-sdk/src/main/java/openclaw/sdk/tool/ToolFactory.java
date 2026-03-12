package openclaw.sdk.tool;

import openclaw.sdk.core.OpenClawConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tool factory for creating agent tools.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
@FunctionalInterface
public interface ToolFactory {

    /**
     * Creates tools for the given context.
     *
     * @param context the tool context
     * @return list of tools
     */
    List<AgentTool> create(ToolContext context);

    /**
     * Tool context.
     *
     * @param config the OpenClaw config
     * @param workspaceDir the workspace directory
     * @param agentDir the agent directory
     * @param agentId the agent ID
     * @param sessionKey the session key
     * @param sessionId the session ID
     * @param messageChannel the message channel
     * @param agentAccountId the agent account ID
     * @param requesterSenderId the requester sender ID
     * @param senderIsOwner whether the sender is an owner
     * @param sandboxed whether running in sandbox
     */
    record ToolContext(
            Optional<OpenClawConfig> config,
            Optional<Path> workspaceDir,
            Optional<Path> agentDir,
            Optional<String> agentId,
            Optional<String> sessionKey,
            Optional<String> sessionId,
            Optional<String> messageChannel,
            Optional<String> agentAccountId,
            Optional<String> requesterSenderId,
            boolean senderIsOwner,
            boolean sandboxed
    ) {

        /**
         * Creates a builder for ToolContext.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for ToolContext.
         */
        public static class Builder {
            private OpenClawConfig config;
            private Path workspaceDir;
            private Path agentDir;
            private String agentId;
            private String sessionKey;
            private String sessionId;
            private String messageChannel;
            private String agentAccountId;
            private String requesterSenderId;
            private boolean senderIsOwner = false;
            private boolean sandboxed = false;

            public Builder config(OpenClawConfig config) {
                this.config = config;
                return this;
            }

            public Builder workspaceDir(Path workspaceDir) {
                this.workspaceDir = workspaceDir;
                return this;
            }

            public Builder agentDir(Path agentDir) {
                this.agentDir = agentDir;
                return this;
            }

            public Builder agentId(String agentId) {
                this.agentId = agentId;
                return this;
            }

            public Builder sessionKey(String sessionKey) {
                this.sessionKey = sessionKey;
                return this;
            }

            public Builder sessionId(String sessionId) {
                this.sessionId = sessionId;
                return this;
            }

            public Builder messageChannel(String messageChannel) {
                this.messageChannel = messageChannel;
                return this;
            }

            public Builder agentAccountId(String agentAccountId) {
                this.agentAccountId = agentAccountId;
                return this;
            }

            public Builder requesterSenderId(String requesterSenderId) {
                this.requesterSenderId = requesterSenderId;
                return this;
            }

            public Builder senderIsOwner(boolean senderIsOwner) {
                this.senderIsOwner = senderIsOwner;
                return this;
            }

            public Builder sandboxed(boolean sandboxed) {
                this.sandboxed = sandboxed;
                return this;
            }

            public ToolContext build() {
                return new ToolContext(
                        Optional.ofNullable(config),
                        Optional.ofNullable(workspaceDir),
                        Optional.ofNullable(agentDir),
                        Optional.ofNullable(agentId),
                        Optional.ofNullable(sessionKey),
                        Optional.ofNullable(sessionId),
                        Optional.ofNullable(messageChannel),
                        Optional.ofNullable(agentAccountId),
                        Optional.ofNullable(requesterSenderId),
                        senderIsOwner,
                        sandboxed
                );
            }
        }
    }
}
