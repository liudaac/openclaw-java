package openclaw.tasks.runtime;

import openclaw.tasks.delivery.FlowDeliveryService;
import openclaw.tasks.model.*;
import openclaw.tasks.registry.FlowRegistry;
import openclaw.tasks.registry.TaskRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Flow runtime service implementing the ClawFlow runtime logic.
 * Mirrors the functionality of flow-runtime.ts
 */
@Service
public class FlowRuntime {

    private final FlowRegistry flowRegistry;
    private final TaskRegistry taskRegistry;
    private final FlowDeliveryService deliveryService;

    public FlowRuntime(FlowRegistry flowRegistry, TaskRegistry taskRegistry, FlowDeliveryService deliveryService) {
        this.flowRegistry = flowRegistry;
        this.taskRegistry = taskRegistry;
        this.deliveryService = deliveryService;
    }

    /**
     * Create a new flow.
     */
    public FlowRecord createFlow(CreateFlowParams params) {
        return flowRegistry.createFlowRecord(new FlowRegistry.CreateFlowRequest(
            FlowShape.LINEAR,
            params.ownerSessionKey(),
            params.goal(),
            params.notifyPolicy() != null ? params.notifyPolicy() : TaskNotifyPolicy.STATE_CHANGES,
            params.currentStep(),
            FlowStatus.QUEUED
        ));
    }

    /**
     * Run a task within a flow.
     */
    public TaskInFlowResult runTaskInFlow(RunTaskParams params) {
        FlowRecord flow = requireLinearFlow(params.flowId());
        TaskStatus initialStatus = "running".equals(params.launch()) ? TaskStatus.RUNNING : TaskStatus.QUEUED;

        TaskRecord task = taskRegistry.createTaskRecord(new TaskRegistry.CreateTaskRequest(
            params.runtime(),
            params.sourceId(),
            flow.getOwnerSessionKey(),
            flow.getFlowId(),
            params.childSessionKey(),
            params.parentTaskId(),
            params.agentId(),
            params.runId(),
            params.label(),
            params.task(),
            initialStatus,
            params.deliveryStatus() != null ? params.deliveryStatus() : TaskDeliveryStatus.PENDING,
            params.notifyPolicy() != null ? params.notifyPolicy() : flow.getNotifyPolicy()
        ));

        Instant updatedAt = task.getLastEventAt() != null ? task.getLastEventAt() :
                           (task.getStartedAt() != null ? task.getStartedAt() : Instant.now());

        FlowRecord updatedFlow = flowRegistry.updateFlowRecordById(flow.getFlowId(), new FlowRegistry.UpdateFlowRequest(
            FlowStatus.WAITING,
            params.currentStep() != null ? params.currentStep() : 
                (flow.getCurrentStep() != null ? flow.getCurrentStep() : "wait_for_task"),
            task.getTaskId(),
            null,
            null,
            null,
            null,
            updatedAt
        ));

        return new TaskInFlowResult(updatedFlow, task);
    }

    /**
     * Set flow to waiting state.
     */
    public FlowRecord setFlowWaiting(SetFlowWaitingParams params) {
        FlowRecord flow = requireLinearFlow(params.flowId());

        if (params.waitingOnTaskId() != null && !params.waitingOnTaskId().isBlank()) {
            String waitingOnTaskId = params.waitingOnTaskId().trim();
            Set<String> linkedTaskIds = new HashSet<>();
            taskRegistry.listTasksForFlowId(flow.getFlowId()).forEach(t -> linkedTaskIds.add(t.getTaskId()));
            
            if (!linkedTaskIds.contains(waitingOnTaskId)) {
                throw new IllegalArgumentException(
                    String.format("Flow %s is not linked to task %s", flow.getFlowId(), waitingOnTaskId)
                );
            }
        }

        return flowRegistry.updateFlowRecordById(flow.getFlowId(), new FlowRegistry.UpdateFlowRequest(
            FlowStatus.WAITING,
            params.currentStep(),
            params.waitingOnTaskId(),
            null,
            null,
            null,
            null,
            params.updatedAt() != null ? params.updatedAt() : Instant.now()
        ));
    }

    /**
     * Set a flow output value.
     */
    public FlowRecord setFlowOutput(SetFlowOutputParams params) {
        FlowRecord flow = requireLinearFlow(params.flowId());
        String key = params.key().trim();
        
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Flow output key is required.");
        }

        Map<String, Object> outputs = flow.getOutputs() != null ? 
            new HashMap<>(flow.getOutputs()) : new HashMap<>();
        outputs.put(key, deepClone(params.value()));

