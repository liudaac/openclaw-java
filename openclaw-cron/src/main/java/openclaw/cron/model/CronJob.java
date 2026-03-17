package openclaw.cron.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Cron job entity representing a scheduled task.
 * 
 * <p>Equivalent to Node.js src/cron/types.ts CronJob interface.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class CronJob {
    
    private final String id;
    private String name;
    private String schedule;           // Cron expression
    private String command;            // Command to execute
    private String timezone;           // Timezone (e.g., "Asia/Shanghai")
    private JobStatus status;
    private Instant lastRun;
    private Instant nextRun;
    private int runCount;
    private int failCount;
    private final Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> metadata;
    
    // Retry configuration
    private int maxRetries;
    private long retryDelayMs;
    
    // Execution configuration
    private boolean isolated;          // Run in isolated process
    private long timeoutSeconds;       // Execution timeout
    private String workingDirectory;   // Working directory for execution
    
    // Wake mode for heartbeat integration
    private WakeMode wakeMode;         // "now" or "next-heartbeat"
    private String sessionTarget;      // "main" or "isolated"
    private String agentId;            // Target agent ID
    private String sessionKey;         // Target session key
    private String deliveryChannel;    // Delivery channel
    private String deliveryTo;         // Delivery target
    
    public CronJob() {
        this.id = UUID.randomUUID().toString();
        this.status = JobStatus.PENDING;
        this.runCount = 0;
        this.failCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.maxRetries = 3;
        this.retryDelayMs = 1000;
        this.isolated = true;
        this.timeoutSeconds = 60;
        this.timezone = "UTC";
        this.wakeMode = WakeMode.NEXT_HEARTBEAT;  // Default to next heartbeat
        this.sessionTarget = "isolated";            // Default to isolated execution
    }
    
    public CronJob(String name, String schedule, String command) {
        this();
        this.name = name;
        this.schedule = schedule;
        this.command = command;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }
    
    public String getSchedule() {
        return schedule;
    }
    
    public void setSchedule(String schedule) {
        this.schedule = schedule;
        this.updatedAt = Instant.now();
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
        this.updatedAt = Instant.now();
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this.updatedAt = Instant.now();
    }
    
    public JobStatus getStatus() {
        return status;
    }
    
    public void setStatus(JobStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public Instant getLastRun() {
        return lastRun;
    }
    
    public void setLastRun(Instant lastRun) {
        this.lastRun = lastRun;
        this.updatedAt = Instant.now();
    }
    
    public Instant getNextRun() {
        return nextRun;
    }
    
    public void setNextRun(Instant nextRun) {
        this.nextRun = nextRun;
        this.updatedAt = Instant.now();
    }
    
    public int getRunCount() {
        return runCount;
    }
    
    public void incrementRunCount() {
        this.runCount++;
        this.updatedAt = Instant.now();
    }
    
    public int getFailCount() {
        return failCount;
    }
    
    public void incrementFailCount() {
        this.failCount++;
        this.updatedAt = Instant.now();
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        this.updatedAt = Instant.now();
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        this.updatedAt = Instant.now();
    }
    
    public long getRetryDelayMs() {
        return retryDelayMs;
    }
    
    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
        this.updatedAt = Instant.now();
    }
    
    public boolean isIsolated() {
        return isolated;
    }
    
    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
        this.updatedAt = Instant.now();
    }
    
    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.updatedAt = Instant.now();
    }
    
    public String getWorkingDirectory() {
        return workingDirectory;
    }
    
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.updatedAt = Instant.now();
    }
    
    public WakeMode getWakeMode() {
        return wakeMode;
    }
    
    public void setWakeMode(WakeMode wakeMode) {
        this.wakeMode = wakeMode;
        this.updatedAt = Instant.now();
    }
    
    public String getSessionTarget() {
        return sessionTarget;
    }
    
    public void setSessionTarget(String sessionTarget) {
        this.sessionTarget = sessionTarget;
        this.updatedAt = Instant.now();
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
        this.updatedAt = Instant.now();
    }
    
    public String getSessionKey() {
        return sessionKey;
    }
    
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
        this.updatedAt = Instant.now();
    }
    
    public String getDeliveryChannel() {
        return deliveryChannel;
    }
    
    public void setDeliveryChannel(String deliveryChannel) {
        this.deliveryChannel = deliveryChannel;
        this.updatedAt = Instant.now();
    }
    
    public String getDeliveryTo() {
        return deliveryTo;
    }
    
    public void setDeliveryTo(String deliveryTo) {
        this.deliveryTo = deliveryTo;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if the job can be transitioned to the given status.
     */
    public boolean canTransitionTo(JobStatus newStatus) {
        return JobStatusMachine.isValidTransition(this.status, newStatus);
    }
    
    /**
     * Transition to a new status.
     * 
     * @throws IllegalStateException if transition is not valid
     */
    public void transitionTo(JobStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition: %s -> %s", this.status, newStatus)
            );
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CronJob cronJob = (CronJob) o;
        return Objects.equals(id, cronJob.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("CronJob{id='%s', name='%s', status=%s, schedule='%s', wakeMode=%s}", 
            id, name, status, schedule, wakeMode);
    }
    
    /**
     * Wake mode for cron job execution.
     */
    public enum WakeMode {
        /**
         * Execute immediately when triggered.
         */
        NOW("now"),
        
        /**
         * Wait for next heartbeat to execute.
         */
        NEXT_HEARTBEAT("next-heartbeat");
        
        private final String value;
        
        WakeMode(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static WakeMode fromString(String value) {
            if (value == null) {
                return NEXT_HEARTBEAT;
            }
            for (WakeMode mode : values()) {
                if (mode.value.equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            return NEXT_HEARTBEAT;
        }
    }
}
