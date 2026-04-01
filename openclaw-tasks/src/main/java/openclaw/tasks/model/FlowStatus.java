package openclaw.tasks.model;

/**
 * Flow status enum representing the state machine of a flow.
 * Mirrors the TypeScript type: FlowStatus from flow-registry.types.ts
 */
public enum FlowStatus {
    QUEUED("queued"),
    RUNNING("running"),
    WAITING("waiting"),
    BLOCKED("blocked"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    LOST("lost");

    private final String value;

    FlowStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED || this == LOST;
    }

    public boolean isActive() {
        return !isTerminal();
    }
}
