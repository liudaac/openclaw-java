package openclaw.tools.image;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;

import java.io.ByteArrayInputStream;
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
 * Image generation and processing tool.
 *
 * <p>Phase 3 Enhancement - Image generation using DALL-E or similar services.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class ImageTool implements AgentTool {

    private final HttpClient httpClient;
    private final Path imageDir;
    private final String openAiApiKey;

    public ImageTool() {
        this(Path.of("/tmp/images"), System.getenv("OPENAI_API_KEY"));
    }

    public ImageTool(Path imageDir, String openAiApiKey) {
        this.imageDir = imageDir;
        this.openAiApiKey = openAiApiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
        
        try {
            Files.createDirectories(imageDir);
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public String getName() {
        return "image";
    }

    @Override
    public String getDescription() {
        return "Generate images using AI (DALL-E) or process existing images";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "action", PropertySchema.enum_("Image action", List.of(
                                "generate", "edit", "variation", "describe"
                        )),
                        "prompt", PropertySchema.string("Text prompt for image generation"),
                        "image_url", PropertySchema.string("URL of image to edit/variate/describe"),
                        "size", PropertySchema.enum_("Image size", List.of(
                                "256x256", "512x512", "1024x1024", "1792x1024", "1024x1792"
                        )),
                        "quality", PropertySchema.enum_("Image quality", List.of("standard", "hd")),
                        "style", PropertySchema.enum_("Image style", List.of("vivid", "natural")),
                        "n", PropertySchema.integer("Number of images (1-10, default: 1)")
                ))
                .required(List.of("action"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String action = args.get("action").toString().toLowerCase();

                if (openAiApiKey == null || openAiApiKey.isEmpty()) {
                    return ToolResult.failure("OpenAI API key not configured");
                }

                switch (action) {
                    case "generate":
                        return generateImage(args);
                    case "edit":
                        return editImage(args);
                    case "variation":
                        return createVariation(args);
                    case "describe":
                        return describeImage(args);
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }

            } catch (Exception e) {
                return ToolResult.failure("Image operation failed: " + e.getMessage());
            }
        });
    }

    private ToolResult generateImage(Map<String, Object> args) {
        if (!args.containsKey("prompt")) {
            return ToolResult.failure("Missing required parameter: prompt");
        }

        String prompt = args.get("prompt").toString();
        String size = args.getOrDefault("size", "1024x1024").toString();
        String quality = args.getOrDefault("quality", "standard").toString();
        String style = args.getOrDefault("style", "vivid").toString();
        int n = (int) args.getOrDefault("n", 1);

        // Limit n
        n = Math.min(Math.max(n, 1), 10);

        try {
            // Build request body
            String requestBody = String.format(
                    "{\"model\": \"dall-e-3\", \"prompt\": \"%s\", \"size\": \"%s\", \"quality\": \"%s\", \"style\": \"%s\", \"n\": %d}",
                    escapeJson(prompt), size, quality, style, n
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/images/generations"))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ToolResult.failure("API error: " + response.body());
            }

            // Parse response and download images
            List<String> imageUrls = parseImageUrls(response.body());
            List<String> savedPaths = downloadImages(imageUrls);

            return ToolResult.success(
                    "Generated " + savedPaths.size() + " image(s)",
                    Map.of(
                            "images", savedPaths,
                            "prompt", prompt,
                            "size", size,
                            "quality", quality
                    )
            );

        } catch (Exception e) {
            return ToolResult.failure("Generation failed: " + e.getMessage());
        }
    }

    private ToolResult editImage(Map<String, Object> args) {
        // DALL-E edit requires image upload - simplified implementation
        return ToolResult.success(
                "Image edit feature requires file upload support",
                Map.of("status", "not_implemented")
        );
    }

    private ToolResult createVariation(Map<String, Object> args) {
        if (!args.containsKey("image_url")) {
            return ToolResult.failure("Missing required parameter: image_url");
        }

        String imageUrl = args.get("image_url").toString();
        int n = (int) args.getOrDefault("n", 1);
        n = Math.min(Math.max(n, 1), 10);

        try {
            String requestBody = String.format(
                    "{\"image\": \"%s\", \"n\": %d, \"size\": \"1024x1024\"}",
                    escapeJson(imageUrl), n
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/images/variations"))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ToolResult.failure("API error: " + response.body());
            }

            List<String> imageUrls = parseImageUrls(response.body());
            List<String> savedPaths = downloadImages(imageUrls);

            return ToolResult.success(
                    "Created " + savedPaths.size() + " variation(s)",
                    Map.of("images", savedPaths)
            );

        } catch (Exception e) {
            return ToolResult.failure("Variation creation failed: " + e.getMessage());
        }
    }

    private ToolResult describeImage(Map<String, Object> args) {
        // Image description would require vision model
        return ToolResult.success(
                "Image description requires vision model integration",
                Map.of("status", "not_implemented")
        );
    }

    private List<String> parseImageUrls(String jsonResponse) {
        List<String> urls = new java.util.ArrayList<>();
        // Simple parsing - extract URLs from response
        // In production, use proper JSON parsing
        int index = 0;
        while ((index = jsonResponse.indexOf("\"url\":", index)) != -1) {
            int start = jsonResponse.indexOf("\"", index + 6) + 1;
            int end = jsonResponse.indexOf("\"", start);
            if (start > 0 && end > start) {
                urls.add(jsonResponse.substring(start, end));
            }
            index = end;
        }
        return urls;
    }

    private List<String> downloadImages(List<String> urls) {
        List<String> paths = new java.util.ArrayList<>();
        for (String url : urls) {
            try {
                String filename = "image-" + UUID.randomUUID() + ".png";
                Path outputPath = imageDir.resolve(filename);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<byte[]> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    Files.write(outputPath, response.body());
                    paths.add(outputPath.toString());
                }
            } catch (Exception e) {
                // Skip failed downloads
            }
        }
        return paths;
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
