package openclaw.agent.tool;

import openclaw.acp.model.ToolCall;
import openclaw.agent.approval.ApprovalRequiredException;
import openclaw.agent.approval.ApprovalService;
import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Approval-aware tool executor.
 * Wraps tool execution with ACP approval classification.
 *
 * @author OpenClaw Team
 * @version 2026.4.2
 */
@Service
public class ApprovalAwareToolExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalAwareToolExecutor.class);

    @Autowired
    private ApprovalService approvalService;

    // Tool registry
    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

    /**
     * Register a tool.
     *
     * @param tool the tool to register
     */
    public void registerTool(AgentTool tool) {
        tools.put(tool.getName(), tool);
        logger.debug("Registered tool: {}", tool.getName());
    }

    /**
     * Execute a tool with approval check.
     *
     * @param toolName the tool name
     * @param arguments the tool arguments
     * @param context the execution context
     * @return CompletableFuture of tool result
     */
    public CompletableFuture<ToolResult> execute(String toolName, 
                                                  Map<String, Object> arguments,
                                                  ToolExecuteContext context) {
        // Convert to ToolCall for ACP classification
        ToolCall toolCall = convertToToolCall(toolName, arguments);

        // Check approval
        return approvalService.evaluate(toolCall, context)
            .flatMap(decision -> {
                if (decision.isApproved()) {
                    // Auto-approved or pre-approved, execute directly
                    logger.debug("Tool '{}' approved (auto={}), executing...", 
                        toolName, decision.isAutoApproved());
                    return executeTool(toolName, arguments, context);
                } else if (decision.isPending()) {
                    // Requires manual approval
                    logger.info("Tool '{}' requires approval (requestId={})", 
                        toolName, decision.getRequestId());
                    return Mono.error(new ApprovalRequiredException(
                        decision.getRequestId(),
                        toolName,
                        decision.getApprovalClass(),
                        decision.getMessage()
                    ));
                } else {
                    // Rejected
                    return Mono.error(new RuntimeException("Tool execution rejected: " + decision.getMessage()));
                }
            })
            .subscribeOn(Schedulers.boundedElastic())
            .toFuture();
    }

    /**
     * Execute a tool that was previously pending approval.
     *
     * @param requestId the approval request ID
     * @param context the execution context
     * @return CompletableFuture of tool result
     */
    public CompletableFuture<ToolResult> executeApprovedTool(String requestId,
                                                              ToolExecuteContext context) {
        ApprovalService.ApprovalRequest request = approvalService.getPendingRequest(requestId);
        
        if (request == null) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Approval request not found: " + requestId)
            );
        }

        if (!request.isApproved()) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Tool not yet approved: " + requestId)
            );
        }

        // Execute the approved tool
        ToolCall toolCall = request.getToolCall();
        String toolName = request.getClassification().toolName();
        Map<String, Object> arguments = extractArguments(toolCall);

        logger.info("Executing approved tool '{}' (requestId={})", toolName, requestId);

        return executeTool(toolName, arguments, context)
            .doOnSuccess(result -> {
                // Clean up the approval request
                approvalService.removeRequest(requestId);
            })
            .doOnError(error -> {
                // Clean up on error too
                approvalService.removeRequest(requestId);
            })
            .toFuture();
    }

    /**
     * Check if a tool is registered.
     *
     * @param toolName the tool name
     * @return true if registered
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * Get a registered tool.
     *
     * @param toolName the tool name
     * @return the tool or null
     */
    public AgentTool getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * Execute tool directly (internal use).
     */
    private Mono<ToolResult> executeTool(String toolName,
                                          Map<String, Object> arguments,
                                          ToolExecuteContext context) {
        AgentTool tool = tools.get(toolName);
        
        if (tool == null) {
            return Mono.error(new RuntimeException("Tool not found: " + toolName));
        }

        return Mono.fromFuture(() -> {
            ToolExecuteContext toolContext = ToolExecuteContext.builder()
                .toolName(toolName)
                .arguments(arguments)
                .sessionKey(context.sessionKey().orElse(null))
                .sessionId(context.sessionId().orElse(null))
                .messageChannel(context.messageChannel().orElse(null))
                .agentId(context.agentId().orElse(null))
                .build();

            return tool.execute(toolContext);
        });
    }

    /**
     * Convert to ToolCall for ACP classification.
     */
    private ToolCall convertToToolCall(String toolName, Map<String, Object> arguments) {
        ToolCall toolCall = new ToolCall();
        toolCall.setTool(toolName);
        toolCall.setTitle(toolName);
        toolCall.setType("function");
        // Set arguments as raw input for classification
        toolCall.setRawInput("{\"tool\": \"" + toolName + "\"}");
        return toolCall;
    }

    /**
     * Extract arguments from ToolCall.
     */
    private Map<String, Object> extractArguments(ToolCall toolCall) {
        // In real implementation, parse from toolCall
        // For now, return empty map
        return Map.of();
    }

    /**
     * Get all registered tool names.
     */
    public java.util.Set<String> getToolNames() {
        return tools.keySet();
    }
}
