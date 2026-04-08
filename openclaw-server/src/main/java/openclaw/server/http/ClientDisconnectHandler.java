package openclaw.server.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handler for managing HTTP request lifecycle and client disconnect detection.
 *
 * <p>Provides signal-based cancellation for long-running agent operations
 * when the HTTP client disconnects.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
public class ClientDisconnectHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClientDisconnectHandler.class);

    private final Map<String, RequestContext> activeRequests = new ConcurrentHashMap<>();

    /**
     * Create a new request context for tracking client disconnect.
     *
     * @param requestId the unique request ID
     * @return the request context
     */
    public RequestContext createContext(String requestId) {
        RequestContext context = new RequestContext(requestId);
        activeRequests.put(requestId, context);
        return context;
    }

    /**
     * Remove a request context when the request completes.
     *
     * @param requestId the request ID
     */
    public void removeContext(String requestId) {
        activeRequests.remove(requestId);
    }

    /**
     * Check if a request has been cancelled by client disconnect.
     *
     * @param requestId the request ID
     * @return true if cancelled
     */
    public boolean isCancelled(String requestId) {
        RequestContext context = activeRequests.get(requestId);
        return context != null && context.isCancelled();
    }

    /**
     * Signal cancellation for a request.
     *
     * @param requestId the request ID
     * @param reason the cancellation reason
     */
    public void cancel(String requestId, String reason) {
        RequestContext context = activeRequests.get(requestId);
        if (context != null) {
            context.cancel(reason);
            logger.debug("Request {} cancelled: {}", requestId, reason);
        }
    }

    /**
     * Get the number of active requests.
     *
     * @return active request count
     */
    public int getActiveRequestCount() {
        return activeRequests.size();
    }

    /**
     * Request context for tracking a single HTTP request.
     */
    public static class RequestContext {
        private final String requestId;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final Sinks.One<String> cancellationSink = Sinks.one();
        private volatile String cancellationReason;
        private volatile long startTime = System.currentTimeMillis();

        public RequestContext(String requestId) {
            this.requestId = requestId;
        }

        /**
         * Check if this request has been cancelled.
         *
         * @return true if cancelled
         */
        public boolean isCancelled() {
            return cancelled.get();
        }

        /**
         * Cancel this request.
         *
         * @param reason the cancellation reason
         */
        public void cancel(String reason) {
            if (cancelled.compareAndSet(false, true)) {
                this.cancellationReason = reason;
                cancellationSink.tryEmitValue(reason);
            }
        }

        /**
         * Get a mono that completes when this request is cancelled.
         *
         * @return cancellation mono
         */
        public Mono<String> onCancel() {
            return cancellationSink.asMono();
        }

        /**
         * Get the cancellation reason.
         *
         * @return the reason, or null if not cancelled
         */
        public String getCancellationReason() {
            return cancellationReason;
        }

        /**
         * Get the request ID.
         *
         * @return the request ID
         */
        public String getRequestId() {
            return requestId;
        }

        /**
         * Get the request duration in milliseconds.
         *
         * @return duration in ms
         */
        public long getDurationMs() {
            return System.currentTimeMillis() - startTime;
        }

        /**
         * Create a disposable that cancels this context when disposed.
         *
         * @return the disposable
         */
        public Disposable asDisposable() {
            return new Disposable() {
                private boolean disposed = false;

                @Override
                public void dispose() {
                    if (!disposed) {
                        disposed = true;
                        cancel("Disposed");
                    }
                }

                @Override
                public boolean isDisposed() {
                    return disposed || isCancelled();
                }
            };
        }
    }
}
