package openclaw.session.binding;

/**
 * Error codes for session binding.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public enum SessionBindingErrorCode {
    /**
     * Adapter is not available for the channel/account.
     */
    BINDING_ADAPTER_UNAVAILABLE("BINDING_ADAPTER_UNAVAILABLE"),

    /**
     * Binding capability is not supported.
     */
    BINDING_CAPABILITY_UNSUPPORTED("BINDING_CAPABILITY_UNSUPPORTED"),

    /**
     * Failed to create binding.
     */
    BINDING_CREATE_FAILED("BINDING_CREATE_FAILED");

    private final String value;

    SessionBindingErrorCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
