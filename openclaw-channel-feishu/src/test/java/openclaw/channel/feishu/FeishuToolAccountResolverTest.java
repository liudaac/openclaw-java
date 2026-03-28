package openclaw.channel.feishu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FeishuToolAccountResolver.
 *
 * <p>Ported from extensions/feishu/src/tool-account-routing.test.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 */
class FeishuToolAccountResolverTest {

    private Map<String, Object> createConfig(Map<String, Object> params) {
        Map<String, Object> toolsA = (Map<String, Object>) params.get("toolsA");
        Map<String, Object> toolsB = (Map<String, Object>) params.get("toolsB");
        String defaultAccount = (String) params.get("defaultAccount");

        Map<String, Object> accountA = Map.of(
                "appId", "app-a",
                "appSecret", "sec-a",
                "tools", toolsA != null ? toolsA : Map.of()
        );

        Map<String, Object> accountB = Map.of(
                "appId", "app-b",
                "appSecret", "sec-b",
                "tools", toolsB != null ? toolsB : Map.of()
        );

        Map<String, Object> accounts = Map.of("a", accountA, "b", accountB);

        Map<String, Object> feishuConfig = Map.of(
                "enabled", true,
                "accounts", accounts
        );

        if (defaultAccount != null) {
            feishuConfig = new java.util.HashMap<>(feishuConfig);
            ((Map<String, Object>) feishuConfig).put("defaultAccount", defaultAccount);
        }

        return Map.of("channels", Map.of("feishu", feishuConfig));
    }

    @Test
    void testExplicitAccountIdTakesPrecedence() {
        Map<String, Object> config = createConfig(Map.of());
        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        Optional<String> result = resolver.resolveImplicitToolAccountId("b", "a");
        assertEquals("b", result.orElse(null));
    }

    @Test
    void testConfiguredDefaultAccountTakesPrecedenceOverContextual() {
        Map<String, Object> config = createConfig(Map.of("defaultAccount", "b"));
        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        Optional<String> result = resolver.resolveImplicitToolAccountId(null, "a");
        assertEquals("b", result.orElse(null));
    }

    @Test
    void testContextualAccountIdUsedWhenValid() {
        Map<String, Object> config = createConfig(Map.of());
        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        Optional<String> result = resolver.resolveImplicitToolAccountId(null, "b");
        assertEquals("b", result.orElse(null));
    }

    @Test
    void testSyntheticAccountFallsBackToDefault() {
        Map<String, Object> config = createConfig(Map.of("defaultAccount", "a"));
        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        // "agent-spawner" is a synthetic account (not in config)
        Optional<String> result = resolver.resolveImplicitToolAccountId(null, "agent-spawner");
        assertEquals("a", result.orElse(null));
    }

    @Test
    void testSyntheticAccountFallsBackToEmptyWhenNoDefault() {
        Map<String, Object> config = createConfig(Map.of());
        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        // "agent-spawner" is a synthetic account (not in config)
        Optional<String> result = resolver.resolveImplicitToolAccountId(null, "agent-spawner");
        assertTrue(result.isEmpty());
    }

    @Test
    void testEmptyExplicitFallsBackToContextual() {
        Map<String, Object> config = createConfig(Map.of());
        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        Optional<String> result = resolver.resolveImplicitToolAccountId("", "b");
        assertEquals("b", result.orElse(null));
    }

    @Test
    void testWhitespaceOnlyExplicitFallsBackToContextual() {
        Map<String, Object> config = createConfig(Map.of());
        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        Optional<String> result = resolver.resolveImplicitToolAccountId("   ", "b");
        assertEquals("b", result.orElse(null));
    }

    @Test
    void testNullContextualReturnsEmpty() {
        Map<String, Object> config = createConfig(Map.of());
        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        Optional<String> result = resolver.resolveImplicitToolAccountId(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDisabledAccountFallsBack() {
        Map<String, Object> accountA = Map.of(
                "appId", "app-a",
                "appSecret", "sec-a",
                "enabled", false
        );
        Map<String, Object> accountB = Map.of(
                "appId", "app-b",
                "appSecret", "sec-b",
                "enabled", true
        );

        Map<String, Object> config = Map.of(
                "channels", Map.of(
                        "feishu", Map.of(
                                "enabled", true,
                                "accounts", Map.of("a", accountA, "b", accountB)
                        )
                )
        );

        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        // Account "a" is disabled, should fall back
        Optional<String> result = resolver.resolveImplicitToolAccountId(null, "a");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetFirstEnabledAccountId() {
        Map<String, Object> config = createConfig(Map.of());
        FeishuToolAccountResolver resolver = new FeishuToolAccountResolver(config);

        Optional<String> result = resolver.getFirstEnabledAccountId();
        assertTrue(result.isPresent());
        // Should return "a" or "b" (both enabled)
        assertTrue(result.get().equals("a") || result.get().equals("b"));
    }
}
