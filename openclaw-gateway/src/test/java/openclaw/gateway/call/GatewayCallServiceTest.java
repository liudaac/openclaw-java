package openclaw.gateway.call;

import openclaw.gateway.auth.DeviceAuthV3;
import openclaw.gateway.client.GatewayClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GatewayCallService with dependency injection.
 *
 * <p>Demonstrates the test isolation pattern using GatewayCallService.Testing.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.23
 */
class GatewayCallServiceTest {

    private GatewayCallDeps originalDeps;
    private GatewayCallDeps mockDeps;
    private MockGatewayClient mockClient;

    @BeforeEach
    void setUp() {
        // Save original deps
        originalDeps = GatewayCallService.getInstance().getDeps();

        // Create mock client
        mockClient = new MockGatewayClient();

        // Create mock dependencies
        mockDeps = new GatewayCallDeps();
        mockDeps.setCreateGatewayClient(opts -> mockClient);

        // Set for test
        GatewayCallService.Testing.setDepsForTests(mockDeps);
    }

    @AfterEach
    void tearDown() {
        // Restore original deps
        if (originalDeps != null) {
            GatewayCallService.Testing.setDepsForTests(originalDeps);
        } else {
            GatewayCallService.Testing.resetDepsForTests();
        }
        GatewayCallService.Testing.clearThreadLocal();
    }

    @Test
    void testSuccessfulCall() {
        // Given
        String expectedResult = "{\"status\": \"ok\"}";
        mockClient.setNextResult(expectedResult);

        // When
        GatewayCallService service = GatewayCallService.getInstance();
        GatewayCallService.GatewayCallOptions options = new GatewayCallService.GatewayCallOptions(
                "ws://localhost:8080",
                new DeviceAuthV3.AuthRequest(
                    "test-device",
                    "test-client",
                    new String[]{"read", "write"},
                    "nonce-1234567890",
                    System.currentTimeMillis(),
                    "test-token",
                    "signature-abc123"
                ),
                "testMethod",
                Map.of("key", "value")
        );

        GatewayCallService.GatewayCallResult result = service.call(options).join();

        // Then
        assertTrue(result.success());
        assertEquals(expectedResult, result.result());
        assertNull(result.error());
    }

    @Test
    void testFailedCall() {
        // Given
        String errorMessage = "Connection refused";
        mockClient.setNextError(new RuntimeException(errorMessage));

        // When
        GatewayCallService service = GatewayCallService.getInstance();
        GatewayCallService.GatewayCallOptions options = new GatewayCallService.GatewayCallOptions(
                "ws://localhost:8080",
                new DeviceAuthV3.AuthRequest(
                    "test-device",
                    "test-client",
                    new String[]{"read"},
                    "nonce-0987654321",
                    System.currentTimeMillis(),
                    "test-token",
                    "signature-xyz789"
                ),
                "testMethod",
                Map.of()
        );

        GatewayCallService.GatewayCallResult result = service.call(options).join();

        // Then
        assertFalse(result.success());
        assertNull(result.result());
        assertNotNull(result.error());
        assertTrue(result.error().contains(errorMessage));
    }

    @Test
    void testDepsIsolation() {
        // Given
        GatewayCallDeps deps1 = new GatewayCallDeps();
        deps1.setCreateGatewayClient(opts -> mockClient);

        GatewayCallDeps deps2 = new GatewayCallDeps();
        MockGatewayClient client2 = new MockGatewayClient();
        client2.setNextResult("result2");
        deps2.setCreateGatewayClient(opts -> client2);

        // When - Set deps in current thread
        GatewayCallService.getInstance().setDeps(deps1);

        // Then - Verify current thread has deps1
        assertSame(deps1, GatewayCallService.getInstance().getDeps());

        // When - Set deps in another thread
        Thread otherThread = new Thread(() -> {
            GatewayCallService.getInstance().setDeps(deps2);
            assertSame(deps2, GatewayCallService.getInstance().getDeps());
        });
        otherThread.start();
        try {
            otherThread.join();
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }

        // Then - Current thread should still have deps1
        assertSame(deps1, GatewayCallService.getInstance().getDeps());
    }

    @Test
    void testResetDeps() {
        // Given
        GatewayCallDeps customDeps = new GatewayCallDeps();
        customDeps.setCreateGatewayClient(opts -> mockClient);
        GatewayCallService.getInstance().setDeps(customDeps);

        // When
        GatewayCallService.getInstance().resetDeps();

        // Then - Should have new default deps
        GatewayCallDeps resetDeps = GatewayCallService.getInstance().getDeps();
        assertNotNull(resetDeps);
        assertNotSame(customDeps, resetDeps);
    }

    /**
     * Mock GatewayClient for testing.
     */
    private static class MockGatewayClient extends GatewayClient {
        private Object nextResult;
        private Throwable nextError;

        public MockGatewayClient() {
            super("ws://localhost:8080", new DeviceAuthV3.AuthRequest(
                "test",
                "test-client",
                new String[]{"read"},
                "nonce-mock",
                System.currentTimeMillis(),
                "token",
                "signature-mock"
            ));
        }

        @Override
        public CompletableFuture<Object> request(String method, Object params) {
            if (nextError != null) {
                return CompletableFuture.failedFuture(nextError);
            }
            return CompletableFuture.completedFuture(nextResult);
        }

        void setNextResult(Object result) {
            this.nextResult = result;
            this.nextError = null;
        }

        void setNextError(Throwable error) {
            this.nextError = error;
            this.nextResult = null;
        }
    }

    private static class Map {
        static java.util.Map<String, String> of(String k1, String v1) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put(k1, v1);
            return map;
        }

        static java.util.Map<String, String> of() {
            return new java.util.HashMap<>();
        }
    }
}
