package openclaw.server.controller;

import openclaw.cron.model.CronJob;
import openclaw.cron.model.JobExecution;
import openclaw.cron.service.CronService;
import openclaw.cron.store.CronJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cron REST API Controller
 *
 * <p>Provides HTTP endpoints for cron job management.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
@RestController
@RequestMapping("/api/v1/cron")
public class CronController {

    private static final Logger logger = LoggerFactory.getLogger(CronController.class);

    private final CronService cronService;

    public CronController(CronService cronService) {
        this.cronService = cronService;
    }

    /**
     * List all cron jobs
     */
    @GetMapping("/jobs")
    public Mono<List<JobResponse>> listJobs() {
        return Mono.fromFuture(cronService.listJobs())
                .map(jobs -> jobs.stream()
                        .map(this::toJobResponse)
                        .collect(Collectors.toList()));
    }

    /**
     * Get a specific job
     */
    @GetMapping("/jobs/{jobId}")
    public Mono<JobResponse> getJob(@PathVariable String jobId) {
        return Mono.fromFuture(cronService.getJob(jobId))
                .flatMap(opt -> opt.map(job -> Mono.just(toJobResponse(job)))
                        .orElse(Mono.error(new JobNotFoundException(jobId))));
    }

    /**
     * Create a new cron job
     */
    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<JobResponse> createJob(@RequestBody CreateJobRequest request) {
        logger.info("Creating cron job: {}", request.name());
        
        return Mono.fromFuture(cronService.createJob(request.name(), request.schedule(), request.command()))
                .map(this::toJobResponse);
    }

    /**
     * Delete a cron job
     */
    @DeleteMapping("/jobs/{jobId}")
    public Mono<Void> deleteJob(@PathVariable String jobId) {
        return Mono.fromFuture(cronService.deleteJob(jobId))
                .flatMap(deleted -> {
                    if (deleted) {
                        return Mono.empty();
                    }
                    return Mono.error(new JobNotFoundException(jobId));
                });
    }

    /**
     * Pause a cron job
     */
    @PostMapping("/jobs/{jobId}/pause")
    public Mono<JobResponse> pauseJob(@PathVariable String jobId) {
        return Mono.fromFuture(cronService.pauseJob(jobId))
                .then(Mono.fromFuture(cronService.getJob(jobId)))
                .flatMap(opt -> opt.map(job -> Mono.just(toJobResponse(job)))
                        .orElse(Mono.error(new JobNotFoundException(jobId))));
    }

    /**
     * Resume a paused cron job
     */
    @PostMapping("/jobs/{jobId}/resume")
    public Mono<JobResponse> resumeJob(@PathVariable String jobId) {
        return Mono.fromFuture(cronService.resumeJob(jobId))
                .then(Mono.fromFuture(cronService.getJob(jobId)))
                .flatMap(opt -> opt.map(job -> Mono.just(toJobResponse(job)))
                        .orElse(Mono.error(new JobNotFoundException(jobId))));
    }

    /**
     * Trigger a job immediately
     */
    @PostMapping("/jobs/{jobId}/trigger")
    public Mono<ExecutionResponse> triggerJob(@PathVariable String jobId) {
        return Mono.fromFuture(cronService.triggerJob(jobId))
                .map(this::toExecutionResponse);
    }

    /**
     * Get job execution history
     */
    @GetMapping("/jobs/{jobId}/history")
    public Mono<List<ExecutionResponse>> getJobHistory(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "10") int limit) {
        return Mono.fromFuture(cronService.getJobHistory(jobId, limit))
                .map(executions -> executions.stream()
                        .map(this::toExecutionResponse)
                        .collect(Collectors.toList()));
    }

    /**
     * Get job execution statistics
     */
    @GetMapping("/jobs/{jobId}/stats")
    public Mono<StatsResponse> getJobStats(@PathVariable String jobId) {
        return Mono.fromFuture(cronService.getExecutionStats(jobId))
                .map(stats -> {
                    long totalRuns = ((Number) stats.getOrDefault("totalRuns", 0)).longValue();
                    long successfulRuns = ((Number) stats.getOrDefault("successfulRuns", 0)).longValue();
                    long failedRuns = ((Number) stats.getOrDefault("failedRuns", 0)).longValue();
                    long averageDurationMs = ((Number) stats.getOrDefault("averageDurationMs", 0)).longValue();
                    
                    return new StatsResponse(
                        (int) totalRuns,
                        (int) successfulRuns,
                        (int) failedRuns,
                        totalRuns > 0 ? (double) successfulRuns / totalRuns : 0,
                        (double) averageDurationMs,
                        stats.getOrDefault("lastRun", null)
                    );
                });
    }

    // Helper methods

    private JobResponse toJobResponse(CronJob job) {
        return new JobResponse(
                job.getId(),
                job.getName(),
                job.getSchedule(),
                job.getCommand(),
                job.getTimezone(),
                job.getStatus().name(),
                job.getRunCount(),
                job.getFailCount(),
                job.getLastRun() != null ? job.getLastRun().toString() : null,
                job.getNextRun() != null ? job.getNextRun().toString() : null,
                job.getCreatedAt().toString()
        );
    }

    private ExecutionResponse toExecutionResponse(JobExecution execution) {
        return new ExecutionResponse(
                execution.getId(),
                execution.getJobId(),
                execution.getStartTime().toString(),
                execution.getEndTime() != null ? execution.getEndTime().toString() : null,
                execution.getStatus().name(),
                execution.getExitCode(),
                execution.getDuration().toMillis(),
                execution.getOutput(),
                execution.getError()
        );
    }

    // Request/Response Records

    public record CreateJobRequest(
            String name,
            String schedule,
            String command,
            String timezone,
            Integer maxRetries,
            Long timeoutSeconds
    ) {}

    public record JobResponse(
            String id,
            String name,
            String schedule,
            String command,
            String timezone,
            String status,
            int runCount,
            int failCount,
            String lastRun,
            String nextRun,
            String createdAt
    ) {}

    public record ExecutionResponse(
            String id,
            String jobId,
            String startTime,
            String endTime,
            String status,
            int exitCode,
            long durationMs,
            String output,
            String error
    ) {}

    public record StatsResponse(
            int totalRuns,
            int successfulRuns,
            int failedRuns,
            double successRate,
            double averageDurationMs,
            Object lastRun
    ) {}

    // Exceptions

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class JobNotFoundException extends RuntimeException {
        public JobNotFoundException(String jobId) {
            super("Job not found: " + jobId);
        }
    }
}
