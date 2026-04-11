package openclaw.cli.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing exec policy.
 * 
 * @author OpenClaw Team
 * @version 2026.4.11
 */
public class ExecPolicyService {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    // Preset policies
    private static final Map<String, PolicyPreset> PRESETS = new HashMap<>();
    
    static {
        PRESETS.put("yolo", new PolicyPreset("gateway", "full", "off", "full"));
        PRESETS.put("cautious", new PolicyPreset("gateway", "allowlist", "on-miss", "deny"));
        PRESETS.put("deny-all", new PolicyPreset("gateway", "deny", "off", "deny"));
    }
    
    /**
     * Shows current exec policy.
     */
    public PolicyShowResult showPolicy() throws IOException {
        Path configPath = getConfigPath();
        Path approvalsPath = getApprovalsPath();
        
        boolean approvalsExists = Files.exists(approvalsPath);
        
        List<PolicyScope> scopes = collectPolicyScopes();
        
        boolean hasNodeRuntimeScope = scopes.stream()
            .anyMatch(s -> "node-runtime".equals(s.getRuntimeApprovalsSource()));
        
        String note = hasNodeRuntimeScope
            ? "Scopes requesting host=node are node-managed at runtime. Local approvals are shown only for local/gateway scopes."
            : "Effective exec policy is the host approvals file intersected with requested tools.exec policy.";
        
        return new PolicyShowResult(
            configPath.toString(),
            approvalsPath.toString(),
            approvalsExists,
            scopes,
            note
        );
    }
    
    /**
     * Applies a preset policy.
     */
    public PolicyApplyResult applyPreset(String presetName) throws IOException {
        PolicyPreset preset = PRESETS.get(presetName);
        if (preset == null) {
            throw new IllegalArgumentException("Unknown preset: " + presetName);
        }
        
        return applyPolicyValues(preset);
    }
    
    /**
     * Applies custom policy values.
     */
    public PolicyApplyResult applyPolicy(String host, String security, String ask, String askFallback) 
            throws IOException {
        PolicyPreset preset = new PolicyPreset(host, security, ask, askFallback);
        return applyPolicyValues(preset);
    }
    
    private PolicyApplyResult applyPolicyValues(PolicyPreset preset) throws IOException {
        // Validate host=node is not allowed for local policy
        if ("node".equals(preset.host)) {
            throw new IllegalArgumentException(
                "Local exec-policy cannot synchronize host=node. Node approvals are fetched from the node at runtime."
            );
        }
        
        // Read current config
        Path configPath = getConfigPath();
        Map<String, Object> config = readConfig(configPath);
        
        // Apply policy to config
        applyPolicyToConfig(config, preset);
        
        // Write config
        writeConfig(configPath, config);
        
        // Update approvals file
        Path approvalsPath = getApprovalsPath();
        Map<String, Object> approvals = readApprovals(approvalsPath);
        applyPolicyToApprovals(approvals, preset);
        writeApprovals(approvalsPath, approvals);
        
        // Return updated policy view
        PolicyShowResult showResult = showPolicy();
        return new PolicyApplyResult(showResult);
    }
    
    private List<PolicyScope> collectPolicyScopes() throws IOException {
        List<PolicyScope> scopes = new ArrayList<>();
        
        // Read config and approvals
        Map<String, Object> config = readConfig(getConfigPath());
        Map<String, Object> approvals = readApprovals(getApprovalsPath());
        
        // Get tools.exec config
        Map<String, Object> tools = (Map<String, Object>) config.getOrDefault("tools", Map.of());
        Map<String, Object> execConfig = (Map<String, Object>) tools.getOrDefault("exec", Map.of());
        
        // Get defaults from approvals
        Map<String, Object> defaults = (Map<String, Object>) approvals.getOrDefault("defaults", Map.of());
        
        // Create default scope
        String hostRequested = (String) execConfig.getOrDefault("host", "gateway");
        String securityRequested = (String) execConfig.getOrDefault("security", "allowlist");
        String askRequested = (String) execConfig.getOrDefault("ask", "on-miss");
        
        String securityHost = (String) defaults.getOrDefault("security", securityRequested);
        String askHost = (String) defaults.getOrDefault("ask", askRequested);
        String askFallback = (String) defaults.getOrDefault("askFallback", "deny");
        
        PolicyScope defaultScope = new PolicyScope(
            "default",
            hostRequested,
            "config",
            securityRequested,
            "config",
            askRequested,
            "config",
            securityHost,
            "approvals",
            askHost,
            "approvals",
            securityHost, // effective = host for simplicity
            askHost, // effective = host for simplicity
            askFallback,
            "approvals",
            "local-file"
        );
        
        scopes.add(defaultScope);
        
        return scopes;
    }
    
