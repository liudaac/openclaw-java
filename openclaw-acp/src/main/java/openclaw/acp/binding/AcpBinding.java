package openclaw.acp.binding;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ACP Binding entity.
 *
 * <p>Represents a persistent binding between an ACP agent and a channel.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
public class AcpBinding {

    private final String bindingId;
    private String agentId;
    private String channelId;
    private String channelType;
    private AcpBindingManager.BindingState state;
    private Map<String, Object> metadata;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant lastActivityAt;
    private Optional<String> resetReason;
    private int resetCount;
    private int recoveryCount;

    public AcpBinding() {
        this.bindingId = UUID.randomUUID().toString();
        this.state = AcpBindingManager.BindingState.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.lastActivityAt = Instant.now();
        this.resetReason = Optional.empty();
        this.resetCount = 0;
        this.recoveryCount = 0;
    }

    public AcpBinding(String agentId, String channelId, String channelType) {
        this();
        this.agentId = agentId;
        this.channelId = channelId;
        this.channelType = channelType;
    }

    // Getters and Setters

    public String getBindingId() {
        return bindingId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
        this.updatedAt = Instant.now();
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
        this.updatedAt = Instant.now();
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
        this.updatedAt = Instant.now();
    }

    public AcpBindingManager.BindingState getState() {
        return state;
    }

    public void setState(AcpBindingManager.BindingState state) {
        this.state = state;
        this.updatedAt = Instant.now();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void updateActivity() {
        this.lastActivityAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Optional<String> getResetReason() {
        return resetReason;
    }

    public void setResetReason(String resetReason) {
        this.resetReason = Optional.ofNullable(resetReason);
        this.updatedAt = Instant.now();
    }

    public int getResetCount() {
        return resetCount;
    }

    public void incrementResetCount() {
        this.resetCount++;
        this.updatedAt = Instant.now();
    }

    public int getRecoveryCount() {
        return recoveryCount;
    }

    public void incrementRecoveryCount() {
        this.recoveryCount++;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if binding can be reset.
     *
     * @return true if can be reset
     */
    public boolean canReset() {
        return state == AcpBindingManager.BindingState.ACTIVE ||
               state == AcpBindingManager.BindingState.INACTIVE ||
               state == AcpBindingManager.BindingState.FAILED;
    }

    /**
     * Check if binding can be recovered.
     *
     * @return true if can be recovered
     */
    public boolean canRecover() {
        return state == AcpBindingManager.BindingState.FAILED ||
               state == AcpBindingManager.BindingState.INACTIVE;
    }

    @Override
    public String toString() {
        return String.format("AcpBinding{bindingId='%s', agentId='%s', channelId='%s', state=%s}",
                bindingId, agentId, channelId, state);
    }
}
