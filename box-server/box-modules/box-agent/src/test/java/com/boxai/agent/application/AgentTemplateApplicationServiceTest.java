package com.boxai.agent.application;

import com.boxai.agent.api.AgentVO;
import com.boxai.agent.api.template.CreateAgentTemplateRequest;
import com.boxai.agent.api.template.UpdateAgentTemplateStatusRequest;
import com.boxai.common.constant.MarketReviewStatuses;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentTemplate;
import com.boxai.domain.agent.AgentTemplateRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.platform.PlatformModel;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentTemplateApplicationServiceTest {

    @Mock
    private AgentTemplateRepository agentTemplateRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentVersionRepository agentVersionRepository;
    @Mock
    private PlatformModelApplicationService platformModelApplicationService;
    @Mock
    private PlatformModelRepository platformModelRepository;
    @Mock
    private AgentApplicationService agentApplicationService;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private MarketRolloutResolver marketRolloutResolver;

    @InjectMocks
    private AgentTemplateApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSavesDraftPendingReview() {
        authenticate();
        when(platformModelRepository.findById(9L)).thenReturn(Optional.of(model("GPT")));

        var vo = service.create(new CreateAgentTemplateRequest(
                " helper ", " Helper ", " desc ", null, "support", "You are helpful", 9L, null, null, null, null, null));

        ArgumentCaptor<AgentTemplate> captor = ArgumentCaptor.forClass(AgentTemplate.class);
        verify(agentTemplateRepository).save(captor.capture());
        verify(platformModelApplicationService).resolveForChat(9L);
        assertEquals("helper", captor.getValue().getTemplateCode());
        assertEquals("DRAFT", captor.getValue().getStatus());
        assertEquals(MarketReviewStatuses.PENDING_REVIEW, captor.getValue().getReviewStatus());
        assertEquals(100, captor.getValue().getRolloutPercent());
        assertEquals("GPT", vo.platformModelName());
    }

    @Test
    void updateStatusRejectsListingBeforeApproval() {
        authenticate();
        AgentTemplate template = listedTemplate();
        template.setStatus("DRAFT");
        template.setReviewStatus(MarketReviewStatuses.PENDING_REVIEW);
        when(agentTemplateRepository.findById(4L)).thenReturn(Optional.of(template));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStatus(4L, new UpdateAgentTemplateStatusRequest("LISTED")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(agentTemplateRepository, never()).update(any());
    }

    @Test
    void listMarketHidesPromptAndNonRunnableTemplates() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        AgentTemplate visible = listedTemplate();
        visible.setSystemPrompt("secret");
        AgentTemplate hidden = listedTemplate();
        hidden.setId(5L);
        hidden.setPlatformModelId(10L);
        when(agentTemplateRepository.listListed()).thenReturn(List.of(visible, hidden));
        when(platformModelApplicationService.isRunnable(9L)).thenReturn(true);
        when(platformModelApplicationService.isRunnable(10L)).thenReturn(false);
        when(marketRolloutResolver.isVisible(4L, "GLOBAL", null, 100)).thenReturn(true);
        when(platformModelRepository.findById(9L)).thenReturn(Optional.of(model("GPT")));

        var vos = service.listMarket();

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        assertEquals(1, vos.size());
        assertEquals(4L, vos.get(0).id());
        assertNull(vos.get(0).systemPrompt());
    }

    @Test
    void enableCreatesDraftAgentFromListedTemplate() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        AgentTemplate template = listedTemplate();
        when(agentTemplateRepository.findById(4L)).thenReturn(Optional.of(template));
        when(marketRolloutResolver.isVisible(4L, "GLOBAL", null, 100)).thenReturn(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(21L);
            return agent;
        }).when(agentRepository).save(any(Agent.class));
        AgentVO detail = mock(AgentVO.class);
        when(detail.id()).thenReturn(21L);
        when(agentApplicationService.detail(21L)).thenReturn(detail);

        var vo = service.enable(4L);

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_CREATE);
        verify(platformModelApplicationService).resolveForChat(9L);
        ArgumentCaptor<AgentVersion> versionCaptor = ArgumentCaptor.forClass(AgentVersion.class);
        verify(agentVersionRepository).save(versionCaptor.capture());
        assertEquals(21L, versionCaptor.getValue().getAgentId());
        assertEquals(9L, versionCaptor.getValue().getPlatformModelId());
        assertEquals("DRAFT", versionCaptor.getValue().getStatus());
        verify(agentTemplateRepository).incrementInstallCount(4L);
        assertEquals(21L, vo.id());
    }

    @Test
    void enableRejectsUnlistedTemplate() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        AgentTemplate template = listedTemplate();
        template.setStatus("DRAFT");
        when(agentTemplateRepository.findById(4L)).thenReturn(Optional.of(template));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.enable(4L));
        assertEquals(ErrorCode.AGENT_TEMPLATE_NOT_FOUND, ex.getCode());
        verify(agentRepository, never()).save(any());
    }

    private void authenticate() {
        LoginUser user = new LoginUser(1L, "admin", UserTypes.PLATFORM_ADMIN, "SUPER_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static AgentTemplate listedTemplate() {
        AgentTemplate template = new AgentTemplate();
        template.setId(4L);
        template.setTemplateCode("helper");
        template.setName("Helper");
        template.setPlatformModelId(9L);
        template.setStatus("LISTED");
        template.setReviewStatus(MarketReviewStatuses.APPROVED);
        template.setVisibility("GLOBAL");
        template.setRolloutPercent(100);
        template.setSystemPrompt("You are helpful");
        return template;
    }

    private static PlatformModel model(String name) {
        PlatformModel model = new PlatformModel();
        model.setId(9L);
        model.setModelName(name);
        return model;
    }
}
