package openclaw.gateway.node;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Node registry for managing connected nodes.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface NodeRegistry {

    /**
     * Registers a node.
     *
     * @param node the node info
     * @return registration result
     */
    CompletableFuture<RegistrationResult> registerNode(NodeInfo node);

    /**
     * Unregisters a node.
     *
     * @param nodeId the node ID
     * @return completion future
     */
    CompletableFuture<Void> unregisterNode(String nodeId);

    /**
     * Gets a node by ID.
     *
     * @param nodeId the node ID
     * @return the node if found
     */
    CompletableFuture<Optional<NodeInfo>> getNode(String nodeId);

    /**
     * Lists all nodes.
     *
     * @return list of nodes
     */
    CompletableFuture<List<NodeInfo>> listNodes();

    /**
     * Lists nodes by status.
     *
     * @param status the status
     * @return list of nodes
     */
    CompletableFuture<List<NodeInfo>> listNodesByStatus(NodeStatus status);

    /**
     * Updates node heartbeat.
     *
     * @param nodeId the node ID
     * @return completion future
     */
    CompletableFuture<Void> heartbeat(String nodeId);

    /**
     * Checks if a node is healthy.
     *
     * @param nodeId the node ID
     * @return true if healthy
     */
    CompletableFuture<Boolean> isNodeHealthy(String nodeId);

    /**
     * Gets the total node count.
     *
     * @return the count
     */
    default CompletableFuture<Integer> getNodeCount() {
        return listNodes().thenApply(List::size);
    }

    /**
     * Node information.
     *
     * @param id the node ID
     * @param name the node name
     * @param host the host address
     * @param port the port
     * @param status the status
     * @param capabilities the capabilities
     * @param registeredAt registration timestamp
     * @param lastHeartbeat last heartbeat timestamp
     * @param metadata additional metadata
     */
    record NodeInfo(
            String id,
            String name,
            String host,
            int port,
            NodeStatus status,
            List<String> capabilities,
            Instant registeredAt,
            Instant lastHeartbeat,
            Map<String, Object> metadata
    ) {

        /**
         * Creates a builder for NodeInfo.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for NodeInfo.
         */
        public static class Builder {
            private String id;
            private String name;
            private String host;
            private int port;
            private NodeStatus status = NodeStatus.ONLINE;
            private List<String> capabilities = List.of();
            private Instant registeredAt = Instant.now();
            private Instant lastHeartbeat = Instant.now();
            private Map<String, Object> metadata = Map.of();

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder host(String host) {
                this.host = host;
                return this;
            }

            public Builder port(int port) {
                this.port = port;
                return this;
            }

            public Builder status(NodeStatus status) {
                this.status = status;
                return this;
            }

            public Builder capabilities(List<String> capabilities) {
                this.capabilities = capabilities != null ? capabilities : List.of();
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata != null ? metadata : Map.of();
                return this;
            }

            public NodeInfo build() {
                return new NodeInfo(id, name, host, port, status, capabilities,
                        registeredAt, lastHeartbeat, metadata);
            }
        }
    }

    /**
     * Node status.
     */
    enum NodeStatus {
        ONLINE,
        OFFLINE,
        BUSY,
        DORMANT,
        ERROR
    }

    /**
     * Registration result.
     *
     * @param success whether registration succeeded
     * @param nodeId the node ID
     * @param message the message
     */
    record RegistrationResult(
            boolean success,
            String nodeId,
            String message
    ) {

        /**
         * Creates a successful result.
         *
         * @param nodeId the node ID
         * @return the result
         */
        public static RegistrationResult success(String nodeId) {
            return new RegistrationResult(true, nodeId, "Registered successfully");
        }

        /**
         * Creates a failed result.
         *
         * @param message the error message
         * @return the result
         */
        public static RegistrationResult failure(String message) {
            return new RegistrationResult(false, null, message);
        }
    }
}
