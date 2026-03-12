package openclaw.server.controller;

import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tool REST API Controller
 *
 * <p>Provides HTTP endpoints for tool discovery and execution.</p>
 */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    private static final Logger logger = LoggerFactory.getLogger(ToolController.class);

    private final Map<String, AgentTool> tools;

    public ToolController(List<AgentTool> toolList) {
        this.tools = toolList.stream()
                .collect(Collectors.toMap(AgentTool::getName, t -> t));
    }

    /**
     * List all available tools
     */
    @GetMapping
    public Mono<List<ToolInfo>> listTools() {
        return Mono.just(tools.values().stream()
                .map(tool -> new ToolInfo(
                        tool.getName(),
                        tool.getDescription(),
                        tool.getParameters()
                ))
                .toList());
    }

    /**
     * Get tool details
     */
    @GetMapping("/{toolName}")
    public Mono<ToolInfo> getTool(@PathVariable String toolName) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            return Mono.error(new ToolNotFoundException("Tool not found: " + toolName));
        }

        return Mono.just(new ToolInfo(
                tool.getName(),
                tool.getDescription(),
                tool.getParameters()
        ));
    }

    /**
     * Execute a tool
     */
    @PostMapping("/{toolName}/execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ToolExecuteResponse> executeTool(
            @PathVariable String toolName,
            @RequestBody Map<String, Object> arguments) {

        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            return Mono.error(new ToolNotFoundException("Tool not found: " + toolName));
        }

        logger.info("Executing tool: {} with args: {}", toolName, arguments);

        ToolExecuteContext context = new ToolExecuteContext(arguments, Map.of());

        return Mono.fromFuture(tool.execute(context))
                .map(result -> new ToolExecuteResponse(
                        result.success(),
                        result.output(),
                        result.error().orElse(null),
                        result.metadata()
                ));
    }

    // Request/Response Records

    public record ToolInfo(
            String name,
            String description,
            AgentTool.ToolParameters parameters
    ) {}

    public record ToolExecuteResponse(
            boolean success,
            String output,
            String error,
            Map<String, Object> metadata
    ) {}

    // Exceptions

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ToolNotFoundException extends RuntimeException {
        public ToolNotFoundException(String message) {
            super(message);
        }
    }
}
