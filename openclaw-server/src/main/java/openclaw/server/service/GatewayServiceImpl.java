package openclaw.server.service;

import openclaw.gateway.GatewayService;
import openclaw.gateway.GatewayService.*;
import openclaw.gateway.node.NodeRegistry;
import openclaw.gateway.queue.WorkQueue;
import openclaw.gateway.work.WorkDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway Service Implementation
 */
@Service
public class GatewayServiceImpl implements GatewayService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayServiceImpl.class);

    private final Map<String, WorkStatus> workStatusMap = new ConcurrentHashMap<>();
    private final NodeRegistry nodeRegistry;
    private final WorkQueue workQueue;
    private final WorkDispatcher workDispatcher;

    public GatewayServiceImpl() {
        // Initialize with in-memory implementations
        this.nodeRegistry = new InMemoryNodeRegistry();
        this.workQueue = new InMemoryWorkQueue();
        this.workDispatcher = new InMemoryWorkDispatcher();
    }

    @Override
    public CompletableFuture<Void> initialize(GatewayConfig config) {
        logger.info("Initializing Gateway Service on port {}", config.port());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        logger.info("Shutting down Gateway Service");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public NodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    @Override
    public WorkQueue getWorkQueue() {
        return workQueue;
    }

    @Override
    public WorkDispatcher getWorkDispatcher() {
        return workDispatcher;
    }

    @Override
    public CompletableFuture<String> submitWork(WorkItem work) {
        String workId = UUID.randomUUID().toString();
        workStatusMap.put(workId, WorkStatus.pending(workId));
        logger.info("Submitted work: {} of type {}", workId, work.type());
        return CompletableFuture.completedFuture(workId);
    }

    @Override
    public CompletableFuture<WorkStatus> getWorkStatus(String workId) {
        WorkStatus status = workStatusMap.getOrDefault(workId, WorkStatus.failed(workId, "Work not found"));
        return CompletableFuture.completedFuture(status);
    }

    @Override
    public CompletableFuture<Void> cancelWork(String workId) {
        workStatusMap.remove(workId);
        logger.info("Cancelled work: {}", workId);
        return CompletableFuture.completedFuture(null);
    }

    // In-memory implementations
    private static class InMemoryNodeRegistry implements NodeRegistry {
        @Override
        public int getNodeCount() {
            return 1;
        }
    }

    private static class InMemoryWorkQueue implements WorkQueue {
        @Override
        public int getPendingCount() {
            return 0;
        }

        @Override
        public long getCompletedCount() {
            return 0;
        }
    }

    private static class InMemoryWorkDispatcher implements WorkDispatcher {
    }
}
