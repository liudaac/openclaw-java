package openclaw.tools.browser;

import openclaw.browser.BrowserService;
import openclaw.browser.batch.BatchAction;
import openclaw.browser.batch.BatchResult;
import openclaw.browser.model.BrowserSession;
import openclaw.browser.model.BrowserSession.SessionOptions;
import openclaw.browser.snapshot.PageSnapshot;
import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import openclaw.security.ssrf.FetchGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Browser automation tool - Enhanced Version.
 *
 * <p>Uses the new openclaw-browser module with Playwright Java API.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
@Component
public class BrowserTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(BrowserTool.class);

    private final BrowserService browserService;
    private final FetchGuard fetchGuard;
    private final Path screenshotDir;
    private final Map<String, String> sessionAliasMap; // alias -> sessionId

    @Autowired
    public BrowserTool(BrowserService browserService) {
        this.browserService = browserService;
        this.fetchGuard = new FetchGuard();
        this.screenshotDir = Path.of("/tmp/browser-screenshots");
        this.sessionAliasMap = new ConcurrentHashMap<>();
        
        // Create screenshot directory if not exists
        try {
            Files.createDirectories(screenshotDir);
        } catch (Exception e) {
            logger.warn("Failed to create screenshot directory: {}", e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "browser";
    }

    @Override
    public String getDescription() {
        return "Control browser for web automation, screenshots, and interaction using Playwright";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "action", PropertySchema.enum_("Browser action", List.of(
                                "create_session", "close_session", "list_sessions",
                                "navigate", "screenshot", "click", "type", "fill", 
                                "select", "hover", "scroll", "evaluate", "snapshot",
                                "get_text", "get_html", "get_url", "get_title",
                                "batch"  // NEW: batch operation
                        )),
                        "session_id", PropertySchema.string("Session ID or alias (optional, uses default if not provided)"),
                        "url", PropertySchema.string("URL to navigate (for navigate/screenshot)"),
                        "selector", PropertySchema.string("CSS selector (for click/type/fill/select/hover/get_text)"),
                        "text", PropertySchema.string("Text to type (for type/fill action)"),
                        "value", PropertySchema.string("Value to select (for select action)"),
                        "script", PropertySchema.string("JavaScript to evaluate (for evaluate action)"),
                        "direction", PropertySchema.enum_("Scroll direction", List.of("up", "down", "left", "right")),
                        "amount", PropertySchema.integer("Scroll amount in pixels (default: 500)"),
                        "width", PropertySchema.integer("Viewport width (default: 1280)"),
                        "height", PropertySchema.integer("Viewport height (default: 720)"),
                        "headless", PropertySchema.boolean_("Run in headless mode (default: true)"),
                        "timeout", PropertySchema.integer("Timeout in seconds (default: 30)"),
                        "save_path", PropertySchema.string("Path to save screenshot (optional)"),
                        "full_page", PropertySchema.boolean_("Capture full page screenshot (default: false)"),
                        "actions", PropertySchema.array("Batch actions (for batch action)", PropertySchema.string("Action")),
                        "stop_on_error", PropertySchema.boolean_("Stop batch on first error (default: true)")
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
                    // Session Management
                    case "create_session":
                        return createSession(args);
                    case "close_session":
                        return closeSession(args);
                    case "list_sessions":
                        return listSessions();
                    
                    // Navigation
                    case "navigate":
                        return navigate(args);
                    
                    // Screenshots
                    case "screenshot":
                        return takeScreenshot(args);
                    
                    // Element Interaction
                    case "click":
                        return clickElement(args);
                    case "type":
                        return typeText(args);
                    case "fill":
                        return fillText(args);
                    case "select":
                        return selectOption(args);
                    case "hover":
                        return hoverElement(args);
                    case "scroll":
                        return scrollPage(args);
                    
                    // JavaScript
                    case "evaluate":
                        return evaluateScript(args);
                    
                    // Snapshot
                    case "snapshot":
                        return getSnapshot(args);
                    
                    // Content
                    case "get_text":
                        return getText(args);
                    case "get_html":
                        return getHtml(args);
                    case "get_url":
                        return getUrl(args);
                    case "get_title":
                        return getTitle(args);
                    
                    // Batch
                    case "batch":
                        return executeBatch(args);
                    
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }

            } catch (Exception e) {
                logger.error("Browser action failed", e);
                return ToolResult.failure("Browser action failed: " + e.getMessage());
            }
        });
    }

    // ==================== Session Management ====================

    private ToolResult createSession(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        int width = (int) args.getOrDefault("width", 1280);
        int height = (int) args.getOrDefault("height", 720);
        boolean headless = (boolean) args.getOrDefault("headless", true);

        SessionOptions options = new SessionOptions(width, height, null, "en-US", "UTC", headless, Map.of());

        try {
            BrowserSession session = browserService.createSession(alias, options).join();
            sessionAliasMap.put(alias, session.getId());
            
            return ToolResult.success(
                    "Browser session created",
                    Map.of(
                            "session_id", session.getId(),
                            "alias", alias,
                            "viewport", Map.of("width", width, "height", height),
                            "headless", headless
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Failed to create session: " + e.getMessage());
        }
    }

    private ToolResult closeSession(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);

        try {
            browserService.closeSession(sessionId).join();
            sessionAliasMap.remove(alias);
            return ToolResult.success("Browser session closed", Map.of("alias", alias));
        } catch (Exception e) {
            return ToolResult.failure("Failed to close session: " + e.getMessage());
        }
    }

    private ToolResult listSessions() {
        try {
            Map<String, BrowserSession> sessions = browserService.listSessions().join();
            
            List<Map<String, Object>> sessionList = sessions.entrySet().stream()
                    .map(entry -> {
                        BrowserSession session = entry.getValue();
                        return Map.of(
                                "session_id", session.getId(),
                                "profile", session.getProfile(),
                                "page_count", session.getPages().size(),
                                "created_at", session.getCreatedAt().toString()
                        );
                    })
                    .toList();

            return ToolResult.success(
                    "Found " + sessionList.size() + " session(s)",
                    Map.of("sessions", sessionList)
            );
        } catch (Exception e) {
            return ToolResult.failure("Failed to list sessions: " + e.getMessage());
        }
    }

    // ==================== Navigation ====================

    private ToolResult navigate(Map<String, Object> args) {
        if (!args.containsKey("url")) {
            return ToolResult.failure("Missing required parameter: url");
        }

        String url = args.get("url").toString();
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        int timeout = (int) args.getOrDefault("timeout", 30) * 1000;

        // SSRF check
        var validation = fetchGuard.validate(url);
        if (!validation.isAllowed()) {
            return ToolResult.failure("URL blocked: " + validation.reason().orElse("Unknown"));
        }

        try {
            browserService.navigate(sessionId, url, timeout).join();
            String currentUrl = browserService.getUrl(sessionId).join();
            String title = browserService.getTitle(sessionId).join();
            
            return ToolResult.success(
                    "Navigated to: " + title,
                    Map.of(
                            "url", currentUrl,
                            "title", title
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Navigation failed: " + e.getMessage());
        }
    }

    // ==================== Screenshots ====================

    private ToolResult takeScreenshot(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        boolean fullPage = (boolean) args.getOrDefault("full_page", false);

        try {
            byte[] screenshot;
            
            if (args.containsKey("selector")) {
                // Element screenshot
                String selector = args.get("selector").toString();
                screenshot = browserService.screenshot(sessionId, selector).join();
            } else {
                // Full page or viewport screenshot
                screenshot = browserService.screenshot(sessionId).join();
            }

            // Save to file if path provided
            String savePath = null;
            if (args.containsKey("save_path")) {
                savePath = args.get("save_path").toString();
            } else {
                String filename = "screenshot-" + UUID.randomUUID() + ".png";
                savePath = screenshotDir.resolve(filename).toString();
            }

            Path path = Path.of(savePath);
            Files.createDirectories(path.getParent());
            Files.write(path, screenshot);

            // Return base64 for inline display
            String base64 = Base64.getEncoder().encodeToString(screenshot);
            
            return ToolResult.success(
                    "Screenshot saved: " + savePath,
                    Map.of(
                            "path", savePath,
                            "size_bytes", screenshot.length,
                            "base64", base64,
                            "full_page", fullPage
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Screenshot failed: " + e.getMessage());
        }
    }

    // ==================== Element Interaction ====================

    private ToolResult clickElement(Map<String, Object> args) {
        if (!args.containsKey("selector")) {
            return ToolResult.failure("Missing required parameter: selector");
        }

        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        String selector = args.get("selector").toString();

        try {
            browserService.click(sessionId, selector).join();
            return ToolResult.success("Element clicked", Map.of("selector", selector));
        } catch (Exception e) {
            return ToolResult.failure("Click failed: " + e.getMessage());
        }
    }

    private ToolResult typeText(Map<String, Object> args) {
        if (!args.containsKey("selector") || !args.containsKey("text")) {
            return ToolResult.failure("Missing required parameters: selector and text");
        }

        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        String selector = args.get("selector").toString();
        String text = args.get("text").toString();

        try {
            browserService.type(sessionId, selector, text).join();
            return ToolResult.success(
                    "Text typed",
                    Map.of("selector", selector, "text_length", text.length())
            );
        } catch (Exception e) {
            return ToolResult.failure("Type failed: " + e.getMessage());
        }
    }

    private ToolResult fillText(Map<String, Object> args) {
        if (!args.containsKey("selector") || !args.containsKey("text")) {
            return ToolResult.failure("Missing required parameters: selector and text");
        }

        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        String selector = args.get("selector").toString();
        String text = args.get("text").toString();

        try {
            browserService.fill(sessionId, selector, text).join();
            return ToolResult.success(
                    "Text filled",
                    Map.of("selector", selector, "text_length", text.length())
            );
        } catch (Exception e) {
            return ToolResult.failure("Fill failed: " + e.getMessage());
        }
    }

    private ToolResult selectOption(Map<String, Object> args) {
        if (!args.containsKey("selector") || !args.containsKey("value")) {
            return ToolResult.failure("Missing required parameters: selector and value");
        }

        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        String selector = args.get("selector").toString();
        String value = args.get("value").toString();

        try {
            browserService.select(sessionId, selector, value).join();
            return ToolResult.success(
                    "Option selected",
                    Map.of("selector", selector, "value", value)
            );
        } catch (Exception e) {
            return ToolResult.failure("Select failed: " + e.getMessage());
        }
    }

    private ToolResult hoverElement(Map<String, Object> args) {
        if (!args.containsKey("selector")) {
            return ToolResult.failure("Missing required parameter: selector");
        }

        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        String selector = args.get("selector").toString();

        try {
            browserService.hover(sessionId, selector).join();
            return ToolResult.success("Element hovered", Map.of("selector", selector));
        } catch (Exception e) {
            return ToolResult.failure("Hover failed: " + e.getMessage());
        }
    }

    private ToolResult scrollPage(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        String direction = args.getOrDefault("direction", "down").toString();
        int amount = (int) args.getOrDefault("amount", 500);

        try {
            browserService.scroll(sessionId, direction, amount).join();
            return ToolResult.success(
                    "Page scrolled",
                    Map.of("direction", direction, "amount", amount)
            );
        } catch (Exception e) {
            return ToolResult.failure("Scroll failed: " + e.getMessage());
        }
    }

    // ==================== JavaScript ====================

    private ToolResult evaluateScript(Map<String, Object> args) {
        if (!args.containsKey("script")) {
            return ToolResult.failure("Missing required parameter: script");
        }

        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        String script = args.get("script").toString();

        // Security: Limit script execution
        if (script.length() > 10000) {
            return ToolResult.failure("Script too long (max 10000 chars)");
        }

        try {
            Object result = browserService.evaluate(sessionId, script).join();
            
            return ToolResult.success(
                    "Script evaluated",
                    Map.of(
                            "result", result != null ? result.toString() : "null",
                            "result_type", result != null ? result.getClass().getSimpleName() : "null",
                            "script_length", script.length()
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Script evaluation failed: " + e.getMessage());
        }
    }

    // ==================== Snapshot ====================

    private ToolResult getSnapshot(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);

        try {
            PageSnapshot snapshot = browserService.getSnapshot(sessionId).join();
            
            return ToolResult.success(
                    "Page snapshot captured",
                    Map.of(
                            "title", snapshot.getTitle(),
                            "url", snapshot.getUrl(),
                            "elements", snapshot.getElements().size(),
                            "links", snapshot.getLinks().size(),
                            "images", snapshot.getImages().size(),
                            "text_preview", snapshot.getHtml().replaceAll("<[^>]*>", "").substring(0, 
                                    Math.min(500, snapshot.getHtml().replaceAll("<[^>]*>", "").length()))
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Snapshot failed: " + e.getMessage());
        }
    }

    // ==================== Content ====================

    private ToolResult getText(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);

        try {
            String text;
            if (args.containsKey("selector")) {
                String selector = args.get("selector").toString();
                text = browserService.getText(sessionId, selector).join();
            } else {
                text = browserService.getHtml(sessionId).join(); // Fallback to HTML
                // Strip HTML tags for plain text
                text = text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            }
            
            return ToolResult.success(
                    "Text extracted",
                    Map.of(
                            "text", text.substring(0, Math.min(2000, text.length())),
                            "length", text.length()
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Get text failed: " + e.getMessage());
        }
    }

    private ToolResult getHtml(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);

        try {
            String html = browserService.getHtml(sessionId).join();
            
            return ToolResult.success(
                    "HTML extracted",
                    Map.of(
                            "html", html.substring(0, Math.min(5000, html.length())),
                            "length", html.length()
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Get HTML failed: " + e.getMessage());
        }
    }

    private ToolResult getUrl(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);

        try {
            String url = browserService.getUrl(sessionId).join();
            return ToolResult.success("URL retrieved", Map.of("url", url));
        } catch (Exception e) {
            return ToolResult.failure("Get URL failed: " + e.getMessage());
        }
    }

    private ToolResult getTitle(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);

        try {
            String title = browserService.getTitle(sessionId).join();
            return ToolResult.success("Title retrieved", Map.of("title", title));
        } catch (Exception e) {
            return ToolResult.failure("Get title failed: " + e.getMessage());
        }
    }

    // ==================== Batch Operations ====================

    private ToolResult executeBatch(Map<String, Object> args) {
        String alias = args.getOrDefault("session_id", "default").toString();
        String sessionId = resolveSessionId(alias);
        boolean stopOnError = (boolean) args.getOrDefault("stop_on_error", true);

        if (!args.containsKey("actions")) {
            return ToolResult.failure("Missing required parameter: actions");
        }

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> actionMaps = (List<Map<String, Object>>) args.get("actions");
            
            List<BatchAction> actions = new ArrayList<>();
            for (Map<String, Object> actionMap : actionMaps) {
                actions.add(parseBatchAction(actionMap));
            }

            BatchResult result = browserService.executeBatch(sessionId, actions, stopOnError).join();
            
            // Build result summary
            List<Map<String, Object>> results = new ArrayList<>();
            for (int i = 0; i < result.results().size(); i++) {
                var actionResult = result.results().get(i);
                Map<String, Object> resultMap = new java.util.HashMap<>();
                resultMap.put("index", i);
                resultMap.put("ok", actionResult.ok());
                resultMap.put("duration_ms", actionResult.durationMs());
                if (!actionResult.ok()) {
                    resultMap.put("error", actionResult.error());
                }
                if (actionResult.result() != null) {
                    resultMap.put("result", actionResult.result());
                }
                results.add(resultMap);
            }

            return ToolResult.success(
                    "Batch execution completed: " + result.completedCount() + " succeeded, " + result.failedCount() + " failed",
                    Map.of(
                            "completed", result.completedCount(),
                            "failed", result.failedCount(),
                            "total_duration_ms", result.totalDurationMs(),
                            "results", results
                    )
            );
        } catch (Exception e) {
            return ToolResult.failure("Batch execution failed: " + e.getMessage());
        }
    }

    private BatchAction parseBatchAction(Map<String, Object> map) {
        String kind = map.get("kind").toString();
        
        return new BatchAction(
                kind,
                map.get("selector") != null ? map.get("selector").toString() : null,
                map.get("ref") != null ? map.get("ref").toString() : null,
                map.get("text") != null ? map.get("text").toString() : null,
                map.get("value") != null ? map.get("value").toString() : null,
                map.get("script") != null ? map.get("script").toString() : null,
                map.get("key") != null ? map.get("key").toString() : null,
                map.get("direction") != null ? map.get("direction").toString() : null,
                map.get("amount") != null ? (Integer) map.get("amount") : null,
                map.get("width") != null ? (Integer) map.get("width") : null,
                map.get("height") != null ? (Integer) map.get("height") : null,
                map.get("delay_ms") != null ? (Integer) map.get("delay_ms") : null,
                map.get("time_ms") != null ? (Integer) map.get("time_ms") : null,
                map.get("timeout_ms") != null ? (Integer) map.get("timeout_ms") : null,
                map.get("submit") != null ? (Boolean) map.get("submit") : null,
                map.get("slowly") != null ? (Boolean) map.get("slowly") : null,
                map.get("double_click") != null ? (Boolean) map.get("double_click") : null,
                map.get("button") != null ? map.get("button").toString() : null,
                map.get("modifiers") != null ? ((List<?>) map.get("modifiers")).toArray(new String[0]) : null,
                map.get("start_selector") != null ? map.get("start_selector").toString() : null,
                map.get("start_ref") != null ? map.get("start_ref").toString() : null,
                map.get("end_selector") != null ? map.get("end_selector").toString() : null,
                map.get("end_ref") != null ? map.get("end_ref").toString() : null,
                map.get("extra") != null ? (Map<String, Object>) map.get("extra") : null
        );
    }

    // ==================== Helper Methods ====================

    private String resolveSessionId(String alias) {
        return sessionAliasMap.getOrDefault(alias, alias);
    }
}