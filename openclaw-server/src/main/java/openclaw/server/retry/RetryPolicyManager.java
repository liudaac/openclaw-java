package openclaw.server.retry;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.core.IntervalFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Retry Policy Manager
 * 
 * <p>Advanced retry strategies:</p>
 * <ul>
 *   <li>Exponential backoff</li>
 *   <li>Specific error code retry</li>
 *   <li>Circuit breaker integration</li>
 *   <li>Custom retry policies</li>
 * </ul>
 */
@Service
public class RetryPolicyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryPolicyManager.class);
    
    private final RetryRegistry registry;
    private final Map<String, Retry> retryPolicies;
    
    // Default configuration
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Duration DEFAULT_WAIT_DURATION = Duration.ofSeconds(1);
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private static final Duration DEFAULT_MAX_WAIT_DURATION = Duration.ofSeconds(10);
    
    // Retryable exceptions
    private static final Set<Class<? extends Throwable>> RETRYABLE_EXCEPTIONS = Set.of(
        IOException.class,
        TimeoutException.class,
        java.net.ConnectException.class,
        java.net.SocketTimeoutException.class
    );
    
    public RetryPolicyManager() {
        this.registry = RetryRegistry.ofDefaults();
        this.retryPolicies = new ConcurrentHashMap<>();
    }
    
    /**
     * Execute with default retry policy
     * 
     * @param operation operation to execute
     * @param operationName operation name for metrics
     * @param <T> return type
     * @return result
     */
    public <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation,
            String operationName) {
        
        return executeWithRetry(operation, operationName, 
            DEFAULT_MAX_RETRIES, DEFAULT_WAIT_DURATION);
    }
    
    /**
     * Execute with custom retry policy
     * 
     * @param operation operation to execute
     * @param operationName operation name
     * @param maxRetries maximum retries
     * @param waitDuration initial wait duration
     * @param <T> return type
     * @return result
     */
    public <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int maxRetries,
            Duration waitDuration) {
        
        return executeWithRetry(operation, operationName, maxRetries, 
            waitDuration, DEFAULT_MULTIPLIER, DEFAULT_MAX_WAIT_DURATION);
    }
    
    /**
     * Execute with exponential backoff retry
     * 
     * @param operation operation to execute
     * @param operationName operation name
     * @param maxRetries maximum retries
     * @param waitDuration initial wait duration
     * @param multiplier backoff multiplier
     * @param maxWaitDuration maximum wait duration
     * @param <T> return type
     * @return result
     */
    public <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int maxRetries,
            Duration waitDuration,
            double multiplier,
            Duration maxWaitDuration) {
        
        RetryConfig config = RetryConfig.<Throwable>custom()
            .maxAttempts(maxRetries + 1) // +1 for initial attempt
            .waitDuration(waitDuration)
            .multiplier(multiplier)
            .maxWaitDuration(maxWaitDuration)
            .retryExceptions(RETRYABLE_EXCEPTIONS.toArray(new Class[0]))
            .failAfterMaxAttempts(true)
            .build();
        
        Retry retry = retryPolicies.computeIfAbsent(operationName,
            name -> registry.retry(name, config));
        
        return executeWithRetry(operation, retry, 0, waitDuration, multiplier, maxWaitDuration);
    }
    
    private <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation,
            Retry retry,
            int attempt,
            Duration waitDuration,
            double multiplier,
            Duration maxWaitDuration) {
        
        return operation.get()
            .exceptionallyCompose(throwable -> {
                // Check if retryable
                if (!isRetryable(throwable)) {
                    logger.error("Non-retryable error in attempt {}: {}", 
                        attempt, throwable.getMessage());
                    return CompletableFuture.failedFuture(throwable);
                }
                
                // Check max retries
                if (attempt >= retry.getRetryConfig().getMaxAttempts() - 1) {
                    logger.error("Max retries ({}) exceeded for operation", attempt);
                    return CompletableFuture.failedFuture(new RetryExhaustedException(
                        "Max retries exceeded after " + attempt + " attempts", throwable));
                }
                
                // Calculate next wait duration
                Duration nextWait = calculateNextWait(waitDuration, attempt, multiplier, maxWaitDuration);
                
                logger.warn("Attempt {} failed: {}, retrying in {}ms",
                    attempt + 1, throwable.getMessage(), nextWait.toMillis());
                
                // Schedule retry
                return CompletableFuture.supplyAsync(() -> null)
                    .thenCompose(v -> {
                        try {
                            Thread.sleep(nextWait.toMillis());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return CompletableFuture.failedFuture(e);
                        }
                        return executeWithRetry(operation, retry, attempt + 1, 
                            nextWait, multiplier, maxWaitDuration);
                    });
            });
    }
    
    /**
     * Execute with specific error code retry
     * 
     * @param operation operation to execute
     * @param operationName operation name
     * @param retryableErrorCodes set of retryable error codes
     * @param <T> return type
     * @return result
     */
    public <T> CompletableFuture<T> executeWithErrorCodeRetry(
            Supplier<CompletableFuture<ErrorCodeResult<T>>> operation,
            String operationName,
            Set<Integer> retryableErrorCodes) {
        
        return executeWithErrorCodeRetry(operation, operationName, 
            retryableErrorCodes, DEFAULT_MAX_RETRIES);
    }
    
    private <T> CompletableFuture<T> executeWithErrorCodeRetry(
            Supplier<CompletableFuture<ErrorCodeResult<T>>> operation,
            String operationName,
            Set<Integer> retryableErrorCodes,
            int maxRetries) {
        
        return executeWithErrorCodeRetry(operation, operationName, 
            retryableErrorCodes, maxRetries, 0, DEFAULT_WAIT_DURATION);
    }
    
    private <T> CompletableFuture<T> executeWithErrorCodeRetry(
            Supplier<CompletableFuture<ErrorCodeResult<T>>> operation,
            String operationName,
            Set<Integer> retryableErrorCodes,
            int maxRetries,
            int attempt,
            Duration waitDuration) {
        
        return operation.get()
            .thenCompose(result -> {
                if (result.isSuccess()) {
                    return CompletableFuture.completedFuture(result.data());
                }
                
                // Check if error code is retryable
                if (!retryableErrorCodes.contains(result.errorCode())) {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Non-retryable error code: " + result.errorCode()));
                }
                
                // Check max retries
                if (attempt >= maxRetries) {
                    return CompletableFuture.failedFuture(new RetryExhaustedException(
                        "Max retries exceeded, last error code: " + result.errorCode()));
                }
                
                // Calculate next wait
                Duration nextWait = calculateNextWait(waitDuration, attempt, 
                    DEFAULT_MULTIPLIER, DEFAULT_MAX_WAIT_DURATION);
                
                logger.warn("Attempt {} failed with error code {}, retrying in {}ms",
                    attempt + 1, result.errorCode(), nextWait.toMillis());
                
                // Schedule retry
                return CompletableFuture.supplyAsync(() -> null)
                    .thenCompose(v -> {
                        try {
                            Thread.sleep(nextWait.toMillis());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return CompletableFuture.failedFuture(e);
                        }
                        return executeWithErrorCodeRetry(operation, operationName,
                            retryableErrorCodes, maxRetries, attempt + 1, nextWait);
                    });
            });
    }
    
    /**
     * Calculate next wait duration with exponential backoff
     */
    private Duration calculateNextWait(Duration baseWait, int attempt, 
                                       double multiplier, Duration maxWait) {
        long nextWaitMillis = (long) (baseWait.toMillis() * Math.pow(multiplier, attempt));
        nextWaitMillis = Math.min(nextWaitMillis, maxWait.toMillis());
        return Duration.ofMillis(nextWaitMillis);
    }
    
    /**
     * Check if exception is retryable
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable == null) return false;
        
        for (Class<? extends Throwable> retryableClass : RETRYABLE_EXCEPTIONS) {
            if (retryableClass.isAssignableFrom(throwable.getClass())) {
                return true;
            }
        }
        
        // Check cause
        if (throwable.getCause() != null) {
            return isRetryable(throwable.getCause());
        }
        
        return false;
    }
    
    /**
     * Get retry metrics
     */
    public RetryMetrics getMetrics(String operationName) {
        Retry retry = retryPolicies.get(operationName);
        
        if (retry == null) {
            return null;
        }
        
        return new RetryMetrics(
            operationName,
            retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt(),
            retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt(),
            retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt(),
            retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()
        );
    }
    
    /**
     * Result with error code
     */
    public record ErrorCodeResult<T>(
        boolean isSuccess,
        T data,
        int errorCode,
        String errorMessage
    ) {
        public static <T> ErrorCodeResult<T> success(T data) {
            return new ErrorCodeResult<>(true, data, 0, null);
        }
        
        public static <T> ErrorCodeResult<T> failure(int errorCode, String message) {
            return new ErrorCodeResult<>(false, null, errorCode, message);
        }
    }
    
    /**
     * Retry metrics
     */
    public record RetryMetrics(
        String operationName,
        long successfulWithoutRetry,
        long successfulWithRetry,
        long failedWithoutRetry,
        long failedWithRetry
    ) {
        public long getTotalCalls() {
            return successfulWithoutRetry + successfulWithRetry + 
                   failedWithoutRetry + failedWithRetry;
        }
        
        public double getRetryRate() {
            long total = getTotalCalls();
            if (total == 0) return 0.0;
            return (double) (successfulWithRetry + failedWithRetry) / total;
        }
    }
    
    /**
     * Retry exhausted exception
     */
    public static class RetryExhaustedException extends RuntimeException {
        public RetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public RetryExhaustedException(String message) {
            super(message);
        }
    }
}
