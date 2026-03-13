package openclaw.browser;

import com.microsoft.playwright.*;
import openclaw.browser.action.BrowserActions;
import openclaw.browser.model.BrowserSession;
import openclaw.browser.model.BrowserSession.SessionOptions;
import openclaw.browser.session.SessionManager;
import openclaw.browser.snapshot.PageSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main service for browser automation.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
@Service
public class BrowserService {
    
    private static final Logger logger = LoggerFactory.getLogger(BrowserService.class);
    
    private final SessionManager sessionManager;
    
    public BrowserService(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    // Session Management
    
    public CompletableFuture<BrowserSession> createSession(String profile, SessionOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            return sessionManager.createSession(profile, options);
        });
    }
    
    public CompletableFuture<Optional<BrowserSession>> getSession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            return sessionManager.getSession(sessionId);
        });
    }
    
    public CompletableFuture<Void> closeSession(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            sessionManager.closeSession(sessionId);
        });
    }
    
    public CompletableFuture<Map<String, BrowserSession>> listSessions() {
        return CompletableFuture.supplyAsync(() -> {
            return sessionManager.listSessions();
        });
    }
    
    // Page Management
    
    public CompletableFuture<Page> createPage(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            BrowserSession session = sessionManager.getSession(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            return session.createPage();
        });
    }
    
    public CompletableFuture<Page> getPage(String sessionId, String pageId) {
        return CompletableFuture.supplyAsync(() -> {
            BrowserSession session = sessionManager.getSession(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            Page page = session.getPage(pageId);
            if (page == null) {
                throw new IllegalArgumentException("Page not found: " + pageId);
            }
            return page;
        });
    }
    
    public CompletableFuture<Page> getCurrentPage(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            BrowserSession session = sessionManager.getSession(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            Page page = session.getCurrentPage();
            if (page == null) {
                throw new IllegalStateException("No active page in session: " + sessionId);
            }
            return page;
        });
    }
    
    public CompletableFuture<Void> closePage(String sessionId, String pageId) {
        return CompletableFuture.runAsync(() -> {
            BrowserSession session = sessionManager.getSession(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            session.closePage(pageId);
        });
    }
    
    // Actions
    
    public CompletableFuture<Void> navigate(String sessionId, String url) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).navigate(url));
    }
    
    public CompletableFuture<Void> navigate(String sessionId, String url, int timeoutMs) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).navigate(url, timeoutMs));
    }
    
    public CompletableFuture<Void> click(String sessionId, String selector) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).click(selector));
    }
    
    public CompletableFuture<Void> type(String sessionId, String selector, String text) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).type(selector, text));
    }
    
    public CompletableFuture<Void> fill(String sessionId, String selector, String text) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).fill(selector, text));
    }
    
    public CompletableFuture<Void> select(String sessionId, String selector, String value) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).select(selector, value));
    }
    
    public CompletableFuture<Void> hover(String sessionId, String selector) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).hover(selector));
    }
    
    public CompletableFuture<Void> scroll(String sessionId, String direction, int amount) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).scroll(direction, amount));
    }
    
    // Screenshots
    
    public CompletableFuture<byte[]> screenshot(String sessionId) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).screenshot());
    }
    
    public CompletableFuture<byte[]> screenshot(String sessionId, String selector) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).screenshot(selector));
    }
    
    public CompletableFuture<Void> screenshot(String sessionId, Path path) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).screenshot(path));
    }
    
    // JavaScript
    
    public CompletableFuture<Object> evaluate(String sessionId, String script) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).evaluate(script));
    }
    
    // Snapshot
    
    public CompletableFuture<PageSnapshot> getSnapshot(String sessionId) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).getSnapshot());
    }
    
    // Content
    
    public CompletableFuture<String> getText(String sessionId, String selector) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).getText(selector));
    }
    
    public CompletableFuture<String> getHtml(String sessionId) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).getHtml());
    }
    
    public CompletableFuture<String> getUrl(String sessionId) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).getUrl());
    }
    
    public CompletableFuture<String> getTitle(String sessionId) {
        return getCurrentPage(sessionId)
            .thenCompose(page -> new BrowserActions(page).getTitle());
    }
}
