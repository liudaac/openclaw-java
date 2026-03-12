package openclaw.server.controller;

import openclaw.gateway.GatewayService;
import openclaw.gateway.GatewayService.WorkItem;
import openclaw.gateway.GatewayService.WorkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Gateway REST API Controller
 *
 * <p>Provides HTTP endpoints for gateway operations including work submission,
 * status checking, and health monitoring.</p>
 */
@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
                "status", "UP",
                "service", "openclaw-gateway",
                "version", "2026.3.9",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Submit work to the gateway
     */
    @PostMapping("/work")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<WorkSubmissionResponse> submitWork(@RequestBody WorkSubmissionRequest request) {
        logger.info("Received work submission: type={}, priority={}", request.type(), request.priority());

        WorkItem workItem = WorkItem.of(
                request.type(),
                request.payload().getBytes()
        );

        return Mono.fromFuture(gatewayService.submitWork(workItem))
                .map(workId -> new WorkSubmissionResponse(workId, "ACCEPTED", null));
    }

    /**
     * Get work status
     */
    @GetMapping("/work/{workId}")
    public Mono<WorkStatusResponse> getWorkStatus(@PathVariable String workId) {
        return Mono.fromFuture(gatewayService.getWorkStatus(workId))
                .map(this::mapToResponse);
    }

    /**
     * Stream work status updates
     */
    @GetMapping(value = "/work/{workId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WorkStatusResponse> streamWorkStatus(@PathVariable String workId) {
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(i -> Mono.fromFuture(gatewayService.getWorkStatus(workId)))
                .map(this::mapToResponse)
                .takeWhile(response -> response.state().equals("RUNNING") || response.state().equals("PENDING"));
    }

    /**
     * Cancel work
     */
    @DeleteMapping("/work/{workId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> cancelWork(@PathVariable String workId) {
        logger.info("Cancelling work: {}", workId);
        return Mono.fromFuture(gatewayService.cancelWork(workId));
    }

    /**
     * Get gateway statistics
     */
    @GetMapping("/stats")
    public Mono<GatewayStatsResponse> getStats() {
        var nodeRegistry = gatewayService.getNodeRegistry();
        var workQueue = gatewayService.getWorkQueue();

        return Mono.just(new GatewayStatsResponse(
                nodeRegistry.getNodeCount(),
                workQueue.getPendingCount(),
                workQueue.getCompletedCount(),
                Runtime.getRuntime().availableProcessors()
        ));
    }

    private WorkStatusResponse mapToResponse(WorkStatus status) {
        return new WorkStatusResponse(
                status.workId(),
                status.state().name(),
                status.progress(),
                status.error(),
                status.result() != null ? new String(status.result()) : null
        );
    }

    // Request/Response Records
    public record WorkSubmissionRequest(
            GatewayService.WorkType type,
            String payload,
            int priority
    ) {}

    public record WorkSubmissionResponse(
            String workId,
            String status,
            String error
    ) {}

    public record WorkStatusResponse(
            String workId,
            String state,
            int progress,
            String error,
            String result
    ) {}

    public record GatewayStatsResponse(
            int activeNodes,
            int pendingWork,
            long completedWork,
            int workerThreads
    ) {}
}
