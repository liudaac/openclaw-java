package openclaw.browser.model;

import com.microsoft.playwright.BrowserContext;

/**
 * Browser session options.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
public record SessionOptions(
        String viewport,
        String userAgent,
        java.util.Map<String, String> extraHeaders
) {
    public SessionOptions {
        extraHeaders = extraHeaders != null ? java.util.Map.copyOf(extraHeaders) : java.util.Map.of();
    }

    public SessionOptions() {
        this(null, null, java.util.Map.of());
    }

    public SessionOptions(String viewport) {
        this(viewport, null, java.util.Map.of());
    }
}
