package openclaw.server.config;

import openclaw.gateway.websocket.PreauthConnectionBudget;
import openclaw.gateway.websocket.PreauthWebSocketInterceptor;
import openclaw.server.websocket.GatewayWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;

import java.util.Map;

/**
 * WebSocket Configuration
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public PreauthConnectionBudget preauthConnectionBudget() {
        return new PreauthConnectionBudget();
    }

    @Bean
    public RequestUpgradeStrategy requestUpgradeStrategy() {
        return new ReactorNettyRequestUpgradeStrategy();
    }

    @Bean
    public WebSocketService webSocketService(PreauthConnectionBudget budget, RequestUpgradeStrategy upgradeStrategy) {
        return new PreauthWebSocketInterceptor(upgradeStrategy, budget);
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping(GatewayWebSocketHandler webSocketHandler) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of("/ws", webSocketHandler));
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter(WebSocketService webSocketService) {
        return new WebSocketHandlerAdapter(webSocketService);
    }
}
