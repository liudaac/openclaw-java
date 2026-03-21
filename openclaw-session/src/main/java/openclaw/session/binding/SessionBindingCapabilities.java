package openclaw.session.binding;

import java.util.List;
import java.util.Objects;

/**
 * Capabilities of a session binding adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class SessionBindingCapabilities {

    private final boolean adapterAvailable;
    private final boolean bindSupported;
    private final boolean unbindSupported;
    private final List<SessionBindingPlacement> placements;

    private SessionBindingCapabilities(Builder builder) {
        this.adapterAvailable = builder.adapterAvailable;
        this.bindSupported = builder.bindSupported;
        this.unbindSupported = builder.unbindSupported;
        this.placements = builder.placements != null ? List.copyOf(builder.placements) : List.of();
    }

    public boolean isAdapterAvailable() {
        return adapterAvailable;
    }

    public boolean isBindSupported() {
        return bindSupported;
    }

    public boolean isUnbindSupported() {
        return unbindSupported;
    }

    public List<SessionBindingPlacement> getPlacements() {
        return placements;
    }

    /**
     * Creates unavailable capabilities.
     *
     * @return unavailable capabilities
     */
    public static SessionBindingCapabilities unavailable() {
        return builder()
                .adapterAvailable(false)
                .bindSupported(false)
                .unbindSupported(false)
                .placements(List.of())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionBindingCapabilities that = (SessionBindingCapabilities) o;
        return adapterAvailable == that.adapterAvailable &&
                bindSupported == that.bindSupported;
    }

    @Override
    public int hashCode() {
        return Objects.hash(adapterAvailable, bindSupported);
    }

    @Override
    public String toString() {
        return "SessionBindingCapabilities{" +
                "adapterAvailable=" + adapterAvailable +
                ", bindSupported=" + bindSupported +
                ", placements=" + placements.size() +
                '}';
    }

    public static class Builder {
        private boolean adapterAvailable;
        private boolean bindSupported;
        private boolean unbindSupported;
        private List<SessionBindingPlacement> placements;

        public Builder adapterAvailable(boolean adapterAvailable) {
            this.adapterAvailable = adapterAvailable;
            return this;
        }

        public Builder bindSupported(boolean bindSupported) {
            this.bindSupported = bindSupported;
            return this;
        }

        public Builder unbindSupported(boolean unbindSupported) {
            this.unbindSupported = unbindSupported;
            return this;
        }

        public Builder placements(List<SessionBindingPlacement> placements) {
            this.placements = placements;
            return this;
        }

        public SessionBindingCapabilities build() {
            return new SessionBindingCapabilities(this);
        }
    }
}
