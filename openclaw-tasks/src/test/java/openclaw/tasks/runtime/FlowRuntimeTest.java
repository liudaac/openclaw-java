package openclaw.tasks.runtime;

import openclaw.tasks.delivery.FlowDeliveryService;
import openclaw.tasks.model.*;
import openclaw.tasks.registry.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FlowRuntime.
 */
class FlowRuntimeTest {

    @TempDir
    Path tempDir;

    private FlowRegistry flowRegistry;
    private TaskRegistry taskRegistry;
    private FlowDeliveryService deliveryService;
    private FlowRuntime flowRuntime;

    @BeforeEach
    void setUp() throws Exception {
        String dbPath = tempDir.resolve("test.db").toString();
        flowRegistry = new SqliteFlowRegistry(dbPath);
        taskRegistry = new SqliteTaskRegistry(dbPath);
        deliveryService = new FlowDeliveryService();
        flowRuntime = new FlowRuntime(flowRegistry, taskRegistry, deliveryService);
    }

    @Test
    void testCreateFlow() {
        // Given
        FlowRuntime.CreateFlowParams params = new FlowRuntime.CreateFlowParams(
            "session-key-123",
            null,
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            "initial_step",
            null,
            null
        );

        // When
        FlowRecord flow = flowRuntime.createFlow(params);

        // Then
        assertNotNull(flow);
        assertNotNull(flow.getFlowId());
        assertEquals(FlowShape.LINEAR, flow.getShape());
        assertEquals("session-key-123", flow.getOwnerSessionKey());
        assertEquals("Test goal", flow.getGoal());
        assertEquals(FlowStatus.QUEUED, flow.getStatus());
        assertEquals(TaskNotifyPolicy.STATE_CHANGES, flow.getNotifyPolicy());
        assertNotNull(flow.getCreatedAt());
        assertNotNull(flow.getUpdatedAt());
    }

    @Test
    void testSetFlowOutput() {
        // Given
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "session-key-123",
            null,
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);

        // When
        FlowRuntime.SetFlowOutputParams outputParams = new FlowRuntime.SetFlowOutputParams(
            flow.getFlowId(),
            "classification",
            Map.of("route", "business"),
            null
        );
        FlowRecord updatedFlow = flowRuntime.setFlowOutput(outputParams);

