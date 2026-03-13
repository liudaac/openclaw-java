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
        return String.format("CronJob{id='%s', name='%s', status=%s, schedule='%s'}", 
            id, name, status, schedule);
    }
}
