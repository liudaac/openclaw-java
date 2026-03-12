package openclaw.server.controller;

import openclaw.agent.AcpProtocol;
import openclaw.agent.AcpProtocol.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Agent Controller Tests - Phase 4
 */
@WebFluxTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private AcpProtocol acpProtocol;

    @Test
    void spawnAgentShouldCreateSession() {
        when(acpProtocol.spawnAgent(any(SpawnRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        SpawnResult.success("session-123", "agent-456")
                ));

        webClient.post()
                .uri("/api/v1/agent/spawn")
                .bodyValue(Map.of(
                        "message", "Hello",
                        "model", "gpt-4"
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.sessionKey").isEqualTo("session-123");
    }

    @Test
    void sendMessageShouldReturnOk() {
        when(acpProtocol.sendMessage(anyString(), any(AgentMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        webClient.post()
                .uri("/api/v1/agent/session-123/message")
                .bodyValue(Map.of("message", "How are you?"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("sent");
    }

    @Test
    void getMessagesShouldReturnList() {
        when(acpProtocol.getMessages(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        new AgentMessages(java.util.List.of(
                                new AgentMessage("user", "Hello", System.currentTimeMillis()),
                                new AgentMessage("assistant", "Hi!", System.currentTimeMillis())
                        ), false)
                ));

        webClient.get()
                .uri("/api/v1/agent/session-123/messages?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.messages").isArray()
                .jsonPath("$.messages.length()").isEqualTo(2);
    }

    @Test
    void deleteSessionShouldReturnNoContent() {
        when(acpProtocol.deleteSession(anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        webClient.delete()
                .uri("/api/v1/agent/session-123")
                .exchange()
                .expectStatus().isNoContent();
    }
}
