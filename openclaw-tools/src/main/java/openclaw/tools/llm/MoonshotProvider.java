package openclaw.tools.llm;

import java.util.List;

/**
 * Moonshot (月之暗面 / Kimi) AI provider.
 *
 * API Docs: https://platform.moonshot.cn/
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public class MoonshotProvider extends OpenAICompatibleProvider {

    public static final String DEFAULT_BASE_URL = "https://api.moonshot.cn/v1";

    public MoonshotProvider(String apiKey) {
        super("moonshot", apiKey, DEFAULT_BASE_URL);
    }

    public MoonshotProvider(String apiKey, String baseUrl) {
        super("moonshot", apiKey, baseUrl != null ? baseUrl : DEFAULT_BASE_URL);
    }

    @Override
    public List<ModelInfo> getModels() {
        return List.of(
                new ModelInfo(
                        "moonshot-v1-8k",
                        "Moonshot v1 8K",
                        "Moonshot AI model with 8K context window",
                        8192,
                        4096,
                        false,
                        true
                ),
                new ModelInfo(
                        "moonshot-v1-32k",
                        "Moonshot v1 32K",
                        "Moonshot AI model with 32K context window",
                        32768,
                        4096,
                        false,
                        true
                ),
                new ModelInfo(
                        "moonshot-v1-128k",
                        "Moonshot v1 128K",
                        "Moonshot AI model with 128K context window",
                        131072,
                        4096,
                        false,
                        true
                )
        );
    }
}
