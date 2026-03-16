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
    
    // Private constructor for factory methods
    private BatchAction(String kind, String selector, String ref, String text, String value, 
                        String script, String key, String direction, Integer amount,
                        Integer width, Integer height, Integer delayMs, Integer timeMs, Integer timeoutMs,
                        Boolean submit, Boolean slowly, Boolean doubleClick, String button, String[] modifiers,
                        String startSelector, String startRef, String endSelector, String endRef,
                        Map<String, Object> extra, boolean dummy) {
        this(kind, selector, ref, text, value, script, key, direction, amount, 
             width, height, delayMs, timeMs, timeoutMs, submit, slowly, doubleClick, 
             button, modifiers, startSelector, startRef, endSelector, endRef, extra);
    }
    
    /**
     * Create a click action.
     */
    public static BatchAction click(String selector) {
        return new BatchAction("click", selector, null, null, null, null, null, null, null, 
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a click action with options.
     */
    public static BatchAction click(String selector, Integer delayMs, Boolean doubleClick, String button) {
        return new BatchAction("click", selector, null, null, null, null, null, null, null,
            null, null, delayMs, null, null, null, null, doubleClick, button, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a type action.
     */
    public static BatchAction type(String selector, String text) {
        return new BatchAction("type", selector, null, text, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a type action with options.
     */
    public static BatchAction type(String selector, String text, Boolean submit, Boolean slowly) {
        return new BatchAction("type", selector, null, text, null, null, null, null, null,
            null, null, null, null, null, submit, slowly, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a fill action.
     */
    public static BatchAction fill(String selector, String text) {
        return new BatchAction("fill", selector, null, text, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a select action.
     */
    public static BatchAction select(String selector, String value) {
        return new BatchAction("select", selector, null, null, value, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a hover action.
     */
    public static BatchAction hover(String selector) {
        return new BatchAction("hover", selector, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a scroll action.
     */
    public static BatchAction scroll(String direction, int amount) {
        return new BatchAction("scroll", null, null, null, null, null, null, direction, amount,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a drag action.
     */
    public static BatchAction drag(String startSelector, String endSelector) {
        return new BatchAction("drag", null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, startSelector, null, endSelector, null, null, true);
    }
    
    /**
     * Create a press key action.
     */
    public static BatchAction press(String key) {
        return new BatchAction("press", null, null, null, null, null, key, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create an evaluate action.
     */
    public static BatchAction evaluate(String script) {
        return new BatchAction("evaluate", null, null, null, null, script, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a wait action.
     */
    public static BatchAction wait(int timeMs) {
        return new BatchAction("wait", null, null, null, null, null, null, null, null,
            null, null, null, timeMs, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a resize action.
     */
    public static BatchAction resize(int width, int height) {
        return new BatchAction("resize", null, null, null, null, null, null, null, null,
            width, height, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
    
    /**
     * Create a close action.
     */
    public static BatchAction close() {
        return new BatchAction("close", null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true);
    }
}
