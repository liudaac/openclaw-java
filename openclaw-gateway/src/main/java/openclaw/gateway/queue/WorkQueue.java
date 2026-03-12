package openclaw.gateway.queue;

import openclaw.gateway.GatewayService.WorkItem;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Work queue for pending work items.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface WorkQueue {

    /**
     * Enqueues a work item.
     *
     * @param item the work item
     * @return completion future
     */
    CompletableFuture<Void> enqueue(WorkItem item);

    /**
     * Dequeues a work item.
     *
     * @return the work item if available
     */
    CompletableFuture<Optional<WorkItem>> dequeue();

    /**
     * Peeks at the next work item without removing.
     *
     * @return the work item if available
     */
    CompletableFuture<Optional<WorkItem>> peek();

    /**
     * Gets the queue size.
     *
     * @return the size
     */
    CompletableFuture<Integer> size();

    /**
     * Checks if the queue is empty.
     *
     * @return true if empty
     */
    CompletableFuture<Boolean> isEmpty();

    /**
     * Checks if the queue is full.
     *
     * @return true if full
     */
    CompletableFuture<Boolean> isFull();

    /**
     * Clears the queue.
     *
     * @return completion future
     */
    CompletableFuture<Void> clear();

    /**
     * Gets all pending items.
     *
     * @return list of items
     */
    CompletableFuture<List<WorkItem>> getPendingItems();

    /**
     * Removes a specific item.
     *
     * @param workId the work ID
     * @return true if removed
     */
    CompletableFuture<Boolean> remove(String workId);

    /**
     * Gets an item by ID.
     *
     * @param workId the work ID
     * @return the item if found
     */
    CompletableFuture<Optional<WorkItem>> getItem(String workId);

    /**
     * Gets queue statistics.
     *
     * @return the statistics
     */
    CompletableFuture<QueueStats> getStats();

    /**
     * Queue statistics.
     *
     * @param size current size
     * @param capacity total capacity
     * @param totalEnqueued total enqueued
     * @param totalDequeued total dequeued
     * @param averageWaitTimeMs average wait time
     */
    record QueueStats(
            int size,
            int capacity,
            long totalEnqueued,
            long totalDequeued,
            double averageWaitTimeMs
    ) {

        /**
         * Gets the utilization percentage.
         *
         * @return the percentage
         */
        public double utilizationPercent() {
            return capacity > 0 ? (size * 100.0) / capacity : 0;
        }
    }
}
