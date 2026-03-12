package openclaw.channel.telegram;

import openclaw.sdk.channel.ChannelMentionAdapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Telegram mention adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class TelegramMentionAdapter implements ChannelMentionAdapter {

    @Override
    public String formatMention(String userId, Optional<String> userName) {
        if (userName.isPresent()) {
            return "@" + userName.get();
        }
        return "<a href=\"tg://user?id=" + userId + "\">User</a>";
    }

    @Override
    public List<ParsedMention> parseMentions(String text) {
        List<ParsedMention> mentions = new java.util.ArrayList<>();
        
        // Parse @username mentions
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([a-zA-Z0-9_]{5,32})");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            mentions.add(new ParsedMention(
                    matcher.group(1),
                    Optional.of(matcher.group(1)),
                    matcher.start(),
                    matcher.end()
            ));
        }
        
        return mentions;
    }

    @Override
    public CompletableFuture<Optional<String>> resolveMention(String mention) {
        // Telegram @username to user ID resolution requires API call
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
