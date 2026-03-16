package openclaw.server.controller;

import openclaw.gateway.GatewayService;
import openclaw.server.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway Controller Tests
 */
@WebFluxTest(GatewayController.class)
@AutoConfigureWebTestClient(timeout = "10000")
@Import(TestSecurityConfig.class)
class GatewayControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private GatewayService gatewayService;

    @Test
    void healthCheckShouldReturnUp() {
        webClient.get()
                .uri("/api/v1/gateway/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(response -> {
                    assertThat(response.get("status")).isEqualTo("UP");
                    assertThat(response.get("service")).isEqualTo("openclaw-gateway");
                });
    }

    @Test
    void statsShouldReturnGatewayStats() {
        webClient.get()
                .uri("/api/v1/gateway/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(GatewayController.GatewayStatsResponse.class)
                .value(stats -> {
                    assertThat(stats.activeNodes()).isGreaterThanOrEqualTo(0);
                    assertThat(stats.pendingWork()).isGreaterThanOrEqualTo(0);
                });
    }
}
