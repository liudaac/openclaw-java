package openclaw.server.config;

import openclaw.agent.AcpProtocol;
import openclaw.gateway.GatewayService;
import openclaw.sdk.channel.ChannelPlugin;
import openclaw.sdk.tool.AgentTool;
import openclaw.server.service.AcpProtocolImpl;
import openclaw.server.service.GatewayServiceImpl;
import org.springframework.ai.chat.client.ChatClient;
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
    
    @Bean
    public ChannelPlugin channelPlugin() {
        // Return a mock implementation for now
        return new ChannelPlugin() {
            @Override
            public String getChannelName() {
                return "mock";
            }
            
            @Override
            public boolean isAvailable() {
                return true;
            }
            
            @Override
            public List<String> getCapabilities() {
                return List.of("send", "receive", "typing");
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<openclaw.sdk.channel.SendResult> sendMessage(openclaw.sdk.channel.ChannelMessage message) {
                return java.util.concurrent.CompletableFuture.completedFuture(
                    new openclaw.sdk.channel.SendResult(true, message.messageId(), System.currentTimeMillis(), null)
                );
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> sendTypingIndicator(String chatId) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        };
    }
}
