package openclaw.cron.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openclaw.cron.model.CronJob;
import openclaw.cron.model.JobExecution;
import openclaw.cron.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SQLite implementation of CronJobStore.
 * 
 * <p>Provides persistent storage for cron jobs and their execution history.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class SQLiteCronJobStore implements CronJobStore {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLiteCronJobStore.class);
    
    private final Path dbPath;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private Connection connection;
    
    public SQLiteCronJobStore(Path dataDir) {
        this.dbPath = dataDir.resolve("cron.db");
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "cron-store-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                dbPath.getParent().toFile().mkdirs();
                String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
                connection = DriverManager.getConnection(url);
                connection.setAutoCommit(true);
                createTables();
                logger.info("SQLite cron store initialized at {}", dbPath);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize SQLite store", e);
            }
        }, executor);
    }
    
    private void createTables() throws SQLException {
        String createJobsTable = """
            CREATE TABLE IF NOT EXISTS jobs (
                id TEXT PRIMARY KEY, name TEXT NOT NULL, schedule TEXT NOT NULL,
                command TEXT NOT NULL, timezone TEXT DEFAULT 'UTC',
                status TEXT DEFAULT 'PENDING', last_run INTEGER, next_run INTEGER,
                run_count INTEGER DEFAULT 0, fail_count INTEGER DEFAULT 0,
                created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
                metadata TEXT, max_retries INTEGER DEFAULT 3, retry_delay_ms INTEGER DEFAULT 1000,
                isolated INTEGER DEFAULT 1, timeout_seconds INTEGER DEFAULT 60, working_directory TEXT
            )""";
        
        String createExecutionsTable = """
            CREATE TABLE IF NOT EXISTS executions (
                id TEXT PRIMARY KEY, job_id TEXT NOT NULL, start_time INTEGER NOT NULL,
                end_time INTEGER, status TEXT DEFAULT 'RUNNING', output TEXT, error TEXT,
                exit_code INTEGER, retry_attempt INTEGER DEFAULT 0, executor_host TEXT,
                memory_usage_bytes INTEGER DEFAULT 0, cpu_time_ms INTEGER,
                FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
            )""";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createJobsTable);
            stmt.execute(createExecutionsTable);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_jobs_next_run ON jobs(next_run)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_executions_job ON executions(job_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_executions_start ON executions(start_time DESC)");
        }
    }
    
    @Override
    public CompletableFuture<Void> save(CronJob job) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO jobs VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, job.getId());
                stmt.setString(2, job.getName());
                stmt.setString(3, job.getSchedule());
                stmt.setString(4, job.getCommand());
                stmt.setString(5, job.getTimezone());
                stmt.setString(6, job.getStatus().name());
                stmt.setLong(7, job.getLastRun() != null ? job.getLastRun().toEpochMilli() : 0);
                stmt.setLong(8, job.getNextRun() != null ? job.getNextRun().toEpochMilli() : 0);
                stmt.setInt(9, job.getRunCount());
                stmt.setInt(10, job.getFailCount());
                stmt.setLong(11, job.getCreatedAt().toEpochMilli());
                stmt.setLong(12, job.getUpdatedAt().toEpochMilli());
                stmt.setString(13, serializeMetadata(job.getMetadata()));
                stmt.setInt(14, job.getMaxRetries());
                stmt.setLong(15, job.getRetryDelayMs());
                stmt.setInt(16, job.isIsolated() ? 1 : 0);
                stmt.setLong(17, job.getTimeoutSeconds());
                stmt.setString(18, job.getWorkingDirectory());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save job", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Optional<CronJob>> findById(String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM jobs WHERE id = ?")) {
                stmt.setString(1, jobId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? Optional.of(mapResultSetToJob(rs)) : Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find job", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Optional<CronJob>> findByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM jobs WHERE name = ?")) {
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? Optional.of(mapResultSetToJob(rs)) : Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find job by name", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<CronJob>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<CronJob> jobs = new ArrayList<>();
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM jobs ORDER BY created_at DESC")) {
                while (rs.next()) {
                    jobs.add(mapResultSetToJob(rs));
                }
                return jobs;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to list jobs", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<CronJob>> findByStatus(JobStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            List<CronJob> jobs = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM jobs WHERE status = ? ORDER BY next_run ASC")) {
                stmt.setString(1, status.name());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    jobs.add(mapResultSetToJob(rs));
                }
                return jobs;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find jobs by status", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<CronJob>> findActive() {
        return CompletableFuture.supplyAsync(() -> {
            List<CronJob> jobs = new ArrayList<>();
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM jobs WHERE status IN ('PENDING', 'RUNNING', 'PAUSED') ORDER BY next_run ASC")) {
                while (rs.next()) {
                    jobs.add(mapResultSetToJob(rs));
                }
                return jobs;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find active jobs", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Boolean> delete(String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM jobs WHERE id = ?")) {
                stmt.setString(1, jobId);
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete job", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> updateStatus(String jobId, JobStatus status) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("UPDATE jobs SET status = ?, updated_at = ? WHERE id = ?")) {
                stmt.setString(1, status.name());
                stmt.setLong(2, Instant.now().toEpochMilli());
                stmt.setString(3, jobId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update job status", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> updateNextRun(String jobId, Instant nextRun) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("UPDATE jobs SET next_run = ?, updated_at = ? WHERE id = ?")) {
                stmt.setLong(1, nextRun.toEpochMilli());
                stmt.setLong(2, Instant.now().toEpochMilli());
                stmt.setString(3, jobId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update next run", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> incrementRunCount(String jobId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("UPDATE jobs SET run_count = run_count + 1, last_run = ?, updated_at = ? WHERE id = ?")) {
                stmt.setLong(1, Instant.now().toEpochMilli());
                stmt.setLong(2, Instant.now().toEpochMilli());
                stmt.setString(3, jobId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to increment run count", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> incrementFailCount(String jobId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("UPDATE jobs SET fail_count = fail_count + 1, updated_at = ? WHERE id = ?")) {
                stmt.setLong(1, Instant.now().toEpochMilli());
                stmt.setString(2, jobId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to increment fail count", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> saveExecution(JobExecution execution) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO executions VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, execution.getId());
                stmt.setString(2, execution.getJobId());
                stmt.setLong(3, execution.getStartTime().toEpochMilli());
                stmt.setLong(4, execution.getEndTime() != null ? execution.getEndTime().toEpochMilli() : 0);
                stmt.setString(5, execution.getStatus().name());
                stmt.setString(6, execution.getOutput());
                stmt.setString(7, execution.getError());
                stmt.setInt(8, execution.getExitCode());
                stmt.setInt(9, execution.getRetryAttempt());
                stmt.setString(10, execution.getExecutorHost());
                stmt.setLong(11, execution.getMemoryUsageBytes());
                stmt.setLong(12, execution.getCpuTime() != null ? execution.getCpuTime().toMillis() : 0);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save execution", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Optional<JobExecution>> findExecutionById(String executionId) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM executions WHERE id = ?")) {
                stmt.setString(1, executionId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? Optional.of(mapResultSetToExecution(rs)) : Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find execution", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<JobExecution>> findExecutionsByJob(String jobId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<JobExecution> executions = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM executions WHERE job_id = ? ORDER BY start_time DESC LIMIT ?")) {
                stmt.setString(1, jobId);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    executions.add(mapResultSetToExecution(rs));
                }
                return executions;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find executions", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<JobExecution>> findRecentExecutions(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<JobExecution> executions = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM executions ORDER BY start_time DESC LIMIT ?")) {
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    executions.add(mapResultSetToExecution(rs));
                }
                return executions;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find recent executions", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<ExecutionStats> getExecutionStats(String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT COUNT(*) as total, 
                       SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success,
                       SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed,
                       AVG(CASE WHEN end_time > start_time THEN (end_time - start_time) ELSE 0 END) as avg_duration,
                       MAX(start_time) as last_run
                FROM executions WHERE job_id = ?
                """;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, jobId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new ExecutionStats(
                        rs.getInt("total"),
                        rs.getInt("success"),
                        rs.getInt("failed"),
                        rs.getDouble("avg_duration"),
                        rs.getLong("last_run") > 0 ? Instant.ofEpochMilli(rs.getLong("last_run")) : null
                    );
                }
                return new ExecutionStats(0, 0, 0, 0, null);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get execution stats", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            executor.shutdown();
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Failed to close connection", e);
                }
            }
        });
    }
    
    // Helper methods
    
    private CronJob mapResultSetToJob(ResultSet rs) throws SQLException {
        CronJob job = new CronJob();
        job.setName(rs.getString("name"));
        job.setSchedule(rs.getString("schedule"));
        job.setCommand(rs.getString("command"));
        job.setTimezone(rs.getString("timezone"));
        job.setStatus(JobStatus.valueOf(rs.getString("status")));
        long lastRun = rs.getLong("last_run");
        if (lastRun > 0) job.setLastRun(Instant.ofEpochMilli(lastRun));
        long nextRun = rs.getLong("next_run");
        if (nextRun > 0) job.setNextRun(Instant.ofEpochMilli(nextRun));
        return job;
    }
    
    private JobExecution mapResultSetToExecution(ResultSet rs) throws SQLException {
        JobExecution execution = new JobExecution(rs.getString("job_id"));
        execution.setStatus(JobExecution.JobExecutionStatus.valueOf(rs.getString("status")));
        execution.setOutput(rs.getString("output"));
        execution.setError(rs.getString("error"));
        execution.setExitCode(rs.getInt("exit_code"));
        execution.setRetryAttempt(rs.getInt("retry_attempt"));
        return execution;
    }
    
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize metadata", e);
            return null;
        }
    }
    
    private Map<String, Object> deserializeMetadata(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize metadata", e);
            return null;
        }
    }
}
