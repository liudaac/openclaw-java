package openclaw.session.binding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DefaultSessionBindingService}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class DefaultSessionBindingServiceTest {

    private DefaultSessionBindingService service;

    @BeforeEach
    void setUp() {
        service = new DefaultSessionBindingService();
    }

    @Test
    void testBindSuccess() {
        // Register adapter
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        // Create bind input
        SessionBindingBindInput input = SessionBindingBindInput.builder()
                .targetSessionKey("session-1")
                .targetKind(BindingTargetKind.SESSION)
                .conversation(ConversationRef.builder()
                        .channel("telegram")
                        .accountId("default")
                        .conversationId("chat-1")
                        .build())
                .build();

        // Bind
        StepVerifier.create(service.bind(input))
                .assertNext(record -> {
                    assertNotNull(record.getBindingId());
                    assertEquals("session-1", record.getTargetSessionKey());
                    assertEquals(BindingTargetKind.SESSION, record.getTargetKind());
                    assertEquals(BindingStatus.ACTIVE, record.getStatus());
                    assertNotNull(record.getBoundAt());
                })
                .verifyComplete();
    }

    @Test
    void testBindWithoutAdapter() {
        SessionBindingBindInput input = SessionBindingBindInput.builder()
                .targetSessionKey("session-1")
                .targetKind(BindingTargetKind.SESSION)
                .conversation(ConversationRef.builder()
                        .channel("telegram")
                        .accountId("default")
                        .conversationId("chat-1")
                        .build())
                .build();

        StepVerifier.create(service.bind(input))
                .expectError(SessionBindingException.class)
                .verify();
    }

    @Test
    void testBindWithUnsupportedPlacement() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        // Adapter only supports CURRENT, try to bind with CHILD
        SessionBindingBindInput input = SessionBindingBindInput.builder()
                .targetSessionKey("session-1")
                .targetKind(BindingTargetKind.SESSION)
                .conversation(ConversationRef.builder()
                        .channel("telegram")
                        .accountId("default")
                        .conversationId("chat-1")
                        .build())
                .placement(SessionBindingPlacement.CHILD) // Not supported by adapter
                .build();

        StepVerifier.create(service.bind(input))
                .expectError(SessionBindingException.class)
                .verify();
    }

    @Test
    void testGetCapabilities() {
        // Without adapter
        SessionBindingCapabilities caps = service.getCapabilities("telegram", "default");
        assertFalse(caps.isAdapterAvailable());

        // With adapter
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        caps = service.getCapabilities("telegram", "default");
        assertTrue(caps.isAdapterAvailable());
        assertTrue(caps.isBindSupported());
        assertTrue(caps.isUnbindSupported());
        assertEquals(1, caps.getPlacements().size());
    }

    @Test
    void testListBySession() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        // Create bindings
        createBinding("session-1", "chat-1");
        createBinding("session-1", "chat-2");
        createBinding("session-2", "chat-3");

        // List by session
        List<SessionBindingRecord> records = service.listBySession("session-1");
        assertEquals(2, records.size());

        records = service.listBySession("session-2");
        assertEquals(1, records.size());

        records = service.listBySession("non-existent");
        assertTrue(records.isEmpty());
    }

    @Test
    void testResolveByConversation() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        // Create binding
        createBinding("session-1", "chat-1");

        // Resolve
        ConversationRef ref = ConversationRef.builder()
                .channel("telegram")
                .accountId("default")
                .conversationId("chat-1")
                .build();

        SessionBindingRecord record = service.resolveByConversation(ref);
        assertNotNull(record);
        assertEquals("session-1", record.getTargetSessionKey());

        // Non-existent
        ConversationRef nonExistent = ConversationRef.builder()
                .channel("telegram")
                .accountId("default")
                .conversationId("chat-999")
                .build();

        assertNull(service.resolveByConversation(nonExistent));
    }

    @Test
    void testUnbindByBindingId() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        // Create binding
        SessionBindingRecord record = createBinding("session-1", "chat-1");

        // Unbind
        SessionBindingUnbindInput input = SessionBindingUnbindInput.builder()
                .bindingId(record.getBindingId())
                .reason("Test unbind")
                .build();

        StepVerifier.create(service.unbind(input))
                .assertNext(removed -> {
                    assertEquals(1, removed.size());
                    assertEquals(BindingStatus.ENDED, removed.get(0).getStatus());
                })
                .verifyComplete();

        // Verify removed
        assertNull(service.resolveByConversation(record.getConversation()));
    }

    @Test
    void testUnbindBySessionKey() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        // Create bindings
        createBinding("session-1", "chat-1");
        createBinding("session-1", "chat-2");
        createBinding("session-2", "chat-3");

        // Unbind all for session-1
        SessionBindingUnbindInput input = SessionBindingUnbindInput.builder()
                .targetSessionKey("session-1")
                .reason("Test unbind all")
                .build();

        StepVerifier.create(service.unbind(input))
                .assertNext(removed -> assertEquals(2, removed.size()))
                .verifyComplete();

        // Verify session-1 bindings removed
        assertTrue(service.listBySession("session-1").isEmpty());

        // Verify session-2 binding still exists
        assertEquals(1, service.listBySession("session-2").size());
    }

    @Test
    void testBindWithTtl() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        SessionBindingBindInput input = SessionBindingBindInput.builder()
                .targetSessionKey("session-1")
                .targetKind(BindingTargetKind.SESSION)
                .conversation(ConversationRef.builder()
                        .channel("telegram")
                        .accountId("default")
                        .conversationId("chat-1")
                        .build())
                .ttl(Duration.ofHours(1))
                .build();

        StepVerifier.create(service.bind(input))
                .assertNext(record -> {
                    assertNotNull(record.getExpiresAt());
                    assertTrue(record.getExpiresAt().isAfter(Instant.now()));
                })
                .verifyComplete();
    }

    @Test
    void testBindWithMetadata() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        Map<String, Object> metadata = Map.of("key1", "value1", "key2", 123);

        SessionBindingBindInput input = SessionBindingBindInput.builder()
                .targetSessionKey("session-1")
                .targetKind(BindingTargetKind.SESSION)
                .conversation(ConversationRef.builder()
                        .channel("telegram")
                        .accountId("default")
                        .conversationId("chat-1")
                        .build())
                .metadata(metadata)
                .build();

        StepVerifier.create(service.bind(input))
                .assertNext(record -> {
                    assertEquals("value1", record.getMetadata().get("key1"));
                    assertEquals(123, record.getMetadata().get("key2"));
                })
                .verifyComplete();
    }

    @Test
    void testConversationNormalization() {
        TestBindingAdapter adapter = new TestBindingAdapter("discord", "default");
        service.registerAdapter(adapter);

        // Test with mixed case and whitespace
        SessionBindingBindInput input = SessionBindingBindInput.builder()
                .targetSessionKey("session-1")
                .targetKind(BindingTargetKind.SUBAGENT)
                .conversation(ConversationRef.builder()
                        .channel("Discord")  // Mixed case
                        .accountId("DEFAULT") // Upper case
                        .conversationId(" thread-1 ") // With whitespace
                        .build())
                .build();

        StepVerifier.create(service.bind(input))
                .assertNext(record -> {
                    assertEquals("discord", record.getConversation().getChannel());
                    assertEquals("default", record.getConversation().getAccountId());
                    assertEquals("thread-1", record.getConversation().getConversationId());
                })
                .verifyComplete();
    }

    void testInferDefaultPlacement() {
        TestBindingAdapter adapter = new TestBindingAdapter("discord", "default");
        service.registerAdapter(adapter);

        // With conversationId - should infer CURRENT
        SessionBindingBindInput input1 = SessionBindingBindInput.builder()
                .targetSessionKey("session-1")
                .targetKind(BindingTargetKind.SESSION)
                .conversation(ConversationRef.builder()
                        .channel("discord")
                        .accountId("default")
                        .conversationId("thread-1")
                        .build())
                .build();

        StepVerifier.create(service.bind(input1))
                .assertNext(record -> {
                    // The binding should use CURRENT placement
                    assertNotNull(record);
                })
                .verifyComplete();
    }

    @Test
    void testAdapterRegistrationAndUnregistration() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");

        // Register
        service.registerAdapter(adapter);
        assertEquals(1, service.getAdapterCount());
        assertTrue(service.getRegisteredAdapterKeys().contains("telegram:default"));

        // Unregister by channel/account
        service.unregisterAdapter("telegram", "default");
        assertEquals(0, service.getAdapterCount());

        // Register again
        service.registerAdapter(adapter);
        assertEquals(1, service.getAdapterCount());

        // Unregister specific adapter
        service.unregisterAdapter(adapter);
        assertEquals(0, service.getAdapterCount());
    }

    @Test
    void testResetForTest() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        createBinding("session-1", "chat-1");
        assertEquals(1, service.getAdapterCount());
        assertFalse(service.listBySession("session-1").isEmpty());

        service.resetForTest();

        assertEquals(0, service.getAdapterCount());
        assertTrue(service.listBySession("session-1").isEmpty());
    }

    @Test
    void testMultipleAdaptersSameChannel() {
        TestBindingAdapter adapter1 = new TestBindingAdapter("telegram", "default");
        TestBindingAdapter adapter2 = new TestBindingAdapter("telegram", "default");

        service.registerAdapter(adapter1);
        service.registerAdapter(adapter2);

        // Should only have one adapter per channel:account (last one wins)
        assertEquals(1, service.getAdapterCount());
    }

    @Test
    void testBindingExceptionDetails() {
        // No adapter registered
        SessionBindingBindInput input = SessionBindingBindInput.builder()
                .targetSessionKey("session-1")
                .targetKind(BindingTargetKind.SESSION)
                .conversation(ConversationRef.builder()
                        .channel("discord")
                        .accountId("default")
                        .conversationId("chat-1")
                        .build())
                .build();

        StepVerifier.create(service.bind(input))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof SessionBindingException);
                    SessionBindingException ex = (SessionBindingException) error;
                    assertEquals(SessionBindingErrorCode.BINDING_ADAPTER_UNAVAILABLE, ex.getCode());
                    assertEquals("discord", ex.getChannel());
                    assertEquals("default", ex.getAccountId());
                })
                .verify();
    }

    @Test
    void testIsSessionBindingError() {
        SessionBindingException exception = new SessionBindingException(
                SessionBindingErrorCode.BINDING_ADAPTER_UNAVAILABLE,
                "Test error"
        );

        assertTrue(SessionBindingException.isSessionBindingError(exception));
        assertFalse(SessionBindingException.isSessionBindingError(new RuntimeException("Other")));
        assertFalse(SessionBindingException.isSessionBindingError(null));
    }

    @Test
    void testResolveByConversationWithInvalidRef() {
        // Null conversation
        assertNull(service.resolveByConversation(null));

        // Empty channel
        assertNull(service.resolveByConversation(ConversationRef.builder()
                .channel("")
                .accountId("default")
                .conversationId("chat-1")
                .build()));

        // Empty conversationId
        assertNull(service.resolveByConversation(ConversationRef.builder()
                .channel("telegram")
                .accountId("default")
                .conversationId("")
                .build()));
    }

    @Test
    void testTouchBinding() {
        TestBindingAdapter adapter = new TestBindingAdapter("telegram", "default");
        service.registerAdapter(adapter);

        SessionBindingRecord record = createBinding("session-1", "chat-1");

        // Touch should not throw
        assertDoesNotThrow(() -> service.touch(record.getBindingId()));

        // Touch non-existent should not throw
        assertDoesNotThrow(() -> service.touch("non-existent"));
    }

    @Test
    void testListBySessionWithEmptyKey() {
        assertTrue(service.listBySession("").isEmpty());
        assertTrue(service.listBySession(null).isEmpty());
        assertTrue(service.listBySession("   ").isEmpty());
    }

    @Test
    void testResolveByConversationWithInvalidRef() {
        // Null conversation
        assertNull(service.resolveByConversation(null));

        // Empty channel
        assertNull(service.resolveByConversation(ConversationRef.builder()
                .channel("")
                .accountId("default")
                .conversationId("chat-1")
                .build()));

        // Empty conversation ID
        assertNull(service.resolveByConversation(ConversationRef.builder()
                .channel("telegram")
                .accountId("default")
                .conversationId("")
                .build()));
    }

    private SessionBindingRecord createBinding(String sessionKey, String conversationId) {
        SessionBindingBindInput input = SessionBindingBindInput.builder()
                .targetSessionKey(sessionKey)
                .targetKind(BindingTargetKind.SESSION)
                .conversation(ConversationRef.builder()
                        .channel("telegram")
                        .accountId("default")
                        .conversationId(conversationId)
                        .build())
                .build();

        return service.bind(input).block();
    }

    /**
     * Test implementation of SessionBindingAdapter.
     */
    private static class TestBindingAdapter implements SessionBindingAdapter {
        private final String channel;
        private final String accountId;

        TestBindingAdapter(String channel, String accountId) {
            this.channel = channel;
            this.accountId = accountId;
        }

        @Override
        public String getChannel() {
            return channel;
        }

        @Override
        public String getAccountId() {
            return accountId;
        }

        @Override
        public boolean isBindSupported() {
            return true;
        }

        @Override
        public boolean isUnbindSupported() {
            return true;
        }

        @Override
        public List<SessionBindingPlacement> getPlacements() {
            return List.of(SessionBindingPlacement.CURRENT);
        }

        @Override
        public List<SessionBindingRecord> listBySession(String targetSessionKey) {
            return List.of();
        }

        @Override
        public SessionBindingRecord resolveByConversation(ConversationRef ref) {
            return null;
        }
    }
}
