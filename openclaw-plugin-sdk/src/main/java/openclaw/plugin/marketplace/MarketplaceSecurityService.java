package openclaw.plugin.marketplace;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Security service for Plugin Marketplace operations.
 * Implements security hardening from original TypeScript marketplace.ts
 */
public class MarketplaceSecurityService {

    private static final java.util.logging.Logger logger = 
        java.util.logging.Logger.getLogger(MarketplaceSecurityService.class.getName());
    
    // Security constants from original code
    private static final long MAX_MARKETPLACE_ARCHIVE_BYTES = 256L * 1024 * 1024; // 256MB
    private static final long DEFAULT_DOWNLOAD_TIMEOUT_MS = 120_000; // 120 seconds
    private static final long MIN_DOWNLOAD_TIMEOUT_MS = 1_000; // 1 second minimum
    
    // Patterns for security checks
    private static final Pattern DRIVE_RELATIVE_PATTERN = Pattern.compile("^[a-zA-Z]:");
    private static final Pattern ABSOLUTE_PATH_PATTERN = Pattern.compile("^/|^\\\\");
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\./|\\.\\\\|\\.\\./|\\.\\.\\\\");
    
    // Sensitive URL patterns to redact
    private static final Set<String> SENSITIVE_URL_PATTERNS = Set.of(
        "token=", "api_key=", "apikey=", "key=", "secret=", 
        "password=", "passwd=", "pwd=", "auth=", "access_token="
    );
    
    private final HttpClient httpClient;
    
    public MarketplaceSecurityService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    /**
     * Resolve a safe download file name from URL.
     * Rejects drive-relative paths, absolute paths, and path traversal attempts.
     */
    public String resolveSafeDownloadFileName(String url, String fallback) throws SecurityException {
        if (url == null || url.isBlank()) {
            throw new SecurityException("URL is null or empty");
        }
        
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                return validateFileName(fallback);
            }
            
            String fileName = Path.of(path).getFileName().toString();
            if (fileName.isEmpty()) {
                return validateFileName(fallback);
            }
            
