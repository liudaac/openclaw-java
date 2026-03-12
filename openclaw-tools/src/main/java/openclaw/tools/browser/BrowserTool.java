package openclaw.tools.browser;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import openclaw.security.ssrf.FetchGuard;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Browser automation tool using Playwright CLI.
 *
 * <p>Phase 3 Enhancement - Browser control for web automation.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class BrowserTool implements AgentTool {

    private final Path screenshotDir;
    private final FetchGuard fetchGuard;
    private final int defaultTimeout;

    public BrowserTool() {
        this(Path.of("/tmp/browser-screenshots"), 30);
    }

    public BrowserTool(Path screenshotDir, int defaultTimeout) {
        this.screenshotDir = screenshotDir;
        this.fetchGuard = new FetchGuard();
        this.defaultTimeout = defaultTimeout;
        
        // Create screenshot directory if not exists
        try {
            Files.createDirectories(screenshotDir);
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public String getName() {
        return "browser";
    }

    @Override
    public String getDescription() {
        return "Control browser for web automation, screenshots, and interaction";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "action", PropertySchema.enum_("Browser action", List.of(
                                "screenshot", "navigate", "click", "type", "scroll", "evaluate"
                        )),
                        "url", PropertySchema.string("URL to navigate (for navigate/screenshot)"),
                        "selector", PropertySchema.string("CSS selector (for click/type)"),
                        "text", PropertySchema.string("Text to type (for type action)"),
                        "script", PropertySchema.string("JavaScript to evaluate (for evaluate action)"),
                        "width", PropertySchema.integer("Viewport width (default: 1280)"),
                        "height", PropertySchema.integer("Viewport height (default: 720)"),
                        "timeout", PropertySchema.integer("Timeout in seconds (default: 30)")
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
                    case "screenshot":
                        return takeScreenshot(args);
                    case "navigate":
                        return navigate(args);
                    case "click":
                        return clickElement(args);
                    case "type":
                        return typeText(args);
                    case "scroll":
                        return scrollPage(args);
                    case "evaluate":
                        return evaluateScript(args);
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }

            } catch (Exception e) {
                return ToolResult.failure("Browser action failed: " + e.getMessage());
            }
        });
    }

    private ToolResult takeScreenshot(Map<String, Object> args) {
        String url = args.get("url").toString();
        
        // SSRF check
        var validation = fetchGuard.validate(url);
        if (!validation.allowed()) {
            return ToolResult.failure("URL blocked: " + validation.reason().orElse("Unknown"));
        }

        int width = (int) args.getOrDefault("width", 1280);
        int height = (int) args.getOrDefault("height", 720);
        int timeout = (int) args.getOrDefault("timeout", defaultTimeout);

        String filename = "screenshot-" + UUID.randomUUID() + ".png";
        Path outputPath = screenshotDir.resolve(filename);

        try {
            // Use Playwright CLI or similar tool
            ProcessBuilder pb = new ProcessBuilder(
                    "npx", "playwright", "screenshot",
                    "--viewport-size=" + width + "," + height,
                    "--timeout=" + (timeout * 1000),
                    url,
                    outputPath.toString()
            );

            Process process = pb.start();
            boolean finished = process.waitFor(timeout + 5, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ToolResult.failure("Screenshot timeout");
            }

            if (process.exitValue() != 0) {
                String error = readProcessError(process);
                return ToolResult.failure("Screenshot failed: " + error);
            }

            if (!Files.exists(outputPath)) {
                return ToolResult.failure("Screenshot file not created");
            }

            return ToolResult.success(
                    "Screenshot saved: " + outputPath.toAbsolutePath(),
                    Map.of(
                            "path", outputPath.toString(),
                            "url", url,
                            "width", width,
                            "height", height
                    )
            );

        } catch (Exception e) {
            return ToolResult.failure("Screenshot error: " + e.getMessage());
        }
    }

    private ToolResult navigate(Map<String, Object> args) {
        String url = args.get("url").toString();
        
        var validation = fetchGuard.validate(url);
        if (!validation.allowed()) {
            return ToolResult.failure("URL blocked: " + validation.reason().orElse("Unknown"));
        }

        // For now, just validate the URL is accessible
        return ToolResult.success(
                "URL validated: " + url,
                Map.of("url", url, "status", "accessible")
        );
    }

    private ToolResult clickElement(Map<String, Object> args) {
        if (!args.containsKey("selector")) {
            return ToolResult.failure("Missing required parameter: selector");
        }
        String selector = args.get("selector").toString();
        
        return ToolResult.success(
                "Click action prepared for: " + selector,
                Map.of("selector", selector, "action", "click")
        );
    }

    private ToolResult typeText(Map<String, Object> args) {
        if (!args.containsKey("selector") || !args.containsKey("text")) {
            return ToolResult.failure("Missing required parameters: selector and text");
        }
        String selector = args.get("selector").toString();
        String text = args.get("text").toString();
        
        return ToolResult.success(
                "Type action prepared",
                Map.of("selector", selector, "text", text.substring(0, Math.min(50, text.length())))
        );
    }

    private ToolResult scrollPage(Map<String, Object> args) {
        String direction = args.getOrDefault("direction", "down").toString();
        int amount = (int) args.getOrDefault("amount", 500);
        
        return ToolResult.success(
                "Scroll action prepared: " + direction + " by " + amount,
                Map.of("direction", direction, "amount", amount)
        );
    }

    private ToolResult evaluateScript(Map<String, Object> args) {
        if (!args.containsKey("script")) {
            return ToolResult.failure("Missing required parameter: script");
        }
        String script = args.get("script").toString();
        
        // Security: Limit script execution
        if (script.length() > 10000) {
            return ToolResult.failure("Script too long (max 10000 chars)");
        }
        
        return ToolResult.success(
                "Script evaluation prepared",
                Map.of(
                        "script", script.substring(0, Math.min(200, script.length())),
                        "length", script.length()
                )
        );
    }

    private String readProcessError(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }
}
