package openclaw.tasks.delivery;

import openclaw.tasks.model.DeliveryContext;
import openclaw.tasks.model.FlowRecord;
import openclaw.tasks.model.FlowStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Flow Delivery Service Implementation.
 * Manages flow updates and message delivery.
 *
 * @author OpenClaw Team
 * @version 2026.4.2
 */
@Service
public class FlowDeliveryServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(FlowDeliveryServiceImpl.class);

    // System event queue for background processing
    private final BlockingQueue<SystemEvent> eventQueue = new LinkedBlockingQueue<>();

    // Active flow tracking
    private final Map<String, FlowRecord> activeFlows = new ConcurrentHashMap<>();

    // Message delivery handler (can be replaced with actual implementation)
    private MessageDeliveryHandler deliveryHandler;

    public FlowDeliveryServiceImpl() {
        // Default no-op handler
        this.deliveryHandler = new NoOpMessageDeliveryHandler();
    }

    /**
     * Set custom message delivery handler.
     */
    public void setDeliveryHandler(MessageDeliveryHandler handler) {
        this.deliveryHandler = handler;
    }

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
     */
    public DeliveryResult sendMessage(SendMessageRequest request) {
        try {
            logger.info("Sending message via {} to {}", request.channel(), request.to());
            logger.debug("Message content: {}", request.content());

            // Use delivery handler to send message
            boolean sent = deliveryHandler.deliver(request);
            
            if (sent) {
                return new DeliveryResult(true, null);
            } else {
                return new DeliveryResult(false, "Delivery handler failed");
            }
            
        } catch (Exception e) {
            logger.error("Failed to send message", e);
            return new DeliveryResult(false, e.getMessage());
        }
    }

    /**
     * Send a flow status update.
     */
    public DeliveryResult sendFlowUpdate(String flowId, FlowStatus status, String message) {
        FlowRecord flow = activeFlows.get(flowId);
        if (flow == null) {
            logger.warn("Flow not found: {}", flowId);
            return new DeliveryResult(false, "Flow not found: " + flowId);
        }

        if (!canDeliverToRequesterOrigin(flow)) {
            logger.debug("Flow {} cannot be delivered to origin", flowId);
            return new DeliveryResult(false, "Cannot deliver to origin");
        }

        DeliveryContext origin = flow.getRequesterOrigin();
        String content = buildFlowUpdateMessage(flow, status, message);

        SendMessageRequest request = new SendMessageRequest(
            origin.getChannel(),
            origin.getTo(),
            origin.getAccountId(),
            origin.getThreadId(),
            content,
            flow.getOwnerSessionKey(),
            null,  // idempotencyKey
            new MirrorContext(flow.getOwnerSessionKey(), null, null)
        );

        return sendMessage(request);
    }

    /**
     * Enqueue a system event for later delivery.
     */
    public void enqueueSystemEvent(String content, SystemEventContext context) {
        SystemEvent event = new SystemEvent(content, context, Instant.now());
        boolean enqueued = eventQueue.offer(event);
        if (enqueued) {
            logger.debug("Enqueued system event: {}", content);
        } else {
            logger.warn("Failed to enqueue system event (queue full)");
        }
    }

    /**
     * Process pending system events.
     * Should be called periodically by a scheduler.
     */
    public void processPendingEvents() {
        SystemEvent event;
        while ((event = eventQueue.poll()) != null) {
            try {
                processEvent(event);
            } catch (Exception e) {
                logger.error("Failed to process event: {}", event.content(), e);
            }
        }
    }

    /**
     * Request an immediate heartbeat for the session.
     */
    public void requestHeartbeatNow(String reason, String sessionKey) {
        logger.info("Requesting immediate heartbeat for session {}: {}", sessionKey, reason);
        
        // Enqueue heartbeat request as system event
        SystemEventContext context = new SystemEventContext(
            sessionKey,
            "heartbeat_request",
            null
        );
        
        enqueueSystemEvent("HEARTBEAT_REQUEST: " + reason, context);
    }

    /**
     * Register a flow for tracking.
     */
    public void registerFlow(FlowRecord flow) {
        activeFlows.put(flow.getFlowId(), flow);
        logger.debug("Registered flow: {}", flow.getFlowId());
    }

    /**
     * Update flow status.
     */
    public void updateFlowStatus(String flowId, FlowStatus status, String message) {
        FlowRecord flow = activeFlows.get(flowId);
        if (flow != null) {
            flow.setStatus(status);
            flow.setCurrentStep(message);
            flow.setUpdatedAt(Instant.now());
            
            // Send update to origin
            sendFlowUpdate(flowId, status, message);
        }
    }

    /**
     * Complete a flow.
     */
    public void completeFlow(String flowId, String result) {
        FlowRecord flow = activeFlows.remove(flowId);
        if (flow != null) {
            flow.setStatus(FlowStatus.SUCCEEDED);
            flow.setOutputs(Map.of("result", result));
            flow.setUpdatedAt(Instant.now());
            flow.setEndedAt(Instant.now());
            
            sendFlowUpdate(flowId, FlowStatus.SUCCEEDED, "Flow completed");
            logger.info("Flow completed: {}", flowId);
        }
    }

    /**
     * Fail a flow.
     */
    public void failFlow(String flowId, String error) {
        FlowRecord flow = activeFlows.remove(flowId);
        if (flow != null) {
            flow.setStatus(FlowStatus.FAILED);
            flow.setOutputs(Map.of("error", error));
            flow.setUpdatedAt(Instant.now());
            flow.setEndedAt(Instant.now());
            
            sendFlowUpdate(flowId, FlowStatus.FAILED, "Flow failed: " + error);
            logger.error("Flow failed: {} - {}", flowId, error);
        }
    }

    /**
     * Get active flow count.
     */
    public int getActiveFlowCount() {
        return activeFlows.size();
    }

    /**
     * Clean up stale flows.
     */
    public void cleanupStaleFlows(long maxAgeSeconds) {
        Instant cutoff = Instant.now().minusSeconds(maxAgeSeconds);
        activeFlows.entrySet().removeIf(entry -> {
            FlowRecord flow = entry.getValue();
            boolean stale = flow.getUpdatedAt() != null && flow.getUpdatedAt().isBefore(cutoff);
            if (stale) {
                logger.debug("Removing stale flow: {}", entry.getKey());
            }
            return stale;
        });
    }

    // Helper methods

    private boolean isDeliverableChannel(String channel) {
        return switch (channel.toLowerCase()) {
            case "discord", "telegram", "slack", "feishu", "matrix", "wecom" -> true;
            default -> false;
        };
    }

    private String buildFlowUpdateMessage(FlowRecord flow, FlowStatus status, String message) {
        String emoji = switch (status) {
            case QUEUED -> "⏳";
            case RUNNING -> "▶️";
            case WAITING -> "⏸️";
            case BLOCKED -> "🚫";
            case SUCCEEDED -> "✅";
            case FAILED -> "❌";
            case CANCELLED -> "🛑";
            case LOST -> "⚠️";
        };
        
        String flowName = flow.getGoal() != null ? flow.getGoal() : flow.getFlowId();
        return String.format("%s **Flow Update**\n%s: %s", emoji, flowName, message);
    }

    private void processEvent(SystemEvent event) {
        logger.debug("Processing event: {}", event.content());
        // Process based on event type
        if (event.content().startsWith("HEARTBEAT_REQUEST:")) {
            // Trigger heartbeat
            logger.info("Triggering heartbeat for session: {}", event.context().sessionKey());
        }
    }

    // Inner classes

    private record SystemEvent(String content, SystemEventContext context, Instant timestamp) {}

    /**
     * Message delivery handler interface.
     */
    public interface MessageDeliveryHandler {
        boolean deliver(SendMessageRequest request);
    }

    /**
     * No-op delivery handler (default).
     */
    private static class NoOpMessageDeliveryHandler implements MessageDeliveryHandler {
        @Override
        public boolean deliver(SendMessageRequest request) {
            logger.debug("No-op delivery for message to {}", request.to());
            return true;
        }
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
