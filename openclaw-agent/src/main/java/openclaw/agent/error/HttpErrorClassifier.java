package openclaw.agent.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Classifies HTTP errors for model fallback chain.
 *
 * <p>Identifies specific HTTP error codes that should trigger model fallback
 * rather than hard failures.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
public class HttpErrorClassifier {

    private static final Logger logger = LoggerFactory.getLogger(HttpErrorClassifier.class);

    private HttpErrorClassifier() {
        // Utility class
    }

    /**
     * Error classification result.
     */
    public enum ErrorClassification {
        /**
         * Error should trigger model fallback.
         */
        FALLBACK_ELIGIBLE,

        /**
         * Error is a hard failure, do not fallback.
         */
        HARD_FAILURE,

        /**
         * Context overflow error (special handling).
         */
        CONTEXT_OVERFLOW,

        /**
         * Rate limit error (should retry with backoff).
         */
        RATE_LIMITED,

        /**
         * Unknown error type.
         */
        UNKNOWN
    }

    /**
     * Classify an HTTP error code.
     *
     * @param statusCode the HTTP status code
     * @return the error classification
     */
    public static ErrorClassification classify(int statusCode) {
        return switch (statusCode) {
            case 404 -> {
                // HTTP 404: Resource not found
                // This can happen when a model is deprecated or unavailable
                // Should trigger fallback to alternative model
                logger.debug("HTTP 404 classified as FALLBACK_ELIGIBLE");
                yield ErrorClassification.FALLBACK_ELIGIBLE;
            }
            case 429 -> {
                // HTTP 429: Too Many Requests
                // Rate limited, should retry with backoff
                logger.debug("HTTP 429 classified as RATE_LIMITED");
                yield ErrorClassification.RATE_LIMITED;
            }
            case 401, 403 -> {
                // Authentication/Authorization errors
                // Hard failures - credentials issue
                logger.debug("HTTP {} classified as HARD_FAILURE", statusCode);
                yield ErrorClassification.HARD_FAILURE;
            }
            case 400 -> {
                // HTTP 400: Bad Request
                // Could be context overflow or malformed request
                logger.debug("HTTP 400 classified as CONTEXT_OVERFLOW (possible)");
                yield ErrorClassification.CONTEXT_OVERFLOW;
            }
            case 500, 502, 503, 504 -> {
                // Server errors
                // May be transient, could fallback
                logger.debug("HTTP {} classified as FALLBACK_ELIGIBLE", statusCode);
                yield ErrorClassification.FALLBACK_ELIGIBLE;
            }
            default -> {
                if (statusCode >= 400 && statusCode < 500) {
                    // Other client errors
                    logger.debug("HTTP {} classified as HARD_FAILURE", statusCode);
                    yield ErrorClassification.HARD_FAILURE;
                } else if (statusCode >= 500) {
                    // Other server errors
                    logger.debug("HTTP {} classified as FALLBACK_ELIGIBLE", statusCode);
                    yield ErrorClassification.FALLBACK_ELIGIBLE;
                } else {
                    logger.debug("HTTP {} classified as UNKNOWN", statusCode);
                    yield ErrorClassification.UNKNOWN;
                }
            }
        };
    }

    /**
     * Check if error should trigger model fallback.
     *
     * @param statusCode the HTTP status code
     * @return true if should fallback
     */
    public static boolean shouldFallback(int statusCode) {
        ErrorClassification classification = classify(statusCode);
        return classification == ErrorClassification.FALLBACK_ELIGIBLE ||
               classification == ErrorClassification.RATE_LIMITED;
    }

    /**
     * Check if error is a hard failure (no fallback).
     *
     * @param statusCode the HTTP status code
     * @return true if hard failure
     */
    public static boolean isHardFailure(int statusCode) {
        return classify(statusCode) == ErrorClassification.HARD_FAILURE;
    }

    /**
     * Check if error is a context overflow.
     *
     * @param statusCode the HTTP status code
     * @param errorMessage optional error message for additional context
     * @return true if context overflow
     */
    public static boolean isContextOverflow(int statusCode, Optional<String> errorMessage) {
        if (statusCode == 400) {
            // Check error message for context overflow indicators
            return errorMessage.map(msg ->
                msg.toLowerCase().contains("context") &&
                (msg.toLowerCase().contains("overflow") ||
                 msg.toLowerCase().contains("too long") ||
                 msg.toLowerCase().contains("token") ||
                 msg.toLowerCase().contains("maximum"))
            ).orElse(false);
        }
        return false;
    }

    /**
     * Get retry delay for rate limited errors.
     *
     * @param statusCode the HTTP status code
     * @param retryAfterHeader optional Retry-After header value
     * @return delay in milliseconds
     */
    public static long getRetryDelay(int statusCode, Optional<String> retryAfterHeader) {
        if (statusCode == 429) {
            // Try to parse Retry-After header
            return retryAfterHeader
                    .map(HttpErrorClassifier::parseRetryAfter)
                    .orElse(1000L); // Default 1 second
        }
        return 0;
    }

    private static long parseRetryAfter(String value) {
        try {
            // Retry-After can be seconds or HTTP date
            // Try parsing as seconds first
            return Long.parseLong(value.trim()) * 1000;
        } catch (NumberFormatException e) {
            // Could be a date, default to 1 second
            logger.debug("Could not parse Retry-After header: {}", value);
            return 1000;
        }
    }
}
