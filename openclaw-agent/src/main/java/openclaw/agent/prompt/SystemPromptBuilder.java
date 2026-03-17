package openclaw.agent.prompt;

import openclaw.agent.heartbeat.HeartbeatConfig;
import openclaw.agent.workspace.WorkspaceFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Builder for agent system prompts.
 *
 * <p>Constructs system prompts with heartbeat guidance, context files, and runtime info.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
public class SystemPromptBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SystemPromptBuilder.class);

    private final WorkspaceFileService workspaceFileService;
    private final HeartbeatConfig heartbeatConfig;

    // Runtime info
    private String agentId = "default";
    private String host = "localhost";
    private String os = System.getProperty("os.name");
    private String arch = System.getProperty("os.arch");
    private String node = System.getProperty("java.version");
    private String model = "unknown";
    private String channel = "unknown";
    private ZoneId userTimezone = ZoneId.systemDefault();

    public SystemPromptBuilder(WorkspaceFileService workspaceFileService, HeartbeatConfig heartbeatConfig) {
        this.workspaceFileService = workspaceFileService;
        this.heartbeatConfig = heartbeatConfig;
    }

    /**
     * Set agent ID.
     *
     * @param agentId the agent ID
     * @return this builder
     */
    public SystemPromptBuilder withAgentId(String agentId) {
        this.agentId = agentId != null ? agentId : "default";
        return this;
    }

    /**
     * Set host.
     *
     * @param host the host
     * @return this builder
     */
    public SystemPromptBuilder withHost(String host) {
        this.host = host != null ? host : "localhost";
        return this;
    }

    /**
     * Set OS.
     *
     * @param os the OS
     * @return this builder
     */
    public SystemPromptBuilder withOs(String os) {
        this.os = os != null ? os : System.getProperty("os.name");
        return this;
    }

    /**
     * Set architecture.
     *
     * @param arch the architecture
     * @return this builder
     */
    public SystemPromptBuilder withArch(String arch) {
        this.arch = arch != null ? arch : System.getProperty("os.arch");
        return this;
    }

    /**
     * Set Java version.
     *
     * @param node the Java version
     * @return this builder
     */
    public SystemPromptBuilder withNode(String node) {
        this.node = node != null ? node : System.getProperty("java.version");
        return this;
    }

    /**
     * Set model.
     *
     * @param model the model
     * @return this builder
     */
    public SystemPromptBuilder withModel(String model) {
        this.model = model != null ? model : "unknown";
        return this;
    }

    /**
     * Set channel.
     *
     * @param channel the channel
     * @return this builder
     */
    public SystemPromptBuilder withChannel(String channel) {
        this.channel = channel != null ? channel : "unknown";
        return this;
    }

    /**
     * Set user timezone.
     *
     * @param timezone the timezone
     * @return this builder
     */
    public SystemPromptBuilder withTimezone(ZoneId timezone) {
        this.userTimezone = timezone != null ? timezone : ZoneId.systemDefault();
        return this;
    }

    /**
     * Build the system prompt.
     *
     * @return the system prompt
     */
    public String build() {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("# OpenClaw Agent System Prompt\n\n");

        // Runtime info
        sb.append(buildRuntimeSection());

        // Context files
        sb.append(buildContextSection());

        // Heartbeat section
        sb.append(buildHeartbeatSection());

        // Silent replies
        sb.append(buildSilentRepliesSection());

        // Group chat guidance
        sb.append(buildGroupChatSection());

        return sb.toString();
    }

    private String buildRuntimeSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Runtime\n\n");
        sb.append(String.format("Runtime: agent=%s | host=%s | os=%s | arch=%s | node=%s | model=%s | channel=%s\n\n",
            agentId, host, os, arch, node, model, channel));

        // Current time
        ZonedDateTime now = ZonedDateTime.now(userTimezone);
        sb.append(String.format("Current time: %s\n", now.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
        sb.append(String.format("Timezone: %s\n\n", userTimezone.getId()));

        return sb.toString();
    }

    private String buildContextSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Project Context\n\n");

        // AGENTS.md
        Optional<String> agentsMd = workspaceFileService.readAgentsMd();
        if (agentsMd.isPresent()) {
            sb.append("### AGENTS.md\n\n");
            sb.append(agentsMd.get()).append("\n\n");
        }

        // SOUL.md
        Optional<String> soulMd = workspaceFileService.readSoulMd();
        if (soulMd.isPresent()) {
            sb.append("### SOUL.md\n\n");
            sb.append(soulMd.get()).append("\n\n");
        }

        // USER.md
        Optional<String> userMd = workspaceFileService.readUserMd();
        if (userMd.isPresent()) {
            sb.append("### USER.md\n\n");
            sb.append(userMd.get()).append("\n\n");
        }

        // MEMORY.md
        Optional<String> memoryMd = workspaceFileService.readMemoryMd();
        if (memoryMd.isPresent()) {
            sb.append("### MEMORY.md\n\n");
            sb.append(memoryMd.get()).append("\n\n");
        }

        // TOOLS.md
        Optional<String> toolsMd = workspaceFileService.readToolsMd();
        if (toolsMd.isPresent()) {
            sb.append("### TOOLS.md\n\n");
            sb.append(toolsMd.get()).append("\n\n");
        }

        return sb.toString();
    }

    private String buildHeartbeatSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Heartbeats\n\n");

        sb.append("Heartbeat prompt: ").append(heartbeatConfig.resolvePrompt()).append("\n\n");

        sb.append("If you receive a heartbeat poll (a user message matching the heartbeat prompt above), ");
        sb.append("and there is nothing that needs attention, reply exactly:\n");
        sb.append(HeartbeatConfig.HEARTBEAT_TOKEN).append("\n\n");

        sb.append("OpenClaw treats a leading/trailing \"").append(HeartbeatConfig.HEARTBEAT_TOKEN);
        sb.append("\" as a heartbeat ack (and may discard it).\n");
        sb.append("If something needs attention, do NOT include \"").append(HeartbeatConfig.HEARTBEAT_TOKEN);
        sb.append("\"; reply with the alert text instead.\n\n");

        // Heartbeat vs Cron guidance
        sb.append("### Heartbeat vs Cron: When to Use Each\n\n");
        sb.append("**Use heartbeat when:**\n");
        sb.append("- Multiple checks can batch together (inbox + calendar + notifications in one turn)\n");
        sb.append("- You need conversational context from recent messages\n");
        sb.append("- Timing can drift slightly (every ~30 min is fine, not exact)\n");
        sb.append("- You want to reduce API calls by combining periodic checks\n\n");

        sb.append("**Use cron when:**\n");
        sb.append("- Exact timing matters (\"9:00 AM sharp every Monday\")\n");
        sb.append("- Task needs isolation from main session history\n");
        sb.append("- You want a different model or thinking level for the task\n");
        sb.append("- One-shot reminders (\"remind me in 20 minutes\")\n");
        sb.append("- Output should deliver directly to a channel without main session involvement\n\n");

        return sb.toString();
    }

    private String buildSilentRepliesSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Silent Replies\n\n");
        sb.append("When you have nothing to say, respond with ONLY: ").append(HeartbeatConfig.SILENT_REPLY_TOKEN).append("\n\n");

        sb.append("Rules:\n");
        sb.append("- It must be your ENTIRE message - nothing else\n");
        sb.append("- Never append it to an actual response (never include \"").append(HeartbeatConfig.SILENT_REPLY_TOKEN).append("\" in real replies)\n");
        sb.append("- Never wrap it in markdown or code blocks\n");
        sb.append("Wrong: \"Here's help... ").append(HeartbeatConfig.SILENT_REPLY_TOKEN).append("\"\n");
        sb.append("Wrong: \"").append(HeartbeatConfig.SILENT_REPLY_TOKEN).append("\"\n");
        sb.append("Right: ").append(HeartbeatConfig.SILENT_REPLY_TOKEN).append("\n\n");

        return sb.toString();
    }

    private String buildGroupChatSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Group Chat Context\n\n");

        sb.append("**Know When to Speak!**\n\n");
        sb.append("In group chats where you receive every message, be **smart about when to contribute**:\n\n");

        sb.append("**Respond when:**\n");
        sb.append("- Directly mentioned or asked a question\n");
        sb.append("- You can add genuine value (info, insight, help)\n");
        sb.append("- Something witty/funny fits naturally\n");
        sb.append("- Correcting important misinformation\n");
        sb.append("- Summarizing when asked\n\n");

        sb.append("**Stay silent (").append(HeartbeatConfig.SILENT_REPLY_TOKEN).append(") when:**\n");
        sb.append("- It's just casual banter between humans\n");
        sb.append("- Someone already answered the question\n");
        sb.append("- Your response would just be \"yeah\" or \"nice\"\n");
        sb.append("- The conversation is flowing fine without you\n");
        sb.append("- Adding a message would interrupt the vibe\n\n");

        sb.append("**The human rule:** Humans in group chats don't respond to every single message. Neither should you. ");
        sb.append("Quality > quantity. If you wouldn't send it in a real group chat with friends, don't send it.\n\n");

        sb.append("**Avoid the triple-tap:** Don't respond multiple times to the same message with different reactions. ");
        sb.append("One thoughtful response beats three fragments.\n\n");

        sb.append("Participate, don't dominate.\n\n");

        return sb.toString();
    }
}
