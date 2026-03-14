package openclaw.security.ssrf;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Guard for HTTP fetch operations with SSRF protection.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class FetchGuard {

    private final SsrfPolicy policy;

    public FetchGuard() {
        this(new DefaultSsrfPolicy());
    }

    public FetchGuard(SsrfPolicy policy) {
        this.policy = policy;
    }

    /**
     * Validates a URL before fetching.
     *
     * @param url the URL
     * @return validation result
     */
    public SsrfPolicy.SsrfValidationResult validate(String url) {
        return policy.validate(url);
    }

    /**
     * Validates a URL and throws if blocked.
     *
     * @param url the URL
     * @throws SsrfException if blocked
     */
    public void validateOrThrow(String url) {
        SsrfPolicy.SsrfValidationResult result = validate(url);
        if (!result.isAllowed()) {
            throw new SsrfException(
                    "SSRF blocked: " + result.reason().orElse("Unknown reason")
            );
        }
    }

    /**
     * Wraps a fetch operation with SSRF validation.
     *
     * @param url the URL to fetch
     * @param fetcher the fetch function
     * @param <T> the result type
     * @return the fetch result
     */
    public <T> CompletableFuture<T> guardedFetch(
            String url,
            FetchFunction<T> fetcher
    ) {
        SsrfPolicy.SsrfValidationResult result = validate(url);
        if (!result.isAllowed()) {
            return CompletableFuture.failedFuture(
                    new SsrfException(result.reason().orElse("SSRF blocked"))
            );
        }

        return fetcher.fetch(url);
    }

    /**
     * Fetch function interface.
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface FetchFunction<T> {
        CompletableFuture<T> fetch(String url);
    }

    /**
     * SSRF exception.
     */
    public static class SsrfException extends RuntimeException {
        public SsrfException(String message) {
            super(message);
        }
    }
}
