package openclaw.tasks.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Task record representing a task within a flow.
 * Mirrors the TypeScript type: TaskRecord from task-registry.types.ts
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskRecord {
    private String taskId;
    private TaskRuntime runtime;
    private String sourceId;
    private String requesterSessionKey;
    private String parentFlowId;
    private String childSessionKey;
    private String parentTaskId;
    private String agentId;
    private String runId;
    private String label;
    private String task;
    private TaskStatus status;
    private TaskDeliveryStatus deliveryStatus;
    private TaskNotifyPolicy notifyPolicy;
    private Instant createdAt;
    private Instant startedAt;
    private Instant endedAt;
    private Instant lastEventAt;
    private Instant cleanupAfter;
    private String error;
    private String progressSummary;
    private String terminalSummary;
    private String terminalOutcome;

    /**
     * Check if the task is in a terminal state.
     */
    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }
}
