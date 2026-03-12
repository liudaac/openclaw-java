# OpenClaw Plugin SDK (Java)

Java implementation of the OpenClaw Plugin SDK, compatible with Node.js version 2026.3.9.

## Overview

This SDK provides the plugin infrastructure for OpenClaw Java Edition, enabling:
- Channel plugins (Telegram, Slack, Feishu, etc.)
- Provider plugins (AI model providers)
- Custom tools and commands

## Architecture

```
openclaw-plugin-sdk/
├── core/           # Core runtime interfaces
├── channel/        # Channel plugin interfaces
├── provider/       # Provider plugin interfaces
├── tool/           # Tool system interfaces
├── command/        # Command handling interfaces
├── config/         # Configuration interfaces
├── discovery/      # Plugin discovery service
└── feishu/         # Feishu-specific utilities
```

## Core Components

### 1. PluginRuntime
Main runtime interface providing access to:
- Subagent execution
- Channel operations
- Core services

```java
public interface PluginRuntime {
    SubagentRuntime subagent();
    ChannelRuntime channel();
    CoreRuntime core();
    void initialize(PluginContext context);
    void shutdown();
    CompletableFuture<HealthStatus> health();
}
```

### 2. ChannelPlugin
Channel plugin interface with 20+ optional adapters:
- `ChannelConfigAdapter` (required)
- `ChannelOutboundAdapter`
- `ChannelStatusAdapter`
- `ChannelSecurityAdapter`
- `ChannelGroupAdapter`
- `ChannelThreadingAdapter`
- `ChannelStreamingAdapter`
- etc.

### 3. ProviderPlugin
AI model provider interface:
- Authentication methods
- Model configuration
- API key formatting
- OAuth refresh

### 4. AgentTool
Tool system for AI agents:
- Tool definitions with JSON Schema
- Execution context
- Result handling

## Usage

### Creating a Channel Plugin

```java
public class MyChannelPlugin implements ChannelPlugin<MyAccount, Void, Void> {
    
    @Override
    public ChannelId getId() {
        return new ChannelId("mychannel");
    }
    
    @Override
    public ChannelMeta getMeta() {
        return ChannelMeta.builder()
            .name("My Channel")
            .description("Description")
            .build();
    }
    
    @Override
    public ChannelCapabilities getCapabilities() {
        return ChannelCapabilities.builder()
            .supportsText(true)
            .supportsImages(true)
            .build();
    }
    
    @Override
    public ChannelConfigAdapter<MyAccount> getConfigAdapter() {
        return new MyConfigAdapter();
    }
    
    @Override
    public Optional<ChannelOutboundAdapter> getOutboundAdapter() {
        return Optional.of(new MyOutboundAdapter());
    }
}
```

### Registering a Plugin

Create file `META-INF/services/openclaw.sdk.channel.ChannelPlugin`:
```
com.example.MyChannelPlugin
```

## Statistics

- **Total Java files**: 60
- **Core interfaces**: 15
- **Channel adapters**: 33
- **Provider interfaces**: 4
- **Tool interfaces**: 4
- **Test files**: 3

## Compatibility

- Java 17+
- Maven/Gradle build
- Jackson for JSON
- Jakarta Validation

## Next Steps

1. Implement concrete channel plugins (Telegram, Feishu, etc.)
2. Add Memory system (Phase 1.2)
3. Add Secrets management (Phase 1.3)
4. Add Security layer (Phase 2)
5. Add Gateway service (Phase 2)
