package openclaw.tasks.registry;

import openclaw.tasks.model.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of TaskRegistry for testing.
 */
@Repository
public class InMemoryTaskRegistry implements TaskRegistry {

    private final Map<String, TaskRecord> tasks = new ConcurrentHashMap<>();

    @Override
    public TaskRecord createTaskRecord(CreateTaskRequest request) {
        String taskId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        
        TaskRecord task = TaskRecord.builder()
            .taskId(taskId)
            .runtime(request.runtime())
            .sourceId(request.sourceId())
            .requesterSessionKey(request.requesterSessionKey())
            .parentFlowId(request.parentFlowId())
            .childSessionKey(request.childSessionKey())
            .parentTaskId(request.parentTaskId())
            .agentId(request.agentId())
            .runId(request.runId())
            .label(request.label())
            .task(request.task())
            .status(request.status())
            .deliveryStatus(request.deliveryStatus())
            .notifyPolicy(request.notifyPolicy())
            .createdAt(now)
            .build();
        
        tasks.put(taskId, task);
        return task;
    }

    @Override
    public Optional<TaskRecord> getTaskById(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    @Override
    public TaskRecord updateTaskRecordById(String taskId, UpdateTaskRequest request) {
        TaskRecord existing = tasks.get(taskId);
        if (existing == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        TaskRecord.TaskRecordBuilder builder = existing.toBuilder();
        
        if (request.status() != null) {
            builder.status(request.status());
        }
        if (request.deliveryStatus() != null) {
            builder.deliveryStatus(request.deliveryStatus());
        }
        if (request.progressSummary() != null) {
            builder.progressSummary(request.progressSummary());
        }
        if (request.terminalSummary() != null) {
            builder.terminalSummary(request.terminalSummary());
        }
        if (request.terminalOutcome() != null) {
            builder.terminalOutcome(request.terminalOutcome());
        }
        if (request.error() != null) {
            builder.error(request.error());
        }
        if (request.startedAt() != null) {
            builder.startedAt(request.startedAt());
        }
        if (request.endedAt() != null) {
            builder.endedAt(request.endedAt());
        }
        if (request.lastEventAt() != null) {
            builder.lastEventAt(request.lastEventAt());
        }
        if (request.updatedAt() != null) {
            // updatedAt is not in builder, skip for now
        }
        
        TaskRecord updated = builder.build();
        tasks.put(taskId, updated);
        return updated;
    }

    @Override
    public List<TaskRecord> listTasksForFlowId(String flowId) {
        return tasks.values().stream()
            .filter(t -> flowId.equals(t.getParentFlowId()))
            .toList();
    }

    @Override
    public List<TaskRecord> listTasksByOwner(String ownerSessionKey) {
        return tasks.values().stream()
            .filter(t -> ownerSessionKey.equals(t.getRequesterSessionKey()))
            .toList();
    }

    @Override
    public boolean deleteTaskById(String taskId) {
        return tasks.remove(taskId) != null;
    }
}
