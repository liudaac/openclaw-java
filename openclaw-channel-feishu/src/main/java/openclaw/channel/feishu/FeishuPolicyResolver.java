package openclaw.channel.feishu;

import openclaw.channel.feishu.config.FeishuGroupConfig;
import openclaw.channel.feishu.policy.FeishuPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Feishu policy resolver.
 * <p>
 * Resolves group policies and reply policies for Feishu channel.
 * Based on: extensions/feishu/src/policy.ts
 *
 * @author OpenClaw Team
 * @version 2026.3.25
 */
public class FeishuPolicyResolver {

    private static final Logger logger = LoggerFactory.getLogger(FeishuPolicyResolver.class);

    /**
     * Result of allowlist match.
     */
    public record AllowlistMatch(boolean allowed, String matchKey, MatchSource matchSource) {
        public AllowlistMatch(boolean allowed) {
            this(allowed, null, null);
        }
    }

    public enum MatchSource {
        WILDCARD,
        ID
    }

    /**
     * Resolve Feishu allowlist match.
     *
     * @param allowFrom  allowed entries list
     * @param senderId   sender ID
     * @param senderIds  additional sender IDs
     * @param senderName sender display name
     * @return allowlist match result
     */
    public AllowlistMatch resolveAllowlistMatch(
            List<String> allowFrom,
            String senderId,
            List<String> senderIds,
            String senderName) {

        List<String> normalizedAllowFrom = allowFrom.stream()
                .map(this::normalizeAllowEntry)
                .filter(s -> !s.isEmpty())
                .toList();

        if (normalizedAllowFrom.isEmpty()) {
            return new AllowlistMatch(false);
        }

        if (normalizedAllowFrom.contains("*")) {
            return new AllowlistMatch(true, "*", MatchSource.WILDCARD);
        }

        // Build sender candidates
        List<String> senderCandidates = new ArrayList<>();
        senderCandidates.add(normalizeAllowEntry(senderId));
        if (senderIds != null) {
            senderIds.stream()
                    .filter(Objects::nonNull)
                    .map(this::normalizeAllowEntry)
                    .filter(s -> !s.isEmpty())
                    .forEach(senderCandidates::add);
        }

        for (String senderCandidate : senderCandidates) {
            if (normalizedAllowFrom.contains(senderCandidate)) {
                return new AllowlistMatch(true, senderCandidate, MatchSource.ID);
            }
        }

        return new AllowlistMatch(false);
    }

    /**
     * Normalize allow entry.
     *
     * @param raw raw entry string
     * @return normalized entry
     */
    private String normalizeAllowEntry(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if ("*".equals(trimmed)) {
            return "*";
        }
        // Remove feishu: prefix if present
        String withoutPrefix = trimmed.replaceFirst("(?i)^feishu:", "");
        return withoutPrefix.trim().toLowerCase();
    }

    /**
     * Resolve group configuration for a specific group.
     *
     * @param groups  groups configuration map
     * @param groupId group ID
     * @return optional group config
     */
    public Optional<FeishuGroupConfig> resolveGroupConfig(
            Map<String, FeishuGroupConfig> groups,
            String groupId) {

        if (groups == null || groups.isEmpty()) {
            return Optional.empty();
        }

        // Try wildcard first
        FeishuGroupConfig wildcard = groups.get("*");

        if (groupId == null || groupId.trim().isEmpty()) {
            return Optional.ofNullable(wildcard);
        }

        // Try exact match
        FeishuGroupConfig direct = groups.get(groupId);
        if (direct != null) {
            return Optional.of(direct);
        }

        // Try case-insensitive match
        String lowered = groupId.toLowerCase();
        Optional<String> matchKey = groups.keySet().stream()
                .filter(key -> key.toLowerCase().equals(lowered))
                .findFirst();

        if (matchKey.isPresent()) {
            return Optional.of(groups.get(matchKey.get()));
        }

        return Optional.ofNullable(wildcard);
    }

