# OpenClaw Java版实施计划

**日期**: 2026-03-29  
**基于**: nightly-sync分析报告  
**优先级**: P0-P1高优先级同步项

---

## 执行摘要

本实施计划涵盖从原版OpenClaw同步到Java版的4个高优先级项目：
- **1个P0安全修复**: Exec安全沙箱失败关闭
- **3个P1功能增强**: Memory FTS5分词器、CJK字符处理、子代理内存工具策略

预计总工作量：**7-10天**

---

## 项目概览

| # | 项目 | 优先级 | 模块 | 预估工作量 | 风险等级 |
|---|------|--------|------|------------|----------|
| 1 | Exec安全沙箱失败关闭 | P0 | openclaw-tools | 2-3天 | 高 |
| 2 | Memory FTS5分词器支持CJK | P1 | openclaw-memory | 2-3天 | 中 |
| 3 | CJK字符Token计数修复 | P1 | openclaw-utils (新建) | 1-2天 | 低 |
| 4 | 子代理内存工具策略 | P1 | openclaw-agent | 0.5天 | 低 |

---

## 项目1: Exec安全沙箱失败关闭 (P0)

### 背景
当前Java版`CommandExecutionTool`直接执行系统命令，无沙箱隔离机制。当沙箱不可用时，应执行"失败关闭"策略，拒绝执行命令。

### 目标模块
- `openclaw-tools/src/main/java/openclaw/tools/exec/`

### 实施步骤

#### 步骤1: 创建沙箱检测器

**新建文件**: `SandboxDetector.java`

```java
package openclaw.tools.exec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Detects sandbox availability for command execution.
 */
public class SandboxDetector {
    
    private static final String[] SANDBOX_INDICATORS = {
        "/.dockerenv",
        "/run/.containerenv"
    };
    
    private final Boolean cachedResult;
    
    public SandboxDetector() {
        this.cachedResult = detectSandbox();
    }
    
    /**
     * Check if running in a sandboxed environment.
     */
    public boolean isSandboxAvailable() {
        return cachedResult;
    }
    
    private Boolean detectSandbox() {
        // Check for container indicators
        for (String indicator : SANDBOX_INDICATORS) {
            if (Files.exists(Paths.get(indicator))) {
                return true;
            }
        }
        
        // Check for restricted user (non-root in container)
        String user = System.getProperty("user.name");
        if ("openclaw".equals(user) || "sandbox".equals(user)) {
            return true;
        }
        
        // Check cgroup for container indicators
        try {
            Path cgroup = Paths.get("/proc/self/cgroup");
            if (Files.exists(cgroup)) {
                String content = Files.readString(cgroup);
                if (content.contains("docker") || content.contains("containerd")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return false;
    }
}
```

#### 步骤2: 修改CommandExecutionTool

**修改文件**: `CommandExecutionTool.java`

**新增字段**:
```java
private final boolean sandboxEnabled;
private final boolean failClosed;
private final SandboxDetector sandboxDetector;
```

**新增构造函数**:
```java
public CommandExecutionTool(
        Path workingDir,
        Duration timeout,
        Set<String> allowedCommands,
        Set<String> blockedCommands,
        boolean requireApproval,
        boolean sandboxEnabled,
        boolean failClosed) {
    this.workingDir = workingDir;
    this.timeout = timeout;
    this.allowedCommands = allowedCommands;
    this.blockedCommands = blockedCommands;
    this.requireApproval = requireApproval;
    this.sandboxEnabled = sandboxEnabled;
    this.failClosed = failClosed;
    this.sandboxDetector = new SandboxDetector();
}
```

**新增沙箱检查方法**:
```java
private ToolResult checkSandboxAvailability() {
    if (!sandboxEnabled) {
        if (failClosed) {
            return ToolResult.failure(
                "SECURITY: Sandbox is disabled and fail-closed mode is enabled. " +
                "Command execution denied."
            );
        }
        return null;
    }
    
    if (!sandboxDetector.isSandboxAvailable()) {
        if (failClosed) {
            return ToolResult.failure(
                "SECURITY: Sandbox is unavailable and fail-closed mode is enabled. " +
                "Command execution denied."
            );
        }
    }
    
    return null;
}
```

**在execute()方法开头调用**:
```java
@Override
public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
    return CompletableFuture.supplyAsync(() -> {
        // 1. 沙箱检查
        ToolResult sandboxCheck = checkSandboxAvailability();
        if (sandboxCheck != null) {
            return sandboxCheck;
        }
        
        // 2. 安全检查
        // ... existing code
    });
}
```

#### 步骤3: 更新配置

