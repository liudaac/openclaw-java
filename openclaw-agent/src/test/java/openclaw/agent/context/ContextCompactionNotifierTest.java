package openclaw.agent.context;

import openclaw.agent.event.AgentEventEmitter;
import openclaw.agent.event.AgentEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContextCompactionNotifier}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class ContextCompactionNotifierTest {

    private ContextCompactionNotifier notifier;
    private AgentEventEmitter eventEmitter;

    @BeforeEach
    void setUp() {
        eventEmitter = new AgentEventEmitter();
        notifier = new ContextCompactionNotifier(eventEmitter);
    }

    @Test
    void testNotifyCompactionStart() {
        List<ContextCompactionNotifier.CompactionEvent> received = new ArrayList<>();
        notifier.subscribe(received::add);

        notifier.notifyCompactionStart("run-1", "session-1");

        assertEquals(1, received.size());
        assertEquals("run-1", received.get(0).runId());
        assertEquals("session-1", received.get(0).sessionKey());
        assertTrue(received.get(0).isStart());
        assertEquals(ContextCompactionNotifier.CompactionPhase.STARTED, received.get(0).phase());
    }

    @Test
    void testNotifyCompactionEnd() {
        List<ContextCompactionNotifier.CompactionEvent> received = new ArrayList<>();
        notifier.subscribe(received::add);

        // First start
        notifier.notifyCompactionStart("run-1", "session-1");

        // Then end
        notifier.notifyCompactionEnd("run-1", false, true);

        assertEquals(2, received.size());
        assertTrue(received.get(1).isEnd());
        assertEquals(ContextCompactionNotifier.CompactionPhase.ENDED, received.get(1).phase());
        assertFalse(received.get(1).willRetry());
        assertTrue(received.get(1).completed());
    }

    @Test
    void testNotifyCompactionEndWithRetry() {
        List<ContextCompactionNotifier.CompactionEvent> received = new ArrayList<>();
        notifier.subscribe(received::add);

        notifier.notifyCompactionStart("run-1", "session-1");
        notifier.notifyCompactionEnd("run-1", true, false);

        assertTrue(received.get(1).willRetry());
        assertFalse(received.get(1).completed());

        // State should not be cleared when retrying
        assertNotNull(notifier.getCompactionState("run-1"));
    }

    @Test
    void testIsCompactionInFlight() {
        assertFalse(notifier.isCompactionInFlight("run-1"));

        notifier.notifyCompactionStart("run-1", "session-1");
        assertTrue(notifier.isCompactionInFlight("run-1"));

        notifier.notifyCompactionEnd("run-1", false, true);
        assertFalse(notifier.isCompactionInFlight("run-1"));
    }

    @Test
    void testGetCompactionState() {
        assertNull(notifier.getCompactionState("run-1"));

        notifier.notifyCompactionStart("run-1", "session-1");

        ContextCompactionNotifier.CompactionState state = notifier.getCompactionState("run-1");
        assertNotNull(state);
        assertEquals("session-1", state.sessionKey());
        assertEquals(ContextCompactionNotifier.CompactionPhase.STARTED, state.phase());
    }

    @Test
    void testSubscribeAndUnsubscribe() {
        List<ContextCompactionNotifier.CompactionEvent> received = new ArrayList<>();

        Runnable unsubscribe = notifier.subscribe(received::add);

        notifier.notifyCompactionStart("run-1", "session-1");
        assertEquals(1, received.size());

        unsubscribe.run();

        notifier.notifyCompactionEnd("run-1", false, true);
        assertEquals(1, received.size()); // No new events
    }

    @Test
    void testMultipleListeners() {
        List<ContextCompactionNotifier.CompactionEvent> received1 = new ArrayList<>();
        List<ContextCompactionNotifier.CompactionEvent> received2 = new ArrayList<>();

        notifier.subscribe(received1::add);
        notifier.subscribe(received2::add);

        notifier.notifyCompactionStart("run-1", "session-1");

        assertEquals(1, received1.size());
        assertEquals(1, received2.size());
    }

    @Test
    void testListenerExceptionDoesNotBreakOthers() {
        List<ContextCompactionNotifier.CompactionEvent> received = new ArrayList<>();

        notifier.subscribe(evt -> {
            throw new RuntimeException("Test exception");
        });
        notifier.subscribe(received::add);

        assertDoesNotThrow(() -> notifier.notifyCompactionStart("run-1", "session-1"));
        assertEquals(1, received.size());
    }

    @Test
    void testResetForTest() {
        notifier.notifyCompactionStart("run-1", "session-1");
        assertNotNull(notifier.getCompactionState("run-1"));

        notifier.resetForTest();

        assertNull(notifier.getCompactionState("run-1"));
    }

    @Test
    void testNullSessionKeyHandled() {
        List<ContextCompactionNotifier.CompactionEvent> received = new ArrayList<>();
        notifier.subscribe(received::add);

        notifier.notifyCompactionStart("run-1", null);

        assertEquals(1, received.size());
        assertNull(received.get(0).sessionKey());
    }
}
