package openclaw.lsp.config;

import java.util.Map;

/**
 * LSP Server configuration.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspServerConfig {

    private String name;
    private String command;
    private String[] args = new String[0];
    private String rootUri;
    private Map<String, String> env;
    private long requestTimeout = 10000;

    public LspServerConfig() {
    }

    public LspServerConfig(String name, String command) {
        this.name = name;
        this.command = command;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args != null ? args : new String[0];
    }

    public String getRootUri() {
        return rootUri;
    }

    public void setRootUri(String rootUri) {
        this.rootUri = rootUri;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Builder for LspServerConfig.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LspServerConfig config = new LspServerConfig();

        public Builder name(String name) {
            config.setName(name);
            return this;
        }

        public Builder command(String command) {
            config.setCommand(command);
            return this;
        }

        public Builder args(String[] args) {
            config.setArgs(args);
            return this;
        }

        public Builder rootUri(String rootUri) {
            config.setRootUri(rootUri);
            return this;
        }

        public Builder env(Map<String, String> env) {
            config.setEnv(env);
            return this;
        }

        public Builder requestTimeout(long timeout) {
            config.setRequestTimeout(timeout);
            return this;
        }

        public LspServerConfig build() {
            return config;
        }
    }
}
