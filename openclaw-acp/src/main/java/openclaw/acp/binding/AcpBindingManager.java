package openclaw.acp.binding;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ACP Binding Manager interface.
 *
 * <p>Manages persistent bindings between ACP agents and channels.
 * Supports recovery and reset flows.</p>
 *
 * <p>Equivalent to Node.js src/acp/control-plane/manager.core.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
public interface AcpBindingManager {

    /**
     * Initialize the binding manager.
     *
     * @return completion future
     */
    CompletableFuture<Void> initialize();

    /**
     * Create a new binding.
     *
     * @param request the binding request
     * @return the binding result
     */
    CompletableFuture<BindingResult> createBinding(BindingRequest request);

    /**
     * Get an existing binding.
     *
     * @param bindingId the binding ID
     * @return the binding if found
     */
    CompletableFuture<Optional<AcpBinding>> getBinding(String bindingId);

    /**
     * Reset a binding (recovery flow).
     *
     * @param bindingId the binding ID to reset
     * @param reason the reset reason
     * @return the reset result
     */
    CompletableFuture<ResetResult> resetBinding(String bindingId, String reason);

    /**
     * Recover a binding after failure.
     *
     * @param bindingId the binding ID
     * @return the recovery result
     */
    CompletableFuture<RecoveryResult> recoverBinding(String bindingId);

    /**
     * Delete a binding.
     *
     * @param bindingId the binding ID
     * @return true if deleted
     */
    CompletableFuture<Boolean> deleteBinding(String bindingId);

    /**
     * List all active bindings.
     *
     * @return list of bindings
     */
    CompletableFuture<java.util.List<AcpBinding>> listBindings();

    /**
     * Check if a binding exists.
     *
     * @param bindingId the binding ID
     * @return true if exists
     */
    CompletableFuture<Boolean> bindingExists(String bindingId);

    /**
     * Update binding state.
     *
     * @param bindingId the binding ID
     * @param state the new state
     * @return completion future
     */
    CompletableFuture<Void> updateBindingState(String bindingId, BindingState state);

    /**
     * Binding request.
     */
    record BindingRequest(
            String agentId,
            String channelId,
            String channelType,
            java.util.Map<String, Object> metadata
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String agentId;
            private String channelId;
            private String channelType;
            private java.util.Map<String, Object> metadata = java.util.Map.of();

            public Builder agentId(String agentId) {
                this.agentId = agentId;
                return this;
            }

            public Builder channelId(String channelId) {
                this.channelId = channelId;
                return this;
            }

            public Builder channelType(String channelType) {
                this.channelType = channelType;
                return this;
            }

            public Builder metadata(java.util.Map<String, Object> metadata) {
                this.metadata = metadata != null ? metadata : java.util.Map.of();
                return this;
            }

            public BindingRequest build() {
                return new BindingRequest(agentId, channelId, channelType, metadata);
            }
        }
    }

    /**
     * Binding result.
     */
    record BindingResult(
            boolean success,
            String bindingId,
            Optional<String> error
    ) {
        public static BindingResult success(String bindingId) {
            return new BindingResult(true, bindingId, Optional.empty());
        }

        public static BindingResult failure(String error) {
            return new BindingResult(false, null, Optional.of(error));
        }
    }

    /**
     * Reset result.
     */
    record ResetResult(
            boolean success,
            String bindingId,
            ResetState state,
            Optional<String> error
    ) {
        public static ResetResult success(String bindingId, ResetState state) {
            return new ResetResult(true, bindingId, state, Optional.empty());
        }

        public static ResetResult failure(String bindingId, String error) {
            return new ResetResult(false, bindingId, ResetState.FAILED, Optional.of(error));
        }
    }

    /**
     * Recovery result.
     */
    record RecoveryResult(
            boolean success,
            String bindingId,
            RecoveryState state,
            Optional<String> error
    ) {
        public static RecoveryResult success(String bindingId, RecoveryState state) {
            return new RecoveryResult(true, bindingId, state, Optional.empty());
        }

        public static RecoveryResult failure(String bindingId, String error) {
            return new RecoveryResult(false, bindingId, RecoveryState.FAILED, Optional.of(error));
        }
    }

    /**
     * Binding state.
     */
    enum BindingState {
        ACTIVE,
        INACTIVE,
        RESETTING,
        RECOVERING,
        FAILED
    }

    /**
     * Reset state.
     */
    enum ResetState {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    /**
     * Recovery state.
     */
    enum RecoveryState {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
