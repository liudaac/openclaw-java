package openclaw.plugin.sdk.bundle;

import openclaw.sdk.core.PluginLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry for loading and managing Claude Bundle commands.
 *
 * <p>This registry scans plugin directories for markdown command files,
 * parses their frontmatter, and registers them as invocable commands.</p>
 *
 * <p>Commands are loaded from:</p>
 * <ul>
 *   <li>{@code commands/} directory (default)</li>
 *   <li>Paths specified in {@code .claude-plugin/plugin.json} {@code commands} field</li>
 * </ul>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class BundleCommandRegistry {

    private static final String CLAUDE_PLUGIN_MANIFEST = ".claude-plugin/plugin.json";
    private static final String DEFAULT_COMMANDS_DIR = "commands";
    private static final String MARKDOWN_EXTENSION = ".md";
    private static final String FRONTMATTER_START = "---";

    private final PluginLogger logger;
    private final Map<String, BundleCommand> commands;

    /**
     * Creates a new bundle command registry.
     *
     * @param logger the plugin logger for diagnostic output
     */
    public BundleCommandRegistry(PluginLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.commands = new HashMap<>();
    }

    /**
     * Loads all enabled bundle commands from a plugin directory.
     *
     * @param pluginId the plugin ID
     * @param pluginRoot the plugin root directory
     * @param enabled whether the plugin is enabled
     * @return list of loaded commands
     */
    public List<BundleCommand> loadCommands(String pluginId, Path pluginRoot, boolean enabled) {
        if (!enabled) {
            logger.debug("Plugin " + pluginId + " is disabled, skipping command loading");
            return Collections.emptyList();
        }

        List<BundleCommand> loaded = new ArrayList<>();
        
        try {
            List<Path> commandRoots = resolveCommandRootDirs(pluginRoot);
            
            for (Path commandRoot : commandRoots) {
                if (!Files.exists(commandRoot)) {
                    logger.debug("Command root does not exist: " + commandRoot);
                    continue;
                }
                
                List<BundleCommand> rootCommands = loadCommandsFromRoot(pluginId, commandRoot);
                loaded.addAll(rootCommands);
            }
            
        } catch (Exception e) {
            logger.error("Failed to load commands for plugin " + pluginId + ": " + e.getMessage());
        }

        // Register loaded commands
        for (BundleCommand command : loaded) {
            String key = command.getPluginId() + ":" + command.getRawName();
            commands.put(key, command);
        }

        logger.info("Loaded " + loaded.size() + " bundle commands for plugin: " + pluginId);
        return loaded;
    }

    /**
     * Gets a command by its full name.
     *
     * @param pluginId the plugin ID
     * @param commandName the command name
     * @return the command or empty if not found
     */
    public Optional<BundleCommand> getCommand(String pluginId, String commandName) {
        String key = pluginId + ":" + commandName;
        return Optional.ofNullable(commands.get(key));
    }

    /**
     * Gets all registered commands.
     *
     * @return collection of all commands
     */
    public Collection<BundleCommand> getAllCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    /**
     * Gets all commands for a specific plugin.
     *
     * @param pluginId the plugin ID
     * @return list of commands for the plugin
     */
    public List<BundleCommand> getCommandsForPlugin(String pluginId) {
        return commands.values().stream()
                .filter(cmd -> cmd.getPluginId().equals(pluginId))
                .collect(Collectors.toList());
    }

    /**
     * Clears all registered commands.
     */
    public void clear() {
        commands.clear();
    }

    /**
     * Resolves the command root directories for a plugin.
     *
     * @param pluginRoot the plugin root directory
     * @return list of command root directories
     */
    private List<Path> resolveCommandRootDirs(Path pluginRoot) throws IOException {
        List<Path> roots = new ArrayList<>();
        
        // Check for manifest-defined command paths
        Path manifestPath = pluginRoot.resolve(CLAUDE_PLUGIN_MANIFEST);
        if (Files.exists(manifestPath)) {
            try {
                String content = Files.readString(manifestPath);
                // Simple JSON parsing for "commands" field
                List<String> declaredPaths = parseCommandsFromManifest(content);
                for (String declaredPath : declaredPaths) {
                    roots.add(pluginRoot.resolve(declaredPath));
                }
            } catch (Exception e) {
                logger.warn("Failed to parse Claude plugin manifest: " + e.getMessage());
            }
        }
        
        // Add default commands directory if it exists
        Path defaultCommands = pluginRoot.resolve(DEFAULT_COMMANDS_DIR);
        if (Files.exists(defaultCommands)) {
            roots.add(defaultCommands);
        }
        
        // Remove duplicates while preserving order
        return roots.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Parses the commands field from manifest JSON.
     *
     * @param manifestContent the manifest JSON content
     * @return list of command paths
     */
    private List<String> parseCommandsFromManifest(String manifestContent) {
        List<String> paths = new ArrayList<>();
        
        // Simple parsing for "commands" field - can be a string or array
        int commandsIndex = manifestContent.indexOf("\"commands\"");
        if (commandsIndex < 0) {
            return paths;
        }
        
        int colonIndex = manifestContent.indexOf(':', commandsIndex);
        if (colonIndex < 0) {
            return paths;
        }
        
        int valueStart = colonIndex + 1;
        while (valueStart < manifestContent.length() && 
               Character.isWhitespace(manifestContent.charAt(valueStart))) {
            valueStart++;
        }
        
        char firstChar = manifestContent.charAt(valueStart);
        if (firstChar == '[') {
            // Array of paths
            int arrayEnd = manifestContent.indexOf(']', valueStart);
            if (arrayEnd > 0) {
                String arrayContent = manifestContent.substring(valueStart + 1, arrayEnd);
                String[] items = arrayContent.split(",");
                for (String item : items) {
                    String trimmed = item.trim().replaceAll("^\"|\"$", "");
                    if (!trimmed.isEmpty()) {
                        paths.add(trimmed);
                    }
                }
            }
        } else if (firstChar == '"') {
            // Single string path
            int stringEnd = manifestContent.indexOf('"', valueStart + 1);
            if (stringEnd > 0) {
                paths.add(manifestContent.substring(valueStart + 1, stringEnd));
            }
        }
        
        return paths;
    }

    /**
     * Loads commands from a command root directory.
     *
     * @param pluginId the plugin ID
     * @param commandRoot the command root directory
     * @return list of loaded commands
     */
    private List<BundleCommand> loadCommandsFromRoot(String pluginId, Path commandRoot) throws IOException {
        List<BundleCommand> loaded = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(commandRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(MARKDOWN_EXTENSION))
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .sorted()
                    .forEach(filePath -> {
                        try {
                            BundleCommand command = parseCommandFile(pluginId, commandRoot, filePath);
                            if (command != null) {
                                loaded.add(command);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to parse command file " + filePath + ": " + e.getMessage());
                        }
                    });
        }
        
        return loaded;
    }

    /**
     * Parses a markdown command file.
     *
     * @param pluginId the plugin ID
     * @param commandRoot the command root directory
     * @param filePath the file path
     * @return the parsed command or null if skipped
     * @throws IOException if reading fails
     */
    private BundleCommand parseCommandFile(String pluginId, Path commandRoot, Path filePath) throws IOException {
        String content = Files.readString(filePath);
        
        // Parse frontmatter
        FrontmatterParseResult frontmatter = parseFrontmatter(content);
        
        // Check if disabled
        String disableModelInvocation = frontmatter.get("disable-model-invocation");
        if (isTruthy(disableModelInvocation)) {
            logger.debug("Skipping disabled command: " + filePath);
            return null;
        }
        
        // Extract prompt template (content after frontmatter)
        String promptTemplate = stripFrontmatter(content);
        if (promptTemplate == null || promptTemplate.trim().isEmpty()) {
            logger.debug("Skipping empty command: " + filePath);
            return null;
        }
        
        // Determine command name
        String rawName = frontmatter.get("name");
        if (rawName == null || rawName.trim().isEmpty()) {
            rawName = toDefaultCommandName(commandRoot, filePath);
        }
        rawName = rawName.trim();
        
        // Determine description
        String description = frontmatter.get("description");
        if (description == null || description.trim().isEmpty()) {
            description = toDefaultDescription(rawName, promptTemplate);
        }
        
        return new BundleCommand(
                pluginId,
                rawName,
                description.trim(),
                promptTemplate.trim(),
                filePath.toString()
        );
    }

    /**
     * Parses frontmatter from markdown content.
     *
     * @param content the markdown content
     * @return map of frontmatter key-value pairs
     */
    private FrontmatterParseResult parseFrontmatter(String content) {
        Map<String, String> result = new HashMap<>();
        
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        if (!normalized.startsWith(FRONTMATTER_START)) {
            return new FrontmatterParseResult(result);
        }
        
        int endIndex = normalized.indexOf("\n" + FRONTMATTER_START, 3);
        if (endIndex < 0) {
            return new FrontmatterParseResult(result);
        }
        
        String frontmatterContent = normalized.substring(3, endIndex).trim();
        String[] lines = frontmatterContent.split("\n");
        
        for (String line : lines) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                // Remove surrounding quotes if present
                value = value.replaceAll("^\"|\"$", "");
                result.put(key, value);
            }
        }
        
        return new FrontmatterParseResult(result);
    }

    /**
     * Strips frontmatter from markdown content.
     *
     * @param content the markdown content
     * @return content without frontmatter
     */
    private String stripFrontmatter(String content) {
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        if (!normalized.startsWith(FRONTMATTER_START)) {
            return normalized.trim();
        }
        
        int endIndex = normalized.indexOf("\n" + FRONTMATTER_START, 3);
        if (endIndex < 0) {
            return normalized.trim();
        }
        
        return normalized.substring(endIndex + 4).trim();
    }

    /**
     * Generates default command name from file path.
     *
     * @param commandRoot the command root directory
     * @param filePath the file path
     * @return the default command name
     */
    private String toDefaultCommandName(Path commandRoot, Path filePath) {
        Path relativePath = commandRoot.relativize(filePath);
        String fileName = relativePath.toString();
        
        // Remove extension
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            fileName = fileName.substring(0, lastDot);
        }
        
        // Replace path separators with colons
        return fileName.replace(java.io.File.separator, ":").replace("/", ":");
    }

    /**
     * Generates default description from prompt template.
     *
     * @param rawName the raw command name
     * @param promptTemplate the prompt template
     * @return the default description
     */
    private String toDefaultDescription(String rawName, String promptTemplate) {
        String[] lines = promptTemplate.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return rawName;
    }

    /**
     * Checks if a string value is truthy.
     *
     * @param value the value to check
     * @return true if truthy
     */
    private boolean isTruthy(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return "true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized);
    }

    /**
     * Result of parsing frontmatter.
     */
    private static class FrontmatterParseResult {
        private final Map<String, String> data;

        FrontmatterParseResult(Map<String, String> data) {
            this.data = data;
        }

        String get(String key) {
            return data.get(key);
        }
    }
}