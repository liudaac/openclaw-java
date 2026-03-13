package openclaw.session.model;

/**
 * Session status enumeration.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public enum SessionStatus {
    PENDING("pending", "等待中"),
    ACTIVE("active", "进行中"),
    PAUSED("paused", "已暂停"),
    COMPLETED("completed", "已完成"),
    ERROR("error", "错误"),
    ARCHIVED("archived", "已归档");
    
    private final String code;
    private final String description;
    
    SessionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this is a terminal state.
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == ARCHIVED;
    }
    
    /**
     * Check if session is active.
     */
    public boolean isActive() {
        return this == ACTIVE || this == PAUSED;
    }
    
    /**
     * Parse from string.
     */
    public static SessionStatus fromString(String status) {
        if (status == null) return null;
        for (SessionStatus s : values()) {
            if (s.code.equalsIgnoreCase(status) || s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown session status: " + status);
    }
}
