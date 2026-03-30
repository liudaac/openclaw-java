package openclaw.browser.model;

import java.util.Map;

/**
 * Browser session options.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
public class SessionOptions {
    
    private String viewport;
    private String userAgent;
    private Map<String, String> extraHeaders;
    private int viewportWidth;
    private int viewportHeight;
    
    public SessionOptions() {
        this.viewportWidth = 0;
        this.viewportHeight = 0;
        this.extraHeaders = Map.of();
    }
    
    public SessionOptions(String viewport) {
        this.viewport = viewport;
        this.viewportWidth = 0;
        this.viewportHeight = 0;
        this.extraHeaders = Map.of();
    }
    
    public SessionOptions(String viewport, String userAgent, Map<String, String> extraHeaders) {
        this.viewport = viewport;
        this.userAgent = userAgent;
        this.extraHeaders = extraHeaders != null ? extraHeaders : Map.of();
        parseViewport();
    }
    
    private void parseViewport() {
        if (viewport != null && viewport.contains("x")) {
            String[] parts = viewport.split("x");
            try {
                viewportWidth = Integer.parseInt(parts[0].trim());
                viewportHeight = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                viewportWidth = 0;
                viewportHeight = 0;
            }
        }
    }
    
    public String getViewport() {
        return viewport;
    }
    
    public void setViewport(String viewport) {
        this.viewport = viewport;
        parseViewport();
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public Map<String, String> getExtraHeaders() {
        return extraHeaders;
    }
    
    public void setExtraHeaders(Map<String, String> extraHeaders) {
        this.extraHeaders = extraHeaders != null ? extraHeaders : Map.of();
    }
    
    public int viewportWidth() {
        return viewportWidth;
    }
    
    public int viewportHeight() {
        return viewportHeight;
    }
}
