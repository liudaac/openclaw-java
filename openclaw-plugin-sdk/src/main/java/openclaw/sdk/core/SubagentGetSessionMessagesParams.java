package openclaw.sdk.core;

import java.util.Optional;

/**
 * Parameters for getting session messages.
 *
 * @param sessionKey the session key
 * @param limit optional message limit
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record SubagentGetSessionMessagesParams(
        String sessionKey,
        Optional<Integer> limit
) {

    /**
     * Creates params with just a session key.
     *
     * @param sessionKey the session key
     * @return the params
     */
    public static SubagentGetSessionMessagesParams of(String sessionKey) {
        return new SubagentGetSessionMessagesParams(sessionKey, Optional.empty());
    }

    /**
     * Creates params with session key and limit.
     *
     * @param sessionKey the session key
     * @param limit the message limit
     * @return the params
     */
    public static SubagentGetSessionMessagesParams withLimit(String sessionKey, int limit) {
        return new SubagentGetSessionMessagesParams(sessionKey, Optional.of(limit));
    }
}
