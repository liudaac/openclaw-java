package openclaw.server.controller;

import openclaw.gateway.GatewayService;
import openclaw.gateway.node.NodeRegistry;
import openclaw.gateway.queue.WorkQueue;
import openclaw.server.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    @MockBean
    private NodeRegistry nodeRegistry;

    @MockBean
    private WorkQueue workQueue;

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
        // Setup mocks
        when(gatewayService.getNodeRegistry()).thenReturn(nodeRegistry);
        when(gatewayService.getWorkQueue()).thenReturn(workQueue);
        when(nodeRegistry.getNodeCount()).thenReturn(CompletableFuture.completedFuture(5));
        when(workQueue.getPendingCount()).thenReturn(CompletableFuture.completedFuture(10));
        when(workQueue.getCompletedCount()).thenReturn(CompletableFuture.completedFuture(100L));

        webClient.get()
                .uri("/api/v1/gateway/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(GatewayController.GatewayStatsResponse.class)
                .value(stats -> {
                    assertThat(stats.activeNodes()).isEqualTo(5);
                    assertThat(stats.pendingWork()).isEqualTo(10);
                    assertThat(stats.completedWork()).isEqualTo(100L);
                });
    }
}
