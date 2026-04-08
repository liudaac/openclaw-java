package openclaw.agent.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HttpErrorClassifier.
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
class HttpErrorClassifierTest {

    @ParameterizedTest
    @CsvSource({
        "404, FALLBACK_ELIGIBLE",
        "429, RATE_LIMITED",
        "401, HARD_FAILURE",
        "403, HARD_FAILURE",
        "400, CONTEXT_OVERFLOW",
        "500, FALLBACK_ELIGIBLE",
        "502, FALLBACK_ELIGIBLE",
        "503, FALLBACK_ELIGIBLE",
        "504, FALLBACK_ELIGIBLE"
    })
    void testClassify(int statusCode, HttpErrorClassifier.ErrorClassification expected) {
        assertEquals(expected, HttpErrorClassifier.classify(statusCode));
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 500, 502, 503, 504})
    void testShouldFallback(int statusCode) {
        assertTrue(HttpErrorClassifier.shouldFallback(statusCode));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 405, 406})
    void testShouldNotFallback(int statusCode) {
        assertFalse(HttpErrorClassifier.shouldFallback(statusCode));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void testIsHardFailure(int statusCode) {
        assertTrue(HttpErrorClassifier.isHardFailure(statusCode));
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 500, 200, 429})
    void testIsNotHardFailure(int statusCode) {
        assertFalse(HttpErrorClassifier.isHardFailure(statusCode));
    }

    @Test
    void testIsContextOverflow() {
        // Context overflow indicators in error message
        assertTrue(HttpErrorClassifier.isContextOverflow(400,
                Optional.of("Context overflow: length exceeded maximum")));
        assertTrue(HttpErrorClassifier.isContextOverflow(400,
                Optional.of("Token overflow error in context")));
        assertTrue(HttpErrorClassifier.isContextOverflow(400,
                Optional.of("Context too long - maximum tokens overflow")));

        // Not context overflow
        assertFalse(HttpErrorClassifier.isContextOverflow(400,
                Optional.of("Bad request")));
        assertFalse(HttpErrorClassifier.isContextOverflow(404,
                Optional.of("Not found")));
        assertFalse(HttpErrorClassifier.isContextOverflow(400, Optional.empty()));
    }

    @Test
    void testGetRetryDelay() {
        // With Retry-After header
        assertEquals(5000, HttpErrorClassifier.getRetryDelay(429, Optional.of("5")));

        // Without header - default
        assertEquals(1000, HttpErrorClassifier.getRetryDelay(429, Optional.empty()));

        // Non-rate-limit status
        assertEquals(0, HttpErrorClassifier.getRetryDelay(404, Optional.empty()));
    }

    @Test
    void testGetRetryDelayInvalidHeader() {
        // Invalid header value - should default to 1000
        assertEquals(1000, HttpErrorClassifier.getRetryDelay(429, Optional.of("invalid")));
    }
}
