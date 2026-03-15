package openclaw.agent.tool;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import openclaw.sdk.llm.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool Chain Executor
 * 
 * <p>Enhanced tool execution with chaining support:</p>
 * <ul>
 *   <li>Multi-step tool chains</li>
 *   <li>Automatic retry with exponential backoff</li>
 *   <li>Tool result integration to LLM context</li>
 *   <li>Timeout handling</li>
 *   <li>Parallel tool execution</li>
 * </ul>
 */
@Service
public class ToolChainExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolChainExecutor.class);
    
    private final Map<String, AgentTool> tools;
    private final LlmService llmService;
    
    // Configuration
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;
    private static final long MAX_RETRY_DELAY_MS = 10000;
    private static final long TOOL_TIMEOUT_SECONDS = 60;
    private static final int MAX_CHAIN_DEPTH = 5;
    
    // Pattern to detect tool calls in LLM response
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
        "TOOL_CALL:\\s*\\{\\s*\"tool\":\\s*\"([^\"]+)\"\\s*,\\s*\"args\":\\s*(\\{[^}]+\\})\\s*\\}"
    );
    
    public ToolChainExecutor(List<AgentTool> toolList, LlmService llmService) {
        this.tools = new HashMap<>();
        for (AgentTool tool : toolList) {
            this.tools.put(tool.getName(), tool);
        }
        this.llmService = llmService;
    }
    
    /**
     * Execute tool chain from LLM response
     * 
     * @param llmResponse the LLM response that may contain tool calls
     * @param context the execution context
     * @return the final response after tool execution
     */
    public CompletableFuture<ToolChainResult> executeToolChain(
            String llmResponse, 
            ToolExecuteContext context) {
        
        return executeToolChain(llmResponse, context, 0);
    }
    
    private CompletableFuture<ToolChainResult> executeToolChain(
            String llmResponse,
            ToolExecuteContext context,
            int depth) {
        
        if (depth >= MAX_CHAIN_DEPTH) {
            logger.warn("Max chain depth reached: {}", MAX_CHAIN_DEPTH);
            return CompletableFuture.completedFuture(
                new ToolChainResult(llmResponse, Collections.emptyList(), false)
            );
        }
        
        // Parse tool calls from response
        List<ToolCall> toolCalls = parseToolCalls(llmResponse);
        
        if (toolCalls.isEmpty()) {
            // No tool calls, return original response
            return CompletableFuture.completedFuture(
                new ToolChainResult(llmResponse, Collections.emptyList(), false)
            );
        }
        
        logger.info("Executing {} tool calls at depth {}", toolCalls.size(), depth);
        
        // Execute tools (parallel for independent tools)
        List<CompletableFuture<ToolExecutionResult>> futures = toolCalls.stream()
            .map(toolCall -> executeToolWithRetry(toolCall, context))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList()
            )
            .thenCompose(results -> {
                // Check if any tool failed
                boolean allSuccess = results.stream().allMatch(r -> r.success());
                
                if (!allSuccess) {
                    // Some tools failed, return error
                    String errorMsg = results.stream()
                        .filter(r -> !r.success())
                        .map(r -> "Tool " + r.toolName() + " failed: " + r.error())
                        .collect(java.util.stream.Collectors.joining("\n"));
                    
                    return CompletableFuture.completedFuture(
                        new ToolChainResult(errorMsg, results, false)
                    );
                }
                
                // Build context with tool results
                String toolResultsContext = buildToolResultsContext(results);
                
                // Call LLM again with tool results
                String newPrompt = llmResponse + "\n\n" + toolResultsContext + "\n\nAssistant:";
                
                return llmService.chat(newPrompt)
                    .thenCompose(newResponse -> {
                        // Check if new response has more tool calls
                        if (hasToolCalls(newResponse)) {
                            // Recursive call for tool chain
                            return executeToolChain(newResponse, context, depth + 1);
                        }
                        
                        return CompletableFuture.completedFuture(
                            new ToolChainResult(newResponse, results, true)
                        );
                    });
            });
    }
    
    /**
     * Execute a single tool with retry logic
     */
    private CompletableFuture<ToolExecutionResult> executeToolWithRetry(
            ToolCall toolCall, 
            ToolExecuteContext context) {
        
        return executeToolWithRetry(toolCall, context, 0, INITIAL_RETRY_DELAY_MS);
    }
    
    private CompletableFuture<ToolExecutionResult> executeToolWithRetry(
            ToolCall toolCall,
            ToolExecuteContext context,
            int attempt,
            long delayMs) {
        
        AgentTool tool = tools.get(toolCall.toolName());
        
        if (tool == null) {
            return CompletableFuture.completedFuture(
                new ToolExecutionResult(toolCall.toolName(), false, null, "Tool not found: " + toolCall.toolName())
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Executing tool: {} (attempt {})", toolCall.toolName(), attempt + 1);
                
                ToolExecuteContext toolContext = ToolExecuteContext.builder()
                    .toolName(toolCall.toolName())
                    .arguments(toolCall.args())
                    .build();
                
                ToolResult result = tool.execute(toolContext)
                    .get(TOOL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                if (result.success()) {
                    logger.debug("Tool {} executed successfully", toolCall.toolName());
                    return new ToolExecutionResult(
                        toolCall.toolName(), 
                        true, 
                        result.content().orElse(null), 
                        null
                    );
                } else {
                    throw new RuntimeException(result.error().orElse("Unknown error"));
                }
                
            } catch (Exception e) {
                logger.warn("Tool {} failed (attempt {}): {}", 
                    toolCall.toolName(), attempt + 1, e.getMessage());
                
                if (attempt < MAX_RETRIES - 1) {
                    // Retry with exponential backoff
                    long nextDelay = Math.min(delayMs * 2, MAX_RETRY_DELAY_MS);
                    logger.debug("Retrying in {}ms", nextDelay);
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    
                    return executeToolWithRetry(toolCall, context, attempt + 1, nextDelay).join();
                }
                
                return new ToolExecutionResult(
                    toolCall.toolName(), 
                    false, 
                    null, 
                    e.getMessage()
                );
            }
        });
    }
    
    /**
     * Parse tool calls from LLM response
     */
    private List<ToolCall> parseToolCalls(String response) {
        List<ToolCall> calls = new ArrayList<>();
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        
        while (matcher.find()) {
            String toolName = matcher.group(1);
            String argsJson = matcher.group(2);
            
            try {
                Map<String, Object> args = parseArgs(argsJson);
                calls.add(new ToolCall(toolName, args));
            } catch (Exception e) {
                logger.error("Failed to parse tool call args: {}", argsJson, e);
            }
        }
        
        return calls;
    }
    
    /**
     * Check if response contains tool calls
     */
    private boolean hasToolCalls(String response) {
        return TOOL_CALL_PATTERN.matcher(response).find();
    }
    
    /**
     * Parse JSON args (simplified)
     */
    private Map<String, Object> parseArgs(String argsJson) {
        Map<String, Object> args = new HashMap<>();
        // Simplified parsing - in production use proper JSON parser
        argsJson = argsJson.replace("{", "").replace("}", "").trim();
        String[] pairs = argsJson.split(",");
        
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim().replace("\"", "");
                args.put(key, value);
            }
        }
        
        return args;
    }
    
    /**
     * Build context string from tool results
     */
    private String buildToolResultsContext(List<ToolExecutionResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Tool Execution Results]:\n");
        
        for (ToolExecutionResult result : results) {
            sb.append("Tool: ").append(result.toolName()).append("\n");
            if (result.success()) {
                sb.append("Result: ").append(result.output()).append("\n");
            } else {
                sb.append("Error: ").append(result.error()).append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Execute tools in parallel
     */
    public CompletableFuture<List<ToolExecutionResult>> executeToolsParallel(
            List<ToolCall> toolCalls,
            ToolExecuteContext context) {
        
        List<CompletableFuture<ToolExecutionResult>> futures = toolCalls.stream()
            .map(toolCall -> executeToolWithRetry(toolCall, context))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList()
            );
    }
    
    // Records
    
    public record ToolCall(String toolName, Map<String, Object> args) {}
    
    public record ToolExecutionResult(
        String toolName,
        boolean success,
        String output,
        String error
    ) {}
    
    public record ToolChainResult(
        String finalResponse,
        List<ToolExecutionResult> toolResults,
        boolean success
    ) {}
}
