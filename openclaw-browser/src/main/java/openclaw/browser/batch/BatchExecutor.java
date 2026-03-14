package openclaw.browser.batch;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import openclaw.browser.action.BrowserActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Executes batch browser actions.
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public class BatchExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchExecutor.class);
    
    public static final int MAX_BATCH_DEPTH = 5;
    public static final int MAX_BATCH_ACTIONS = 100;
    
    private final Page page;
    private final int depth;
    
    public BatchExecutor(Page page) {
        this(page, 0);
    }
    
    private BatchExecutor(Page page, int depth) {
        this.page = page;
        this.depth = depth;
    }
    
    /**
     * Execute a batch of actions.
     */
    public BatchResult execute(List<BatchAction> actions, boolean stopOnError) {
        if (depth > MAX_BATCH_DEPTH) {
            throw new IllegalArgumentException("Batch nesting depth exceeds maximum of " + MAX_BATCH_DEPTH);
        }
        if (actions.size() > MAX_BATCH_ACTIONS) {
            throw new IllegalArgumentException("Batch exceeds maximum of " + MAX_BATCH_ACTIONS + " actions");
        }
        
        List<ActionResult> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < actions.size(); i++) {
            BatchAction action = actions.get(i);
            long actionStart = System.currentTimeMillis();
            
            try {
                // Validate action
                action.validate();
                
                // Execute action
                Object result = executeSingleAction(action);
                long duration = System.currentTimeMillis() - actionStart;
                results.add(ActionResult.success(result, duration));
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - actionStart;
                String error = e.getMessage();
                results.add(ActionResult.failure(error, duration));
                
                logger.warn("Batch action {} failed: {}", i, error);
                
                if (stopOnError) {
                    break;
                }
            }
        }
        
        long totalDuration = System.currentTimeMillis() - startTime;
        int completed = (int) results.stream().filter(ActionResult::ok).count();
        int failed = results.size() - completed;
        
        return new BatchResult(results, completed, failed, totalDuration);
    }
    
    /**
     * Execute a single action.
     */
    private Object executeSingleAction(BatchAction action) throws Exception {
        BrowserActions actions = new BrowserActions(page);
        
        switch (action.kind()) {
            case "navigate" -> {
                String url = (String) action.extra().get("url");
                int timeout = action.timeoutMs() != null ? action.timeoutMs() : 30000;
                actions.navigate(url, timeout).join();
                return null;
            }
            
            case "click" -> {
                Locator locator = resolveLocator(action);
                int timeout = action.timeoutMs() != null ? action.timeoutMs() : 30000;
                
                // Handle delay
                if (action.delayMs() != null && action.delayMs() > 0) {
                    locator.hover(new Locator.HoverOptions().setTimeout(timeout));
                    Thread.sleep(action.delayMs());
                }
                
                // Handle double click
                if (action.doubleClick() != null && action.doubleClick()) {
                    locator.dblclick(new Locator.DblclickOptions().setTimeout(timeout));
                } else {
                    Locator.ClickOptions options = new Locator.ClickOptions().setTimeout(timeout);
                    if (action.button() != null) {
                        options.setButton(action.button());
                    }
                    if (action.modifiers() != null) {
                        // Convert modifiers to Playwright format
                        List<String> modifiers = List.of(action.modifiers());
                        // Playwright Java API doesn't have direct modifier support in same way
                        // This is simplified
                    }
                    locator.click(options);
                }
                return null;
            }
            
            case "type" -> {
                Locator locator = resolveLocator(action);
                int timeout = action.timeoutMs() != null ? action.timeoutMs() : 30000;
                
                if (action.slowly() != null && action.slowly()) {
                    locator.pressSequentially(action.text(), 
                        new Locator.PressSequentiallyOptions().setTimeout(timeout));
                } else {
                    locator.fill(action.text(), new Locator.FillOptions().setTimeout(timeout));
                }
                
                if (action.submit() != null && action.submit()) {
                    locator.press("Enter");
                }
                return null;
            }
            
            case "fill" -> {
                Locator locator = resolveLocator(action);
                int timeout = action.timeoutMs() != null ? action.timeoutMs() : 30000;
                locator.fill(action.text(), new Locator.FillOptions().setTimeout(timeout));
                return null;
            }
            
            case "select" -> {
                Locator locator = resolveLocator(action);
                int timeout = action.timeoutMs() != null ? action.timeoutMs() : 30000;
                locator.selectOption(action.value(), new Locator.SelectOptionOptions().setTimeout(timeout));
                return null;
            }
            
            case "hover" -> {
                Locator locator = resolveLocator(action);
                int timeout = action.timeoutMs() != null ? action.timeoutMs() : 30000;
                locator.hover(new Locator.HoverOptions().setTimeout(timeout));
                return null;
            }
            
            case "scroll" -> {
                String direction = action.direction();
                int amount = action.amount();
                
                String script = switch (direction.toLowerCase()) {
                    case "up" -> "window.scrollBy(0, -" + amount + ")";
                    case "down" -> "window.scrollBy(0, " + amount + ")";
                    case "left" -> "window.scrollBy(-" + amount + ", 0)";
                    case "right" -> "window.scrollBy(" + amount + ", 0)";
                    default -> throw new IllegalArgumentException("Invalid direction: " + direction);
                };
                
                page.evaluate(script);
                return null;
            }
            
            case "drag" -> {
                Locator startLocator = resolveLocator(action.startSelector(), action.startRef());
                Locator endLocator = resolveLocator(action.endSelector(), action.endRef());
                int timeout = action.timeoutMs() != null ? action.timeoutMs() : 30000;
                startLocator.dragTo(endLocator, new Locator.DragToOptions().setTimeout(timeout));
                return null;
            }
            
            case "press" -> {
                page.keyboard().press(action.key());
                return null;
            }
            
            case "evaluate" -> {
                return page.evaluate(action.script());
            }
            
            case "wait" -> {
                Thread.sleep(action.timeMs());
                return null;
            }
            
            case "resize" -> {
                page.setViewportSize(action.width(), action.height());
                return null;
            }
            
            case "close" -> {
                page.close();
                return null;
            }
            
            case "screenshot" -> {
                if (action.selector() != null) {
                    Locator locator = page.locator(action.selector());
                    byte[] bytes = locator.screenshot();
                    return Base64.getEncoder().encodeToString(bytes);
                } else {
                    byte[] bytes = page.screenshot();
                    return Base64.getEncoder().encodeToString(bytes);
                }
            }
            
            default -> throw new IllegalArgumentException("Unsupported action kind: " + action.kind());
        }
    }
    
    /**
     * Resolve locator from action.
     */
    private Locator resolveLocator(BatchAction action) {
        return resolveLocator(action.selector(), action.ref());
    }
    
    /**
     * Resolve locator from selector or ref.
     */
    private Locator resolveLocator(String selector, String ref) {
        if (selector != null) {
            return page.locator(selector);
        }
        if (ref != null) {
            // ARIA ref resolution - simplified
            // In real implementation, would use refLocator from snapshot system
            return page.locator("[data-ref='" + ref + "']");
        }
        throw new IllegalArgumentException("Either selector or ref must be provided");
    }
}
