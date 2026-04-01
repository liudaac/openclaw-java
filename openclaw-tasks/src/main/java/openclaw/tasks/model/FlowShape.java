package openclaw.tasks.model;

/**
 * Flow shape enum defining the structure of a flow.
 * Mirrors the TypeScript type: FlowShape from flow-registry.types.ts
 */
public enum FlowShape {
    SINGLE_TASK("single_task"),
    LINEAR("linear");

    private final String value;

    FlowShape(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
