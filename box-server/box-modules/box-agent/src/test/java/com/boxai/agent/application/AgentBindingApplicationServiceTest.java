package com.boxai.agent.application;

import com.boxai.agent.api.BindAgentKnowledgeRequest;
import com.boxai.agent.api.BindAgentSubAgentRequest;
import com.boxai.agent.api.BindAgentToolRequest;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentKnowledge;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.domain.agent.AgentMcpRepository;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentSubAgentRepository;
import com.boxai.domain.agent.AgentTool;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.agent.AgentWorkflowRepository;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentBindingApplicationServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentVersionRepository agentVersionRepository;
    @Mock
    private AgentKnowledgeRepository agentKnowledgeRepository;
    @Mock
    private AgentToolRepository agentToolRepository;
    @Mock
    private AgentMcpRepository agentMcpRepository;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private ToolRepository toolRepository;
    @Mock
    private McpServerRepository mcpServerRepository;
    @Mock
    private AgentSubAgentRepository agentSubAgentRepository;
    @Mock
    private AgentWorkflowRepository agentWorkflowRepository;
    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private AgentBindingApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void bindKnowledgeAppliesDefaultsAndEnablesDraft() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        stubAgentAndDraft();
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(4L);
        kb.setWorkspaceId(7L);
        when(knowledgeBaseRepository.findById(4L)).thenReturn(Optional.of(kb));
        when(agentKnowledgeRepository.findByVersionAndKnowledgeBase(8L, 4L)).thenReturn(Optional.empty());

        var vo = service.bindKnowledge(21L, new BindAgentKnowledgeRequest(4L, null, null, null, null));

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_UPDATE);
        ArgumentCaptor<AgentKnowledge> captor = ArgumentCaptor.forClass(AgentKnowledge.class);
        verify(agentKnowledgeRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getTopK());
        assertEquals("HYBRID", captor.getValue().getRetrievalMode());
        assertTrue(captor.getValue().getRerankEnabled());
        ArgumentCaptor<AgentVersion> versionCaptor = ArgumentCaptor.forClass(AgentVersion.class);
        verify(agentVersionRepository).update(versionCaptor.capture());
        assertTrue(versionCaptor.getValue().getKnowledgeEnabled());
        assertEquals(4L, vo.knowledgeBaseId());
    }

    @Test
    void bindKnowledgeRejectsOtherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        stubAgentAndDraft();
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(4L);
        kb.setWorkspaceId(99L);
        when(knowledgeBaseRepository.findById(4L)).thenReturn(Optional.of(kb));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bindKnowledge(21L, new BindAgentKnowledgeRequest(4L, 3, "VECTOR", false, false)));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
        verify(agentKnowledgeRepository, never()).save(any());
    }

    @Test
    void unbindLastKnowledgeDisablesDraft() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        stubAgentAndDraft();
        AgentKnowledge binding = new AgentKnowledge();
        binding.setId(12L);
        when(agentKnowledgeRepository.findByVersionAndKnowledgeBase(8L, 4L)).thenReturn(Optional.of(binding));
        when(agentKnowledgeRepository.listByVersionId(8L)).thenReturn(List.of());

        service.unbindKnowledge(21L, 4L);

        verify(agentKnowledgeRepository).delete(12L);
        ArgumentCaptor<AgentVersion> captor = ArgumentCaptor.forClass(AgentVersion.class);
        verify(agentVersionRepository).update(captor.capture());
        assertFalse(captor.getValue().getKnowledgeEnabled());
    }

    @Test
    void bindToolRejectsOtherWorkspaceAndEnablesToolsOnSuccess() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        stubAgentAndDraft();
        Tool foreign = new Tool();
        foreign.setId(6L);
        foreign.setWorkspaceId(99L);
        when(toolRepository.findById(6L)).thenReturn(Optional.of(foreign));
        BusinessException denied = assertThrows(BusinessException.class,
                () -> service.bindTool(21L, new BindAgentToolRequest(6L, true, true, "{}")));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, denied.getCode());

        Tool local = new Tool();
        local.setId(6L);
        local.setWorkspaceId(7L);
        when(toolRepository.findById(6L)).thenReturn(Optional.of(local));
        when(agentToolRepository.findByVersionAndTool(8L, 6L)).thenReturn(Optional.empty());
        when(agentWorkflowRepository.listByVersionId(8L)).thenReturn(List.of());
        when(agentToolRepository.listByVersionId(8L)).thenReturn(List.of(new AgentTool()));

        service.bindTool(21L, new BindAgentToolRequest(6L, null, null, "  "));

        ArgumentCaptor<AgentTool> toolCaptor = ArgumentCaptor.forClass(AgentTool.class);
        verify(agentToolRepository).save(toolCaptor.capture());
        assertTrue(toolCaptor.getValue().getEnabled());
        assertFalse(toolCaptor.getValue().getRequireConfirmation());
        ArgumentCaptor<AgentVersion> versionCaptor = ArgumentCaptor.forClass(AgentVersion.class);
        verify(agentVersionRepository).update(versionCaptor.capture());
        assertTrue(versionCaptor.getValue().getToolEnabled());
    }

    @Test
    void bindSubAgentRejectsSelf() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        stubAgentAndDraft();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bindSubAgent(21L, new BindAgentSubAgentRequest(21L, true, 0)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(agentSubAgentRepository, never()).save(any());
    }

    private void stubAgentAndDraft() {
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setWorkspaceId(7L);
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));
        AgentVersion draft = new AgentVersion();
        draft.setId(8L);
        draft.setAgentId(21L);
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.of(draft));
    }
}
