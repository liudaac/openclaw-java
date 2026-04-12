package openclaw.plugin.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Plugin manifest loader.
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public class ManifestLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ManifestLoader.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Loads a plugin manifest from a file path.
     */
    public static ManifestLoadResult load(Path manifestPath) {
        if (!Files.exists(manifestPath)) {
            return ManifestLoadResult.error("Manifest file not found: " + manifestPath, manifestPath.toString());
        }
        
        try {
            String content = Files.readString(manifestPath);
            return loadFromString(content, manifestPath.toString());
        } catch (IOException e) {
            logger.error("Failed to read manifest file: {}", manifestPath, e);
            return ManifestLoadResult.error("Failed to read manifest: " + e.getMessage(), manifestPath.toString());
        }
    }
    
    /**
     * Loads a plugin manifest from a string.
     */
    public static ManifestLoadResult loadFromString(String content, String manifestPath) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = mapper.readValue(content, Map.class);
            PluginManifest manifest = normalizeManifest(raw);
            return ManifestLoadResult.success(manifest, manifestPath);
        } catch (Exception e) {
            logger.error("Failed to parse manifest: {}", manifestPath, e);
            return ManifestLoadResult.error("Failed to parse manifest: " + e.getMessage(), manifestPath);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static PluginManifest normalizeManifest(Map<String, Object> raw) {
        String id = normalizeString(raw.get("id"));
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Manifest must have an 'id' field");
        }
        
        String name = normalizeString(raw.get("name"));
        String description = normalizeString(raw.get("description"));
        String version = normalizeString(raw.get("version"));
        
        Map<String, Object> configSchema = raw.get("configSchema") instanceof Map 
            ? (Map<String, Object>) raw.get("configSchema") : Map.of();
        
        boolean enabledByDefault = Boolean.TRUE.equals(raw.get("enabledByDefault"));
        
        return new PluginManifest(
            id,
            name != null ? name : id,
            description,
            version != null ? version : "1.0.0",
            configSchema,
            Optional.empty(),
            enabledByDefault,
            normalizeStringList(raw.get("autoEnableWhenConfiguredProviders")),
            normalizeStringList(raw.get("legacyPluginIds")),
            parseKind(normalizeString(raw.get("kind"))),
            normalizeStringList(raw.get("channels")),
            normalizeStringList(raw.get("providers")),
            Optional.ofNullable(normalizeString(raw.get("providerDiscoveryEntry"))),
            Optional.empty(),
            normalizeStringList(raw.get("cliBackends")),
            List.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            List.of(),
            Optional.empty(),
            Optional.empty(),
            normalizeStringList(raw.get("skills")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
    
    private static String normalizeString(Object value) {
        if (value instanceof String) {
            String str = ((String) value).trim();
            return str.isEmpty() ? null : str;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private static List<String> normalizeStringList(Object value) {
        if (value instanceof List) {
            return ((List<Object>) value).stream()
                .map(ManifestLoader::normalizeString)
                .filter(Objects::nonNull)
                .toList();
        }
        return List.of();
    }
    
    private static PluginManifest.PluginKind parseKind(String kind) {
        if (kind == null) return PluginManifest.PluginKind.TOOL;
        try {
            return PluginManifest.PluginKind.valueOf(kind.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PluginManifest.PluginKind.TOOL;
        }
    }
    
    /**
     * Manifest load result.
     */
    public sealed interface ManifestLoadResult {
        boolean ok();
        PluginManifest manifest();
        String error();
        String manifestPath();
        
        static ManifestLoadResult success(PluginManifest manifest, String manifestPath) {
            return new Success(manifest, manifestPath);
        }
        
        static ManifestLoadResult error(String error, String manifestPath) {
            return new Failure(error, manifestPath);
        }
        
        record Success(PluginManifest manifest, String manifestPath) implements ManifestLoadResult {
            @Override
            public boolean ok() { return true; }
            @Override
            public String error() { return null; }
        }
        
        record Failure(String error, String manifestPath) implements ManifestLoadResult {
            @Override
            public boolean ok() { return false; }
            @Override
            public PluginManifest manifest() { return null; }
        }
    }
}
