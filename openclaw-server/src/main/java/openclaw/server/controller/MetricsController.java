package openclaw.server.controller;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;

/**
 * Metrics Controller - Phase 4
 *
 * <p>Provides metrics and health endpoints for monitoring.</p>
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);

    private final PrometheusMeterRegistry prometheusRegistry;

    public MetricsController(PrometheusMeterRegistry prometheusRegistry) {
        this.prometheusRegistry = prometheusRegistry;
    }

    /**
     * Prometheus metrics endpoint
     */
    @GetMapping(value = "/prometheus", produces = "text/plain")
    public Mono<String> prometheus() {
        return Mono.just(prometheusRegistry.scrape());
    }

    /**
     * System metrics
     */
    @GetMapping("/system")
    public Mono<SystemMetrics> systemMetrics() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        long uptime = runtimeMXBean.getUptime();
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        int threadCount = threadMXBean.getThreadCount();

        return Mono.just(new SystemMetrics(
                uptime,
                formatBytes(heapUsed),
                formatBytes(heapMax),
                threadCount,
                Runtime.getRuntime().availableProcessors()
        ));
    }

    /**
     * Application metrics
     */
    @GetMapping("/app")
    public Mono<AppMetrics> appMetrics() {
        // These would be populated from actual metrics in production
        return Mono.just(new AppMetrics(
                0, // activeAgents
                0, // totalMessages
                0, // totalToolsExecuted
                "2026.3.9"
        ));
    }

    /**
     * Health check with details
     */
    @GetMapping("/health")
    public Mono<HealthStatus> health() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        double heapUsagePercent = (double) heapUsed / heapMax * 100;

        String status = heapUsagePercent > 90 ? "WARNING" : "UP";

        return Mono.just(new HealthStatus(
                status,
                Map.of(
                        "heap_usage_percent", String.format("%.2f%%", heapUsagePercent),
                        "heap_used", formatBytes(heapUsed),
                        "heap_max", formatBytes(heapMax)
                )
        ));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // Response Records

    public record SystemMetrics(
            long uptimeMs,
            String heapUsed,
            String heapMax,
            int threadCount,
            int cpuCount
    ) {}

    public record AppMetrics(
            int activeAgents,
            long totalMessages,
            long totalToolsExecuted,
            String version
    ) {}

    public record HealthStatus(
            String status,
            Map<String, String> details
    ) {}
}
