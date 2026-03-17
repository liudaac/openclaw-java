package openclaw.cron.service;

import openclaw.agent.heartbeat.HeartbeatService;
import openclaw.cron.executor.IsolatedJobExecutor;
import openclaw.cron.executor.JobExecutor;
import openclaw.cron.model.CronJob;
import openclaw.cron.model.CronJob.WakeMode;
import openclaw.cron.model.JobExecution;
import openclaw.cron.model.JobStatus;
import openclaw.cron.scheduler.CronExpressionParser;
import openclaw.cron.store.CronJobStore;
import openclaw.cron.store.SQLiteCronJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Main service for cron job management.
 * 
 * <p>Provides high-level API for scheduling, executing, and managing cron jobs.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
@Service
public class CronService {
    
    private static final Logger logger = LoggerFactory.getLogger(CronService.class);
    
    private final CronJobStore store;
    private final JobExecutor executor;
    private final CronExpressionParser parser;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks;
    
    private HeartbeatService heartbeatService;
    
    public CronService() {
        this.store = new SQLiteCronJobStore(Paths.get("data/cron"));
        this.executor = new IsolatedJobExecutor();
        this.parser = new CronExpressionParser();
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.scheduledTasks = new ConcurrentHashMap<>();
    }
    
    @Autowired(required = false)
    public void setHeartbeatService(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
        logger.info("Heartbeat service injected into CronService");
    }
    
    @PostConstruct
    public void initialize() {
        store.initialize().join();
        loadAndScheduleActiveJobs();
        logger.info("Cron service initialized");
    }
    
    @PreDestroy
    public void shutdown() {
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduler.shutdown();
        store.close().join();
        logger.info("Cron service shutdown");
    }
    
    /**
     * Create and schedule a new cron job.
     */
    public CompletableFuture<CronJob> createJob(String name, String schedule, String command) {
        CronJob job = new CronJob(name, schedule, command);
        return store.save(job)
            .thenApply(v -> {
                scheduleJob(job);
                return job;
            });
    }
    
    /**
     * Get all jobs.
     */
    public CompletableFuture<List<CronJob>> listJobs() {
        return store.findAll();
    }
    
    /**
     * Get job by ID.
     */
    public CompletableFuture<Optional<CronJob>> getJob(String jobId) {
        return store.findById(jobId);
    }
    
    /**
     * Cancel and delete a job.
     */
    public CompletableFuture<Boolean> deleteJob(String jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
        return store.updateStatus(jobId, JobStatus.CANCELLED)
            .thenCompose(v -> store.delete(jobId));
    }
    
    /**
     * Pause a job.
     */
    public CompletableFuture<Void> pauseJob(String jobId) {
        ScheduledFuture<?> future = scheduledTasks.get(jobId);
        if (future != null) {
            future.cancel(false);
        }
        return store.updateStatus(jobId, JobStatus.PAUSED);
    }
    
    /**
     * Resume a paused job.
     */
    public CompletableFuture<Void> resumeJob(String jobId) {
        return store.findById(jobId)
            .thenCompose(opt -> {
                if (opt.isPresent()) {
                    CronJob job = opt.get();
                    job.setStatus(JobStatus.PENDING);
                    return store.save(job)
                        .thenRun(() -> scheduleJob(job));
                }
                return CompletableFuture.completedFuture(null);
            });
    }
    
    /**
     * Trigger a job immediately.
     */
    public CompletableFuture<JobExecution> triggerJob(String jobId) {
        return store.findById(jobId)
            .thenCompose(opt -> {
                if (opt.isPresent()) {
                    return executeJob(opt.get());
                }
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Job not found: " + jobId));
            });
    }
    
    /**
     * Get job execution history.
     */
    public CompletableFuture<List<JobExecution>> getJobHistory(String jobId, int limit) {
        return store.findExecutionsByJob(jobId, limit);
    }

