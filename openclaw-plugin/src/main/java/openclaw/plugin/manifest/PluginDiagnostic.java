package openclaw.plugin.manifest;

import java.util.Optional;

/**
 * Plugin diagnostic message.
 *
 * <p>Following the TypeScript PluginDiagnostic pattern.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public record PluginDiagnostic(
        Level level,
        String message,
        Optional<String> pluginId,
        Optional<String> source
) {
    
    /**
     * Creates a diagnostic with required fields.
     */
    public PluginDiagnostic(Level level, String message) {
        this(level, message, Optional.empty(), Optional.empty());
    }
    
    /**
     * Creates a diagnostic with plugin ID.
     */
    public PluginDiagnostic(Level level, String message, String pluginId, String source) {
        this(level, message, Optional.ofNullable(pluginId), Optional.ofNullable(source));
    }
    
    /**
     * Diagnostic level enumeration.
     */
    public enum Level {
        ERROR,
        WARN,
        INFO,
        DEBUG
    }
}
