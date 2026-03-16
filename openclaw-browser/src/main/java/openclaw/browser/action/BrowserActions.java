package openclaw.browser.action;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import openclaw.browser.snapshot.PageSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Browser actions wrapper for Playwright.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class BrowserActions {
    
    private static final Logger logger = LoggerFactory.getLogger(BrowserActions.class);
    
    private final Page page;
    
    public BrowserActions(Page page) {
        this.page = page;
    }
    
    // Navigation
    
    public CompletableFuture<Void> navigate(String url) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Navigating to: {}", url);
            page.navigate(url);
        });
    }
    
    public CompletableFuture<Void> navigate(String url, int timeoutMs) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Navigating to: {} (timeout: {}ms)", url, timeoutMs);
            page.navigate(url, new Page.NavigateOptions().setTimeout(timeoutMs));
        });
    }
    
    public CompletableFuture<Void> goBack() {
        return CompletableFuture.runAsync(() -> page.goBack());
    }
    
    public CompletableFuture<Void> goForward() {
        return CompletableFuture.runAsync(() -> page.goForward());
    }
    
    public CompletableFuture<Void> reload() {
        return CompletableFuture.runAsync(() -> page.reload());
    }
    
    // Element Interaction
    
    public CompletableFuture<Void> click(String selector) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Clicking element: {}", selector);
            page.click(selector);
        });
    }
    
    public CompletableFuture<Void> click(String selector, int timeoutMs) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Clicking element: {} (timeout: {}ms)", selector, timeoutMs);
            page.click(selector, new Page.ClickOptions().setTimeout(timeoutMs));
        });
    }
    
    public CompletableFuture<Void> type(String selector, String text) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Typing into element: {}", selector);
            page.fill(selector, text);
        });
    }
    
    public CompletableFuture<Void> type(String selector, String text, int delayMs) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Typing into element: {} (delay: {}ms)", selector, delayMs);
            page.type(selector, text, new Page.TypeOptions().setDelay(delayMs));
        });
    }
    
    public CompletableFuture<Void> fill(String selector, String text) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Filling element: {}", selector);
            page.fill(selector, text);
        });
    }
    
    public CompletableFuture<Void> clear(String selector) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Clearing element: {}", selector);
            page.fill(selector, "");
        });
    }
    
    public CompletableFuture<Void> select(String selector, String value) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Selecting option: {} = {}", selector, value);
            page.selectOption(selector, value);
        });
    }
    
    public CompletableFuture<Void> select(String selector, List<String> values) {
        return CompletableFuture.runAsync(() -> {
            page.selectOption(selector, values.toArray(new String[0]));
        });
    }
    
    public CompletableFuture<Void> hover(String selector) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Hovering over element: {}", selector);
            page.hover(selector);
        });
    }
    
    public CompletableFuture<Void> focus(String selector) {
        return CompletableFuture.runAsync(() -> {
            page.focus(selector);
        });
    }
    
    public CompletableFuture<Void> scroll(String direction, int amount) {
        return CompletableFuture.runAsync(() -> {
            String script = switch (direction.toLowerCase()) {
                case "up" -> "window.scrollBy(0, -" + amount + ")";
                case "down" -> "window.scrollBy(0, " + amount + ")";
                case "left" -> "window.scrollBy(-" + amount + ", 0)";
                case "right" -> "window.scrollBy(" + amount + ", 0)";
                default -> "window.scrollBy(0, " + amount + ")";
            };
            page.evaluate(script);
        });
    }
    
    public CompletableFuture<Void> scrollTo(String selector) {
        return CompletableFuture.runAsync(() -> {
            page.evaluate("document.querySelector('" + selector + "').scrollIntoView()");
        });
    }
    
    // Waiting
    
    public CompletableFuture<Void> waitForSelector(String selector, int timeoutMs) {
        return CompletableFuture.runAsync(() -> {
            page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
        });
    }
    
    public CompletableFuture<Void> waitForNavigation(int timeoutMs) {
        return CompletableFuture.runAsync(() -> {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(timeoutMs));
        });
    }
    
    public CompletableFuture<Void> waitForLoadState(String state) {
        return CompletableFuture.runAsync(() -> {
            LoadState loadState = LoadState.valueOf(state.toUpperCase());
            page.waitForLoadState(loadState);
        });
    }
    
    public CompletableFuture<Void> waitForTimeout(int timeoutMs) {
        return CompletableFuture.runAsync(() -> {
            page.waitForTimeout(timeoutMs);
        });
    }
    
    // Screenshots
    
    public CompletableFuture<byte[]> screenshot() {
        return CompletableFuture.supplyAsync(() -> {
            return page.screenshot();
        });
    }
    
    public CompletableFuture<byte[]> screenshot(Page.ScreenshotOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            return page.screenshot(options);
        });
    }
    
    public CompletableFuture<byte[]> screenshot(String selector) {
        return CompletableFuture.supplyAsync(() -> {
            ElementHandle element = page.querySelector(selector);
            if (element == null) {
                throw new RuntimeException("Element not found: " + selector);
            }
            return element.screenshot();
        });
    }
    
    public CompletableFuture<Void> screenshot(Path path) {
        return CompletableFuture.runAsync(() -> {
            page.screenshot(new Page.ScreenshotOptions().setPath(path));
        });
    }
    
    // JavaScript Execution
    
    public CompletableFuture<Object> evaluate(String script) {
        return CompletableFuture.supplyAsync(() -> {
            return page.evaluate(script);
        });
    }
    
    public CompletableFuture<Object> evaluate(String script, Object arg) {
        return CompletableFuture.supplyAsync(() -> {
            return page.evaluate(script, arg);
        });
    }
    
    // Content
    
    public CompletableFuture<String> getText(String selector) {
        return CompletableFuture.supplyAsync(() -> {
            return page.textContent(selector);
        });
    }
    
    public CompletableFuture<String> getAttribute(String selector, String attribute) {
        return CompletableFuture.supplyAsync(() -> {
            return page.getAttribute(selector, attribute);
        });
    }
    
    public CompletableFuture<String> getHtml() {
        return CompletableFuture.supplyAsync(() -> {
            return page.content();
        });
    }
    
    public CompletableFuture<String> getUrl() {
        return CompletableFuture.supplyAsync(() -> {
            return page.url();
        });
    }
    
    public CompletableFuture<String> getTitle() {
        return CompletableFuture.supplyAsync(() -> {
            return page.title();
        });
    }
    
    // Snapshot
    
    public CompletableFuture<PageSnapshot> getSnapshot() {
        return CompletableFuture.supplyAsync(() -> {
            return PageSnapshot.capture(page);
        });
    }
    
    // Keyboard
    
    public CompletableFuture<Void> press(String key) {
        return CompletableFuture.runAsync(() -> {
            page.keyboard().press(key);
        });
    }
    
    public CompletableFuture<Void> keyDown(String key) {
        return CompletableFuture.runAsync(() -> {
            page.keyboard().down(key);
        });
    }
    
    public CompletableFuture<Void> keyUp(String key) {
        return CompletableFuture.runAsync(() -> {
            page.keyboard().up(key);
        });
    }
}
