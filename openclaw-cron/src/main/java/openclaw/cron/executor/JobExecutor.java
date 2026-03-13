package openclaw.cron.executor;

import openclaw.cron.model.CronJob;
import openclaw.cron.model.JobExecution;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for job execution.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public interface JobExecutor {
    
    /**
     * Execute a cron job.
     *
     * @param job the job to execute
     * @return the execution result
     */
    CompletableFuture<JobExecution> execute(CronJob job);
}
