package openclaw.tasks.model;

/**
 * Task status enum.
 * Mirrors the TypeScript type: TaskStatus from task-registry.types.ts
 */
public enum TaskStatus {
    QUEUED("queued"),
    RUNNING("running"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    TIMED_OUT("timed_out"),
    CANCELLED("cancelled"),
    LOST("lost");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == TIMED_OUT || 
               this == CANCELLED || this == LOST;
    }
}
