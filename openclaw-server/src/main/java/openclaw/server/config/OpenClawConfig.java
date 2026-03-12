package openclaw.server.config;

import openclaw.agent.AcpProtocol;
import openclaw.gateway.GatewayService;
import openclaw.sdk.channel.ChannelPlugin;
import openclaw.sdk.tool.AgentTool;
import openclaw.server.service.AcpProtocolImpl;
import openclaw.server.service.GatewayServiceImpl;
import org.springframework.ai.chat.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * OpenClaw Configuration
 *
 * <p>Configures OpenClaw services and components.</p>
 */
@Configuration
public class OpenClawConfig {

    @Bean
    @Primary
    public GatewayService gatewayService() {
        return new GatewayServiceImpl();
    }

    @Bean
    @Primary
    public AcpProtocol acpProtocol(ChatClient chatClient) {
        return new AcpProtocolImpl(chatClient);
    }
}
