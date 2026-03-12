package openclaw.gateway;

import openclaw.gateway.node.NodeRegistry;
import openclaw.gateway.queue.WorkQueue;
import openclaw.gateway.work.WorkDispatcher;

import java.util.concurrent.CompletableFuture;

/**
 * Gateway service for managing node communication and work distribution.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface GatewayService {

    /**
     * Initializes the gateway service.
     *
     * @param config the configuration
     * @return completion future
     */
    CompletableFuture<Void> initialize(GatewayConfig config);

    /**
     * Shuts down the gateway service.
     *
     * @return completion future
     */
    CompletableFuture<Void> shutdown();

    /**
     * Gets the node registry.
     *
     * @return the node registry
     */
    NodeRegistry getNodeRegistry();

    /**
     * Gets the work queue.
     *
     * @return the work queue
     */
    WorkQueue getWorkQueue();

    /**
     * Gets the work dispatcher.
     *
     * @return the work dispatcher
     */
    WorkDispatcher getWorkDispatcher();

    /**
     * Submits work to the gateway.
     *
     * @param work the work item
     * @return the work ID
     */
    CompletableFuture<String> submitWork(WorkItem work);

    /**
     * Gets work status.
     *
     * @param workId the work ID
     * @return the status
     */
    CompletableFuture<WorkStatus> getWorkStatus(String workId);

    /**
     * Cancels work.
     *
     * @param workId the work ID
     * @return completion future
     */
    CompletableFuture<Void> cancelWork(String workId);

    /**
     * Gateway configuration.
     *
     * @param port the port
     * @param maxNodes the maximum nodes
     * @param queueCapacity the queue capacity
     * @param workerThreads the worker threads
     */
    record GatewayConfig(
            int port,
            int maxNodes,
            int queueCapacity,
            int workerThreads
    ) {

        /**
         * Creates a builder for GatewayConfig.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for GatewayConfig.
         */
        public static class Builder {
            private int port = 8080;
            private int maxNodes = 100;
            private int queueCapacity = 10000;
            private int workerThreads = 10;

            public Builder port(int port) {
                this.port = port;
                return this;
            }

            public Builder maxNodes(int maxNodes) {
                this.maxNodes = maxNodes;
                return this;
            }

            public Builder queueCapacity(int queueCapacity) {
                this.queueCapacity = queueCapacity;
                return this;
            }

            public Builder workerThreads(int workerThreads) {
                this.workerThreads = workerThreads;
                return this;
            }

            public GatewayConfig build() {
                return new GatewayConfig(port, maxNodes, queueCapacity, workerThreads);
            }
        }
    }

    /**
     * Work item.
     *
     * @param id the work ID
     * @param type the work type
     * @param payload the payload
     * @param priority the priority
     * @param targetNode the target node if any
     */
    record WorkItem(
            String id,
            WorkType type,
            byte[] payload,
            int priority,
            String targetNode
    ) {

        /**
         * Creates a work item with auto-generated ID.
         *
         * @param type the type
         * @param payload the payload
         * @return the work item
         */
        public static WorkItem of(WorkType type, byte[] payload) {
            return new WorkItem(
                    java.util.UUID.randomUUID().toString(),
                    type,
                    payload,
                    5,
                    null
            );
        }
    }

    /**
     * Work type.
     */
    enum WorkType {
        AGENT_RUN,
        AGENT_SPAWN,
        CHANNEL_MESSAGE,
        TOOL_EXECUTION,
        MEMORY_UPDATE,
        SYSTEM_MAINTENANCE
    }

    /**
     * Work status.
     *
     * @param workId the work ID
     * @param state the state
     * @param progress the progress (0-100)
     * @param result the result if complete
     * @param error the error if failed
     */
    record WorkStatus(
            String workId,
            WorkState state,
            int progress,
            byte[] result,
            String error
    ) {

        /**
         * Work state.
         */
        public enum WorkState {
            PENDING,
            RUNNING,
            COMPLETED,
            FAILED,
            CANCELLED
        }

        /**
         * Creates a pending status.
         *
         * @param workId the work ID
         * @return the status
         */
        public static WorkStatus pending(String workId) {
            return new WorkStatus(workId, WorkState.PENDING, 0, null, null);
        }

        /**
         * Creates a running status.
         *
         * @param workId the work ID
         * @param progress the progress
         * @return the status
         */
        public static WorkStatus running(String workId, int progress) {
            return new WorkStatus(workId, WorkState.RUNNING, progress, null, null);
        }

        /**
         * Creates a completed status.
         *
         * @param workId the work ID
         * @param result the result
         * @return the status
         */
        public static WorkStatus completed(String workId, byte[] result) {
            return new WorkStatus(workId, WorkState.COMPLETED, 100, result, null);
        }

        /**
         * Creates a failed status.
         *
         * @param workId the work ID
         * @param error the error
         * @return the status
         */
        public static WorkStatus failed(String workId, String error) {
            return new WorkStatus(workId, WorkState.FAILED, 0, null, error);
        }
    }
}
