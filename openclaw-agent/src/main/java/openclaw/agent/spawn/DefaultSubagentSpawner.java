package openclaw.agent.spawn;

import openclaw.agent.AcpProtocol;
import openclaw.agent.AcpProtocol.SpawnRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default subagent spawner implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class DefaultSubagentSpawner implements SubagentSpawner {

    private final AcpProtocol acpProtocol;
    private final Map<String, SubagentInfo> subagents = new ConcurrentHashMap<>();

    public DefaultSubagentSpawner(AcpProtocol acpProtocol) {
        this.acpProtocol = acpProtocol;
    }

    @Override
    public CompletableFuture<SpawnResult> spawn(String parentSession, String task) {
        return spawnWithOptions(parentSession, task, SpawnOptions.defaults());
    }

    @Override
    public CompletableFuture<SpawnResult> spawnWithOptions(
            String parentSession,
            String task,
            SpawnOptions options) {

        return CompletableFuture.supplyAsync(() -> {
            String subagentId = generateSubagentId();
            String sessionKey = "subagent-" + subagentId;

            SpawnRequest request = SpawnRequest.builder()
                    .sessionKey(sessionKey)
                    .userMessage(task)
                    .systemPrompt(options.systemPrompt())
                    .model(options.model())
                    .tools(options.tools())
                    .metadata(Map.of(
                            "parentSession", parentSession,
                            "subagentId", subagentId
                    ))
                    .build();

            try {
                AcpProtocol.SpawnResult result = acpProtocol.spawnAgent(request).join();

                if (result.success()) {
                    SubagentInfo info = new SubagentInfo(
                            subagentId,
                            result.sessionKey(),
                            parentSession,
                            SubagentStatus.RUNNING,
                            System.currentTimeMillis()
                    );
                    subagents.put(subagentId, info);

                    return SpawnResult.success(result.sessionKey(), subagentId);
                } else {
                    return SpawnResult.failure(result.error().orElse("Unknown error"));
                }
            } catch (Exception e) {
                return SpawnResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<SubagentInfo>> listSubagents(String parentSession) {
        return CompletableFuture.supplyAsync(() ->
                subagents.values().stream()
                        .filter(s -> s.parentSession().equals(parentSession))
                        .toList()
        );
    }

    @Override
    public CompletableFuture<Void> kill(String subagentId) {
        return CompletableFuture.runAsync(() -> {
            SubagentInfo info = subagents.get(subagentId);
            if (info != null) {
                acpProtocol.deleteSession(info.sessionKey()).join();
                subagents.put(subagentId, new SubagentInfo(
                        info.id(),
                        info.sessionKey(),
                        info.parentSession(),
                        SubagentStatus.KILLED,
                        info.startTime()
                ));
            }
        });
    }

    @Override
    public CompletableFuture<SubagentStatus> getStatus(String subagentId) {
        return CompletableFuture.supplyAsync(() -> {
            SubagentInfo info = subagents.get(subagentId);
            if (info == null) {
                return SubagentStatus.FAILED;
            }

            // Check if still running
            if (info.status() == SubagentStatus.RUNNING) {
                AcpProtocol.AgentMessages messages = acpProtocol.getMessages(info.sessionKey(), 1).join();
                // Check if completed based on messages
                // Simplified: assume running if no error
            }

            return info.status();
        });
    }

    private String generateSubagentId() {
        return "sub-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
