package openclaw.security;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Security for CDP screenshot to prevent cross-origin image issues.
 */
public class CdpScreenshotSecurity {

    /**
     * Validate image URL for screenshot.
     * Prevents cross-origin images from disappearing.
     */
    public boolean isValidImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            
            // Only allow http and https
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }

            // Check for data URIs (can cause issues)
            if (url.startsWith("data:")) {
                return false;
            }

            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Sanitize image URL for CDP.
     */
    public String sanitizeImageUrl(String url) {
        if (!isValidImageUrl(url)) {
            return null;
        }
        return url;
    }
}
