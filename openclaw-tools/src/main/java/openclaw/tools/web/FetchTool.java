package openclaw.tools.web;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import openclaw.security.ssrf.FetchGuard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP fetch tool for web requests.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class FetchTool implements AgentTool {

    private final HttpClient httpClient;
    private final FetchGuard fetchGuard;
    private final Duration timeout;

    public FetchTool() {
        this(Duration.ofSeconds(30));
    }

    public FetchTool(Duration timeout) {
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.fetchGuard = new FetchGuard();
    }

    @Override
    public String getName() {
        return "fetch";
    }

    @Override
    public String getDescription() {
        return "Fetch web content via HTTP GET/POST";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "url", PropertySchema.string("The URL to fetch"),
                        "method", PropertySchema.enum_("HTTP method", List.of("GET", "POST")),
                        "headers", PropertySchema.string("Request headers (JSON)"),
                        "body", PropertySchema.string("Request body (for POST)"),
                        "timeout_seconds", PropertySchema.integer("Timeout in seconds")
                ))
                .required(List.of("url"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String url = args.get("url").toString();
                String method = args.getOrDefault("method", "GET").toString().toUpperCase();

                // SSRF check
                var validation = fetchGuard.validate(url);
                if (!validation.allowed()) {
                    return ToolResult.failure("URL blocked by security policy: " + validation.reason().orElse("Unknown"));
                }

                int timeoutSec = (int) args.getOrDefault("timeout_seconds", 30);
                Duration requestTimeout = Duration.ofSeconds(timeoutSec);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(requestTimeout);

                // Headers
                if (args.containsKey("headers")) {
                    String headersJson = args.get("headers").toString();
                    // Parse simple JSON headers
                    Map<String, String> headers = parseHeaders(headersJson);
                    headers.forEach(requestBuilder::header);
                }

                // Method and body
                HttpRequest request;
                if ("POST".equals(method) && args.containsKey("body")) {
                    String body = args.get("body").toString();
                    request = requestBuilder
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
                } else {
                    request = requestBuilder.GET().build();
                }

                // Execute
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                // Check status
                if (response.statusCode() >= 400) {
                    return ToolResult.failure("HTTP " + response.statusCode() + ": " + response.body().substring(0, Math.min(500, response.body().length())));
                }

                // Truncate large responses
                String body = response.body();
                boolean truncated = false;
                if (body.length() > 100000) {
                    body = body.substring(0, 100000) + "\n... (truncated)";
                    truncated = true;
                }

                return ToolResult.success(body, Map.of(
                        "status_code", response.statusCode(),
                        "content_type", response.headers().firstValue("Content-Type").orElse("unknown"),
                        "truncated", truncated
                ));

            } catch (Exception e) {
                return ToolResult.failure("Fetch failed: " + e.getMessage());
            }
        });
    }

    private Map<String, String> parseHeaders(String headersJson) {
        // Simple JSON parsing - in production use Jackson
        Map<String, String> headers = new java.util.HashMap<>();
        try {
            if (headersJson.startsWith("{")) {
                String content = headersJson.substring(1, headersJson.length() - 1);
                String[] pairs = content.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        headers.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return headers;
    }
}
