package com.boxai.conversation.application;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.conversation.api.AdminMessageFeedbackReplyRequest;
import com.boxai.domain.conversation.Message;
import com.boxai.domain.conversation.MessageFeedback;
import com.boxai.domain.conversation.MessageFeedbackRepository;
import com.boxai.domain.conversation.MessageRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.security.notification.NotificationPublisher;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMessageFeedbackApplicationServiceTest {

    @Mock
    private MessageFeedbackRepository messageFeedbackRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private AdminMessageFeedbackApplicationService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void replyRejectsMissingFeedback() {
        when(messageFeedbackRepository.findById(8L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reply(8L, new AdminMessageFeedbackReplyRequest("thanks")));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void replyRejectsGoodRating() {
        MessageFeedback feedback = feedback("good", "PENDING");
        when(messageFeedbackRepository.findById(8L)).thenReturn(Optional.of(feedback));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reply(8L, new AdminMessageFeedbackReplyRequest("thanks")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(messageFeedbackRepository, never()).updateReply(any());
    }

    @Test
    void replyNotifiesUserAndMarksReplied() {
        authenticate();
        MessageFeedback feedback = feedback("bad", "PENDING");
        when(messageFeedbackRepository.findById(8L)).thenReturn(Optional.of(feedback));

        service.reply(8L, new AdminMessageFeedbackReplyRequest("  已处理  "));

        ArgumentCaptor<MessageFeedback> captor = ArgumentCaptor.forClass(MessageFeedback.class);
        verify(messageFeedbackRepository).updateReply(captor.capture());
        assertEquals("REPLIED", captor.getValue().getStatus());
        assertEquals("已处理", captor.getValue().getAdminReply());
        assertEquals(1L, captor.getValue().getAdminReplyBy());
        verify(notificationPublisher).publish(3L, 7L, "反馈已回复", "已处理", "FEEDBACK", "/chat/11");
    }

    @Test
    void detailIncludesMessageContent() {
        MessageFeedback feedback = feedback("bad", "PENDING");
        when(messageFeedbackRepository.findById(8L)).thenReturn(Optional.of(feedback));
        User user = new User();
        user.setEmail("u@example.com");
        user.setNickname("Ada");
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        Message message = new Message();
        message.setContent("hello world");
        when(messageRepository.findById(22L)).thenReturn(Optional.of(message));

        var vo = service.detail(8L);

        assertEquals("hello world", vo.messageContent());
        assertEquals("Ada", vo.userNickname());
        assertEquals("u@example.com", vo.userEmail());
    }

    private void authenticate() {
        LoginUser user = new LoginUser(1L, "admin", UserTypes.PLATFORM_ADMIN, "OPS");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static MessageFeedback feedback(String rating, String status) {
        MessageFeedback feedback = new MessageFeedback();
        feedback.setId(8L);
        feedback.setWorkspaceId(7L);
        feedback.setConversationId(11L);
        feedback.setMessageId(22L);
        feedback.setUserId(3L);
        feedback.setRating(rating);
        feedback.setStatus(status);
        return feedback;
    }
}
