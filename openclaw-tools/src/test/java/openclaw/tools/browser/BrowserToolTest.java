package openclaw.tools.browser;

import openclaw.browser.BrowserService;
import openclaw.browser.model.BrowserSession;
import openclaw.sdk.tool.ToolResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BrowserTool with dependency injection.
 *
 * <p>Demonstrates the test isolation pattern using BrowserToolDeps.Testing.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.23
 */
class BrowserToolTest {

    private BrowserToolDeps originalDeps;
    private BrowserToolDeps mockDeps;
    private MockBrowserService mockBrowserService;

    @BeforeEach
    void setUp() {
        // Save original deps
        originalDeps = BrowserToolDeps.Testing.getTestDeps();

        // Create mock dependencies
        mockBrowserService = new MockBrowserService();
        mockDeps = new BrowserToolDeps();
        mockDeps.setBrowserServiceSupplier(() -> mockBrowserService);
        mockDeps.setScreenshotDirSupplier(() -> Path.of("/tmp/test-screenshots"));

        // Set for test
        BrowserToolDeps.Testing.setDepsForTest(mockDeps);
    }

    @AfterEach
    void tearDown() {
        // Restore original deps
        if (originalDeps != null) {
            BrowserToolDeps.Testing.setDepsForTest(originalDeps);
        } else {
            BrowserToolDeps.Testing.clearTestDeps();
        }
    }

    @Test
    void testCreateSession() {
        // Given
        String expectedSessionId = "test-session-123";
        mockBrowserService.setNextSessionId(expectedSessionId);

        // When
        BrowserTool tool = new BrowserTool(mockBrowserService);
        ToolResult result = tool.execute("create_session", Map.of(
                "url", "https://example.com"
        ), null);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().containsKey("session_id"));
        assertEquals(expectedSessionId, result.getData().get("session_id"));
    }

    @Test
    void testNavigate() {
        // Given
        String sessionId = "existing-session";
        String url = "https://example.com";

        // When
        BrowserTool tool = new BrowserTool(mockBrowserService);
        ToolResult result = tool.execute("navigate", Map.of(
                "session", sessionId,
                "url", url
        ), null);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(url, mockBrowserService.getLastNavigatedUrl());
    }

    @Test
    void testScreenshot() {
        // Given
        String sessionId = "existing-session";
        String expectedScreenshot = "base64-screenshot-data";
        mockBrowserService.setNextScreenshot(expectedScreenshot);

        // When
        BrowserTool tool = new BrowserTool(mockBrowserService);
        ToolResult result = tool.execute("screenshot", Map.of(
                "session", sessionId
        ), null);

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("screenshot"));
        assertEquals(expectedScreenshot, result.getData().get("screenshot"));
    }

    @Test
    void testInvalidAction() {
        // When
        BrowserTool tool = new BrowserTool(mockBrowserService);
        ToolResult result = tool.execute("invalid_action", Map.of(), null);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        // Given - Test that deps are thread-local
        BrowserToolDeps.Testing.clearTestDeps();

        // When - Set deps in another thread
        Thread otherThread = new Thread(() -> {
            BrowserToolDeps otherDeps = new BrowserToolDeps();
            otherDeps.setScreenshotDirSupplier(() -> Path.of("/other/path"));
            BrowserToolDeps.Testing.setDepsForTest(otherDeps);
        });
        otherThread.start();
        otherThread.join();

        // Then - Current thread should not have deps
        assertNull(BrowserToolDeps.Testing.getTestDeps());
    }

    /**
     * Mock BrowserService for testing.
     */
    private static class MockBrowserService extends BrowserService {
        private String nextSessionId;
        private String nextScreenshot;
        private String lastNavigatedUrl;

        @Override
        public CompletableFuture<BrowserSession> createSession(String url, BrowserSession.SessionOptions options) {
            BrowserSession session = new BrowserSession(nextSessionId, url);
            return CompletableFuture.completedFuture(session);
        }

        @Override
        public CompletableFuture<Void> navigate(String sessionId, String url) {
            lastNavigatedUrl = url;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<String> screenshot(String sessionId) {
            return CompletableFuture.completedFuture(nextScreenshot);
        }

        // Setters for mock control
        void setNextSessionId(String sessionId) {
            this.nextSessionId = sessionId;
        }

        void setNextScreenshot(String screenshot) {
            this.nextScreenshot = screenshot;
        }

        String getLastNavigatedUrl() {
            return lastNavigatedUrl;
        }
    }
}
