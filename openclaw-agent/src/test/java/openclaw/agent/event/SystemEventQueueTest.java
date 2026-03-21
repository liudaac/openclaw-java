package openclaw.agent.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SystemEventQueue}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class SystemEventQueueTest {

    private SystemEventQueue queue;

    @BeforeEach
    void setUp() {
        queue = new SystemEventQueue();
    }

    @Test
    void testEnqueueAndDrain() {
        assertTrue(queue.enqueue("Test event", new SystemEventQueue.SystemEventOptions("session-1")));

        List<SystemEventQueue.SystemEvent> events = queue.drain("session-1");
        assertEquals(1, events.size());
        assertEquals("Test event", events.get(0).text());
    }

    @Test
    void testDrainTexts() {
        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1"));
        queue.enqueue("Event 2", new SystemEventQueue.SystemEventOptions("session-1"));

        List<String> texts = queue.drainTexts("session-1");
        assertEquals(2, texts.size());
        assertEquals("Event 1", texts.get(0));
        assertEquals("Event 2", texts.get(1));
    }

    @Test
    void testDrainClearsQueue() {
        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1"));

        queue.drain("session-1");

        List<SystemEventQueue.SystemEvent> events = queue.drain("session-1");
        assertTrue(events.isEmpty());
    }

    @Test
    void testPeek() {
        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1"));

        List<SystemEventQueue.SystemEvent> events = queue.peek("session-1");
        assertEquals(1, events.size());

        // Queue should not be cleared
        events = queue.peek("session-1");
        assertEquals(1, events.size());
    }

    @Test
    void testSkipConsecutiveDuplicates() {
        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1"));
        assertFalse(queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1"))); // Duplicate
        assertTrue(queue.enqueue("Event 2", new SystemEventQueue.SystemEventOptions("session-1"))); // Different

        List<SystemEventQueue.SystemEvent> events = queue.drain("session-1");
        assertEquals(2, events.size());
    }

    @Test
    void testSkipEmptyText() {
        assertFalse(queue.enqueue("", new SystemEventQueue.SystemEventOptions("session-1")));
        assertFalse(queue.enqueue("   ", new SystemEventQueue.SystemEventOptions("session-1")));
        assertFalse(queue.enqueue(null, new SystemEventQueue.SystemEventOptions("session-1")));
    }

    @Test
    void testMaxEventsLimit() {
        // Enqueue more than MAX_EVENTS (20)
        for (int i = 0; i < 25; i++) {
            queue.enqueue("Event " + i, new SystemEventQueue.SystemEventOptions("session-1"));
        }

        List<SystemEventQueue.SystemEvent> events = queue.drain("session-1");
        assertEquals(20, events.size()); // Should be limited to 20
    }

    @Test
    void testHasEvents() {
        assertFalse(queue.hasEvents("session-1"));

        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1"));
        assertTrue(queue.hasEvents("session-1"));

        queue.drain("session-1");
        assertFalse(queue.hasEvents("session-1"));
    }

    @Test
    void testContextChanged() {
        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1", "context-a"));

        assertTrue(queue.isContextChanged("session-1", "context-b"));
        assertFalse(queue.isContextChanged("session-1", "context-a"));
        assertTrue(queue.isContextChanged("session-1", null));
    }

    @Test
    void testClear() {
        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1"));
        assertTrue(queue.hasEvents("session-1"));

        queue.clear("session-1");
        assertFalse(queue.hasEvents("session-1"));
    }

    @Test
    void testSeparateSessions() {
        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1"));
        queue.enqueue("Event 2", new SystemEventQueue.SystemEventOptions("session-2"));

        List<SystemEventQueue.SystemEvent> events1 = queue.drain("session-1");
        List<SystemEventQueue.SystemEvent> events2 = queue.drain("session-2");

        assertEquals(1, events1.size());
        assertEquals("Event 1", events1.get(0).text());

        assertEquals(1, events2.size());
        assertEquals("Event 2", events2.get(0).text());
    }

    @Test
    void testNullSessionKeyThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                queue.enqueue("Event", new SystemEventQueue.SystemEventOptions(null))
        );

        assertThrows(IllegalArgumentException.class, () ->
                queue.drain(null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                queue.drain("")
        );
    }

    @Test
    void testResetForTest() {
        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1"));
        queue.enqueue("Event 2", new SystemEventQueue.SystemEventOptions("session-2"));

        queue.resetForTest();

        assertFalse(queue.hasEvents("session-1"));
        assertFalse(queue.hasEvents("session-2"));
    }

    @Test
    void testContextKeyNormalization() {
        queue.enqueue("Event 1", new SystemEventQueue.SystemEventOptions("session-1", "CONTEXT-A"));

        // Should be normalized to lowercase
        assertFalse(queue.isContextChanged("session-1", "context-a"));
        assertTrue(queue.isContextChanged("session-1", "CONTEXT-B"));
    }
}
