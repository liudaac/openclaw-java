package openclaw.server.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics Configuration - Phase 4
 *
 * <p>Configures Prometheus metrics and monitoring.</p>
 */
@Configuration
public class MetricsConfig {

    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Custom metrics for OpenClaw
     */
    @Bean
    public OpenClawMetrics openClawMetrics(MeterRegistry registry) {
        return new OpenClawMetrics(registry);
    }

    /**
     * Custom metrics class
     */
    public static class OpenClawMetrics {

        private final MeterRegistry registry;

        public OpenClawMetrics(MeterRegistry registry) {
            this.registry = registry;
        }

        /**
         * Record agent spawn
         */
        public void recordAgentSpawn(String model, boolean success) {
            registry.counter("openclaw.agent.spawn",
                    "model", model,
                    "success", String.valueOf(success)
            ).increment();
        }

        /**
         * Record message sent
         */
        public void recordMessageSent(String channel, boolean success) {
            registry.counter("openclaw.message.sent",
                    "channel", channel,
                    "success", String.valueOf(success)
            ).increment();
        }

        /**
         * Record tool execution
         */
        public Timer.Sample startToolExecution(String toolName) {
            return Timer.start(registry);
        }

        /**
         * Record tool execution complete
         */
        public void recordToolExecution(Timer.Sample sample, String toolName, boolean success) {
            sample.stop(registry.timer("openclaw.tool.execution",
                    "tool", toolName,
                    "success", String.valueOf(success)
            ));
        }

        /**
         * Record gateway work
         */
        public void recordGatewayWork(String type, String status) {
            registry.counter("openclaw.gateway.work",
                    "type", type,
                    "status", status
            ).increment();
        }

        /**
         * Record webhook received
         */
        public void recordWebhookReceived(String channel, String eventType) {
            registry.counter("openclaw.webhook.received",
                    "channel", channel,
                    "event_type", eventType
            ).increment();
        }
    }
}
