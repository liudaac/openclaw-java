package openclaw.gateway.webchat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for persisting WebChat inbound images to disk.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class WebChatImagePersistence {

    private static final Logger logger = LoggerFactory.getLogger(WebChatImagePersistence.class);

    private final HttpClient httpClient;
    private final Path storagePath;

    public WebChatImagePersistence(Path storagePath) {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.storagePath = storagePath;
    }

    /**
     * Persists an image from URL to disk.
     *
     * @param imageUrl the image URL
     * @param conversationId the conversation ID
     * @return CompletableFuture with the persisted file path
     */
    public CompletableFuture<Path> persistImage(String imageUrl, String conversationId) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Image URL cannot be null or empty"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create conversation directory
                Path conversationDir = storagePath.resolve(sanitizeFilename(conversationId));
                Files.createDirectories(conversationDir);

                // Generate unique filename
                String extension = extractExtension(imageUrl);
                String filename = Instant.now().getEpochSecond() + "_" + UUID.randomUUID() + extension;
                Path targetPath = conversationDir.resolve(filename);

                // Download image
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(imageUrl))
                        .GET()
                        .build();

                HttpResponse<Path> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofFile(targetPath)
                );

                if (response.statusCode() != 200) {
                    Files.deleteIfExists(targetPath);
                    throw new IOException("Failed to download image: HTTP " + response.statusCode());
                }

                logger.info("Persisted WebChat image: {} -> {}", imageUrl, targetPath);
                return targetPath;

            } catch (Exception e) {
                logger.error("Failed to persist image from {}: {}", imageUrl, e.getMessage());
                throw new RuntimeException("Failed to persist image", e);
            }
        });
    }

    /**
     * Sanitizes a filename.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Extracts file extension from URL.
     */
    private String extractExtension(String url) {
        if (url == null) {
            return ".jpg";
        }
        int lastDot = url.lastIndexOf('.');
        int lastSlash = url.lastIndexOf('/');
        if (lastDot > lastSlash && lastDot > 0) {
            String ext = url.substring(lastDot);
            if (ext.length() <= 5) {
                return ext;
            }
        }
        return ".jpg";
    }
}
