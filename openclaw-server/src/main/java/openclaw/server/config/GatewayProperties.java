package openclaw.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Gateway Configuration Properties
 */
@Component
@ConfigurationProperties(prefix = "openclaw.gateway")
public class GatewayProperties {
    
    private String host = "localhost";
    private int port = 8080;
    private boolean enabled = true;
    private Map<String, NodeConfig> nodes;
    private QueueConfig queue;
    private SecurityConfig security;
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public Map<String, NodeConfig> getNodes() { return nodes; }
    public void setNodes(Map<String, NodeConfig> nodes) { this.nodes = nodes; }
    
    public QueueConfig getQueue() { return queue; }
    public void setQueue(QueueConfig queue) { this.queue = queue; }
    
    public SecurityConfig getSecurity() { return security; }
    public void setSecurity(SecurityConfig security) { this.security = security; }
    
    public static class NodeConfig {
        private String host;
        private int port;
        private boolean enabled;
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class QueueConfig {
        private int maxSize = 1000;
        private int workers = 4;
        
        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
        
        public int getWorkers() { return workers; }
        public void setWorkers(int workers) { this.workers = workers; }
    }
    
    public static class SecurityConfig {
        private boolean enabled = true;
        private String apiKey;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }
}
