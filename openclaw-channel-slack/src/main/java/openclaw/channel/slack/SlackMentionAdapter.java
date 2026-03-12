package openclaw.channel.slack;

import openclaw.sdk.channel.ChannelMentionAdapter;
import openclaw.sdk.channel.MentionParseResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slack Mention Adapter
 */
public class SlackMentionAdapter implements ChannelMentionAdapter {

    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("<@([A-Z0-9]+)>");
    private static final Pattern CHANNEL_MENTION_PATTERN = Pattern.compile("<#([A-Z0-9]+)>");
    private static final Pattern URL_PATTERN = Pattern.compile("<(https?://[^|]+)(?:\\|([^>]+))?>", Pattern.CASE_INSENSITIVE);

    @Override
    public CompletableFuture<MentionParseResult> parse(String text) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder parsed = new StringBuilder(text);

            // Parse user mentions
            Matcher userMatcher = USER_MENTION_PATTERN.matcher(parsed);
            while (userMatcher.find()) {
                String userId = userMatcher.group(1);
                parsed.replace(userMatcher.start(), userMatcher.end(), "@" + userId);
            }

            // Parse channel mentions
            Matcher channelMatcher = CHANNEL_MENTION_PATTERN.matcher(parsed);
            while (channelMatcher.find()) {
                String channelId = channelMatcher.group(1);
                parsed.replace(channelMatcher.start(), channelMatcher.end(), "#" + channelId);
            }

            // Parse URLs
            Matcher urlMatcher = URL_PATTERN.matcher(parsed);
            while (urlMatcher.find()) {
                String url = urlMatcher.group(1);
                String label = urlMatcher.group(2);
                String replacement = label != null ? label : url;
                parsed.replace(urlMatcher.start(), urlMatcher.end(), replacement);
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
        return CompletableFuture.completedFuture("<!subteam^" + roleId + ">");
    }

    /**
     * Format URL for Slack
     */
    public String formatUrl(String url, Optional<String> label) {
        if (label.isPresent()) {
            return "<" + url + "|" + label.get() + ">";
        }
        return "<" + url + ">";
    }
}
