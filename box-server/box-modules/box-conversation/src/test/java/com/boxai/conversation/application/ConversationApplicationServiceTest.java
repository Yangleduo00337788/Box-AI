package com.boxai.conversation.application;

import com.boxai.agent.application.AgentLongTermMemoryApplicationService;
import com.boxai.agent.application.ConversationPluginApplicationService;
import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.agent.chat.ConversationPluginRound;
import com.boxai.agent.chat.PreparedAgentChat;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.conversation.api.CreateConversationRequest;
import com.boxai.conversation.api.SendMessageRequest;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.conversation.Conversation;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.domain.conversation.Message;
import com.boxai.domain.conversation.MessageRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.trace.application.ExecutionRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationApplicationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentVersionRepository agentVersionRepository;
    @Mock
    private AgentChatPreparer agentChatPreparer;
    @Mock
    private AgentChatExecutor agentChatExecutor;
    @Mock
    private ExecutionRecorder executionRecorder;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private AgentLongTermMemoryApplicationService longTermMemoryApplicationService;
    @Mock
    private ChatProjectApplicationService chatProjectApplicationService;
    @Mock
    private MessagePluginContextService messagePluginContextService;
    @Mock
    private ConversationPluginApplicationService conversationPluginApplicationService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ConversationApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createPersistsConversationForAgentInWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setWorkspaceId(7L);
        agent.setName("Helper");
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));

        var vo = service.create(new CreateConversationRequest(21L, "  Hello  ", null));

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        Conversation saved = captor.getValue();
        assertEquals(7L, saved.getWorkspaceId());
        assertEquals(3L, saved.getUserId());
        assertEquals(21L, saved.getAgentId());
        assertEquals("Hello", saved.getTitle());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals("Helper", vo.agentName());
    }

    @Test
    void createRejectsAgentFromAnotherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setWorkspaceId(99L);
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(new CreateConversationRequest(21L, "Hello", null)));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
    }

    @Test
    void sendMessageRejectsBlankContentWithoutPlugins() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(ownedConversation()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendMessage(5L, new SendMessageRequest("  ", false, null, null, null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void sendMessageRejectsConversationOwnedBySomeoneElse() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Conversation conversation = ownedConversation();
        conversation.setUserId(99L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendMessage(5L, new SendMessageRequest("hello", false, null, null, null)));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
    }

    @Test
    void sendMessagePersistsUserAndAssistantTurns() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Conversation conversation = ownedConversation();
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));
        when(messagePluginContextService.enrich(7L, "hello", null))
                .thenReturn(new MessagePluginContextService.EnrichedMessage("hello", null));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.empty());
        Execution execution = new Execution();
        execution.setId(90L);
        when(executionRecorder.startAgentExecution(eq(21L), isNull(), eq(5L), any())).thenReturn(execution);
        when(conversationPluginApplicationService.resolve(7L, null)).thenReturn(ConversationPluginRound.empty());
        PreparedAgentChat prepared = preparedChat();
        when(agentChatPreparer.prepare(eq(21L), any(), eq("hello"), isNull(), any(), any())).thenReturn(prepared);
        when(messageRepository.findMaxSequenceNo(5L)).thenReturn(Optional.of(0), Optional.of(1));
        when(messageRepository.listByConversationId(5L)).thenReturn(List.of());
        doAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId("USER".equals(message.getRole()) ? 11L : 12L);
            return message;
        }).when(messageRepository).save(any(Message.class));
        when(agentChatExecutor.chat(prepared, execution)).thenReturn("world");

        var vo = service.sendMessage(5L, new SendMessageRequest("hello", false, null, null, null));

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        verify(agentChatExecutor).assertQuotaAvailable();
        verify(executionRecorder).succeed(eq(execution), any(), eq(1));
        assertEquals("USER", vo.userMessage().role());
        assertEquals("hello", vo.userMessage().content());
        assertEquals("ASSISTANT", vo.assistantMessage().role());
        assertEquals("world", vo.assistantMessage().content());
        assertEquals(2, conversation.getMessageCount());
    }

    @Test
    void sendMessageRecordsFailureWhenChatThrows() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(ownedConversation()));
        when(messagePluginContextService.enrich(7L, "hello", null))
                .thenReturn(new MessagePluginContextService.EnrichedMessage("hello", null));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.empty());
        Execution execution = new Execution();
        execution.setId(90L);
        when(executionRecorder.startAgentExecution(eq(21L), isNull(), eq(5L), any())).thenReturn(execution);
        when(conversationPluginApplicationService.resolve(7L, null)).thenReturn(ConversationPluginRound.empty());
        PreparedAgentChat prepared = preparedChat();
        when(agentChatPreparer.prepare(eq(21L), any(), eq("hello"), isNull(), any(), any())).thenReturn(prepared);
        when(messageRepository.findMaxSequenceNo(5L)).thenReturn(Optional.of(0));
        when(messageRepository.listByConversationId(5L)).thenReturn(List.of());
        doAnswer(invocation -> invocation.getArgument(0)).when(messageRepository).save(any(Message.class));
        doThrow(new RuntimeException("model down")).when(agentChatExecutor).chat(prepared, execution);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.sendMessage(5L, new SendMessageRequest("hello", false, null, null, null)));
        assertEquals("model down", ex.getMessage());
        verify(executionRecorder).fail(execution, "model down");
    }

    @Test
    void regenerateMessageRejectsEmptyHistory() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(ownedConversation()));
        when(messageRepository.listByConversationId(5L)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.regenerateMessage(5L, null, null));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void streamMessagePersistsAssistantWhenStreamCompletes() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Conversation conversation = ownedConversation();
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));
        when(messagePluginContextService.enrich(7L, "hello", null))
                .thenReturn(new MessagePluginContextService.EnrichedMessage("hello", null));
        when(agentVersionRepository.findLatestDraft(21L)).thenReturn(Optional.empty());
        when(conversationPluginApplicationService.resolve(7L, null)).thenReturn(ConversationPluginRound.empty());
        PreparedAgentChat prepared = preparedChat();
        when(agentChatPreparer.prepare(eq(21L), any(), eq("hello"), isNull(), any(), any())).thenReturn(prepared);
        Execution execution = new Execution();
        execution.setId(90L);
        when(executionRecorder.startAgentExecution(eq(21L), eq(2L), eq(5L), any())).thenReturn(execution);
        when(messageRepository.findMaxSequenceNo(5L)).thenReturn(Optional.of(0), Optional.of(1));
        when(messageRepository.listByConversationId(5L)).thenReturn(List.of());
        doAnswer(invocation -> invocation.getArgument(0)).when(messageRepository).save(any(Message.class));
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        doAnswer(invocation -> {
            Consumer<String> onCompleted = invocation.getArgument(1);
            onCompleted.accept("streamed");
            return new SseEmitter(1L);
        }).when(agentChatExecutor).stream(eq(prepared), any(), eq(90L), isNull(), eq(execution));

        service.streamMessage(5L, new SendMessageRequest("hello", true, null, null, null), mock(HttpServletResponse.class));

        verify(agentChatExecutor).assertQuotaAvailable();
        verify(executionRecorder).succeed(eq(execution), any(), eq(2));
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals("USER", captor.getAllValues().get(0).getRole());
        assertEquals("ASSISTANT", captor.getAllValues().get(1).getRole());
        assertEquals("streamed", captor.getAllValues().get(1).getContent());
        assertEquals(2, conversation.getMessageCount());
    }

    private static Conversation ownedConversation() {
        Conversation conversation = new Conversation();
        conversation.setId(5L);
        conversation.setWorkspaceId(7L);
        conversation.setUserId(3L);
        conversation.setAgentId(21L);
        conversation.setMessageCount(0);
        conversation.setStatus("ACTIVE");
        return conversation;
    }

    private static PreparedAgentChat preparedChat() {
        return new PreparedAgentChat(
                21L, null, List.of(), null, null, null, null, false, 8L, 2L, List.of(), List.of(), null);
    }
}
