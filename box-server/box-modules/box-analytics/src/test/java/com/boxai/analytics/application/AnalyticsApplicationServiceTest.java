package com.boxai.analytics.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsApplicationServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private ExecutionRepository executionRepository;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private ToolRepository toolRepository;
    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private McpServerRepository mcpServerRepository;
    @Mock
    private QuotaApplicationService quotaApplicationService;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private AnalyticsApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void overviewComputesSuccessRateAndTopAgents() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setName("Helper");
        agent.setStatus("PUBLISHED");
        agent.setUpdatedAt(LocalDateTime.now());
        when(agentRepository.listByWorkspace(7L)).thenReturn(List.of(agent));
        when(agentRepository.countByWorkspace(7L)).thenReturn(1);
        when(conversationRepository.listByWorkspaceAndUser(7L, 3L)).thenReturn(List.of());
        when(conversationRepository.countByWorkspaceAndUser(7L, 3L)).thenReturn(0);
        when(workspaceRepository.listMembersByUserId(3L)).thenReturn(List.of());
        when(executionRepository.countByWorkspace(7L)).thenReturn(2);
        when(knowledgeBaseRepository.listByWorkspace(7L)).thenReturn(List.of());
        when(toolRepository.listByWorkspace(7L)).thenReturn(List.of());
        when(workflowRepository.listByWorkspace(7L)).thenReturn(List.of());
        when(mcpServerRepository.listByWorkspace(7L)).thenReturn(List.of());
        when(quotaApplicationService.getQuotaForUser(3L)).thenReturn(null);
        when(executionRepository.listByWorkspace(7L, 500)).thenReturn(List.of(
                execution(21L, "SUCCEEDED", 100L),
                execution(21L, "FAILED", 50L)));

        var vo = service.overview(0);

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        assertEquals(7, vo.periodDays());
        assertEquals(2, vo.periodExecutionCount());
        assertEquals(50D, vo.successRate());
        assertEquals(75L, vo.avgLatencyMs());
        assertEquals(1, vo.topAgents().size());
        assertEquals("Helper", vo.topAgents().get(0).agentName());
        assertEquals(2, vo.topAgents().get(0).executionCount());
        assertEquals(50D, vo.topAgents().get(0).successRate());
        assertEquals("Helper", vo.recentAgents().get(0).name());
    }

    @Test
    void trendsClampsDaysAndFillsEveryDate() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(executionRepository.listByWorkspace(7L, 1000)).thenReturn(List.of());

        var vo = service.trends(999);

        assertEquals(90, vo.periodDays());
        assertEquals(90, vo.points().size());
        assertEquals(0, vo.points().get(0).executionCount());
    }

    private static Execution execution(Long agentId, String status, long durationMs) {
        Execution execution = new Execution();
        execution.setAgentId(agentId);
        execution.setStatus(status);
        execution.setDurationMs(durationMs);
        execution.setStartedAt(LocalDateTime.now());
        return execution;
    }
}
