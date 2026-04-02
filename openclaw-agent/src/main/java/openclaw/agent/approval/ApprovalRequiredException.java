package openclaw.agent.approval;

import openclaw.acp.AcpApprovalClass;

/**
 * Exception thrown when tool execution requires approval.
 *
 * @author OpenClaw Team
 * @version 2026.4.2
 */
public class ApprovalRequiredException extends RuntimeException {

    private final String requestId;
    private final String toolName;
    private final AcpApprovalClass approvalClass;
    private final String approvalMessage;

    public ApprovalRequiredException(String requestId, String toolName,
                                     AcpApprovalClass approvalClass, String message) {
        super(message);
        this.requestId = requestId;
        this.toolName = toolName;
        this.approvalClass = approvalClass;
        this.approvalMessage = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getToolName() {
        return toolName;
    }

    public AcpApprovalClass getApprovalClass() {
        return approvalClass;
    }

    public String getApprovalMessage() {
        return approvalMessage;
    }

    /**
     * Create a user-friendly approval request message.
     */
    public String getUserMessage() {
        return String.format(
            "🔒 需要审批\n\n" +
            "工具: %s\n" +
            "分类: %s\n" +
            "请求ID: %s\n\n" +
            "请使用 /approve %s 批准，或 /reject %s [原因] 拒绝",
            toolName, approvalClass, requestId, requestId, requestId
        );
    }
}
