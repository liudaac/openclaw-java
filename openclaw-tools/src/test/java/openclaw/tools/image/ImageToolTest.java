package openclaw.tools.image;

import openclaw.sdk.tool.ToolResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ImageTool with dependency injection.
 *
 * <p>Demonstrates the test isolation pattern using ImageToolDeps.Testing.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.23
 */
class ImageToolTest {

    private ImageToolDeps originalDeps;
    private ImageToolDeps mockDeps;
    private MockHttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        // Save original deps
        originalDeps = ImageToolDeps.Testing.getTestDeps();

        // Create mock dependencies
        mockHttpClient = new MockHttpClient();
        mockDeps = new ImageToolDeps();
        mockDeps.setHttpClientSupplier(() -> mockHttpClient);
        mockDeps.setImageDirSupplier(() -> Path.of("/tmp/test-images"));
        mockDeps.setApiKeySupplier(() -> "test-api-key");

        // Set for test
        ImageToolDeps.Testing.setDepsForTest(mockDeps);
    }

    @AfterEach
    void tearDown() {
        // Restore original deps
        if (originalDeps != null) {
            ImageToolDeps.Testing.setDepsForTest(originalDeps);
        } else {
            ImageToolDeps.Testing.clearTestDeps();
        }
    }

    @Test
    void testGenerateImage() {
        // Given
        String prompt = "A beautiful sunset";
        mockHttpClient.setNextResponse(createMockImageResponse("generated-image-url"));

        // When
        ImageTool tool = new ImageTool(Path.of("/tmp/test-images"), "test-api-key");
        ToolResult result = tool.execute("generate", Map.of(
                "prompt", prompt,
                "size", "1024x1024"
        ), null);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().containsKey("url"));
    }

    @Test
    void testGenerateImageWithOptions() {
        // Given
        String prompt = "A futuristic city";
        mockHttpClient.setNextResponse(createMockImageResponse("city-image-url"));

        // When
        ImageTool tool = new ImageTool(Path.of("/tmp/test-images"), "test-api-key");
        ToolResult result = tool.execute("generate", Map.of(
                "prompt", prompt,
                "size", "1792x1024",
                "quality", "hd",
                "style", "vivid",
                "n", 2
        ), null);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void testMissingPrompt() {
        // When
        ImageTool tool = new ImageTool(Path.of("/tmp/test-images"), "test-api-key");
        ToolResult result = tool.execute("generate", Map.of(
                "size", "1024x1024"
        ), null);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }

    @Test
    void testInvalidAction() {
        // When
        ImageTool tool = new ImageTool(Path.of("/tmp/test-images"), "test-api-key");
        ToolResult result = tool.execute("invalid_action", Map.of(), null);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        // Given - Test that deps are thread-local
        ImageToolDeps.Testing.clearTestDeps();

        // When - Set deps in another thread
        Thread otherThread = new Thread(() -> {
            ImageToolDeps otherDeps = new ImageToolDeps();
            otherDeps.setApiKeySupplier(() -> "other-key");
            ImageToolDeps.Testing.setDepsForTest(otherDeps);
        });
        otherThread.start();
        otherThread.join();

        // Then - Current thread should not have deps
        assertNull(ImageToolDeps.Testing.getTestDeps());
    }

    private String createMockImageResponse(String imageUrl) {
        return "{\"data\": [{\"url\": \"" + imageUrl + "\"}]}";
    }

    /**
     * Mock HttpClient for testing.
     */
    private static class MockHttpClient extends HttpClient {
        private String nextResponse;

        void setNextResponse(String response) {
            this.nextResponse = response;
        }

        @Override
        public <T> java.net.http.HttpResponse<T> send(
                java.net.http.HttpRequest request,
                java.net.http.HttpResponse.BodyHandler<T> responseBodyHandler
        ) throws java.io.IOException, InterruptedException {
            // Return mock response
            return null; // Simplified for example
        }
    }
}
