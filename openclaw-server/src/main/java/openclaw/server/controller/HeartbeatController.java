package openclaw.server.controller;

import openclaw.agent.heartbeat.HeartbeatConfig;
import openclaw.agent.heartbeat.HeartbeatService;
import openclaw.agent.heartbeat.HeartbeatService.HeartbeatResult;
import openclaw.agent.heartbeat.HeartbeatService.HeartbeatStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for heartbeat operations.
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
@RestController
@RequestMapping("/api/v1/heartbeat")
public class HeartbeatController {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatController.class);

    private final HeartbeatService heartbeatService;
    private final HeartbeatConfig heartbeatConfig;

    @Autowired
    public HeartbeatController(HeartbeatService heartbeatService, HeartbeatConfig heartbeatConfig) {
        this.heartbeatService = heartbeatService;
        this.heartbeatConfig = heartbeatConfig;
    }

    /**
     * Trigger a manual heartbeat.
     */
    @PostMapping("/trigger")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerHeartbeat(
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String sessionKey) {
        
        logger.info("Manual heartbeat triggered - reason: {}, agentId: {}", reason, agentId);
        
        return heartbeatService.executeHeartbeat(
            reason != null ? reason : "manual",
            agentId,
            sessionKey
        ).thenApply(result -> {
            Map<String, Object> response = Map.of(
                "status", result.status().name(),
                "shouldSkip", result.shouldSkip(),
                "text", result.text() != null ? result.text() : "",
                "error", result.error() != null ? result.error() : ""
            );
            return ResponseEntity.ok(response);
        });
    }

    /**
     * Get heartbeat statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        HeartbeatStats stats = heartbeatService.getStats();
        
        Map<String, Object> response = Map.of(
            "heartbeatCount", stats.heartbeatCount(),
            "lastHeartbeatTime", stats.lastHeartbeatTime(),
            "pendingWakes", stats.pendingWakes(),
            "running", stats.running()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get heartbeat configuration.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> response = Map.of(
            "enabled", heartbeatConfig.isEnabled(),
            "every", heartbeatConfig.getEvery().toString(),
            "everyMillis", heartbeatConfig.getEvery().toMillis(),
            "ackMaxChars", heartbeatConfig.getAckMaxChars(),
            "target", heartbeatConfig.getTarget(),
            "prompt", heartbeatConfig.resolvePrompt()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update heartbeat configuration.
     */
    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String every,
            @RequestParam(required = false) Integer ackMaxChars,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) String prompt) {
        
        if (enabled != null) {
            heartbeatConfig.setEnabled(enabled);
        }
        if (every != null) {
            try {
                heartbeatConfig.setEvery(Duration.parse(every));
            } catch (Exception e) {
                logger.warn("Invalid duration format: {}", every);
            }
        }
        if (ackMaxChars != null) {
            heartbeatConfig.setAckMaxChars(ackMaxChars);
        }
        if (target != null) {
            heartbeatConfig.setTarget(target);
        }
        if (prompt != null) {
            heartbeatConfig.setPrompt(prompt);
        }
        
        return getConfig();
    }

    /**
     * Request immediate heartbeat.
     */
    @PostMapping("/request")
    public CompletableFuture<ResponseEntity<Map<String, String>>> requestHeartbeat(
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String sessionKey) {
        
        return heartbeatService.requestHeartbeatNow(reason, agentId, sessionKey)
            .thenApply(v -> ResponseEntity.ok(Map.of("status", "requested")));
    }
}
