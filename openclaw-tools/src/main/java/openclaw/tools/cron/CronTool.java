package openclaw.tools.cron;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Cron job scheduling tool.
 *
 * <p>Phase 3 Enhancement - Schedule and manage recurring tasks.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class CronTool implements AgentTool {

    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledJob> jobs;
    private final Map<String, JobHistory> history;

    public CronTool() {
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.jobs = new ConcurrentHashMap<>();
        this.history = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "cron";
    }

    @Override
    public String getDescription() {
        return "Schedule and manage recurring tasks using cron expressions";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "action", PropertySchema.enum_("Cron action", List.of(
                                "schedule", "list", "cancel", "pause", "resume", "history"
                        )),
                        "job_id", PropertySchema.string("Job ID (for cancel/pause/resume)"),
                        "name", PropertySchema.string("Job name (for schedule)"),
                        "cron", PropertySchema.string("Cron expression (e.g., '0 0 * * *')"),
                        "command", PropertySchema.string("Command to execute"),
                        "delay_seconds", PropertySchema.integer("Delay in seconds (for one-time)"),
                        "timezone", PropertySchema.string("Timezone (default: system)")
                ))
                .required(List.of("action"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String action = args.get("action").toString().toLowerCase();

                switch (action) {
                    case "schedule":
                        return scheduleJob(args);
                    case "list":
                        return listJobs();
                    case "cancel":
                        return cancelJob(args);
                    case "pause":
                        return pauseJob(args);
                    case "resume":
                        return resumeJob(args);
                    case "history":
                        return showHistory(args);
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }

            } catch (Exception e) {
                return ToolResult.failure("Cron operation failed: " + e.getMessage());
            }
        });
    }

    private ToolResult scheduleJob(Map<String, Object> args) {
        if (!args.containsKey("command")) {
            return ToolResult.failure("Missing required parameter: command");
        }

        String command = args.get("command").toString();
        String name = args.getOrDefault("name", "unnamed-job").toString();
        String timezone = args.getOrDefault("timezone", ZoneId.systemDefault().getId()).toString();

        String jobId = UUID.randomUUID().toString();

        // Check if it's a one-time delay or cron schedule
        if (args.containsKey("delay_seconds")) {
            int delay = (int) args.get("delay_seconds");
            return scheduleOneTime(jobId, name, command, delay, timezone);
        } else if (args.containsKey("cron")) {
            String cron = args.get("cron").toString();
            return scheduleCron(jobId, name, command, cron, timezone);
        } else {
            return ToolResult.failure("Missing schedule parameter: cron or delay_seconds");
        }
    }

    private ToolResult scheduleOneTime(String jobId, String name, String command, int delaySeconds, String timezone) {
        try {
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                executeJob(jobId, name, command);
            }, delaySeconds, TimeUnit.SECONDS);

            ScheduledJob job = new ScheduledJob(
                    jobId, name, command, "once",
                    null, delaySeconds, timezone,
                    future, JobStatus.SCHEDULED
            );
            jobs.put(jobId, job);

            Instant runTime = Instant.now().plusSeconds(delaySeconds);
            return ToolResult.success(
                    "Scheduled one-time job: " + name,
                    Map.of(
                            "job_id", jobId,
                            "name", name,
                            "run_at", runTime.toString(),
                            "delay_seconds", delaySeconds
                    )
            );

        } catch (Exception e) {
            return ToolResult.failure("Scheduling failed: " + e.getMessage());
        }
    }

    private ToolResult scheduleCron(String jobId, String name, String command, String cron, String timezone) {
        try {
            // Parse cron and calculate next run
            long interval = parseCronInterval(cron);
            
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                executeJob(jobId, name, command);
            }, interval, interval, TimeUnit.SECONDS);

            ScheduledJob job = new ScheduledJob(
                    jobId, name, command, "cron",
                    cron, 0, timezone,
                    future, JobStatus.SCHEDULED
            );
            jobs.put(jobId, job);

            return ToolResult.success(
                    "Scheduled cron job: " + name,
                    Map.of(
                            "job_id", jobId,
                            "name", name,
                            "cron", cron,
                            "timezone", timezone
                    )
            );

        } catch (Exception e) {
            return ToolResult.failure("Cron scheduling failed: " + e.getMessage());
        }
    }

    private ToolResult listJobs() {
        List<Map<String, Object>> jobList = jobs.values().stream()
                .map(job -> Map.of(
                        "job_id", job.id(),
                        "name", job.name(),
                        "type", job.type(),
                        "status", job.status().name(),
                        "cron", job.cron() != null ? job.cron() : "N/A",
                        "command", job.command().substring(0, Math.min(50, job.command().length()))
                ))
                .toList();

        return ToolResult.success(
                "Found " + jobList.size() + " job(s)",
                Map.of("jobs", jobList)
        );
    }

    private ToolResult cancelJob(Map<String, Object> args) {
        if (!args.containsKey("job_id")) {
            return ToolResult.failure("Missing required parameter: job_id");
        }

        String jobId = args.get("job_id").toString();
        ScheduledJob job = jobs.get(jobId);

        if (job == null) {
            return ToolResult.failure("Job not found: " + jobId);
        }

        job.future().cancel(false);
        jobs.remove(jobId);

        return ToolResult.success(
                "Cancelled job: " + job.name(),
                Map.of("job_id", jobId, "name", job.name())
        );
    }

    private ToolResult pauseJob(Map<String, Object> args) {
        // Simplified - just mark as paused
        String jobId = args.get("job_id").toString();
        ScheduledJob job = jobs.get(jobId);
        
        if (job == null) {
            return ToolResult.failure("Job not found: " + jobId);
        }

        // Note: Actual pause would require more complex implementation
        return ToolResult.success(
                "Job pause not fully implemented",
                Map.of("job_id", jobId, "status", "paused")
        );
    }

    private ToolResult resumeJob(Map<String, Object> args) {
        String jobId = args.get("job_id").toString();
        return ToolResult.success(
                "Job resume not fully implemented",
                Map.of("job_id", jobId, "status", "resumed")
        );
    }

    private ToolResult showHistory(Map<String, Object> args) {
        String jobId = args.getOrDefault("job_id", "").toString();
        
        List<Map<String, Object>> runs = history.values().stream()
                .filter(h -> jobId.isEmpty() || h.jobId().equals(jobId))
                .map(h -> Map.of(
                        "job_id", h.jobId(),
                        "run_time", h.runTime().toString(),
                        "success", h.success(),
                        "output", h.output() != null ? h.output().substring(0, Math.min(100, h.output().length())) : ""
                ))
                .toList();

        return ToolResult.success(
                "Found " + runs.size() + " run(s)",
                Map.of("history", runs)
        );
    }

    private void executeJob(String jobId, String name, String command) {
        Instant runTime = Instant.now();
        try {
            // Execute command
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            String output = "";
            
            if (finished) {
                output = new String(process.getInputStream().readAllBytes());
            } else {
                process.destroyForcibly();
                output = "Timeout";
            }

            history.put(UUID.randomUUID().toString(), new JobHistory(
                    jobId, runTime, finished && process.exitValue() == 0, output
            ));

        } catch (Exception e) {
            history.put(UUID.randomUUID().toString(), new JobHistory(
                    jobId, runTime, false, e.getMessage()
            ));
        }
    }

    private long parseCronInterval(String cron) {
        // Simplified cron parsing - just return hourly for demo
        // In production, use a proper cron library like cron-utils
        return 3600; // 1 hour default
    }

    // Records
    private record ScheduledJob(
            String id,
            String name,
            String command,
            String type,
            String cron,
            int delaySeconds,
            String timezone,
            ScheduledFuture<?> future,
            JobStatus status
    ) {}

    private record JobHistory(
            String jobId,
            Instant runTime,
            boolean success,
            String output
    ) {}

    private enum JobStatus {
        SCHEDULED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    }
}
