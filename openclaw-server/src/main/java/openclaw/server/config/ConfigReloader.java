package openclaw.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Config Reloader - High Priority Improvement
 *
 * <p>Supports dynamic configuration reloading at runtime.</p>
 */
@Component
public class ConfigReloader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigReloader.class);

    private final ConfigurableEnvironment environment;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${openclaw.config.reload.enabled:true}")
    private boolean reloadEnabled;

    @Value("${openclaw.config.reload.path:config/}")
    private String configPath;

    private final AtomicReference<Map<String, Object>> lastConfig = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicLong reloadCount = new AtomicLong(0);
    private WatchService watchService;
    private Thread watchThread;

    public ConfigReloader(ConfigurableEnvironment environment,
                         ApplicationEventPublisher eventPublisher) {
        this.environment = environment;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Initialize config watcher
     */
    public void initialize() {
        if (!reloadEnabled) {
            logger.info("Config reload is disabled");
            return;
        }

        try {
            // Load initial config
            loadConfig();

            // Start file watcher
            startFileWatcher();

            logger.info("Config reloader initialized, watching: {}", configPath);

        } catch (Exception e) {
            logger.error("Failed to initialize config reloader: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled config check (fallback)
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void scheduledCheck() {
        if (!reloadEnabled) {
            return;
        }

        try {
            checkAndReload();
        } catch (Exception e) {
            logger.error("Scheduled config check failed: {}", e.getMessage());
        }
    }

    /**
     * Check and reload config
     */
    public synchronized boolean checkAndReload() {
        try {
            Map<String, Object> newConfig = loadConfigFromFile();
            Map<String, Object> oldConfig = lastConfig.get();

            if (hasConfigChanged(oldConfig, newConfig)) {
                logger.info("Configuration changed, reloading...");
                applyConfig(newConfig);
                lastConfig.set(newConfig);
                reloadCount.incrementAndGet();

                // Publish event
                eventPublisher.publishEvent(new ConfigChangeEvent(this, oldConfig, newConfig));

                logger.info("Configuration reloaded successfully");
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("Config reload failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Force reload config
     */
    public boolean forceReload() {
        logger.info("Force reloading configuration...");
        return checkAndReload();
    }

    /**
     * Get config value
     */
    public String getConfig(String key) {
        return environment.getProperty(key);
    }

    /**
     * Get config value with default
     */
    public String getConfig(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    /**
     * Update config value (runtime only, not persisted)
     */
    public void updateConfig(String key, String value) {
        Map<String, Object> config = new ConcurrentHashMap<>(lastConfig.get());
        config.put(key, value);

        // Update environment
        MapPropertySource propertySource = new MapPropertySource("runtime-config", config);
        environment.getPropertySources().addFirst(propertySource);

        logger.info("Runtime config updated: {} = {}", key, value);
    }

    /**
     * Get reload statistics
     */
    public ReloadStats getStats() {
        return new ReloadStats(
                reloadCount.get(),
                lastConfig.get().size(),
                reloadEnabled
        );
    }

    private void loadConfig() throws IOException {
        Map<String, Object> config = loadConfigFromFile();
        lastConfig.set(config);
        logger.debug("Initial config loaded: {} properties", config.size());
    }

    private Map<String, Object> loadConfigFromFile() throws IOException {
        Map<String, Object> config = new ConcurrentHashMap<>();

        // Load from application.yml
        File configFile = new File(configPath, "application.yml");
        if (configFile.exists()) {
            // Parse YAML (simplified)
            config.putAll(parseYamlFile(configFile));
        }

        // Load from application.properties
        File propsFile = new File(configPath, "application.properties");
        if (propsFile.exists()) {
            Resource resource = new FileSystemResource(propsFile);
            Properties properties = PropertiesLoaderUtils.loadProperties(resource);
            properties.forEach((k, v) -> config.put(k.toString(), v));
        }

        // Load from custom config file
        File customFile = new File(configPath, "openclaw.properties");
        if (customFile.exists()) {
            Resource resource = new FileSystemResource(customFile);
            Properties properties = PropertiesLoaderUtils.loadProperties(resource);
            properties.forEach((k, v) -> config.put(k.toString(), v));
        }

        return config;
    }

    private Map<String, Object> parseYamlFile(File file) {
        // Simplified YAML parsing
        // In production, use SnakeYAML or similar
        Map<String, Object> result = new ConcurrentHashMap<>();
        try {
            java.util.List<String> lines = Files.readAllLines(file.toPath());
            String currentSection = "";
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.endsWith(":")) {
                    currentSection = line.substring(0, line.length() - 1);
                } else if (line.contains(":")) {
                    int colonIndex = line.indexOf(':');
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    if (!currentSection.isEmpty()) {
                        key = currentSection + "." + key;
                    }
                    result.put(key, value);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to parse YAML: {}", e.getMessage());
        }
        return result;
    }

    private boolean hasConfigChanged(Map<String, Object> oldConfig, Map<String, Object> newConfig) {
        if (oldConfig.size() != newConfig.size()) {
            return true;
        }

        for (Map.Entry<String, Object> entry : newConfig.entrySet()) {
            String key = entry.getKey();
            Object newValue = entry.getValue();
            Object oldValue = oldConfig.get(key);

            if (oldValue == null || !oldValue.equals(newValue)) {
                return true;
            }
        }

        return false;
    }

    private void applyConfig(Map<String, Object> newConfig) {
        // Update environment
        MapPropertySource propertySource = new MapPropertySource("dynamic-config", newConfig);

        // Remove old dynamic config if exists
        if (environment.getPropertySources().contains("dynamic-config")) {
            environment.getPropertySources().remove("dynamic-config");
        }

        // Add new config
        environment.getPropertySources().addFirst(propertySource);

        logger.debug("Applied {} config properties", newConfig.size());
    }

    private void startFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(configPath);

            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            watchThread = new Thread(this::watchFiles, "config-watcher");
            watchThread.setDaemon(true);
            watchThread.start();

            logger.info("File watcher started for: {}", configPath);

        } catch (IOException e) {
            logger.error("Failed to start file watcher: {}", e.getMessage());
        }
    }

    private void watchFiles() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = (Path) event.context();
                    logger.debug("Config file changed: {}", changed);
                    checkAndReload();
                }
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("File watcher error: {}", e.getMessage());
            }
        }
    }

    /**
     * Shutdown file watcher
     */
    public void shutdown() {
        if (watchThread != null) {
            watchThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.error("Failed to close watch service: {}", e.getMessage());
            }
        }
    }

    /**
     * Config change event
     */
    public static class ConfigChangeEvent extends org.springframework.context.ApplicationEvent {
        private final Map<String, Object> oldConfig;
        private final Map<String, Object> newConfig;

        public ConfigChangeEvent(Object source, Map<String, Object> oldConfig, Map<String, Object> newConfig) {
            super(source);
            this.oldConfig = oldConfig;
            this.newConfig = newConfig;
        }

        public Map<String, Object> getOldConfig() {
            return oldConfig;
        }

        public Map<String, Object> getNewConfig() {
            return newConfig;
        }
    }

    /**
     * Config change listener
     */
    @EventListener
    public void onConfigChange(ConfigChangeEvent event) {
        logger.info("Config change detected: {} properties changed",
                event.getNewConfig().size());

        // Notify components
        // This could trigger reconnections, cache clears, etc.
    }

    // Record
    public record ReloadStats(long reloadCount, int configSize, boolean enabled) {}
}
