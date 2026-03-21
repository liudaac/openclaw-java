package openclaw.session.binding;

/**
 * Placement option for session binding.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public enum SessionBindingPlacement {
    /**
     * Bind to current conversation.
     */
    CURRENT("current"),

    /**
     * Bind to child conversation.
     */
    CHILD("child");

    private final String value;

    SessionBindingPlacement(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SessionBindingPlacement fromString(String value) {
        if (value == null) {
            return null;
        }
        for (SessionBindingPlacement placement : values()) {
            if (placement.value.equals(value)) {
                return placement;
            }
        }
        return null;
    }
}
