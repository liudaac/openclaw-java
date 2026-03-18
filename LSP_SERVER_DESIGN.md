# LSP Server 支持 - 设计方案

**版本**: 2026.3.18  
**目标**: 为 OpenClaw Java 添加 LSP Server 支持

---

## 概述

LSP (Language Server Protocol) 支持允许 OpenClaw 连接语言服务器，为 Agent 提供代码分析能力：
- Hover 信息
- 代码补全
- 定义跳转
- 引用查找
- 诊断信息

---

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    LSP Architecture                          │
├─────────────────────────────────────────────────────────────┤
│  1. LSP Runtime (openclaw-lsp)                              │
│     ├── LspClient - JSON-RPC 客户端                        │
│     ├── LspSession - 会话管理                               │
│     ├── LspProcessManager - 进程管理                        │
│     └── LspToolFactory - 工具工厂                          │
│                                                              │
│  2. LSP Tools (openclaw-tools)                              │
│     ├── LspHoverTool                                        │
│     ├── LspCompletionTool                                   │
│     ├── LspDefinitionTool                                   │
│     └── LspDiagnosticsTool                                  │
│                                                              │
│  3. Configuration                                           │
│     - lsp.servers.{name}.command                            │
│     - lsp.servers.{name}.args                               │
│     - lsp.servers.{name}.rootUri                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 核心组件

### 1. LspClient

```java
public interface LspClient {
    /**
     * Send a request to the LSP server.
     */
    <T> CompletableFuture<T> sendRequest(String method, Object params, Class<T> responseType);
    
    /**
     * Send a notification to the LSP server.
     */
    void sendNotification(String method, Object params);
    
    /**
     * Initialize the client.
     */
    CompletableFuture<InitializeResult> initialize(InitializeParams params);
    
    /**
     * Shutdown the client.
     */
    CompletableFuture<Void> shutdown();
    
    /**
     * Get server capabilities.
     */
    ServerCapabilities getCapabilities();
}
```

### 2. LspSession

```java
public class LspSession {
    private final String serverName;
    private final LspClient client;
    private final Process process;
    private final ServerCapabilities capabilities;
    private final Map<Integer, PendingRequest> pendingRequests;
    private final AtomicInteger requestId;
    private volatile boolean initialized;
    
    // Methods for managing session lifecycle
}
```

### 3. LspProcessManager

```java
public class LspProcessManager {
    /**
     * Start an LSP server process.
     */
    public Process startProcess(String command, String[] args);
    
    /**
     * Stop an LSP server process.
     */
    public void stopProcess(Process process);
    
    /**
     * Check if process is running.
     */
    public boolean isRunning(Process process);
}
```

### 4. LspToolFactory

```java
public class LspToolFactory {
    /**
     * Create tools based on server capabilities.
     */
    public List<AgentTool> createTools(LspSession session) {
        List<AgentTool> tools = new ArrayList<>();
        
        if (session.getCapabilities().getHoverProvider()) {
            tools.add(new LspHoverTool(session));
        }
        
        if (session.getCapabilities().getCompletionProvider() != null) {
            tools.add(new LspCompletionTool(session));
        }
        
        if (session.getCapabilities().getDefinitionProvider()) {
            tools.add(new LspDefinitionTool(session));
        }
        
        if (session.getCapabilities().getReferencesProvider()) {
            tools.add(new LspReferencesTool(session));
        }
        
        if (session.getCapabilities().getDiagnosticProvider() != null) {
            tools.add(new LspDiagnosticsTool(session));
        }
        
        return tools;
    }
}
```

---

## JSON-RPC 协议

### 消息格式

```java
public class JsonRpcMessage {
    private String jsonrpc = "2.0";
    private Integer id;
    private String method;
    private Object params;
    private Object result;
    private JsonRpcError error;
    
    // Getters and setters
}

public class JsonRpcError {
    private int code;
    private String message;
    private Object data;
}
```

### 编码/解码

```java
public class JsonRpcProtocol {
    /**
     * Encode a message with Content-Length header.
     */
    public static String encode(JsonRpcMessage message) {
        String json = objectMapper.writeValueAsString(message);
        return "Content-Length: " + json.getBytes(StandardCharsets.UTF_8).length 
               + "\r\n\r\n" + json;
    }
    
    /**
     * Decode messages from buffer.
     */
    public static List<JsonRpcMessage> decode(String buffer) {
        // Parse Content-Length header
        // Extract JSON body
        // Parse JSON
    }
}
```

---

## LSP 工具实现

### LspHoverTool

```java
public class LspHoverTool implements AgentTool {
    private final LspSession session;
    
    @Override
    public String getName() {
        return "lsp_hover_" + session.getServerName();
    }
    
    @Override
    public String getDescription() {
        return "Get hover information for a symbol via " + session.getServerName();
    }
    
    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
            .property("uri", PropertySchema.string("File URI"))
            .property("line", PropertySchema.integer("Line number (0-based)"))
            .property("character", PropertySchema.integer("Character offset (0-based)"))
            .required("uri", "line", "character")
            .build();
    }
    
    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        Map<String, Object> args = context.arguments();
        
        TextDocumentPositionParams params = new TextDocumentPositionParams(
            new TextDocumentIdentifier((String) args.get("uri")),
            new Position((Integer) args.get("line"), (Integer) args.get("character"))
        );
        
        return session.getClient()
            .sendRequest("textDocument/hover", params, Hover.class)
            .thenApply(hover -> ToolResult.success(formatHover(hover)));
    }
}
```

---

## 配置

### application.yml

```yaml
openclaw:
  lsp:
    servers:
      typescript:
        command: "typescript-language-server"
        args: ["--stdio"]
        rootUri: "${workspaceFolder}"
      python:
        command: "pylsp"
        args: []
        rootUri: "${workspaceFolder}"
      java:
        command: "jdtls"
        args: []
        rootUri: "${workspaceFolder}"
```

### 环境变量

```bash
export OPENCLAW_LSP_TYPESCRIPT_COMMAND=typescript-language-server
export OPENCLAW_LSP_PYTHON_COMMAND=pylsp
export OPENCLAW_LSP_JAVA_COMMAND=jdtls
```

---

## 生命周期管理

### 启动流程

```
1. Load LSP configuration
2. For each configured server:
   a. Start process
   b. Connect stdio
   c. Send initialize request
   d. Receive capabilities
   e. Send initialized notification
   f. Create tools based on capabilities
3. Register tools with Agent
```

### 关闭流程

```
1. Send shutdown request
2. Send exit notification
3. Kill process
4. Clean up resources
```

---

## 错误处理

### 常见错误

| 错误 | 原因 | 处理 |
|------|------|------|
| Process not found | 命令不存在 | 检查安装 |
| Connection refused | 端口冲突 | 使用 stdio |
| Timeout | 服务器无响应 | 重试或报错 |
| Parse error | 消息格式错误 | 跳过并记录 |

### 重试策略

```java
public class LspRetryPolicy {
    private final int maxRetries = 3;
    private final Duration initialDelay = Duration.ofMillis(100);
    
    public <T>