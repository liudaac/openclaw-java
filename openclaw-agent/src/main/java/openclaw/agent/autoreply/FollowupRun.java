package openclaw.agent.autoreply;

import openclaw.agent.config.AgentConfig;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a followup run in the queue.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public record FollowupRun(
        String prompt,
        String messageId,
        String summaryLine,
        Instant enqueuedAt,
        String originatingChannel,
        String originatingTo,
        String originatingAccountId,
        String originatingThreadId,
        String originatingChatType,
        FollowupRunContext run
) {
    /**
     * Context for the followup run.
     */
    public record FollowupRunContext(
            String agentId,
            String agentDir,
            String sessionId,
            String sessionKey,
            String messageProvider,
            String agentAccountId,
            String groupId,
            String groupChannel,
            String groupSpace,
            String senderId,
            String senderName,
            String senderUsername,
            String senderE164,
            boolean senderIsOwner,
            String sessionFile,
            String workspaceDir,
            AgentConfig config,
            Map<String, Object> skillsSnapshot,
            String provider,
            String model,
            String thinkLevel,
            String verboseLevel,
            String reasoningLevel,
            int timeoutMs,
            String blockReplyBreak,
            String[] ownerNumbers,
            String extraSystemPrompt,
            boolean enforceFinalTag
    ) {}
}
