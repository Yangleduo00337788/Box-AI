package com.boxai.publish.application;

import com.boxai.agent.api.AgentChatRequest;
import com.boxai.agent.api.AgentEmbedConfigVO;
import com.boxai.agent.application.AgentLongTermMemoryApplicationService;
import com.boxai.agent.application.EmbedDomainApplicationService;
import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.agent.chat.PreparedAgentChat;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.publish.Publish;
import com.boxai.domain.publish.PublishRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.knowledge.application.KnowledgeRetrievalResult;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.trace.application.ExecutionRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.boxai.agent.application.AgentLongTermMemoryApplicationService;
import com.boxai.agent.application.EmbedDomainApplicationService;
import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.publish.Publish;
import com.boxai.domain.publish.PublishRepository;
import com.boxai.trace.application.ExecutionRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishedAgentApplicationServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentVersionRepository agentVersionRepository;
    @Mock
    private PublishRepository publishRepository;
    @Mock
    private AgentChatPreparer agentChatPreparer;
    @Mock
    private AgentChatExecutor agentChatExecutor;
    @Mock
    private ExecutionRecorder executionRecorder;
    @Mock
    private AgentLongTermMemoryApplicationService longTermMemoryApplicationService;
    @Mock
    private EmbedDomainApplicationService embedDomainApplicationService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PublishedAgentApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void getEmbedConfigRejectsUnpublishedAgent() {
        Agent agent = new Agent();
        agent.setId(3L);
        when(agentRepository.findById(3L)).thenReturn(Optional.of(agent));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getEmbedConfig(3L));
        assertEquals(ErrorCode.AGENT_NOT_PUBLISHED, ex.getCode());
    }

    @Test
    void getEmbedConfigReturnsPublishedEmbed() {
        Agent agent = new Agent();
        agent.setId(3L);
        agent.setName("Helper");
        agent.setPublishedVersionId(8L);
        AgentVersion version = new AgentVersion();
        version.setId(8L);
        version.setConfigJson("{}");
        when(agentRepository.findById(3L)).thenReturn(Optional.of(agent));
        when(publishRepository.findLatestActive(any(), eq(3L))).thenReturn(Optional.of(new Publish()));
        when(agentVersionRepository.findById(8L)).thenReturn(Optional.of(version));
        AgentEmbedConfigVO embed = new AgentEmbedConfigVO(
                "#0052d9", "", "", List.of(), "Helper", "", false, null, true, null, "platform", null);
        when(embedDomainApplicationService.attach(any(), eq(3L), anyBoolean())).thenReturn(embed);

        assertEquals("Helper", service.getEmbedConfig(3L).agentName());
    }

    @Test
    void resolveByHostRequiresVerifiedDomain() {
        when(embedDomainApplicationService.findVerifiedByHost("unknown.example.com")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resolveByHost("unknown.example.com"));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void streamChatRecordsSuccessWhenStreamCompletes() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = new Agent();
        agent.setId(3L);
        agent.setWorkspaceId(7L);
        agent.setPublishedVersionId(8L);
        AgentVersion version = new AgentVersion();
        version.setId(8L);
        when(agentRepository.findById(3L)).thenReturn(Optional.of(agent));
        when(publishRepository.findLatestActive(any(), eq(3L))).thenReturn(Optional.of(new Publish()));
        when(agentVersionRepository.findById(8L)).thenReturn(Optional.of(version));
        when(agentChatPreparer.retrieveKnowledge(version, "hello")).thenReturn(KnowledgeRetrievalResult.empty());
        PreparedAgentChat prepared = new PreparedAgentChat(
                3L, null, List.of(), null, null, null, null, false, 8L, 8L, List.of(), List.of(), null);
        when(agentChatPreparer.preparePublished(eq(3L), any(), eq("hello"), any())).thenReturn(prepared);
        Execution execution = new Execution();
        execution.setId(90L);
        when(executionRecorder.startAgentExecution(eq(3L), eq(8L), isNull(), any())).thenReturn(execution);
        doAnswer(invocation -> {
            Consumer<String> onCompleted = invocation.getArgument(1);
            onCompleted.accept("streamed");
            return new SseEmitter(1L);
        }).when(agentChatExecutor).stream(eq(prepared), any(), eq(90L), any(), eq(execution));

        service.streamChat(3L, new AgentChatRequest("hello", true, null, null), mock(HttpServletResponse.class));

        verify(agentChatExecutor).assertQuotaAvailable();
        verify(agentChatExecutor).assertPublishedChatRateLimit(3L);
        verify(executionRecorder).succeed(eq(execution), any(), eq(2));
        verify(longTermMemoryApplicationService).captureFromTurn(version, 3L, 7L, 3L, "hello", "streamed");
    }
}
