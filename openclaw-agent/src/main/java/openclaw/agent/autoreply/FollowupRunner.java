package openclaw.agent.autoreply;

import openclaw.agent.config.AgentConfig;
import openclaw.agent.event.AgentEventEmitter;
import openclaw.agent.event.AgentEventPayload;
import openclaw.agent.event.AgentEventStream;
import openclaw.agent.event.AgentRunContext;
import openclaw.agent.heartbeat.HeartbeatProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runner for followup (auto-reply) operations.
 *
 * <p>This class handles the execution of followup runs, including:
 * <ul>
 *   <li>Agent execution with fallback support</li>
 *   <li>Reply payload processing and routing</li>
 *   <li>Context compaction handling</li>
 *   <li>Heartbeat token stripping</li>
 *   <li>Typing signal management</li>
 * </ul>
 * </p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class FollowupRunner {

    private static final Logger logger = LoggerFactory.getLogger(FollowupRunner.class);

    private final FollowupRunnerOptions options;
    private final AgentEventEmitter eventEmitter;
    private final HeartbeatProcessor heartbeatProcessor;
    private final TypingController typingController;

    public FollowupRunner(
            FollowupRunnerOptions options,
            AgentEventEmitter eventEmitter,
            HeartbeatProcessor heartbeatProcessor,
            TypingController typingController) {
        this.options = options;
        this.eventEmitter = eventEmitter;
        this.heartbeatProcessor = heartbeatProcessor;
        this.typingController = typingController;
    }

    /**
     * Executes a followup run.
     *
     * @param queued the queued followup run
     * @return CompletableFuture that completes when the run is finished
     */
    public CompletableFuture<Void> run(FollowupRun queued) {
        return CompletableFuture.runAsync(() -> {
            String runId = UUID.randomUUID().toString();
            logger.info("Starting followup run: {} for session: {}", 
                    runId, queued.run().sessionKey());

            try {
                // Register run context
                if (queued.run().sessionKey() != null) {
                    AgentRunContext context = AgentRunContext.builder()
                            .sessionKey(queued.run().sessionKey())
                            .verboseLevel(queued.run().verboseLevel())
                            .controlUiVisible(shouldSurfaceToControlUi(queued))
                            .build();
                    eventEmitter.registerRunContext(runId, context);
                }

                // Setup compaction notice handler
                Consumer<String> sendCompactionNotice = (text) -> {
                    List<ReplyPayload> noticePayloads = applyReplyThreading(
                            List.of(ReplyPayload.compactionNotice(text)),
                            queued
                    );
                    if (!noticePayloads.isEmpty()) {
                        try {
                            sendFollowupPayloads(noticePayloads, queued);
                        } catch (Exception e) {
                            logger.warn("Failed to send compaction notice: {}", e.getMessage());
                        }
                    }
                };

                // Track compaction count
                int[] autoCompactionCount = {0};

                // Subscribe to agent events for compaction
                Runnable unsubscribe = eventEmitter.subscribe(evt -> {
                    if (evt.getStream() != AgentEventStream.LIFECYCLE) {
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) evt.getData().get("data");
                    if (data == null) {
                        return;
                    }
                    String phase = (String) data.get("phase");
                    if ("start".equals(phase)) {
                        sendCompactionNotice.accept("🧹 Compacting context...");
                    }
                    if ("end".equals(phase) && Boolean.TRUE.equals(data.get("completed"))) {
                        autoCompactionCount[0]++;
                    }
                });

                try {
                    // Execute agent run
                    AgentRunResult result = executeAgentRun(queued, runId);

                    // Process payloads
                    List<ReplyPayload> payloads = processPayloads(result, autoCompactionCount[0], queued);

                    // Send final payloads
                    if (!payloads.isEmpty()) {
                        sendFollowupPayloads(payloads, queued);
                    }

                } finally {
                    unsubscribe.run();
                }

            } catch (Exception e) {
                logger.error("Followup run failed: {}", e.getMessage(), e);
            } finally {
                // Cleanup
                typingController.markRunComplete();
                typingController.markDispatchIdle();
                eventEmitter.clearRunContext(runId);
            }
        });
    }

    /**
     * Executes the agent run.
     */
    private AgentRunResult executeAgentRun(FollowupRun queued, String runId) {
        // This would integrate with the actual agent execution
        // For now, return a placeholder result
        logger.debug("Executing agent run: {} with prompt: {}", runId, 
                queued.prompt().substring(0, Math.min(50, queued.prompt().length())));

        return new AgentRunResult(
                List.of(),
                Map.of(),
                0,
                0,
                null
        );
    }

    /**
     * Processes the agent run result into reply payloads.
     */
    private List<ReplyPayload> processPayloads(
            AgentRunResult result,
            int compactionCount,
            FollowupRun queued) {

        List<ReplyPayload> payloads = new ArrayList<>();

        // Sanitize payloads - strip heartbeat tokens
        for (ReplyPayload payload : result.payloads()) {
            String text = payload.text();
            if (text == null || !text.contains("HEARTBEAT_OK")) {
                payloads.add(payload);
                continue;
            }

            HeartbeatProcessor.StripResult stripResult = heartbeatProcessor.stripHeartbeatToken(
                    text, HeartbeatProcessor.StripMode.MESSAGE);

            boolean hasMedia = payload.media() != null && !payload.media().isEmpty();
            if (stripResult.shouldSkip() && !hasMedia) {
                continue;
            }

            payloads.add(new ReplyPayload(
                    stripResult.text(),
                    payload.replyToCurrent(),
                    payload.replyToMessageId(),
                    payload.isCompactionNotice(),
                    payload.media(),
                    payload.metadata()
            ));
        }

        // Apply reply threading
        payloads = applyReplyThreading(payloads, queued);

        // Filter duplicates
        payloads = filterDuplicates(result, result);

        // Add compaction completion notice
        if (compactionCount > 0) {
            String completionText = isVerbose(queued)
                    ? "🧹 Auto-compaction complete (count " + compactionCount + ")."
                    : "✅ Context compacted (count " + compactionCount + ").";

            List<ReplyPayload> compactionPayloads = applyReplyThreading(
                    List.of(ReplyPayload.compactionNotice(completionText)),
                    queued
            );

            List<ReplyPayload> combined = new ArrayList<>();
            combined.addAll(compactionPayloads);
            combined.addAll(payloads);
            payloads = combined;
        }

        return payloads;
    }

    /**
     * Sends followup payloads to the appropriate destination.
     */
    private void sendFollowupPayloads(List<ReplyPayload> payloads, FollowupRun queued) {
        for (ReplyPayload payload : payloads) {
            if (!payload.hasContent()) {
                continue;
            }

            // Check for silent reply
            if (heartbeatProcessor.isSilentReplyText(payload.text()) &&
                    (payload.media() == null || payload.media().isEmpty())) {
                continue;
            }

            // Signal typing
            typingController.signalTextDelta(payload.text());

            // Send via appropriate handler
            if (options.onBlockReply() != null) {
                options.onBlockReply().accept(payload);
            }
        }
    }

    /**
     * Applies reply threading to payloads.
     */
    private List<ReplyPayload> applyReplyThreading(List<ReplyPayload> payloads, FollowupRun queued) {
        // Determine reply-to mode based on channel and config
        ReplyToMode replyToMode = resolveReplyToMode(queued);

        return payloads.stream()
                .map(payload -> {
                    if (!payload.replyToCurrent()) {
                        return payload;
                    }

                    // Apply threading based on mode
                    String replyToMessageId = switch (replyToMode) {
                        case CURRENT -> queued.messageId();
                        case THREAD -> queued.originatingThreadId();
                        case NONE -> null;
                    };

                    return new ReplyPayload(
                            payload.text(),
                            payload.replyToCurrent(),
                            replyToMessageId,
                            payload.isCompactionNotice(),
                            payload.media(),
                            payload.metadata()
                    );
                })
                .toList();
    }

    /**
     * Filters duplicate payloads.
     */
    private List<ReplyPayload> filterDuplicates(AgentRunResult result, AgentRunResult runResult) {
        // Filter messaging tool duplicates
        Set<String> sentTexts = new HashSet<>(
                runResult.messagingToolSentTexts() != null
                        ? runResult.messagingToolSentTexts()
                        : List.of()
        );

        return result.payloads().stream()
                .filter(payload -> payload.text() == null || !sentTexts.contains(payload.text()))
                .toList();
    }

    /**
     * Resolves the reply-to mode.
     */
    private ReplyToMode resolveReplyToMode(FollowupRun queued) {
        AgentConfig config = queued.run().config();
        String channel = queued.originatingChannel();

        // Check config for channel-specific settings
        if (config != null && channel != null) {
            // This would check config for reply threading settings
            // For now, use defaults
        }

        // Default based on channel type
        if (channel == null) {
            return ReplyToMode.CURRENT;
        }

        return switch (channel.toLowerCase()) {
            case "telegram", "discord", "slack" -> ReplyToMode.THREAD;
            default -> ReplyToMode.CURRENT;
        };
    }

    /**
     * Checks if should surface to control UI.
     */
    private boolean shouldSurfaceToControlUi(FollowupRun queued) {
        String provider = queued.run().messageProvider();
        if (provider == null) {
            return true;
        }

        // Internal channels should not surface to control UI
        return !provider.startsWith("internal:");
    }

    /**
     * Checks if verbose mode is enabled.
     */
    private boolean isVerbose(FollowupRun queued) {
        String verboseLevel = queued.run().verboseLevel();
        return verboseLevel != null && !"off".equals(verboseLevel);
    }

    /**
     * Result of an agent run.
     */
    public record AgentRunResult(
            List<ReplyPayload> payloads,
            Map<String, Object> meta,
            int promptTokens,
            int completionTokens,
            String model
    ) {
        public List<String> messagingToolSentTexts() {
            @SuppressWarnings("unchecked")
            List<String> texts = meta != null
                    ? (List<String>) meta.get("messagingToolSentTexts")
                    : null;
            return texts != null ? texts : List.of();
        }
    }

    /**
     * Reply-to mode for threading.
     */
    public enum ReplyToMode {
        CURRENT,
        THREAD,
        NONE
    }

    /**
     * Controller for typing indicators.
     */
    public interface TypingController {
        void signalTextDelta(String text);
        void markRunComplete();
        void markDispatchIdle();
    }
}
