package openclaw.tools.llm;

import java.util.List;
import java.util.Map;

/**
 * LLM Provider interface for different AI model services.
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public interface LLMProvider {

    /**
     * Gets the provider name.
     *
     * @return the provider name
     */
    String getName();

    /**
     * Gets available models.
     *
     * @return list of model information
     */
    List<ModelInfo> getModels();

    /**
     * Sends a chat completion request.
     *
     * @param request the chat request
     * @return the chat response
     */
    ChatResponse chat(ChatRequest request);

    /**
     * Streams chat completion.
     *
     * @param request the chat request
     * @param callback the stream callback
     */
    void chatStream(ChatRequest request, StreamCallback callback);

    /**
     * Model information.
     */
    record ModelInfo(
            String id,
            String name,
            String description,
            int contextWindow,
            int maxTokens,
            boolean supportsVision,
            boolean supportsTools
    ) {}

    /**
     * Chat request.
     */
    record ChatRequest(
            String model,
            List<Message> messages,
            Double temperature,
            Integer maxTokens,
            Boolean stream,
            List<Tool> tools
    ) {
        public ChatRequest(String model, List<Message> messages) {
            this(model, messages, 0.7, null, false, null);
        }
    }

    /**
     * Chat message.
     */
    record Message(
            String role,
            String content,
            List<ContentPart> contentParts
    ) {
        public static Message system(String content) {
            return new Message("system", content, null);
        }

        public static Message user(String content) {
            return new Message("user", content, null);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content, null);
        }
    }

    /**
     * Content part for multimodal messages.
     */
    record ContentPart(
            String type,
            String text,
            ImageUrl imageUrl
    ) {}

    /**
     * Image URL for vision models.
     */
    record ImageUrl(
            String url,
            String detail
    ) {}

    /**
     * Tool definition.
     */
    record Tool(
            String type,
            ToolFunction function
    ) {}

    /**
     * Tool function.
     */
    record ToolFunction(
            String name,
            String description,
            Map<String, Object> parameters
    ) {}

    /**
     * Chat response.
     */
    record ChatResponse(
            String id,
            String model,
            String content,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String finishReason,
            ToolCall toolCall
    ) {}

    /**
     * Tool call in response.
     */
    record ToolCall(
            String id,
            String type,
            ToolFunctionCall function
    ) {}

    /**
     * Tool function call.
     */
    record ToolFunctionCall(
            String name,
            String arguments
    ) {}

    /**
     * Stream callback interface.
     */
    interface StreamCallback {
        void onChunk(String chunk);
        void onComplete();
        void onError(Throwable error);
    }
}
