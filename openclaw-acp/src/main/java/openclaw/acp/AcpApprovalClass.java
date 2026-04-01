package openclaw.acp;

/**
 * ACP approval classification enum.
 * Mirrors the TypeScript type: AcpApprovalClass from approval-classifier.ts
 */
public enum AcpApprovalClass {
    READONLY_SCOPED("readonly_scoped", false),
    READONLY_SEARCH("readonly_search", true),  // Auto-approved
    MUTATING("mutating", false),
    EXEC_CAPABLE("exec_capable", false),
    CONTROL_PLANE("control_plane", false),
    INTERACTIVE("interactive", false),
    OTHER("other", false),
    UNKNOWN("unknown", false);

    private final String value;
    private final boolean autoApprove;

    AcpApprovalClass(String value, boolean autoApprove) {
        this.value = value;
        this.autoApprove = autoApprove;
    }

    public String getValue() {
        return value;
    }

    public boolean isAutoApprove() {
        return autoApprove;
    }
}
