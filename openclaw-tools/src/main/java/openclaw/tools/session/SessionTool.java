package openclaw.tools.session;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import openclaw.session.model.Message;
import openclaw.session.model.Session;
import openclaw.session.model.SessionStatus;
import openclaw.session.service.SessionPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Session management tool.
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
@Component
public class SessionTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(SessionTool.class);

    private final SessionPersistenceService sessionService;

    @Autowired
    public SessionTool(SessionPersistenceService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public String getName() {
        return "session";
    }

    @Override
    public String getDescription() {
        return "Manage agent sessions, history, and persistence";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "action", PropertySchema.enum_("Session action", List.of(
                                "create", "get", "get_by_key", "list", "search",
                                "update_status", "archive", "delete", "add_message",
                                "get_messages"
                        )),
                        "session_id", PropertySchema.string("Session ID"),
                        "session_key", PropertySchema.string("Session key"),
                        "model", PropertySchema.string("Model name"),
                        "status", PropertySchema.enum_("Status", List.of(
                                "PENDING", "ACTIVE", "COMPLETED", "ARCHIVED", "PAUSED", "ERROR"
                        )),
                        "query", PropertySchema.string("Search query"),
                        "limit", PropertySchema.integer("Limit"),
                        "role", PropertySchema.enum_("Role", List.of("user", "assistant", "system")),
                        "content", PropertySchema.string("Content")
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
                    case "create":
                        return createSession(args);
                    case "get":
                        return getSession(args);
                    case "get_by_key":
                        return getSessionByKey(args);
                    case "list":
                        return listSessions(args);
                    case "search":
                        return searchSessions(args);
                    case "update_status":
                        return updateStatus(args);
                    case "archive":
                        return archiveSession(args);
                    case "delete":
                        return deleteSession(args);
                    case "add_message":
                        return addMessage(args);
                    case "get_messages":
                        return getMessages(args);
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }
            } catch (Exception e) {
                logger.error("Session operation failed", e);
                return ToolResult.failure("Session operation failed: " + e.getMessage());
            }
        });
    }

    private ToolResult createSession(Map<String, Object> args) {
        String sessionKey = args.getOrDefault("session_key", "default").toString();
        String model = args.getOrDefault("model", "default").toString();

        try {
            Session session = sessionService.createSession(sessionKey, model).join();
            return ToolResult.success("Session created", Map.of(
                    "session_id", session.getId(),
                    "session_key", session.getSessionKey(),
                    "model", session.getModel(),
                    "status", session.getStatus().name()
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to create session: " + e.getMessage());
        }
    }

    private ToolResult getSession(Map<String, Object> args) {
        if (!args.containsKey("session_id")) {
            return ToolResult.failure("Missing required parameter: session_id");
        }

        String sessionId = args.get("session_id").toString();

        try {
            Optional<Session> sessionOpt = sessionService.getSession(sessionId).join();
            if (sessionOpt.isEmpty()) {
                return ToolResult.failure("Session not found: " + sessionId);
            }

            Session session = sessionOpt.get();
            return ToolResult.success("Session retrieved", Map.of(
                    "session_id", session.getId(),
                    "session_key", session.getSessionKey(),
                    "model", session.getModel(),
                    "status", session.getStatus().name()
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to get session: " + e.getMessage());
        }
    }

    private ToolResult getSessionByKey(Map<String, Object> args) {
        if (!args.containsKey("session_key")) {
            return ToolResult.failure("Missing required parameter: session_key");
        }

        String sessionKey = args.get("session_key").toString();

        try {
            Optional<Session> sessionOpt = sessionService.getSessionByKey(sessionKey).join();
            if (sessionOpt.isEmpty()) {
                return ToolResult.failure("Session not found: " + sessionKey);
            }

            Session session = sessionOpt.get();
            return ToolResult.success("Session retrieved", Map.of(
                    "session_id", session.getId(),
                    "session_key", session.getSessionKey(),
                    "model", session.getModel(),
                    "status", session.getStatus().name()
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to get session: " + e.getMessage());
        }
    }

    private ToolResult listSessions(Map<String, Object> args) {
        int limit = (int) args.getOrDefault("limit", 10);

        try {
            List<Session> sessions = sessionService.getRecentSessions(limit).join();

            List<Map<String, Object>> sessionList = sessions.stream()
                    .map(session -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("session_id", session.getId());
                        map.put("session_key", session.getSessionKey());
                        map.put("model", session.getModel());
                        map.put("status", session.getStatus().name());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ToolResult.success("Found " + sessionList.size() + " session(s)", Map.of(
                    "sessions", sessionList
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to list sessions: " + e.getMessage());
        }
    }

    private ToolResult searchSessions(Map<String, Object> args) {
        if (!args.containsKey("query")) {
            return ToolResult.failure("Missing required parameter: query");
        }

        String query = args.get("query").toString();

        try {
            List<Session> sessions = sessionService.searchSessions(query).join();

            List<Map<String, Object>> sessionList = sessions.stream()
                    .map(session -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("session_id", session.getId());
                        map.put("session_key", session.getSessionKey());
                        map.put("model", session.getModel());
                        map.put("status", session.getStatus().name());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ToolResult.success("Found " + sessionList.size() + " matching session(s)", Map.of(
                    "query", query,
                    "sessions", sessionList
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to search sessions: " + e.getMessage());
        }
    }

    private ToolResult updateStatus(Map<String, Object> args) {
        if (!args.containsKey("session_id") || !args.containsKey("status")) {
            return ToolResult.failure("Missing required parameters: session_id and status");
        }

        String sessionId = args.get("session_id").toString();
        SessionStatus status = SessionStatus.valueOf(args.get("status").toString());

        try {
            sessionService.updateStatus(sessionId, status).join();
            return ToolResult.success("Status updated", Map.of(
                    "session_id", sessionId,
                    "status", status.name()
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to update status: " + e.getMessage());
        }
    }

    private ToolResult archiveSession(Map<String, Object> args) {
        if (!args.containsKey("session_id")) {
            return ToolResult.failure("Missing required parameter: session_id");
        }

        String sessionId = args.get("session_id").toString();

        try {
            sessionService.archiveSession(sessionId).join();
            return ToolResult.success("Session archived", Map.of(
                    "session_id", sessionId,
                    "status", "ARCHIVED"
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to archive session: " + e.getMessage());
        }
    }

    private ToolResult deleteSession(Map<String, Object> args) {
        if (!args.containsKey("session_id")) {
            return ToolResult.failure("Missing required parameter: session_id");
        }

        String sessionId = args.get("session_id").toString();

        try {
            sessionService.deleteSession(sessionId).join();
            return ToolResult.success("Session deleted", Map.of(
                    "session_id", sessionId
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to delete session: " + e.getMessage());
        }
    }

    private ToolResult addMessage(Map<String, Object> args) {
        if (!args.containsKey("session_id") || !args.containsKey("role") || !args.containsKey("content")) {
            return ToolResult.failure("Missing required parameters: session_id, role, content");
        }

        String sessionId = args.get("session_id").toString();
        String role = args.get("role").toString();
        String content = args.get("content").toString();

        try {
            Message message = sessionService.addMessage(sessionId, role, content).join();
            return ToolResult.success("Message added", Map.of(
                    "session_id", sessionId,
                    "role", role,
                    "content_length", content.length()
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to add message: " + e.getMessage());
        }
    }

    private ToolResult getMessages(Map<String, Object> args) {
        if (!args.containsKey("session_id")) {
            return ToolResult.failure("Missing required parameter: session_id");
        }

        String sessionId = args.get("session_id").toString();
        int limit = (int) args.getOrDefault("limit", 10);

        try {
            List<Message> messages = sessionService.getMessages(sessionId, limit).join();

            List<Map<String, Object>> messageList = messages.stream()
                    .map(msg -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("role", msg.getRole());
                        map.put("content", msg.getContent());
                        map.put("timestamp", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : "");
                        return map;
                    })
                    .collect(Collectors.toList());

            return ToolResult.success("Found " + messageList.size() + " message(s)", Map.of(
                    "session_id", sessionId,
                    "messages", messageList
            ));
        } catch (Exception e) {
            return ToolResult.failure("Failed to get messages: " + e.getMessage());
        }
    }
}
