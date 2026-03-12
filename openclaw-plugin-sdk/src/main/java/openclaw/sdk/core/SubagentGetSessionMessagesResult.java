package openclaw.sdk.core;

import java.util.List;

/**
 * Result of getting session messages.
 *
 * @param messages the list of messages
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record SubagentGetSessionMessagesResult(List<Object> messages) {
}
