package openclaw.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * LLM Service Tests - Phase 4
 */
@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    private ChatClient chatClient;

    @InjectMocks
    private LlmService llmService;

    @Test
    void chatShouldReturnResponse() {
        // Given
        ChatResponse mockResponse = createMockResponse("Hello, world!");
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        // When
        CompletableFuture<String> result = llmService.chat("Hi");

        // Then
        assertThat(result.join()).isEqualTo("Hello, world!");
    }

    @Test
    void chatWithSystemPromptShouldCombinePrompts() {
        // Given
        ChatResponse mockResponse = createMockResponse("I'm helpful!");
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        // When
        CompletableFuture<String> result = llmService.chat(
                "You are helpful",
                "Who are you?"
        );

        // Then
        assertThat(result.join()).isEqualTo("I'm helpful!");
    }

    @Test
    void isAvailableShouldReturnTrueWhenClientWorks() {
        // Given
        ChatResponse mockResponse = createMockResponse("OK");
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        // When
        CompletableFuture<Boolean> result = llmService.isAvailable();

        // Then
        assertThat(result.join()).isTrue();
    }

    @Test
    void isAvailableShouldReturnFalseWhenClientFails() {
        // Given
        when(chatClient.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        // When
        CompletableFuture<Boolean> result = llmService.isAvailable();

        // Then
        assertThat(result.join()).isFalse();
    }

    private ChatResponse createMockResponse(String content) {
        AssistantMessage message = new AssistantMessage(content);
        Generation generation = new Generation(message);
        return new ChatResponse(java.util.List.of(generation));
    }
}
