package openclaw.cron.delivery;

import openclaw.cron.model.CronJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cron delivery service with persistence and stale delivery handling.
 *
 * <p>Ported from original: a290f5e50f - persist outbound sends and skip stale cron deliveries</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.19
 */
@Service
public class CronDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(CronDeliveryService.class);
    
    // Stale delivery threshold: 1 hour
    private static final Duration STALE_THRESHOLD = Duration.ofHours(1);
    
    // In-memory delivery queue (should be backed by database in production)
    private final ConcurrentHashMap<String, DeliveryRecord> deliveryQueue = new ConcurrentHashMap<>();

    /**
     * Persists an outbound send for a cron job.
     *
     * @param job the cron job
     * @param message the message to deliver
     * @return the delivery record
     */
    public DeliveryRecord persistOutboundSend(CronJob job, OutboundMessage message) {
        String deliveryId = UUID.randomUUID().toString();
        
        DeliveryRecord record = new DeliveryRecord(
                deliveryId,
                job.getId(),
                message,
                DeliveryStatus.PENDING,
                Instant.now(),
                null,
                null
        );
        
        deliveryQueue.put(deliveryId, record);
        logger.info("Persisted outbound send for job {}: deliveryId={}", job.getId(), deliveryId);
        
        return record;
    }
    
    /**
     * Checks if a delivery should be skipped because it's stale.
     *
     * @param job the cron job
     * @return true if the delivery is stale and should be skipped
     */
    public boolean shouldSkipStaleDelivery(CronJob job) {
        if (job.getScheduledTime() == null) {
            return false;
        }
        
        Duration delay = Duration.between(job.getScheduledTime(), Instant.now());
        boolean isStale = delay.compareTo(STALE_THRESHOLD) > 0;
        
        if (isStale) {
            logger.warn("Skipping stale delivery for job {}: scheduled={}, delay={} hours",
                    job.getId(), job.getScheduledTime(), delay.toHours());
        }
        
        return isStale;
    }
    
    /**
     * Marks a delivery as completed.
     *
     * @param deliveryId the delivery ID
     * @return true if the delivery was found and updated
     */
    public boolean markDelivered(String deliveryId) {
        DeliveryRecord record = deliveryQueue.get(deliveryId);
        if (record == null) {
            return false;
        }
        
        record = new DeliveryRecord(
                record.deliveryId(),
                record.jobId(),
                record.message(),
                DeliveryStatus.DELIVERED,
                record.createdAt(),
                Instant.now(),
                null
        );
        
        deliveryQueue.put(deliveryId, record);
        logger.info("Marked delivery {} as delivered", deliveryId);
        return true;
    }
    
    /**
     * Marks a delivery as failed.
     *
     * @param deliveryId the delivery ID
     * @param error the error message
     * @return true if the delivery was found and updated
     */
    public boolean markFailed(String deliveryId, String error) {
        DeliveryRecord record = deliveryQueue.get(deliveryId);
        if (record == null) {
            return false;
        }
        
        record = new DeliveryRecord(
                record.deliveryId(),
                record.jobId(),
                record.message(),
                DeliveryStatus.FAILED,
                record.createdAt(),
                null,
                error
        );
        
        deliveryQueue.put(deliveryId, record);
        logger.error("Marked delivery {} as failed: {}", deliveryId, error);
        return true;
    }
    
    /**
     * Gets a delivery record by ID.
     *
     * @param deliveryId the delivery ID
     * @return the delivery record, or null if not found
     */
    public DeliveryRecord getDelivery(String deliveryId) {
        return deliveryQueue.get(deliveryId);
    }
    
    /**
     * Cleans up old delivered/failed records.
     *
     * @param maxAge the maximum age of records to keep
     */
    public void cleanupOldRecords(Duration maxAge) {
        Instant cutoff = Instant.now().minus(maxAge);
        deliveryQueue.entrySet().removeIf(entry -> {
            DeliveryRecord record = entry.getValue();
            boolean shouldRemove = (record.status() == DeliveryStatus.DELIVERED || 
                                   record.status() == DeliveryStatus.FAILED) &&
                                  record.createdAt().isBefore(cutoff);
            if (shouldRemove) {
                logger.debug("Cleaned up old delivery record: {}", entry.getKey());
            }
            return shouldRemove;
        });
    }

    /**
     * Delivery record.
     */
    public record DeliveryRecord(
            String deliveryId,
            String jobId,
            OutboundMessage message,
            DeliveryStatus status,
            Instant createdAt,
            Instant deliveredAt,
            String error
    ) {
    }
    
    /**
     * Outbound message.
     */
    public record OutboundMessage(
            String channel,
            String target,
            String content,
            String contentType
    ) {
    }
    
    /**
     * Delivery status.
     */
    public enum DeliveryStatus {
        PENDING,
        DELIVERED,
        FAILED,
        SKIPPED_STALE
    }
}
