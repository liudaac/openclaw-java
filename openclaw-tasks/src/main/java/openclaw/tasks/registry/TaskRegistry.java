package openclaw.tasks.registry;

import openclaw.tasks.model.*;

import java.util.List;
import java.util.Optional;

/**
 * Task registry interface for CRUD operations on tasks.
 * Mirrors the functionality of task-registry.ts
 */
public interface TaskRegistry {

    /**
     * Create a new task record.
     */
    TaskRecord createTaskRecord(CreateTaskRequest request);

    /**
     * Get a task by its ID.
     */
    Optional<TaskRecord> getTaskById(String taskId);

    /**
     * Update a task by its ID.
     */
    TaskRecord updateTaskRecordById(String taskId, UpdateTaskRequest request);

    /**
     * List all tasks for a given flow ID.
     */
    List<TaskRecord> listTasksForFlowId(String flowId);

    /**
     * List all tasks for a given owner session key.
     */
    List<TaskRecord> listTasksByOwner(String ownerSessionKey);

    /**
     * Delete a task by its ID.
     */
    boolean deleteTaskById(String taskId);

    /**
     * Request object for creating a task.
     */
    record CreateTaskRequest(
        TaskRuntime runtime,
        String sourceId,
        String requesterSessionKey,
        String parentFlowId,
        String childSessionKey,
        String parentTaskId,
        String agentId,
        String runId,
        String label,
        String task,
        TaskStatus status,
        TaskDeliveryStatus deliveryStatus,
        TaskNotifyPolicy notifyPolicy
    ) {}

    /**
     * Request object for updating a task.
     */
    record UpdateTaskRequest(
        TaskStatus status,
        TaskDeliveryStatus deliveryStatus,
        String progressSummary,
        String terminalSummary,
        String terminalOutcome,
        String error,
        java.time.Instant startedAt,
        java.time.Instant endedAt,
        java.time.Instant lastEventAt,
        java.time.Instant updatedAt
    ) {}
}
