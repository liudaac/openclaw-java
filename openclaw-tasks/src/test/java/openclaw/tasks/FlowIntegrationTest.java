package openclaw.tasks;

import openclaw.tasks.delivery.FlowDeliveryService;
import openclaw.tasks.model.*;
import openclaw.tasks.registry.*;
import openclaw.tasks.runtime.FlowRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete flow lifecycle.
 */
class FlowIntegrationTest {

    @TempDir
    Path tempDir;

    private FlowRegistry flowRegistry;
    private TaskRegistry taskRegistry;
    private FlowDeliveryService deliveryService;
    private FlowRuntime flowRuntime;

    @BeforeEach
    void setUp() throws Exception {
        String dbPath = tempDir.resolve("integration.db").toString();
        flowRegistry = new SqliteFlowRegistry(dbPath);
        taskRegistry = new SqliteTaskRegistry(dbPath);
        deliveryService = new FlowDeliveryService();
        flowRuntime = new FlowRuntime(flowRegistry, taskRegistry, deliveryService);
    }

    @Test
    void testCompleteFlowLifecycle() {
        // Step 1: Create a flow
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "owner-session-123",
            DeliveryContext.builder().channel("discord").to("user123").build(),
            "Process customer support ticket",
            TaskNotifyPolicy.STATE_CHANGES,
            "created",
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);
        
        assertNotNull(flow.getFlowId());
        assertEquals(FlowStatus.QUEUED, flow.getStatus());
        assertEquals("Process customer support ticket", flow.getGoal());
        
        // Step 2: Start the flow by running first task
        FlowRuntime.RunTaskParams task1Params = new FlowRuntime.RunTaskParams(
            flow.getFlowId(),
            TaskRuntime.ACP,
            null,
            null,
            null,
            "agent-classifier",
            null,
            "ticket-classification",
            "Classify the support ticket",
            false,
            TaskNotifyPolicy.STATE_CHANGES,
            TaskDeliveryStatus.PENDING,
            "running",
            null,
            null,
            "Analyzing ticket content...",
            "classifying"
        );
        FlowRuntime.TaskInFlowResult task1Result = flowRuntime.runTaskInFlow(task1Params);
        
        assertEquals(FlowStatus.WAITING, task1Result.flow().getStatus());
        assertEquals(TaskStatus.RUNNING, task1Result.task().getStatus());
        assertEquals("classifying", task1Result.flow().getCurrentStep());
        
        // Step 3: Set flow output from task 1
        FlowRuntime.SetFlowOutputParams outputParams = new FlowRuntime.SetFlowOutputParams(
            flow.getFlowId(),
            "classification",
            Map.of("priority", "high", "category", "bug"),
            null
        );
        FlowRecord flowWithOutput = flowRuntime.setFlowOutput(outputParams);
        
        assertNotNull(flowWithOutput.getOutputs());
        assertTrue(flowWithOutput.getOutputs().containsKey("classification"));
        @SuppressWarnings("unchecked")
        Map<String, Object> classification = (Map<String, Object>) flowWithOutput.getOutputs().get("classification");
        assertEquals("high", classification.get("priority"));
        assertEquals("bug", classification.get("category"));
        
        // Step 4: Resume flow and run second task
        FlowRuntime.ResumeFlowParams resumeParams = new FlowRuntime.ResumeFlowParams(
            flow.getFlowId(),
            "routing",
            null
        );
        FlowRecord resumedFlow = flowRuntime.resumeFlow(resumeParams);
        assertEquals(FlowStatus.RUNNING, resumedFlow.getStatus());
        
        // Step 5: Run second task
        FlowRuntime.RunTaskParams task2Params = new FlowRuntime.RunTaskParams(
            flow.getFlowId(),
            TaskRuntime.SUBAGENT,
            null,
            null,
            null,
            "agent-router",
            null,
            "ticket-routing",
            "Route ticket to appropriate team",
            false,
            TaskNotifyPolicy.STATE_CHANGES,
            TaskDeliveryStatus.PENDING,
            "queued",
            null,
            null,
            null,
            "routing"
        );
        FlowRuntime.TaskInFlowResult task2Result = flowRuntime.runTaskInFlow(task2Params);
        
        assertEquals(FlowStatus.WAITING, task2Result.flow().getStatus());
        assertEquals(TaskStatus.QUEUED, task2Result.task().getStatus());
        
        // Step 6: Emit flow update
        FlowRuntime.EmitFlowUpdateParams emitParams = new FlowRuntime.EmitFlowUpdateParams(
            flow.getFlowId(),
            "Ticket classified as high priority bug, routing to engineering team",
            "routing-update",
            "updating",
            null
        );
        FlowRuntime.FlowUpdateResult updateResult = flowRuntime.emitFlowUpdate(emitParams);
        
