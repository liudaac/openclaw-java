package openclaw.session.binding;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adapter for session binding operations.
 *
 * <p>Channel implementations can provide this adapter to support
 * session binding functionality.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public interface SessionBindingAdapter {

    /**
     * Gets the channel name.
     *
     * @return the channel
     */
    String getChannel();

    /**
     * Gets the account ID.
     *
     * @return the account ID
     */
    String getAccountId();

    /**
     * Checks if bind is supported.
     *
     * @return true if supported
     */
    boolean isBindSupported();

    /**
     * Checks if unbind is supported.
     *
     * @return true if supported
     */
    boolean isUnbindSupported();

    /**
     * Gets supported placements.
     *
     * @return list of placements
     */
    List<SessionBindingPlacement> getPlacements();

    /**
     * Lists bindings by session.
     *
     * @param targetSessionKey the target session key
     * @return list of binding records
     */
    List<SessionBindingRecord> listBySession(String targetSessionKey);

    /**
     * Resolves binding by conversation.
     *
     * @param ref the conversation reference
     * @return the binding record or null
     */
    SessionBindingRecord resolveByConversation(ConversationRef ref);
}
