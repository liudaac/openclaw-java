package openclaw.tasks.delivery;

import openclaw.tasks.model.DeliveryContext;
import openclaw.tasks.model.FlowRecord;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for delivering flow updates to requester origins.
 * This is a placeholder implementation that would integrate with the messaging system.
 */
@Service
public class FlowDeliveryService {

    /**
     * Check if the flow can be delivered to its requester origin.
     */
    public boolean canDeliverToRequesterOrigin(FlowRecord flow) {
        DeliveryContext origin = flow.getRequesterOrigin();
        if (origin == null) {
            return false;
        }
        String channel = origin.getChannel();
        String to = origin.getTo();
        return channel != null && !channel.isBlank() &&
               to != null && !to.isBlank() &&
               isDeliverableChannel(channel);
    }

    /**
     * Send a message to the requester origin.
     * This is a placeholder that would integrate with the actual messaging system.
     */
    public DeliveryResult sendMessage(SendMessageRequest request) {
        // TODO: Integrate with actual messaging system (Discord, Telegram, etc.)
        // For now, return success as a placeholder
        return new DeliveryResult(true, null);
    }

    /**
     * Enqueue a system event for later delivery.
     */
    public void enqueueSystemEvent(String content, SystemEventContext context) {
        // TODO: Integrate with system event queue
        // This would enqueue the event for background processing
    }

    /**
     * Request an immediate heartbeat for the session.
     */
    public void requestHeartbeatNow(String reason, String sessionKey) {
        // TODO: Integrate with heartbeat system
        // This would trigger an immediate heartbeat for the session
    }

    private boolean isDeliverableChannel(String channel) {
        // List of channels that support direct message delivery
        return switch (channel.toLowerCase()) {
            case "discord", "telegram", "slack", "feishu", "matrix", "wecom" -> true;
            default -> false;
        };
    }

    /**
     * Parse agent ID from session key.
     * Format: agentId:sessionId or similar
     */
    public Optional<String> parseAgentIdFromSessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return Optional.empty();
        }
        // Simple parsing - extract first part before colon
        int colonIndex = sessionKey.indexOf(':');
        if (colonIndex > 0) {
            return Optional.of(sessionKey.substring(0, colonIndex));
        }
        return Optional.of(sessionKey);
    }

    // Record types

    public record SendMessageRequest(
        String channel,
        String to,
        String accountId,
        String threadId,
        String content,
        String agentId,
        String idempotencyKey,
        MirrorContext mirror
    ) {}

    public record MirrorContext(
        String sessionKey,
        String agentId,
        String idempotencyKey
    ) {}

    public record SystemEventContext(
        String sessionKey,
        String contextKey,
        DeliveryContext deliveryContext
    ) {}

    public record DeliveryResult(
        boolean success,
        String error
    ) {}
}
