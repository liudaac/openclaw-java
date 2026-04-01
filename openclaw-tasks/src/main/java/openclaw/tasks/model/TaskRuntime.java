package openclaw.tasks.model;

/**
 * Task runtime type enum.
 * Mirrors the TypeScript type: TaskRuntime from task-registry.types.ts
 */
public enum TaskRuntime {
    SUBAGENT("subagent"),
    ACP("acp"),
    CLI("cli"),
    CRON("cron");

    private final String value;

    TaskRuntime(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
