package openclaw.channel.feishu;

import openclaw.sdk.channel.ChannelMentionAdapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Feishu mention adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class FeishuMentionAdapter implements ChannelMentionAdapter {

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
}
}
