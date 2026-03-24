package openclaw.desktop.model;

import javafx.scene.paint.Color;

/**
 * UI Theme definition with color palette.
 *
 * <p>Modern color scheme with support for dark/light modes.</p>
 */
public record UITheme(
    String name,
    Color backgroundPrimary,
    Color backgroundSecondary,
    Color backgroundTertiary,
    Color surface,
    Color surfaceElevated,
    Color textPrimary,
    Color textSecondary,
    Color textMuted,
    Color accent,
    Color accentHover,
    Color accentLight,
    Color border,
    Color borderLight,
    Color success,
    Color warning,
    Color error,
    Color info,
    Color userBubble,
    Color assistantBubble,
    Color systemBubble,
    double glassOpacity,
    double borderRadius,
    double spacing,
    String cssFile
) {

    /**
     * Modern Dark Theme (Default)
     */
    public static UITheme modernDark() {
        return new UITheme(
            "Modern Dark",
            Color.web("#0d1117"),      // backgroundPrimary
            Color.web("#161b22"),      // backgroundSecondary
            Color.web("#21262d"),      // backgroundTertiary
            Color.web("#1c2128"),      // surface
            Color.web("#22272e"),      // surfaceElevated
            Color.web("#e6edf3"),      // textPrimary
            Color.web("#8b949e"),      // textSecondary
            Color.web("#6e7681"),      // textMuted
            Color.web("#58a6ff"),      // accent
            Color.web("#79c0ff"),      // accentHover
            Color.web("#1f6feb"),      // accentLight
            Color.web("#30363d"),      // border
            Color.web("#21262d"),      // borderLight
            Color.web("#238636"),      // success
            Color.web("#d29922"),      // warning
            Color.web("#da3633"),      // error
            Color.web("#58a6ff"),      // info
            Color.web("#0d1117"),      // userBubble
            Color.web("#161b22"),      // assistantBubble
            Color.web("#21262d"),      // systemBubble
            0.85,                       // glassOpacity
            12.0,                       // borderRadius
            16.0,                       // spacing
            "/css/modern-dark.css"
        );
    }

    /**
     * Modern Light Theme
     */
    public static UITheme modernLight() {
        return new UITheme(
            "Modern Light",
            Color.web("#ffffff"),      // backgroundPrimary
            Color.web("#f6f8fa"),      // backgroundSecondary
            Color.web("#eaeef2"),      // backgroundTertiary
            Color.web("#ffffff"),      // surface
            Color.web("#f6f8fa"),      // surfaceElevated
            Color.web("#1f2328"),      // textPrimary
            Color.web("#656d76"),      // textSecondary
            Color.web("#8c959f"),      // textMuted
            Color.web("#0969da"),      // accent
            Color.web("#0550ae"),      // accentHover
            Color.web("#ddf4ff"),      // accentLight
            Color.web("#d0d7de"),      // border
            Color.web("#eaeef2"),      // borderLight
            Color.web("#1a7f37"),      // success
            Color.web("(#9a6700"),      // warning
            Color.web("#cf222e"),      // error
            Color.web("#0969da"),      // info
            Color.web("#ddf4ff"),      // userBubble
            Color.web("#f6f8fa"),      // assistantBubble
            Color.web("#eaeef2"),      // systemBubble
            0.95,                       // glassOpacity
            12.0,                       // borderRadius
            16.0,                       // spacing
            "/css/modern-light.css"
        );
    }

    /**
     * Midnight Theme (Deep dark)
     */
    public static UITheme midnight() {
        return new UITheme(
            "Midnight",
            Color.web("#000000"),
            Color.web("#0a0a0a"),
            Color.web("#141414"),
            Color.web("#1a1a1a"),
            Color.web("#202020"),
            Color.web("#ffffff"),
            Color.web("#a0a0a0"),
            Color.web("#666666"),
            Color.web("#6366f1"),
            Color.web("#818cf8"),
            Color.web("#4f46e5"),
            Color.web("#333333"),
            Color.web("#222222"),
            Color.web("#22c55e"),
            Color.web("#f59e0b"),
            Color.web("#ef4444"),
            Color.web("#3b82f6"),
            Color.web("#0a0a0a"),
            Color.web("#141414"),
            Color.web("#1a1a1a"),
            0.90,
            16.0,
            20.0,
            "/css/modern-dark.css"
        );
    }

    /**
     * Convert Color to CSS hex string.
     */
    public String toCssColor(Color color) {
        return String.format("#%02x%02x%02x",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    /**
     * Convert Color to CSS rgba string with opacity.
     */
    public String toCssRgba(Color color, double opacity) {
        return String.format("rgba(%d, %d, %d, %.2f)",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255),
            opacity);
    }

    /**
     * Generate CSS variables for this theme.
     */
    public String generateCssVariables() {
        return String.format("""
            :root {
                --bg-primary: %s;
                --bg-secondary: %s;
                --bg-tertiary: %s;
                --surface: %s;
                --surface-elevated: %s;
                --text-primary: %s;
                --text-secondary: %s;
                --text-muted: %s;
                --accent: %s;
                --accent-hover: %s;
                --accent-light: %s;
                --border: %s;
                --border-light: %s;
                --success: %s;
                --warning: %s;
                --error: %s;
                --info: %s;
                --user-bubble: %s;
                --assistant-bubble: %s;
                --system-bubble: %s;
                --glass-opacity: %.2f;
                --border-radius: %.1fpx;
                --spacing: %.1fpx;
            }
            """,
            toCssColor(backgroundPrimary),
            toCssColor(backgroundSecondary),
            toCssColor(backgroundTertiary),
            toCssColor(surface),
            toCssColor(surfaceElevated),
            toCssColor(textPrimary),
            toCssColor(textSecondary),
            toCssColor(textMuted),
            toCssColor(accent),
            toCssColor(accentHover),
            toCssColor(accentLight),
            toCssColor(border),
            toCssColor(borderLight),
            toCssColor(success),
            toCssColor(warning),
            toCssColor(error),
            toCssColor(info),
            toCssColor(userBubble),
            toCssColor(assistantBubble),
            toCssColor(systemBubble),
            glassOpacity,
            borderRadius,
            spacing
        );
    }
}
