package openclaw.tasks.executor;

import openclaw.tasks.model.*;
import openclaw.tasks.registry.TaskRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Task executor for creating and managing task runs.
 * Mirrors the functionality of task-executor.ts
 */
@Service
public class TaskExecutor {

    private final TaskRegistry taskRegistry;

    public TaskExecutor(TaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
    }

    /**
     * Create a queued task run.
     */
    public TaskRecord createQueuedTaskRun(CreateQueuedTaskRunParams params) {
        return taskRegistry.createTaskRecord(new TaskRegistry.CreateTaskRequest(
            params.runtime(),
            params.sourceId(),
            params.requesterSessionKey(),
            params.parentFlowId(),
            params.childSessionKey(),
            params.parentTaskId(),
            params.agentId(),
            params.runId(),
            params.label(),
            params.task(),
            TaskStatus.QUEUED,
            params.deliveryStatus() != null ? params.deliveryStatus() : TaskDeliveryStatus.PENDING,
            params.notifyPolicy()
        ));
    }

    /**
     * Create a running task run.
     */
    public TaskRecord createRunningTaskRun(CreateRunningTaskRunParams params) {
        Instant now = Instant.now();
        TaskRecord task = taskRegistry.createTaskRecord(new TaskRegistry.CreateTaskRequest(
            params.runtime(),
            params.sourceId(),
            params.requesterSessionKey(),
            params.parentFlowId(),
            params.childSessionKey(),
            params.parentTaskId(),
            params.agentId(),
            params.runId(),
            params.label(),
            params.task(),
            TaskStatus.RUNNING,
            params.deliveryStatus() != null ? params.deliveryStatus() : TaskDeliveryStatus.PENDING,
            params.notifyPolicy()
        ));

        // Update with started timestamp and progress summary
        return taskRegistry.updateTaskRecordById(task.getTaskId(), new TaskRegistry.UpdateTaskRequest(
            null,
            null,
            params.progressSummary(),
            null,
            null,
            null,
            now,
            null,
            null,
            null
        ));
    }

    /**
     * Mark a task as started.
     */
    public TaskRecord startTask(String taskId) {
        return taskRegistry.updateTaskRecordById(taskId, new TaskRegistry.UpdateTaskRequest(
            TaskStatus.RUNNING,
            null,
            null,
            null,
            null,
            null,
            Instant.now(),
            null,
            Instant.now(),
            Instant.now()
        ));
    }

    /**
     * Mark a task as succeeded.
     */
    public TaskRecord succeedTask(String taskId, String terminalSummary) {
        Instant now = Instant.now();
        return taskRegistry.updateTaskRecordById(taskId, new TaskRegistry.UpdateTaskRequest(
            TaskStatus.SUCCEEDED,
            TaskDeliveryStatus.DELIVERED,
            null,
            terminalSummary,
            "succeeded",
            null,
            null,
            now,
            now,
            now
        ));
    }

    /**
     * Mark a task as failed.
     */
    public TaskRecord failTask(String taskId, String error) {
        Instant now = Instant.now();
        return taskRegistry.updateTaskRecordById(taskId, new TaskRegistry.UpdateTaskRequest(
            TaskStatus.FAILED,
            TaskDeliveryStatus.FAILED,
            null,
            null,
            "failed",
            error,
            null,
            now,
            now,
            now
        ));
    }

    /**
     * Update task progress.
     */
    public TaskRecord updateProgress(String taskId, String progressSummary) {
        return taskRegistry.updateTaskRecordById(taskId, new TaskRegistry.UpdateTaskRequest(
            null,
            null,
            progressSummary,
            null,
            null,
            null,
            null,
            null,
            Instant.now(),
            Instant.now()
        ));
    }

    // Record types for parameters

    public record CreateQueuedTaskRunParams(
        TaskRuntime runtime,
        String sourceId,
        String requesterSessionKey,
        String requesterOrigin,
        String parentFlowId,
        String childSessionKey,
        String parentTaskId,
        String agentId,
        String runId,
        String label,
        String task,
        Boolean preferMetadata,
        TaskNotifyPolicy notifyPolicy,
        TaskDeliveryStatus deliveryStatus
    ) {}

    public record CreateRunningTaskRunParams(
        TaskRuntime runtime,
        String sourceId,
        String requesterSessionKey,
        String requesterOrigin,
        String parentFlowId,
        String childSessionKey,
        String parentTaskId,
        String agentId,
        String runId,
        String label,
        String task,
        Boolean preferMetadata,
        TaskNotifyPolicy notifyPolicy,
        TaskDeliveryStatus deliveryStatus,
        Instant startedAt,
        Instant lastEventAt,
        String progressSummary
    ) {}
}
