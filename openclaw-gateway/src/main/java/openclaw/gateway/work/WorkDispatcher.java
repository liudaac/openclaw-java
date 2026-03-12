package openclaw.gateway.work;

import openclaw.gateway.GatewayService.WorkItem;
import openclaw.gateway.node.NodeRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * Work dispatcher for distributing work to nodes.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface WorkDispatcher {

    /**
     * Dispatches work to an available node.
     *
     * @param work the work item
     * @return the dispatch result
     */
    CompletableFuture<DispatchResult> dispatch(WorkItem work);

    /**
     * Dispatches work to a specific node.
     *
     * @param work the work item
     * @param nodeId the target node ID
     * @return the dispatch result
     */
    CompletableFuture<DispatchResult> dispatchToNode(WorkItem work, String nodeId);

    /**
     * Gets the dispatch strategy.
     *
     * @return the strategy
     */
    DispatchStrategy getStrategy();

    /**
     * Sets the dispatch strategy.
     *
     * @param strategy the strategy
     */
    void setStrategy(DispatchStrategy strategy);

    /**
     * Dispatch result.
     *
     * @param success whether dispatch succeeded
     * @param workId the work ID
     * @param nodeId the node ID if dispatched
     * @param message the message
     */
    record DispatchResult(
            boolean success,
            String workId,
            String nodeId,
            String message
    ) {

        /**
         * Creates a successful result.
         *
         * @param workId the work ID
         * @param nodeId the node ID
         * @return the result
         */
        public static DispatchResult success(String workId, String nodeId) {
            return new DispatchResult(true, workId, nodeId, "Dispatched successfully");
        }

        /**
         * Creates a failed result.
         *
         * @param workId the work ID
         * @param message the error message
         * @return the result
         */
        public static DispatchResult failure(String workId, String message) {
            return new DispatchResult(false, workId, null, message);
        }
    }

    /**
     * Dispatch strategy.
     */
    enum DispatchStrategy {
        ROUND_ROBIN,
        LEAST_LOADED,
        RANDOM,
        AFFINITY,
        PRIORITY
    }
}
