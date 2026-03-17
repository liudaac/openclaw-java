package openclaw.agent.heartbeat;

import openclaw.agent.AcpProtocol;
import openclaw.agent.AcpProtocol.AgentMessage;
import openclaw.agent.AcpProtocol.SpawnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Heartbeat service for triggering periodic agent tasks.
 *
 * <p>Monitors HEARTBEAT.md and triggers agent execution at configured intervals.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
@Service
public class HeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatService.class);

    // Default workspace directory
    private static final String DEFAULT_WORKSPACE_DIR = System.getProperty("user.home") + "/.openclaw/workspace";

    // Heartbeat statistics
    private final AtomicLong heartbeatCount = new AtomicLong(0);
    private final AtomicLong lastHeartbeatTime = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Pending wake requests
    private final ConcurrentHashMap<String, PendingWakeRequest> pendingWakes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "heartbeat-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final HeartbeatConfig config;
    private final HeartbeatProcessor processor;
    private final AcpProtocol acpProtocol;
    private final String workspaceDir;

    // Coalesce and retry delays
    private static final long DEFAULT_COALESCE_MS = 250;
    private static final long DEFAULT_RETRY_MS = 1000;

    // Priority levels for wake reasons
    private static final int PRIORITY_RETRY = 0;
    private static final int PRIORITY_INTERVAL = 1;
    private static final int PRIORITY_DEFAULT = 2;
    private static final int PRIORITY_ACTION = 3;

    public HeartbeatService(HeartbeatConfig config, AcpProtocol acpProtocol) {
        this(config, acpProtocol, DEFAULT_WORKSPACE_DIR);
    }

    public HeartbeatService(HeartbeatConfig config, AcpProtocol acpProtocol, String workspaceDir) {
        this.config = config;
        this.processor = new HeartbeatProcessor(config);
        this.acpProtocol = acpProtocol;
        this.workspaceDir = workspaceDir != null ? workspaceDir : DEFAULT_WORKSPACE_DIR;
    }

    @PostConstruct
    public void initialize() {
        if (!config.isEnabled()) {
            logger.info("Heartbeat service is disabled");
            return;
        }

        running.set(true);
        logger.info("Heartbeat service initialized - interval: {}", config.getEvery());
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        logger.info("Heartbeat service shutdown");
    }

    /**
     * Main heartbeat - triggered by Spring Scheduler.
     */
    @Scheduled(fixedDelayString = "${openclaw.agents.defaults.heartbeat.every:PT30M}")
    public void scheduledHeartbeat() {
        if (!running.get() || !config.isEnabled()) {
            return;
        }

        long count = heartbeatCount.incrementAndGet();
        lastHeartbeatTime.set(System.currentTimeMillis());
        logger.debug("Heartbeat #{} executing", count);

        try {
            executeHeartbeat("interval", null, null);
        } catch (Exception e) {
            logger.error("Heartbeat execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Execute heartbeat for a specific agent/session.
     *
     * @param reason the wake reason
     * @param agentId optional agent ID
     * @param sessionKey optional session key
     * @return the execution result
     */
    public CompletableFuture<HeartbeatResult> executeHeartbeat(String reason, String agentId, String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Read HEARTBEAT.md
                String heartbeatContent = readHeartbeatMd();

                // Check if effectively empty
                if (processor.isHeartbeatContentEffectivelyEmpty(heartbeatContent)) {
                    logger.debug("HEARTBEAT.md is effectively empty, skipping");
                    return HeartbeatResult.skipped("empty-content");
                }

                // Build the heartbeat message
                String message = buildHeartbeatMessage(heartbeatContent);

                // Spawn or use existing session
                String targetSessionKey = sessionKey != null ? sessionKey : generateSessionKey();

                // Send message to agent
                Optional<AcpProtocol.SpawnResult> spawnResult = spawnOrGetAgent(agentId, targetSessionKey, message);

                if (spawnResult.isEmpty() || !spawnResult.get().success()) {
                    return HeartbeatResult.failed("spawn-failed");
                }

                // Wait for response
                AcpProtocol.WaitResult waitResult = acpProtocol.waitForAgent(targetSessionKey, 60000).join();

                if (waitResult.status() == AcpProtocol.WaitResult.WaitStatus.TIMEOUT) {
                    return HeartbeatResult.failed("timeout");
                }

                if (waitResult.status() == AcpProtocol.WaitResult.WaitStatus.ERROR) {
                    return HeartbeatResult.failed(waitResult.error().orElse("unknown-error"));
                }

                // Process response
                String response = waitResult.result().orElse("");
                HeartbeatProcessor.StripResult stripResult = processor.stripHeartbeatToken(
                    response, HeartbeatProcessor.StripMode.HEARTBEAT
                );

                if (stripResult.shouldSkip()) {
                    logger.debug("Heartbeat response is HEARTBEAT_OK, skipping delivery");
                    return HeartbeatResult.ok(null, true);
                }

                // Return the actual content
                return HeartbeatResult.ok(stripResult.text(), false);

            } catch (Exception e) {
                logger.error("Heartbeat execution error: {}", e.getMessage(), e);
                return HeartbeatResult.failed(e.getMessage());
            }
        });
    }

    /**
     * Request immediate heartbeat execution.
     *
     * @param reason the wake reason
     * @param agentId optional agent ID
     * @param sessionKey optional session key
     * @return completion future
     */
    public CompletableFuture<Void> requestHeartbeatNow(String reason, String agentId, String sessionKey) {
        PendingWakeRequest request = new PendingWakeRequest(
            reason != null ? reason : "manual",
            resolveReasonPriority(reason),
            System.currentTimeMillis(),
            agentId,
            sessionKey
        );

        String wakeKey = getWakeTargetKey(agentId, sessionKey);
        pendingWakes.merge(wakeKey, request, (old, newReq) -> {
            // Keep higher priority or newer request
            if (newReq.priority > old.priority) {
                return newReq;
            }
            if (newReq.priority == old.priority && newReq.requestedAt >= old.requestedAt) {
                return newReq;
            }
            return old;
        });

        // Schedule execution with coalescing
        schedulePendingWakes();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get heartbeat statistics.
     *
     * @return the statistics
     */
    public HeartbeatStats getStats() {
        return new HeartbeatStats(
            heartbeatCount.get(),
            lastHeartbeatTime.get(),
            pendingWakes.size(),
            running.get()
        );
    }

    // Private methods

    private void schedulePendingWakes() {
        scheduler.schedule(this::processPendingWakes, DEFAULT_COALESCE_MS, TimeUnit.MILLISECONDS);
    }

    private void processPendingWakes() {
        if (pendingWakes.isEmpty()) {
            return;
        }

        // Copy and clear pending wakes
        Map<String, PendingWakeRequest> batch = new ConcurrentHashMap<>(pendingWakes);
        pendingWakes.clear();

        for (PendingWakeRequest request : batch.values()) {
            try {
                HeartbeatResult result = executeHeartbeat(
                    request.reason, request.agentId, request.sessionKey
                ).join();

                if (result.status == HeartbeatStatus.SKIPPED && "requests-in-flight".equals(result.error)) {
                    // Re-queue for retry
                    queuePendingWake(request);
                    scheduleRetry();
                }
            } catch (Exception e) {
                logger.error("Failed to process wake request: {}", e.getMessage());
                // Re-queue for retry
                queuePendingWake(request);
                scheduleRetry();
            }
        }
    }

    private void queuePendingWake(PendingWakeRequest request) {
        String wakeKey = getWakeTargetKey(request.agentId, request.sessionKey);
        pendingWakes.put(wakeKey, new PendingWakeRequest(
            "retry",
            PRIORITY_RETRY,
            System.currentTimeMillis(),
            request.agentId,
            request.sessionKey
        ));
    }

    private void scheduleRetry() {
        scheduler.schedule(this::processPendingWakes, DEFAULT_RETRY_MS, TimeUnit.MILLISECONDS);
    }

    private String readHeartbeatMd() {
        Path path = Paths.get(workspaceDir, "HEARTBEAT.md");
        try {
            if (Files.exists(path)) {
                return Files.readString(path);
            }
        } catch (IOException e) {
            logger.warn("Failed to read HEARTBEAT.md: {}", e.getMessage());
        }
        return "";
    }

    private String buildHeartbeatMessage(String heartbeatContent) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.resolvePrompt());
        if (heartbeatContent != null && !heartbeatContent.isEmpty()) {
            sb.append("\n\n").append(heartbeatContent);
        }
        return sb.toString();
    }

    private Optional<AcpProtocol.SpawnResult> spawnOrGetAgent(String agentId, String sessionKey, String message) {
        // Try to get existing session first
        try {
            var messages = acpProtocol.getMessages(sessionKey, 1).join();
            if (messages != null && !messages.messages().isEmpty()) {
                // Session exists, send message
                acpProtocol.sendMessage(sessionKey, AgentMessage.user(message)).join();
                return Optional.of(AcpProtocol.SpawnResult.success(sessionKey, agentId != null ? agentId : "default"));
            }
        } catch (Exception e) {
            // Session doesn't exist, spawn new
        }

        // Spawn new session
        SpawnRequest request = SpawnRequest.builder()
            .sessionKey(sessionKey)
            .userMessage(message)
            .build();

        return Optional.ofNullable(acpProtocol.spawnAgent(request).join());
    }

    private String generateSessionKey() {
        return "heartbeat-" + System.currentTimeMillis();
    }

    private String getWakeTargetKey(String agentId, String sessionKey) {
        String normAgentId = agentId != null ? agentId.trim() : "";
        String normSessionKey = sessionKey != null ? sessionKey.trim() : "";
        return normAgentId + "::" + normSessionKey;
    }

    private int resolveReasonPriority(String reason) {
        if (reason == null) {
            return PRIORITY_DEFAULT;
        }
        String lower = reason.toLowerCase();
        if (lower.contains("retry")) {
            return PRIORITY_RETRY;
        }
        if (lower.contains("interval")) {
            return PRIORITY_INTERVAL;
        }
        if (lower.contains("action") || lower.contains("cron:")) {
            return PRIORITY_ACTION;
        }
        return PRIORITY_DEFAULT;
    }

    // Records
    private record PendingWakeRequest(
        String reason,
        int priority,
        long requestedAt,
        String agentId,
        String sessionKey
    ) {}

    /**
     * Heartbeat execution result.
     */
    public record HeartbeatResult(
        HeartbeatStatus status,
        String text,
        boolean shouldSkip,
        String error
    ) {
        public static HeartbeatResult ok(String text, boolean shouldSkip) {
            return new HeartbeatResult(HeartbeatStatus.OK, text, shouldSkip, null);
        }

        public static HeartbeatResult skipped(String reason) {
            return new HeartbeatResult(HeartbeatStatus.SKIPPED, null, true, reason);
        }

        public static HeartbeatResult failed(String error) {
            return new HeartbeatResult(HeartbeatStatus.FAILED, null, false, error);
        }
    }

    /**
     * Heartbeat status.
     */
    public enum HeartbeatStatus {
        OK,
        SKIPPED,
        FAILED
    }

    /**
     * Heartbeat statistics.
     */
    public record HeartbeatStats(
        long heartbeatCount,
        long lastHeartbeatTime,
        int pendingWakes,
        boolean running
    ) {}
}