**修改文件**: `ToolConfiguration.java`

```java
@ConfigurationProperties(prefix = "openclaw.tools.exec")
public class ToolConfiguration {
    private boolean sandboxEnabled = true;
    private boolean failClosed = true;  // 默认启用失败关闭
    
    // getters and setters
}
```

### 测试计划

**新建测试**: `CommandExecutionToolSandboxTest.java`

```java
@Test
void shouldRejectExecutionWhenSandboxDisabledAndFailClosed() {
    CommandExecutionTool tool = new CommandExecutionTool(
        tempDir, Duration.ofMinutes(1), 
        Set.of(), Set.of(), false, 
        false, true  // sandboxEnabled=false, failClosed=true
    );
    
    ToolResult result = tool.execute(createContext("ls")).join();
    
    assertTrue(result.isFailure());
    assertTrue(result.getError().contains("fail-closed"));
}

@Test
void shouldRejectExecutionWhenSandboxUnavailableAndFailClosed() {
    // Mock SandboxDetector to return false
    CommandExecutionTool tool = createToolWithMockedSandbox(false, true);
    
    ToolResult result = tool.execute(createContext("ls")).join();
    
    assertTrue(result.isFailure());
    assertTrue(result.getError().contains("unavailable"));
}
```

### 工作量预估
- **开发**: 1-2天
- **测试**: 0.5-1天
- **文档**: 0.5天
- **总计**: 2-3天

---

## 项目2: Memory FTS5分词器支持CJK (P1)

### 背景
Java版Memory使用SQLite FTS5，但未配置分词器。需要支持CJK（中日韩）文本的全文搜索。

### 目标模块
- `openclaw-memory/src/main/java/openclaw/memory/store/`

### 实施步骤

#### 步骤1: 更新Memory配置

**修改文件**: `MemoryConfiguration.java`

```java
@ConfigurationProperties(prefix = "openclaw.memory")
public class MemoryConfiguration {
    
    /**
     * FTS5 tokenizer: porter (default), icu, unicode61
     * Use 'icu' for CJK support.
     */
    private String ftsTokenizer = "porter";
    
    /**
     * Enable FTS-only mode (no embeddings).
     */
    private boolean ftsOnly = false;
    
    // getters and setters
}
```

#### 步骤2: 修改SQLite存储初始化

**修改文件**: `SQLiteMemoryStore.java`

```java
private void initializeFtsTable() {
    String tokenizer = config.getFtsTokenizer();
    
    String createFtsSql = String.format(
        "CREATE VIRTUAL TABLE IF NOT EXISTS memories_fts USING fts5(" +
        "  content, " +
        "  tokenize='%s'" +
        ")",
        tokenizer
    );
    
    jdbcTemplate.execute(createFtsSql);
}
```

#### 步骤3: 添加ICU分词器支持

**新建文件**: `FtsTokenizer.java`

```java
package openclaw.memory.store;

public enum FtsTokenizer {
    PORTER("porter"),           // 英文词干提取
    ICU("icu"),                 // CJK支持
    UNICODE61("unicode61"),     // Unicode 6.1
    TRIGRAM("trigram");         // 三字符分词
    
    private final String value;
    
    FtsTokenizer(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
```

### 配置示例

```yaml
openclaw:
  memory:
    fts-tokenizer: icu  # 启用CJK支持
    fts-only: false     # 同时启用嵌入搜索
```

### 工作量预估
- **开发**: 1-2天
- **ICU依赖配置**: 0.5-1天
- **测试**: 0.5天
- **总计**: 2-3天

---

## 项目3: CJK字符Token计数修复 (P1)

### 背景
当前Java版无CJK字符处理工具，导致上下文长度计算不准确。

### 目标模块
- 新建: `openclaw-utils/src/main/java/openclaw/utils/`

### 实施步骤

#### 步骤1: 创建CJK工具类

**新建文件**: `CjkCharUtils.java`

