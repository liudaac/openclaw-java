package openclaw.gateway.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * WebSocket interceptor that enforces pre-authentication connection budget.
 *
 * <p>Rejects WebSocket upgrade requests when the pre-auth connection budget
 * is exceeded for the client IP address.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 * @since 2026.3.28
 */
@Component
public class PreauthWebSocketInterceptor extends HandshakeWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(PreauthWebSocketInterceptor.class);

    private final PreauthConnectionBudget budget;

    public PreauthWebSocketInterceptor(PreauthConnectionBudget budget) {
        super();
        this.budget = budget;
    }

    public PreauthWebSocketInterceptor(RequestUpgradeStrategy upgradeStrategy, PreauthConnectionBudget budget) {
        super(upgradeStrategy);
        this.budget = budget;
    }

    @Override
    public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = extractClientIp(request);

        // Check pre-auth budget before allowing upgrade
        if (!budget.acquire(clientIp)) {
            logger.warn("WebSocket upgrade rejected: pre-auth budget exceeded for IP: {}", clientIp);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return response.setComplete();
        }

        // Proceed with handshake
        return super.handleRequest(exchange, handler)
                .doFinally(signal -> {
                    // Release budget when connection completes or fails
                    budget.release(clientIp);
                    logger.debug("Released pre-auth budget for IP: {} (signal: {})", clientIp, signal);
                });
    }

    /**
     * Extracts the client IP from the request.
     *
     * <p>Checks X-Forwarded-For and X-Real-IP headers first (for proxy setups),
     * then falls back to the remote address.</p>
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String extractClientIp(ServerHttpRequest request) {
        // Check X-Forwarded-For header (common with reverse proxies)
        List<String> forwardedFor = request.getHeaders().get("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            String firstIp = forwardedFor.get(0).split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }

        // Check X-Real-IP header (common with nginx)
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }

        // Fall back to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return null;
    }
}
