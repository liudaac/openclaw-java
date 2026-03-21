package openclaw.channel.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TopicLabelGenerator}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class TopicLabelGeneratorTest {

    private TopicLabelGenerator generator;
    private MockLlmClient mockLlmClient;

    @BeforeEach
    void setUp() {
        mockLlmClient = new MockLlmClient();
        generator = new TopicLabelGenerator(mockLlmClient);
    }

    @Test
    void testGenerateDisabled() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.disabled();

        String result = generator.generate("Hello", config).join();

        assertNull(result);
        assertFalse(mockLlmClient.wasCalled());
    }

    @Test
    void testGenerateEmptyMessage() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("", config).join();

        assertNull(result);
        assertFalse(mockLlmClient.wasCalled());
    }

    @Test
    void testGenerateBlankMessage() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("   ", config).join();

        assertNull(result);
        assertFalse(mockLlmClient.wasCalled());
    }

    @Test
    void testGenerateSuccess() {
        mockLlmClient.setResponse("Kubernetes Deployment");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("How do I deploy to Kubernetes?", config).join();

        assertEquals("Kubernetes Deployment", result);
        assertTrue(mockLlmClient.wasCalled());
    }

    @Test
    void testGenerateWithQuotes() {
        mockLlmClient.setResponse("\"Python Script\"");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("Help me write a Python script", config).join();

        assertEquals("Python Script", result);
    }

    @Test
    void testGenerateWithSingleQuotes() {
        mockLlmClient.setResponse("'Weather Question'");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("What's the weather like?", config).join();

        assertEquals("Weather Question", result);
    }

    @Test
    void testGenerateWithNewlines() {
        mockLlmClient.setResponse("Multi\nLine\nTitle");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("Some message", config).join();

        assertEquals("Multi Line Title", result);
    }

    @Test
    void testGenerateTruncatesLongLabel() {
        String longLabel = "A".repeat(50);
        mockLlmClient.setResponse(longLabel);
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("Some message", config).join();

        assertEquals(30, result.length());
    }

    @Test
    void testGenerateEmptyResponse() {
        mockLlmClient.setResponse("");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("Some message", config).join();

        assertNull(result);
    }

    @Test
    void testGenerateWhitespaceResponse() {
        mockLlmClient.setResponse("   ");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("Some message", config).join();

        assertNull(result);
    }

    @Test
    void testGenerateLlmFailure() {
        mockLlmClient.setException(new RuntimeException("LLM error"));
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String result = generator.generate("Some message", config).join();

        assertNull(result);
    }

    @Test
    void testGenerateWithCustomPrompt() {
        String customPrompt = "Custom system prompt";
        mockLlmClient.setResponse("Title");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.withPrompt(customPrompt);

        generator.generate("User message", config).join();

        String sentPrompt = mockLlmClient.getLastPrompt();
        assertTrue(sentPrompt.contains(customPrompt));
    }

    @Test
    void testGenerateTruncatesLongMessage() {
        mockLlmClient.setResponse("Title");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        String longMessage = "A".repeat(1000);
        generator.generate(longMessage, config).join();

        String sentPrompt = mockLlmClient.getLastPrompt();
        assertTrue(sentPrompt.contains("..."));
        assertFalse(sentPrompt.contains("A".repeat(600)));
    }

    /**
     * Mock LLM client for testing.
     */
    private static class MockLlmClient implements TopicLabelGenerator.LlmClient {
        private String response;
        private Exception exception;
        private String lastPrompt;
        private boolean called = false;

        void setResponse(String response) {
            this.response = response;
            this.exception = null;
        }

        void setException(Exception exception) {
            this.exception = exception;
            this.response = null;
        }

        String getLastPrompt() {
            return lastPrompt;
        }

        boolean wasCalled() {
            return called;
        }

        @Override
        public CompletableFuture<String> generate(String prompt) {
            called = true;
            lastPrompt = prompt;
            if (exception != null) {
                return CompletableFuture.failedFuture(exception);
            }
            return CompletableFuture.completedFuture(response);
        }
    }
}
