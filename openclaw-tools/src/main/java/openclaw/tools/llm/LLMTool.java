package openclaw.tools.llm;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.AgentTool.PropertySchema;
import openclaw.sdk.tool.AgentTool.ToolParameters;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM Tool for interacting with various AI model providers.
 *
 * Supported providers:
 * - moonshot (Moonshot / 月之暗面 / Kimi)
 * - minimax (MiniMax)
 * - mistral (Mistral AI)
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
@Component
public class LLMTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(LLMTool.class);

    private final Map<String, LLMProvider> providers = new ConcurrentHashMap<>();

    public LLMTool() {
        // Initialize with environment variables if available
        initProvidersFromEnv();
    }

    private void initProvidersFromEnv() {
        // Moonshot
        String moonshotKey = System.getenv("MOONSHOT_API_KEY");
        if (moonshotKey != null && !moonshotKey.isEmpty()) {
            String baseUrl = System.getenv("MOONSHOT_BASE_URL");
            providers.put("moonshot", new MoonshotProvider(moonshotKey, baseUrl));
            logger.info("Initialized Moonshot provider");
        }

        // MiniMax
        String minimaxKey = System.getenv("MINIMAX_API_KEY");
        if (minimaxKey != null && !minimaxKey.isEmpty()) {
            String baseUrl = System.getenv("MINIMAX_BASE_URL");
            providers.put("minimax", new MiniMaxProvider(minimaxKey, baseUrl));
            logger.info("Initialized MiniMax provider");
        }

        // Mistral
        String mistralKey = System.getenv("MISTRAL_API_KEY");
        if (mistralKey != null && !mistralKey.isEmpty()) {
            String baseUrl = System.getenv("MISTRAL_BASE_URL");
            providers.put("mistral", new MistralProvider(mistralKey, baseUrl));
            logger.info("Initialized Mistral provider");
        }
    }

    @Override
    public String getName() {
        return "llm";
    }

    @Override
    public String getDescription() {
        return "Chat with AI models from various providers (Moonshot, MiniMax, Mistral)";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.ofEntries(
                        Map.entry("action", PropertySchema.enum_("Action to perform", List.of(
                                "chat",
                                "chat_stream",
                                "list_models",
                                "list_providers"
                        ))),
                        Map.entry("provider", PropertySchema.enum_("AI provider", List.of(
                                "moonshot", "minimax", "mistral"
                        ))),
                        Map.entry("model", PropertySchema.string("Model ID to use")),
                        Map.entry("message", PropertySchema.string("User message to send")),
                        Map.entry("messages", PropertySchema.array("Conversation history", PropertySchema.string("JSON message object"))),
                        Map.entry("system_prompt", PropertySchema.string("System prompt/instruction")),
                        Map.entry("temperature", PropertySchema.integer("Temperature (0-100, default: 70)")),
                        Map.entry("max_tokens", PropertySchema.integer("Maximum tokens to generate")),
                        Map.entry("api_key", PropertySchema.string("API key (if not using env var)")),
                        Map.entry("base_url", PropertySchema.string("Custom base URL (optional)"))
                ))
                .required(List.of("action"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> args = context.arguments();
                String action = (String) args.get("action");

                switch (action) {
                    case "chat":
                        return chat(args);
                    case "chat_stream":
                        return chatStream(args);
                    case "list_models":
                        return listModels(args);
                    case "list_providers":
                        return listProviders();
                    default:
                        return ToolResult.failure("Unknown action: " + action);
                }
            } catch (Exception e) {
                logger.error("LLM tool execution failed", e);
                return ToolResult.failure("Execution failed: " + e.getMessage());
            }
        });
    }

    /**
     * Send a chat request.
     */
    private ToolResult chat(Map<String, Object> args) {
        String providerName = (String) args.get("provider");
        String model = (String) args.get("model");
        String message = (String) args.get("message");
        String systemPrompt = (String) args.get("system_prompt");

        if (providerName == null || message == null) {
            return ToolResult.failure("provider and message are required");
        }

        LLMProvider provider = getOrCreateProvider(providerName, args);
        if (provider == null) {
            return ToolResult.failure("Provider not available: " + providerName +
                    ". Please set " + providerName.toUpperCase() + "_API_KEY environment variable.");
        }

        // Use default model if not specified
        if (model == null || model.isEmpty()) {
            model = provider.getModels().get(0).id();
        }

        try {
            // Build messages
            List<LLMProvider.Message> messages = new java.util.ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(LLMProvider.Message.system(systemPrompt));
            }
            messages.add(LLMProvider.Message.user(message));

            // Build request
            int temperature = (int) args.getOrDefault("temperature", 70);
            Integer maxTokens = (Integer) args.get("max_tokens");

            LLMProvider.ChatRequest request = new LLMProvider.ChatRequest(
                    model,
                    messages,
                    temperature / 100.0,
                    maxTokens,
                    false,
                    null
            );

            LLMProvider.ChatResponse response = provider.chat(request);

            Map<String, Object> result = new HashMap<>();
            result.put("content", response.content());
            result.put("model", response.model());
            result.put("finish_reason", response.finishReason());
            if (response.promptTokens() != null) {
                result.put("prompt_tokens", response.promptTokens());
            }
            if (response.completionTokens() != null) {
                result.put("completion_tokens", response.completionTokens());
            }
            if (response.totalTokens() != null) {
                result.put("total_tokens", response.totalTokens());
            }

            return ToolResult.success("Chat completed", result);

        } catch (Exception e) {
            return ToolResult.failure("Chat request failed: " + e.getMessage());
        }
    }

    /**
     * Send a streaming chat request.
     */
    private ToolResult chatStream(Map<String, Object> args) {
        // For now, return non-streaming response
        // Full streaming support would require async response handling
        return chat(args);
    }

    /**
     * List available models for a provider.
     */
    private ToolResult listModels(Map<String, Object> args) {
        String providerName = (String) args.get("provider");
        if (providerName == null) {
            return ToolResult.failure("provider is required");
        }

        LLMProvider provider = providers.get(providerName);
        if (provider == null) {
            return ToolResult.failure("Provider not available: " + providerName);
        }

        List<Map<String, Object>> modelList = provider.getModels().stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.id());
                    map.put("name", m.name());
                    map.put("description", m.description());
                    map.put("context_window", m.contextWindow());
                    map.put("max_tokens", m.maxTokens());
                    map.put("supports_vision", m.supportsVision());
                    map.put("supports_tools", m.supportsTools());
                    return map;
                })
                .toList();

        return ToolResult.success("Available models for " + providerName, Map.of(
                "provider", providerName,
                "models", modelList
        ));
    }

    /**
     * List available providers.
     */
    private ToolResult listProviders() {
        List<Map<String, Object>> providerList = providers.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("available", true);
                    map.put("model_count", e.getValue().getModels().size());
                    return map;
                })
                .toList();

        return ToolResult.success("Available LLM providers", Map.of(
                "providers", providerList,
                "count", providerList.size()
        ));
    }

    /**
     * Get or create a provider instance.
     */
    private LLMProvider getOrCreateProvider(String name, Map<String, Object> args) {
        // Check if already initialized
        LLMProvider provider = providers.get(name);
        if (provider != null) {
            return provider;
        }

        // Try to create from args
        String apiKey = (String) args.get("api_key");
        String baseUrl = (String) args.get("base_url");

        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        switch (name.toLowerCase()) {
            case "moonshot":
                provider = new MoonshotProvider(apiKey, baseUrl);
                break;
            case "minimax":
                provider = new MiniMaxProvider(apiKey, baseUrl);
                break;
            case "mistral":
                provider = new MistralProvider(apiKey, baseUrl);
                break;
            default:
                return null;
        }

        providers.put(name, provider);
        return provider;
    }
}