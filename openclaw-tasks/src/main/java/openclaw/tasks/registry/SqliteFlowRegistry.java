package openclaw.tasks.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openclaw.tasks.model.*;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * SQLite implementation of FlowRegistry.
 */
@Repository
public class SqliteFlowRegistry implements FlowRegistry {

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public SqliteFlowRegistry(String dbPath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        initializeSchema();
    }

    private void initializeSchema() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS flows (
                flow_id TEXT PRIMARY KEY,
                shape TEXT NOT NULL,
                owner_session_key TEXT NOT NULL,
                requester_origin TEXT,
                status TEXT NOT NULL,
                notify_policy TEXT NOT NULL,
                goal TEXT NOT NULL,
                current_step TEXT,
                waiting_on_task_id TEXT,
                outputs TEXT,
                blocked_task_id TEXT,
                blocked_summary TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                ended_at INTEGER
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public FlowRecord createFlowRecord(CreateFlowRequest request) {
        String flowId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        
        String sql = """
            INSERT INTO flows (flow_id, shape, owner_session_key, requester_origin, status, 
                notify_policy, goal, current_step, waiting_on_task_id, outputs, blocked_task_id, 
                blocked_summary, created_at, updated_at, ended_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, flowId);
            stmt.setString(2, request.shape().getValue());
            stmt.setString(3, request.ownerSessionKey());
            stmt.setString(4, null); // requesterOrigin - serialize as JSON
            stmt.setString(5, request.status().getValue());
            stmt.setString(6, request.notifyPolicy().getValue());
            stmt.setString(7, request.goal());
            stmt.setString(8, request.currentStep());
            stmt.setNull(9, Types.VARCHAR);
            stmt.setNull(10, Types.VARCHAR);
            stmt.setNull(11, Types.VARCHAR);
            stmt.setNull(12, Types.VARCHAR);
            stmt.setLong(13, now.getEpochSecond());
            stmt.setLong(14, now.getEpochSecond());
            stmt.setNull(15, Types.INTEGER);
            
            stmt.executeUpdate();
            
            return getFlowById(flowId).orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create flow", e);
        }
    }

    @Override
    public Optional<FlowRecord> getFlowById(String flowId) {
        String sql = "SELECT * FROM flows WHERE flow_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, flowId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToFlow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get flow", e);
        }
    }

    @Override
    public FlowRecord updateFlowRecordById(String flowId, UpdateFlowRequest request) {
        StringBuilder sql = new StringBuilder("UPDATE flows SET ");
        List<Object> params = new ArrayList<>();
        
        if (request.status() != null) {
            sql.append("status = ?, ");
            params.add(request.status().getValue());
        }
        if (request.currentStep() != null) {
            sql.append("current_step = ?, ");
            params.add(request.currentStep());
        }
        if (request.waitingOnTaskId() != null) {
            sql.append("waiting_on_task_id = ?, ");
            params.add(request.waitingOnTaskId());
        }
        if (request.blockedTaskId() != null) {
            sql.append("blocked_task_id = ?, ");
            params.add(request.blockedTaskId());
        }
        if (request.blockedSummary() != null) {
            sql.append("blocked_summary = ?, ");
            params.add(request.blockedSummary());
        }
        if (request.outputs() != null) {
            sql.append("outputs = ?, ");
            try {
                params.add(objectMapper.writeValueAsString(request.outputs()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize outputs", e);
            }
        }
        if (request.endedAt() != null) {
            sql.append("ended_at = ?, ");
            params.add(request.endedAt().getEpochSecond());
        }
        if (request.updatedAt() != null) {
            sql.append("updated_at = ?, ");
            params.add(request.updatedAt().getEpochSecond());
        }
        
        // Remove trailing comma and space
        if (params.isEmpty()) {
            return getFlowById(flowId).orElseThrow(() -> new IllegalArgumentException("Flow not found: " + flowId));
        }
        
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE flow_id = ?");
        params.add(flowId);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Flow not found: " + flowId);
            }
            
            return getFlowById(flowId).orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update flow", e);
        }
    }

    @Override
    public List<FlowRecord> listFlowsByOwner(String ownerSessionKey) {
        String sql = "SELECT * FROM flows WHERE owner_session_key = ? ORDER BY created_at DESC";
        List<FlowRecord> flows = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerSessionKey);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                flows.add(mapResultSetToFlow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list flows", e);
        }
        
        return flows;
    }

    @Override
    public List<FlowRecord> listFlowsByStatus(FlowStatus status) {
        String sql = "SELECT * FROM flows WHERE status = ? ORDER BY updated_at DESC";
        List<FlowRecord> flows = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.getValue());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                flows.add(mapResultSetToFlow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list flows by status", e);
        }
        
        return flows;
    }

    @Override
    public boolean deleteFlowById(String flowId) {
        String sql = "DELETE FROM flows WHERE flow_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, flowId);
            int deleted = stmt.executeUpdate();
            return deleted > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete flow", e);
        }
    }

    @SuppressWarnings("unchecked")
    private FlowRecord mapResultSetToFlow(ResultSet rs) throws SQLException {
        FlowRecord.FlowRecordBuilder builder = FlowRecord.builder()
            .flowId(rs.getString("flow_id"))
            .shape(FlowShape.valueOf(rs.getString("shape").toUpperCase()))
            .ownerSessionKey(rs.getString("owner_session_key"))
            .status(FlowStatus.valueOf(rs.getString("status").toUpperCase()))
            .notifyPolicy(TaskNotifyPolicy.valueOf(rs.getString("notify_policy").toUpperCase()))
            .goal(rs.getString("goal"))
            .currentStep(rs.getString("current_step"))
            .waitingOnTaskId(rs.getString("waiting_on_task_id"))
            .blockedTaskId(rs.getString("blocked_task_id"))
            .blockedSummary(rs.getString("blocked_summary"))
            .createdAt(Instant.ofEpochSecond(rs.getLong("created_at")))
            .updatedAt(Instant.ofEpochSecond(rs.getLong("updated_at")));

        // Parse requester_origin JSON
        String requesterOriginJson = rs.getString("requester_origin");
        if (requesterOriginJson != null) {
            try {
                DeliveryContext origin = objectMapper.readValue(requesterOriginJson, DeliveryContext.class);
                builder.requesterOrigin(origin);
            } catch (JsonProcessingException e) {
                // Ignore parsing errors
            }
        }

        // Parse outputs JSON
        String outputsJson = rs.getString("outputs");
        if (outputsJson != null) {
            try {
                Map<String, Object> outputs = objectMapper.readValue(outputsJson, Map.class);
                builder.outputs(outputs);
            } catch (JsonProcessingException e) {
                // Ignore parsing errors
            }
        }

        // Parse ended_at
        long endedAt = rs.getLong("ended_at");
        if (!rs.wasNull()) {
            builder.endedAt(Instant.ofEpochSecond(endedAt));
        }

        return builder.build();
    }
}