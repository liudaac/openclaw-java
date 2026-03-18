package openclaw.tools.search;

import openclaw.plugin.sdk.websearch.WebSearchProviderRegistry;
import openclaw.sdk.tool.AgentTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web Search Auto Configuration.
 * Automatically configures Web Search tools when Spring Boot starts.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
@Configuration
public class WebSearchAutoConfiguration {

    /**
     * Web Search Provider Registry.
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSearchProviderRegistry webSearchProviderRegistry() {
        return new WebSearchProviderRegistry();
    }

    /**
     * Provider-based Web Search Tool.
     */
    @Bean
    @ConditionalOnMissingBean(name = "webSearchTool")
    public AgentTool webSearchTool(WebSearchProviderRegistry registry) {
        // Try to find configured provider
        String defaultProvider = System.getenv("BRAVE_API_KEY") != null ? "brave" : null;
        if (defaultProvider == null) {
            defaultProvider = registry.getProviderIds().stream()
                    .findFirst()
                    .orElse("brave");
        }
        return new ProviderBasedWebSearchTool(registry, defaultProvider);
    }
}
