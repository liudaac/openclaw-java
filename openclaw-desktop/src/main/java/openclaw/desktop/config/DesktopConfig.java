package openclaw.desktop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Desktop application configuration.
 *
 * <p>Manages UI preferences, theme settings, and desktop-specific options.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "openclaw.desktop")
public class DesktopConfig {

    /** UI theme preference */
    private UITheme theme = UITheme.DARK;

    /** Window width */
    private int windowWidth = 1400;

    /** Window height */
    private int windowHeight = 900;

    /** Sidebar width */
    private int sidebarWidth = 280;

    /** Font size scale (0.8 - 1.5) */
    private double fontScale = 1.0;

    /** Enable animations */
    private boolean animationsEnabled = true;

    /** Enable transparency effects */
    private boolean transparencyEnabled = true;

    /** Auto-save conversations */
    private boolean autoSave = true;

    /** Default model provider */
    private String defaultProvider = "openai";

    /** Default model */
    private String defaultModel = "gpt-4";

    /** API Keys storage path */
    private Path apiKeysPath = Paths.get(System.getProperty("user.home"), ".openclaw", "desktop", "api-keys.enc");

    /** Export directory */
    private Path exportPath = Paths.get(System.getProperty("user.home"), "Documents", "OpenClaw");

    /** Maximum messages per conversation */
    private int maxMessagesPerConversation = 1000;

    /** Enable streaming responses */
    private boolean streamingEnabled = true;

    /** Show token usage */
    private boolean showTokenUsage = true;

    /** Compact mode (smaller padding) */
    private boolean compactMode = false;

    public enum UITheme {
        DARK, LIGHT, AUTO
    }

    // Getters and Setters

    public UITheme getTheme() {
        return theme;
    }

    public void setTheme(UITheme theme) {
        this.theme = theme;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public int getSidebarWidth() {
        return sidebarWidth;
    }

    public void setSidebarWidth(int sidebarWidth) {
        this.sidebarWidth = sidebarWidth;
    }

    public double getFontScale() {
        return fontScale;
    }

    public void setFontScale(double fontScale) {
        this.fontScale = fontScale;
    }

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }

    public boolean isTransparencyEnabled() {
        return transparencyEnabled;
    }

    public void setTransparencyEnabled(boolean transparencyEnabled) {
        this.transparencyEnabled = transparencyEnabled;
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Path getApiKeysPath() {
        return apiKeysPath;
    }

    public void setApiKeysPath(Path apiKeysPath) {
        this.apiKeysPath = apiKeysPath;
    }

    public Path getExportPath() {
        return exportPath;
    }

    public void setExportPath(Path exportPath) {
        this.exportPath = exportPath;
    }

    public int getMaxMessagesPerConversation() {
        return maxMessagesPerConversation;
    }

    public void setMaxMessagesPerConversation(int maxMessagesPerConversation) {
        this.maxMessagesPerConversation = maxMessagesPerConversation;
    }

    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }

    public boolean isShowTokenUsage() {
        return showTokenUsage;
    }

    public void setShowTokenUsage(boolean showTokenUsage) {
        this.showTokenUsage = showTokenUsage;
    }

    public boolean isCompactMode() {
        return compactMode;
    }

    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
    }
}
