package openclaw.agent.config;

import java.util.Map;
import java.util.Optional;

/**
 * Agent configuration.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
public record AgentConfig(
        String agentId,
        String agentDir,
        String sessionFile,
        String workspaceDir,
        String provider,
        String model,
        String thinkLevel,
        String verboseLevel,
        String reasoningLevel,
        int timeoutMs,
        String blockReplyBreak,
        String[] ownerNumbers,
        String extraSystemPrompt,
        boolean enforceFinalTag,
        Map<String, Object> additionalConfig
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String agentId;
        private String agentDir;
        private String sessionFile;
        private String workspaceDir;
        private String provider = "openai";
        private String model = "gpt-4";
        private String thinkLevel = "off";
        private String verboseLevel = "off";
        private String reasoningLevel = "off";
        private int timeoutMs = 60000;
        private String blockReplyBreak;
        private String[] ownerNumbers = new String[0];
        private String extraSystemPrompt;
        private boolean enforceFinalTag = false;
        private Map<String, Object> additionalConfig = Map.of();

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder agentDir(String agentDir) {
            this.agentDir = agentDir;
            return this;
        }

        public Builder sessionFile(String sessionFile) {
            this.sessionFile = sessionFile;
            return this;
        }

        public Builder workspaceDir(String workspaceDir) {
            this.workspaceDir = workspaceDir;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder thinkLevel(String thinkLevel) {
            this.thinkLevel = thinkLevel;
            return this;
        }

        public Builder verboseLevel(String verboseLevel) {
            this.verboseLevel = verboseLevel;
            return this;
        }

        public Builder reasoningLevel(String reasoningLevel) {
            this.reasoningLevel = reasoningLevel;
            return this;
        }

        public Builder timeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder blockReplyBreak(String blockReplyBreak) {
            this.blockReplyBreak = blockReplyBreak;
            return this;
        }

        public Builder ownerNumbers(String[] ownerNumbers) {
            this.ownerNumbers = ownerNumbers != null ? ownerNumbers : new String[0];
            return this;
        }

        public Builder extraSystemPrompt(String extraSystemPrompt) {
            this.extraSystemPrompt = extraSystemPrompt;
            return this;
        }

        public Builder enforceFinalTag(boolean enforceFinalTag) {
            this.enforceFinalTag = enforceFinalTag;
            return this;
        }

        public Builder additionalConfig(Map<String, Object> additionalConfig) {
            this.additionalConfig = additionalConfig != null ? additionalConfig : Map.of();
            return this;
        }

        public AgentConfig build() {
            return new AgentConfig(
                    agentId,
                    agentDir,
                    sessionFile,
                    workspaceDir,
                    provider,
                    model,
                    thinkLevel,
                    verboseLevel,
                    reasoningLevel,
                    timeoutMs,
                    blockReplyBreak,
                    ownerNumbers,
                    extraSystemPrompt,
                    enforceFinalTag,
                    additionalConfig
            );
        }
    }
}