        assertNotNull(updateResult.flow());
        assertEquals("updating", updateResult.flow().getCurrentStep());
        
        // Step 7: Append to results array
        FlowRuntime.AppendFlowOutputParams appendParams1 = new FlowRuntime.AppendFlowOutputParams(
            flow.getFlowId(),
            "actions",
            "classified",
            null
        );
        flowRuntime.appendFlowOutput(appendParams1);
        
        FlowRuntime.AppendFlowOutputParams appendParams2 = new FlowRuntime.AppendFlowOutputParams(
            flow.getFlowId(),
            "actions",
            "routed",
            null
        );
        FlowRecord flowWithActions = flowRuntime.appendFlowOutput(appendParams2);
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> actions = (java.util.List<Object>) flowWithActions.getOutputs().get("actions");
        assertEquals(2, actions.size());
        assertEquals("classified", actions.get(0));
        assertEquals("routed", actions.get(1));
        
        // Step 8: Complete the flow
        FlowRuntime.FinishFlowParams finishParams = new FlowRuntime.FinishFlowParams(
            flow.getFlowId(),
            "completed",
            null,
            null
        );
        FlowRecord completedFlow = flowRuntime.finishFlow(finishParams);
        
        assertEquals(FlowStatus.SUCCEEDED, completedFlow.getStatus());
        assertEquals("completed", completedFlow.getCurrentStep());
        assertNotNull(completedFlow.getEndedAt());
        assertTrue(completedFlow.isTerminal());
        
        // Verify we can retrieve the flow and its tasks
        FlowRecord retrievedFlow = flowRegistry.getFlowById(flow.getFlowId()).orElseThrow();
        assertEquals(FlowStatus.SUCCEEDED, retrievedFlow.getStatus());
        
        java.util.List<TaskRecord> tasks = taskRegistry.listTasksForFlowId(flow.getFlowId());
        assertEquals(2, tasks.size());
    }

    @Test
    void testFailedFlowLifecycle() {
        // Create and start a flow
        FlowRuntime.CreateFlowParams createParams = new FlowRuntime.CreateFlowParams(
            "owner-session",
            null,
            "Process data",
            TaskNotifyPolicy.STATE_CHANGES,
            null,
            null,
            null
        );
        FlowRecord flow = flowRuntime.createFlow(createParams);
        
        // Run a task
        FlowRuntime.RunTaskParams taskParams = new FlowRuntime.RunTaskParams(
            flow.getFlowId(),
            TaskRuntime.SUBAGENT,
            null,
            null,
            null,
            null,
            null,
            null,
            "processing",
            null,
            null,
            null,
            "queued",
            null,
            null,
            null,
            "processing"
        );
        flowRuntime.runTaskInFlow(taskParams);
        
        // Fail the flow
        FlowRuntime.FailFlowParams failParams = new FlowRuntime.FailFlowParams(
            flow.getFlowId(),
            "failed",
            null,
            null
        );
        FlowRecord failedFlow = flowRuntime.failFlow(failParams);
        
        assertEquals(FlowStatus.FAILED, failedFlow.getStatus());
        assertEquals("failed", failedFlow.getCurrentStep());
        assertNotNull(failedFlow.getEndedAt());
        assertTrue(failedFlow.isTerminal());
    }

    @Test
    void testMultipleFlowsForSameOwner() {
        String ownerSession = "owner-session-multi";
        
        // Create multiple flows
        for (int i = 0; i < 5; i++) {
            FlowRuntime.CreateFlowParams params = new FlowRuntime.CreateFlowParams(
                ownerSession,
                null,
                "Task " + i,
                TaskNotifyPolicy.STATE_CHANGES,
                null,
                null,
                null
            );
            flowRuntime.createFlow(params);
        }
        
        // List flows for owner
        java.util.List<FlowRecord> flows = flowRegistry.listFlowsByOwner(ownerSession);
        assertEquals(5, flows.size());
        
        // Complete some flows
        for (int i = 0; i < 3; i++) {
            FlowRuntime.FinishFlowParams params = new FlowRuntime.FinishFlowParams(
                flows.get(i).getFlowId(),
                null,
                null,
                null
            );
            flowRuntime.finishFlow(params);
        }
        
        // List succeeded flows
        java.util.List<FlowRecord> succeededFlows = flowRegistry.listFlowsByStatus(FlowStatus.SUCCEEDED);
        assertEquals(3, succeededFlows.size());
        
        // List queued flows
        java.util.List<FlowRecord> queuedFlows = flowRegistry.listFlowsByStatus(FlowStatus.QUEUED);
        assertEquals(2, queuedFlows.size());
    }
}
