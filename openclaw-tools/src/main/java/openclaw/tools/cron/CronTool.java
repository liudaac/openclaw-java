package openclaw.tools.cron;

import openclaw.cron.model.CronJob;
import openclaw.cron.model.JobExecution;
import openclaw.cron.model.JobStatus;
import openclaw.cron.service.CronService;
import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Cron job scheduling tool - Enhanced Version.
 *
 * <p>Uses the new openclaw-cron module for persistent job management.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class CronTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(CronTool.class);

    private final CronService cronService;

    public CronTool() {
        this.cronService = new CronService();
        this.cronService.initialize();
    }

    @Override
    public String getName() {
        return "cron";
    }

    @Override
    public String getDescription() {
        return "Schedule and manage recurring tasks using cron expressions (persistent)";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "action", PropertySchema.enum_("Cron action", List.of(
                                "schedule", "list", "get", "cancel", "pause", "resume", 
                                "trigger", "history", "stats"
                        )),
                        "job_id", PropertySchema.string("Job ID (for get/cancel/pause/resume/trigger/history)"),
                        "name", PropertySchema.string("Job name (for schedule)"),
                        "cron", PropertySchema.string("Cron expression (e.g., '0 0 * * *') or @daily/@weekly/@monthly/@hourly"),
                        "command", PropertySchema.string("Command to execute"),
                        "timezone", PropertySchema.string("Timezone (default: UTC)"),
                        "max_retries", PropertySchema.integer("Max retry attempts (default: 3)"),
                        "timeout", PropertySchema.integer("Timeout in seconds (default: 60)")
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
                    case "get":
                        return getJob(args);
                    case "cancel":
                        return cancelJob(args);
                    case "pause":
                        return pauseJob(args);
                    case "resume":
                        return resumeJob(args);
                    case "trigger":
                        return triggerJob(args);
                    case "history":
                        return showHistory(args);
                    case "stats":
                        return showStats(args);
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }

            } catch (Exception e) {
                logger.error("Cron operation failed", e);
                return ToolResult.failure("Cron operation failed: " + e.getMessage());
            }
        });
    }

    private ToolResult scheduleJob(Map<String, Object> args) {
        if (!args.containsKey("command")) {
            return ToolResult.failure("Missing required parameter: command");
        }
        if (!args.containsKey("cron")) {
            return ToolResult.failure("Missing required parameter: cron");
        }

        String command = args.get("command").toString();
        String schedule = args.get("cron").toString();
        String name = args.getOrDefault("name", "unnamed-job").toString();
        String timezone = args.getOrDefault("timezone", ZoneId.systemDefault().getId()).toString();

        // Handle predefined macros
        if (schedule.startsWith("@")) {
            schedule = convertPredefinedMacro(schedule);
        }

        try {
            CronJob job = cronService.createJob(name, schedule, command).join();
            
            // Apply additional config
            if (args.containsKey("max_retries")) {
                job.setMaxRetries((Integer) args.get("max_retries"));
            }
            if (args.containsKey("timeout")) {
                job.setTimeoutSeconds((Integer) args.get("timeout"));
            }
            job.setTimezone(timezone);
            
            return ToolResult.success(
                    "Scheduled job: " + name,
                    Map.of(
                            "job_id", job.getId(),
                            "name", job.getName(),
                            "schedule", job.getSchedule(),
                            "timezone", job.getTimezone(),
                            "status", job.getStatus().name(),
                            "next_run", job.getNextRun() != null ? job.getNextRun().toString() : "calculating..."
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Failed to schedule job: " + e.getMessage());
        }
    }

    private ToolResult listJobs() {
        try {
            List<CronJob> jobs = cronService.listJobs().join();
            
            List<Map<String, Object>> jobList = jobs.stream()
                    .map(job -> Map.of(
                            "job_id", job.getId(),
                            "name", job.getName(),
                            "schedule", job.getSchedule(),
                            "status", job.getStatus().name(),
                            "run_count", job.getRunCount(),
                            "fail_count", job.getFailCount(),
                            "last_run", job.getLastRun() != null ? job.getLastRun().toString() : "never",
                            "next_run", job.getNextRun() != null ? job.getNextRun().toString() : "N/A"
                    ))
                    .collect(Collectors.toList());

            return ToolResult.success(
                    "Found " + jobList.size() + " job(s)",
                    Map.of("jobs", jobList)
            );
        } catch (Exception e) {
            return ToolResult.failure("Failed to list jobs: " + e.getMessage());
        }
    }

    private ToolResult getJob(Map<String, Object> args) {
        if (!args.containsKey("job_id")) {
            return ToolResult.failure("Missing required parameter: job_id");
        }

        String jobId = args.get("job_id").toString();
        
        try {
            CronJob job = cronService.getJob(jobId).join()
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

            return ToolResult.success(
                    "Job details",
                    Map.ofEntries(
                            Map.entry("job_id", job.getId()),
                            Map.entry("name", job.getName()),
                            Map.entry("schedule", job.getSchedule()),
                            Map.entry("command", job.getCommand()),
                            Map.entry("timezone", job.getTimezone()),
                            Map.entry("status", job.getStatus().name()),
                            Map.entry("run_count", job.getRunCount()),
                            Map.entry("fail_count", job.getFailCount()),
                            Map.entry("last_run", job.getLastRun() != null ? job.getLastRun().toString() : "never"),
                            Map.entry("next_run", job.getNextRun() != null ? job.getNextRun().toString() : "N/A"),
                            Map.entry("created_at", job.getCreatedAt().toString())
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Failed to get job: " + e.getMessage());
        }
    }

    private ToolResult cancelJob(Map<String, Object> args) {
        if (!args.containsKey("job_id")) {
            return ToolResult.failure("Missing required parameter: job_id");
        }

        String jobId = args.get("job_id").toString();
        
        try {
            boolean deleted = cronService.deleteJob(jobId).join();
            if (deleted) {
                return ToolResult.success("Job cancelled and deleted", Map.of("job_id", jobId));
            } else {
                return ToolResult.failure("Job not found: " + jobId);
            }
        } catch (Exception e) {
            return ToolResult.failure("Failed to cancel job: " + e.getMessage());
        }
    }

    private ToolResult pauseJob(Map<String, Object> args) {
        if (!args.containsKey("job_id")) {
            return ToolResult.failure("Missing required parameter: job_id");
        }

        String jobId = args.get("job_id").toString();
        
        try {
            cronService.pauseJob(jobId).join();
            return ToolResult.success("Job paused", Map.of("job_id", jobId, "status", "PAUSED"));
        } catch (Exception e) {
            return ToolResult.failure("Failed to pause job: " + e.getMessage());
        }
    }

    private ToolResult resumeJob(Map<String, Object> args) {
        if (!args.containsKey("job_id")) {
            return ToolResult.failure("Missing required parameter: job_id");
        }

        String jobId = args.get("job_id").toString();
        
        try {
            cronService.resumeJob(jobId).join();
            return ToolResult.success("Job resumed", Map.of("job_id", jobId, "status", "PENDING"));
        } catch (Exception e) {
            return ToolResult.failure("Failed to resume job: " + e.getMessage());
        }
    }

    private ToolResult triggerJob(Map<String, Object> args) {
        if (!args.containsKey("job_id")) {
            return ToolResult.failure("Missing required parameter: job_id");
        }

        String jobId = args.get("job_id").toString();
        
        try {
            JobExecution execution = cronService.triggerJob(jobId).join();
            return ToolResult.success(
                    "Job triggered",
                    Map.of(
                            "job_id", jobId,
                            "execution_id", execution.getId(),
                            "status", execution.getStatus().name(),
                            "output", execution.getOutput() != null ? execution.getOutput() : "",
                            "duration_ms", execution.getDuration().toMillis()
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Failed to trigger job: " + e.getMessage());
        }
    }

    private ToolResult showHistory(Map<String, Object> args) {
        if (!args.containsKey("job_id")) {
            return ToolResult.failure("Missing required parameter: job_id");
        }

        String jobId = args.get("job_id").toString();
        int limit = (Integer) args.getOrDefault("limit", 10);
        
        try {
            List<JobExecution> executions = cronService.getJobHistory(jobId, limit).join();
            
            List<Map<String, Object>> history = executions.stream()
                    .map(exec -> Map.of(
                            "execution_id", exec.getId(),
                            "start_time", exec.getStartTime().toString(),
                            "status", exec.getStatus().name(),
                            "duration_ms", exec.getDuration().toMillis(),
                            "output", exec.getOutput() != null ? 
                                    exec.getOutput().substring(0, Math.min(200, exec.getOutput().length())) : ""
                    ))
                    .collect(Collectors.toList());

            return ToolResult.success(
                    "Found " + history.size() + " execution(s)",
                    Map.of("history", history)
            );
        } catch (Exception e) {
            return ToolResult.failure("Failed to get history: " + e.getMessage());
        }
    }

    private ToolResult showStats(Map<String, Object> args) {
        if (!args.containsKey("job_id")) {
            return ToolResult.failure("Missing required parameter: job_id");
        }

        String jobId = args.get("job_id").toString();
        
        try {
            Map<String, Object> stats = cronService.getExecutionStats(jobId).join();
            
            return ToolResult.success(
                    "Job statistics",
                    Map.of(
                            "total_runs", stats.get("totalRuns"),
                            "successful_runs", stats.get("successfulRuns"),
                            "failed_runs", stats.get("failedRuns"),
                            "success_rate", (int) stats.get("totalRuns") > 0 ? 
                                    String.format("%.2f%%", (double) (int) stats.get("successfulRuns") / (int) stats.get("totalRuns") * 100) : "N/A",
                            "average_duration_ms", String.format("%.2f", stats.get("averageDurationMs")),
                            "last_run", stats.get("lastRun") != null ? stats.get("lastRun").toString() : "never"
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Failed to get stats: " + e.getMessage());
        }
    }

    private String convertPredefinedMacro(String macro) {
        return switch (macro.toLowerCase()) {
            case "@yearly", "@annually" -> "0 0 1 1 *";
            case "@monthly" -> "0 0 1 * *";
            case "@weekly" -> "0 0 * * 0";
            case "@daily", "@midnight" -> "0 0 * * *";
            case "@hourly" -> "0 * * * *";
            default -> throw new IllegalArgumentException("Unknown macro: " + macro);
        };
    }
}