        // Then
        assertNotNull(updatedFlow.getOutputs());
        assertTrue(updatedFlow.getOutputs().containsKey("classification"));
        @SuppressWarnings("unchecked")
        Map<String, Object> classification = (Map<String, Object>) updatedFlow.getOutputs().get("classification");
        assertEquals("business", classification.get("route"));
    }

    @Test
    void testAppendFlowOutput() {
        // Given
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "session-key-123",
            null,
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);

        // When - append first value
        FlowRuntime.AppendFlowOutputParams appendParams1 = new FlowRuntime.AppendFlowOutputParams(
            flow.getFlowId(),
            "results",
            "result1",
            null
        );
        flowRuntime.appendFlowOutput(appendParams1);

        // When - append second value
        FlowRuntime.AppendFlowOutputParams appendParams2 = new FlowRuntime.AppendFlowOutputParams(
            flow.getFlowId(),
            "results",
            "result2",
            null
        );
        FlowRecord updatedFlow = flowRuntime.appendFlowOutput(appendParams2);

        // Then
        assertNotNull(updatedFlow.getOutputs());
        assertTrue(updatedFlow.getOutputs().containsKey("results"));
        @SuppressWarnings("unchecked")
        java.util.List<Object> results = (java.util.List<Object>) updatedFlow.getOutputs().get("results");
        assertEquals(2, results.size());
        assertEquals("result1", results.get(0));
        assertEquals("result2", results.get(1));
    }

    @Test
    void testResumeFlow() {
        // Given
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "session-key-123",
            null,
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);

        // When
        FlowRuntime.ResumeFlowParams resumeParams = new FlowRuntime.ResumeFlowParams(
            flow.getFlowId(),
            "resumed_step",
            null
        );
        FlowRecord updatedFlow = flowRuntime.resumeFlow(resumeParams);

        // Then
        assertEquals(FlowStatus.RUNNING, updatedFlow.getStatus());
        assertEquals("resumed_step", updatedFlow.getCurrentStep());
        assertNull(updatedFlow.getWaitingOnTaskId());
    }

    @Test
    void testFinishFlow() {
        // Given
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "session-key-123",
            null,
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);

        // When
        FlowRuntime.FinishFlowParams finishParams = new FlowRuntime.FinishFlowParams(
            flow.getFlowId(),
            "completed_step",
            null,
            null
        );
        FlowRecord updatedFlow = flowRuntime.finishFlow(finishParams);

        // Then
        assertEquals(FlowStatus.SUCCEEDED, updatedFlow.getStatus());
        assertEquals("completed_step", updatedFlow.getCurrentStep());
        assertNotNull(updatedFlow.getEndedAt());
        assertTrue(updatedFlow.isTerminal());
    }

    @Test
    void testFailFlow() {
        // Given
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "session-key-123",
            null,
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);

        // When
        FlowRuntime.FailFlowParams failParams = new FlowRuntime.FailFlowParams(
            flow.getFlowId(),
            "failed_step",
            null,
            null
        );
        FlowRecord updatedFlow = flowRuntime.failFlow(failParams);

        // Then
        assertEquals(FlowStatus.FAILED, updatedFlow.getStatus());
        assertEquals("failed_step", updatedFlow.getCurrentStep());
        assertNotNull(updatedFlow.getEndedAt());
        assertTrue(updatedFlow.isTerminal());
    }

    @Test
    void testSetFlowOutputWithEmptyKeyThrowsException() {
        // Given
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "session-key-123",
            null,
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);

        // When/Then
        FlowRuntime.SetFlowOutputParams outputParams = new FlowRuntime.SetFlowOutputParams(
            flow.getFlowId(),
            "   ",  // empty/whitespace key
            "value",
            null
        );
        assertThrows(IllegalArgumentException.class, () -> flowRuntime.setFlowOutput(outputParams));
    }

    @Test
    void testRequireFlowThrowsExceptionForNonExistentFlow() {
        // When/Then
        FlowRuntime.ResumeFlowParams resumeParams = new FlowRuntime.ResumeFlowParams(
            "non-existent-flow-id",
            null,
            null
        );
        assertThrows(IllegalArgumentException.class, () -> flowRuntime.resumeFlow(resumeParams));
    }

    @Test
    void testListFlowsByOwner() {
        // Given
        String ownerKey = "owner-123";
        
        FlowRuntime.CreateFlowParams params1 = new FlowRuntime.CreateFlowParams(
            ownerKey, null, "Goal 1", TaskNotifyPolicy.STATE_CHANGES, null, null, null
        );
        FlowRuntime.CreateFlowParams params2 = new FlowRuntime.CreateFlowParams(
            ownerKey, null, "Goal 2", TaskNotifyPolicy.STATE_CHANGES, null, null, null
        );
        FlowRuntime.CreateFlowParams params3 = new FlowRuntime.CreateFlowParams(
            "other-owner", null, "Goal 3", TaskNotifyPolicy.STATE_CHANGES, null, null, null
        );
        
        flowRuntime.createFlow(params1);
        flowRuntime.createFlow(params2);
        flowRuntime.createFlow(params3);

        // When
        var flows = flowRegistry.listFlowsByOwner(ownerKey);

        // Then
        assertEquals(2, flows.size());
        assertTrue(flows.stream().allMatch(f -> ownerKey.equals(f.getOwnerSessionKey())));
    }

    @Test
    void testDeleteFlow() {
        // Given
        FlowRuntime.CreateFlowParams params = new FlowRuntime.CreateFlowParams(
            "owner-key", null, "Goal", TaskNotifyPolicy.STATE_CHANGES, null, null, null
        );
        FlowRecord flow = flowRuntime.createFlow(params);

        // When
        boolean deleted = flowRegistry.deleteFlowById(flow.getFlowId());

        // Then
        assertTrue(deleted);
        assertTrue(flowRegistry.getFlowById(flow.getFlowId()).isEmpty());
    }

    @Test
    void testDeleteNonExistentFlowReturnsFalse() {
        // When
        boolean deleted = flowRegistry.deleteFlowById("non-existent-id");

        // Then
        assertFalse(deleted);
    }

    @Test
    void testEmitFlowUpdateWithEmptyOwner() {
        // Given - create a flow without owner
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "",  // empty owner
            null,
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);

        // When
        FlowRuntime.EmitFlowUpdateParams emitParams = new FlowRuntime.EmitFlowUpdateParams(
            flow.getFlowId(),
            "Update content",
            null,
            null,
            null
        );
        FlowRuntime.FlowUpdateResult result = flowRuntime.emitFlowUpdate(emitParams);

        // Then
        assertEquals(FlowRuntime.FlowUpdateDelivery.PARENT_MISSING, result.delivery());
    }

    @Test
    void testEmitFlowUpdateWithContent() {
        // Given
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "owner-session",
            DeliveryContext.builder().channel("discord").to("user123").build(),
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);

        // When
        FlowRuntime.EmitFlowUpdateParams emitParams = new FlowRuntime.EmitFlowUpdateParams(
            flow.getFlowId(),
            "Flow update message",
            "event-1",
            "updated_step",
            null
        );
        FlowRuntime.FlowUpdateResult result = flowRuntime.emitFlowUpdate(emitParams);

        // Then - should be SESSION_QUEUED (direct delivery is mocked to succeed but falls through)
        assertNotNull(result.flow());
        assertEquals("updated_step", result.flow().getCurrentStep());
    }

    @Test
    void testEmitFlowUpdateWithEmptyContentThrowsException() {
        // Given
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "owner-session",
            null,
            "Test goal",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);

        // When/Then
        FlowRuntime.EmitFlowUpdateParams emitParams = new FlowRuntime.EmitFlowUpdateParams(
            flow.getFlowId(),
            "   ",  // whitespace only
            null,
            null,
            null
        );
        assertThrows(IllegalArgumentException.class, () -> flowRuntime.emitFlowUpdate(emitParams));
    }
}