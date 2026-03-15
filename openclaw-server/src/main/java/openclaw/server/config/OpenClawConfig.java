package openclaw.server.config;

import openclaw.agent.AcpProtocol;
import openclaw.gateway.GatewayService;
import openclaw.sdk.channel.ChannelPlugin;
import openclaw.sdk.channel.ChannelCapabilities;
import openclaw.sdk.channel.ChannelId;
import openclaw.sdk.channel.ChannelMeta;
import openclaw.sdk.channel.ChannelConfigAdapter;
import openclaw.sdk.channel.SendResult;
import openclaw.sdk.tool.AgentTool;
import openclaw.server.service.AcpProtocolImpl;
import openclaw.server.service.GatewayServiceImpl;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

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
    
    @Bean
    public ChannelPlugin<?, ?, ?> channelPlugin() {
        // Return a mock implementation for now
        return new ChannelPlugin<Object, Object, Object>() {
            @Override
            public ChannelId getId() {
                return ChannelId.valueOf("mock");
            }
            
            @Override
            public ChannelMeta getMeta() {
                return ChannelMeta.builder()
                    .name("Mock Channel")
                    .description("Mock channel for testing")
                    .build();
            }
            
            @Override
            public ChannelCapabilities getCapabilities() {
                return ChannelCapabilities.builder()
                    .supportsText(true)
                    .supportsImages(true)
                    .supportsFiles(true)
                    .supportsTyping(true)
                    .build();
            }
            
            @Override
            public ChannelConfigAdapter<Object> getConfigAdapter() {
                return null;
            }
        };
    }
}
