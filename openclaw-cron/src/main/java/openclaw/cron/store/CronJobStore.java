package openclaw.cron.store;

import openclaw.cron.model.CronJob;
import openclaw.cron.model.JobExecution;
import openclaw.cron.model.JobStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for cron job persistence storage.
 * 
 * <p>Equivalent to Node.js src/cron/store.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public interface CronJobStore {
    
    /**
     * Initialize the store.
     */
    CompletableFuture<Void> initialize();
    
    /**
     * Save a cron job.
     *
     * @param job the job to save
     * @return completion future
     */
    CompletableFuture<Void> save(CronJob job);
    
    /**
     * Find a job by ID.
     *
     * @param jobId the job ID
     * @return the job if found
     */
    CompletableFuture<Optional<CronJob>> findById(String jobId);
    
    /**
     * Find a job by name.
     *
     * @param name the job name
     * @return the job if found
     */
    CompletableFuture<Optional<CronJob>> findByName(String name);
    
    /**
     * Get all jobs.
     *
     * @return list of all jobs
     */
    CompletableFuture<List<CronJob>> findAll();
    
    /**
     * Get jobs by status.
     *
     * @param status the status to filter by
     * @return list of jobs with the given status
     */
    CompletableFuture<List<CronJob>> findByStatus(JobStatus status);
    
    /**
     * Get active jobs (PENDING, RUNNING, PAUSED).
     *
     * @return list of active jobs
     */
    CompletableFuture<List<CronJob>> findActive();
    
    /**
     * Delete a job by ID.
     *
     * @param jobId the job ID
     * @return true if deleted
     */
    CompletableFuture<Boolean> delete(String jobId);
    
    /**
     * Update job status.
     *
     * @param jobId the job ID
     * @param status the new status
     * @return completion future
     */
    CompletableFuture<Void> updateStatus(String jobId, JobStatus status);
    
    /**
     * Update job next run time.
     *
     * @param jobId the job ID
     * @param nextRun the next run time
     * @return completion future
     */
    CompletableFuture<Void> updateNextRun(String jobId, java.time.Instant nextRun);
    
    /**
     * Increment run count.
     *
     * @param jobId the job ID
     * @return completion future
     */
    CompletableFuture<Void> incrementRunCount(String jobId);
    
    /**
     * Increment fail count.
     *
     * @param jobId the job ID
     * @return completion future
     */
    CompletableFuture<Void> incrementFailCount(String jobId);
    
    // Job Execution methods
    
    /**
     * Save a job execution.
     *
     * @param execution the execution to save
     * @return completion future
     */
    CompletableFuture<Void> saveExecution(JobExecution execution);
    
    /**
     * Find execution by ID.
     *
     * @param executionId the execution ID
     * @return the execution if found
     */
    CompletableFuture<Optional<JobExecution>> findExecutionById(String executionId);
    
    /**
     * Get executions for a job.
     *
     * @param jobId the job ID
     * @param limit maximum number of executions
     * @return list of executions
     */
    CompletableFuture<List<JobExecution>> findExecutionsByJob(String jobId, int limit);
    
    /**
     * Get recent executions.
     *
     * @param limit maximum number of executions
     * @return list of recent executions
     */
    CompletableFuture<List<JobExecution>> findRecentExecutions(int limit);
    
    /**
     * Get execution statistics for a job.
     *
     * @param jobId the job ID
     * @return execution statistics
     */
    CompletableFuture<ExecutionStats> getExecutionStats(String jobId);
    
    /**
     * Close the store.
     */
    CompletableFuture<Void> close();
    
    /**
     * Execution statistics.
     */
    record ExecutionStats(
        int totalRuns,
        int successfulRuns,
        int failedRuns,
        double averageDurationMs,
        java.time.Instant lastRun
    ) {}
}
