package openclaw.server.service;

import openclaw.gateway.node.NodeInfo;
import openclaw.gateway.node.NodeRegistry;
import openclaw.gateway.queue.QueueStats;
import openclaw.gateway.queue.WorkItem;
import openclaw.gateway.queue.WorkQueue;
import openclaw.gateway.work.DispatchResult;
import openclaw.gateway.work.DispatchStrategy;
import openclaw.gateway.work.WorkDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GatewayServiceImpl implements GatewayService {
    
    private static final Logger logger = LoggerFactory.getLogger(GatewayServiceImpl.class);
    
    private final NodeRegistry nodeRegistry;
    private final WorkQueue workQueue;
    private final WorkDispatcher workDispatcher;
    
    public GatewayServiceImpl() {
        this.nodeRegistry = new InMemoryNodeRegistry();
        this.workQueue = new InMemoryWorkQueue();
        this.workDispatcher = new InMemoryWorkDispatcher();
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
    
    private class InMemoryNodeRegistry implements NodeRegistry {
        private final Map<String, NodeInfo> nodes = new ConcurrentHashMap<>();
        
        @Override
        public CompletableFuture<Void> register(NodeInfo node) {
            nodes.put(node.getId(), node);
            return CompletableFuture.completedFuture(null);
        }
        
        @Override
        public CompletableFuture<Void> unregister(String nodeId) {
            nodes.remove(nodeId);
            return CompletableFuture.completedFuture(null);
        }
        
        @Override
        public CompletableFuture<Optional<NodeInfo>> getNode(String nodeId) {
            return CompletableFuture.completedFuture(Optional.ofNullable(nodes.get(nodeId)));
        }
        
        @Override
        public CompletableFuture<List<NodeInfo>> listNodes() {
            return CompletableFuture.completedFuture(new ArrayList<>(nodes.values()));
        }
        
        @Override
        public CompletableFuture<Boolean> isNodeHealthy(String nodeId) {
            return CompletableFuture.completedFuture(nodes.containsKey(nodeId));
        }
        
        @Override
        public CompletableFuture<Integer> getNodeCount() {
            return CompletableFuture.completedFuture(nodes.size());
        }
    }
    
    private class InMemoryWorkQueue implements WorkQueue {
        private final Queue<WorkItem> queue = new ConcurrentLinkedQueue<>();
        private final AtomicLong totalEnqueued = new AtomicLong(0);
        private final AtomicLong totalDequeued = new AtomicLong(0);
        
        @Override
        public CompletableFuture<Void> submit(WorkItem work) {
            queue.offer(work);
            totalEnqueued.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
        
        @Override
        public CompletableFuture<Optional<WorkItem>> poll() {
            WorkItem item = queue.poll();
            if (item != null) {
                totalDequeued.incrementAndGet();
            }
            return CompletableFuture.completedFuture(Optional.ofNullable(item));
        }
        
        @Override
        public CompletableFuture<Integer> size() {
            return CompletableFuture.completedFuture(queue.size());
        }
        
        @Override
        public CompletableFuture<QueueStats> getStats() {
            return CompletableFuture.completedFuture(
                new QueueStats(queue.size(), totalEnqueued.get(), totalDequeued.get(), 0, 0.0)
            );
        }
        
        @Override
        public CompletableFuture<Integer> getPendingCount() {
            return CompletableFuture.completedFuture(queue.size());
        }
        
        @Override
        public CompletableFuture<Long> getCompletedCount() {
            return CompletableFuture.completedFuture(totalDequeued.get());
        }
    }
    
    private class InMemoryWorkDispatcher implements WorkDispatcher {
        private DispatchStrategy strategy = DispatchStrategy.ROUND_ROBIN;
        
        @Override
        public CompletableFuture<DispatchResult> dispatch(WorkItem work) {
            return CompletableFuture.completedFuture(
                new DispatchResult(true, work.getId(), "local", null)
            );
        }
        
        @Override
        public void setStrategy(DispatchStrategy strategy) {
            this.strategy = strategy;
        }
    }
}
