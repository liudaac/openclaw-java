package openclaw.gateway.queue;

import openclaw.gateway.GatewayService.WorkItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of work queue.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class InMemoryWorkQueue implements WorkQueue {

    private final int capacity;
    private final PriorityBlockingQueue<QueuedItem> queue;
    private final AtomicLong totalEnqueued;
    private final AtomicLong totalDequeued;
    private volatile long totalWaitTimeMs;

    public InMemoryWorkQueue(int capacity) {
        this.capacity = capacity;
        this.queue = new PriorityBlockingQueue<>(capacity, 
                (a, b) -> Integer.compare(b.priority(), a.priority()));
        this.totalEnqueued = new AtomicLong(0);
        this.totalDequeued = new AtomicLong(0);
    }

    @Override
    public CompletableFuture<Void> enqueue(WorkItem item) {
        return CompletableFuture.runAsync(() -> {
            if (queue.size() >= capacity) {
                throw new QueueFullException("Work queue is full");
            }
            
            QueuedItem queuedItem = new QueuedItem(
                    item,
                    System.currentTimeMillis()
            );
            
            queue.offer(queuedItem);
            totalEnqueued.incrementAndGet();
        });
    }

    @Override
    public CompletableFuture<Optional<WorkItem>> dequeue() {
        return CompletableFuture.supplyAsync(() -> {
            QueuedItem item = queue.poll();
            if (item != null) {
                totalDequeued.incrementAndGet();
                long waitTime = System.currentTimeMillis() - item.enqueuedAt();
                totalWaitTimeMs += waitTime;
                return Optional.of(item.workItem());
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<WorkItem>> peek() {
        return CompletableFuture.supplyAsync(() -> {
            QueuedItem item = queue.peek();
            return Optional.ofNullable(item).map(QueuedItem::workItem);
        });
    }

    @Override
    public CompletableFuture<Integer> size() {
        return CompletableFuture.completedFuture(queue.size());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return CompletableFuture.completedFuture(queue.isEmpty());
    }

    @Override
    public CompletableFuture<Boolean> isFull() {
        return CompletableFuture.completedFuture(queue.size() >= capacity);
    }

    @Override
    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> {
            queue.clear();
        });
    }

    @Override
    public CompletableFuture<List<WorkItem>> getPendingItems() {
        return CompletableFuture.supplyAsync(() -> {
            return queue.stream()
                    .map(QueuedItem::workItem)
                    .toList();
        });
    }

    @Override
    public CompletableFuture<Boolean> remove(String workId) {
        return CompletableFuture.supplyAsync(() -> {
            return queue.removeIf(item -> item.workItem().id().equals(workId));
        });
    }

    @Override
    public CompletableFuture<Optional<WorkItem>> getItem(String workId) {
        return CompletableFuture.supplyAsync(() -> {
            return queue.stream()
                    .filter(item -> item.workItem().id().equals(workId))
                    .findFirst()
                    .map(QueuedItem::workItem);
        });
    }

    @Override
    public CompletableFuture<QueueStats> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            long dequeued = totalDequeued.get();
            double avgWait = dequeued > 0 ? (double) totalWaitTimeMs / dequeued : 0;
            
            return new QueueStats(
                    queue.size(),
                    capacity,
                    totalEnqueued.get(),
                    dequeued,
                    avgWait
            );
        });
    }

    /**
     * Queued item wrapper.
     *
     * @param workItem the work item
     * @param enqueuedAt the enqueue timestamp
     */
    private record QueuedItem(
            WorkItem workItem,
            long enqueuedAt
    ) {
        /**
         * Gets the effective priority (higher = more urgent).
         *
         * @return the priority
         */
        public int priority() {
            // Priority decreases with wait time
            long waitTime = System.currentTimeMillis() - enqueuedAt;
            int ageBonus = (int) (waitTime / 10000); // +1 priority per 10 seconds
            return workItem.priority() + ageBonus;
        }
    }

    /**
     * Queue full exception.
     */
    public static class QueueFullException extends RuntimeException {
        public QueueFullException(String message) {
            super(message);
        }
    }
}
