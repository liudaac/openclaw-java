package openclaw.sdk.channel;

/**
 * Channel capabilities indicating supported features.
 *
 * @param supportsText whether text messages are supported
 * @param supportsImages whether image messages are supported
 * @param supportsFiles whether file attachments are supported
 * @param supportsVoice whether voice messages are supported
 * @param supportsVideo whether video messages are supported
 * @param supportsTyping whether typing indicators are supported
 * @param supportsReactions whether message reactions are supported
 * @param supportsThreads whether threaded conversations are supported
 * @param supportsEditing whether message editing is supported
 * @param supportsDeleting whether message deletion is supported
 * @param supportsMarkdown whether Markdown formatting is supported
 * @param supportsHtml whether HTML formatting is supported
 * @param supportsMentions whether user mentions are supported
 * @param supportsGroups whether group chats are supported
 * @param supportsDMs whether direct messages are supported
 * @param supportsPolling whether message polling is supported
 * @param supportsStreaming whether streaming responses are supported
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ChannelCapabilities(
        boolean supportsText,
        boolean supportsImages,
        boolean supportsFiles,
        boolean supportsVoice,
        boolean supportsVideo,
        boolean supportsTyping,
        boolean supportsReactions,
        boolean supportsThreads,
        boolean supportsEditing,
        boolean supportsDeleting,
        boolean supportsMarkdown,
        boolean supportsHtml,
        boolean supportsMentions,
        boolean supportsGroups,
        boolean supportsDMs,
        boolean supportsPolling,
        boolean supportsStreaming
) {

    /**
     * Creates a builder with all capabilities disabled.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a basic capability set with text support.
     *
     * @return basic capabilities
     */
    public static ChannelCapabilities basic() {
        return builder()
                .supportsText(true)
                .build();
    }

    /**
     * Builder for ChannelCapabilities.
     */
    public static class Builder {
        private boolean supportsText = false;
        private boolean supportsImages = false;
        private boolean supportsFiles = false;
        private boolean supportsVoice = false;
        private boolean supportsVideo = false;
        private boolean supportsTyping = false;
        private boolean supportsReactions = false;
        private boolean supportsThreads = false;
        private boolean supportsEditing = false;
        private boolean supportsDeleting = false;
        private boolean supportsMarkdown = false;
        private boolean supportsHtml = false;
        private boolean supportsMentions = false;
        private boolean supportsGroups = false;
        private boolean supportsDMs = false;
        private boolean supportsPolling = false;
        private boolean supportsStreaming = false;

        public Builder supportsText(boolean supports) {
            this.supportsText = supports;
            return this;
        }

        public Builder supportsImages(boolean supports) {
            this.supportsImages = supports;
            return this;
        }

        public Builder supportsFiles(boolean supports) {
            this.supportsFiles = supports;
            return this;
        }

        public Builder supportsVoice(boolean supports) {
            this.supportsVoice = supports;
            return this;
        }

        public Builder supportsVideo(boolean supports) {
            this.supportsVideo = supports;
            return this;
        }

        public Builder supportsTyping(boolean supports) {
            this.supportsTyping = supports;
            return this;
        }

        public Builder supportsReactions(boolean supports) {
            this.supportsReactions = supports;
            return this;
        }

        public Builder supportsThreads(boolean supports) {
            this.supportsThreads = supports;
            return this;
        }

        public Builder supportsEditing(boolean supports) {
            this.supportsEditing = supports;
            return this;
        }

        public Builder supportsDeleting(boolean supports) {
            this.supportsDeleting = supports;
            return this;
        }

        public Builder supportsMarkdown(boolean supports) {
            this.supportsMarkdown = supports;
            return this;
        }

        public Builder supportsHtml(boolean supports) {
            this.supportsHtml = supports;
            return this;
        }

        public Builder supportsMentions(boolean supports) {
            this.supportsMentions = supports;
            return this;
        }

        public Builder supportsGroups(boolean supports) {
            this.supportsGroups = supports;
            return this;
        }

        public Builder supportsDMs(boolean supports) {
            this.supportsDMs = supports;
            return this;
        }

        public Builder supportsPolling(boolean supports) {
            this.supportsPolling = supports;
            return this;
        }

        public Builder supportsStreaming(boolean supports) {
            this.supportsStreaming = supports;
            return this;
        }

        public ChannelCapabilities build() {
            return new ChannelCapabilities(
                    supportsText,
                    supportsImages,
                    supportsFiles,
                    supportsVoice,
                    supportsVideo,
                    supportsTyping,
                    supportsReactions,
                    supportsThreads,
                    supportsEditing,
                    supportsDeleting,
                    supportsMarkdown,
                    supportsHtml,
                    supportsMentions,
                    supportsGroups,
                    supportsDMs,
                    supportsPolling,
                    supportsStreaming
            );
        }
    }
}