    /**
     * Check if group is allowed based on policy.
     *
     * @param groupPolicy group policy
     * @param allowFrom   allowed list
     * @param senderId    sender ID
     * @param senderIds   additional sender IDs
     * @param senderName  sender name
     * @return true if allowed
     */
    public boolean isGroupAllowed(
            FeishuGroupPolicy groupPolicy,
            List<String> allowFrom,
            String senderId,
            List<String> senderIds,
            String senderName) {

        // ALLOWALL is treated as OPEN
        FeishuGroupPolicy effectivePolicy = groupPolicy == FeishuGroupPolicy.ALLOWALL
                ? FeishuGroupPolicy.OPEN
                : groupPolicy;

        return switch (effectivePolicy) {
            case OPEN -> true;
            case DISABLED -> false;
            case ALLOWLIST -> {
                AllowlistMatch match = resolveAllowlistMatch(allowFrom, senderId, senderIds, senderName);
                yield match.allowed();
            }
            default -> false;
        };
    }

    /**
     * Static version of isGroupAllowed for convenience.
     */
    public static boolean isGroupAllowed(
            FeishuGroupPolicy groupPolicy,
            List<String> allowFrom,
            String senderId,
            String senderName) {

        FeishuPolicyResolver resolver = new FeishuPolicyResolver();
        return resolver.isGroupAllowed(groupPolicy, allowFrom, senderId, List.of(), senderName);
    }

    /**
     * Reply policy result.
     */
    public record ReplyPolicy(boolean requireMention) {
        public boolean isRequireMention() {
            return requireMention;
        }
    }

    /**
     * Resolve reply policy for a message.
     * <p>
     * When groupPolicy is "open", requireMention defaults to false
     * so that non-text messages (e.g. images) that cannot carry
     * @-mentions are still delivered to the agent.
     *
     * @param isDirectMessage whether this is a direct message
     * @param groupPolicy     effective group policy
     * @param groupConfig     group-specific config
     * @param globalRequireMention global requireMention setting
     * @return reply policy
     */
    public ReplyPolicy resolveReplyPolicy(
            boolean isDirectMessage,
            FeishuGroupPolicy groupPolicy,
            Optional<FeishuGroupConfig> groupConfig,
            Optional<Boolean> globalRequireMention) {

        // DM always doesn't require mention
        if (isDirectMessage) {
            return new ReplyPolicy(false);
        }

        // Check group-specific setting
        Optional<Boolean> groupRequireMention = groupConfig.flatMap(FeishuGroupConfig::getRequireMention);

        boolean requireMention = groupRequireMention
                .orElse(globalRequireMention
                        .orElseGet(() -> {
                            // When groupPolicy is "open", default to false
                            // This allows non-text messages (images, etc.) to be processed
                            // even without @-mentions
                            return groupPolicy != FeishuGroupPolicy.OPEN;
                        }));

        return new ReplyPolicy(requireMention);
    }

    /**
     * Static version of resolveReplyPolicy for convenience.
     */
    public static ReplyPolicy resolveReplyPolicy(
            boolean isDirectMessage,
            FeishuPolicy policy,
            Optional<FeishuGroupConfig> groupConfig,
            String messageType) {

        FeishuGroupPolicy groupPolicy = groupConfig
                .map(FeishuGroupConfig::getGroupPolicy)
                .orElse(policy.getGroupPolicy());

        Optional<Boolean> globalRequireMention = Optional.of(policy.isRequireMention());

        // For non-text messages in OPEN policy, don't require mention
        if (!isDirectMessage && groupPolicy == FeishuGroupPolicy.OPEN && !"text".equals(messageType)) {
            return new ReplyPolicy(false);
        }

        // DM always doesn't require mention
        if (isDirectMessage) {
            return new ReplyPolicy(false);
        }

        // Check group-specific setting
        Optional<Boolean> groupRequireMention = groupConfig.flatMap(FeishuGroupConfig::getRequireMention);

        boolean requireMention = groupRequireMention
                .orElse(globalRequireMention
                        .orElseGet(() -> groupPolicy != FeishuGroupPolicy.OPEN));

        return new ReplyPolicy(requireMention);
    }

    /**
     * Check if message should be processed based on mention policy.
     *
     * @param isDirectMessage whether this is a direct message
     * @param mentionedBot    whether the bot was mentioned
     * @param contentType     message content type
     * @param replyPolicy     resolved reply policy
     * @return true if should process
     */
    public boolean shouldProcessMessage(
            boolean isDirectMessage,
            boolean mentionedBot,
            String contentType,
            ReplyPolicy replyPolicy) {

        // DM always processed
        if (isDirectMessage) {
            return true;
        }

        // If mentioned, always process
        if (mentionedBot) {
            return true;
        }

        // If requireMention is false, process all messages
        if (!replyPolicy.requireMention()) {
            return true;
        }

        // Otherwise, only process if mentioned
        return false;
    }
}