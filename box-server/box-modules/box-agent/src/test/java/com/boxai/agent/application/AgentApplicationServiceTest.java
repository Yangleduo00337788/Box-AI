package com.boxai.agent.application;

import com.boxai.agent.api.AgentChatRequest;
import com.boxai.agent.api.CreateAgentRequest;
import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.agent.chat.AgentToolRuntimeService;
import com.boxai.agent.chat.PreparedAgentChat;
import com.boxai.agent.chat.ToolConfirmationService;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.model.ModelDefinitionRepository;
import com.boxai.domain.model.ModelProviderRepository;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.knowledge.application.KnowledgeRetrievalResult;
import com.boxai.model.application.ModelRouterApplicationService;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.guard.ResourceDeleteGuard;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.trace.application.ExecutionRecorder;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.agent.chat.AgentToolRuntimeService;
import com.boxai.agent.chat.ToolConfirmationService;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.model.ModelDefinitionRepository;
import com.boxai.domain.model.ModelProviderRepository;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.model.application.ModelRouterApplicationService;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.guard.ResourceDeleteGuard;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.trace.application.ExecutionRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentApplicationServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentVersionRepository agentVersionRepository;
    @Mock
    private ModelDefinitionRepository modelDefinitionRepository;
    @Mock
    private ModelProviderRepository modelProviderRepository;
    @Mock
    private AgentChatPreparer agentChatPreparer;
    @Mock
    private AgentChatExecutor agentChatExecutor;
    @Mock
    private PlatformModelApplicationService platformModelApplicationService;
    @Mock
    private ModelRouterApplicationService modelRouterApplicationService;
    @Mock
    private PlatformModelRepository platformModelRepository;
    @Mock
    private ExecutionRecorder executionRecorder;
    @Mock
    private AgentBindingApplicationService agentBindingApplicationService;
    @Mock
    private AgentPublishApplicationService agentPublishApplicationService;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private AgentLongTermMemoryApplicationService longTermMemoryApplicationService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ResourceDeleteGuard resourceDeleteGuard;
    @Mock
    private ToolConfirmationService toolConfirmationService;
    @Mock
    private AgentToolRuntimeService agentToolRuntimeService;
    @Mock
    private EmbedDomainApplicationService embedDomainApplicationService;

    @InjectMocks
    private AgentApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createRequiresPlatformModelAndPersistsDraft() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        doAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(11L);
            return null;
        }).when(agentRepository).save(any(Agent.class));
        AgentVersion draft = new AgentVersion();
        draft.setAgentId(11L);
        draft.setVersionNo(1);
        draft.setPlatformModelId(99L);
        draft.setModelSource("PLATFORM");
        when(agentVersionRepository.findLatestDraft(11L)).thenReturn(Optional.of(draft));

        var vo = service.create(new CreateAgentRequest("  Helper  ", " desc ", null, 99L, null));

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_CREATE);
        verify(platformModelApplicationService).resolveForChat(99L);
        ArgumentCaptor<AgentVersion> versionCaptor = ArgumentCaptor.forClass(AgentVersion.class);
        verify(agentVersionRepository).save(versionCaptor.capture());
        assertEquals(11L, versionCaptor.getValue().getAgentId());
        assertEquals("DRAFT", versionCaptor.getValue().getStatus());
        assertEquals(99L, versionCaptor.getValue().getPlatformModelId());
        assertEquals("Helper", vo.name());
        assertEquals("DRAFT", vo.status());
    }

    @Test
    void createRejectsWhenNeitherPlatformNorByokModelSelected() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        doAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(11L);
            return null;
        }).when(agentRepository).save(any(Agent.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(new CreateAgentRequest("Helper", null, null, null, null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void streamChatRecordsSuccessWhenStreamCompletes() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setWorkspaceId(7L);
        AgentVersion draft = new AgentVersion();
        draft.setId(8L);
        draft.setAgentId(21L);
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.of(draft));
        when(agentChatPreparer.retrieveKnowledge(draft, "hello")).thenReturn(KnowledgeRetrievalResult.empty());
        PreparedAgentChat prepared = new PreparedAgentChat(
                21L, null, List.of(), null, null, null, null, false, 8L, 8L, List.of(), List.of(), null);
        when(agentChatPreparer.prepare(eq(21L), any(), eq("hello"), isNull(), any())).thenReturn(prepared);
        Execution execution = new Execution();
        execution.setId(90L);
        when(executionRecorder.startAgentExecution(eq(21L), eq(8L), isNull(), any())).thenReturn(execution);
        doAnswer(invocation -> {
            Consumer<String> onCompleted = invocation.getArgument(1);
            onCompleted.accept("streamed");
            return new SseEmitter(1L);
        }).when(agentChatExecutor).stream(eq(prepared), any(), eq(90L), any(), eq(execution));

        service.streamChat(21L, new AgentChatRequest("hello", true, null, null), mock(HttpServletResponse.class));

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        verify(agentChatExecutor).assertQuotaAvailable();
        verify(executionRecorder).succeed(eq(execution), any(), eq(8));
        verify(longTermMemoryApplicationService).captureFromTurn(draft, 21L, 7L, 3L, "hello", "streamed");
    }
}
