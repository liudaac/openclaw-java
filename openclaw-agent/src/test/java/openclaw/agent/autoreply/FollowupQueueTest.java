package openclaw.agent.autoreply;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FollowupQueue}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class FollowupQueueTest {

    private FollowupQueue queue;

    @BeforeEach
    void setUp() {
        queue = new FollowupQueue(FollowupQueue.QueueSettings.defaults());
    }

    @Test
    void testEnqueueAndDequeue() {
        FollowupRun run = createFollowupRun("test-prompt");

        assertTrue(queue.enqueue(run));
        assertEquals(1, queue.getDepth());

        FollowupRun dequeued = queue.dequeue();
        assertNotNull(dequeued);
        assertEquals("test-prompt", dequeued.prompt());
        assertEquals(0, queue.getDepth());
    }

    @Test
    void testDeduplicationByMessageId() {
        FollowupRun run1 = new FollowupRun(
                "prompt-1", "msg-1", null, Instant.now(),
                null, null, null, null, null, null
        );
        FollowupRun run2 = new FollowupRun(
                "prompt-2", "msg-1", null, Instant.now(),
                null, null, null, null, null, null
        );

        assertTrue(queue.enqueue(run1));
        assertFalse(queue.enqueue(run2)); // Duplicate message ID
        assertEquals(1, queue.getDepth());
    }

    @Test
    void testDeduplicationByPrompt() {
        queue = new FollowupQueue(new FollowupQueue.QueueSettings(
                FollowupQueue.QueueMode.FOLLOWUP, 0, 100,
                FollowupQueue.QueueDropPolicy.NEW,
                FollowupQueue.QueueDedupeMode.PROMPT
        ));

        FollowupRun run1 = createFollowupRun("same-prompt");
        FollowupRun run2 = createFollowupRun("same-prompt");

        assertTrue(queue.enqueue(run1));
        assertFalse(queue.enqueue(run2)); // Duplicate prompt
        assertEquals(1, queue.getDepth());
    }

    @Test
    void testCapacityLimit() {
        queue = new FollowupQueue(new FollowupQueue.QueueSettings(
                FollowupQueue.QueueMode.FOLLOWUP, 0, 2,
                FollowupQueue.QueueDropPolicy.NEW,
                FollowupQueue.QueueDedupeMode.NONE
        ));

        assertTrue(queue.enqueue(createFollowupRun("prompt-1")));
        assertTrue(queue.enqueue(createFollowupRun("prompt-2")));
        assertFalse(queue.enqueue(createFollowupRun("prompt-3"))); // At capacity
        assertEquals(2, queue.getDepth());
    }

    @Test
    void testDropPolicyOld() {
        queue = new FollowupQueue(new FollowupQueue.QueueSettings(
                FollowupQueue.QueueMode.FOLLOWUP, 0, 2,
                FollowupQueue.QueueDropPolicy.OLD,
                FollowupQueue.QueueDedupeMode.NONE
        ));

        assertTrue(queue.enqueue(createFollowupRun("prompt-1")));
        assertTrue(queue.enqueue(createFollowupRun("prompt-2")));
        assertTrue(queue.enqueue(createFollowupRun("prompt-3"))); // Should drop oldest

        assertEquals(2, queue.getDepth());
        FollowupRun first = queue.dequeue();
        assertEquals("prompt-2", first.prompt()); // Oldest dropped
    }

    @Test
    void testPeek() {
        FollowupRun run = createFollowupRun("test-prompt");
        queue.enqueue(run);

        FollowupRun peeked = queue.peek();
        assertNotNull(peeked);
        assertEquals("test-prompt", peeked.prompt());
        assertEquals(1, queue.getDepth()); // Not removed
    }

    @Test
    void testClear() {
        queue.enqueue(createFollowupRun("prompt-1"));
        queue.enqueue(createFollowupRun("prompt-2"));
        assertEquals(2, queue.getDepth());

        queue.clear();
        assertEquals(0, queue.getDepth());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testDequeueEmpty() {
        assertNull(queue.dequeue());
    }

    @Test
    void testStartAndStopDraining() throws Exception {
        AtomicInteger runCount = new AtomicInteger(0);
        FollowupRunner runner = new FollowupRunner(
                FollowupRunnerOptions.builder().build(),
                null, null, null
        ) {
            @Override
            public CompletableFuture<Void> run(FollowupRun queued) {
                runCount.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };

        queue.enqueue(createFollowupRun("prompt-1"));
        queue.enqueue(createFollowupRun("prompt-2"));

        queue.startDraining(runner);

        // Wait for processing
        Thread.sleep(500);

        queue.stopDraining();

        assertEquals(2, runCount.get());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testNullRunRejected() {
        assertFalse(queue.enqueue(null));
    }

    @Test
    void testRunWithNullPromptRejected() {
        FollowupRun run = new FollowupRun(
                null, null, null, Instant.now(),
                null, null, null, null, null, null
        );
        assertFalse(queue.enqueue(run));
    }

    private FollowupRun createFollowupRun(String prompt) {
        return new FollowupRun(
                prompt, null, null, Instant.now(),
                null, null, null, null, null, null
        );
    }
}
