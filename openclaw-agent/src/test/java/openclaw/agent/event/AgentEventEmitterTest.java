package openclaw.agent.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AgentEventEmitter}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class AgentEventEmitterTest {

    private AgentEventEmitter emitter;

    @BeforeEach
    void setUp() {
        emitter = new AgentEventEmitter();
    }

    @Test
    void testEmitEvent() {
        List<AgentEventPayload> received = new ArrayList<>();
        
        // Subscribe
        emitter.subscribe(received::add);
        
        // Emit event
        emitter.emit("run-1", AgentEventStream.LIFECYCLE, Map.of("type", "start"));
        
        // Verify
        assertEquals(1, received.size());
        assertEquals("run-1", received.get(0).getRunId());
        assertEquals(AgentEventStream.LIFECYCLE, received.get(0).getStream());
        assertEquals(1, received.get(0).getSeq());
        assertNotNull(received.get(0).getTimestamp());
    }

    @Test
    void testSequenceNumbersIncrement() {
        List<AgentEventPayload> received = new ArrayList<>();
        emitter.subscribe(received::add);
        
        // Emit multiple events
        emitter.emit("run-1", AgentEventStream.LIFECYCLE, Map.of());
        emitter.emit("run-1", AgentEventStream.TOOL, Map.of());
        emitter.emit("run-1", AgentEventStream.ASSISTANT, Map.of());
        
        // Verify sequence numbers
        assertEquals(1, received.get(0).getSeq());
        assertEquals(2, received.get(1).getSeq());
        assertEquals(3, received.get(2).getSeq());
    }

    @Test
    void testMultipleRunsHaveSeparateSequences() {
        List<AgentEventPayload> received = new ArrayList<>();
        emitter.subscribe(received::add);
        
        // Emit events for different runs
        emitter.emit("run-1", AgentEventStream.LIFECYCLE, Map.of());
        emitter.emit("run-2", AgentEventStream.LIFECYCLE, Map.of());
        emitter.emit("run-1", AgentEventStream.TOOL, Map.of());
        
        // Verify sequences are per-run
        assertEquals(1, received.get(0).getSeq()); // run-1 first
        assertEquals(1, received.get(1).getSeq()); // run-2 first
        assertEquals(2, received.get(2).getSeq()); // run-1 second
    }

    @Test
    void testRegisterRunContext() {
        AgentRunContext context = AgentRunContext.builder()
                .sessionKey("session-1")
                .heartbeat(false)
                .controlUiVisible(true)
                .build();
        
        emitter.registerRunContext("run-1", context);
        
        AgentRunContext retrieved = emitter.getRunContext("run-1");
        assertNotNull(retrieved);
        assertEquals("session-1", retrieved.getSessionKey());
    }

    @Test
    void testSessionKeyFromContext() {
        AgentRunContext context = AgentRunContext.builder()
                .sessionKey("session-1")
                .controlUiVisible(true)
                .build();
        
        emitter.registerRunContext("run-1", context);
        
        List<AgentEventPayload> received = new ArrayList<>();
        emitter.subscribe(received::add);
        
        emitter.emit("run-1", AgentEventStream.LIFECYCLE, Map.of());
        
        assertEquals("session-1", received.get(0).getSessionKey());
    }

    @Test
    void testSessionKeyNullWhenControlUiHidden() {
        AgentRunContext context = AgentRunContext.builder()
                .sessionKey("session-1")
                .controlUiVisible(false)
                .build();
        
        emitter.registerRunContext("run-1", context);
        
        List<AgentEventPayload> received = new ArrayList<>();
        emitter.subscribe(received::add);
        
        emitter.emit("run-1", AgentEventStream.LIFECYCLE, Map.of());
        
        assertNull(received.get(0).getSessionKey());
    }

    @Test
    void testExplicitSessionKeyOverridesContext() {
        AgentRunContext context = AgentRunContext.builder()
                .sessionKey("context-session")
                .controlUiVisible(true)
                .build();
        
        emitter.registerRunContext("run-1", context);
        
        List<AgentEventPayload> received = new ArrayList<>();
        emitter.subscribe(received::add);
        
        emitter.emit("run-1", AgentEventStream.LIFECYCLE, Map.of(), "explicit-session");
        
        assertEquals("explicit-session", received.get(0).getSessionKey());
    }

    @Test
    void testUnsubscribe() {
        List<AgentEventPayload> received = new ArrayList<>();
        
        Runnable unsubscribe = emitter.subscribe(received::add);
        
        emitter.emit("run-1", AgentEventStream.LIFECYCLE, Map.of());
        assertEquals(1, received.size());
        
        // Unsubscribe
        unsubscribe.run();
        
        emitter.emit("run-1", AgentEventStream.TOOL, Map.of());
        assertEquals(1, received.size()); // No new events
    }

    @Test
    void testClearRunContext() {
        AgentRunContext context = AgentRunContext.builder()
                .sessionKey("session-1")
                .build();
        
        emitter.registerRunContext("run-1", context);
        assertNotNull(emitter.getRunContext("run-1"));
        
        emitter.clearRunContext("run-1");
        assertNull(emitter.getRunContext("run-1"));
    }

    @Test
    void testListenerExceptionDoesNotBreakOthers() {
        AtomicInteger counter = new AtomicInteger(0);
        
        // Subscribe listener that throws
        emitter.subscribe(evt -> {
            throw new RuntimeException("Test exception");
        });
        
        // Subscribe listener that works
        emitter.subscribe(evt -> counter.incrementAndGet());
        
        // Emit should not throw and second listener should still receive
        assertDoesNotThrow(() -> 
            emitter.emit("run-1", AgentEventStream.LIFECYCLE, Map.of())
        );
        
        assertEquals(1, counter.get());
    }

    @Test
    void testNullRunIdIsIgnored() {
        List<AgentEventPayload> received = new ArrayList<>();
        emitter.subscribe(received::add);
        
        emitter.emit(null, AgentEventStream.LIFECYCLE, Map.of());
        emitter.emit("", AgentEventStream.LIFECYCLE, Map.of());
        
        assertTrue(received.isEmpty());
    }
}
