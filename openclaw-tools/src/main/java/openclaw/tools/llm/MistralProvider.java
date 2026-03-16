package openclaw.tools.llm;

import java.util.List;

/**
 * Mistral AI provider.
 *
 * API Docs: https://docs.mistral.ai/
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public class MistralProvider extends OpenAICompatibleProvider {

    public static final String DEFAULT_BASE_URL = "https://api.mistral.ai/v1";

    public MistralProvider(String apiKey) {
        super("mistral", apiKey, DEFAULT_BASE_URL);
    }

    public MistralProvider(String apiKey, String baseUrl) {
        super("mistral", apiKey, baseUrl != null ? baseUrl : DEFAULT_BASE_URL);
    }

    @Override
    public List<ModelInfo> getModels() {
        return List.of(
                new ModelInfo(
                        "mistral-tiny",
                        "Mistral Tiny",
                        "Fast and cost-effective model for simple tasks",
                        32768,
                        4096,
                        false,
                        false
                ),
                new ModelInfo(
                        "mistral-small",
                        "Mistral Small",
                        "Balanced model for most tasks",
                        32768,
                        4096,
                        false,
                        true
                ),
                new ModelInfo(
                        "mistral-medium",
                        "Mistral Medium",
                        "High-quality model for complex tasks",
                        32768,
                        4096,
                        false,
                        true
                ),
                new ModelInfo(
                        "mistral-large",
                        "Mistral Large",
                        "Most capable model for demanding tasks",
                        32768,
                        4096,
                        false,
                        true
                ),
                new ModelInfo(
                        "pixtral-large",
                        "Pixtral Large",
                        "Vision-capable model",
                        128000,
                        4096,
                        true,
                        true
                )
        );
    }
}