        return flowRegistry.updateFlowRecordById(flow.getFlowId(), new FlowRegistry.UpdateFlowRequest(
            null,
            null,
            null,
            null,
            null,
            outputs,
            null,
            params.updatedAt() != null ? params.updatedAt() : Instant.now()
        ));
    }

    /**
     * Append a value to a flow output array.
     */
    @SuppressWarnings("unchecked")
    public FlowRecord appendFlowOutput(AppendFlowOutputParams params) {
        FlowRecord flow = requireLinearFlow(params.flowId());
        String key = params.key().trim();
        
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Flow output key is required.");
        }

        Map<String, Object> outputs = flow.getOutputs() != null ? 
            new HashMap<>(flow.getOutputs()) : new HashMap<>();
        
        Object nextValue = deepClone(params.value());
        Object current = outputs.get(key);
        
        if (current == null) {
            List<Object> list = new ArrayList<>();
            list.add(nextValue);
            outputs.put(key, list);
        } else if (current instanceof List) {
            List<Object> list = new ArrayList<>((List<Object>) current);
            list.add(nextValue);
            outputs.put(key, list);
        } else {
            throw new IllegalStateException(String.format("Flow output %s is not an array.", key));
        }

        return flowRegistry.updateFlowRecordById(flow.getFlowId(), new FlowRegistry.UpdateFlowRequest(
            null,
            null,
            null,
            null,
            null,
            outputs,
            null,
            params.updatedAt() != null ? params.updatedAt() : Instant.now()
        ));
    }

    /**
     * Resume a flow from waiting state.
     */
    public FlowRecord resumeFlow(ResumeFlowParams params) {
        FlowRecord flow = requireLinearFlow(params.flowId());
        
        return flowRegistry.updateFlowRecordById(flow.getFlowId(), new FlowRegistry.UpdateFlowRequest(
            FlowStatus.RUNNING,
            params.currentStep(),
            null,
            null,
            null,
            null,
            null,
            params.updatedAt() != null ? params.updatedAt() : Instant.now()
        ));
    }

    /**
     * Mark a flow as finished successfully.
     */
    public FlowRecord finishFlow(FinishFlowParams params) {
        FlowRecord flow = requireLinearFlow(params.flowId());
        Instant endedAt = params.endedAt() != null ? params.endedAt() : 
                         (params.updatedAt() != null ? params.updatedAt() : Instant.now());
        
        return flowRegistry.updateFlowRecordById(flow.getFlowId(), new FlowRegistry.UpdateFlowRequest(
            FlowStatus.SUCCEEDED,
            params.currentStep(),
            null,
            null,
            null,
            null,
            endedAt,
            params.updatedAt() != null ? params.updatedAt() : endedAt
        ));
    }

    /**
     * Mark a flow as failed.
     */
    public FlowRecord failFlow(FailFlowParams params) {
        FlowRecord flow = requireLinearFlow(params.flowId());
        Instant endedAt = params.endedAt() != null ? params.endedAt() : 
                         (params.updatedAt() != null ? params.updatedAt() : Instant.now());
        
        return flowRegistry.updateFlowRecordById(flow.getFlowId(), new FlowRegistry.UpdateFlowRequest(
            FlowStatus.FAILED,
            params.currentStep(),
            null,
            null,
            null,
            null,
            endedAt,
            params.updatedAt() != null ? params.updatedAt() : endedAt
        ));
    }

    /**
     * Emit a flow update notification.
     */
    public FlowUpdateResult emitFlowUpdate(EmitFlowUpdateParams params) {
        FlowRecord flow = requireFlow(params.flowId());
        String content = params.content().trim();
        
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Flow update content is required.");
        }

        String ownerSessionKey = flow.getOwnerSessionKey() != null ? flow.getOwnerSessionKey().trim() : "";
        Instant updatedAt = params.updatedAt() != null ? params.updatedAt() : Instant.now();
        
        FlowRecord updatedFlow = flowRegistry.updateFlowRecordById(flow.getFlowId(), new FlowRegistry.UpdateFlowRequest(
            null,
            params.currentStep(),
            null,
            null,
            null,
            null,
            null,
            updatedAt
        ));

        if (ownerSessionKey.isEmpty()) {
            return new FlowUpdateResult(updatedFlow, FlowUpdateDelivery.PARENT_MISSING);
        }

        // Try direct delivery if possible
        if (deliveryService.canDeliverToRequesterOrigin(updatedFlow)) {
            try {
                String requesterAgentId = deliveryService.parseAgentIdFromSessionKey(ownerSessionKey).orElse(null);
                String eventKey = params.eventKey() != null ? params.eventKey().trim() : String.valueOf(updatedAt.toEpochMilli());
                String idempotencyKey = String.format("flow:%s:update:%s", updatedFlow.getFlowId(), eventKey);
                
                DeliveryContext origin = updatedFlow.getRequesterOrigin();
                FlowDeliveryService.DeliveryResult result = deliveryService.sendMessage(
                    new FlowDeliveryService.SendMessageRequest(
                        origin != null ? origin.getChannel() : null,
                        origin != null ? origin.getTo() : "",
                        origin != null ? origin.getAccountId() : null,
                        origin != null ? origin.getThreadId() : null,
                        content,
                        requesterAgentId,
                        idempotencyKey,
                        new FlowDeliveryService.MirrorContext(ownerSessionKey, requesterAgentId, idempotencyKey)
                    )
                );
                
                if (result.success()) {
                    return new FlowUpdateResult(updatedFlow, FlowUpdateDelivery.DIRECT);
                }
            } catch (Exception e) {
                // Fall through to session queued delivery
            }
        }

        // Fall back to session queued delivery
        try {
            deliveryService.enqueueSystemEvent(content, new FlowDeliveryService.SystemEventContext(
                ownerSessionKey,
                "flow:" + updatedFlow.getFlowId(),
                updatedFlow.getRequesterOrigin()
            ));
            deliveryService.requestHeartbeatNow("clawflow-update", ownerSessionKey);
            return new FlowUpdateResult(updatedFlow, FlowUpdateDelivery.SESSION_QUEUED);
        } catch (Exception e) {
            return new FlowUpdateResult(updatedFlow, FlowUpdateDelivery.FAILED);
        }
    }

    // Helper methods

    private FlowRecord requireFlow(String flowId) {
        return flowRegistry.getFlowById(flowId)
            .orElseThrow(() -> new IllegalArgumentException("Flow not found: " + flowId));
    }

    private FlowRecord requireLinearFlow(String flowId) {
        FlowRecord flow = requireFlow(flowId);
        if (flow.getShape() != FlowShape.LINEAR) {
            throw new IllegalArgumentException("Flow is not linear: " + flowId);
        }
        return flow;
    }

    @SuppressWarnings("unchecked")
    private Object deepClone(Object value) {
        if (value == null) {
            return null;
        }
        // Simple JSON-based deep clone
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(mapper.writeValueAsString(value), Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone value", e);
        }
    }

    // Record types for parameters

    public record CreateFlowParams(
        String ownerSessionKey,
        DeliveryContext requesterOrigin,
        String goal,
        TaskNotifyPolicy notifyPolicy,
        String currentStep,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record RunTaskParams(
        String flowId,
        TaskRuntime runtime,
        String sourceId,
        String childSessionKey,
        String parentTaskId,
        String agentId,
        String runId,
        String label,
        String task,
        Boolean preferMetadata,
        TaskNotifyPolicy notifyPolicy,
        TaskDeliveryStatus deliveryStatus,
        String launch,
        Instant startedAt,
        Instant lastEventAt,
        String progressSummary,
        String currentStep
    ) {}

    public record SetFlowWaitingParams(
        String flowId,
        String currentStep,
        String waitingOnTaskId,
        Instant updatedAt
    ) {}

    public record SetFlowOutputParams(
        String flowId,
        String key,
        Object value,
        Instant updatedAt
    ) {}

    public record AppendFlowOutputParams(
        String flowId,
        String key,
        Object value,
        Instant updatedAt
    ) {}

    public record ResumeFlowParams(
        String flowId,
        String currentStep,
        Instant updatedAt
    ) {}

    public record FinishFlowParams(
        String flowId,
        String currentStep,
        Instant updatedAt,
        Instant endedAt
    ) {}

    public record FailFlowParams(
        String flowId,
        String currentStep,
        Instant updatedAt,
        Instant endedAt
    ) {}

    public record EmitFlowUpdateParams(
        String flowId,
        String content,
        String eventKey,
        String currentStep,
        Instant updatedAt
    ) {}

    public record TaskInFlowResult(FlowRecord flow, TaskRecord task) {}

    public record FlowUpdateResult(FlowRecord flow, FlowUpdateDelivery delivery) {}

    public enum FlowUpdateDelivery {
        DIRECT("direct"),
        SESSION_QUEUED("session_queued"),
        PARENT_MISSING("parent_missing"),
        FAILED("failed");

        private final String value;

        FlowUpdateDelivery(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}