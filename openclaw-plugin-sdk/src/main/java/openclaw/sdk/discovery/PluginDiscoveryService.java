package openclaw.sdk.discovery;

import openclaw.sdk.channel.ChannelPlugin;
import openclaw.sdk.provider.ProviderPlugin;
import openclaw.sdk.core.PluginLogger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * Service for discovering plugins.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class PluginDiscoveryService {

    private final PluginLogger logger;

    public PluginDiscoveryService(PluginLogger logger) {
        this.logger = logger;
    }

    /**
     * Discovers all channel plugins.
     *
     * @return list of discovered channel plugins
     */
    public List<ChannelPlugin<?, ?, ?>> discoverChannelPlugins() {
        List<ChannelPlugin<?, ?, ?>> plugins = new ArrayList<>();
        
        ServiceLoader<ChannelPlugin> loader = ServiceLoader.load(ChannelPlugin.class);
        loader.forEach(plugin -> {
            logger.info("Discovered channel plugin: " + plugin.getId());
            plugins.add(plugin);
        });
        
        return plugins;
    }

    /**
     * Discovers all provider plugins.
     *
     * @return list of discovered provider plugins
     */
    public List<ProviderPlugin> discoverProviderPlugins() {
        List<ProviderPlugin> plugins = new ArrayList<>();
        
        ServiceLoader<ProviderPlugin> loader = ServiceLoader.load(ProviderPlugin.class);
        loader.forEach(plugin -> {
            logger.info("Discovered provider plugin: " + plugin.getId());
            plugins.add(plugin);
        });
        
        return plugins;
    }

    /**
     * Discovers plugins from a directory.
     *
     * @param pluginDir the plugin directory
     * @return list of discovered plugin descriptors
     */
    public List<PluginDescriptor> discoverFromDirectory(Path pluginDir) {
        List<PluginDescriptor> discovered = new ArrayList<>();
        
        if (!pluginDir.toFile().exists()) {
            logger.warn("Plugin directory does not exist: " + pluginDir);
            return discovered;
        }
        
        try (Stream<Path> jars = java.nio.file.Files.list(pluginDir)
                .filter(p -> p.toString().endsWith(".jar"))) {
            
            jars.forEach(jar -> {
                try {
                    PluginDescriptor descriptor = extractDescriptor(jar);
                    if (descriptor != null) {
                        discovered.add(descriptor);
                        logger.info("Discovered plugin from JAR: " + descriptor.id());
                    }
                } catch (Exception e) {
                    logger.error("Failed to load plugin from " + jar + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("Failed to scan plugin directory: " + e.getMessage());
        }
        
        return discovered;
    }

    /**
     * Extracts plugin descriptor from a JAR file.
     *
     * @param jarPath the JAR path
     * @return the plugin descriptor or null
     * @throws Exception if extraction fails
     */
    private PluginDescriptor extractDescriptor(Path jarPath) throws Exception {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return null;
            }
            
            java.util.jar.Attributes attrs = manifest.getMainAttributes();
            String pluginId = attrs.getValue("OpenClaw-Plugin-Id");
            String pluginVersion = attrs.getValue("OpenClaw-Plugin-Version");
            String pluginClass = attrs.getValue("OpenClaw-Plugin-Class");
            
            if (pluginId == null || pluginClass == null) {
                return null;
            }
            
            return new PluginDescriptor(
                    pluginId,
                    pluginVersion != null ? pluginVersion : "unknown",
                    pluginClass,
                    jarPath
            );
        }
    }

    /**
     * Plugin descriptor.
     *
     * @param id the plugin ID
     * @param version the plugin version
     * @param mainClass the main class
     * @param sourcePath the source JAR path
     */
    public record PluginDescriptor(
            String id,
            String version,
            String mainClass,
            Path sourcePath
    ) {
    }
}
