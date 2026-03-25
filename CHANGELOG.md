# Changelog

All notable changes to OpenClaw Java Edition.

## [2026.3.9-SNAPSHOT] - 2026-03-10

### Added

#### Phase 1 - Infrastructure
- **Plugin SDK** (60 files)
  - Core runtime interfaces (PluginRuntime, SubagentRuntime, ChannelRuntime)
  - 33 channel adapters (Config, Outbound, Security, Group, etc.)
  - Provider plugin interfaces
  - Tool system (AgentTool, ToolFactory)
  - Plugin discovery via Java SPI
  - CompletableFuture-based async API
  - Builder pattern for all data classes

- **Memory System** (11 files)
  - Embedding providers: OpenAI, Mistral, Ollama
  - Batch embedding with concurrency control (Semaphore)
  - SQLite-based vector search
  - Memory manager with CRUD operations
  - Hybrid search support

- **Secrets Manager** (7 files)
  - AES-256-GCM encryption
  - Audit logging with file persistence
  - Credential matrix
  - Key rotation support
  - SecretManager interface

#### Phase 2 - Security & Gateway
- **Security** (5 files)
  - SSRF protection (private IPs, ports, schemes)
  - Input validation (SQL injection, XSS, path traversal)
  - Config validation (dangerous flags detection)
  - FetchGuard for HTTP requests

- **Gateway** (5 files)
  - Node registry with heartbeat
  - Priority work queue (age-weighted)
  - Work dispatcher with 5 strategies
  - Gateway service interface

#### Phase 3 - Channels
- **Telegram** (7 files)
  - Full channel implementation
  - Bot API integration
  - Message sending (text, media)
  - Command handling (/status, /help)
  - Mention parsing
  - Webhook security

- **Feishu** (7 files)
  - Full channel implementation
  - App authentication
  - Message cards support
  - User directory
  - Mention handling

#### Build & Documentation
- Parent POM with dependency management
- Module structure with 7 modules
- README with architecture diagram
- CONTRIBUTING guide
- CHANGELOG

### Statistics
- Total Java files: 102
- Modules: 7
- Core interfaces: 60+
- Channel adapters: 33
- Test files: 10+

### Dependencies
- Jackson 2.16 (JSON)
- OkHttp 4.12 (HTTP)
- SLF4J 2.0.9 (Logging)
- JUnit 5.10 (Testing)
- AssertJ 3.24 (Assertions)

## [2026.3.25] - 2026-03-25

### Synced from Original OpenClaw

Based on analysis report: `openclaw-nightly-sync-2026-03-25.md`

#### Feishu Channel - Mention Policy Enhancement

**New Files:**
- `FeishuGroupPolicy.java` - Group policy enumeration (OPEN, ALLOWLIST, DISABLED, ALLOWALL)
- `FeishuGroupConfig.java` - Per-group configuration with requireMention, tools, skills
- `FeishuPolicyResolver.java` - Policy resolution logic matching TypeScript implementation

**Modified Files:**
- `FeishuConfigAdapter.java` - Added groupPolicy, groups, requireMention configuration support
- `FeishuInboundAdapter.java` - Integrated policy-based mention handling

**New Test Files:**
- `FeishuPolicyResolverTest.java` - Comprehensive tests for policy resolution
- `FeishuMentionAdapterTest.java` - Tests for mention parsing and formatting

**Key Features:**
- When `groupPolicy` is `"open"`, `requireMention` defaults to `false`
  - Allows non-text messages (images, etc.) to be processed without @-mentions
  - Improves user experience in open group chats
- Support for per-group configuration via `groups` map
- Allowlist matching with wildcard support (`*`)
- Case-insensitive group ID matching

**Implementation Details:**
- Ported from: `extensions/feishu/src/policy.ts`
- Maintains backward compatibility (defaults to ALLOWLIST policy)
- All new configuration fields are optional

### Skipped (Java Edition Does Not Have These Modules)

- **WhatsApp Reply Detection Fixes** - No WhatsApp channel module
- **WhatsApp Identity Handling Refactor** - No WhatsApp channel module
- **OpenAI Codex Auth Refactor** - No dedicated Codex integration

## [Unreleased]

### Planned

#### Phase 4 - AI Agent
- ACP protocol implementation
- Context engine
- Subagent runtime
- Memory management

#### Phase 5 - Tools
- Web search integration
- Skills system
- Tool execution
- Provider management

#### Phase 6 - More Channels
- Slack
- Discord
- WhatsApp
- Signal

## Versioning

We use [Semantic Versioning](https://semver.org/):
- MAJOR: Incompatible API changes
- MINOR: Backward-compatible features
- PATCH: Backward-compatible fixes

Format: `YYYY.M.D[-SNAPSHOT]`

## Contributors

- OpenClaw Team

## License

MIT License - See LICENSE file
