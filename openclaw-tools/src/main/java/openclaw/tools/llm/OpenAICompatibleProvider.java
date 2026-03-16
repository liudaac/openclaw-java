package openclaw.tools.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Base class for OpenAI-compatible API providers.
 *
 * Supports providers: OpenAI, Moonshot, MiniMax, Mistral, etc.
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public abstract class OpenAICompatibleProvider implements LLMProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleProvider.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    protected final String apiKey;
    protected final String baseUrl;
    protected final String providerName;

    public OpenAICompatibleProvider(String providerName, String apiKey, String baseUrl) {
        this.providerName = providerName;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            String url = baseUrl + "/chat/completions";
            String requestBody = buildRequestBody(request);

            String response = makeRequest(url, requestBody);
            return parseResponse(response, request.model());

        } catch (Exception e) {
            logger.error("Chat request failed", e);
            throw new RuntimeException("Chat request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(ChatRequest request, StreamCallback callback) {
        try {
            String url = baseUrl + "/chat/completions";
            ChatRequest streamRequest = new ChatRequest(
                    request.model(),
                    request.messages(),
                    request.temperature(),
                    request.maxTokens(),
                    true,
                    request.tools()
            );
            String requestBody = buildRequestBody(streamRequest);

            makeStreamRequest(url, requestBody, callback);

        } catch (Exception e) {
            logger.error("Stream request failed", e);
            callback.onError(e);
        }
    }

    /**
     * Builds the request body JSON.
     */
    protected String buildRequestBody(ChatRequest request) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", request.model());

        ArrayNode messagesArray = root.putArray("messages");
        for (Message message : request.messages()) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", message.role());

            if (message.contentParts() != null && !message.contentParts().isEmpty()) {
                // Multimodal content
                ArrayNode contentArray = msgNode.putArray("content");
                for (ContentPart part : message.contentParts()) {
                    ObjectNode partNode = contentArray.addObject();
                    partNode.put("type", part.type());
                    if (part.text() != null) {
                        partNode.put("text", part.text());
                    }
                    if (part.imageUrl() != null) {
                        ObjectNode imageNode = partNode.putObject("image_url");
                        imageNode.put("url", part.imageUrl().url());
                        if (part.imageUrl().detail() != null) {
                            imageNode.put("detail", part.imageUrl().detail());
                        }
                    }
                }
            } else {
                // Simple text content
                msgNode.put("content", message.content());
            }
        }

        if (request.temperature() != null) {
            root.put("temperature", request.temperature());
        }

        if (request.maxTokens() != null) {
            root.put("max_tokens", request.maxTokens());
        }

        if (request.stream() != null && request.stream()) {
            root.put("stream", true);
        }

        if (request.tools() != null && !request.tools().isEmpty()) {
            ArrayNode toolsArray = root.putArray("tools");
            for (Tool tool : request.tools()) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("type", tool.type());
                ObjectNode functionNode = toolNode.putObject("function");
                functionNode.put("name", tool.function().name());
                functionNode.put("description", tool.function().description());
                functionNode.set("parameters", mapper.valueToTree(tool.function().parameters()));
            }
        }

        return mapper.writeValueAsString(root);
    }

    /**
     * Parses the response JSON.
     */
    protected ChatResponse parseResponse(String responseBody, String model) throws IOException {
        JsonNode root = mapper.readTree(responseBody);

        String id = root.path("id").asText();
        String responseModel = root.path("model").asText(model);

        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path("message");
            String content = message.path("content").asText("");
            String finishReason = firstChoice.path("finish_reason").asText();

            // Parse tool calls
            ToolCall toolCall = null;
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                JsonNode firstToolCall = toolCalls.get(0);
                JsonNode function = firstToolCall.path("function");
                toolCall = new ToolCall(
                        firstToolCall.path("id").asText(),
                        firstToolCall.path("type").asText(),
                        new ToolFunctionCall(
                                function.path("name").asText(),
                                function.path("arguments").asText()
                        )
                );
            }

            // Parse usage
            JsonNode usage = root.path("usage");
            Integer promptTokens = usage.path("prompt_tokens").asInt();
            Integer completionTokens = usage.path("completion_tokens").asInt();
            Integer totalTokens = usage.path("total_tokens").asInt();

            return new ChatResponse(
                    id,
                    responseModel,
                    content,
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    finishReason,
                    toolCall
            );
        }

        throw new IOException("Invalid response format");
    }

    /**
     * Makes a synchronous HTTP request.
     */
    protected String makeRequest(String url, String requestBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new IOException("HTTP " + responseCode + ": " + errorBody);
        }

        return readStream(conn.getInputStream());
    }

    /**
     * Makes a streaming HTTP request.
     */
    protected void makeStreamRequest(String url, String requestBody, StreamCallback callback) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new IOException("HTTP " + responseCode + ": " + errorBody);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        String chunk = parseStreamChunk(data);
                        if (chunk != null && !chunk.isEmpty()) {
                            callback.onChunk(chunk);
                        }
                    } catch (Exception e) {
                        logger.error("Error parsing stream chunk", e);
                    }
                }
            }
            callback.onComplete();
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    /**
     * Parses a stream chunk.
     */
    protected String parseStreamChunk(String data) throws IOException {
        JsonNode root = mapper.readTree(data);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode delta = choices.get(0).path("delta");
            return delta.path("content").asText("");
        }
        return null;
    }

    /**
     * Reads an input stream to string.
     */
    protected String readStream(java.io.InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
