package openclaw.agent.security;

import openclaw.agent.session.SessionStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToolPolicyEnforcer.
 */
class ToolPolicyEnforcerTest {

    private ToolPolicyConfiguration config;
    private ToolPolicyEnforcer enforcer;

    @BeforeEach
    void setUp() {
        config = new ToolPolicyConfiguration();
        enforcer = new ToolPolicyEnforcer(config);
    }

    @Test
    void shouldAllowMemorySearchForSubagent() {
        SessionStateMachine.SessionContext subagentContext = createSubagentContext();
        
        assertTrue(enforcer.isToolAllowed("memory_search", subagentContext));
        assertTrue(enforcer.isToolAllowed("memory_get", subagentContext));
    }

    @Test
    void shouldAllowReadToolsForSubagent() {
        SessionStateMachine.SessionContext subagentContext = createSubagentContext();
        
        assertTrue(enforcer.isToolAllowed("read", subagentContext));
        assertTrue(enforcer.isToolAllowed("web_search", subagentContext));
        assertTrue(enforcer.isToolAllowed("web_fetch", subagentContext));
    }

    @Test
    void shouldDenyWriteForSubagent() {
        SessionStateMachine.SessionContext subagentContext = createSubagentContext();
        
        assertFalse(enforcer.isToolAllowed("write", subagentContext));
        assertFalse(enforcer.isToolAllowed("edit", subagentContext));
    }

    @Test
    void shouldDenyMemoryCreateForSubagent() {
        SessionStateMachine.SessionContext subagentContext = createSubagentContext();
        
        assertFalse(enforcer.isToolAllowed("memory_create", subagentContext));
        assertFalse(enforcer.isToolAllowed("memory_update", subagentContext));
        assertFalse(enforcer.isToolAllowed("memory_delete", subagentContext));
    }

    @Test
    void shouldAllowAllToolsForMainAgent() {
        SessionStateMachine.SessionContext mainContext = createMainAgentContext();
        
        assertTrue(enforcer.isToolAllowed("memory_search", mainContext));
        assertTrue(enforcer.isToolAllowed("memory_get", mainContext));
        assertTrue(enforcer.isToolAllowed("write", mainContext));
        assertTrue(enforcer.isToolAllowed("edit", mainContext));
        assertTrue(enforcer.isToolAllowed("memory_create", mainContext));
    }

    @Test
    void shouldRespectGlobalDenylistForMainAgent() {
        config.setDeniedTools(Set.of("dangerous_tool"));
        SessionStateMachine.SessionContext mainContext = createMainAgentContext();
        
        assertFalse(enforcer.isToolAllowed("dangerous_tool", mainContext));
        assertTrue(enforcer.isToolAllowed("memory_search", mainContext));
    }

    @Test
    void shouldDenyAllForNullContext() {
        assertFalse(enforcer.isToolAllowed("memory_search", null));
        assertFalse(enforcer.isToolAllowed("write", null));
    }

    @Test
    void shouldThrowExceptionWhenToolNotAllowed() {
        SessionStateMachine.SessionContext subagentContext = createSubagentContext();
        
        assertThrows(ToolPolicyEnforcer.ToolPolicyViolationException.class, () -> {
            enforcer.assertToolAllowed("write", subagentContext);
        });
    }

    @Test
    void shouldNotThrowWhenToolAllowed() {
        SessionStateMachine.SessionContext subagentContext = createSubagentContext();
        
        assertDoesNotThrow(() -> {
            enforcer.assertToolAllowed("memory_search", subagentContext);
        });
    }

    @Test
    void shouldReturnAllowedToolsForSubagent() {
        SessionStateMachine.SessionContext subagentContext = createSubagentContext();
        
        Set<String> allowed = enforcer.getAllowedTools(subagentContext);
        
        assertTrue(allowed.contains("memory_search"));
        assertTrue(allowed.contains("memory_get"));
        assertTrue(allowed.contains("read"));
    }

    @Test
    void shouldReturnEmptySetForMainAgent() {
        SessionStateMachine.SessionContext mainContext = createMainAgentContext();
        
        Set<String> allowed = enforcer.getAllowedTools(mainContext);
        
        assertTrue(allowed.isEmpty());
    }

    @Test
    void shouldDetectSubagentSession() {
        SessionStateMachine.SessionContext subagentContext = createSubagentContext();
        SessionStateMachine.SessionContext mainContext = createMainAgentContext();
        
        assertTrue(enforcer.isSubagentSession(subagentContext));
        assertFalse(enforcer.isSubagentSession(mainContext));
    }

    private SessionStateMachine.SessionContext createSubagentContext() {
        return new SessionStateMachine.SessionContext(
            "subagent-session-1",
            "gpt-4",
            SessionStateMachine.SessionState.ACTIVE,
            Instant.now(),
            null,
            0,
            0,
            Map.of(),
            "parent-session-1",
            SessionStateMachine.SessionType.SUBAGENT
        );
    }

    private SessionStateMachine.SessionContext createMainAgentContext() {
        return new SessionStateMachine.SessionContext(
            "main-session-1",
            "gpt-4",
            SessionStateMachine.SessionState.ACTIVE,
            Instant.now(),
            null,
            0,
            0,
            Map.of()
        );
    }
}
