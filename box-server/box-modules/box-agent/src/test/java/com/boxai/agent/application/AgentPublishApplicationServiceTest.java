package com.boxai.agent.application;

import com.boxai.common.constant.AuditActions;
import com.boxai.common.constant.AuditResourceTypes;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.constant.PublishResourceTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.publish.Publish;
import com.boxai.domain.publish.PublishRepository;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.notification.NotificationPublisher;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPublishApplicationServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentVersionRepository agentVersionRepository;
    @Mock
    private AgentBindingApplicationService agentBindingApplicationService;
    @Mock
    private PublishRepository publishRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private AgentPublishApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void publishRejectsDraftWithoutModel() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = agent(21L, 7L);
        AgentVersion draft = new AgentVersion();
        draft.setId(8L);
        draft.setAgentId(21L);
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.of(draft));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.publish(21L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(agentRepository, never()).update(any());
    }

    @Test
    void publishCopiesDraftBindingsAndRevokesPreviousRelease() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = agent(21L, 7L);
        agent.setName("Helper");
        AgentVersion draft = new AgentVersion();
        draft.setId(8L);
        draft.setAgentId(21L);
        draft.setPlatformModelId(99L);
        draft.setVersionNo(1);
        draft.setVersionName("v1");
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.of(draft));
        when(agentVersionRepository.maxVersionNo(21L)).thenReturn(1);
        doAnswer(invocation -> {
            AgentVersion version = invocation.getArgument(0);
            version.setId(9L);
            return version;
        }).when(agentVersionRepository).save(any(AgentVersion.class));
        when(agentVersionRepository.findById(8L)).thenReturn(Optional.of(draft));

        var vo = service.publish(21L);

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_PUBLISH);
        verify(agentVersionRepository).update(draft);
        assertEquals("PUBLISHED", draft.getStatus());
        assertEquals("PUBLISHED", agent.getStatus());
        assertEquals(8L, agent.getPublishedVersionId());
        verify(agentBindingApplicationService).copyBindings(8L, 9L, 21L);
        verify(publishRepository).revokeByResource(PublishResourceTypes.AGENT, 21L);
        ArgumentCaptor<Publish> publishCaptor = ArgumentCaptor.forClass(Publish.class);
        verify(publishRepository).save(publishCaptor.capture());
        assertEquals(8L, publishCaptor.getValue().getVersionId());
        assertEquals("API", publishCaptor.getValue().getChannel());
        verify(auditLogService).recordSuccess(
                eq(AuditActions.AGENT_PUBLISH), eq(AuditResourceTypes.AGENT), eq(21L), eq("Helper"), any());
        verify(notificationPublisher).publish(eq(3L), eq(7L), eq("智能体已发布"), any(), eq("AGENT"), any());
        assertEquals(8L, vo.publishedVersionId());
        assertEquals(1, vo.publishedVersionNo());
    }

    @Test
    void unpublishClearsPublishedVersion() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = agent(21L, 7L);
        agent.setName("Helper");
        agent.setPublishedVersionId(8L);
        agent.setStatus("PUBLISHED");
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));

        var vo = service.unpublish(21L);

        assertNull(agent.getPublishedVersionId());
        assertEquals("DRAFT", agent.getStatus());
        verify(publishRepository).revokeByResource(PublishResourceTypes.AGENT, 21L);
        verify(auditLogService).recordSuccess(
                AuditActions.AGENT_UNPUBLISH, AuditResourceTypes.AGENT, 21L, "Helper", null);
        assertNull(vo.publishedVersionId());
        assertEquals("DRAFT", vo.status());
    }

    @Test
    void publishRejectsOtherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent(21L, 99L)));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.publish(21L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
    }

    private static Agent agent(Long id, Long workspaceId) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setWorkspaceId(workspaceId);
        agent.setStatus("DRAFT");
        return agent;
    }
}
