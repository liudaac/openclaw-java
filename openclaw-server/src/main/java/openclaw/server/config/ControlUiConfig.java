package openclaw.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Control UI 配置
 */
@Component
@ConfigurationProperties(prefix = "openclaw.gateway.control-ui")
public class ControlUiConfig {
    
    private boolean enabled = true;
    private String basePath = "/";
    private List<String> allowedOrigins;
    private boolean allowInsecureAuth = false;
    private boolean dangerouslyDisableDeviceAuth = false;
    private String staticPath = "classpath:static/control-ui";
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getBasePath() {
        return basePath;
    }
    
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }
    
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
    
    public boolean isAllowInsecureAuth() {
        return allowInsecureAuth;
    }
    
    public void setAllowInsecureAuth(boolean allowInsecureAuth) {
        this.allowInsecureAuth = allowInsecureAuth;
    }
    
    public boolean isDangerouslyDisableDeviceAuth() {
        return dangerouslyDisableDeviceAuth;
    }
    
    public void setDangerouslyDisableDeviceAuth(boolean dangerouslyDisableDeviceAuth) {
        this.dangerouslyDisableDeviceAuth = dangerouslyDisableDeviceAuth;
    }
    
    public String getStaticPath() {
        return staticPath;
    }
    
    public void setStaticPath(String staticPath) {
        this.staticPath = staticPath;
    }
}
