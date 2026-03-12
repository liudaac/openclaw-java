package openclaw.sdk;

import openclaw.sdk.channel.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for plugin discovery.
 */
class DiscoveryTest {

    @Test
    void testServiceLoader() {
        // Test that ServiceLoader can find ChannelPlugin implementations
        ServiceLoader<ChannelPlugin> loader = ServiceLoader.load(ChannelPlugin.class);
        
        // In a real scenario, this would find registered plugins
        // For now, just verify the loader works
        assertThat(loader).isNotNull();
    }

    @Test
    void testChannelPluginOptionalAdapters() {
        // Create a minimal channel plugin implementation
        ChannelPlugin<Void, Void, Void> plugin = new ChannelPlugin<>() {
            @Override
            public ChannelId getId() {
                return new ChannelId("test");
            }

            @Override
            public ChannelMeta getMeta() {
                return ChannelMeta.builder()
                        .name("Test")
                        .description("Test channel")
                        .build();
            }

            @Override
            public ChannelCapabilities getCapabilities() {
                return ChannelCapabilities.basic();
            }

            @Override
            public ChannelConfigAdapter<Void> getConfigAdapter() {
                return null; // Simplified for test
            }
        };

        // Verify optional adapters return empty by default
        assertThat(plugin.getOnboardingAdapter()).isEmpty();
        assertThat(plugin.getSetupAdapter()).isEmpty();
        assertThat(plugin.getPairingAdapter()).isEmpty();
        assertThat(plugin.getSecurityAdapter()).isEmpty();
        assertThat(plugin.getGroupAdapter()).isEmpty();
        assertThat(plugin.getOutboundAdapter()).isEmpty();
    }
}
