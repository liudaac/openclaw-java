package openclaw.session.binding;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service for managing session bindings.
 *
 * <p>Provides operations for binding and unbinding sessions to conversations,
 * as well as querying binding information.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public interface SessionBindingService {

    /**
     * Binds a session to a conversation.
     *
     * @param input the bind input
     * @return Mono of the binding record
     * @throws SessionBindingException if binding fails
     */
    Mono<SessionBindingRecord> bind(SessionBindingBindInput input);

    /**
     * Gets capabilities for a channel/account.
     *
     * @param channel the channel
     * @param accountId the account ID
     * @return the capabilities
     */
    SessionBindingCapabilities getCapabilities(String channel, String accountId);

    /**
     * Lists bindings for a session.
     *
     * @param targetSessionKey the target session key
     * @return list of binding records
     */
    List<SessionBindingRecord> listBySession(String targetSessionKey);

    /**
     * Resolves a binding by conversation.
     *
     * @param ref the conversation reference
     * @return the binding record or null
     */
    SessionBindingRecord resolveByConversation(ConversationRef ref);

    /**
     * Touches a binding (updates last activity).
     *
     * @param bindingId the binding ID
     */
    void touch(String bindingId);

    /**
     * Unbinds sessions.
     *
     * @param input the unbind input
     * @return Mono of removed binding records
     */
    Mono<List<SessionBindingRecord>> unbind(SessionBindingUnbindInput input);
}
