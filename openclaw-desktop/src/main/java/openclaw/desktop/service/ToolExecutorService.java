package openclaw.desktop.service;

import javafx.application.Platform;
import openclaw.browser.BrowserService;
import openclaw.browser.model.BrowserSession;
import openclaw.browser.model.SessionOptions;
import openclaw.cron.model.CronJob;
import openclaw.cron.service.CronService;
import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import openclaw.session.service.SessionPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Tool Executor Service for Desktop Application.
 *
 * <p>Provides direct access to OpenClaw tools without REST API.
 * Supports Cron, Browser, Session, and custom tools.</p>
 */
@Service
public class ToolExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(ToolExecutorService.class);

    @Autowired
    private CronService cronService;

    @Autowired
    private BrowserService browserService;

    @Autowired
    private SessionPersistenceService sessionService;

    @Autowired
    private List<AgentTool> availableTools;

    // ==================== Tool Discovery ====================

    /**
     * Get all available tools.
     */
    public List<ToolInfo> getAvailableTools() {
        return availableTools.stream()
            .map(tool -> new ToolInfo(
                tool.getName(),
                tool.getDescription(),
                tool.getParameters()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get tool by name.
     */
    public ToolInfo getTool(String name) {
        return availableTools.stream()
            .filter(t -> t.getName().equals(name))
            .findFirst()
            .map(tool -> new ToolInfo(
                tool.getName(),
                tool.getDescription(),
                tool.getParameters()
            ))
            .orElse(null);
    }

    // ==================== Generic Tool Execution ====================

    /**
     * Execute a tool by name with arguments.
     */
    public CompletableFuture<ToolExecutionResult> executeTool(String toolName, 
                                                               Map<String, Object> arguments) {
        AgentTool tool = availableTools.stream()
            .filter(t -> t.getName().equals(toolName))
            .findFirst()
            .orElse(null);

        if (tool == null) {
            return CompletableFuture.completedFuture(
                ToolExecutionResult.failure("Tool not found: " + toolName)
            );
        }

        ToolExecuteContext context = ToolExecuteContext.builder()
            .toolName(toolName)
            .arguments(arguments)
            .build();

        return CompletableFuture.supplyAsync(() -> {
            try {
                ToolResult result = tool.execute(context).join();
                return ToolExecutionResult.from(result);
            } catch (Exception e) {
                logger.error("Tool execution failed: {}", toolName, e);
                return ToolExecutionResult.failure(e.getMessage());
            }
        });
    }

    // ==================== Cron Tool Operations ====================

    /**
     * Create a cron job.
     */
    public CompletableFuture<CronJobInfo> createCronJob(String name, String schedule, 
                                                         String command, String timezone) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CronJob job = cronService.createJob(name, schedule, command).join();
                
                if (timezone != null) {
                    job.setTimezone(timezone);
                }

                return CronJobInfo.from(job);
            } catch (Exception e) {
                logger.error("Failed to create cron job", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * List all cron jobs.
     */
    public CompletableFuture<List<CronJobInfo>> listCronJobs() {
        return CompletableFuture.supplyAsync(() -> {
            List<CronJob> jobs = cronService.listJobs().join();
            return jobs.stream()
                .map(CronJobInfo::from)
                .collect(Collectors.toList());
        });
    }

    /**
     * Get cron job details.
     */
    public CompletableFuture<CronJobInfo> getCronJob(String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            CronJob job = cronService.getJob(jobId).join()
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            return CronJobInfo.from(job);
        });
    }

    /**
     * Pause a cron job.
     */
    public CompletableFuture<Void> pauseCronJob(String jobId) {
        return CompletableFuture.runAsync(() -> {
            cronService.pauseJob(jobId).join();
        });
    }

    /**
     * Resume a cron job.
     */
    public CompletableFuture<Void> resumeCronJob(String jobId) {
        return CompletableFuture.runAsync(() -> {
            cronService.resumeJob(jobId).join();
        });
    }

    /**
     * Delete a cron job.
     */
    public CompletableFuture<Boolean> deleteCronJob(String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            return cronService.deleteJob(jobId).join();
        });
    }

    /**
     * Trigger a cron job immediately.
     */
    public CompletableFuture<CronExecutionInfo> triggerCronJob(String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            var execution = cronService.triggerJob(jobId).join();
            return new CronExecutionInfo(
                execution.getId(),
                execution.getStatus().name(),
                execution.getOutput(),
                execution.getDuration().toMillis()
            );
        });
    }

    // ==================== Browser Tool Operations ====================

    /**
     * Create a browser session.
     */
    public CompletableFuture<BrowserSessionInfo> createBrowserSession(String name, 
                                                                       BrowserSession.SessionOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            BrowserSession session = browserService.createSession(name, options).join();
            return new BrowserSessionInfo(
                session.getId(),
                session.getName(),
                session.getStatus().name(),
                session.getCreatedAt()
            );
        });
    }

    /**
     * Navigate to URL.
     */
    public CompletableFuture<Void> navigate(String sessionId, String url) {
        return CompletableFuture.runAsync(() -> {
            browserService.navigate(sessionId, url).join();
        });
    }

    /**
     * Take screenshot.
     */
    public CompletableFuture<byte[]> takeScreenshot(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            return browserService.screenshot(sessionId).join();
        });
    }

    /**
     * Execute JavaScript.
     */
    public CompletableFuture<String> executeJavaScript(String sessionId, String script) {
        return CompletableFuture.supplyAsync(() -> {
            return browserService.evaluate(sessionId, script).join();
        });
    }

    /**
     * Close browser session.
     */
    public CompletableFuture<Void> closeBrowserSession(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            browserService.closeSession(sessionId).join();
        });
    }

    /**
     * List browser sessions.
     */
    public CompletableFuture<List<BrowserSessionInfo>> listBrowserSessions() {
        return CompletableFuture.supplyAsync(() -> {
            return browserService.listSessions().join().values().stream()
                .map(s -> new BrowserSessionInfo(
                    s.getId(),
                    s.getName(),
                    s.getStatus().name(),
                    s.getCreatedAt()
                ))
                .toList();
        });
    }

    // ==================== Session Operations ====================

    /**
     * Search sessions.
     */
    public CompletableFuture<List<SessionInfo>> searchSessions(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            return sessionService.searchSessions(keyword).join().stream()
                .map(s -> new SessionInfo(
                    s.getId(),
                    s.getSessionKey(),
                    s.getStatus().name(),
                    s.getCreatedAt(),
                    s.getLastActivityAt()
                ))
                .collect(Collectors.toList());
        });
    }

    /**
     * Archive session.
     */
    public CompletableFuture<Void> archiveSession(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            sessionService.archiveSession(sessionId).join();
        });
    }

    // ==================== Data Classes ====================

    public record ToolInfo(String name, String description, AgentTool.ToolParameters parameters) {}

    public record ToolExecutionResult(boolean success, String output, String error, 
                                       Map<String, Object> metadata) {
        public static ToolExecutionResult from(openclaw.sdk.tool.ToolResult result) {
            return new ToolExecutionResult(
                result.success(),
                result.content().orElse(null),
                result.error().orElse(null),
                result.metadata()
            );
        }

        public static ToolExecutionResult failure(String error) {
            return new ToolExecutionResult(false, null, error, Map.of());
        }
    }

    public record CronJobInfo(String id, String name, String schedule, String command,
                               String status, String timezone, int runCount, int failCount) {
        public static CronJobInfo from(CronJob job) {
            return new CronJobInfo(
                job.getId(),
                job.getName(),
                job.getSchedule(),
                job.getCommand(),
                job.getStatus().name(),
                job.getTimezone(),
                job.getRunCount(),
                job.getFailCount()
            );
        }
    }

    public record CronExecutionInfo(String executionId, String status, String output, long durationMs) {}

    public record BrowserSessionInfo(String id, String name, String status, java.time.Instant createdAt) {}

    public record SessionInfo(String id, String sessionKey, String status, 
                               java.time.Instant createdAt, java.time.Instant lastActivity) {}
}
