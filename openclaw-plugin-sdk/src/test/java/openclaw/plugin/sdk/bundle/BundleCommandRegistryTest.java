package openclaw.plugin.sdk.bundle;

import openclaw.sdk.core.PluginLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BundleCommandRegistry}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class BundleCommandRegistryTest {

    @TempDir
    Path tempDir;

    private PluginLogger logger;
    private BundleCommandRegistry registry;

    @BeforeEach
    void setUp() {
        logger = new PluginLogger() {
            @Override
            public void debug(String message) {
                System.out.println("[DEBUG] " + message);
            }

            @Override
            public void info(String message) {
                System.out.println("[INFO] " + message);
            }

            @Override
            public void warn(String message) {
                System.out.println("[WARN] " + message);
            }

            @Override
            public void error(String message) {
                System.out.println("[ERROR] " + message);
            }
        };
        registry = new BundleCommandRegistry(logger);
    }

    @Test
    void testLoadCommandsFromDefaultDirectory() throws IOException {
        // Create plugin structure
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path commandsDir = pluginRoot.resolve("commands");
        Files.createDirectories(commandsDir);

        // Create a command file
        Path commandFile = commandsDir.resolve("test-command.md");
        Files.writeString(commandFile, """
                ---
                name: custom:test
                description: A test command
                ---
                This is the prompt template for testing.
                """);

        // Load commands
        List<BundleCommand> commands = registry.loadCommands("test-plugin", pluginRoot, true);

        // Verify
        assertEquals(1, commands.size());
        BundleCommand command = commands.get(0);
        assertEquals("test-plugin", command.getPluginId());
        assertEquals("custom:test", command.getRawName());
        assertEquals("A test command", command.getDescription());
        assertEquals("This is the prompt template for testing.", command.getPromptTemplate());
    }

    @Test
    void testLoadCommandsWithDefaultName() throws IOException {
        // Create plugin structure
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path commandsDir = pluginRoot.resolve("commands");
        Files.createDirectories(commandsDir);

        // Create a command file without explicit name
        Path commandFile = commandsDir.resolve("office-hours.md");
        Files.writeString(commandFile, """
                ---
                description: Help with scoping
                ---
                Give direct engineering advice.
                """);

        // Load commands
        List<BundleCommand> commands = registry.loadCommands("test-plugin", pluginRoot, true);

        // Verify - name should be derived from file path
        assertEquals(1, commands.size());
        assertEquals("office-hours", commands.get(0).getRawName());
    }

    @Test
    void testLoadCommandsWithNestedPaths() throws IOException {
        // Create plugin structure with nested directories
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path workflowsDir = pluginRoot.resolve("commands/workflows");
        Files.createDirectories(workflowsDir);

        // Create a nested command file
        Path commandFile = workflowsDir.resolve("review.md");
        Files.writeString(commandFile, """
                ---
                name: workflows:review
                description: Run a structured review
                ---
                Review the code.
                """);

        // Load commands
        List<BundleCommand> commands = registry.loadCommands("test-plugin", pluginRoot, true);

        // Verify
        assertEquals(1, commands.size());
        assertEquals("workflows:review", commands.get(0).getRawName());
    }

    @Test
    void testSkipDisabledCommands() throws IOException {
        // Create plugin structure
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path commandsDir = pluginRoot.resolve("commands");
        Files.createDirectories(commandsDir);

        // Create a disabled command
        Path disabledFile = commandsDir.resolve("disabled.md");
        Files.writeString(disabledFile, """
                ---
                disable-model-invocation: true
                ---
                This should not be loaded.
                """);

        // Create an enabled command
        Path enabledFile = commandsDir.resolve("enabled.md");
        Files.writeString(enabledFile, """
                ---
                name: enabled-cmd
                ---
                This should be loaded.
                """);

        // Load commands
        List<BundleCommand> commands = registry.loadCommands("test-plugin", pluginRoot, true);

        // Verify - only enabled command should be loaded
        assertEquals(1, commands.size());
        assertEquals("enabled-cmd", commands.get(0).getRawName());
    }

    @Test
    void testSkipDisabledPlugin() throws IOException {
        // Create plugin structure
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path commandsDir = pluginRoot.resolve("commands");
        Files.createDirectories(commandsDir);

        // Create a command file
        Path commandFile = commandsDir.resolve("test.md");
        Files.writeString(commandFile, "Test content");

        // Load commands with disabled plugin
        List<BundleCommand> commands = registry.loadCommands("test-plugin", pluginRoot, false);

        // Verify - no commands should be loaded
        assertTrue(commands.isEmpty());
    }

    @Test
    void testGetCommand() throws IOException {
        // Create and load commands
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path commandsDir = pluginRoot.resolve("commands");
        Files.createDirectories(commandsDir);

        Path commandFile = commandsDir.resolve("test.md");
        Files.writeString(commandFile, """
                ---
                name: my-command
                ---
                Test content
                """);

        registry.loadCommands("test-plugin", pluginRoot, true);

        // Test get existing command
        Optional<BundleCommand> found = registry.getCommand("test-plugin", "my-command");
        assertTrue(found.isPresent());
        assertEquals("my-command", found.get().getRawName());

        // Test get non-existent command
        Optional<BundleCommand> notFound = registry.getCommand("test-plugin", "non-existent");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testGetAllCommands() throws IOException {
        // Create plugin with multiple commands
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path commandsDir = pluginRoot.resolve("commands");
        Files.createDirectories(commandsDir);

        Files.writeString(commandsDir.resolve("cmd1.md"), "---\nname: cmd1\n---\nContent 1");
        Files.writeString(commandsDir.resolve("cmd2.md"), "---\nname: cmd2\n---\nContent 2");

        registry.loadCommands("test-plugin", pluginRoot, true);

        // Test get all commands
        Collection<BundleCommand> allCommands = registry.getAllCommands();
        assertEquals(2, allCommands.size());
    }

    @Test
    void testGetCommandsForPlugin() throws IOException {
        // Create two plugins
        Path plugin1Root = tempDir.resolve("plugin1");
        Path plugin2Root = tempDir.resolve("plugin2");
        Files.createDirectories(plugin1Root.resolve("commands"));
        Files.createDirectories(plugin2Root.resolve("commands"));

        Files.writeString(plugin1Root.resolve("commands/cmd.md"), "---\nname: p1-cmd\n---\nContent");
        Files.writeString(plugin2Root.resolve("commands/cmd.md"), "---\nname: p2-cmd\n---\nContent");

        registry.loadCommands("plugin1", plugin1Root, true);
        registry.loadCommands("plugin2", plugin2Root, true);

        // Test get commands for specific plugin
        List<BundleCommand> plugin1Commands = registry.getCommandsForPlugin("plugin1");
        assertEquals(1, plugin1Commands.size());
        assertEquals("p1-cmd", plugin1Commands.get(0).getRawName());
    }

    @Test
    void testClear() throws IOException {
        // Create and load commands
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path commandsDir = pluginRoot.resolve("commands");
        Files.createDirectories(commandsDir);
        Files.writeString(commandsDir.resolve("cmd.md"), "Content");

        registry.loadCommands("test-plugin", pluginRoot, true);
        assertFalse(registry.getAllCommands().isEmpty());

        // Clear and verify
        registry.clear();
        assertTrue(registry.getAllCommands().isEmpty());
    }

    @Test
    void testEmptyCommandSkipped() throws IOException {
        // Create plugin with empty command
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path commandsDir = pluginRoot.resolve("commands");
        Files.createDirectories(commandsDir);

        // Create empty command (only frontmatter)
        Path emptyFile = commandsDir.resolve("empty.md");
        Files.writeString(emptyFile, """
                ---
                name: empty-cmd
                ---
                """);

        List<BundleCommand> commands = registry.loadCommands("test-plugin", pluginRoot, true);
        assertTrue(commands.isEmpty());
    }

    @Test
    void testCommandWithoutFrontmatter() throws IOException {
        // Create plugin with command without frontmatter
        Path pluginRoot = tempDir.resolve("test-plugin");
        Path commandsDir = pluginRoot.resolve("commands");
        Files.createDirectories(commandsDir);

        Path noFrontmatterFile = commandsDir.resolve("no-frontmatter.md");
        Files.writeString(noFrontmatterFile, "This is content without frontmatter.");

        List<BundleCommand> commands = registry.loadCommands("test-plugin", pluginRoot, true);

        // Should still be loaded with default name
        assertEquals(1, commands.size());
        assertEquals("no-frontmatter", commands.get(0).getRawName());
        assertEquals("This is content without frontmatter.", commands.get(0).getPromptTemplate());
    }
}
