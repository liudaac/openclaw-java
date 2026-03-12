package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;

/**
 * Channel configuration schema.
 *
 * @param schema the JSON schema
 * @param uiHints UI hints for configuration fields
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ChannelConfigSchema(
        Map<String, Object> schema,
        Optional<Map<String, ConfigUiHint>> uiHints
) {

    /**
     * Creates a schema without UI hints.
     *
     * @param schema the JSON schema
     * @return the config schema
     */
    public static ChannelConfigSchema of(Map<String, Object> schema) {
        return new ChannelConfigSchema(schema, Optional.empty());
    }

    /**
     * Creates a schema with UI hints.
     *
     * @param schema the JSON schema
     * @param uiHints the UI hints
     * @return the config schema
     */
    public static ChannelConfigSchema withHints(
            Map<String, Object> schema,
            Map<String, ConfigUiHint> uiHints) {
        return new ChannelConfigSchema(schema, Optional.of(uiHints));
    }
}