```java
package openclaw.utils;

/**
 * Utilities for CJK (Chinese, Japanese, Korean) character handling.
 */
public final class CjkCharUtils {
    
    private CjkCharUtils() {
        // utility class
    }
    
    /**
     * Check if a character is CJK (Chinese, Japanese, Korean).
     */
    public static boolean isCjk(char c) {
        return isCjkUnified(c) || 
               isHiragana(c) || 
               isKatakana(c) || 
               isHangul(c);
    }
    
    /**
     * Check if character is CJK Unified Ideograph.
     */
    public static boolean isCjkUnified(char c) {
        return (c >= '\u4e00' && c <= '\u9fff') ||     // CJK Unified
               (c >= '\u3400' && c <= '\u4dbf') ||     // CJK Extension A
               (c >= '\u20000' && c <= '\u2a6df');     // CJK Extension B
    }
    
    /**
     * Check if character is Hiragana (Japanese).
     */
    public static boolean isHiragana(char c) {
        return c >= '\u3040' && c <= '\u309f';
    }
    
    /**
     * Check if character is Katakana (Japanese).
     */
    public static boolean isKatakana(char c) {
        return c >= '\u30a0' && c <= '\u30ff';
    }
    
    /**
     * Check if character is Hangul (Korean).
     */
    public static boolean isHangul(char c) {
        return (c >= '\uac00' && c <= '\ud7af') ||     // Hangul Syllables
               (c >= '\u1100' && c <= '\u11ff');       // Hangul Jamo
    }
    
    /**
     * Estimate token count for text.
     * CJK characters count as ~1 token each.
     * Non-CJK characters count as ~0.25 tokens each (roughly 4 chars per token).
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int cjkCount = 0;
        int nonCjkCount = 0;
        
        for (char c : text.toCharArray()) {
            if (isCjk(c)) {
                cjkCount++;
            } else if (!Character.isWhitespace(c)) {
                nonCjkCount++;
            }
        }
        
        // CJK: 1 token per char
        // Non-CJK: 1 token per 4 chars (rough estimate)
        return cjkCount + (nonCjkCount / 4) + (nonCjkCount % 4 > 0 ? 1 : 0);
    }
    
    /**
     * Get weighted character length for text chunking.
     * CJK characters have weight 3, others weight 1.
     */
    public static int getWeightedLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int length = 0;
        for (char c : text.toCharArray()) {
            length += isCjk(c) ? 3 : 1;
        }
        return length;
    }
}
```

#### 步骤2: 在上下文修剪中使用

**修改文件**: `ContextPruner.java` (或其他上下文管理类)

```java
import openclaw.utils.CjkCharUtils;

public class ContextPruner {
    
    private int estimateTokenCount(String text) {
        // 使用CJK-aware的Token估算
        return CjkCharUtils.estimateTokens(text);
    }
    
    private int getWeightedLength(String text) {
        // 用于分块边界决策
        return CjkCharUtils.getWeightedLength(text);
    }
}
```

### 测试计划

**新建测试**: `CjkCharUtilsTest.java`

```java
@Test
void shouldDetectCjkCharacters() {
    assertTrue(CjkCharUtils.isCjk('中'));
    assertTrue(CjkCharUtils.isCjk('あ')); // Hiragana
    assertTrue(CjkCharUtils.isCjk('ア')); // Katakana
    assertTrue(CjkCharUtils.isCjk('한')); // Hangul
    assertFalse(CjkCharUtils.isCjk('A'));
    assertFalse(CjkCharUtils.isCjk('1'));
}

@Test
void shouldEstimateTokensForMixedText() {
    // "Hello世界" = 5 chars, 2 CJK + 3 non-CJK
    // Expected: 2 + ceil(3/4) = 3
    assertEquals(3, CjkCharUtils.estimateTokens("Hello世界"));
    
    // "这是一个测试" = 6 CJK chars
    assertEquals(6, CjkCharUtils.estimateTokens("这是一个测试"));
}
```

### 工作量预估
- **开发**: 0.5-1天
- **测试**: 0.5天
- **集成**: 0.5天
- **总计**: 1-2天

---

## 项目4: 子代理内存工具策略 (P1)

### 背景
原版允许子代理使用 `memory_search` 和 `memory_get` 工具，这些只读工具对多代理共享内存至关重要。

### 目标模块
- `openclaw-agent/src/main/java/openclaw/agent/tools/`

### 实施步骤

#### 步骤1: 更新工具策略配置

**修改文件**: `ToolPolicyConfiguration.java`

```java
@ConfigurationProperties(prefix = "openclaw.agent.tools")
public class ToolPolicyConfiguration {
    
    /**
     * Tools allowed for sub-agent sessions.
     */
    private Set<String> subagentAllowedTools = Set.of(
        "memory_search",
        "memory_get",
        "read",
        "web_search",
        "web_fetch"
    );
    
    /**
     * Tools denied for sub-agent sessions.
     * Note: memory_search and memory_get removed from deny list.
     */
    private Set<String> subagentDeniedTools = Set.of(
        "write",
        "edit",
        "memory_create",
        "memory_update",
        "memory_delete"
    );
    
    // getters and setters
}
```

#### 步骤2: 修改工具策略检查

**修改文件**: `ToolPolicyEnforcer.java`