    /**
     * Get execution statistics.
     */
    public CompletableFuture<Map<String, Object>> getExecutionStats(String jobId) {
        return store.findById(jobId)
            .thenCompose(opt -> {
                if (opt.isPresent()) {
                    CronJob job = opt.get();
                    return getJobHistory(jobId, 100)
                        .thenApply(history -> {
                            long successCount = history.stream().filter(JobExecution::isSuccess).count();
                            long failCount = history.size() - successCount;
                            return Map.<String, Object>of(
                                "jobId", jobId,
                                "runCount", job.getRunCount(),
                                "failCount", job.getFailCount(),
                                "successCount", successCount,
                                "failCount", failCount,
                                "totalExecutions", history.size()
                            );
                        });
                }
                return CompletableFuture.completedFuture(Map.<String, Object>of());
            });
    }
    
    // Private methods
    
    private void loadAndScheduleActiveJobs() {
        store.findActive().thenAccept(jobs -> {
            jobs.forEach(this::scheduleJob);
            logger.info("Loaded and scheduled {} active jobs", jobs.size());
        }).join();
    }
    
    private void scheduleJob(CronJob job) {
        if (job.getStatus() == JobStatus.PAUSED || job.getStatus() == JobStatus.CANCELLED) {
            return;
        }
        
        // Calculate next run
        parser.getNextExecution(job.getSchedule(), job.getTimezone())
            .ifPresent(nextRun -> {
                long delay = nextRun.toInstant().toEpochMilli() - System.currentTimeMillis();
                if (delay < 0) delay = 0;
                
                ScheduledFuture<?> future = scheduler.schedule(
                    () -> runJob(job),
                    delay,
                    TimeUnit.MILLISECONDS
                );
                scheduledTasks.put(job.getId(), future);
                
                store.updateNextRun(job.getId(), nextRun.toInstant());
                logger.debug("Scheduled job {} to run at {}", job.getId(), nextRun);
            });
    }
    
    private void runJob(CronJob job) {
        logger.info("Running job: {}", job.getName());
        
        // Check wake mode
        if (job.getWakeMode() == WakeMode.NEXT_HEARTBEAT && heartbeatService != null) {
            logger.debug("Job {} scheduled for next heartbeat", job.getId());
            // Request heartbeat execution
            heartbeatService.requestHeartbeatNow(
                "cron:" + job.getId(),
                job.getAgentId(),
                job.getSessionKey()
            ).thenRun(() -> {
                // Update status and reschedule
                store.updateStatus(job.getId(), JobStatus.PENDING)
                    .thenRun(() -> scheduleJob(job));
            });
            return;
        }
        
        // Execute immediately (NOW mode or no heartbeat service)
        store.updateStatus(job.getId(), JobStatus.RUNNING)
            .thenCompose(v -> executeJob(job))
            .thenAccept(execution -> {
                store.saveExecution(execution)
                    .thenRun(() -> {
                        if (execution.isSuccess()) {
                            store.incrementRunCount(job.getId());
                            store.updateStatus(job.getId(), JobStatus.PENDING);
                        } else {
                            store.incrementFailCount(job.getId());
                            handleFailedJob(job, execution);
                        }
                        // Reschedule
                        scheduleJob(job);
                    });
            })
            .exceptionally(ex -> {
                logger.error("Job execution failed", ex);
                return null;
            });
    }
    
    private CompletableFuture<JobExecution> executeJob(CronJob job) {
        return executor.execute(job);
    }
    
    private void handleFailedJob(CronJob job, JobExecution execution) {
        if (job.getFailCount() < job.getMaxRetries()) {
            logger.warn("Job {} failed, will retry (attempt {}/{})",
                job.getName(), job.getFailCount(), job.getMaxRetries());
            store.updateStatus(job.getId(), JobStatus.PENDING);
        } else {
            logger.error("Job {} failed after {} retries", job.getName(), job.getMaxRetries());
            store.updateStatus(job.getId(), JobStatus.FAILED);
        }
    }
}
