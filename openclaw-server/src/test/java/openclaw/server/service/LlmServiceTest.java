package openclaw.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * LLM Service Tests - Phase 4
 */
@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @InjectMocks
    private LlmService llmService;

    @Test
    void chatShouldReturnResponse() {
        // Given
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Hello, world!");

        // When
        CompletableFuture<String> result = llmService.chat("Hi");

        // Then
        assertThat(result.join()).isEqualTo("Hello, world!");
    }

    @Test
    void chatWithSystemPromptShouldCombinePrompts() {
        // Given
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("I'm helpful!");

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
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("OK");

        // When & Then
        assertThat(llmService.isAvailable()).isTrue();
    }

    @Test
    void isAvailableShouldReturnFalseWhenClientFails() {
        // Given
        when(chatClient.prompt(anyString())).thenThrow(new RuntimeException("Connection failed"));

        // When & Then
        assertThat(llmService.isAvailable()).isFalse();
    }
}
