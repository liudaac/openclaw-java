package openclaw.channel.discord;

import openclaw.sdk.channel.ChannelMentionAdapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Discord mention adapter.
 */
public class DiscordMentionAdapter implements ChannelMentionAdapter {

    @Override
    public String formatMention(String userId, Optional<String> userName) {
        return "<@" + userId + ">";
    }

    @Override
    public List<ParsedMention> parseMentions(String text) {
        List<ParsedMention> mentions = new java.util.ArrayList<>();

        // Parse <@userId> mentions
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<@!?(\\d+)>");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String userId = matcher.group(1);
            mentions.add(new ParsedMention(
                    userId,
                    Optional.empty(),
                    matcher.start(),
                    matcher.end()
            ));
        }

        return mentions;
    }

    @Override
    public CompletableFuture<Optional<String>> resolveMention(String mention) {
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
