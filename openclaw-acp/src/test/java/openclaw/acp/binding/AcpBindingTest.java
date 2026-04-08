package openclaw.acp.binding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AcpBinding.
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
class AcpBindingTest {

    @Test
    void testDefaultConstructor() {
        AcpBinding binding = new AcpBinding();

        assertNotNull(binding.getBindingId());
        assertEquals(AcpBindingManager.BindingState.ACTIVE, binding.getState());
        assertNotNull(binding.getCreatedAt());
        assertNotNull(binding.getUpdatedAt());
        assertEquals(0, binding.getResetCount());
        assertEquals(0, binding.getRecoveryCount());
        assertTrue(binding.canReset());
        assertFalse(binding.canRecover());
    }

    @Test
    void testParameterizedConstructor() {
        AcpBinding binding = new AcpBinding("agent-1", "channel-1", "discord");

        assertEquals("agent-1", binding.getAgentId());
        assertEquals("channel-1", binding.getChannelId());
        assertEquals("discord", binding.getChannelType());
        assertNotNull(binding.getBindingId());
    }

    @Test
    void testStateTransitions() {
        AcpBinding binding = new AcpBinding();

        // Initially active
        assertEquals(AcpBindingManager.BindingState.ACTIVE, binding.getState());
        assertTrue(binding.canReset());
        assertFalse(binding.canRecover());

        // Set to failed
        binding.setState(AcpBindingManager.BindingState.FAILED);
        assertEquals(AcpBindingManager.BindingState.FAILED, binding.getState());
        assertTrue(binding.canReset());
        assertTrue(binding.canRecover());

        // Set to inactive
        binding.setState(AcpBindingManager.BindingState.INACTIVE);
        assertTrue(binding.canReset());
        assertTrue(binding.canRecover());
    }

    @Test
    void testResetCount() {
        AcpBinding binding = new AcpBinding();

        assertEquals(0, binding.getResetCount());

        binding.incrementResetCount();
        assertEquals(1, binding.getResetCount());

        binding.incrementResetCount();
        assertEquals(2, binding.getResetCount());
    }

    @Test
    void testRecoveryCount() {
        AcpBinding binding = new AcpBinding();

        assertEquals(0, binding.getRecoveryCount());

        binding.incrementRecoveryCount();
        assertEquals(1, binding.getRecoveryCount());
    }

    @Test
    void testResetReason() {
        AcpBinding binding = new AcpBinding();

        assertTrue(binding.getResetReason().isEmpty());

        binding.setResetReason("Test reset");
        assertTrue(binding.getResetReason().isPresent());
        assertEquals("Test reset", binding.getResetReason().get());

        binding.setResetReason(null);
        assertTrue(binding.getResetReason().isEmpty());
    }

    @Test
    void testUpdateActivity() {
        AcpBinding binding = new AcpBinding();

        var lastActivity = binding.getLastActivityAt();

        // Wait a tiny bit
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        binding.updateActivity();

        assertTrue(binding.getLastActivityAt().isAfter(lastActivity));
    }

    @Test
    void testToString() {
        AcpBinding binding = new AcpBinding("agent-1", "channel-1", "discord");
        String str = binding.toString();

        assertTrue(str.contains("AcpBinding"));
        assertTrue(str.contains(binding.getBindingId()));
        assertTrue(str.contains("agent-1"));
        assertTrue(str.contains("channel-1"));
    }
}
