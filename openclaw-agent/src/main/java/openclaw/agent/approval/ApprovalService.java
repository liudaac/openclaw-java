package openclaw.agent.approval;

import openclaw.acp.ApprovalClassifier;
import openclaw.acp.AcpApprovalClassification;
import openclaw.acp.AcpApprovalClass;
import openclaw.acp.model.ToolCall;
import openclaw.sdk.tool.ToolExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Approval Service for tool execution.
 * Integrates ACP (Approval Control Plane) with Agent tool execution.
 *
 * @author OpenClaw Team
 * @version 2026.4.2
 */
@Service
public class ApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalService.class);

    @Autowired
    private ApprovalClassifier approvalClassifier;

    // Pending approval requests storage
    private final Map<String, ApprovalRequest> pendingApprovals = new ConcurrentHashMap<>();

    /**
     * Evaluate a tool call for approval.
     *
     * @param toolCall the tool call to evaluate
     * @param context the execution context
     * @return Mono of approval decision
     */
    public Mono<ApprovalDecision> evaluate(ToolCall toolCall, ToolExecuteContext context) {
        return Mono.fromCallable(() -> {
            // Classify the tool call
            AcpApprovalClassification classification = approvalClassifier.classify(toolCall);
            
            logger.debug("Tool '{}' classified as '{}' (autoApprove={})",
                classification.toolName(),
                classification.approvalClass(),
                classification.autoApprove());

            // If auto-approved, return immediately
            if (classification.autoApprove()) {
                return ApprovalDecision.autoApprove(classification.toolName(), classification.approvalClass());
            }

            // Check if user has permission for this approval class
            if (hasPermission(context, classification.approvalClass())) {
                logger.debug("User has permission for approval class: {}", classification.approvalClass());
                return ApprovalDecision.approve(classification.toolName(), classification.approvalClass());
            }

            // Create pending approval request
            String requestId = UUID.randomUUID().toString();
            ApprovalRequest request = new ApprovalRequest(
                requestId,
                toolCall,
                classification,
                context,
                Instant.now()
            );
            pendingApprovals.put(requestId, request);

            logger.info("Approval required for tool '{}' (requestId={})",
                classification.toolName(), requestId);

            return ApprovalDecision.pending(requestId, classification.toolName(), classification.approvalClass());
        });
    }

    /**
     * Approve a pending request.
     *
     * @param requestId the approval request ID
     * @param approverId the approver user ID
     * @return true if approved successfully
     */
    public boolean approveRequest(String requestId, String approverId) {
        ApprovalRequest request = pendingApprovals.get(requestId);
        if (request == null) {
            logger.warn("Approval request not found: {}", requestId);
            return false;
        }

        request.setApproved(true);
        request.setApproverId(approverId);
        request.setApprovedAt(Instant.now());
        
        logger.info("Approval request {} approved by {}", requestId, approverId);
        return true;
    }

    /**
     * Reject a pending request.
     *
     * @param requestId the approval request ID
     * @param rejectorId the rejector user ID
     * @param reason the rejection reason
     * @return true if rejected successfully
     */
    public boolean rejectRequest(String requestId, String rejectorId, String reason) {
        ApprovalRequest request = pendingApprovals.remove(requestId);
        if (request == null) {
            logger.warn("Approval request not found: {}", requestId);
            return false;
        }

        request.setRejected(true);
        request.setRejectorId(rejectorId);
        request.setRejectionReason(reason);
        request.setRejectedAt(Instant.now());
        
        logger.info("Approval request {} rejected by {}: {}", requestId, rejectorId, reason);
        return true;
    }

    /**
     * Get a pending approval request.
     *
     * @param requestId the request ID
     * @return the approval request or null
     */
    public ApprovalRequest getPendingRequest(String requestId) {
        return pendingApprovals.get(requestId);
    }

    /**
     * Check if a request is approved.
     *
     * @param requestId the request ID
     * @return true if approved
     */
    public boolean isApproved(String requestId) {
        ApprovalRequest request = pendingApprovals.get(requestId);
        return request != null && request.isApproved();
    }

    /**
     * Remove a pending request (after execution or timeout).
     *
     * @param requestId the request ID
     */
    public void removeRequest(String requestId) {
        pendingApprovals.remove(requestId);
    }

    /**
     * Clean up expired approval requests.
     *
     * @param maxAgeSeconds maximum age in seconds
     */
    public void cleanupExpiredRequests(long maxAgeSeconds) {
        Instant cutoff = Instant.now().minusSeconds(maxAgeSeconds);
        pendingApprovals.entrySet().removeIf(entry -> {
            ApprovalRequest request = entry.getValue();
            boolean expired = request.getCreatedAt().isBefore(cutoff);
            if (expired) {
                logger.debug("Removing expired approval request: {}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * Check if user has permission for approval class.
     * Override this method to implement custom permission logic.
     *
     * @param context the execution context
     * @param approvalClass the approval class
     * @return true if user has permission
     */
    protected boolean hasPermission(ToolExecuteContext context, AcpApprovalClass approvalClass) {
        // Default implementation: no special permissions
        // Subclasses can override to implement role-based permissions
        return false;
    }

    // Inner classes

    /**
     * Approval decision.
     */
    public static class ApprovalDecision {
        private final boolean approved;
        private final boolean autoApproved;
        private final boolean pending;
        private final String requestId;
        private final String toolName;
        private final AcpApprovalClass approvalClass;
        private final String message;

        private ApprovalDecision(boolean approved, boolean autoApproved, boolean pending,
                                 String requestId, String toolName, AcpApprovalClass approvalClass,
                                 String message) {
            this.approved = approved;
            this.autoApproved = autoApproved;
            this.pending = pending;
            this.requestId = requestId;
            this.toolName = toolName;
            this.approvalClass = approvalClass;
            this.message = message;
        }

        public static ApprovalDecision autoApprove(String toolName, AcpApprovalClass approvalClass) {
            return new ApprovalDecision(true, true, false, null, toolName, approvalClass,
                "Auto-approved: " + approvalClass);
        }

        public static ApprovalDecision approve(String toolName, AcpApprovalClass approvalClass) {
            return new ApprovalDecision(true, false, false, null, toolName, approvalClass,
                "Approved: " + approvalClass);
        }

        public static ApprovalDecision pending(String requestId, String toolName, AcpApprovalClass approvalClass) {
            return new ApprovalDecision(false, false, true, requestId, toolName, approvalClass,
                "Approval required for tool: " + toolName + " (class: " + approvalClass + ")");
        }

        public static ApprovalDecision rejected(String toolName, AcpApprovalClass approvalClass, String reason) {
            return new ApprovalDecision(false, false, false, null, toolName, approvalClass,
                "Rejected: " + reason);
        }

        // Getters
        public boolean isApproved() { return approved; }
        public boolean isAutoApproved() { return autoApproved; }
        public boolean isPending() { return pending; }
        public String getRequestId() { return requestId; }
        public String getToolName() { return toolName; }
        public AcpApprovalClass getApprovalClass() { return approvalClass; }
        public String getMessage() { return message; }
    }

    /**
     * Approval request.
     */
    public static class ApprovalRequest {
        private final String requestId;
        private final ToolCall toolCall;
        private final AcpApprovalClassification classification;
        private final ToolExecuteContext context;
        private final Instant createdAt;
        
        private boolean approved;
        private String approverId;
        private Instant approvedAt;
        private boolean rejected;
        private String rejectorId;
        private String rejectionReason;
        private Instant rejectedAt;

        public ApprovalRequest(String requestId, ToolCall toolCall,
                              AcpApprovalClassification classification,
                              ToolExecuteContext context, Instant createdAt) {
            this.requestId = requestId;
            this.toolCall = toolCall;
            this.classification = classification;
            this.context = context;
            this.createdAt = createdAt;
        }

        // Getters and setters
        public String getRequestId() { return requestId; }
        public ToolCall getToolCall() { return toolCall; }
        public AcpApprovalClassification getClassification() { return classification; }
        public ToolExecuteContext getContext() { return context; }
        public Instant getCreatedAt() { return createdAt; }
        
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        
        public String getApproverId() { return approverId; }
        public void setApproverId(String approverId) { this.approverId = approverId; }
        
        public Instant getApprovedAt() { return approvedAt; }
        public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
        
        public boolean isRejected() { return rejected; }
        public void setRejected(boolean rejected) { this.rejected = rejected; }
        
        public String getRejectorId() { return rejectorId; }
        public void setRejectorId(String rejectorId) { this.rejectorId = rejectorId; }
        
        public String getRejectionReason() { return rejectionReason; }
        public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
        
        public Instant getRejectedAt() { return rejectedAt; }
        public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
    }
}