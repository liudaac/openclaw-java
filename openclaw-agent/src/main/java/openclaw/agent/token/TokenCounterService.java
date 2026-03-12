package openclaw.agent.token;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Counter Service
 * 
 * <p>Provides accurate token counting for various OpenAI models.
 * Uses jtokkit library (Java port of tiktoken).</p>
 */
@Service
public class TokenCounterService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenCounterService.class);
    
    private final EncodingRegistry registry;
    private final Map<String, Encoding> encodingCache;
    
    // Token limits for different models
    private static final Map<String, Integer> MODEL_TOKEN_LIMITS = Map.of(
        "gpt-4", 8192,
        "gpt-4-32k", 32768,
        "gpt-3.5-turbo", 4096,
        "gpt-3.5-turbo-16k", 16384,
        "text-embedding-ada-002", 8191
    );
    
    // Default token limits
    private static final int DEFAULT_CONTEXT_LIMIT = 4096;
    private static final int DEFAULT_COMPLETION_LIMIT = 1024;
    private static final int SAFETY_MARGIN = 100; // Reserve tokens for safety
    
    public TokenCounterService() {
        this.registry = Encodings.newDefaultEncodingRegistry();
        this.encodingCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Count tokens in text for a specific model
     * 
     * @param text the text to count
     * @param model the model name (e.g., "gpt-4", "gpt-3.5-turbo")
     * @return the number of tokens
     */
    public int countTokens(String text, String model) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        try {
            Encoding encoding = getEncoding(model);
            return encoding.countTokens(text);
        } catch (Exception e) {
            logger.warn("Failed to count tokens for model: {}, using estimation", model, e);
            // Fallback: rough estimation (1 token ≈ 4 characters)
            return text.length() / 4;
        }
    }
    
    /**
     * Count tokens using default model (gpt-3.5-turbo)
     */
    public int countTokens(String text) {
        return countTokens(text, "gpt-3.5-turbo");
    }
    
    /**
     * Count tokens in a conversation message
     * 
     * @param role the message role (system, user, assistant)
     * @param content the message content
     * @param model the model name
     * @return the number of tokens
     */
    public int countMessageTokens(String role, String content, String model) {
        // ChatML format: <|im_start|>{role}\n{content}<|im_end|>\n
        int tokens = 0;
        tokens += 3; // <|im_start|>
        tokens += countTokens(role, model);
        tokens += 1; // \n
        tokens += countTokens(content, model);
        tokens += 3; // <|im_end|>
        tokens += 1; // \n
        return tokens;
    }
    
    /**
     * Calculate available tokens for completion
     * 
     * @param promptTokens number of tokens in the prompt
     * @param model the model name
     * @return available tokens for completion
     */
    public int getAvailableCompletionTokens(int promptTokens, String model) {
        int contextLimit = getContextLimit(model);
        int maxCompletionTokens = getCompletionLimit(model);
        
        int available = contextLimit - promptTokens - SAFETY_MARGIN;
        return Math.min(available, maxCompletionTokens);
    }
    
    /**
     * Check if prompt exceeds context limit
     */
    public boolean isPromptTooLong(int promptTokens, String model) {
        return promptTokens > getContextLimit(model) - SAFETY_MARGIN;
    }
    
    /**
     * Get context token limit for a model
     */
    public int getContextLimit(String model) {
        return MODEL_TOKEN_LIMITS.getOrDefault(model, DEFAULT_CONTEXT_LIMIT);
    }
    
    /**
     * Get completion token limit for a model
     */
    public int getCompletionLimit(String model) {
        // Typically 1/4 to 1/2 of context limit
        return getContextLimit(model) / 2;
    }
    
    /**
     * Get encoding for a model
     */
    private Encoding getEncoding(String model) {
        return encodingCache.computeIfAbsent(model, m -> {
            try {
                // Map model names to ModelType
                ModelType modelType = mapToModelType(m);
                return registry.getEncodingForModel(modelType);
            } catch (Exception e) {
                logger.warn("Failed to get encoding for model: {}, using default", m);
                return registry.getEncodingForModel(ModelType.GPT_3_5_TURBO);
            }
        });
    }
    
    /**
     * Map model name to ModelType
     */
    private ModelType mapToModelType(String model) {
        if (model == null) {
            return ModelType.GPT_3_5_TURBO;
        }
        
        if (model.startsWith("gpt-4-32k")) {
            return ModelType.GPT_4_32K;
        } else if (model.startsWith("gpt-4")) {
            return ModelType.GPT_4;
        } else if (model.contains("16k")) {
            return ModelType.GPT_3_5_TURBO_16K;
        } else {
            return ModelType.GPT_3_5_TURBO;
        }
    }
    
    /**
     * Estimate cost based on token count
     * 
     * @param inputTokens number of input tokens
     * @param outputTokens number of output tokens
     * @param model the model name
     * @return estimated cost in USD
     */
    public double estimateCost(int inputTokens, int outputTokens, String model) {
        // Pricing per 1K tokens (as of 2024)
        Map<String, double[]> PRICING = Map.of(
            "gpt-4", new double[]{0.03, 0.06},           // input, output
            "gpt-4-32k", new double[]{0.06, 0.12},
            "gpt-3.5-turbo", new double[]{0.0015, 0.002},
            "gpt-3.5-turbo-16k", new double[]{0.003, 0.004}
        );
        
        double[] prices = PRICING.getOrDefault(model, PRICING.get("gpt-3.5-turbo"));
        double inputCost = (inputTokens / 1000.0) * prices[0];
        double outputCost = (outputTokens / 1000.0) * prices[1];
        
        return inputCost + outputCost;
    }
    
    /**
     * Get token usage statistics
     */
    public TokenUsageStats getStats(int inputTokens, int outputTokens, String model) {
        return new TokenUsageStats(
            inputTokens,
            outputTokens,
            inputTokens + outputTokens,
            getContextLimit(model),
            getAvailableCompletionTokens(inputTokens, model),
            estimateCost(inputTokens, outputTokens, model)
        );
    }
    
    /**
     * Token usage statistics
     */
    public record TokenUsageStats(
        int inputTokens,
        int outputTokens,
        int totalTokens,
        int contextLimit,
        int availableCompletionTokens,
        double estimatedCost
    ) {}
}
