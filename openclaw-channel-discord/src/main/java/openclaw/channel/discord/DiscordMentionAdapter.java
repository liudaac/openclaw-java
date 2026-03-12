package openclaw.channel.discord;

import openclaw.sdk.channel.ChannelMentionAdapter;
import openclaw.sdk.channel.MentionParseResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discord Mention Adapter
 */
public class DiscordMentionAdapter implements ChannelMentionAdapter {

    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("<@!?(\\d+)>");
    private static final Pattern ROLE_MENTION_PATTERN = Pattern.compile("<@&(\\d+)>");
    private static final Pattern CHANNEL_MENTION_PATTERN = Pattern.compile("<#(\\d+)>");

    @Override
    public CompletableFuture<MentionParseResult> parse(String text) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder parsed = new StringBuilder(text);

            // Parse user mentions
            Matcher userMatcher = USER_MENTION_PATTERN.matcher(parsed);
            while (userMatcher.find()) {
                String userId = userMatcher.group(1);
                // In a real implementation, fetch user info from Discord API
                parsed.replace(userMatcher.start(), userMatcher.end(), "@" + userId);
            }

            // Parse role mentions
            Matcher roleMatcher = ROLE_MENTION_PATTERN.matcher(parsed);
            while (roleMatcher.find()) {
                String roleId = roleMatcher.group(1);
                parsed.replace(roleMatcher.start(), roleMatcher.end(), "@role:" + roleId);
            }

            // Parse channel mentions
            Matcher channelMatcher = CHANNEL_MENTION_PATTERN.matcher(parsed);
            while (channelMatcher.find()) {
                String channelId = channelMatcher.group(1);
                parsed.replace(channelMatcher.start(), channelMatcher.end(), "#" + channelId);
            }

            return new MentionParseResult(parsed.toString(), true, Optional.empty());
        });
    }

    @Override
    public CompletableFuture<String> formatMention(String userId, Optional<String> displayName) {
        return CompletableFuture.completedFuture("<@" + userId + ">");
    }

    @Override
    public CompletableFuture<String> formatChannelMention(String channelId) {
        return CompletableFuture.completedFuture("<#" + channelId + ">");
    }

    @Override
    public CompletableFuture<String> formatRoleMention(String roleId) {
        return CompletableFuture.completedFuture("<@&" + roleId + ">");
    }
}
