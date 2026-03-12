package openclaw.sdk.channel;

import java.util.Optional;

/**
 * Result of uploading media.
 *
 * @param success whether the upload was successful
 * @param mediaId the media ID if successful
 * @param url the media URL if available
 * @param error the error message if failed
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record MediaUploadResult(
        boolean success,
        Optional<String> mediaId,
        Optional<String> url,
        Optional<String> error
) {

    /**
     * Creates a successful upload result.
     *
     * @param mediaId the media ID
     * @return the result
     */
    public static MediaUploadResult success(String mediaId) {
        return new MediaUploadResult(true, Optional.of(mediaId), Optional.empty(), Optional.empty());
    }

    /**
     * Creates a successful upload result with URL.
     *
     * @param mediaId the media ID
     * @param url the media URL
     * @return the result
     */
    public static MediaUploadResult successWithUrl(String mediaId, String url) {
        return new MediaUploadResult(true, Optional.of(mediaId), Optional.of(url), Optional.empty());
    }

    /**
     * Creates a failed upload result.
     *
     * @param error the error message
     * @return the result
     */
    public static MediaUploadResult failure(String error) {
        return new MediaUploadResult(false, Optional.empty(), Optional.empty(), Optional.of(error));
    }
}
