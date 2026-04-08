package openclaw.server.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClientDisconnectHandler.
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
class ClientDisconnectHandlerTest {

    private ClientDisconnectHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ClientDisconnectHandler();
    }

    @Test
    void testCreateContext() {
        String requestId = "test-request-1";
        ClientDisconnectHandler.RequestContext context = handler.createContext(requestId);

        assertNotNull(context);
        assertEquals(requestId, context.getRequestId());
        assertFalse(context.isCancelled());
    }

    @Test
    void testRemoveContext() {
        String requestId = "test-request-2";
        handler.createContext(requestId);
        assertEquals(1, handler.getActiveRequestCount());

        handler.removeContext(requestId);
        assertEquals(0, handler.getActiveRequestCount());
    }

    @Test
    void testIsCancelled() {
        String requestId = "test-request-3";
        handler.createContext(requestId);

        assertFalse(handler.isCancelled(requestId));

        handler.cancel(requestId, "Test cancellation");
        assertTrue(handler.isCancelled(requestId));
    }

    @Test
    void testIsCancelledForNonExistentRequest() {
        assertFalse(handler.isCancelled("non-existent"));
    }

    @Test
    void testCancel() {
        String requestId = "test-request-4";
        ClientDisconnectHandler.RequestContext context = handler.createContext(requestId);

        assertFalse(context.isCancelled());
        assertNull(context.getCancellationReason());

        context.cancel("Test reason");

        assertTrue(context.isCancelled());
        assertEquals("Test reason", context.getCancellationReason());
    }

    @Test
    void testOnCancel() {
        String requestId = "test-request-5";
        ClientDisconnectHandler.RequestContext context = handler.createContext(requestId);

        // Test that onCancel emits when cancelled
        StepVerifier.create(context.onCancel())
                .expectSubscription()
                .then(() -> context.cancel("Cancelled"))
                .expectNext("Cancelled")
                .verifyComplete();
    }

    @Test
    void testOnCancelTimeout() {
        String requestId = "test-request-6";
        ClientDisconnectHandler.RequestContext context = handler.createContext(requestId);

        // Test that onCancel doesn't emit if not cancelled
        StepVerifier.create(context.onCancel())
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(100))
                .thenCancel()
                .verify();
    }

    @Test
    void testGetDurationMs() {
        String requestId = "test-request-7";
        ClientDisconnectHandler.RequestContext context = handler.createContext(requestId);

        // Duration should be positive
        assertTrue(context.getDurationMs() >= 0);

        // Wait a bit and check duration increased
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        assertTrue(context.getDurationMs() >= 10);
    }

    @Test
    void testAsDisposable() {
        String requestId = "test-request-8";
        ClientDisconnectHandler.RequestContext context = handler.createContext(requestId);

        var disposable = context.asDisposable();
        assertFalse(disposable.isDisposed());
        assertFalse(context.isCancelled());

        disposable.dispose();
        assertTrue(disposable.isDisposed());
        assertTrue(context.isCancelled());
    }

    @Test
    void testMultipleCancels() {
        String requestId = "test-request-9";
        ClientDisconnectHandler.RequestContext context = handler.createContext(requestId);

        // First cancel
        context.cancel("First");
        assertEquals("First", context.getCancellationReason());

        // Second cancel should be ignored
        context.cancel("Second");
        assertEquals("First", context.getCancellationReason()); // Still first
    }

    @Test
    void testCancelViaHandler() {
        String requestId = "test-request-10";
        handler.createContext(requestId);

        assertFalse(handler.isCancelled(requestId));
        handler.cancel(requestId, "Via handler");
        assertTrue(handler.isCancelled(requestId));
    }

    @Test
    void testCancelNonExistentRequest() {
        // Should not throw
        handler.cancel("non-existent", "Test");
        assertFalse(handler.isCancelled("non-existent"));
    }
}
