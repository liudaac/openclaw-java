package openclaw.plugin.sdk.websearch;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WebSearchProviderRegistry.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
class WebSearchProviderRegistryTest {

    @Test
    void testRegistryCreation() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry();
        assertNotNull(registry);
    }

    @Test
    void testGetProviderIds() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry();
        Set<String> ids = registry.getProviderIds();
        assertNotNull(ids);
        // May be empty if no providers in classpath
    }

    @Test
    void testGetAllProviders() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry();
        List<WebSearchProvider> providers = registry.getAllProviders();
        assertNotNull(providers);
    }

    @Test
    void testGetProvider() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry();
        Optional<WebSearchProvider> provider = registry.getProvider("nonexistent");
        assertTrue(provider.isEmpty());
    }

    @Test
    void testHasProvider() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry();
        assertFalse(registry.hasProvider("nonexistent"));
    }

    @Test
    void testRegisterProvider() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry();

        WebSearchProvider mockProvider = createMockProvider("test", "Test Provider");
        registry.register(mockProvider);

        assertTrue(registry.hasProvider("test"));
        Optional<WebSearchProvider> retrieved = registry.getProvider("test");
        assertTrue(retrieved.isPresent());
        assertEquals("Test Provider", retrieved.get().getLabel());
    }

    @Test
    void testGetProvidersByAutoDetectOrder() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry();

        // Register providers with different orders
        registry.register(createMockProvider("c", "C", 30));
        registry.register(createMockProvider("a", "A", 10));
        registry.register(createMockProvider("b", "B", 20));

        List<WebSearchProvider> sorted = registry.getProvidersByAutoDetectOrder();

        assertEquals(3, sorted.size());
        assertEquals("a", sorted.get(0).getId());
        assertEquals("b", sorted.get(1).getId());
        assertEquals("c", sorted.get(2).getId());
    }

    private WebSearchProvider createMockProvider(String id, String label) {
        return createMockProvider(id, label, 100);
    }

    private WebSearchProvider createMockProvider(String id, String label, int order) {
        return new WebSearchProvider() {
            @Override
            public String getId() { return id; }

            @Override
            public String getLabel() { return label; }

            @Override
            public String getHint() { return "Test"; }

            @Override
            public String[] getEnvVars() { return new String[0]; }

            @Override
            public String getPlaceholder() { return ""; }

            @Override
            public String getSignupUrl() { return ""; }

            @Override
            public int getAutoDetectOrder() { return order; }

            @Override
            public String getCredentialPath() { return ""; }

            @Override
            public Object getCredentialValue(java.util.Map<String, Object> searchConfig) {
                return null;
            }

            @Override
            public void setCredentialValue(java.util.Map<String, Object> searchConfigTarget, Object value) {}

            @Override
            public WebSearchToolDefinition createTool(WebSearchContext ctx) {
                return null;
            }
        };
    }
}
