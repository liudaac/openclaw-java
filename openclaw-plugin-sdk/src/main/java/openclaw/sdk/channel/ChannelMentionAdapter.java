package openclaw.sdk.channel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel mention adapter for user mentions.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelMentionAdapter {

    /**
     * Formats a user mention.
     *
     * @param userId the user ID
     * @param userName the user name
     * @return the formatted mention
     */
    String formatMention(String userId, Optional<String> userName);

    /**
     * Parses mentions from text.
     *
     * @param text the text
     * @return list of parsed mentions
     */
    List<ParsedMention> parseMentions(String text);

    /**
     * Resolves a mention to a user ID.
     *
     * @param mention the mention text
     * @return the user ID if resolved
     */
    CompletableFuture<Optional<String>> resolveMention(String mention);

    /**
     * Parsed mention.
     *
     * @param userId the user ID
     * @param userName the user name
     * @param startIndex start index in text
     * @param endIndex end index in text
     */
    record ParsedMention(
            String userId,
            Optional<String> userName,
            int startIndex,
            int endIndex
    ) {
    }
}
