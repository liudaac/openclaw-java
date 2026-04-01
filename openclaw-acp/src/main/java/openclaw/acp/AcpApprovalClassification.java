package openclaw.acp;

/**
 * Result of ACP approval classification.
 * Mirrors the TypeScript type from approval-classifier.ts
 */
public record AcpApprovalClassification(
    String toolName,
    AcpApprovalClass approvalClass,
    boolean autoApprove
) {
    /**
     * Check if this classification allows auto-approval.
     */
    public boolean canAutoApprove() {
        return autoApprove;
    }

    /**
     * Get a description of the classification.
     */
    public String getDescription() {
        return String.format("%s [%s] autoApprove=%s", toolName, approvalClass.getValue(), autoApprove);
    }
}
