package openclaw.browser.batch;

import java.util.Map;

/**
 * Single action within a batch operation.
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public record BatchAction(
    String kind,           // click, type, fill, select, hover, scroll, drag, press, evaluate, wait, resize, close
    String selector,       // CSS selector (optional if ref provided)
    String ref,           // ARIA ref (optional if selector provided)
    String text,          // For type/fill
    String value,         // For select
    String script,        // For evaluate
    String key,           // For press
    String direction,     // For scroll: up, down, left, right
    Integer amount,       // For scroll: pixels
    Integer width,        // For resize
    Integer height,       // For resize
    Integer delayMs,      // For click: delay before clicking
    Integer timeMs,       // For wait
    Integer timeoutMs,    // Timeout for this action
    Boolean submit,       // For type: submit after typing
    Boolean slowly,       // For type: type slowly
    Boolean doubleClick,  // For click
    String button,        // For click: left, right, middle
    String[] modifiers,   // For click: Alt, Control, Meta, Shift
    String startSelector, // For drag
    String startRef,      // For drag
    String endSelector,   // For drag
    String endRef,        // For drag
    Map<String, Object> extra  // Extra parameters
) {
    
    public static final int MAX_CLICK_DELAY_MS = 5_000;
    public static final int MAX_WAIT_TIME_MS = 30_000;
    public static final int MAX_TIMEOUT_MS = 60_000;
    
    /**
     * Create a click action.
     */
    public static BatchAction click(String selector) {
        return new BatchAction("click", selector, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a click action with options.
     */
    public static BatchAction click(String selector, Integer delayMs, Boolean doubleClick, String button) {
        return new BatchAction("click", selector, null, null, null, null, null, null, null, null, delayMs, null, null, null, null, doubleClick, button, null, null, null, null, null, null);
    }
    
    /**
     * Create a type action.
     */
    public static BatchAction type(String selector, String text) {
        return new BatchAction("type", selector, null, text, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a type action with options.
     */
    public static BatchAction type(String selector, String text, Boolean submit, Boolean slowly) {
        return new BatchAction("type", selector, null, text, null, null, null, null, null, null, null, null, null, submit, slowly, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a fill action.
     */
    public static BatchAction fill(String selector, String text) {
        return new BatchAction("fill", selector, null, text, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a select action.
     */
    public static BatchAction select(String selector, String value) {
        return new BatchAction("select", selector, null, null, value, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a hover action.
     */
    public static BatchAction hover(String selector) {
        return new BatchAction("hover", selector, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a scroll action.
     */
    public static BatchAction scroll(String direction, int amount) {
        return new BatchAction("scroll", null, null, null, null, null, null, direction, amount, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a drag action.
     */
    public static BatchAction drag(String startSelector, String endSelector) {
        return new BatchAction("drag", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, startSelector, null, endSelector, null, null);
    }
    
    /**
     * Create a press key action.
     */
    public static BatchAction press(String key) {
        return new BatchAction("press", null, null, null, null, null, key, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create an evaluate action.
     */
    public static BatchAction evaluate(String script) {
        return new BatchAction("evaluate", null, null, null, null, script, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a wait action.
     */
    public static BatchAction wait(int timeMs) {
        return new BatchAction("wait", null, null, null, null, null, null, null, null, null, null, timeMs, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a resize action.
     */
    public static BatchAction resize(int width, int height) {
        return new BatchAction("resize", null, null, null, null, null, null, null, null, width, height, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a close action.
     */
    public static BatchAction close() {
        return new BatchAction("close", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Create a navigate action.
     */
    public static BatchAction navigate(String url) {
        return new BatchAction("navigate", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 
            Map.of("url", url));
    }
    
    /**
     * Create a screenshot action.
     */
    public static BatchAction screenshot(String selector) {
        return new BatchAction("screenshot", selector, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * Validate this action.
     */
    public void validate() {
        if (kind == null || kind.isEmpty()) {
            throw new IllegalArgumentException("Action kind is required");
        }
        
        switch (kind) {
            case "click", "type", "fill", "select", "hover", "screenshot" -> {
                if (selector == null && ref == null) {
                    throw new IllegalArgumentException(kind + " action requires selector or ref");
                }
            }
            case "scroll" -> {
                if (direction == null) {
                    throw new IllegalArgumentException("scroll action requires direction");
                }
                if (amount == null || amount < 0) {
                    throw new IllegalArgumentException("scroll action requires positive amount");
                }
            }
            case "drag" -> {
                if ((startSelector == null && startRef == null) || (endSelector == null && endRef == null)) {
                    throw new IllegalArgumentException("drag action requires start and end selectors/refs");
                }
            }
            case "press" -> {
                if (key == null || key.isEmpty()) {
                    throw new IllegalArgumentException("press action requires key");
                }
            }
            case "evaluate" -> {
                if (script == null || script.isEmpty()) {
                    throw new IllegalArgumentException("evaluate action requires script");
                }
            }
            case "wait" -> {
                if (timeMs == null || timeMs < 0 || timeMs > MAX_WAIT_TIME_MS) {
                    throw new IllegalArgumentException("wait timeMs must be between 0 and " + MAX_WAIT_TIME_MS);
                }
            }
            case "resize" -> {
                if (width == null || height == null || width <= 0 || height <= 0) {
                    throw new IllegalArgumentException("resize action requires positive width and height");
                }
            }
            case "type", "fill" -> {
                if (selector == null && ref == null) {
                    throw new IllegalArgumentException(kind + " action requires selector or ref");
                }
                if (text == null) {
                    throw new IllegalArgumentException(kind + " action requires text");
                }
            }
            case "select" -> {
                if (selector == null && ref == null) {
                    throw new IllegalArgumentException("select action requires selector or ref");
                }
                if (value == null) {
                    throw new IllegalArgumentException("select action requires value");
                }
            }
            case "navigate" -> {
                if (extra == null || !extra.containsKey("url")) {
                    throw new IllegalArgumentException("navigate action requires url");
                }
            }
        }
        
        // Validate delayMs
        if (delayMs != null && (delayMs < 0 || delayMs > MAX_CLICK_DELAY_MS)) {
            throw new IllegalArgumentException("delayMs must be between 0 and " + MAX_CLICK_DELAY_MS);
        }
        
        // Validate timeoutMs
        if (timeoutMs != null && (timeoutMs < 0 || timeoutMs > MAX_TIMEOUT_MS)) {
            throw new IllegalArgumentException("timeoutMs must be between 0 and " + MAX_TIMEOUT_MS);
        }
    }
}