            return validateFileName(fileName);
        } catch (URISyntaxException e) {
            throw new SecurityException("Invalid URL: " + sanitizeForLog(url), e);
        }
    }
    
    /**
     * Validate file name for security issues.
     */
    private String validateFileName(String fileName) throws SecurityException {
        if (fileName == null || fileName.isBlank()) {
            throw new SecurityException("File name is null or empty");
        }
        
        String trimmed = fileName.trim();
        
        // Reject drive-relative paths (Windows C:filename)
        if (DRIVE_RELATIVE_PATTERN.matcher(trimmed).find()) {
            throw new SecurityException("Drive-relative paths are not allowed: " + sanitizeForLog(trimmed));
        }
        
        // Reject absolute paths
        if (ABSOLUTE_PATH_PATTERN.matcher(trimmed).find()) {
            throw new SecurityException("Absolute paths are not allowed: " + sanitizeForLog(trimmed));
        }
        
        // Reject path traversal
        if (PATH_TRAVERSAL_PATTERN.matcher(trimmed).find()) {
            throw new SecurityException("Path traversal is not allowed: " + sanitizeForLog(trimmed));
        }
        
        // Reject special directories
        if (trimmed.equals(".") || trimmed.equals("..")) {
            throw new SecurityException("Special directory references are not allowed");
        }
        
        // Check for path separators
        if (trimmed.contains("/") || trimmed.contains("\\")) {
            throw new SecurityException("Path separators are not allowed in file name: " + sanitizeForLog(trimmed));
        }
        
        return trimmed;
    }
    
    /**
     * Stream download with size limit and timeout.
     * Uses streaming to avoid memory issues with large files.
     */
    public DownloadResult streamDownload(String url, Path target, Long timeoutMs) throws SecurityException {
        long resolvedTimeoutMs = resolveDownloadTimeoutMs(timeoutMs);
        String sanitizedUrl = sanitizeForLog(redactSensitiveUrl(url));
        
        logger.info("Starting download from: " + sanitizedUrl);
        
        // Ensure parent directory exists
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new SecurityException("Failed to create target directory: " + e.getMessage(), e);
        }
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(resolvedTimeoutMs))
            .header("User-Agent", "OpenClaw-Java/1.0")
            .GET()
            .build();
        
        Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");
        
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            
            if (response.statusCode() != 200) {
                throw new SecurityException("HTTP error " + response.statusCode() + " for: " + sanitizedUrl);
            }
            
            // Stream download with size limit
            try (InputStream inputStream = response.body()) {
                long totalBytes = streamWithSizeLimit(inputStream, tempFile, MAX_MARKETPLACE_ARCHIVE_BYTES);
                
                // Move temp file to target
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                
                logger.info("Download completed: " + totalBytes + " bytes from " + sanitizedUrl);
                return new DownloadResult(true, target, totalBytes, null);
            }
            
        } catch (java.net.http.HttpTimeoutException e) {
            cleanupOnFailure(tempFile);
            throw new SecurityException("Download timed out after " + resolvedTimeoutMs + "ms: " + sanitizedUrl, e);
        } catch (InterruptedException e) {
            cleanupOnFailure(tempFile);
            Thread.currentThread().interrupt();
            throw new SecurityException("Download interrupted: " + sanitizedUrl, e);
        } catch (IOException e) {
            cleanupOnFailure(tempFile);
            throw new SecurityException("Download failed: " + sanitizedUrl + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * Stream input to file with size limit check.
     */
    private long streamWithSizeLimit(InputStream inputStream, Path target, long maxBytes) throws IOException, SecurityException {
        long totalBytes = 0;
        byte[] buffer = new byte[8192];
        int bytesRead;
        
        try (var outputStream = Files.newOutputStream(target)) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;
                
                if (totalBytes > maxBytes) {
                    throw new SecurityException(
                        "Archive exceeds maximum size of " + formatBytes(maxBytes) + 
                        " (" + totalBytes + " bytes received)"
                    );
                }
                
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        return totalBytes;
    }
    
    /**
     * Clean up temporary file on failure.
     */
    private void cleanupOnFailure(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
                logger.fine("Cleaned up temporary file: " + tempFile);
            } catch (IOException e) {
                logger.warning("Failed to clean up temporary file: " + tempFile + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Resolve download timeout with validation.
     */
    private long resolveDownloadTimeoutMs(Long timeoutMs) {
        if (timeoutMs == null || timeoutMs <= 0) {
            return DEFAULT_DOWNLOAD_TIMEOUT_MS;
        }
        return Math.max(MIN_DOWNLOAD_TIMEOUT_MS, timeoutMs);
    }
    
    /**
     * Redact sensitive information from URL.
     */
    public String redactSensitiveUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        String result = url;
        for (String pattern : SENSITIVE_URL_PATTERNS) {
            // Simple redaction: replace value after pattern with ***
            int index = result.toLowerCase().indexOf(pattern);
            while (index != -1) {
                int valueStart = index + pattern.length();
                int valueEnd = result.indexOf("&", valueStart);
                if (valueEnd == -1) {
                    valueEnd = result.length();
                }
                result = result.substring(0, valueStart) + "***" + result.substring(valueEnd);
                index = result.toLowerCase().indexOf(pattern, valueStart + 3);
            }
        }
        return result;
    }
    
    /**
     * Sanitize string for logging.
     */
    public String sanitizeForLog(String message) {
        if (message == null) {
            return "null";
        }
        // Remove ANSI escape sequences and control characters
        return message.replaceAll("\\x1B\\[[0-9;]*[a-zA-Z]", "")
                     .replaceAll("[\\x00-\\x1F\\x7F]", "");
    }
    
    /**
     * Format bytes to human readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        return bytes + " bytes";
    }
    
    // Record types
    
    public record DownloadResult(
        boolean success,
        Path filePath,
        long bytesDownloaded,
        String error
    ) {
        public boolean isSuccess() {
            return success;
        }
    }
}