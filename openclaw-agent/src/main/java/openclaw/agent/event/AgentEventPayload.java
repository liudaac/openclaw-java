package openclaw.agent.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Payload for agent events.
 *
 * <p>Contains all the information about an agent event including
 * the run ID, sequence number, stream type, timestamp, and data.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class AgentEventPayload {

    private final String runId;
    private final int seq;
    private final AgentEventStream stream;
    private final Instant timestamp;
    private final Map<String, Object> data;
    private final String sessionKey;

    private AgentEventPayload(Builder builder) {
        this.runId = Objects.requireNonNull(builder.runId, "runId cannot be null");
        this.seq = builder.seq;
        this.stream = Objects.requireNonNull(builder.stream, "stream cannot be null");
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp cannot be null");
        this.data = builder.data != null ? Map.copyOf(builder.data) : Map.of();
        this.sessionKey = builder.sessionKey;
    }

    /**
     * Gets the run ID this event belongs to.
     *
     * @return the run ID
     */
    public String getRunId() {
        return runId;
    }

    /**
     * Gets the sequence number of this event within the run.
     *
     * @return the sequence number
     */
    public int getSeq() {
        return seq;
    }

    /**
     * Gets the event stream type.
     *
     * @return the stream type
     */
    public AgentEventStream getStream() {
        return stream;
    }

    /**
     * Gets the event timestamp.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the event data.
     *
     * @return the data map
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Gets the session key (may be null).
     *
     * @return the session key
     */
    public String getSessionKey() {
        return sessionKey;
    }

    /**
     * Creates a new builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentEventPayload that = (AgentEventPayload) o;
        return seq == that.seq &&
                Objects.equals(runId, that.runId) &&
                stream == that.stream;
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId, seq, stream);
    }

    @Override
    public String toString() {
        return "AgentEventPayload{" +
                "runId='" + runId + '\'' +
                ", seq=" + seq +
                ", stream=" + stream +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }

    /**
     * Builder for AgentEventPayload.
     */
    public static class Builder {
        private String runId;
        private int seq;
        private AgentEventStream stream;
        private Instant timestamp = Instant.now();
        private Map<String, Object> data;
        private String sessionKey;

        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder seq(int seq) {
            this.seq = seq;
            return this;
        }

        public Builder stream(AgentEventStream stream) {
            this.stream = stream;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public Builder sessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
            return this;
        }

        public AgentEventPayload build() {
            return new AgentEventPayload(this);
        }
    }
}
