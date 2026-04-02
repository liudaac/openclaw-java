package openclaw.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import openclaw.tasks.delivery.FlowDeliveryServiceImpl;
import openclaw.tasks.model.FlowRecord;
import openclaw.tasks.model.FlowStatus;
import openclaw.tasks.model.DeliveryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Task Flow Controller
 * 
 * REST API for managing task flows.
 *
 * @author OpenClaw Team
 * @version 2026.4.2
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskFlowController {

    private static final Logger logger = LoggerFactory.getLogger(TaskFlowController.class);

    @Autowired
    private FlowDeliveryServiceImpl flowDeliveryService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Create a new task flow.
     * 
     * POST /api/tasks/flows
     */
    @PostMapping("/flows")
    public Mono<ResponseEntity<ObjectNode>> createFlow(@RequestBody CreateFlowRequest request) {
        return Mono.fromCallable(() -> {
            String flowId = UUID.randomUUID().toString();
            
            DeliveryContext origin = new DeliveryContext();
            origin.setChannel(request.channel());
            origin.setTo(request.to());
            origin.setAccountId(request.accountId());
            origin.setThreadId(request.threadId());
            
            FlowRecord flow = FlowRecord.builder()
                .flowId(flowId)
                .goal(request.name())
                .ownerSessionKey(request.sessionKey())
                .requesterOrigin(origin)
                .status(FlowStatus.QUEUED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            flowDeliveryService.registerFlow(flow);
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("flowId", flowId);
            result.put("status", "pending");
            result.put("message", "Flow created successfully");
            
            logger.info("Created flow: {} (name={})", flowId, request.name());
            
            return ResponseEntity.ok(result);
        });
    }

    /**
     * Get flow status.
     * 
     * GET /api/tasks/flows/{flowId}
     */
    @GetMapping("/flows/{flowId}")
    public Mono<ResponseEntity<ObjectNode>> getFlowStatus(@PathVariable String flowId) {
        return Mono.fromCallable(() -> {
            // Note: In real implementation, FlowDeliveryService should expose getFlow()
            ObjectNode result = objectMapper.createObjectNode();
            result.put("flowId", flowId);
            result.put("exists", false);
            result.put("status", "unknown");
            
            return ResponseEntity.ok(result);
        });
    }

    /**
     * Update flow status.
     * 
     * POST /api/tasks/flows/{flowId}/status
     */
    @PostMapping("/flows/{flowId}/status")
    public Mono<ResponseEntity<ObjectNode>> updateFlowStatus(
            @PathVariable String flowId,
            @RequestBody UpdateStatusRequest request) {
        
        return Mono.fromCallable(() -> {
            FlowStatus status = FlowStatus.valueOf(request.status().toUpperCase());
            flowDeliveryService.updateFlowStatus(flowId, status, request.message());
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("flowId", flowId);
            result.put("status", request.status());
            
            return ResponseEntity.ok(result);
        });
    }

    /**
     * Complete a flow.
     * 
     * POST /api/tasks/flows/{flowId}/complete
     */
    @PostMapping("/flows/{flowId}/complete")
    public Mono<ResponseEntity<ObjectNode>> completeFlow(
            @PathVariable String flowId,
            @RequestBody(required = false) CompleteFlowRequest request) {
        
        return Mono.fromCallable(() -> {
            String result = request != null ? request.result() : null;
            flowDeliveryService.completeFlow(flowId, result);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("flowId", flowId);
            response.put("status", "completed");
            
            return ResponseEntity.ok(response);
        });
    }

    /**
     * Fail a flow.
     * 
     * POST /api/tasks/flows/{flowId}/fail
     */
    @PostMapping("/flows/{flowId}/fail")
    public Mono<ResponseEntity<ObjectNode>> failFlow(
            @PathVariable String flowId,
            @RequestBody FailFlowRequest request) {
        
        return Mono.fromCallable(() -> {
            flowDeliveryService.failFlow(flowId, request.error());
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("flowId", flowId);
            result.put("status", "failed");
            result.put("error", request.error());
            
            return ResponseEntity.ok(result);
        });
    }

    /**
     * Get active flow count.
     * 
     * GET /api/tasks/flows/count
     */
    @GetMapping("/flows/count")
    public Mono<ResponseEntity<ObjectNode>> getActiveFlowCount() {
        return Mono.fromCallable(() -> {
            int count = flowDeliveryService.getActiveFlowCount();
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("activeFlows", count);
            
            return ResponseEntity.ok(result);
        });
    }

    // Request body records

    public record CreateFlowRequest(
        String name,
        String description,
        String agentId,
        String sessionKey,
        String idempotencyKey,
        String channel,
        String to,
        String accountId,
        String threadId
    ) {}

    public record UpdateStatusRequest(
        String status,
        String message
    ) {}

    public record CompleteFlowRequest(
        String result
    ) {}

    public record FailFlowRequest(
        String error
    ) {}
}
