package openclaw.desktop.service;

import openclaw.gateway.GatewayService;
import openclaw.gateway.NodeInfo;
import openclaw.gateway.node.NodeRegistry;
import openclaw.gateway.queue.WorkQueue;
import openclaw.gateway.work.WorkDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gateway Service Implementation for Desktop Application.
 *
 * <p>Provides a lightweight in-memory implementation of GatewayService.</p>
 */
@Service
public class DesktopGatewayServiceImpl implements GatewayService {

    private static final Logger logger = LoggerFactory.getLogger(DesktopGatewayServiceImpl.class);

    private final NodeRegistry nodeRegistry;
    private final WorkQueue workQueue;
    private final WorkDispatcher workDispatcher;

    // Connection state
    private String currentUrl;
    private String authToken;
    private boolean autoReconnect;
    private boolean connected;
    private String version;
    private Instant lastConnectedAt;
    private int reconnectAttempts;

    public DesktopGatewayServiceImpl() {
        this.nodeRegistry = new InMemoryNodeRegistry();
        this.workQueue = new InMemoryWorkQueue();
        this.workDispatcher = new InMemoryWorkDispatcher();
        this.version = "2026.3.24";
    }

    @Override
    public CompletableFuture<Void> initialize(GatewayConfig config) {
        logger.info("Initializing gateway service with config: {}", config);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        logger.info("Shutting down gateway service");
        return disconnect();
    }

    @Override
    public CompletableFuture<Boolean> connect(String url, String token, boolean autoReconnect) {
        this.currentUrl = url;
        this.authToken = token;
        this.autoReconnect = autoReconnect;

        logger.info("Connecting to Gateway: {}", url);

        return CompletableFuture.supplyAsync(() -> {
            try {
                this.connected = true;
                this.lastConnectedAt = Instant.now();
                this.reconnectAttempts = 0;
                logger.info("Connected to Gateway");
                return true;
            } catch (Exception e) {
                logger.error("Failed to connect to Gateway", e);
                this.connected = false;
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Disconnecting from Gateway");
            this.connected = false;
            this.currentUrl = null;
            this.authToken = null;
        });
    }

    @Override
    public CompletableFuture<GatewayStatus> getStatus() {
        return CompletableFuture.supplyAsync(() -> {
            List<NodeInfo> nodes = connected ? getNodes().join() : List.of();
            return new GatewayStatus(
                connected,
                currentUrl,
                version,
                lastConnectedAt,
                reconnectAttempts,
                nodes
            );
        });
    }

    @Override
    public CompletableFuture<List<NodeInfo>> getNodes() {
        return nodeRegistry.listNodes()
            .thenApply(registryNodes -> registryNodes.stream()
                .map(this::convertNodeInfo)
                .toList());
    }

    private NodeInfo convertNodeInfo(openclaw.gateway.node.NodeRegistry.NodeInfo registryNode) {
        return new NodeInfo(
            registryNode.id(),
            registryNode.name(),
            registryNode.status().name().toLowerCase(),
            "2026.3.24",
            registryNode.lastHeartbeat()
        );
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
        return workQueue.enqueue(work)
            .thenCompose(v -> workDispatcher.dispatch(work))
            .thenApply(result -> work.id());
    }

    @Override
    public CompletableFuture<WorkStatus> getWorkStatus(String workId) {
        return CompletableFuture.completedFuture(WorkStatus.pending(workId));
    }

    @Override
    public CompletableFuture<Void> cancelWork(String workId) {
        return CompletableFuture.completedFuture(null);
    }

    // Inner classes for in-memory implementations

    private class InMemoryNodeRegistry implements NodeRegistry {
        private final Map<String, openclaw.gateway.node.NodeRegistry.NodeInfo> nodes = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<RegistrationResult> registerNode(openclaw.gateway.node.NodeRegistry.NodeInfo node) {
            nodes.put(node.id(), node);
            return CompletableFuture.completedFuture(RegistrationResult.success(node.id()));
        }

        @Override
        public CompletableFuture<Void> unregisterNode(String nodeId) {
            nodes.remove(nodeId);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<openclaw.gateway.node.NodeRegistry.NodeInfo>> getNode(String nodeId) {
            return CompletableFuture.completedFuture(Optional.ofNullable(nodes.get(nodeId)));
        }

        @Override
        public CompletableFuture<List<openclaw.gateway.node.NodeRegistry.NodeInfo>> listNodes() {
            return CompletableFuture.completedFuture(new ArrayList<>(nodes.values()));
        }

        @Override
        public CompletableFuture<List<openclaw.gateway.node.NodeRegistry.NodeInfo>> listNodesByStatus(NodeStatus status) {
            List<openclaw.gateway.node.NodeRegistry.NodeInfo> filtered = nodes.values().stream()
                .filter(n -> n.status() == status)
                .toList();
            return CompletableFuture.completedFuture(filtered);
        }

        @Override
        public CompletableFuture<Void> heartbeat(String nodeId) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> isNodeHealthy(String nodeId) {
            return CompletableFuture.completedFuture(nodes.containsKey(nodeId));
        }
    }

    private class InMemoryWorkQueue implements WorkQueue {
        private final Queue<WorkItem> queue = new ConcurrentLinkedQueue<>();
        private final AtomicLong totalEnqueued = new AtomicLong(0);
        private final AtomicLong totalDequeued = new AtomicLong(0);

        @Override
        public CompletableFuture<Void> enqueue(WorkItem item) {
            queue.offer(item);
            totalEnqueued.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<WorkItem>> dequeue() {
            WorkItem item = queue.poll();
            if (item != null) {
                totalDequeued.incrementAndGet();
            }
            return CompletableFuture.completedFuture(Optional.ofNullable(item));
        }

        @Override
        public CompletableFuture<Optional<WorkItem>> peek() {
            return CompletableFuture.completedFuture(Optional.ofNullable(queue.peek()));
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
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<Void> clear() {
            queue.clear();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<List<WorkItem>> getPendingItems() {
            return CompletableFuture.completedFuture(new ArrayList<>(queue));
        }

        @Override
        public CompletableFuture<Boolean> remove(String workId) {
            return CompletableFuture.completedFuture(queue.removeIf(item -> item.id().equals(workId)));
        }

        @Override
        public CompletableFuture<Optional<WorkItem>> getItem(String workId) {
            return CompletableFuture.completedFuture(
                queue.stream().filter(item -> item.id().equals(workId)).findFirst()
            );
        }

        @Override
        public CompletableFuture<QueueStats> getStats() {
            return CompletableFuture.completedFuture(
                new QueueStats(queue.size(), 10000, totalEnqueued.get(), totalDequeued.get(), 0.0)
            );
        }
    }

    private class InMemoryWorkDispatcher implements WorkDispatcher {
        private DispatchStrategy strategy = DispatchStrategy.ROUND_ROBIN;

        @Override
        public CompletableFuture<DispatchResult> dispatch(WorkItem work) {
            return CompletableFuture.completedFuture(
                DispatchResult.success(work.id(), "local")
            );
        }

        @Override
        public CompletableFuture<DispatchResult> dispatchToNode(WorkItem work, String nodeId) {
            return CompletableFuture.completedFuture(
                DispatchResult.success(work.id(), nodeId)
            );
        }

        @Override
        public DispatchStrategy getStrategy() {
            return strategy;
        }

        @Override
        public void setStrategy(DispatchStrategy strategy) {
            this.strategy = strategy;
        }
    }
}
