package openclaw.tools.llm;

import java.util.List;

/**
 * MiniMax AI provider.
 *
 * API Docs: https://www.minimaxi.com/
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public class MiniMaxProvider extends OpenAICompatibleProvider {

    public static final String DEFAULT_BASE_URL = "https://api.minimaxi.com/v1";

    public MiniMaxProvider(String apiKey) {
        super("minimax", apiKey, DEFAULT_BASE_URL);
    }

    public MiniMaxProvider(String apiKey, String baseUrl) {
        super("minimax", apiKey, baseUrl != null ? baseUrl : DEFAULT_BASE_URL);
    }

    @Override
    public List<ModelInfo> getModels() {
        return List.of(
                // M2.7 series - new default models
                new ModelInfo(
                        "minimax-m2.7",
                        "MiniMax-M2.7",
                        "MiniMax M2.7 model - improved performance and capabilities",
                        8192,
                        4096,
                        false,
                        true
                ),
                new ModelInfo(
                        "minimax-m2.7-highspeed",
                        "MiniMax-M2.7-HighSpeed",
                        "MiniMax M2.7 high-speed variant for faster responses",
                        8192,
                        4096,
                        false,
                        true
                ),
                // Legacy models
                new ModelInfo(
                        "abab5.5-chat",
                        "MiniMax abab5.5",
                        "MiniMax abab5.5 chat model",
                        8192,
                        4096,
                        false,
                        true
                ),
                new ModelInfo(
                        "abab6-chat",
                        "MiniMax abab6",
                        "MiniMax abab6 chat model",
                        16384,
                        4096,
                        false,
                        true
                ),
                new ModelInfo(
                        "abab6.5-chat",
                        "MiniMax abab6.5",
                        "MiniMax abab6.5 chat model with enhanced capabilities",
                        8192,
                        4096,
                        false,
                        true
                )
        );
    }

    @Override
    public String getDefaultModel() {
        // Updated default to M2.7 as per original update
        return "minimax-m2.7";
    }
}
