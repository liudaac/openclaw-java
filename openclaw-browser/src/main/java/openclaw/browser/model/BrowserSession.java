package openclaw.browser.model;

import com.microsoft.playwright.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Browser session managing Playwright browser context and pages.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class BrowserSession {
    
    private final String id;
    private final String profile;
    private final BrowserContext context;
    private final Map<String, Page> pages;
    private final Instant createdAt;
    private volatile String currentPageId;
    private final SessionOptions options;
    
    public BrowserSession(String profile, BrowserContext context, SessionOptions options) {
        this.id = UUID.randomUUID().toString();
        this.profile = profile;
        this.context = context;
        this.pages = new ConcurrentHashMap<>();
        this.createdAt = Instant.now();
        this.options = options;
    }
    
    public String getId() {
        return id;
    }
    
    public String getProfile() {
        return profile;
    }
    
    public BrowserContext getContext() {
        return context;
    }
    
    public Page createPage() {
        Page page = context.newPage();
        String pageId = UUID.randomUUID().toString();
        pages.put(pageId, page);
        currentPageId = pageId;
        
        // Set viewport if specified
        if (options.viewportWidth() > 0 && options.viewportHeight() > 0) {
            page.setViewportSize(options.viewportWidth(), options.viewportHeight());
        }
        
        return page;
    }
    
    public Page getPage(String pageId) {
        return pages.get(pageId);
    }
    
    public Page getCurrentPage() {
        if (currentPageId == null) {
            return null;
        }
        return pages.get(currentPageId);
    }
    
    public void setCurrentPage(String pageId) {
        if (pages.containsKey(pageId)) {
            this.currentPageId = pageId;
        }
    }
    
    public void closePage(String pageId) {
        Page page = pages.remove(pageId);
        if (page != null) {
            page.close();
        }
        if (pageId.equals(currentPageId)) {
            currentPageId = pages.isEmpty() ? null : pages.keySet().iterator().next();
        }
    }
    
    public Map<String, Page> getPages() {
        return Map.copyOf(pages);
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public SessionOptions getOptions() {
        return options;
    }

    public String getName() {
        return profile;
    }

    public SessionStatus getStatus() {
        return context != null ? SessionStatus.ACTIVE : SessionStatus.CLOSED;
    }
    
    public void close() {
        pages.values().forEach(Page::close);
        pages.clear();
        context.close();
    }
    
    public record SessionOptions(
        int viewportWidth,
        int viewportHeight,
        String userAgent,
        String locale,
        String timezone,
        boolean headless,
        Map<String, String> extraHeaders
    ) {
        public static SessionOptions defaults() {
            return new SessionOptions(1280, 720, null, "en-US", "UTC", true, Map.of());
        }
    }

    public enum SessionStatus {
        ACTIVE, CLOSED, ERROR
    }
}