```java
public class ToolPolicyEnforcer {
    
    public boolean isToolAllowed(String toolName, SessionContext context) {
        // 检查是否是子代理会话
        if (context.isSubagentSession()) {
            // 子代理只能使用允许列表中的工具
            return config.getSubagentAllowedTools().contains(toolName);
        }
        
        // 主代理会话的正常检查
        return !config.getDeniedTools().contains(toolName);
    }
}
```

#### 步骤3: 更新会话上下文

**修改文件**: `SessionContext.java`

```java
public class SessionContext {
    
    /**
     * Check if this is a sub-agent session.
     */
    public boolean isSubagentSession() {
        return sessionType == SessionType.SUBAGENT ||
               parentSessionId != null;
    }
}
```

### 配置示例

```yaml
openclaw:
  agent:
    tools:
      subagent-allowed-tools:
        - memory_search
        - memory_get
        - read
        - web_search
        - web_fetch
      subagent-denied-tools:
        - write
        - edit
        - memory_create
        - memory_update
        - memory_delete
```

### 测试计划

**新建测试**: `ToolPolicySubagentTest.java`

```java
@Test
void shouldAllowMemorySearchForSubagent() {
    SessionContext subagentContext = createSubagentContext();
    
    assertTrue(policyEnforcer.isToolAllowed("memory_search", subagentContext));
    assertTrue(policyEnforcer.isToolAllowed("memory_get", subagentContext));
}

@Test
void shouldDenyWriteForSubagent() {
    SessionContext subagentContext = createSubagentContext();
    
    assertFalse(policyEnforcer.isToolAllowed("write", subagentContext));
    assertFalse(policyEnforcer.isToolAllowed("memory_create", subagentContext));
}

@Test
void shouldAllowAllToolsForMainAgent() {
    SessionContext mainContext = createMainAgentContext();
    
    assertTrue(policyEnforcer.isToolAllowed("memory_search", mainContext));
    assertTrue(policyEnforcer.isToolAllowed("write", mainContext));
}
```

### 工作量预估
- **开发**: 0.5天
- **测试**: 0.5天
- **总计**: 0.5-1天

---

## 实施时间表

### 第一周

| 日期 | 任务 | 负责人 |
|------|------|--------|
| Day 1-2 | 项目1: Exec安全沙箱失败关闭 | 开发 |
| Day 3 | 项目1: 测试与审查 | 测试 |
| Day 4-5 | 项目2: Memory FTS5分词器 | 开发 |
| Day 6 | 项目2: 测试与ICU配置 | 测试 |

### 第二周

| 日期 | 任务 | 负责人 |
|------|------|--------|
| Day 1 | 项目3: CJK字符工具 | 开发 |
| Day 2 | 项目3: 测试与集成 | 测试 |
| Day 3 | 项目4: 子代理工具策略 | 开发 |
| Day 4 | 项目4: 测试 | 测试 |
| Day 5 | 集成测试与文档更新 | 团队 |

---

## 风险评估

### 高风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Exec沙箱检测误报 | 命令无法执行 | 提供配置选项关闭fail-closed模式 |
| ICU分词器依赖问题 | FTS5无法使用 | 提供porter分词器作为fallback |

### 中风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| CJK Token估算不准确 | 上下文截断位置不当 | 提供配置调整参数 |

### 低风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 子代理工具策略配置错误 | 权限问题 | 提供清晰的配置文档 |

---

## 验收标准

### 项目1: Exec安全沙箱
- [ ] 沙箱检测器能正确识别容器环境
- [ ] fail-closed模式拒绝非沙箱执行
- [ ] 配置可禁用fail-closed模式
- [ ] 所有测试通过

### 项目2: Memory FTS5
- [ ] 支持ICU分词器配置
- [ ] CJK文本可被正确索引和搜索
- [ ] 提供porter分词器作为fallback
- [ ] 所有测试通过

### 项目3: CJK字符工具
- [ ] 正确检测CJK字符
- [ ] Token估算符合预期
- [ ] 上下文修剪使用加权长度
- [ ] 所有测试通过

### 项目4: 子代理工具策略
- [ ] 子代理可使用memory_search和memory_get
- [ ] 子代理无法使用write/edit等写入工具
- [ ] 主代理不受限制
- [ ] 所有测试通过

---

## 附录: 参考文档

- [Nightly Sync分析报告](./openclaw-nightly-sync-2026-03-29.md)
- [原版OpenClaw变更日志](../openclaw/CHANGELOG.md)
- [Java版现有实现](./openclaw-java/)

---

*计划制定时间: 2026-03-29*  
*版本: 1.0*  
*状态: 待实施*