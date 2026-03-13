package openclaw.cron.model;

/**
 * Job status enumeration.
 * 
 * <p>Equivalent to Node.js src/cron/types.ts CronJobStatus.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public enum JobStatus {
    /**
     * Job is created but not yet scheduled.
     */
    PENDING("pending", "等待执行"),
    
    /**
     * Job is currently running.
     */
    RUNNING("running", "执行中"),
    
    /**
     * Job is temporarily paused.
     */
    PAUSED("paused", "已暂停"),
    
    /**
     * Job completed successfully.
     */
    COMPLETED("completed", "已完成"),
    
    /**
     * Job failed to execute.
     */
    FAILED("failed", "执行失败"),
    
    /**
     * Job was cancelled.
     */
    CANCELLED("cancelled", "已取消");
    
    private final String code;
    private final String description;
    
    JobStatus(String code, String description) {
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
     * Check if this is a terminal state (cannot transition to other states).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
    
    /**
     * Check if the job is active (can be executed).
     */
    public boolean isActive() {
        return this == PENDING || this == RUNNING;
    }
    
    /**
     * Parse status from string.
     */
    public static JobStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        for (JobStatus s : values()) {
            if (s.code.equalsIgnoreCase(status) || s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown job status: " + status);
    }
}
