package openclaw.session.binding;

import java.util.Objects;

/**
 * Reference to a conversation.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class ConversationRef {

    private final String channel;
    private final String accountId;
    private final String conversationId;
    private final String parentConversationId;

    private ConversationRef(Builder builder) {
        this.channel = Objects.requireNonNull(builder.channel, "channel cannot be null").trim().toLowerCase();
        this.accountId = normalizeAccountId(builder.accountId);
        this.conversationId = Objects.requireNonNull(builder.conversationId, "conversationId cannot be null").trim();
        this.parentConversationId = builder.parentConversationId != null ? builder.parentConversationId.trim() : null;
    }

    public String getChannel() {
        return channel;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getParentConversationId() {
        return parentConversationId;
    }

    private static String normalizeAccountId(String accountId) {
        if (accountId == null) {
            return "default";
        }
        String trimmed = accountId.trim();
        return trimmed.isEmpty() ? "default" : trimmed.toLowerCase();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationRef that = (ConversationRef) o;
        return Objects.equals(channel, that.channel) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(conversationId, that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, accountId, conversationId);
    }

    @Override
    public String toString() {
        return channel + ":" + accountId + ":" + conversationId;
    }

    public static class Builder {
        private String channel;
        private String accountId;
        private String conversationId;
        private String parentConversationId;

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder parentConversationId(String parentConversationId) {
            this.parentConversationId = parentConversationId;
            return this;
        }

        public ConversationRef build() {
            return new ConversationRef(this);
        }
    }
}