    private void applyPolicyToConfig(Map<String, Object> config, PolicyPreset preset) {
        Map<String, Object> tools = (Map<String, Object>) config.computeIfAbsent("tools", k -> new HashMap<>());
        Map<String, Object> exec = (Map<String, Object>) tools.computeIfAbsent("exec", k -> new HashMap<>());
        
        if (preset.host != null) {
            exec.put("host", preset.host);
        }
        if (preset.security != null) {
            exec.put("security", preset.security);
        }
        if (preset.ask != null) {
            exec.put("ask", preset.ask);
        }
    }
    
    private void applyPolicyToApprovals(Map<String, Object> approvals, PolicyPreset preset) {
        Map<String, Object> defaults = (Map<String, Object>) approvals.computeIfAbsent("defaults", k -> new HashMap<>());
        
        if (preset.security != null) {
            defaults.put("security", preset.security);
        }
        if (preset.ask != null) {
            defaults.put("ask", preset.ask);
        }
        if (preset.askFallback != null) {
            defaults.put("askFallback", preset.askFallback);
        }
        
        approvals.put("version", 1);
    }
    
    private Map<String, Object> readConfig(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new HashMap<>();
        }
        String content = Files.readString(path);
        if (content.isBlank()) {
            return new HashMap<>();
        }
        return mapper.readValue(content, Map.class);
    }
    
    private void writeConfig(Path path, Map<String, Object> config) throws IOException {
        Files.createDirectories(path.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), config);
    }
    
    private Map<String, Object> readApprovals(Path path) throws IOException {
        if (!Files.exists(path)) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("version", 1);
            empty.put("defaults", new HashMap<>());
            return empty;
        }
        String content = Files.readString(path);
        if (content.isBlank()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("version", 1);
            empty.put("defaults", new HashMap<>());
            return empty;
        }
        return mapper.readValue(content, Map.class);
    }
    
    private void writeApprovals(Path path, Map<String, Object> approvals) throws IOException {
        Files.createDirectories(path.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), approvals);
    }
    
    private Path getConfigPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".openclaw", "config.json");
    }
    
    private Path getApprovalsPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".openclaw", "approvals.json");
    }
    
    // Policy Preset
    private static class PolicyPreset {
        final String host;
        final String security;
        final String ask;
        final String askFallback;
        
        PolicyPreset(String host, String security, String ask, String askFallback) {
            this.host = host;
            this.security = security;
            this.ask = ask;
            this.askFallback = askFallback;
        }
    }
    
    // Result classes
    public static class PolicyShowResult {
        private final String configPath;
        private final String approvalsPath;
        private final boolean approvalsExists;
        private final List<PolicyScope> scopes;
        private final String note;
        
        public PolicyShowResult(String configPath, String approvalsPath, boolean approvalsExists,
                                List<PolicyScope> scopes, String note) {
            this.configPath = configPath;
            this.approvalsPath = approvalsPath;
            this.approvalsExists = approvalsExists;
            this.scopes = scopes;
            this.note = note;
        }
        
        public String getConfigPath() { return configPath; }
        public String getApprovalsPath() { return approvalsPath; }
        public boolean isApprovalsExists() { return approvalsExists; }
        public List<PolicyScope> getScopes() { return scopes; }
        public String getNote() { return note; }
        
        public String toJson() throws IOException {
            ObjectNode root = mapper.createObjectNode();
            root.put("configPath", configPath);
            root.put("approvalsPath", approvalsPath);
            root.put("approvalsExists", approvalsExists);
            root.put("note", note);
            
            ArrayNode scopesArray = root.putArray("scopes");
            for (PolicyScope scope : scopes) {
                ObjectNode scopeNode = scopesArray.addObject();
                scopeNode.put("scopeLabel", scope.getScopeLabel());
                scopeNode.put("hostRequested", scope.getHostRequested());
                scopeNode.put("securityRequested", scope.getSecurityRequested());
                scopeNode.put("askRequested", scope.getAskRequested());
                scopeNode.put("securityEffective", scope.getSecurityEffective());
                scopeNode.put("askEffective", scope.getAskEffective());
            }
            
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        }
    }
    
    public static class PolicyApplyResult {
        private final PolicyShowResult showResult;
        
        public PolicyApplyResult(PolicyShowResult showResult) {
            this.showResult = showResult;
        }
        
        public PolicyShowResult getShowResult() { return showResult; }
        
        public String toJson() throws IOException {
            return showResult.toJson();
        }
    }
    
    public static class PolicyScope {
        private final String scopeLabel;
        private final String hostRequested;
        private final String hostRequestedSource;
        private final String securityRequested;
        private final String securityRequestedSource;
        private final String askRequested;
        private final String askRequestedSource;
        private final String securityHost;
        private final String securityHostSource;
        private final String askHost;
        private final String askHostSource;
        private final String securityEffective;
        private final String askEffective;
        private final String askFallbackEffective;
        private final String askFallbackSource;
        private final String runtimeApprovalsSource;
        
        public PolicyScope(String scopeLabel,
                          String hostRequested, String hostRequestedSource,
                          String securityRequested, String securityRequestedSource,
                          String askRequested, String askRequestedSource,
                          String securityHost, String securityHostSource,
                          String askHost, String askHostSource,
                          String securityEffective, String askEffective,
                          String askFallbackEffective, String askFallbackSource,
                          String runtimeApprovalsSource) {
            this.scopeLabel = scopeLabel;
            this.hostRequested = hostRequested;
            this.hostRequestedSource = hostRequestedSource;
            this.securityRequested = securityRequested;
            this.securityRequestedSource = securityRequestedSource;
            this.askRequested = askRequested;
            this.askRequestedSource = askRequestedSource;
            this.securityHost = securityHost;
            this.securityHostSource = securityHostSource;
            this.askHost = askHost;
            this.askHostSource = askHostSource;
            this.securityEffective = securityEffective;
            this.askEffective = askEffective;
            this.askFallbackEffective = askFallbackEffective;
            this.askFallbackSource = askFallbackSource;
            this.runtimeApprovalsSource = runtimeApprovalsSource;
        }
        
        public String getScopeLabel() { return scopeLabel; }
        public String getHostRequested() { return hostRequested; }
        public String getHostRequestedSource() { return hostRequestedSource; }
        public String getSecurityRequested() { return securityRequested; }
        public String getSecurityRequestedSource() { return securityRequestedSource; }
        public String getAskRequested() { return askRequested; }
        public String getAskRequestedSource() { return askRequestedSource; }
        public String getSecurityHost() { return securityHost; }
        public String getSecurityHostSource() { return securityHostSource; }
        public String getAskHost() { return askHost; }
        public String getAskHostSource() { return askHostSource; }
        public String getSecurityEffective() { return securityEffective; }
        public String getAskEffective() { return askEffective; }
        public String getAskFallbackEffective() { return askFallbackEffective; }
        public String getAskFallbackSource() { return askFallbackSource; }
        public String getRuntimeApprovalsSource() { return runtimeApprovalsSource; }
    }
}