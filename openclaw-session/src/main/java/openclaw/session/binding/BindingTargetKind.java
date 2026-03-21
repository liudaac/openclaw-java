package openclaw.session.binding;

/**
 * Kind of binding target.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public enum BindingTargetKind {
    /**
     * Binding to a subagent session.
     */
    SUBAGENT("subagent"),

    /**
     * Binding to a regular session.
     */
    SESSION("session");

    private final String value;

    BindingTargetKind(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BindingTargetKind fromString(String value) {
        if (value == null) {
            return null;
        }
        for (BindingTargetKind kind : values()) {
            if (kind.value.equals(value)) {
                return kind;
            }
        }
        return null;
    }
}
