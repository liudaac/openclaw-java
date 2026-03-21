package openclaw.agent.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HeartbeatEventEmitter}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class HeartbeatEventEmitterTest {

    private HeartbeatEventEmitter emitter;

    @BeforeEach
    void setUp() {
        emitter = new HeartbeatEventEmitter();
    }

    @Test
    void testEmitEvent() {
        List<HeartbeatEventPayload> received = new ArrayList<>();
        emitter.subscribe(received::add);
        
        emitter.emit(HeartbeatStatus.SENT, "user-1", "telegram");
        
        assertEquals(1, received.size());
        assertEquals(HeartbeatStatus.SENT, received.get(0).getStatus());
        assertEquals("user-1", received.get(0).getTo());
        assertEquals("telegram", received.get(0).getChannel());
        assertNotNull(received.get(0).getTimestamp());
    }

    @Test
    void testEmitWithFullPayload() {
        List<HeartbeatEventPayload> received = new ArrayList<>();
        emitter.subscribe(received::add);
        
        HeartbeatEventPayload payload = HeartbeatEventPayload.builder()
                .status(HeartbeatStatus.OK_TOKEN)
                .to("user-1")
                .accountId("account-1")
                .preview("Hello world")
                .durationMs(100L)
                .hasMedia(false)
                .channel("telegram")
                .silent(false)
                .build();
        
        emitter.emit(payload);
        
        assertEquals(1, received.size());
        assertEquals(HeartbeatStatus.OK_TOKEN, received.get(0).getStatus());
        assertEquals("account-1", received.get(0).getAccountId());
        assertEquals("Hello world", received.get(0).getPreview());
        assertEquals(100L, received.get(0).getDurationMs());
    }

    @Test
    void testLastHeartbeatIsStored() {
        emitter.emit(HeartbeatStatus.SENT, "user-1", "telegram");
        
        HeartbeatEventPayload last = emitter.getLastHeartbeat();
        assertNotNull(last);
        assertEquals(HeartbeatStatus.SENT, last.getStatus());
        
        emitter.emit(HeartbeatStatus.OK_EMPTY, "user-2", "discord");
        
        last = emitter.getLastHeartbeat();
        assertEquals(HeartbeatStatus.OK_EMPTY, last.getStatus());
        assertEquals("user-2", last.getTo());
    }

    @Test
    void testIndicatorTypeAutoResolved() {
        List<HeartbeatEventPayload> received = new ArrayList<>();
        emitter.subscribe(received::add);
        
        // Test OK status
        emitter.emit(HeartbeatStatus.OK_EMPTY, null, null);
        assertEquals(HeartbeatIndicatorType.OK, received.get(0).getIndicatorType());
        
        // Test ALERT status
        emitter.emit(HeartbeatStatus.SENT, null, null);
        assertEquals(HeartbeatIndicatorType.ALERT, received.get(1).getIndicatorType());
        
        // Test ERROR status
        emitter.emit(HeartbeatStatus.FAILED, null, null);
        assertEquals(HeartbeatIndicatorType.ERROR, received.get(2).getIndicatorType());
        
        // Test SKIPPED status
        emitter.emit(HeartbeatStatus.SKIPPED, null, null);
        assertNull(received.get(3).getIndicatorType());
    }

    @Test
    void testUnsubscribe() {
        List<HeartbeatEventPayload> received = new ArrayList<>();
        
        Runnable unsubscribe = emitter.subscribe(received::add);
        
        emitter.emit(HeartbeatStatus.SENT, null, null);
        assertEquals(1, received.size());
        
        unsubscribe.run();
        
        emitter.emit(HeartbeatStatus.OK_TOKEN, null, null);
        assertEquals(1, received.size());
    }

    @Test
    void testResetForTest() {
        emitter.emit(HeartbeatStatus.SENT, "user-1", "telegram");
        assertNotNull(emitter.getLastHeartbeat());
        
        emitter.resetForTest();
        
        assertNull(emitter.getLastHeartbeat());
    }
}
