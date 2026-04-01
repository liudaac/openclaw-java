package openclaw.tasks.model;

/**
 * Task notification policy enum.
 * Mirrors the TypeScript type: TaskNotifyPolicy from task-registry.types.ts
 */
public enum TaskNotifyPolicy {
    DONE_ONLY("done_only"),
    STATE_CHANGES("state_changes"),
    SILENT("silent");

    private final String value;

    TaskNotifyPolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
