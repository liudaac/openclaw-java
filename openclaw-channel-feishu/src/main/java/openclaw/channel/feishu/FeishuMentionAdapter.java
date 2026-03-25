package openclaw.channel.feishu;

import openclaw.channel.feishu.config.FeishuGroupConfig;
import openclaw.channel.feishu.policy.FeishuGroupPolicy;
import openclaw.channel.feishu.policy.FeishuPolicy;
import openclaw.channel.feishu.policy.FeishuPolicyResolver;
import openclaw.sdk.channel.ChannelMentionAdapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Feishu mention adapter with policy support.
 *
 * <p>Enhanced with group policy support for mention requirements.
 * When groupPolicy is OPEN, non-text messages do not require explicit mention.</p>
 *
 * <p>Ported from original TypeScript: extensions/feishu/src/policy.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.25
 */
public class FeishuMentionAdapter implements ChannelMentionAdapter {

    private FeishuPolicy policy;
    private FeishuGroupConfig groupConfig;

    public FeishuMentionAdapter() {
        this.policy = FeishuPolicy.defaults();
    }

    /**
     * Creates mention adapter with policy.
     *
     * @param policy the policy to use
     */
    public FeishuMentionAdapter(FeishuPolicy policy) {
        this.policy = policy != null ? policy : FeishuPolicy.defaults();
    }

    /**
     * Sets the policy for this adapter.
     *
     * @param policy the policy
     */
    public void setPolicy(FeishuPolicy policy) {
        this.policy = policy != null ? policy : FeishuPolicy.defaults();
    }

    /**
     * Gets the current policy.
     *
     * @return the policy
     */
    public FeishuPolicy getPolicy() {
        return policy;
    }

    /**
     * Sets the group-specific config.
     *
     * @param groupConfig the group config
     */
    public void setGroupConfig(FeishuGroupConfig groupConfig) {
        this.groupConfig = groupConfig;
    }

    /**
     * Gets the group-specific config.
     *
     * @return the group config
     */
    public Optional<FeishuGroupConfig> getGroupConfig() {
        return Optional.ofNullable(groupConfig);
    }

    @Override
    public String formatMention(String userId, Optional<String> userName) {
        if (userName.isPresent()) {
            return "<at user_id=\"" + userId + "\">" + userName.get() + "</at>";
        }
        return "<at user_id=\"" + userId + "\">User</at>";
    }

    @Override
    public List<ParsedMention> parseMentions(String text) {
        List<ParsedMention> mentions = new java.util.ArrayList<>();

        // Parse <at user_id="xxx">name</at> mentions
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<at user_id=\"([^\"]+)\">([^<]*)</at>");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String userId = matcher.group(1);
            String userName = matcher.group(2);
            mentions.add(new ParsedMention(
                    userId,
                    Optional.of(userName),
                    matcher.start(),
                    matcher.end()
            ));
        }

        return mentions;
    }

    @Override
    public CompletableFuture<Optional<String>> resolveMention(String mention) {
        // Feishu user ID to name resolution requires API call
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Checks if a message should require mention based on policy.
     *
     * <p>When groupPolicy is OPEN, non-text messages (images, files, etc.)
     * do not require explicit mention.</p>
     *
     * @param isDirectMessage whether this is a direct message
     * @param messageType the message type (e.g., "text", "image", "file", "post")
     * @return true if mention is required
     */
    public boolean shouldRequireMention(boolean isDirectMessage, String messageType) {
        FeishuPolicyResolver.ReplyPolicy replyPolicy = FeishuPolicyResolver.resolveReplyPolicy(
                isDirectMessage,
                policy,
                getGroupConfig(),
                messageType
        );
        return replyPolicy.isRequireMention();
    }

    /**
     * Checks if a message should require mention (for text messages).
     *
     * @param isDirectMessage whether this is a direct message
     * @return true if mention is required
     */
    public boolean shouldRequireMention(boolean isDirectMessage) {
        return shouldRequireMention(isDirectMessage, "text");
    }

    /**
     * Checks if a sender is allowed based on policy.
     *
     * @param senderId the sender ID
     * @param senderName the sender name (optional)
     * @return true if allowed
     */
    public boolean isSenderAllowed(String senderId, Optional<String> senderName) {
        FeishuGroupPolicy groupPolicy = getGroupConfig()
                .map(FeishuGroupConfig::getGroupPolicy)
                .orElse(policy.getGroupPolicy());

        List<String> allowFrom = getGroupConfig()
                .map(FeishuGroupConfig::getAllowFrom)
                .orElse(policy.getAllowFrom());

        return FeishuPolicyResolver.isGroupAllowed(
                groupPolicy,
                allowFrom,
                senderId,
                senderName.orElse(null)
        );
    }

    /**
     * Checks if the bot is mentioned in the message content.
     *
     * @param content the message content
     * @param botOpenId the bot's Open ID
     * @return true if mentioned
     */
    public boolean isBotMentioned(String content, String botOpenId) {
        if (content == null || botOpenId == null) {
            return false;
        }
        // Check for at mention in content
        String mentionPattern = "<at user_id=\"" + botOpenId + "\">";
        return content.contains(mentionPattern);
    }

    /**
     * Strips bot mention from message content.
     *
     * @param content the message content
     * @param botOpenId the bot's Open ID
     * @return content with mention removed
     */
    public String stripBotMention(String content, String botOpenId) {
        if (content == null || botOpenId == null) {
            return content;
        }
        // Remove the mention tag
        String mentionPattern = "<at user_id=\"" + botOpenId + "\">[^<]*</at>\\s*";
        return content.replaceAll(mentionPattern, "").trim();
    }

    /**
     * Formats a mention for text messages.
     *
     * @param userId the user ID
     * @param userName the user name
     * @return formatted mention
     */
    public String formatMentionForText(String userId, String userName) {
        return formatMention(userId, Optional.ofNullable(userName));
    }

    /**
     * Formats @everyone mention for text messages.
     *
     * @return formatted mention
     */
    public String formatMentionAllForText() {
        return "<at user_id=\"all\">Everyone</at>";
    }

    /**
     * Formats a mention for card messages (lark_md format).
     *
     * @param userId the user ID
     * @return formatted mention
     */
    public String formatMentionForCard(String userId) {
        return "<at id=" + userId + "></at>";
    }

    /**
     * Formats @everyone mention for card messages.
     *
     * @return formatted mention
     */
    public String formatMentionAllForCard() {
        return "<at id=all></at>";
    }

    /**
     * Builds a message with mentions prepended.
     *
     * @param userIds the user IDs to mention
     * @param userNames the user names (parallel to userIds)
     * @param message the message content
     * @return message with mentions
     */
    public String buildMentionedMessage(List<String> userIds, List<String> userNames, String message) {
        if (userIds == null || userIds.isEmpty()) {
            return message;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            String userName = i < userNames.size() ? userNames.get(i) : "User";
            sb.append(formatMentionForText(userId, userName));
            sb.append(" ");
        }
        sb.append(message);

        return sb.toString();
    }
}
