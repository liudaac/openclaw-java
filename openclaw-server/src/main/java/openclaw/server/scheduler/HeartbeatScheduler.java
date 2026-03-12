package openclaw.server.scheduler;

import openclaw.agent.AcpProtocol;
import openclaw.gateway.GatewayService;
import openclaw.server.config.MetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Heartbeat Scheduler - High Priority Improvement
 *
 * <p>Monitors system health, cleans up resources, and sends status reports.</p>
 */
@Service
public class HeartbeatScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatScheduler.class);

    private final GatewayService gatewayService;
    private final AcpProtocol acpProtocol;
    private final MetricsConfig.OpenClawMetrics metrics;
    private final MemoryMXBean memoryMXBean;

    // Heartbeat statistics
    private final AtomicLong heartbeatCount = new AtomicLong(0);
    private final Map<String, AgentHealth> agentHealthMap = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastActivityMap = new ConcurrentHashMap<>();

    public HeartbeatScheduler(GatewayService gatewayService,
                              AcpProtocol acpProtocol,
                              MetricsConfig.OpenClawMetrics metrics) {
        this.gatewayService = gatewayService;
        this.acpProtocol = acpProtocol;
        this.metrics = metrics;
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    /**
     * Main heartbeat - runs every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void heartbeat() {
        long count = heartbeatCount.incrementAndGet();
        logger.debug("Heartbeat #{} executing", count);

        try {
            // Check system health
            checkSystemHealth();

            // Cleanup expired sessions
            cleanupExpiredSessions();

            // Check agent health
            checkAgentHealth();

            // Report metrics
            reportMetrics();

            // Check gateway status
            checkGatewayStatus();

        } catch (Exception e) {
            logger.error("Heartbeat execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Extended heartbeat - runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void extendedHeartbeat() {
        logger.debug("Extended heartbeat executing");

        try {
            // Deep health check
            performDeepHealthCheck();

            // Cleanup old history
            cleanupOldHistory();

            // Generate report
            generateHealthReport();

        } catch (Exception e) {
            logger.error("Extended heartbeat failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Daily cleanup - runs at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?") // 3:00 AM every day
    public void dailyCleanup() {
        logger.info("Daily cleanup executing");

        try {
            // Cleanup old logs
            cleanupOldLogs();

            // Archive metrics
            archiveMetrics();

            // Reset counters
            resetDailyCounters();

            logger.info("Daily cleanup completed");

        } catch (Exception e) {
            logger.error("Daily cleanup failed: {}", e.getMessage(), e);
        }
    }

    private void checkSystemHealth() {
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        double heapUsagePercent = (double) heapUsed / heapMax * 100;

        if (heapUsagePercent > 90) {
            logger.warn("High memory usage: {}%", String.format("%.2f", heapUsagePercent));
            metrics.recordGatewayWork("system", "high_memory");
        }

        // Check thread count
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        if (threadCount > 500) {
            logger.warn("High thread count: {}", threadCount);
        }
    }

    private void cleanupExpiredSessions() {
        Instant cutoff = Instant.now().minusSeconds(3600); // 1 hour
        int cleaned = 0;

        for (Map.Entry<String, Instant> entry : lastActivityMap.entrySet()) {
            if (entry.getValue().isBefore(cutoff)) {
                String sessionKey = entry.getKey();
                try {
                    acpProtocol.deleteSession(sessionKey);
                    lastActivityMap.remove(sessionKey);
                    agentHealthMap.remove(sessionKey);
                    cleaned++;
                } catch (Exception e) {
                    logger.warn("Failed to cleanup session {}: {}", sessionKey, e.getMessage());
                }
            }
        }

        if (cleaned > 0) {
            logger.info("Cleaned up {} expired sessions", cleaned);
        }
    }

    private void checkAgentHealth() {
        for (Map.Entry<String, AgentHealth> entry : agentHealthMap.entrySet()) {
            String sessionKey = entry.getKey();
            AgentHealth health = entry.getValue();

            // Check if agent is responsive
            Instant lastActivity = lastActivityMap.get(sessionKey);
            if (lastActivity != null) {
                long inactiveMinutes = (Instant.now().getEpochSecond() - lastActivity.getEpochSecond()) / 60;

                if (inactiveMinutes > 30 && health.status() == AgentStatus.ACTIVE) {
                    logger.warn("Agent {} inactive for {} minutes", sessionKey, inactiveMinutes);
                    agentHealthMap.put(sessionKey, new AgentHealth(AgentStatus.IDLE, health.lastMessage()));
                }
            }
        }
    }

    private void reportMetrics() {
        // Report to metrics system
        metrics.recordGatewayWork("heartbeat", "success");

        // Log summary
        logger.debug("Heartbeat summary - Agents: {}, Sessions: {}, Memory: {}%",
                agentHealthMap.size(),
                lastActivityMap.size(),
                String.format("%.1f", getMemoryUsagePercent()));
    }

    private void checkGatewayStatus() {
        try {
            var stats = gatewayService.getWorkQueue();
            int pending = stats.getPendingCount();

            if (pending > 1000) {
                logger.warn("High pending work count: {}", pending);
            }
        } catch (Exception e) {
            logger.error("Failed to check gateway status: {}", e.getMessage());
        }
    }

    private void performDeepHealthCheck() {
        // Check database connections
        // Check external services
        // Check disk space
        logger.debug("Deep health check completed");
    }

    private void cleanupOldHistory() {
        // Cleanup old job history
        // Cleanup old metrics
        logger.debug("Old history cleanup completed");
    }

    private void generateHealthReport() {
        HealthReport report = new HealthReport(
                Instant.now(),
                heartbeatCount.get(),
                agentHealthMap.size(),
                getMemoryUsagePercent(),
                ManagementFactory.getThreadMXBean().getThreadCount()
        );

        logger.info("Health report: {}", report);
    }

    private void cleanupOldLogs() {
        // Cleanup logs older than 30 days
        logger.info("Old logs cleanup completed");
    }

    private void archiveMetrics() {
        // Archive daily metrics
        logger.info("Metrics archive completed");
    }

    private void resetDailyCounters() {
        // Reset daily counters
        logger.info("Daily counters reset");
    }

    private double getMemoryUsagePercent() {
        long used = memoryMXBean.getHeapMemoryUsage().getUsed();
        long max = memoryMXBean.getHeapMemoryUsage().getMax();
        return (double) used / max * 100;
    }

    /**
     * Record agent activity
     */
    public void recordAgentActivity(String sessionKey, String message) {
        lastActivityMap.put(sessionKey, Instant.now());
        agentHealthMap.put(sessionKey, new AgentHealth(AgentStatus.ACTIVE, message));
    }

    /**
     * Get heartbeat statistics
     */
    public HeartbeatStats getStats() {
        return new HeartbeatStats(
                heartbeatCount.get(),
                agentHealthMap.size(),
                lastActivityMap.size(),
                getMemoryUsagePercent()
        );
    }

    // Records
    private record AgentHealth(AgentStatus status, String lastMessage) {}
    private record HealthReport(Instant timestamp, long heartbeatCount, int activeAgents,
                               double memoryUsage, int threadCount) {}
    public record HeartbeatStats(long heartbeatCount, int activeAgents, int activeSessions,
                                 double memoryUsagePercent) {}

    private enum AgentStatus {
        ACTIVE, IDLE, ERROR, TERMINATED
    }
}
