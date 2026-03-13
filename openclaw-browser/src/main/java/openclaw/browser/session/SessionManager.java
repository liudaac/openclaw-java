package openclaw.browser.session;

import com.microsoft.playwright.*;
import openclaw.browser.model.BrowserSession;
import openclaw.browser.model.BrowserSession.SessionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages browser sessions using Playwright.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
@Service
public class SessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    
    private Playwright playwright;
    private Browser browser;
    private final Map<String, BrowserSession> sessions;
    
    public SessionManager() {
        this.sessions = new ConcurrentHashMap<>();
    }
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing Playwright...");
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(true)
            .setArgs(java.util.List.of(
                "--disable-dev-shm-usage",
                "--no-sandbox",
                "--disable-setuid-sandbox"
            ))
        );
        logger.info("Playwright initialized successfully");
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Playwright...");
        sessions.values().forEach(BrowserSession::close);
        sessions.clear();
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        logger.info("Playwright shutdown complete");
    }
    
    /**
     * Create a new browser session.
     */
    public BrowserSession createSession(String profile, SessionOptions options) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(options.viewportWidth(), options.viewportHeight())
            .setUserAgent(options.userAgent())
            .setLocale(options.locale())
            .setTimezoneId(options.timezone())
            .setExtraHTTPHeaders(options.extraHeaders())
        );
        
        BrowserSession session = new BrowserSession(profile, context, options);
        sessions.put(session.getId(), session);
        
        logger.info("Created browser session: {} (profile: {})", session.getId(), profile);
        return session;
    }
    
    /**
     * Get a session by ID.
     */
    public Optional<BrowserSession> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
    
    /**
     * Close a session.
     */
    public void closeSession(String sessionId) {
        BrowserSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
            logger.info("Closed browser session: {}", sessionId);
        }
    }
    
    /**
     * List all active sessions.
     */
    public Map<String, BrowserSession> listSessions() {
        return Map.copyOf(sessions);
    }
    
    /**
     * Get session count.
     */
    public int getSessionCount() {
        return sessions.size();
    }
    
    /**
     * Close all sessions.
     */
    public void closeAllSessions() {
        sessions.values().forEach(BrowserSession::close);
        sessions.clear();
        logger.info("All browser sessions closed");
    }
}
