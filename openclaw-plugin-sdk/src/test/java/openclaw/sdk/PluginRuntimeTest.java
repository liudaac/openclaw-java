package openclaw.sdk;

import openclaw.sdk.core.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PluginRuntime interfaces.
 */
class PluginRuntimeTest {

    @Test
    void testSubagentRunParamsBuilder() {
        SubagentRunParams params = SubagentRunParams.builder()
                .sessionKey("test-session")
                .message("Hello")
                .lane("main")
                .deliver(true)
                .build();

        assertThat(params.sessionKey()).isEqualTo("test-session");
        assertThat(params.message()).isEqualTo("Hello");
        assertThat(params.lane()).isEqualTo(Optional.of("main"));
        assertThat(params.deliver()).isTrue();
        assertThat(params.idempotencyKey()).isEmpty();
    }

    @Test
    void testSubagentWaitResult() {
        SubagentWaitResult ok = SubagentWaitResult.ok();
        assertThat(ok.status()).isEqualTo(SubagentWaitResult.WaitStatus.OK);
        assertThat(ok.error()).isEmpty();

        SubagentWaitResult error = SubagentWaitResult.error("Failed");
        assertThat(error.status()).isEqualTo(SubagentWaitResult.WaitStatus.ERROR);
        assertThat(error.error()).hasValue("Failed");

        SubagentWaitResult timeout = SubagentWaitResult.timeout();
        assertThat(timeout.status()).isEqualTo(SubagentWaitResult.WaitStatus.TIMEOUT);
    }

    @Test
    void testHealthStatus() {
        HealthStatus healthy = HealthStatus.healthy("All good");
        assertThat(healthy.status()).isEqualTo(HealthStatus.Status.HEALTHY);
        assertThat(healthy.message()).hasValue("All good");

        HealthStatus unhealthy = HealthStatus.unhealthy("Connection failed");
        assertThat(unhealthy.status()).isEqualTo(HealthStatus.Status.UNHEALTHY);

        HealthStatus degraded = HealthStatus.degraded("Slow response");
        assertThat(degraded.status()).isEqualTo(HealthStatus.Status.DEGRADED);
    }

    @Test
    void testPluginLoggerWithPrefix() {
        StringBuilder log = new StringBuilder();
        PluginLogger logger = new PluginLogger() {
            @Override
            public void info(String message) {
                log.append(message);
            }

            @Override
            public void warn(String message) {
                log.append(message);
            }

            @Override
            public void error(String message) {
                log.append(message);
            }
        };

        PluginLogger prefixed = logger.withPrefix("TEST");
        prefixed.info("message");

        assertThat(log.toString()).isEqualTo("[TEST] message");
    }
}
