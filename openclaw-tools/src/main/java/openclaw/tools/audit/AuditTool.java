package openclaw.tools.audit;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.AgentTool.PropertySchema;
import openclaw.sdk.tool.AgentTool.ToolParameters;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import openclaw.security.audit.AuditEvent;
import openclaw.security.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Audit tool for security event logging and querying.
 *
 * Features:
 * - Log security events
 * - Query audit logs
 * - Get audit statistics
 * - View recent events
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
@Component
public class AuditTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(AuditTool.class);

    private final AuditLogger auditLogger;

    public AuditTool() {
        this.auditLogger = new AuditLogger();
    }

    public AuditTool(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public String getName() {
        return "audit";
    }

    @Override
    public String getDescription() {
        return "Security audit logging and querying tool";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.ofEntries(
                        Map.entry("action", PropertySchema.enum_("Action to perform", List.of(
                                "log_event",
                                "log_security_violation",
                                "query_events",
                                "get_recent",
                                "get_statistics"
                        ))),
                        Map.entry("event_type", PropertySchema.enum_("Event type", List.of(
                                "TOOL_EXECUTION",
                                "COMMAND_EXECUTION",
                                "FILE_ACCESS",
                                "NETWORK_ACCESS",
                                "AUTHENTICATION",
                                "AUTHORIZATION",
                                "CONFIG_CHANGE",
                                "SECURITY_VIOLATION",
                                "SYSTEM_EVENT"
                        ))),
                        Map.entry("severity", PropertySchema.enum_("Event severity", List.of(
                                "DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"
                        ))),
                        Map.entry("status", PropertySchema.enum_("Event status", List.of(
                                "SUCCESS", "FAILURE", "BLOCKED", "PENDING"
                        ))),
                        Map.entry("actor", PropertySchema.string("Actor/user who performed the action")),
                        Map.entry("action_name", PropertySchema.string("Action name")),
                        Map.entry("resource", PropertySchema.string("Resource affected")),
                        Map.entry("message", PropertySchema.string("Event message")),
                        Map.entry("details", PropertySchema.string("Additional details (JSON)")),
                        Map.entry("tool_name", PropertySchema.string("Tool name (for tool events)")),
                        Map.entry("count", PropertySchema.integer("Number of recent events to retrieve (default: 10)")),
                        Map.entry("start_time", PropertySchema.string("Start time for query (ISO-8601)")),
                        Map.entry("end_time", PropertySchema.string("End time for query (ISO-8601)"))
                ))
                .required(List.of("action"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String action = (String) args.get("action");

                switch (action) {
                    case "log_event":
                        return logEvent(args);
                    case "log_security_violation":
                        return logSecurityViolation(args);
                    case "query_events":
                        return queryEvents(args);
                    case "get_recent":
                        return getRecentEvents(args);
                    case "get_statistics":
                        return getStatistics();
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }
            } catch (Exception e) {
                logger.error("Audit tool execution failed", e);
                return ToolResult.failure("Execution failed: " + e.getMessage());
            }
        });
    }

    /**
     * Log a generic audit event.
     */
    private ToolResult logEvent(Map<String, Object> args) {
        String eventTypeStr = (String) args.get("event_type");
        String severityStr = (String) args.get("severity");
        String statusStr = (String) args.get("status");
        String actor = (String) args.get("actor");
        String actionName = (String) args.get("action_name");
        String resource = (String) args.get("resource");
        String message = (String) args.get("message");
        String detailsStr = (String) args.get("details");
        String toolName = (String) args.get("tool_name");

        if (eventTypeStr == null || actionName == null) {
            return ToolResult.failure("event_type and action_name are required");
        }

        try {
            AuditEvent.EventType eventType = AuditEvent.EventType.valueOf(eventTypeStr);
            AuditEvent.EventSeverity severity = severityStr != null ?
                    AuditEvent.EventSeverity.valueOf(severityStr) : AuditEvent.EventSeverity.INFO;
            AuditEvent.EventStatus status = statusStr != null ?
                    AuditEvent.EventStatus.valueOf(statusStr) : AuditEvent.EventStatus.SUCCESS;

            Map<String, Object> details = new HashMap<>();
            if (detailsStr != null && !detailsStr.isEmpty()) {
                details.put("raw", detailsStr);
            }

            AuditEvent event = AuditEvent.builder()
                    .type(eventType)
                    .severity(severity)
                    .status(status)
                    .actor(actor)
                    .action(actionName)
                    .resource(resource)
                    .message(message != null ? message : actionName)
                    .details(details)
                    .toolName(toolName)
                    .build();

            auditLogger.log(event).join();

            return ToolResult.success("Event logged successfully", Map.of(
                    "event_id", event.id(),
                    "timestamp", event.timestamp().toString()
            ));

        } catch (IllegalArgumentException e) {
            return ToolResult.failure("Invalid enum value: " + e.getMessage());
        }
    }

    /**
     * Log a security violation event.
     */
    private ToolResult logSecurityViolation(Map<String, Object> args) {
        String eventTypeStr = (String) args.get("event_type");
        String actionName = (String) args.get("action_name");
        String message = (String) args.get("message");
        String actor = (String) args.get("actor");
        String resource = (String) args.get("resource");
        String detailsStr = (String) args.get("details");

        if (eventTypeStr == null || actionName == null) {
            return ToolResult.failure("event_type and action_name are required");
        }

        try {
            AuditEvent.EventType eventType = AuditEvent.EventType.valueOf(eventTypeStr);

            Map<String, Object> details = new HashMap<>();
            if (detailsStr != null && !detailsStr.isEmpty()) {
                details.put("raw", detailsStr);
            }

            auditLogger.logSecurityViolation(
                    eventType,
                    actionName,
                    message != null ? message : "Security violation: " + actionName,
                    details
            ).join();

            return ToolResult.success("Security violation logged", Map.of(
                    "event_type", eventTypeStr,
                    "action", actionName
            ));

        } catch (IllegalArgumentException e) {
            return ToolResult.failure("Invalid enum value: " + e.getMessage());
        }
    }

    /**
     * Query audit events.
     */
    private ToolResult queryEvents(Map<String, Object> args) {
        String startTimeStr = (String) args.get("start_time");
        String endTimeStr = (String) args.get("end_time");

        AuditLogger.AuditQuery.Builder queryBuilder = AuditLogger.AuditQuery.builder();

        if (startTimeStr != null) {
            queryBuilder.startTime(Instant.parse(startTimeStr));
        }
        if (endTimeStr != null) {
            queryBuilder.endTime(Instant.parse(endTimeStr));
        }

        AuditLogger.AuditQuery query = queryBuilder.build();
        List<AuditEvent> events = auditLogger.query(query);

        List<Map<String, Object>> eventList = events.stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.id());
                    map.put("timestamp", e.timestamp().toString());
                    map.put("type", e.type().name());
                    map.put("severity", e.severity().name());
                    map.put("status", e.status().name());
                    map.put("actor", e.actor());
                    map.put("action", e.action());
                    map.put("resource", e.resource());
                    map.put("message", e.message());
                    return map;
                })
                .toList();

        return ToolResult.success("Found " + eventList.size() + " event(s)", Map.of(
                "events", eventList,
                "count", eventList.size()
        ));
    }

    /**
     * Get recent events.
     */
    private ToolResult getRecentEvents(Map<String, Object> args) {
        int count = (int) args.getOrDefault("count", 10);
        List<AuditEvent> events = auditLogger.getRecentEvents(count);

        List<Map<String, Object>> eventList = events.stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.id());
                    map.put("timestamp", e.timestamp().toString());
                    map.put("type", e.type().name());
                    map.put("severity", e.severity().name());
                    map.put("status", e.status().name());
                    map.put("action", e.action());
                    map.put("message", e.message());
                    return map;
                })
                .toList();

        return ToolResult.success("Recent " + eventList.size() + " event(s)", Map.of(
                "events", eventList
        ));
    }

    /**
     * Get audit statistics.
     */
    private ToolResult getStatistics() {
        Map<String, Object> stats = auditLogger.getStatistics();
        return ToolResult.success("Audit statistics", stats);
    }
}