package openclaw.sdk.channel;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Request for uploading media.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record MediaUploadRequest(
        String channelId,
        String accountId,
        Path filePath,
        String filename,
        Optional<String> mimeType,
        Optional<String> caption
) {

    /**
     * Creates a builder for MediaUploadRequest.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for MediaUploadRequest.
     */
    public static class Builder {
        private String channelId;
        private String accountId;
        private Path filePath;
        private String filename;
        private String mimeType;
        private String caption;

        public Builder channelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder filePath(Path filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder caption(String caption) {
            this.caption = caption;
            return this;
        }

        public MediaUploadRequest build() {
            return new MediaUploadRequest(
                    channelId,
                    accountId,
                    filePath,
                    filename,
                    Optional.ofNullable(mimeType),
                    Optional.ofNullable(caption)
            );
        }
    }
}
