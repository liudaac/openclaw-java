package openclaw.tasks.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openclaw.tasks.model.*;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite implementation of TaskRegistry.
 */
@Repository
public class SqliteTaskRegistry implements TaskRegistry {

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public SqliteTaskRegistry(String dbPath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        initializeSchema();
    }

    private void initializeSchema() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS tasks (
                task_id TEXT PRIMARY KEY,
                runtime TEXT NOT NULL,
                source_id TEXT,
                requester_session_key TEXT NOT NULL,
                parent_flow_id TEXT,
                child_session_key TEXT,
                parent_task_id TEXT,
                agent_id TEXT,
                run_id TEXT,
                label TEXT,
                task TEXT NOT NULL,
                status TEXT NOT NULL,
                delivery_status TEXT NOT NULL,
                notify_policy TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                started_at INTEGER,
                ended_at INTEGER,
                last_event_at INTEGER,
                cleanup_after INTEGER,
                error TEXT,
                progress_summary TEXT,
                terminal_summary TEXT,
                terminal_outcome TEXT
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public TaskRecord createTaskRecord(CreateTaskRequest request) {
        String taskId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        
        String sql = """
            INSERT INTO tasks (task_id, runtime, source_id, requester_session_key, parent_flow_id,
                child_session_key, parent_task_id, agent_id, run_id, label, task, status,
                delivery_status, notify_policy, created_at, started_at, ended_at, last_event_at,
                cleanup_after, error, progress_summary, terminal_summary, terminal_outcome)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, taskId);
            stmt.setString(2, request.runtime().getValue());
            stmt.setString(3, request.sourceId());
            stmt.setString(4, request.requesterSessionKey());
            stmt.setString(5, request.parentFlowId());
            stmt.setString(6, request.childSessionKey());
            stmt.setString(7, request.parentTaskId());
            stmt.setString(8, request.agentId());
            stmt.setString(9, request.runId());
            stmt.setString(10, request.label());
            stmt.setString(11, request.task());
            stmt.setString(12, request.status().getValue());
            stmt.setString(13, request.deliveryStatus().getValue());
            stmt.setString(14, request.notifyPolicy().getValue());
            stmt.setLong(15, now.getEpochSecond());
            stmt.setNull(16, Types.INTEGER);
            stmt.setNull(17, Types.INTEGER);
            stmt.setNull(18, Types.INTEGER);
            stmt.setNull(19, Types.INTEGER);
            stmt.setNull(20, Types.VARCHAR);
            stmt.setNull(21, Types.VARCHAR);
            stmt.setNull(22, Types.VARCHAR);
            stmt.setNull(23, Types.VARCHAR);
            
            stmt.executeUpdate();
            
            return getTaskById(taskId).orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create task", e);
        }
    }

    @Override
    public Optional<TaskRecord> getTaskById(String taskId) {
        String sql = "SELECT * FROM tasks WHERE task_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, taskId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToTask(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get task", e);
        }
    }

    @Override
    public TaskRecord updateTaskRecordById(String taskId, UpdateTaskRequest request) {
        StringBuilder sql = new StringBuilder("UPDATE tasks SET ");
        List<Object> params = new ArrayList<>();
        
        if (request.status() != null) {
            sql.append("status = ?, ");
            params.add(request.status().getValue());
        }
        if (request.deliveryStatus() != null) {
            sql.append("delivery_status = ?, ");
            params.add(request.deliveryStatus().getValue());
        }
        if (request.progressSummary() != null) {
            sql.append("progress_summary = ?, ");
            params.add(request.progressSummary());
        }
        if (request.terminalSummary() != null) {
            sql.append("terminal_summary = ?, ");
            params.add(request.terminalSummary());
        }
        if (request.terminalOutcome() != null) {
            sql.append("terminal_outcome = ?, ");
            params.add(request.terminalOutcome());
        }
        if (request.error() != null) {
            sql.append("error = ?, ");
            params.add(request.error());
        }
        if (request.startedAt() != null) {
            sql.append("started_at = ?, ");
            params.add(request.startedAt().getEpochSecond());
        }
        if (request.endedAt() != null) {
            sql.append("ended_at = ?, ");
            params.add(request.endedAt().getEpochSecond());
        }
        if (request.lastEventAt() != null) {
            sql.append("last_event_at = ?, ");
            params.add(request.lastEventAt().getEpochSecond());
        }
        
        if (params.isEmpty()) {
            return getTaskById(taskId).orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        }
        
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE task_id = ?");
        params.add(taskId);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Task not found: " + taskId);
            }
            
            return getTaskById(taskId).orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update task", e);
        }
    }

    @Override
    public List<TaskRecord> listTasksForFlowId(String flowId) {
        String sql = "SELECT * FROM tasks WHERE parent_flow_id = ? ORDER BY created_at ASC";
        List<TaskRecord> tasks = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, flowId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list tasks for flow", e);
        }
        
        return tasks;
    }

    @Override
    public List<TaskRecord> listTasksByOwner(String ownerSessionKey) {
        String sql = "SELECT * FROM tasks WHERE requester_session_key = ? ORDER BY created_at DESC";
        List<TaskRecord> tasks = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerSessionKey);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list tasks by owner", e);
        }
        
        return tasks;
    }

    @Override
    public boolean deleteTaskById(String taskId) {
        String sql = "DELETE FROM tasks WHERE task_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, taskId);
            int deleted = stmt.executeUpdate();
            return deleted > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete task", e);
        }
    }

    private TaskRecord mapResultSetToTask(ResultSet rs) throws SQLException {
        TaskRecord.TaskRecordBuilder builder = TaskRecord.builder()
            .taskId(rs.getString("task_id"))
            .runtime(TaskRuntime.valueOf(rs.getString("runtime").toUpperCase()))
            .sourceId(rs.getString("source_id"))
            .requesterSessionKey(rs.getString("requester_session_key"))
            .parentFlowId(rs.getString("parent_flow_id"))
            .childSessionKey(rs.getString("child_session_key"))
            .parentTaskId(rs.getString("parent_task_id"))
            .agentId(rs.getString("agent_id"))
            .runId(rs.getString("run_id"))
            .label(rs.getString("label"))
            .task(rs.getString("task"))
            .status(TaskStatus.valueOf(rs.getString("status").toUpperCase()))
            .deliveryStatus(TaskDeliveryStatus.valueOf(rs.getString("delivery_status").toUpperCase()))
            .notifyPolicy(TaskNotifyPolicy.valueOf(rs.getString("notify_policy").toUpperCase()))
            .createdAt(Instant.ofEpochSecond(rs.getLong("created_at")));

        // Optional timestamps
        long startedAt = rs.getLong("started_at");
        if (!rs.wasNull()) {
            builder.startedAt(Instant.ofEpochSecond(startedAt));
        }
        
        long endedAt = rs.getLong("ended_at");
        if (!rs.wasNull()) {
            builder.endedAt(Instant.ofEpochSecond(endedAt));
        }
        
        long lastEventAt = rs.getLong("last_event_at");
        if (!rs.wasNull()) {
            builder.lastEventAt(Instant.ofEpochSecond(lastEventAt));
        }
        
        long cleanupAfter = rs.getLong("cleanup_after");
        if (!rs.wasNull()) {
            builder.cleanupAfter(Instant.ofEpochSecond(cleanupAfter));
        }

        // Optional strings
        builder.error(rs.getString("error"));
        builder.progressSummary(rs.getString("progress_summary"));
        builder.terminalSummary(rs.getString("terminal_summary"));
        builder.terminalOutcome(rs.getString("terminal_outcome"));

        return builder.build();
    }
}
