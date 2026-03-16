package openclaw.tools.media;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Media processing tool for images and files.
 *
 * <p>Phase 3 Enhancement - Image resizing, format conversion, and file handling.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class MediaHandler implements AgentTool {

    private final HttpClient httpClient;
    private final Path mediaDir;

    public MediaHandler() {
        this(Path.of("/tmp/media"));
    }

    public MediaHandler(Path mediaDir) {
        this.mediaDir = mediaDir;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
        
        try {
            Files.createDirectories(mediaDir);
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public String getName() {
        return "media";
    }

    @Override
    public String getDescription() {
        return "Process media files: resize images, convert formats, download files";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "action", PropertySchema.enum_("Media action", List.of(
                                "resize", "convert", "download", "info", "thumbnail"
                        )),
                        "source", PropertySchema.string("Source file path or URL"),
                        "destination", PropertySchema.string("Destination file path"),
                        "width", PropertySchema.integer("Target width"),
                        "height", PropertySchema.integer("Target height"),
                        "format", PropertySchema.enum_("Output format", List.of(
                                "png", "jpg", "jpeg", "gif", "webp", "bmp"
                        )),
                        "quality", PropertySchema.integer("JPEG quality (1-100, default: 85)"),
                        "maintain_aspect", PropertySchema.boolean_("Maintain aspect ratio (default: true)")
                ))
                .required(List.of("action", "source"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String action = args.get("action").toString().toLowerCase();
                String source = args.get("source").toString();

                switch (action) {
                    case "resize":
                        return resizeImage(args, source);
                    case "convert":
                        return convertFormat(args, source);
                    case "download":
                        return downloadFile(args, source);
                    case "info":
                        return getFileInfo(args, source);
                    case "thumbnail":
                        return createThumbnail(args, source);
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }

            } catch (Exception e) {
                return ToolResult.failure("Media operation failed: " + e.getMessage());
            }
        });
    }

    private ToolResult resizeImage(Map<String, Object> args, String source) {
        if (!args.containsKey("width") && !args.containsKey("height")) {
            return ToolResult.failure("Missing required parameter: width or height");
        }

        int targetWidth = (int) args.getOrDefault("width", 0);
        int targetHeight = (int) args.getOrDefault("height", 0);
        boolean maintainAspect = (boolean) args.getOrDefault("maintain_aspect", true);

        try {
            // Load source image
            BufferedImage sourceImage = loadImage(source);
            if (sourceImage == null) {
                return ToolResult.failure("Failed to load image: " + source);
            }

            // Calculate dimensions
            int originalWidth = sourceImage.getWidth();
            int originalHeight = sourceImage.getHeight();

            if (maintainAspect) {
                if (targetWidth > 0 && targetHeight == 0) {
                    targetHeight = (int) ((double) targetWidth / originalWidth * originalHeight);
                } else if (targetHeight > 0 && targetWidth == 0) {
                    targetWidth = (int) ((double) targetHeight / originalHeight * originalWidth);
                }
            }

            if (targetWidth == 0) targetWidth = originalWidth;
            if (targetHeight == 0) targetHeight = originalHeight;

            // Resize
            Image scaledImage = sourceImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            // Save
            String destPath = args.getOrDefault("destination", 
                    mediaDir.resolve("resized-" + UUID.randomUUID() + ".jpg").toString()).toString();
            String format = getFormatFromPath(destPath);
            
            Path outputPath = Path.of(destPath);
            ImageIO.write(outputImage, format, outputPath.toFile());

            return ToolResult.success(
                    "Image resized: " + outputPath.toAbsolutePath(),
                    Map.of(
                            "path", outputPath.toString(),
                            "original_width", originalWidth,
                            "original_height", originalHeight,
                            "new_width", targetWidth,
                            "new_height", targetHeight
                    )
            );

        } catch (Exception e) {
            return ToolResult.failure("Resize failed: " + e.getMessage());
        }
    }

    private ToolResult convertFormat(Map<String, Object> args, String source) {
        if (!args.containsKey("format")) {
            return ToolResult.failure("Missing required parameter: format");
        }

        String format = args.get("format").toString();

        try {
            BufferedImage sourceImage = loadImage(source);
            if (sourceImage == null) {
                return ToolResult.failure("Failed to load image: " + source);
            }

            String destPath = args.getOrDefault("destination",
                    mediaDir.resolve("converted-" + UUID.randomUUID() + "." + format).toString()).toString();

            Path outputPath = Path.of(destPath);
            ImageIO.write(sourceImage, format, outputPath.toFile());

            return ToolResult.success(
                    "Image converted: " + outputPath.toAbsolutePath(),
                    Map.of(
                            "path", outputPath.toString(),
                            "format", format,
                            "width", sourceImage.getWidth(),
                            "height", sourceImage.getHeight()
                    )
            );

        } catch (Exception e) {
            return ToolResult.failure("Conversion failed: " + e.getMessage());
        }
    }

    private ToolResult downloadFile(Map<String, Object> args, String source) {
        try {
            String filename = source.substring(source.lastIndexOf('/') + 1);
            if (filename.isEmpty()) {
                filename = "download-" + UUID.randomUUID();
            }

            String destPath = args.getOrDefault("destination",
                    mediaDir.resolve(filename).toString()).toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(source))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                return ToolResult.failure("Download failed: HTTP " + response.statusCode());
            }

            Path outputPath = Path.of(destPath);
            Files.write(outputPath, response.body());

            return ToolResult.success(
                    "File downloaded: " + outputPath.toAbsolutePath(),
                    Map.of(
                            "path", outputPath.toString(),
                            "url", source,
                            "size", response.body().length
                    )
            );

        } catch (Exception e) {
            return ToolResult.failure("Download failed: " + e.getMessage());
        }
    }

    private ToolResult getFileInfo(Map<String, Object> args, String source) {
        try {
            BufferedImage image = loadImage(source);
            if (image == null) {
                // Try as regular file
                Path path = Path.of(source);
                if (Files.exists(path)) {
                    return ToolResult.success(
                            "File info",
                            Map.of(
                                    "path", source,
                                    "size", Files.size(path),
                                    "exists", true
                            )
                    );
                }
                return ToolResult.failure("File not found: " + source);
            }

            return ToolResult.success(
                    "Image info",
                    Map.of(
                            "path", source,
                            "width", image.getWidth(),
                            "height", image.getHeight(),
                            "type", image.getType()
                    )
            );

        } catch (Exception e) {
            return ToolResult.failure("Info failed: " + e.getMessage());
        }
    }

    private ToolResult createThumbnail(Map<String, Object> args, String source) {
        int thumbWidth = (int) args.getOrDefault("width", 150);
        int thumbHeight = (int) args.getOrDefault("height", 150);

        // Update args for resize
        Map<String, Object> thumbArgs = new java.util.HashMap<>(args);
        thumbArgs.put("width", thumbWidth);
        thumbArgs.put("height", thumbHeight);
        thumbArgs.put("maintain_aspect", true);
        
        if (!thumbArgs.containsKey("destination")) {
            thumbArgs.put("destination", 
                    mediaDir.resolve("thumb-" + UUID.randomUUID() + ".jpg").toString());
        }

        return resizeImage(thumbArgs, source);
    }

    private BufferedImage loadImage(String source) throws IOException, InterruptedException {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            // Download and load
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(source))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return ImageIO.read(new java.io.ByteArrayInputStream(response.body()));
            }
            return null;
        } else {
            // Load from file
            Path path = Path.of(source);
            if (Files.exists(path)) {
                return ImageIO.read(path.toFile());
            }
            return null;
        }
    }

    private String getFormatFromPath(String path) {
        String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            case "gif" -> "gif";
            case "webp" -> "webp";
            case "bmp" -> "bmp";
            default -> "jpg";
        };
    }
}
