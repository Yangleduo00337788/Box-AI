package com.boxai.agent.application;

import com.boxai.agent.api.CreateAgentVersionRequest;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.domain.agent.AgentMcpRepository;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
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
class AgentVersionApplicationServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentVersionRepository agentVersionRepository;
    @Mock
    private AgentBindingApplicationService agentBindingApplicationService;
    @Mock
    private AgentKnowledgeRepository agentKnowledgeRepository;
    @Mock
    private AgentToolRepository agentToolRepository;
    @Mock
    private AgentMcpRepository agentMcpRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private AgentVersionApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void restoreRejectsPublishedAgent() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = agent("PUBLISHED");
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.restore(21L, 8L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(agentVersionRepository, never()).update(any());
    }

    @Test
    void restoreRejectsCurrentDraft() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = agent("DRAFT");
        AgentVersion draft = version(8L, 21L, "你是助手");
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));
        when(agentVersionRepository.findById(8L)).thenReturn(Optional.of(draft));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.of(draft));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.restore(21L, 8L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void restoreCopiesPromptOntoDraft() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = agent("DRAFT");
        AgentVersion archived = version(9L, 21L, "旧提示");
        AgentVersion draft = version(8L, 21L, "新提示");
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));
        when(agentVersionRepository.findById(9L)).thenReturn(Optional.of(archived));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.of(draft));

        service.restore(21L, 9L);

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_UPDATE);
        assertEquals("旧提示", draft.getSystemPrompt());
        verify(agentVersionRepository).update(draft);
        verify(agentBindingApplicationService).replaceBindings(8L, 9L, 21L);
    }

    @Test
    void archiveRejectsPublishedVersion() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = agent("DRAFT");
        agent.setPublishedVersionId(9L);
        AgentVersion published = version(9L, 21L, "发布");
        AgentVersion draft = version(8L, 21L, "草稿");
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));
        when(agentVersionRepository.findById(9L)).thenReturn(Optional.of(published));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.of(draft));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.archive(21L, 9L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void createSnapshotCopiesDraftAndBindings() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = agent("DRAFT");
        AgentVersion draft = version(8L, 21L, "提示");
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.of(draft));
        when(agentVersionRepository.maxVersionNo(21L)).thenReturn(2);
        when(agentVersionRepository.save(any(AgentVersion.class))).thenAnswer(invocation -> {
            AgentVersion snapshot = invocation.getArgument(0);
            snapshot.setId(30L);
            return snapshot;
        });

        var vo = service.createSnapshot(21L, new CreateAgentVersionRequest(null));

        ArgumentCaptor<AgentVersion> captor = ArgumentCaptor.forClass(AgentVersion.class);
        verify(agentVersionRepository).save(captor.capture());
        assertEquals("ARCHIVED", captor.getValue().getStatus());
        assertEquals(3, captor.getValue().getVersionNo());
        assertEquals("v3", captor.getValue().getVersionName());
        verify(agentBindingApplicationService).copyBindings(8L, 30L, 21L);
        assertEquals(30L, vo.id());
        assertFalse(vo.currentDraft());
    }

    @Test
    void compareMarksPromptChange() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent("DRAFT")));
        when(agentVersionRepository.findById(8L)).thenReturn(Optional.of(version(8L, 21L, "A")));
        when(agentVersionRepository.findById(9L)).thenReturn(Optional.of(version(9L, 21L, "B")));
        when(agentKnowledgeRepository.listByVersionId(8L)).thenReturn(List.of());
        when(agentKnowledgeRepository.listByVersionId(9L)).thenReturn(List.of());
        when(agentToolRepository.listByVersionId(8L)).thenReturn(List.of());
        when(agentToolRepository.listByVersionId(9L)).thenReturn(List.of());
        when(agentMcpRepository.listByVersionId(8L)).thenReturn(List.of());
        when(agentMcpRepository.listByVersionId(9L)).thenReturn(List.of());

        var vo = service.compare(21L, 8L, 9L);

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        var prompt = vo.diffs().stream().filter(item -> "systemPrompt".equals(item.field())).findFirst().orElseThrow();
        assertTrue(prompt.changed());
        assertEquals("A", prompt.baseValue());
        assertEquals("B", prompt.targetValue());
    }

    private static Agent agent(String status) {
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setWorkspaceId(7L);
        agent.setStatus(status);
        return agent;
    }

    private static AgentVersion version(Long id, Long agentId, String prompt) {
        AgentVersion version = new AgentVersion();
        version.setId(id);
        version.setAgentId(agentId);
        version.setSystemPrompt(prompt);
        version.setStatus("DRAFT");
        return version;
    }
}
