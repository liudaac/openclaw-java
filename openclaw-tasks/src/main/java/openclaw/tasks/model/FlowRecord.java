package openclaw.tasks.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Flow record representing a ClawFlow instance.
 * Mirrors the TypeScript type: FlowRecord from flow-registry.types.ts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowRecord {
    private String flowId;
    private FlowShape shape;
    private String ownerSessionKey;
    private DeliveryContext requesterOrigin;
    private FlowStatus status;
    private TaskNotifyPolicy notifyPolicy;
    private String goal;
    private String currentStep;
    private String waitingOnTaskId;
    private Map<String, Object> outputs;
    private String blockedTaskId;
    private String blockedSummary;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant endedAt;

    /**
     * Check if the flow is in a terminal state.
     */
    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

    /**
     * Check if the flow is currently active.
     */
    public boolean isActive() {
        return status != null && status.isActive();
    }
}
