package openclaw.session.binding;

/**
 * Status of a session binding.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public enum BindingStatus {
    /**
     * Binding is active.
     */
    ACTIVE("active"),

    /**
     * Binding is ending.
     */
    ENDING("ending"),

    /**
     * Binding has ended.
     */
    ENDED("ended");

    private final String value;

    BindingStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BindingStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        for (BindingStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
