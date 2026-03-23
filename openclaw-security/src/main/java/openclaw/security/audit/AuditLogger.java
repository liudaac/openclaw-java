package openclaw.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Security audit logger for tracking and recording security events.
 *
 * Features:
 * - Asynchronous event logging
 * - Multiple output destinations (file, console, callback)
 * - Event filtering and sampling
 * - Automatic log rotation
 * - Event querying and aggregation
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private final ConcurrentLinkedQueue<AuditEvent> eventQueue;
    private final ExecutorService executor;
    private final List<AuditLogHandler> handlers;
    private final AuditConfig config;
    private volatile boolean running;

    public AuditLogger() {
        this(AuditConfig.defaultConfig());
    }

    public AuditLogger(AuditConfig config) {
        this.config = config;
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.handlers = config.handlers();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "audit-logger");
            t.setDaemon(true);
            return t;
        });
        this.running = true;

        // Start background processing
        executor.submit(this::processEvents);
    }

    /**
     * Logs an audit event asynchronously.
     *
     * @param event the event to log
     * @return CompletableFuture that completes when event is processed
     */
    public CompletableFuture<Void> log(AuditEvent event) {
        return CompletableFuture.runAsync(() -> {
            if (shouldLog(event)) {
                eventQueue.offer(event);
            }
        });
    }

    /**
     * Logs an audit event with builder.
     *
     * @param builder the event builder
     * @return CompletableFuture that completes when event is processed
     */
    public CompletableFuture<Void> log(AuditEvent.Builder builder) {
        return log(builder.build());
    }

    /**
     * Creates a quick log for tool execution.
     *
     * @param toolName the tool name
     * @param action the action
     * @param status the status
     * @param message the message
     * @return CompletableFuture
     */
    public CompletableFuture<Void> logToolExecution(
            String toolName,
            String action,
            AuditEvent.EventStatus status,
            String message
    ) {
        return log(AuditEvent.builder()
                .type(AuditEvent.EventType.TOOL_EXECUTION)
                .toolName(toolName)
                .action(action)
                .status(status)
                .message(message)
                .severity(status == AuditEvent.EventStatus.BLOCKED ?
                        AuditEvent.EventSeverity.WARNING : AuditEvent.EventSeverity.INFO)
        );
    }

    /**
     * Creates a quick log for security violations.
     *
     * @param type the event type
     * @param action the action
     * @param message the message
     * @param details additional details
     * @return CompletableFuture
     */
    public CompletableFuture<Void> logSecurityViolation(
            AuditEvent.EventType type,
            String action,
            String message,
            Map<String, Object> details
    ) {
        return log(AuditEvent.builder()
                .type(type)
                .action(action)
                .status(AuditEvent.EventStatus.BLOCKED)
                .severity(AuditEvent.EventSeverity.WARNING)
                .message(message)
                .details(details)
        );
    }

    /**
     * Queries audit events with filters.
     *
     * @param query the query parameters
     * @return list of matching events
     */
    public List<AuditEvent> query(AuditQuery query) {
        // This is a simplified implementation
        // In production, this would query from persistent storage
        return eventQueue.stream()
                .filter(query::matches)
                .collect(Collectors.toList());
    }

    /**
     * Gets recent events.
     *
     * @param count maximum number of events
     * @return list of recent events
     */
    public List<AuditEvent> getRecentEvents(int count) {
        return eventQueue.stream()
                .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp()))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Gets event statistics.
     *
     * @return statistics map
     */
    public Map<String, Object> getStatistics() {
        long totalEvents = eventQueue.size();
        long criticalEvents = eventQueue.stream()
                .filter(e -> e.severity() == AuditEvent.EventSeverity.CRITICAL)
                .count();
        long blockedEvents = eventQueue.stream()
                .filter(e -> e.status() == AuditEvent.EventStatus.BLOCKED)
                .count();

        return Map.of(
                "total_events", totalEvents,
                "critical_events", criticalEvents,
                "blocked_events", blockedEvents,
                "queue_size", eventQueue.size()
        );
    }

    /**
     * Shuts down the audit logger.
     */
    public void shutdown() {
        running = false;
        executor.shutdown();

        // Process remaining events
        processEvents();

        // Close handlers
        for (AuditLogHandler handler : handlers) {
            try {
                handler.close();
            } catch (Exception e) {
                logger.error("Error closing audit handler", e);
            }
        }
    }

    // ==================== Private Methods ====================

    private boolean shouldLog(AuditEvent event) {
        // Check severity filter
        if (event.severity().ordinal() < config.minSeverity().ordinal()) {
            return false;
        }

        // Check type filter
        if (!config.allowedTypes().isEmpty() &&
                !config.allowedTypes().contains(event.type())) {
            return false;
        }

        return true;
    }

    private void processEvents() {
        while (running || !eventQueue.isEmpty()) {
            AuditEvent event = eventQueue.poll();
            if (event != null) {
                for (AuditLogHandler handler : handlers) {
                    try {
                        handler.handle(event);
                    } catch (Exception e) {
                        logger.error("Error handling audit event", e);
                    }
                }
            } else if (running) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // ==================== Inner Classes ====================

    /**
     * Audit log handler interface.
     */
    public interface AuditLogHandler {
        void handle(AuditEvent event) throws Exception;
        void close() throws Exception;
    }

    /**
     * File-based audit log handler.
     */
    public static class FileAuditHandler implements AuditLogHandler {

        private final Path logFile;
        private final long maxFileSize;
        private final int maxBackupFiles;

        public FileAuditHandler(Path logFile) {
            this(logFile, 100 * 1024 * 1024, 5); // 100MB default
        }

        public FileAuditHandler(Path logFile, long maxFileSize, int maxBackupFiles) {
            this.logFile = logFile;
            this.maxFileSize = maxFileSize;
            this.maxBackupFiles = maxBackupFiles;
        }

        @Override
        public void handle(AuditEvent event) throws IOException {
            rotateIfNeeded();

            String line = formatEvent(event) + "\n";
            Files.writeString(logFile, line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }

        @Override
        public void close() {
            // Nothing to close for file handler
        }

        private void rotateIfNeeded() throws IOException {
            if (Files.exists(logFile) && Files.size(logFile) > maxFileSize) {
                rotateFiles();
            }
        }

        private void rotateFiles() throws IOException {
            // Delete oldest backup
            Path oldestBackup = logFile.resolveSibling(logFile.getFileName() + "." + maxBackupFiles);
            if (Files.exists(oldestBackup)) {
                Files.delete(oldestBackup);
            }

            // Shift backups
            for (int i = maxBackupFiles - 1; i >= 1; i--) {
                Path source = logFile.resolveSibling(logFile.getFileName() + "." + i);
                Path target = logFile.resolveSibling(logFile.getFileName() + "." + (i + 1));
                if (Files.exists(source)) {
                    Files.move(source, target);
                }
            }

            // Move current to .1
            Files.move(logFile, logFile.resolveSibling(logFile.getFileName() + ".1"));
        }

        private String formatEvent(AuditEvent event) {
            return String.format("[%s] %s | %s | %s | %s | %s | %s | %s",
                    DATE_FORMATTER.format(event.timestamp()),
                    event.type(),
                    event.severity(),
                    event.status(),
                    event.actor() != null ? event.actor() : "system",
                    event.action(),
                    event.resource() != null ? event.resource() : "-",
                    event.message()
            );
        }
    }

    /**
     * Console audit log handler.
     */
    public static class ConsoleAuditHandler implements AuditLogHandler {

        private final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

        @Override
        public void handle(AuditEvent event) {
            String message = String.format("[%s] %s: %s - %s",
                    event.type(),
                    event.severity(),
                    event.action(),
                    event.message()
            );

            switch (event.severity()) {
                case DEBUG -> auditLogger.debug(message);
                case INFO -> auditLogger.info(message);
                case WARNING -> auditLogger.warn(message);
                case ERROR, CRITICAL -> auditLogger.error(message);
            }
        }

        @Override
        public void close() {
            // Nothing to close
        }
    }

    /**
     * Audit configuration.
     */
    public record AuditConfig(
            AuditEvent.EventSeverity minSeverity,
            List<AuditEvent.EventType> allowedTypes,
            List<AuditLogHandler> handlers
    ) {
        public static AuditConfig defaultConfig() {
            return new AuditConfig(
                    AuditEvent.EventSeverity.INFO,
                    List.of(), // Empty means all types
                    List.of(new ConsoleAuditHandler())
            );
        }
    }

    /**
     * Audit query for filtering events.
     */
    public record AuditQuery(
            Instant startTime,
            Instant endTime,
            List<AuditEvent.EventType> types,
            List<AuditEvent.EventSeverity> severities,
            List<AuditEvent.EventStatus> statuses,
            String actor,
            String toolName
    ) {
        public boolean matches(AuditEvent event) {
            if (startTime != null && event.timestamp().isBefore(startTime)) {
                return false;
            }
            if (endTime != null && event.timestamp().isAfter(endTime)) {
                return false;
            }
            if (types != null && !types.isEmpty() && !types.contains(event.type())) {
                return false;
            }
            if (severities != null && !severities.isEmpty() && !severities.contains(event.severity())) {
                return false;
            }
            if (statuses != null && !statuses.isEmpty() && !statuses.contains(event.status())) {
                return false;
            }
            if (actor != null && !actor.equals(event.actor())) {
                return false;
            }
            if (toolName != null && !toolName.equals(event.toolName())) {
                return false;
            }
            return true;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Instant startTime;
            private Instant endTime;
            private List<AuditEvent.EventType> types;
            private List<AuditEvent.EventSeverity> severities;
            private List<AuditEvent.EventStatus> statuses;
            private String actor;
            private String toolName;

            public Builder startTime(Instant startTime) {
                this.startTime = startTime;
                return this;
            }

            public Builder endTime(Instant endTime) {
                this.endTime = endTime;
                return this;
            }

            public Builder types(List<AuditEvent.EventType> types) {
                this.types = types;
                return this;
            }

            public Builder severities(List<AuditEvent.EventSeverity> severities) {
                this.severities = severities;
                return this;
            }

            public Builder statuses(List<AuditEvent.EventStatus> statuses) {
                this.statuses = statuses;
                return this;
            }

            public Builder actor(String actor) {
                this.actor = actor;
                return this;
            }

            public Builder toolName(String toolName) {
                this.toolName = toolName;
                return this;
            }

            public AuditQuery build() {
                return new AuditQuery(startTime, endTime, types, severities, statuses, actor, toolName);
            }
        }
    }
}
