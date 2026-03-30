package openclaw.channel.feishu;

import openclaw.channel.feishu.config.FeishuGroupConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FeishuPolicyResolver.
 * <p>
 * Based on: extensions/feishu/src/policy.test.ts
 *
 * @author OpenClaw Team
 * @version 2026.3.25
 */
class FeishuPolicyResolverTest {

    private FeishuPolicyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new FeishuPolicyResolver();
    }

    @Test
    void testResolveAllowlistMatch_Wildcard() {
        List<String> allowFrom = List.of("*");
        FeishuPolicyResolver.AllowlistMatch match = resolver.resolveAllowlistMatch(
                allowFrom, "user123", null, "User Name");

        assertTrue(match.allowed());
        assertEquals("*", match.matchKey());
        assertEquals(FeishuPolicyResolver.MatchSource.WILDCARD, match.matchSource());
    }

    @Test
    void testResolveAllowlistMatch_EmptyList() {
        List<String> allowFrom = List.of();
        FeishuPolicyResolver.AllowlistMatch match = resolver.resolveAllowlistMatch(
                allowFrom, "user123", null, "User Name");

        assertFalse(match.allowed());
    }

    @Test
    void testResolveAllowlistMatch_MatchById() {
        List<String> allowFrom = List.of("user123", "user456");
        FeishuPolicyResolver.AllowlistMatch match = resolver.resolveAllowlistMatch(
                allowFrom, "user123", null, "User Name");

        assertTrue(match.allowed());
        assertEquals("user123", match.matchKey());
        assertEquals(FeishuPolicyResolver.MatchSource.ID, match.matchSource());
    }

    @Test
    void testResolveAllowlistMatch_NoMatch() {
        List<String> allowFrom = List.of("user456", "user789");
        FeishuPolicyResolver.AllowlistMatch match = resolver.resolveAllowlistMatch(
                allowFrom, "user123", null, "User Name");

        assertFalse(match.allowed());
    }

    @Test
    void testResolveAllowlistMatch_CaseInsensitive() {
        List<String> allowFrom = List.of("USER123");
        FeishuPolicyResolver.AllowlistMatch match = resolver.resolveAllowlistMatch(
                allowFrom, "user123", null, "User Name");

        assertTrue(match.allowed());
    }

    @Test
    void testResolveAllowlistMatch_WithPrefix() {
        List<String> allowFrom = List.of("feishu:user123");
        FeishuPolicyResolver.AllowlistMatch match = resolver.resolveAllowlistMatch(
                allowFrom, "user123", null, "User Name");

        assertTrue(match.allowed());
    }

    @Test
    void testResolveAllowlistMatch_MultipleSenderIds() {
        List<String> allowFrom = List.of("user456");
        List<String> senderIds = List.of("user123", "user456");
        FeishuPolicyResolver.AllowlistMatch match = resolver.resolveAllowlistMatch(
                allowFrom, "user789", senderIds, "User Name");

        assertTrue(match.allowed());
        assertEquals("user456", match.matchKey());
    }

    @Test
    void testResolveGroupConfig_ExactMatch() {
        FeishuGroupConfig groupConfig = new FeishuGroupConfig();
        groupConfig.setEnabled(true);

        Map<String, FeishuGroupConfig> groups = Map.of(
                "group123", groupConfig
        );

        Optional<FeishuGroupConfig> result = resolver.resolveGroupConfig(groups, "group123");
        assertTrue(result.isPresent());
        assertTrue(result.get().getEnabled().orElse(false));
    }

    @Test
    void testResolveGroupConfig_Wildcard() {
        FeishuGroupConfig wildcardConfig = new FeishuGroupConfig();
        wildcardConfig.setEnabled(true);

        Map<String, FeishuGroupConfig> groups = Map.of(
                "*", wildcardConfig
        );

        Optional<FeishuGroupConfig> result = resolver.resolveGroupConfig(groups, "unknownGroup");
        assertTrue(result.isPresent());
        assertTrue(result.get().getEnabled().orElse(false));
    }

    @Test
    void testResolveGroupConfig_CaseInsensitive() {
        FeishuGroupConfig groupConfig = new FeishuGroupConfig();
        groupConfig.setEnabled(true);

        Map<String, FeishuGroupConfig> groups = Map.of(
                "GroupABC", groupConfig
        );

        Optional<FeishuGroupConfig> result = resolver.resolveGroupConfig(groups, "groupabc");
        assertTrue(result.isPresent());
    }

    @Test
    void testResolveGroupConfig_NoMatch() {
        Map<String, FeishuGroupConfig> groups = Map.of();
        Optional<FeishuGroupConfig> result = resolver.resolveGroupConfig(groups, "group123");
        assertFalse(result.isPresent());
    }

    @Test
    void testIsGroupAllowed_OpenPolicy() {
        boolean allowed = resolver.isGroupAllowed(
                FeishuGroupPolicy.OPEN,
                List.of(),
                "user123",
                null,
                "User"
        );
        assertTrue(allowed);
    }

    @Test
    void testIsGroupAllowed_AllowallPolicy() {
        // ALLOWALL should be treated as OPEN
        boolean allowed = resolver.isGroupAllowed(
                FeishuGroupPolicy.ALLOWALL,
                List.of(),
                "user123",
                null,
                "User"
        );
        assertTrue(allowed);
    }

    @Test
    void testIsGroupAllowed_DisabledPolicy() {
        boolean allowed = resolver.isGroupAllowed(
                FeishuGroupPolicy.DISABLED,
                List.of("*"),
                "user123",
                null,
                "User"
        );
        assertFalse(allowed);
    }

    @Test
    void testIsGroupAllowed_AllowlistPolicy_Match() {
        boolean allowed = resolver.isGroupAllowed(
                FeishuGroupPolicy.ALLOWLIST,
                List.of("user123"),
                "user123",
                null,
                "User"
        );
        assertTrue(allowed);
    }

    @Test
    void testIsGroupAllowed_AllowlistPolicy_NoMatch() {
        boolean allowed = resolver.isGroupAllowed(
                FeishuGroupPolicy.ALLOWLIST,
                List.of("user456"),
                "user123",
                null,
                "User"
        );
        assertFalse(allowed);
    }

    @Test
    void testResolveReplyPolicy_DirectMessage() {
        FeishuPolicyResolver.ReplyPolicy policy = resolver.resolveReplyPolicy(
                true, // isDirectMessage
                FeishuGroupPolicy.ALLOWLIST,
                Optional.empty(),
                Optional.empty()
        );
        assertFalse(policy.requireMention());
    }

    @Test
    void testResolveReplyPolicy_Group_OpenPolicy() {
        // When groupPolicy is "open", requireMention defaults to false
        FeishuPolicyResolver.ReplyPolicy policy = resolver.resolveReplyPolicy(
                false, // isDirectMessage
                FeishuGroupPolicy.OPEN,
                Optional.empty(),
                Optional.empty()
        );
        assertFalse(policy.requireMention());
    }

    @Test
    void testResolveReplyPolicy_Group_AllowlistPolicy() {
        // When groupPolicy is "allowlist", requireMention defaults to true
        FeishuPolicyResolver.ReplyPolicy policy = resolver.resolveReplyPolicy(
                false, // isDirectMessage
                FeishuGroupPolicy.ALLOWLIST,
                Optional.empty(),
                Optional.empty()
        );
        assertTrue(policy.requireMention());
    }

    @Test
    void testResolveReplyPolicy_GroupConfigOverride() {
        FeishuGroupConfig groupConfig = new FeishuGroupConfig();
        groupConfig.setRequireMention(false);

        FeishuPolicyResolver.ReplyPolicy policy = resolver.resolveReplyPolicy(
                false, // isDirectMessage
                FeishuGroupPolicy.ALLOWLIST,
                Optional.of(groupConfig),
                Optional.empty()
        );
        assertFalse(policy.requireMention());
    }

    @Test
    void testResolveReplyPolicy_GlobalOverride() {
        FeishuPolicyResolver.ReplyPolicy policy = resolver.resolveReplyPolicy(
                false, // isDirectMessage
                FeishuGroupPolicy.ALLOWLIST,
                Optional.empty(),
                Optional.of(false) // global requireMention = false
        );
        assertFalse(policy.requireMention());
    }

    @Test
    void testShouldProcessMessage_DirectMessage() {
        FeishuPolicyResolver.ReplyPolicy policy = new FeishuPolicyResolver.ReplyPolicy(true);
        boolean shouldProcess = resolver.shouldProcessMessage(
                true, // isDirectMessage
                false, // mentionedBot
                "text",
                policy
        );
        assertTrue(shouldProcess);
    }

    @Test
    void testShouldProcessMessage_Mentioned() {
        FeishuPolicyResolver.ReplyPolicy policy = new FeishuPolicyResolver.ReplyPolicy(true);
        boolean shouldProcess = resolver.shouldProcessMessage(
                false, // isDirectMessage
                true, // mentionedBot
                "text",
                policy
        );
        assertTrue(shouldProcess);
    }

    @Test
    void testShouldProcessMessage_NoMention_RequireMention() {
        FeishuPolicyResolver.ReplyPolicy policy = new FeishuPolicyResolver.ReplyPolicy(true);
        boolean shouldProcess = resolver.shouldProcessMessage(
                false, // isDirectMessage
                false, // mentionedBot
                "text",
                policy
        );
        assertFalse(shouldProcess);
    }

    @Test
    void testShouldProcessMessage_NoMention_NoRequireMention() {
        FeishuPolicyResolver.ReplyPolicy policy = new FeishuPolicyResolver.ReplyPolicy(false);
        boolean shouldProcess = resolver.shouldProcessMessage(
                false, // isDirectMessage
                false, // mentionedBot
                "image", // non-text message
                policy
        );
        assertTrue(shouldProcess);
    }

    @Test
    void testGroupPolicyFromString() {
        assertEquals(FeishuGroupPolicy.OPEN, FeishuGroupPolicy.fromString("open"));
        assertEquals(FeishuGroupPolicy.ALLOWLIST, FeishuGroupPolicy.fromString("allowlist"));
        assertEquals(FeishuGroupPolicy.DISABLED, FeishuGroupPolicy.fromString("disabled"));
        assertEquals(FeishuGroupPolicy.ALLOWALL, FeishuGroupPolicy.fromString("allowall"));
    }

    @Test
    void testGroupPolicyFromString_Null() {
        assertEquals(FeishuGroupPolicy.ALLOWLIST, FeishuGroupPolicy.fromString(null));
    }

    @Test
    void testGroupPolicyFromString_Unknown() {
        assertEquals(FeishuGroupPolicy.ALLOWLIST, FeishuGroupPolicy.fromString("unknown"));
    }

    @Test
    void testGroupPolicyIsOpen() {
        assertTrue(FeishuGroupPolicy.OPEN.isOpen());
        assertTrue(FeishuGroupPolicy.ALLOWALL.isOpen());
        assertFalse(FeishuGroupPolicy.ALLOWLIST.isOpen());
        assertFalse(FeishuGroupPolicy.DISABLED.isOpen());
    }
}