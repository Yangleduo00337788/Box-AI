package com.boxai.conversation.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.conversation.api.CreateConversationShareRequest;
import com.boxai.conversation.api.MessageFeedbackRequest;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.conversation.Conversation;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.domain.conversation.ConversationShare;
import com.boxai.domain.conversation.ConversationShareRepository;
import com.boxai.domain.conversation.Message;
import com.boxai.domain.conversation.MessageFeedback;
import com.boxai.domain.conversation.MessageFeedbackRepository;
import com.boxai.domain.conversation.MessageRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.security.context.WorkspaceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationShareApplicationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationShareRepository conversationShareRepository;
    @Mock
    private MessageFeedbackRepository messageFeedbackRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private UserRepository userRepository;

    private ConversationShareApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationShareApplicationService(
                conversationRepository,
                messageRepository,
                conversationShareRepository,
                messageFeedbackRepository,
                agentRepository,
                userRepository,
                new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createShareKeepsSelectedMessagesInSequenceAndIgnoresUnknownIds() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Conversation conversation = conversation(11L, 7L, 3L, 21L, "Daily");
        when(conversationRepository.findById(11L)).thenReturn(Optional.of(conversation));
        when(messageRepository.listByConversationId(11L)).thenReturn(List.of(
                message(2L, 11L, "assistant", "b", 2),
                message(1L, 11L, "user", "a", 1)));
        User user = new User();
        user.setNickname("Ada");
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        Agent agent = new Agent();
        agent.setName("Helper");
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));

        var vo = service.createShare(11L, new CreateConversationShareRequest("  公开  ", List.of(2L, 99L, 1L)));

        ArgumentCaptor<ConversationShare> captor = ArgumentCaptor.forClass(ConversationShare.class);
        verify(conversationShareRepository).save(captor.capture());
        ConversationShare saved = captor.getValue();
        assertEquals("公开", saved.getTitle());
        assertEquals(11L, saved.getConversationId());
        assertTrue(saved.getPayloadJson().contains("\"role\":\"user\""));
        assertTrue(saved.getPayloadJson().indexOf("\"content\":\"a\"")
                < saved.getPayloadJson().indexOf("\"content\":\"b\""));
        assertEquals(saved.getToken(), vo.token());
        assertEquals("/share/" + saved.getToken(), vo.sharePath());
    }

    @Test
    void createShareRejectsWhenNoOwnedMessagesSelected() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(conversationRepository.findById(11L)).thenReturn(Optional.of(conversation(11L, 7L, 3L, 21L, "Daily")));
        when(messageRepository.listByConversationId(11L)).thenReturn(List.of(message(1L, 11L, "user", "a", 1)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createShare(11L, new CreateConversationShareRequest(null, List.of(9L))));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void createShareRejectsOtherWorkspaceConversation() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(conversationRepository.findById(11L)).thenReturn(Optional.of(conversation(11L, 8L, 3L, 21L, "Daily")));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createShare(11L, new CreateConversationShareRequest(null, List.of(1L))));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void getPublicShareReturnsSnapshot() {
        ConversationShare share = new ConversationShare();
        share.setTitle("公开");
        share.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4));
        share.setPayloadJson("""
                {"userName":"Ada","agentName":"Helper","messages":[{"role":"user","content":"hi"}]}
                """);
        when(conversationShareRepository.findByToken("tok")).thenReturn(Optional.of(share));

        var vo = service.getPublicShare("tok");

        assertEquals("公开", vo.title());
        assertEquals("Ada", vo.userName());
        assertEquals("Helper", vo.agentName());
        assertEquals(1, vo.messages().size());
        assertEquals("hi", vo.messages().get(0).content());
    }

    @Test
    void getPublicShareRejectsUnknownToken() {
        when(conversationShareRepository.findByToken("missing")).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPublicShare("missing"));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void submitFeedbackMarksBadAsPending() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(conversationRepository.findById(11L)).thenReturn(Optional.of(conversation(11L, 7L, 3L, 21L, "Daily")));
        Message message = message(5L, 11L, "assistant", "ans", 2);
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message));

        service.submitFeedback(11L, 5L, new MessageFeedbackRequest("bad", "wrong"));

        ArgumentCaptor<MessageFeedback> captor = ArgumentCaptor.forClass(MessageFeedback.class);
        verify(messageFeedbackRepository).save(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
        assertEquals("wrong", captor.getValue().getContent());
    }

    private static Conversation conversation(Long id, Long workspaceId, Long userId, Long agentId, String title) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setWorkspaceId(workspaceId);
        conversation.setUserId(userId);
        conversation.setAgentId(agentId);
        conversation.setTitle(title);
        return conversation;
    }

    private static Message message(Long id, Long conversationId, String role, String content, int sequence) {
        Message message = new Message();
        message.setId(id);
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setSequenceNo(sequence);
        return message;
    }
}
