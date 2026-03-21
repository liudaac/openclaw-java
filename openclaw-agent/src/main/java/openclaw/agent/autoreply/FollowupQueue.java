package openclaw.agent.autoreply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Queue for managing followup runs.
 *
 * <p>Provides:
 * <ul>
 *   <li>Enqueue/dequeue operations</li>
 *   <li>Deduplication</li>
 *   <li>Rate limiting</li>
 *   <li>Queue depth tracking</li>
 * </ul>
 * </p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class FollowupQueue {

    private static final Logger logger = LoggerFactory.getLogger(FollowupQueue.class);

    private final Queue<FollowupRun> queue;
    private final Set<String> dedupeSet;
    private final AtomicInteger depth;
    private final QueueSettings settings;
    private final ExecutorService executor;
    private volatile boolean draining;

    public FollowupQueue(QueueSettings settings) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.dedupeSet = ConcurrentHashMap.newKeySet();
        this.depth = new AtomicInteger(0);
        this.settings = settings != null ? settings : QueueSettings.defaults();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "followup-queue");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Enqueues a followup run.
     *
     * @param run the run to enqueue
     * @return true if enqueued, false if dropped
     */
    public boolean enqueue(FollowupRun run) {
        if (run == null || run.prompt() == null) {
            return false;
        }

        // Check deduplication
        String dedupeKey = generateDedupeKey(run);
        if (dedupeSet.contains(dedupeKey)) {
            logger.debug("Dropping duplicate followup run: {}", dedupeKey);
            return false;
        }

        // Check capacity
        int currentDepth = depth.get();
        if (settings.cap() > 0 && currentDepth >= settings.cap()) {
            logger.warn("Followup queue at capacity ({}), dropping new run", settings.cap());
            return applyDropPolicy(run);
        }

        // Add to queue
        queue.offer(run);
        dedupeSet.add(dedupeKey);
        depth.incrementAndGet();

        logger.debug("Enqueued followup run, depth: {}", depth.get());
        return true;
    }

    /**
     * Dequeues the next followup run.
     *
     * @return the next run or null if empty
     */
    public FollowupRun dequeue() {
        FollowupRun run = queue.poll();
        if (run != null) {
            dedupeSet.remove(generateDedupeKey(run));
            depth.decrementAndGet();
        }
        return run;
    }

    /**
     * Peeks at the next followup run without removing.
     *
     * @return the next run or null if empty
     */
    public FollowupRun peek() {
        return queue.peek();
    }

    /**
     * Gets the current queue depth.
     *
     * @return the depth
     */
    public int getDepth() {
        return depth.get();
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Clears the queue.
     */
    public void clear() {
        queue.clear();
        dedupeSet.clear();
        depth.set(0);
        logger.info("Followup queue cleared");
    }

    /**
     * Starts draining the queue with the given runner.
     *
     * @param runner the runner to execute queued runs
     */
    public void startDraining(FollowupRunner runner) {
        if (draining) {
            return;
        }
        draining = true;

        executor.submit(() -> {
            while (draining) {
                try {
                    FollowupRun run = dequeue();
                    if (run == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    // Apply debounce
                    if (settings.debounceMs() > 0) {
                        Thread.sleep(settings.debounceMs());
                    }

                    // Execute run
                    runner.run(run).join();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing followup run: {}", e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Stops draining the queue.
     */
    public void stopDraining() {
        draining = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generates a deduplication key for a run.
     */
    private String generateDedupeKey(FollowupRun run) {
        return switch (settings.dedupeMode()) {
            case MESSAGE_ID -> run.messageId() != null ? run.messageId() : run.prompt();
            case PROMPT -> run.prompt();
            case NONE -> UUID.randomUUID().toString();
        };
    }

    /**
     * Applies the drop policy when queue is at capacity.
     */
    private boolean applyDropPolicy(FollowupRun run) {
        return switch (settings.dropPolicy()) {
            case OLD -> {
                // Remove oldest and add new
                FollowupRun oldest = queue.poll();
                if (oldest != null) {
                    dedupeSet.remove(generateDedupeKey(oldest));
                    depth.decrementAndGet();
                }
                queue.offer(run);
                dedupeSet.add(generateDedupeKey(run));
                depth.incrementAndGet();
                true;
            }
            case NEW -> {
                // Drop the new run
                false;
            }
            case SUMMARIZE -> {
                // Would summarize older runs - for now just drop
                logger.warn("Summarize drop policy not yet implemented");
                false;
            }
        };
    }

    /**
     * Queue settings.
     */
    public record QueueSettings(
            QueueMode mode,
            long debounceMs,
            int cap,
            QueueDropPolicy dropPolicy,
            QueueDedupeMode dedupeMode
    ) {
        public static QueueSettings defaults() {
            return new QueueSettings(
                    QueueMode.FOLLOWUP,
                    0,
                    100,
                    QueueDropPolicy.NEW,
                    QueueDedupeMode.MESSAGE_ID
            );
        }
    }

    /**
     * Queue mode.
     */
    public enum QueueMode {
        STEER,
        FOLLOWUP,
        COLLECT,
        STEER_BACKLOG,
        INTERRUPT,
        QUEUE
    }

    /**
     * Queue drop policy.
     */
    public enum QueueDropPolicy {
        OLD,
        NEW,
        SUMMARIZE
    }

    /**
     * Queue deduplication mode.
     */
    public enum QueueDedupeMode {
        MESSAGE_ID,
        PROMPT,
        NONE
    }
}
