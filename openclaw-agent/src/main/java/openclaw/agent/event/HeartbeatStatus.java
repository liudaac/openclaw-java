package openclaw.agent.event;

/**
 * Heartbeat message delivery status.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public enum HeartbeatStatus {
    /**
     * Message was sent successfully.
     */
    SENT("sent"),

    /**
     * Empty OK response (no content).
     */
    OK_EMPTY("ok-empty"),

    /**
     * OK response with token.
     */
    OK_TOKEN("ok-token"),

    /**
     * Message was skipped.
     */
    SKIPPED("skipped"),

    /**
     * Message delivery failed.
     */
    FAILED("failed");

    private final String value;

    HeartbeatStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static HeartbeatStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        for (HeartbeatStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
