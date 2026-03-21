package openclaw.channel.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AutoTopicLabelService}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class AutoTopicLabelServiceTest {

    private AutoTopicLabelService service;
    private MockLabelGenerator mockGenerator;
    private MockApiClient mockApiClient;

    @BeforeEach
    void setUp() {
        mockGenerator = new MockLabelGenerator();
        mockApiClient = new MockApiClient();
        service = new AutoTopicLabelService(mockGenerator, mockApiClient);
    }

    @Test
    void testShouldApplyDisabled() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.disabled();
        assertFalse(service.shouldApply(false, true, config));
    }

    @Test
    void testShouldApplyGroup() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();
        assertFalse(service.shouldApply(true, true, config));
    }

    @Test
    void testShouldApplyNotDmTopic() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();
        assertFalse(service.shouldApply(false, false, config));
    }

    @Test
    void testShouldApplyDmTopic() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();
        assertTrue(service.shouldApply(false, true, config));
    }

    @Test
    void testIsFirstTurnInSessionNullStore() {
        assertFalse(service.isFirstTurnInSession(null, "session-1"));
    }

    @Test
    void testIsFirstTurnInSessionNullKey() {
        MockSessionStore store = new MockSessionStore();
        assertFalse(service.isFirstTurnInSession(store, null));
    }

    @Test
    void testIsFirstTurnInSessionNoEntry() {
        MockSessionStore store = new MockSessionStore();
        assertTrue(service.isFirstTurnInSession(store, "session-1"));
    }

    @Test
    void testIsFirstTurnInSessionNoSystemSent() {
        MockSessionStore store = new MockSessionStore();
        store.addEntry("session-1", false);
        assertTrue(service.isFirstTurnInSession(store, "session-1"));
    }

    @Test
    void testIsFirstTurnInSessionHasSystemSent() {
        MockSessionStore store = new MockSessionStore();
        store.addEntry("session-1", true);
        assertFalse(service.isFirstTurnInSession(store, "session-1"));
    }

    @Test
    void testApplyDisabled() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.disabled();

        service.apply(123L, 456, "Hello", config).join();

        assertFalse(mockGenerator.wasCalled());
        assertFalse(mockApiClient.wasCalled());
    }

    @Test
    void testApplySuccess() {
        mockGenerator.setLabel("Test Topic");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        service.apply(123L, 456, "Hello", config).join();

        assertTrue(mockGenerator.wasCalled());
        assertTrue(mockApiClient.wasCalled());
        assertEquals("Test Topic", mockApiClient.getLastName());
    }

    @Test
    void testApplyNullLabel() {
        mockGenerator.setLabel(null);
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        service.apply(123L, 456, "Hello", config).join();

        assertTrue(mockGenerator.wasCalled());
        assertFalse(mockApiClient.wasCalled());
    }

    @Test
    void testApplyEmptyLabel() {
        mockGenerator.setLabel("");
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        service.apply(123L, 456, "Hello", config).join();

        assertTrue(mockGenerator.wasCalled());
        assertFalse(mockApiClient.wasCalled());
    }

    @Test
    void testApplyApiFailure() {
        mockGenerator.setLabel("Test Topic");
        mockApiClient.setException(new RuntimeException("API error"));
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();

        // Should not throw
        assertDoesNotThrow(() -> service.apply(123L, 456, "Hello", config).join());

        assertTrue(mockGenerator.wasCalled());
        assertTrue(mockApiClient.wasCalled());
    }

    /**
     * Mock label generator.
     */
    private static class MockLabelGenerator extends TopicLabelGenerator {
        private String label;
        private boolean called = false;

        MockLabelGenerator() {
            super(null);
        }

        void setLabel(String label) {
            this.label = label;
        }

        boolean wasCalled() {
            return called;
        }

        @Override
        public CompletableFuture<String> generate(String userMessage, AutoTopicLabelConfig config) {
            called = true;
            return CompletableFuture.completedFuture(label);
        }
    }

    /**
     * Mock API client.
     */
    private static class MockApiClient implements AutoTopicLabelService.TelegramApiClient {
        private String lastName;
        private Exception exception;
        private boolean called = false;

        void setException(Exception exception) {
            this.exception = exception;
        }

        String getLastName() {
            return lastName;
        }

        boolean wasCalled() {
            return called;
        }

        @Override
        public CompletableFuture<Void> editForumTopic(long chatId, int topicThreadId, String name) {
            called = true;
            lastName = name;
            if (exception != null) {
                return CompletableFuture.failedFuture(exception);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Mock session store.
     */
    private static class MockSessionStore implements AutoTopicLabelService.SessionStore {
        private final java.util.Map<String, MockSessionEntry> entries = new java.util.HashMap<>();

        void addEntry(String key, boolean hasSystemSent) {
            entries.put(key, new MockSessionEntry(hasSystemSent));
        }

        @Override
        public AutoTopicLabelService.SessionEntry getEntry(String sessionKey) {
            return entries.get(sessionKey);
        }
    }

    /**
     * Mock session entry.
     */
    private static class MockSessionEntry implements AutoTopicLabelService.SessionEntry {
        private final boolean hasSystemSent;

        MockSessionEntry(boolean hasSystemSent) {
            this.hasSystemSent = hasSystemSent;
        }

        @Override
        public boolean hasSystemSent() {
            return hasSystemSent;
        }
    }
}
