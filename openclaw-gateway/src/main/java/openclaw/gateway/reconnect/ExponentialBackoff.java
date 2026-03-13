package openclaw.gateway.reconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Exponential backoff strategy for reconnection.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class ExponentialBackoff {
    
    private static final Logger logger = LoggerFactory.getLogger(ExponentialBackoff.class);
    
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double multiplier;
    private final double jitterFactor;
    
    private long currentDelayMs;
    private int attemptCount;
    
    public ExponentialBackoff() {
        this(1000, 30000, 2.0, 0.1);
    }
    
    public ExponentialBackoff(long initialDelayMs, long maxDelayMs,
                              double multiplier, double jitterFactor) {
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
        this.jitterFactor = jitterFactor;
        this.currentDelayMs = initialDelayMs;
        this.attemptCount = 0;
    }
    
    /**
     * Get next delay with jitter.
     */
    public Duration nextDelay() {
        attemptCount++;
        
        // Calculate delay with exponential backoff
        long delay = Math.min(currentDelayMs, maxDelayMs);
        
        // Add jitter (±jitterFactor)
        double jitter = 1.0 + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * jitterFactor;
        long jitteredDelay = (long) (delay * jitter);
        
        // Increase for next time
        currentDelayMs = (long) (currentDelayMs * multiplier);
        if (currentDelayMs > maxDelayMs) {
            currentDelayMs = maxDelayMs;
        }
        
        logger.debug("Backoff attempt {}: delay {}ms (with jitter: {}ms)",
            attemptCount, delay, jitteredDelay);
        
        return Duration.ofMillis(jitteredDelay);
    }
    
    /**
     * Get next delay without advancing.
     */
    public Duration peekNextDelay() {
        long delay = Math.min(currentDelayMs, maxDelayMs);
        return Duration.ofMillis(delay);
    }
    
    /**
     * Reset to initial state.
     */
    public void reset() {
        currentDelayMs = initialDelayMs;
        attemptCount = 0;
        logger.debug("Backoff reset");
    }
    
    /**
     * Check if max delay reached.
     */
    public boolean isMaxDelayReached() {
        return currentDelayMs >= maxDelayMs;
    }
    
    /**
     * Get current attempt count.
     */
    public int getAttemptCount() {
        return attemptCount;
    }
    
    /**
     * Get current delay.
     */
    public Duration getCurrentDelay() {
        return Duration.ofMillis(currentDelayMs);
    }
    
    // Getters
    public long getInitialDelayMs() { return initialDelayMs; }
    public long getMaxDelayMs() { return maxDelayMs; }
    public double getMultiplier() { return multiplier; }
    public double getJitterFactor() { return jitterFactor; }
}
