package openclaw.plugin.sdk.bundle;

import java.util.Objects;

/**
 * Represents a Claude Bundle command specification.
 * 
 * <p>Bundle commands are loaded from markdown files in the plugin's commands directory.
 * Each command has a name, description, and prompt template that can be invoked
 * by the agent system.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class BundleCommand {

    private final String pluginId;
    private final String rawName;
    private final String description;
    private final String promptTemplate;
    private final String sourceFilePath;

    /**
     * Creates a new bundle command specification.
     *
     * @param pluginId the plugin ID that owns this command
     * @param rawName the raw command name (e.g., "office-hours" or "workflows:review")
     * @param description the command description for UI display
     * @param promptTemplate the prompt template to execute when command is invoked
     * @param sourceFilePath the source file path of the markdown command file
     * @throws NullPointerException if any required parameter is null
     */
    public BundleCommand(
            String pluginId,
            String rawName,
            String description,
            String promptTemplate,
            String sourceFilePath) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId cannot be null");
        this.rawName = Objects.requireNonNull(rawName, "rawName cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.promptTemplate = Objects.requireNonNull(promptTemplate, "promptTemplate cannot be null");
        this.sourceFilePath = Objects.requireNonNull(sourceFilePath, "sourceFilePath cannot be null");
    }

    /**
     * Gets the plugin ID that owns this command.
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Gets the raw command name.
     * <p>This is the command identifier as defined in the markdown file's frontmatter
     * or derived from the file path.</p>
     *
     * @return the raw command name
     */
    public String getRawName() {
        return rawName;
    }

    /**
     * Gets the command description.
     * <p>This is used for UI display and help text.</p>
     *
     * @return the command description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the prompt template.
     * <p>This is the actual prompt content that will be sent to the model
     * when the command is invoked.</p>
     *
     * @return the prompt template
     */
    public String getPromptTemplate() {
        return promptTemplate;
    }

    /**
     * Gets the source file path.
     * <p>This is the absolute path to the markdown file that defines this command.</p>
     *
     * @return the source file path
     */
    public String getSourceFilePath() {
        return sourceFilePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BundleCommand that = (BundleCommand) o;
        return Objects.equals(pluginId, that.pluginId) &&
                Objects.equals(rawName, that.rawName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, rawName);
    }

    @Override
    public String toString() {
        return "BundleCommand{" +
                "pluginId='" + pluginId + '\'' +
                ", rawName='" + rawName + '\'' +
                ", description='" + description + '\'' +
                ", sourceFilePath='" + sourceFilePath + '\'' +
                '}';
    }
}
