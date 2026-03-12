package openclaw.sdk.channel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Channel onboarding adapter for setup wizard integration.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelOnboardingAdapter {

    /**
     * Gets the onboarding steps.
     *
     * @return the list of steps
     */
    java.util.List<OnboardingStep> getSteps();

    /**
     * Executes an onboarding step.
     *
     * @param stepId the step ID
     * @param input the user input
     * @return the step result
     */
    CompletableFuture<StepResult> executeStep(String stepId, Map<String, Object> input);

    /**
     * Validates the final configuration.
     *
     * @param config the configuration
     * @return validation result
     */
    CompletableFuture<ValidationResult> validateConfig(Map<String, Object> config);

    /**
     * Onboarding step definition.
     *
     * @param id the step ID
     * @param title the step title
     * @param description the step description
     * @param type the step type
     * @param required whether the step is required
     */
    record OnboardingStep(
            String id,
            String title,
            String description,
            StepType type,
            boolean required
    ) {
    }

    /**
     * Step type enumeration.
     */
    enum StepType {
        INPUT,
        SELECT,
        CONFIRM,
        OAUTH,
        TOKEN,
        VERIFY
    }

    /**
     * Step execution result.
     *
     * @param success whether the step succeeded
     * @param message result message
     * @param nextStepId the next step ID if applicable
     */
    record StepResult(
            boolean success,
            String message,
            java.util.Optional<String> nextStepId
    ) {
    }

    /**
     * Validation result.
     *
     * @param valid whether the config is valid
     * @param message validation message
     */
    record ValidationResult(
            boolean valid,
            String message
    ) {
    }
}
