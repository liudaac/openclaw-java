package openclaw.acp;

import openclaw.acp.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApprovalClassifier.
 */
class ApprovalClassifierTest {

    private ApprovalClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new ApprovalClassifier();
    }

    @Test
    void testSearchToolsAutoApproved() {
        // Search tools should be READONLY_SEARCH and auto-approved
        ToolCall searchCall = ToolCall.builder()
            .title("web_search")
            .build();
        
        AcpApprovalClassification result = classifier.classify(searchCall);
        
        assertEquals("web_search", result.toolName());
        assertEquals(AcpApprovalClass.READONLY_SEARCH, result.approvalClass());
        assertTrue(result.autoApprove());
        assertTrue(result.canAutoApprove());
    }

    @Test
    void testMemorySearchAutoApproved() {
        ToolCall memoryCall = ToolCall.builder()
            .title("memory_search")
            .build();
        
        AcpApprovalClassification result = classifier.classify(memoryCall);
        
        assertEquals(AcpApprovalClass.READONLY_SEARCH, result.approvalClass());
        assertTrue(result.autoApprove());
    }

    @Test
    void testExecToolsRequireApproval() {
        // Exec tools should be EXEC_CAPABLE and require approval
        ToolCall execCall = ToolCall.builder()
            .title("exec")
            .build();
        
        AcpApprovalClassification result = classifier.classify(execCall);
        
        assertEquals("exec", result.toolName());
        assertEquals(AcpApprovalClass.EXEC_CAPABLE, result.approvalClass());
        assertFalse(result.autoApprove());
        assertFalse(result.canAutoApprove());
    }

    @Test
    void testSpawnToolsRequireApproval() {
        ToolCall spawnCall = ToolCall.builder()
            .title("spawn")
            .build();
        
        AcpApprovalClassification result = classifier.classify(spawnCall);
        
        assertEquals(AcpApprovalClass.EXEC_CAPABLE, result.approvalClass());
        assertFalse(result.autoApprove());
    }

    @Test
    void testControlPlaneToolsRequireApproval() {
        ToolCall gatewayCall = ToolCall.builder()
            .title("gateway")
            .build();
        
        AcpApprovalClassification result = classifier.classify(gatewayCall);
        
        assertEquals(AcpApprovalClass.CONTROL_PLANE, result.approvalClass());
        assertFalse(result.autoApprove());
    }

    @Test
    void testSessionsSpawnControlPlane() {
        ToolCall sessionsCall = ToolCall.builder()
            .title("sessions_spawn")
            .build();
        
        AcpApprovalClassification result = classifier.classify(sessionsCall);
        
        assertEquals(AcpApprovalClass.CONTROL_PLANE, result.approvalClass());
        assertFalse(result.autoApprove());
    }

    @Test
    void testReadonlyScopedTools() {
        ToolCall readCall = ToolCall.builder()
            .title("read_file")
            .build();
        
        AcpApprovalClassification result = classifier.classify(readCall);
        
        assertEquals(AcpApprovalClass.READONLY_SCOPED, result.approvalClass());
        assertFalse(result.autoApprove());  // Not auto-approved, just readonly
    }

    @Test
    void testMutatingTools() {
        ToolCall writeCall = ToolCall.builder()
            .title("write_file")
            .build();
        
        AcpApprovalClassification result = classifier.classify(writeCall);
        
        assertEquals(AcpApprovalClass.MUTATING, result.approvalClass());
        assertFalse(result.autoApprove());
    }

    @Test
    void testDeleteToolMutating() {
        ToolCall deleteCall = ToolCall.builder()
            .title("delete_file")
            .build();
        
        AcpApprovalClassification result = classifier.classify(deleteCall);
        
        assertEquals(AcpApprovalClass.MUTATING, result.approvalClass());
        assertFalse(result.autoApprove());
    }

    @Test
    void testUnknownTool() {
        ToolCall unknownCall = ToolCall.builder()
            .title("some_unknown_tool")
            .build();
        
        AcpApprovalClassification result = classifier.classify(unknownCall);
        
        assertEquals(AcpApprovalClass.OTHER, result.approvalClass());
        assertFalse(result.autoApprove());
    }

    @Test
    void testNullToolCall() {
        AcpApprovalClassification result = classifier.classify(null);
        
        assertEquals("unknown", result.toolName());
        // Null tool call results in OTHER (empty tool name falls through to OTHER)
        assertTrue(result.approvalClass() == AcpApprovalClass.UNKNOWN || 
                   result.approvalClass() == AcpApprovalClass.OTHER);
    }

    @Test
    void testToolNameFromMeta() {
        ToolCall toolCall = ToolCall.builder()
            ._meta(Map.of("tool", "web_search"))
            .build();
        
        AcpApprovalClassification result = classifier.classify(toolCall);
        
        assertEquals("web_search", result.toolName());
        assertEquals(AcpApprovalClass.READONLY_SEARCH, result.approvalClass());
    }

    @Test
    void testToolNameFromToolField() {
        ToolCall toolCall = ToolCall.builder()
            .tool("exec")
            .build();
        
        AcpApprovalClassification result = classifier.classify(toolCall);
        
        assertEquals("exec", result.toolName());
        assertEquals(AcpApprovalClass.EXEC_CAPABLE, result.approvalClass());
    }

    @Test
    void testToolNameFromType() {
        ToolCall toolCall = ToolCall.builder()
            .type("search")
            .build();
        
        AcpApprovalClassification result = classifier.classify(toolCall);
        
        assertEquals("search", result.toolName());
        assertEquals(AcpApprovalClass.READONLY_SEARCH, result.approvalClass());
    }

    @Test
    void testClassificationDescription() {
        ToolCall toolCall = ToolCall.builder()
            .title("web_search")
            .build();
        
        AcpApprovalClassification result = classifier.classify(toolCall);
        
        String description = result.getDescription();
        assertTrue(description.contains("web_search"));
        assertTrue(description.contains("readonly_search"));
        assertTrue(description.contains("autoApprove=true"));
    }
}
