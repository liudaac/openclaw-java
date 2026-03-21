package openclaw.agent.event;

/**
 * Indicator type for heartbeat UI status display.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public enum HeartbeatIndicatorType {
    /**
     * Everything is OK.
     */
    OK("ok"),

    /**
     * Alert status - message sent but needs attention.
     */
    ALERT("alert"),

    /**
     * Error status - delivery failed.
     */
    ERROR("error");

    private final String value;

    HeartbeatIndicatorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Resolves indicator type from status.
     *
     * @param status the heartbeat status
     * @return the indicator type or null
     */
    public static HeartbeatIndicatorType resolveFromStatus(HeartbeatStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OK_EMPTY, OK_TOKEN -> OK;
            case SENT -> ALERT;
            case FAILED -> ERROR;
            case SKIPPED -> null;
        };
    }

    public static HeartbeatIndicatorType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (HeartbeatIndicatorType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
