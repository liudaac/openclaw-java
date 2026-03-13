package openclaw.cron.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single execution of a cron job.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class JobExecution {
    
    private final String id;
    private final String jobId;
    private final Instant startTime;
    private Instant endTime;
    private JobExecutionStatus status;
    private String output;
    private String error;
    private int exitCode;
    private int retryAttempt;
    private String executorHost;
    private long memoryUsageBytes;
    private Duration cpuTime;
    
    public JobExecution(String jobId) {
        this.id = UUID.randomUUID().toString();
        this.jobId = jobId;
        this.startTime = Instant.now();
        this.status = JobExecutionStatus.RUNNING;
        this.retryAttempt = 0;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public JobExecutionStatus getStatus() {
        return status;
    }
    
    public void setStatus(JobExecutionStatus status) {
        this.status = status;
    }
    
    public String getOutput() {
        return output;
    }
    
    public void setOutput(String output) {
        this.output = output;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public int getExitCode() {
        return exitCode;
    }
    
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
    
    public int getRetryAttempt() {
        return retryAttempt;
    }
    
    public void setRetryAttempt(int retryAttempt) {
        this.retryAttempt = retryAttempt;
    }
    
    public String getExecutorHost() {
        return executorHost;
    }
    
    public void setExecutorHost(String executorHost) {
        this.executorHost = executorHost;
    }
    
    public long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }
    
    public void setMemoryUsageBytes(long memoryUsageBytes) {
        this.memoryUsageBytes = memoryUsageBytes;
    }
    
    public Duration getCpuTime() {
        return cpuTime;
    }
    
    public void setCpuTime(Duration cpuTime) {
        this.cpuTime = cpuTime;
    }
    
    /**
     * Complete the execution with success.
     */
    public void complete(String output, int exitCode) {
        this.endTime = Instant.now();
        this.status = JobExecutionStatus.SUCCESS;
        this.output = output;
        this.exitCode = exitCode;
    }
    
    /**
     * Complete the execution with failure.
     */
    public void fail(String error, int exitCode) {
        this.endTime = Instant.now();
        this.status = JobExecutionStatus.FAILED;
        this.error = error;
        this.exitCode = exitCode;
    }
    
    /**
     * Get the duration of this execution.
     */
    public Duration getDuration() {
        if (endTime == null) {
            return Duration.between(startTime, Instant.now());
        }
        return Duration.between(startTime, endTime);
    }
    
    /**
     * Check if the execution was successful.
     */
    public boolean isSuccess() {
        return status == JobExecutionStatus.SUCCESS;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobExecution that = (JobExecution) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("JobExecution{id='%s', jobId='%s', status=%s, duration=%s}",
            id, jobId, status, getDuration());
    }
    
    /**
     * Execution status enumeration.
     */
    public enum JobExecutionStatus {
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED
    }
}
