package openclaw.gateway.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * WebFilter for enforcing scope-based authorization.
 *
 * <p>Intercepts requests and validates that they have the required scopes
 * for accessing protected endpoints.</p>
 *
 * <p>Compatible with Spring WebFlux reactive stack.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 * @since 2026.3.28
 */
@Component
@Order(-100) // High priority, run early
public class ScopeAuthorizationFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(ScopeAuthorizationFilter.class);

    /**
     * Header name for scopes.
     */
    public static final String SCOPES_HEADER = "X-OpenClaw-Scopes";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Check for models endpoint - requires read scope
        if (path.startsWith("/api/models") || path.startsWith("/models")) {
            return checkModelsReadScope(exchange, chain);
        }

        // Check for chat endpoint with system provenance - requires admin scope
        if (path.startsWith("/api/chat") && hasSystemProvenanceHeaders(request)) {
            return checkChatProvenanceScope(exchange, chain);
        }

        return chain.filter(exchange);
    }

    /**
     * Checks if the request has read scope for models API.
     */
    private Mono<Void> checkModelsReadScope(ServerWebExchange exchange, WebFilterChain chain) {
        List<String> grantedScopes = extractScopes(exchange.getRequest());

        boolean hasReadScope = grantedScopes.contains(OperatorScopes.READ_SCOPE) ||
                grantedScopes.contains(OperatorScopes.WRITE_SCOPE) ||
                grantedScopes.contains(OperatorScopes.ADMIN_SCOPE);

        if (!hasReadScope) {
            logger.warn("Models API access denied: missing operator.read scope");
            return createForbiddenResponse(exchange, "missing scope: operator.read");
        }

        return chain.filter(exchange);
    }

    /**
     * Checks if the request has admin scope for system provenance injection.
     */
    private Mono<Void> checkChatProvenanceScope(ServerWebExchange exchange, WebFilterChain chain) {
        List<String> grantedScopes = extractScopes(exchange.getRequest());

        if (!OperatorScopes.canInjectSystemProvenance(grantedScopes)) {
            logger.warn("Chat system provenance injection denied: missing admin scope");
            return createForbiddenResponse(exchange, "system provenance fields require admin scope");
        }

        return chain.filter(exchange);
    }

    /**
     * Checks if request has system provenance headers.
     */
    private boolean hasSystemProvenanceHeaders(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-System-Input-Provenance") != null ||
                request.getHeaders().getFirst("X-System-Provenance-Receipt") != null;
    }

    /**
     * Extracts scopes from the request.
     */
    private List<String> extractScopes(ServerHttpRequest request) {
        String scopesHeader = request.getHeaders().getFirst(SCOPES_HEADER);
        if (scopesHeader == null || scopesHeader.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(scopesHeader.split(","));
    }

    /**
     * Creates a forbidden response.
     */
    private Mono<Void> createForbiddenResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"ok\":false,\"error\":{\"type\":\"forbidden\",\"message\":\"%s\"}}", message);
        byte[] bytes = body.getBytes();

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
