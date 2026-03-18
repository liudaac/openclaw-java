package openclaw.plugin.sdk.websearch.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * HTTP utilities for web search.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
public final class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    // Shared HTTP client with connection pooling
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private HttpUtils() {
        // Utility class
    }

    /**
     * Execute HTTP request with trusted web search endpoint.
     *
     * @param <T> the response type
     * @param url the URL
     * @param timeoutSeconds timeout in seconds
     * @param requestBuilder request builder function
     * @param responseHandler response handler
     * @return future with result
     */
    public static <T> CompletableFuture<T> withTrustedWebSearchEndpoint(
            String url,
            int timeoutSeconds,
            Function<HttpRequest.Builder, HttpRequest.Builder> requestBuilder,
            Function<HttpResponse<String>, T> responseHandler) {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds));

        HttpRequest request = requestBuilder.apply(builder).build();

        logger.debug("Executing request to: {}", url);

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.debug("Response status: {}", response.statusCode());
                    return responseHandler.apply(response);
                })
                .exceptionally(e -> {
                    logger.error("Request failed: {}", url, e);
                    throw new RuntimeException("Web search request failed: " + e.getMessage(), e);
                });
    }

    /**
     * Execute GET request.
     *
     * @param url the URL
     * @param timeoutSeconds timeout in seconds
     * @param headers optional headers
     * @return future with response body
     */
    public static CompletableFuture<String> get(
            String url,
            int timeoutSeconds,
            Map<String, String> headers) {

        return withTrustedWebSearchEndpoint(
                url,
                timeoutSeconds,
                builder -> {
                    builder.GET();
                    if (headers != null) {
                        headers.forEach(builder::header);
                    }
                    return builder;
                },
                response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response.body();
                    }
                    throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
                }
        );
    }

    /**
     * Execute POST request with JSON body.
     *
     * @param url the URL
     * @param timeoutSeconds timeout in seconds
     * @param headers optional headers
     * @param body the JSON body
     * @return future with response body
     */
    public static CompletableFuture<String> postJson(
            String url,
            int timeoutSeconds,
            Map<String, String> headers,
            String body) {

        return withTrustedWebSearchEndpoint(
                url,
                timeoutSeconds,
                builder -> {
                    builder.POST(HttpRequest.BodyPublishers.ofString(body));
                    builder.header("Content-Type", "application/json");
                    if (headers != null) {
                        headers.forEach(builder::header);
                    }
                    return builder;
                },
                response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response.body();
                    }
                    throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
                }
        );
    }

    /**
     * Throw web search API error.
     *
     * @param response the HTTP response
     * @param providerName the provider name
     * @return exception
     */
    public static RuntimeException throwWebSearchApiError(HttpResponse<String> response, String providerName) {
        String body = response.body();
        String message = providerName + " API error (" + response.statusCode() + "): " + 
                (body != null && !body.isEmpty() ? body : response.statusCode());
        return new RuntimeException(message);
    }

    /**
     * Check if response is successful.
     *
     * @param response the response
     * @return true if 2xx
     */
    public static boolean isSuccessful(HttpResponse<?> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    /**
     * Build query string from parameters.
     *
     * @param params the parameters
     * @return query string
     */
    public static String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(urlEncode(entry.getKey()));
                sb.append("=");
                sb.append(urlEncode(entry.getValue()));
            }
        }
        return sb.toString();
    }

    /**
     * URL encode a string.
     *
     * @param value the value
     * @return encoded value
     */
    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Get the shared HTTP client.
     *
     * @return the HTTP client
     */
    public static HttpClient getClient() {
        return CLIENT;
    }
}
