package openclaw.server.controller;

import openclaw.server.config.ControlUiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Control UI 静态文件控制器
 * 
 * 提供 Control UI 的静态文件服务
 */
@RestController
@RequestMapping("${openclaw.gateway.control-ui.base-path:/}")
public class ControlUiController {
    
    private static final Logger logger = LoggerFactory.getLogger(ControlUiController.class);
    
    @Autowired
    private ControlUiConfig controlUiConfig;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    /**
     * 提供 index.html
     */
    @GetMapping({"", "/"})
    public ResponseEntity<String> getIndex() {
        if (!controlUiConfig.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Resource resource = resourceLoader.getResource(
                controlUiConfig.getStaticPath() + "/index.html");
            
            if (!resource.exists()) {
                // 如果静态文件不存在，返回占位页面
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(getPlaceholderHtml());
            }
            
            String content = new String(resource.getInputStream().readAllBytes(), 
                StandardCharsets.UTF_8);
            
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
                
        } catch (IOException e) {
            logger.error("Failed to load index.html", e);
            return ResponseEntity.internalServerError()
                .body("Error loading Control UI");
        }
    }
    
    /**
     * 提供静态资源 (JS, CSS, 图片等)
     */
    @GetMapping("/assets/{filename:.+}")
    public ResponseEntity<Resource> getAsset(@PathVariable String filename) {
        if (!controlUiConfig.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            String resourcePath = controlUiConfig.getStaticPath() + "/assets/" + filename;
            Resource resource = resourceLoader.getResource(resourcePath);
            
            if (!resource.exists()) {
                logger.warn("Asset not found: {}", filename);
                return ResponseEntity.notFound().build();
            }
            
            // 根据文件扩展名设置 Content-Type
            MediaType mediaType = getMediaType(filename);
            
            return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Failed to load asset: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 提供图标文件
     */
    @GetMapping("/{filename:favicon.*|apple-touch-icon.*}")
    public ResponseEntity<Resource> getIcon(@PathVariable String filename) {
        if (!controlUiConfig.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            String resourcePath = controlUiConfig.getStaticPath() + "/" + filename;
            Resource resource = resourceLoader.getResource(resourcePath);
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            MediaType mediaType = filename.endsWith(".svg") ? 
                MediaType.valueOf("image/svg+xml") : MediaType.IMAGE_PNG;
            
            return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Failed to load icon: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取占位 HTML (当静态文件不存在时)
     */
    private String getPlaceholderHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>OpenClaw Java</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: #1a1a2e;
                        color: #fff;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                    }
                    .container {
                        text-align: center;
                        padding: 2rem;
                    }
                    h1 {
                        font-size: 2.5rem;
                        margin-bottom: 1rem;
                        color: #00d4ff;
                    }
                    p {
                        font-size: 1.1rem;
                        color: #a0a0a0;
                        margin-bottom: 2rem;
                    }
                    .status {
                        display: inline-block;
                        padding: 0.5rem 1rem;
                        background: #00d4ff;
                        color: #1a1a2e;
                        border-radius: 20px;
                        font-weight: bold;
                    }
                    .api-info {
                        margin-top: 2rem;
                        padding: 1rem;
                        background: rgba(255,255,255,0.1);
                        border-radius: 8px;
                        font-family: monospace;
                        font-size: 0.9rem;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🦞 OpenClaw Java</h1>
                    <p>Gateway is running successfully</p>
                    <span class="status">✓ Operational</span>
                    <div class="api-info">
                        <p>WebSocket: ws://localhost:18789/ws</p>
                        <p>HTTP API: http://localhost:18789/api</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
    
    /**
     * 根据文件名获取 MediaType
     */
    private MediaType getMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".js")) {
            return MediaType.valueOf("application/javascript");
        } else if (lower.endsWith(".css")) {
            return MediaType.valueOf("text/css");
        } else if (lower.endsWith(".svg")) {
            return MediaType.valueOf("image/svg+xml");
        } else if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        } else if (lower.endsWith(".woff2")) {
            return MediaType.valueOf("font/woff2");
        } else if (lower.endsWith(".woff")) {
            return MediaType.valueOf("font/woff");
        } else if (lower.endsWith(".ttf")) {
            return MediaType.valueOf("font/ttf");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
