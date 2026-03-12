package openclaw.sdk.provider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Provider authentication method.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ProviderAuthMethod {

    /**
     * Gets the method ID.
     *
     * @return the method ID
     */
    String getId();

    /**
     * Gets the method label.
     *
     * @return the label
     */
    String getLabel();

    /**
     * Gets the method hint.
     *
     * @return the hint if available
     */
    default Optional<String> getHint() {
        return Optional.empty();
    }

    /**
     * Gets the authentication kind.
     *
     * @return the auth kind
     */
    ProviderAuthKind getKind();

    /**
     * Runs the authentication flow.
     *
     * @param context the auth context
     * @return the auth result
     */
    CompletableFuture<ProviderAuthResult> run(ProviderAuthContext context);

    /**
     * Authentication kind enumeration.
     */
    enum ProviderAuthKind {
        OAUTH,
        API_KEY,
        TOKEN,
        DEVICE_CODE,
        CUSTOM
    }

    /**
     * Authentication context.
     *
     * @param config the OpenClaw config
     * @param agentDir the agent directory
     * @param workspaceDir the workspace directory
     * @param isRemote whether running remotely
     * @param prompter the wizard prompter
     */
    record ProviderAuthContext(
            Map<String, Object> config,
            Optional<String> agentDir,
            Optional<String> workspaceDir,
            boolean isRemote,
            WizardPrompter prompter
    ) {
    }

    /**
     * Wizard prompter interface.
     */
    interface WizardPrompter {

        /**
         * Prompts for text input.
         *
         * @param message the prompt message
         * @return the user input
         */
        CompletableFuture<String> prompt(String message);

        /**
         * Prompts for password input.
         *
         * @param message the prompt message
         * @return the password
         */
        CompletableFuture<String> promptPassword(String message);

        /**
         * Prompts for confirmation.
         *
         * @param message the prompt message
         * @return true if confirmed
         */
        CompletableFuture<Boolean> confirm(String message);

        /**
         * Prompts for selection.
         *
         * @param message the prompt message
         * @param options the options
         * @return the selected option
         */
        CompletableFuture<String> select(String message, java.util.List<String> options);

        /**
         * Opens a URL in the browser.
         *
         * @param url the URL to open
         * @return completion future
         */
        CompletableFuture<Void> openUrl(String url);
    }
}
