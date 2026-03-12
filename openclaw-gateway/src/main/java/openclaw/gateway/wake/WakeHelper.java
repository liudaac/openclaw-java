package openclaw.gateway.wake;

import openclaw.gateway.node.NodeRegistry;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wake helper for waking up dormant nodes.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class WakeHelper {

    private final NodeRegistry nodeRegistry;
    private final ScheduledExecutorService scheduler;
    private final long checkIntervalMs;

    public WakeHelper(NodeRegistry nodeRegistry) {
        this(nodeRegistry, 30000); // 30 seconds default
    }

    public WakeHelper(NodeRegistry nodeRegistry, long checkIntervalMs) {
        this.nodeRegistry = nodeRegistry;
        this.checkIntervalMs = checkIntervalMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "wake-helper");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts the wake helper.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
                this::checkAndWakeNodes,
                checkIntervalMs,
                checkIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops the wake helper.
     */
    public void stop() {
        scheduler.shutdown();
    }

    /**
     * Wakes a specific node.
     *
     * @param nodeId the node ID
     * @return true if wake succeeded
     */
    public CompletableFuture<Boolean> wakeNode(String nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if node exists and is dormant
                var nodeOpt = nodeRegistry.getNode(nodeId).join();
                if (nodeOpt.isEmpty()) {
                    return false;
                }

                var node = nodeOpt.get();
                if (node.status() != NodeRegistry.NodeStatus.DORMANT) {
                    return false;
                }

                // Send wake signal
                boolean success = sendWakeSignal(node);

                if (success) {
                    // Update node status
                    nodeRegistry.heartbeat(nodeId).join();
                }

                return success;
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void checkAndWakeNodes() {
        try {
            nodeRegistry.listNodesByStatus(NodeRegistry.NodeStatus.DORMANT)
                    .thenAccept(nodes -> {
                        for (var node : nodes) {
                            // Check if node has pending work
                            if (hasPendingWork(node.id())) {
                                wakeNode(node.id());
                            }
                        }
                    })
                    .join();
        } catch (Exception e) {
            // Log error but don't throw
        }
    }

    private boolean hasPendingWork(String nodeId) {
        // Check if node has pending work in queue
        // Implementation depends on work queue integration
        return false;
    }

    private boolean sendWakeSignal(NodeRegistry.NodeInfo node) {
        // Send wake signal to node
        // Implementation depends on network protocol
        try {
            // Example: HTTP wake request
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://" + node.host() + ":" + node.port() + "/wake"))
                    .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();

            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
