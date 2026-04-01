package openclaw.tasks.model;

/**
 * Task delivery status enum.
 * Mirrors the TypeScript type: TaskDeliveryStatus from task-registry.types.ts
 */
public enum TaskDeliveryStatus {
    PENDING("pending"),
    DELIVERED("delivered"),
    SESSION_QUEUED("session_queued"),
    FAILED("failed"),
    PARENT_MISSING("parent_missing"),
    NOT_APPLICABLE("not_applicable");

    private final String value;

    TaskDeliveryStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
