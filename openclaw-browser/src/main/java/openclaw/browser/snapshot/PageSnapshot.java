package openclaw.browser.snapshot;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ViewportSize;

import java.util.*;

/**
 * Captures and represents a page snapshot.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class PageSnapshot {
    
    private final String url;
    private final String title;
    private final String html;
    private final List<ElementInfo> elements;
    private final List<LinkInfo> links;
    private final List<ImageInfo> images;
    private final AccessibilitySnapshot accessibility;
    private final ViewportInfo viewport;
    
    public PageSnapshot(String url, String title, String html,
                       List<ElementInfo> elements, List<LinkInfo> links,
                       List<ImageInfo> images, AccessibilitySnapshot accessibility,
                       ViewportInfo viewport) {
        this.url = url;
        this.title = title;
        this.html = html;
        this.elements = elements;
        this.links = links;
        this.images = images;
        this.accessibility = accessibility;
        this.viewport = viewport;
    }
    
    public static PageSnapshot capture(Page page) {
        // Get basic info
        String url = page.url();
        String title = page.title();
        String html = page.content();
        
        // Get viewport info
        ViewportSize size = page.viewportSize();
        ViewportInfo viewport = new ViewportInfo(size.width, size.height);
        
        // Capture interactive elements
        List<ElementInfo> elements = new ArrayList<>();
        List<Locator> interactiveElements = page.locator(
            "button, a, input, textarea, select, [role='button'], [role='link'], [tabindex]:not([tabindex='-1'])"
        ).all();
        
        for (int i = 0; i < Math.min(interactiveElements.size(), 100); i++) {
            Locator el = interactiveElements.get(i);
            try {
                ElementInfo info = new ElementInfo(
                    el.evaluate("e => e.tagName.toLowerCase()").toString(),
                    getElementText(el),
                    getElementRole(el),
                    el.evaluate("e => e.id || ''").toString(),
                    getElementAttributes(el),
                    isElementVisible(el),
                    isElementEnabled(el)
                );
                elements.add(info);
            } catch (Exception e) {
                // Skip elements that can't be evaluated
            }
        }
        
        // Capture links
        List<LinkInfo> links = new ArrayList<>();
        List<Locator> linkElements = page.locator("a[href]").all();
        for (Locator el : linkElements.subList(0, Math.min(linkElements.size(), 50))) {
            try {
                String href = el.getAttribute("href");
                String text = el.textContent();
                if (href != null && !href.isEmpty()) {
                    links.add(new LinkInfo(href, text, isElementVisible(el)));
                }
            } catch (Exception e) {
                // Skip
            }
        }
        
        // Capture images
        List<ImageInfo> images = new ArrayList<>();
        List<Locator> imgElements = page.locator("img").all();
        for (Locator el : imgElements.subList(0, Math.min(imgElements.size(), 20))) {
            try {
                String src = el.getAttribute("src");
                String alt = el.getAttribute("alt");
                if (src != null && !src.isEmpty()) {
                    images.add(new ImageInfo(src, alt, isElementVisible(el)));
                }
            } catch (Exception e) {
                // Skip
            }
        }
        
        // Accessibility snapshot (not available in Playwright Java 1.40.0)
        AccessibilitySnapshot accessibility = new AccessibilitySnapshot(null);
        
        return new PageSnapshot(url, title, html, elements, links, images, accessibility, viewport);
    }
    
    private static String getElementText(Locator el) {
        try {
            String text = el.textContent();
            return text != null ? text.trim().substring(0, Math.min(100, text.length())) : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private static String getElementRole(Locator el) {
        try {
            Object role = el.evaluate("e => e.getAttribute('role') || e.tagName.toLowerCase()");
            return role != null ? role.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private static Map<String, String> getElementAttributes(Locator el) {
        Map<String, String> attrs = new HashMap<>();
        try {
            String[] attrNames = {"type", "name", "placeholder", "value", "aria-label", "aria-describedby"};
            for (String name : attrNames) {
                String value = el.getAttribute(name);
                if (value != null) {
                    attrs.put(name, value);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return attrs;
    }
    
    private static boolean isElementVisible(Locator el) {
        try {
            return el.isVisible();
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean isElementEnabled(Locator el) {
        try {
            return el.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
    
    // Getters
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getHtml() { return html; }
    public List<ElementInfo> getElements() { return elements; }
    public List<LinkInfo> getLinks() { return links; }
    public List<ImageInfo> getImages() { return images; }
    public AccessibilitySnapshot getAccessibility() { return accessibility; }
    public ViewportInfo getViewport() { return viewport; }
    
    // Records
    public record ElementInfo(String tag, String text, String role, String id,
                             Map<String, String> attributes, boolean visible, boolean enabled) {}
    public record LinkInfo(String href, String text, boolean visible) {}
    public record ImageInfo(String src, String alt, boolean visible) {}
    public record ViewportInfo(int width, int height) {}
    public record AccessibilitySnapshot(Object rawSnapshot) {}
}
