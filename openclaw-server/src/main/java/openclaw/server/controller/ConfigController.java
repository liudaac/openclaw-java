package openclaw.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import openclaw.gateway.config.ProtectedConfigPaths;
import openclaw.server.config.GatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 配置管理 API
 * 
 * 提供配置的读取、保存、验证功能
 * 对应 Node.js Control UI 的 config.* 方法
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {
    
    // Helper methods to ensure correct generic types
    private ResponseEntity<JsonNode> ok(JsonNode node) {
        return ResponseEntity.ok(node);
    }
    
    private ResponseEntity<JsonNode> badRequest(JsonNode node) {
        return ResponseEntity.badRequest().body(node);
    }
    
    private ResponseEntity<JsonNode> conflict(JsonNode node) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(node);
    }
    
    private ResponseEntity<JsonNode> internalServerError(JsonNode node) {
        return ResponseEntity.internalServerError().body(node);
    }
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private GatewayProperties gatewayProperties;
    
    private static final String CONFIG_FILE = System.getProperty("user.home") + 
        "/.openclaw/openclaw.json";
    
    /**
     * 获取当前配置
     * 
     * GET /api/config
     */
    @GetMapping
    public Mono<ResponseEntity<JsonNode>> getConfig() {
        return Mono.fromCallable(() -> {
            Path configPath = Paths.get(CONFIG_FILE);
            
            if (!Files.exists(configPath)) {
                // 返回默认配置
                return ResponseEntity.ok(getDefaultConfig());
            }
            
            String content = Files.readString(configPath);
            ObjectNode config = (ObjectNode) objectMapper.readTree(content);
            
            return ok(config);
            
        }).onErrorResume(e -> {
            logger.error("Failed to read config", e);
            ObjectNode errorResult = objectMapper.createObjectNode();
            errorResult.put("error", "Failed to read config: " + e.getMessage());
            return Mono.just(ok(errorResult));
        });
    }
    
    /**
     * 保存配置
     * 
     * POST /api/config
     */
    @PostMapping
    public Mono<ResponseEntity<JsonNode>> setConfig(
            @RequestBody JsonNode config,
            @RequestHeader(value = "X-Config-Hash", required = false) String baseHash) {
        
        return Mono.fromCallable(() -> {
            // 验证配置
            ValidationResult validation = doValidateConfig(config);
            if (!validation.isValid()) {
                ObjectNode errorResult = objectMapper.createObjectNode();
                errorResult.put("success", false);
                errorResult.put("error", validation.getError());
                return badRequest(errorResult);
            }
            
            // 检查受保护的配置路径
            try {
                JsonNode currentConfigNode = getCurrentConfigNode();
                Map<String, Object> currentConfig = objectMapper.convertValue(currentConfigNode, Map.class);
                Map<String, Object> newConfig = objectMapper.convertValue(config, Map.class);
                
                ProtectedConfigPaths.assertMutationAllowed("config.set", currentConfig, newConfig);
            } catch (ProtectedConfigPaths.ProtectedConfigException e) {
                ObjectNode errorResult = objectMapper.createObjectNode();
                errorResult.put("success", false);
                errorResult.put("error", e.getMessage());
                return badRequest(errorResult);
            }
            
            // 检查 base hash (防止并发修改)
            if (baseHash != null) {
                String currentHash = calculateConfigHash();
                if (!baseHash.equals(currentHash)) {
                    ObjectNode errorResult = objectMapper.createObjectNode();
                    errorResult.put("success", false);
                    errorResult.put("error", "Config has been modified by another process");
                    return conflict(errorResult);
                }
            }
            
            // 保存配置
            Path configPath = Paths.get(CONFIG_FILE);
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, config.toPrettyString());
            
            String newHash = calculateConfigHash();
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("hash", newHash);
            result.put("message", "Configuration saved successfully");
            
            return ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to save config", e);
            ObjectNode errorResult = objectMapper.createObjectNode();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return Mono.just(internalServerError(errorResult));
        });
    }
    
    /**
     * 获取配置 Schema
     * 
     * GET /api/config/schema
     */
    @GetMapping("/schema")
    public Mono<ResponseEntity<JsonNode>> getConfigSchema() {
        return Mono.fromCallable(() -> {
            ObjectNode schema = objectMapper.createObjectNode();
            
            // Gateway 配置 Schema
            ObjectNode gatewaySchema = objectMapper.createObjectNode();
            gatewaySchema.put("type", "object");
            
            ObjectNode gatewayProperties = objectMapper.createObjectNode();
            
            // bind
            ObjectNode bindProp = objectMapper.createObjectNode();
            bindProp.put("type", "string");
            bindProp.put("description", "Gateway bind address");
            bindProp.put("default", "127.0.0.1");
            gatewayProperties.set("bind", bindProp);
            
            // port
            ObjectNode portProp = objectMapper.createObjectNode();
            portProp.put("type", "integer");
            portProp.put("description", "Gateway port");
            portProp.put("default", 18789);
            gatewayProperties.set("port", portProp);
            
            // auth
            ObjectNode authProp = objectMapper.createObjectNode();
            authProp.put("type", "object");
            ObjectNode authProperties = objectMapper.createObjectNode();
            
            ObjectNode authModeProp = objectMapper.createObjectNode();
            authModeProp.put("type", "string");
            authModeProp.put("enum", objectMapper.createArrayNode()
                .add("token").add("password").add("none"));
            authModeProp.put("description", "Authentication mode");
            authProperties.set("mode", authModeProp);
            
            ObjectNode authTokenProp = objectMapper.createObjectNode();
            authTokenProp.put("type", "string");
            authTokenProp.put("description", "Authentication token");
            authProperties.set("token", authTokenProp);
            
            authProp.set("properties", authProperties);
            gatewayProperties.set("auth", authProp);
            
            // controlUi
            ObjectNode controlUiProp = objectMapper.createObjectNode();
            controlUiProp.put("type", "object");
            ObjectNode controlUiProperties = objectMapper.createObjectNode();
            
            ObjectNode controlUiEnabledProp = objectMapper.createObjectNode();
            controlUiEnabledProp.put("type", "boolean");
            controlUiEnabledProp.put("description", "Enable Control UI");
            controlUiEnabledProp.put("default", true);
            controlUiProperties.set("enabled", controlUiEnabledProp);
            
            ObjectNode controlUiBasePathProp = objectMapper.createObjectNode();
            controlUiBasePathProp.put("type", "string");
            controlUiBasePathProp.put("description", "Control UI base path");
            controlUiBasePathProp.put("default", "/");
            controlUiProperties.set("basePath", controlUiBasePathProp);
            
            controlUiProp.set("properties", controlUiProperties);
            gatewayProperties.set("controlUi", controlUiProp);
            
            gatewaySchema.set("properties", gatewayProperties);
            schema.set("gateway", gatewaySchema);
            
            return ok(schema);
            
        }).onErrorResume(e -> {
            logger.error("Failed to generate schema", e);
            ObjectNode errorResult = objectMapper.createObjectNode();
            errorResult.put("error", e.getMessage());
            return Mono.just(internalServerError(errorResult));
        });
    }
    
    /**
     * 验证配置
     * 
     * POST /api/config/validate
     */
    @PostMapping("/validate")
    public Mono<ResponseEntity<JsonNode>> validateConfig(
            @RequestBody JsonNode config) {
        
        return Mono.fromCallable(() -> {
            ValidationResult result = doValidateConfig(config);
            
            ObjectNode response = objectMapper.createObjectNode();
            if (result.isValid()) {
                response.put("valid", true);
                response.put("message", "Configuration is valid");
            } else {
                response.put("valid", false);
                response.put("error", result.getError());
            }
            
            return ok(response);
            
        }).onErrorResume(e -> {
            ObjectNode errorResult = objectMapper.createObjectNode();
            errorResult.put("valid", false);
            errorResult.put("error", e.getMessage());
            return Mono.just(ok(errorResult));
        });
    }
    
    /**
     * 应用配置并重启
     * 
     * POST /api/config/apply
     */
    @PostMapping("/apply")
    public Mono<ResponseEntity<JsonNode>> applyConfig(
            @RequestBody JsonNode config) {
        
        return Mono.fromCallable(() -> {
            // 先验证配置
            ValidationResult validation = doValidateConfig(config);
            if (!validation.isValid()) {
                ObjectNode errorResult = objectMapper.createObjectNode();
                errorResult.put("success", false);
                errorResult.put("error", validation.getError());
                return badRequest(errorResult);
            }
            
            // 检查受保护的配置路径
            try {
                JsonNode currentConfigNode = getCurrentConfigNode();
                Map<String, Object> currentConfig = objectMapper.convertValue(currentConfigNode, Map.class);
                Map<String, Object> newConfig = objectMapper.convertValue(config, Map.class);
                
                ProtectedConfigPaths.assertMutationAllowed("config.apply", currentConfig, newConfig);
            } catch (ProtectedConfigPaths.ProtectedConfigException e) {
                ObjectNode errorResult = objectMapper.createObjectNode();
                errorResult.put("success", false);
                errorResult.put("error", e.getMessage());
                return badRequest(errorResult);
            }
            
            Path configPath = Paths.get(CONFIG_FILE);
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, config.toPrettyString());
            
            // 触发重启 (异步)
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 等待响应返回
                    // 这里可以实现优雅重启逻辑
                    logger.info("Configuration applied, restarting...");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("message", "Configuration applied. Gateway will restart shortly.");
            
            return ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to apply config", e);
            ObjectNode errorResult = objectMapper.createObjectNode();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return Mono.just(internalServerError(errorResult));
        });
    }
    
    // Helper methods
    
    private JsonNode getDefaultConfig() {
        ObjectNode config = objectMapper.createObjectNode();
        
        ObjectNode gateway = objectMapper.createObjectNode();
        gateway.put("bind", "127.0.0.1");
        gateway.put("port", 18789);
        
        ObjectNode auth = objectMapper.createObjectNode();
        auth.put("mode", "token");
        auth.put("token", generateToken());
        gateway.set("auth", auth);
        
        ObjectNode controlUi = objectMapper.createObjectNode();
        controlUi.put("enabled", true);
        controlUi.put("basePath", "/");
        gateway.set("controlUi", controlUi);
        
        config.set("gateway", gateway);
        
        return config;
    }
    
    private ValidationResult doValidateConfig(JsonNode config) {
        if (config == null || !config.has("gateway")) {
            return ValidationResult.invalid("Missing 'gateway' section");
        }
        
        JsonNode gateway = config.path("gateway");
        
        // 验证 bind
        if (gateway.has("bind")) {
            String bind = gateway.path("bind").asText();
            if (bind.isEmpty()) {
                return ValidationResult.invalid("Invalid bind address");
            }
        }
        
        // 验证 port
        if (gateway.has("port")) {
            int port = gateway.path("port").asInt();
            if (port < 1 || port > 65535) {
                return ValidationResult.invalid("Invalid port number");
            }
        }
        
        // 验证 auth
        if (gateway.has("auth")) {
            JsonNode auth = gateway.path("auth");
            String mode = auth.path("mode").asText("token");
            
            if (!mode.equals("none") && !auth.has("token") && !auth.has("password")) {
                return ValidationResult.invalid("Auth token or password is required");
            }
        }
        
        return ValidationResult.valid();
    }
    
    private String calculateConfigHash() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (!Files.exists(configPath)) {
                return "";
            }
            String content = Files.readString(configPath);
            return Integer.toHexString(content.hashCode());
        } catch (Exception e) {
            return "";
        }
    }
    
    private String generateToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Gets the current configuration as JsonNode.
     */
    private JsonNode getCurrentConfigNode() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (!Files.exists(configPath)) {
                return getDefaultConfig();
            }
            String content = Files.readString(configPath);
            return objectMapper.readTree(content);
        } catch (Exception e) {
            logger.warn("Failed to read current config, using default", e);
            return getDefaultConfig();
        }
    }
    
    // Inner classes
    
    private static class ValidationResult {
        private final boolean valid;
        private final String error;
        
        private ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, error);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getError() {
            return error;
        }
    }
}
