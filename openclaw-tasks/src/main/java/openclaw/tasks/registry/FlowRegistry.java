package openclaw.tasks.registry;

import openclaw.tasks.model.FlowRecord;
import openclaw.tasks.model.FlowShape;
import openclaw.tasks.model.FlowStatus;
import openclaw.tasks.model.TaskNotifyPolicy;

import java.util.List;
import java.util.Optional;

/**
 * Flow registry interface for CRUD operations on flows.
 * Mirrors the functionality of flow-registry.ts
 */
public interface FlowRegistry {

    /**
     * Create a new flow record.
     */
    FlowRecord createFlowRecord(CreateFlowRequest request);

    /**
     * Get a flow by its ID.
     */
    Optional<FlowRecord> getFlowById(String flowId);

    /**
     * Update a flow by its ID.
     */
    FlowRecord updateFlowRecordById(String flowId, UpdateFlowRequest request);

    /**
     * List all flows for a given owner session key.
     */
    List<FlowRecord> listFlowsByOwner(String ownerSessionKey);

    /**
     * List all flows with a specific status.
     */
    List<FlowRecord> listFlowsByStatus(FlowStatus status);

    /**
     * Delete a flow by its ID.
     */
    boolean deleteFlowById(String flowId);

    /**
     * Request object for creating a flow.
     */
    record CreateFlowRequest(
        FlowShape shape,
        String ownerSessionKey,
        String goal,
        TaskNotifyPolicy notifyPolicy,
        String currentStep,
        FlowStatus status
    ) {}

    /**
     * Request object for updating a flow.
     */
    record UpdateFlowRequest(
        FlowStatus status,
        String currentStep,
        String waitingOnTaskId,
        String blockedTaskId,
        String blockedSummary,
        java.util.Map<String, Object> outputs,
        java.time.Instant endedAt,
        java.time.Instant updatedAt
    ) {}
}
