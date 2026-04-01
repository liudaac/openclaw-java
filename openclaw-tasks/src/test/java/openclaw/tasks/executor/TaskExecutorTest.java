package openclaw.tasks.executor;

import openclaw.tasks.model.*;
import openclaw.tasks.registry.SqliteTaskRegistry;
import openclaw.tasks.registry.TaskRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TaskExecutor.
 */
class TaskExecutorTest {

    @TempDir
    Path tempDir;

    private TaskRegistry taskRegistry;
    private TaskExecutor taskExecutor;

    @BeforeEach
    void setUp() throws Exception {
        String dbPath = tempDir.resolve("test.db").toString();
        taskRegistry = new SqliteTaskRegistry(dbPath);
        taskExecutor = new TaskExecutor(taskRegistry);
    }

    @Test
    void testCreateQueuedTaskRun() {
        // Given
        TaskExecutor.CreateQueuedTaskRunParams params = new TaskExecutor.CreateQueuedTaskRunParams(
            TaskRuntime.SUBAGENT,
            "source-123",
            "requester-session",
            null,
            "flow-123",
            null,
            null,
            "agent-1",
            null,
            "test-label",
            "Test task description",
            false,
            TaskNotifyPolicy.STATE_CHANGES,
            TaskDeliveryStatus.PENDING
        );

        // When
        TaskRecord task = taskExecutor.createQueuedTaskRun(params);

        // Then
        assertNotNull(task);
        assertNotNull(task.getTaskId());
        assertEquals(TaskRuntime.SUBAGENT, task.getRuntime());
        assertEquals("requester-session", task.getRequesterSessionKey());
        assertEquals("flow-123", task.getParentFlowId());
        assertEquals("agent-1", task.getAgentId());
        assertEquals("Test task description", task.getTask());
        assertEquals(TaskStatus.QUEUED, task.getStatus());
        assertEquals(TaskDeliveryStatus.PENDING, task.getDeliveryStatus());
        assertEquals(TaskNotifyPolicy.STATE_CHANGES, task.getNotifyPolicy());
        assertNotNull(task.getCreatedAt());
    }

    @Test
    void testCreateRunningTaskRun() {
        // Given
        TaskExecutor.CreateRunningTaskRunParams params = new TaskExecutor.CreateRunningTaskRunParams(
            TaskRuntime.ACP,
            "source-456",
            "requester-session",
            null,
            "flow-456",
            null,
            null,
            "agent-2",
            "run-1",
            "running-label",
            "Running task",
            true,
            TaskNotifyPolicy.DONE_ONLY,
            TaskDeliveryStatus.PENDING,
            null,
            null,
            "Starting execution..."
        );

        // When
        TaskRecord task = taskExecutor.createRunningTaskRun(params);

        // Then
        assertNotNull(task);
        assertEquals(TaskStatus.RUNNING, task.getStatus());
        assertEquals("Starting execution...", task.getProgressSummary());
        assertNotNull(task.getStartedAt());
    }

    @Test
    void testStartTask() {
        // Given - create a queued task
        TaskExecutor.CreateQueuedTaskRunParams createParams = new TaskExecutor.CreateQueuedTaskRunParams(
            TaskRuntime.SUBAGENT,
            null,
            "session-1",
            null,
            "flow-1",
            null,
            null,
            null,
            null,
            null,
            "Task to start",
            false,
            TaskNotifyPolicy.STATE_CHANGES,
            TaskDeliveryStatus.PENDING
        );
        TaskRecord task = taskExecutor.createQueuedTaskRun(createParams);
        assertEquals(TaskStatus.QUEUED, task.getStatus());

        // When
        TaskRecord startedTask = taskExecutor.startTask(task.getTaskId());

        // Then
        assertEquals(TaskStatus.RUNNING, startedTask.getStatus());
        assertNotNull(startedTask.getStartedAt());
    }

    @Test
    void testSucceedTask() {
        // Given - create and start a task
        TaskExecutor.CreateRunningTaskRunParams createParams = new TaskExecutor.CreateRunningTaskRunParams(
            TaskRuntime.SUBAGENT,
            null,
            "session-1",
            null,
            "flow-1",
            null,
            null,
            null,
            null,
            null,
            "Task to complete",
            false,
            TaskNotifyPolicy.STATE_CHANGES,
            TaskDeliveryStatus.PENDING,
            null,
            null,
            null
        );
        TaskRecord task = taskExecutor.createRunningTaskRun(createParams);

        // When
        TaskRecord completedTask = taskExecutor.succeedTask(task.getTaskId(), "Task completed successfully");

        // Then
        assertEquals(TaskStatus.SUCCEEDED, completedTask.getStatus());
        assertEquals(TaskDeliveryStatus.DELIVERED, completedTask.getDeliveryStatus());
        assertEquals("Task completed successfully", completedTask.getTerminalSummary());
        assertEquals("succeeded", completedTask.getTerminalOutcome());
        assertNotNull(completedTask.getEndedAt());
        assertTrue(completedTask.isTerminal());
    }

    @Test
    void testFailTask() {
        // Given - create and start a task
        TaskExecutor.CreateRunningTaskRunParams createParams = new TaskExecutor.CreateRunningTaskRunParams(
            TaskRuntime.SUBAGENT,
            null,
            "session-1",
            null,
            "flow-1",
            null,
            null,
            null,
            null,
            null,
            "Task to fail",
            false,
            TaskNotifyPolicy.STATE_CHANGES,
            TaskDeliveryStatus.PENDING,
            null,
            null,
            null
        );
        TaskRecord task = taskExecutor.createRunningTaskRun(createParams);

        // When
        TaskRecord failedTask = taskExecutor.failTask(task.getTaskId(), "Connection timeout");

        // Then
        assertEquals(TaskStatus.FAILED, failedTask.getStatus());
        assertEquals(TaskDeliveryStatus.FAILED, failedTask.getDeliveryStatus());
        assertEquals("Connection timeout", failedTask.getError());
        assertEquals("failed", failedTask.getTerminalOutcome());
        assertNotNull(failedTask.getEndedAt());
        assertTrue(failedTask.isTerminal());
    }

    @Test
    void testUpdateProgress() {
        // Given - create a running task
        TaskExecutor.CreateRunningTaskRunParams createParams = new TaskExecutor.CreateRunningTaskRunParams(
            TaskRuntime.SUBAGENT,
            null,
            "session-1",
            null,
            "flow-1",
            null,
            null,
            null,
            null,
            null,
            "Long running task",
            false,
            TaskNotifyPolicy.STATE_CHANGES,
            TaskDeliveryStatus.PENDING,
            null,
            null,
            "0% complete"
        );
        TaskRecord task = taskExecutor.createRunningTaskRun(createParams);

        // When
        TaskRecord updatedTask = taskExecutor.updateProgress(task.getTaskId(), "50% complete");

        // Then
        assertEquals("50% complete", updatedTask.getProgressSummary());
        assertNotNull(updatedTask.getLastEventAt());
    }
}
