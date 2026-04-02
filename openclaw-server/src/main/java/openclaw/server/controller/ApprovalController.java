package openclaw.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import openclaw.agent.approval.ApprovalService;
import openclaw.agent.approval.ApprovalService.ApprovalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Approval Controller
 * 
 * REST API for managing tool execution approvals.
 *
 * @author OpenClaw Team
 * @version 2026.4.2
 */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalController.class);

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * List pending approval requests.
     * 
     * GET /api/approvals/pending
     */
    @GetMapping("/pending")
    public Mono<ResponseEntity<ObjectNode>> listPendingApprovals() {
        return Mono.fromCallable(() -> {
            // Note: In real implementation, ApprovalService should expose getPendingRequests()
            // For now, return empty list
            ObjectNode result = objectMapper.createObjectNode();
            result.putArray("requests");
            result.put("count", 0);
            
            return ResponseEntity.ok(result);
        });
    }

    /**
     * Get a specific approval request.
     * 
     * GET /api/approvals/{requestId}
     */
    @GetMapping("/{requestId}")
    public Mono<ResponseEntity<ObjectNode>> getApprovalRequest(@PathVariable String requestId) {
        return Mono.fromCallable(() -> {
            ApprovalRequest request = approvalService.getPendingRequest(requestId);
            
            if (request == null) {
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Approval request not found");
                return ResponseEntity.status(404).body(error);
            }
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("requestId", request.getRequestId());
            result.put("toolName", request.getClassification().toolName());
            result.put("approvalClass", request.getClassification().approvalClass().toString());
            result.put("autoApprove", request.getClassification().autoApprove());
            result.put("createdAt", request.getCreatedAt().toString());
            result.put("status", request.isApproved() ? "approved" : 
                                 request.isRejected() ? "rejected" : "pending");
            
            return ResponseEntity.ok(result);
        });
    }

    /**
     * Approve a pending request.
     * 
     * POST /api/approvals/{requestId}/approve
     */
    @PostMapping("/{requestId}/approve")
    public Mono<ResponseEntity<ObjectNode>> approveRequest(
            @PathVariable String requestId,
            @RequestBody(required = false) ApprovalRequestBody body) {
        
        return Mono.fromCallable(() -> {
            String approverId = body != null && body.approverId() != null ? 
                body.approverId() : "system";
            
            boolean success = approvalService.approveRequest(requestId, approverId);
            
            ObjectNode result = objectMapper.createObjectNode();
            if (success) {
                result.put("success", true);
                result.put("message", "Approval granted");
                result.put("requestId", requestId);
                logger.info("Approval request {} approved by {}", requestId, approverId);
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("error", "Approval request not found or already processed");
                return ResponseEntity.status(404).body(result);
            }
        });
    }

    /**
     * Reject a pending request.
     * 
     * POST /api/approvals/{requestId}/reject
     */
    @PostMapping("/{requestId}/reject")
    public Mono<ResponseEntity<ObjectNode>> rejectRequest(
            @PathVariable String requestId,
            @RequestBody(required = false) RejectionRequestBody body) {
        
        return Mono.fromCallable(() -> {
            String rejectorId = body != null && body.rejectorId() != null ? 
                body.rejectorId() : "system";
            String reason = body != null && body.reason() != null ? 
                body.reason() : "Rejected by user";
            
            boolean success = approvalService.rejectRequest(requestId, rejectorId, reason);
            
            ObjectNode result = objectMapper.createObjectNode();
            if (success) {
                result.put("success", true);
                result.put("message", "Approval rejected");
                result.put("requestId", requestId);
                result.put("reason", reason);
                logger.info("Approval request {} rejected by {}: {}", 
                    requestId, rejectorId, reason);
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("error", "Approval request not found or already processed");
                return ResponseEntity.status(404).body(result);
            }
        });
    }

    /**
     * Check approval status.
     * 
     * GET /api/approvals/{requestId}/status
     */
    @GetMapping("/{requestId}/status")
    public Mono<ResponseEntity<ObjectNode>> getApprovalStatus(@PathVariable String requestId) {
        return Mono.fromCallable(() -> {
            ApprovalRequest request = approvalService.getPendingRequest(requestId);
            
            ObjectNode result = objectMapper.createObjectNode();
            if (request == null) {
                result.put("exists", false);
                result.put("status", "unknown");
            } else {
                result.put("exists", true);
                result.put("status", request.isApproved() ? "approved" : 
                                     request.isRejected() ? "rejected" : "pending");
                result.put("toolName", request.getClassification().toolName());
                result.put("approvalClass", request.getClassification().approvalClass().toString());
            }
            
            return ResponseEntity.ok(result);
        });
    }

    // Request body records

    public record ApprovalRequestBody(String approverId) {}
    
    public record RejectionRequestBody(String rejectorId, String reason) {}
}
