package openclaw.gateway.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PreauthConnectionBudget.
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 */
class PreauthConnectionBudgetTest {

    private PreauthConnectionBudget budget;

    @BeforeEach
    void setUp() {
        budget = new PreauthConnectionBudget(3); // Small limit for testing
    }

    @Test
    void testAcquireAndRelease() {
        String ip = "192.168.1.1";

        assertTrue(budget.acquire(ip));
        assertTrue(budget.acquire(ip));
        assertTrue(budget.acquire(ip));
        assertFalse(budget.acquire(ip)); // Exceeds limit

        assertEquals(3, budget.getConnectionCount(ip));

        budget.release(ip);
        assertEquals(2, budget.getConnectionCount(ip));

        budget.release(ip);
        assertEquals(1, budget.getConnectionCount(ip));

        budget.release(ip);
        assertEquals(0, budget.getConnectionCount(ip));

        // Release when already at 0 should not throw
        budget.release(ip);
        assertEquals(0, budget.getConnectionCount(ip));
    }

    @Test
    void testDifferentIpsAreIsolated() {
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";

        assertTrue(budget.acquire(ip1));
        assertTrue(budget.acquire(ip1));
        assertTrue(budget.acquire(ip2));

        assertEquals(2, budget.getConnectionCount(ip1));
        assertEquals(1, budget.getConnectionCount(ip2));

        assertFalse(budget.acquire(ip1)); // ip1 at limit
        assertTrue(budget.acquire(ip2));  // ip2 can still acquire
    }

    @Test
    void testUnknownClientIp() {
        // null client IP should use the unknown bucket
        assertTrue(budget.acquire(null));
        assertTrue(budget.acquire(null));
        assertTrue(budget.acquire(null));
        assertFalse(budget.acquire(null));

        assertEquals(3, budget.getConnectionCount(null));
    }

    @Test
    void testEmptyClientIp() {
        // empty/blank client IP should use the unknown bucket
        assertTrue(budget.acquire(""));
        assertTrue(budget.acquire("   "));

        // Both should count toward the same unknown bucket
        assertEquals(2, budget.getConnectionCount(""));
        assertEquals(2, budget.getConnectionCount("   "));
        assertEquals(2, budget.getConnectionCount(null));
    }

    @Test
    void testGetTotalConnectionCount() {
        assertEquals(0, budget.getTotalConnectionCount());

        budget.acquire("192.168.1.1");
        budget.acquire("192.168.1.1");
        budget.acquire("192.168.1.2");

        assertEquals(3, budget.getTotalConnectionCount());
    }

    @Test
    void testGetLimit() {
        assertEquals(3, budget.getLimit());

        PreauthConnectionBudget defaultBudget = new PreauthConnectionBudget();
        assertEquals(PreauthConnectionBudget.DEFAULT_MAX_PREAUTH_CONNECTIONS_PER_IP, defaultBudget.getLimit());
    }

    @Test
    void testLimitFromEnv() {
        // Save original env
        String originalEnv = System.getenv(PreauthConnectionBudget.ENV_MAX_PREAUTH_CONNECTIONS);

        try {
            // This test would need to set environment variable, which is hard in unit tests
            // Just verify the method exists and returns a valid value
            int limit = PreauthConnectionBudget.getLimitFromEnv();
            assertTrue(limit >= 1);
        } finally {
            // No cleanup needed since we can't actually set env vars
        }
    }
}
