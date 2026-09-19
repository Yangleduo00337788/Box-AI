package com.boxai.conversation.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.result.PageResult;
import com.boxai.conversation.api.AdminMessageFeedbackDetailVO;
import com.boxai.conversation.api.AdminMessageFeedbackReplyRequest;
import com.boxai.conversation.api.AdminMessageFeedbackVO;
import com.boxai.domain.conversation.Message;
import com.boxai.domain.conversation.MessageFeedback;
import com.boxai.domain.conversation.MessageFeedbackQuery;
import com.boxai.domain.conversation.MessageFeedbackRepository;
import com.boxai.domain.conversation.MessageRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.security.context.SecurityContexts;
import com.boxai.security.notification.NotificationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminMessageFeedbackApplicationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_REPLIED = "REPLIED";
    private static final String CATEGORY_FEEDBACK = "FEEDBACK";

    private final MessageFeedbackRepository messageFeedbackRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;

    public AdminMessageFeedbackApplicationService(MessageFeedbackRepository messageFeedbackRepository,
                                                  MessageRepository messageRepository,
                                                  UserRepository userRepository,
                                                  NotificationPublisher notificationPublisher) {
        this.messageFeedbackRepository = messageFeedbackRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationPublisher = notificationPublisher;
    }

    public PageResult<AdminMessageFeedbackVO> page(String rating, String status, int page, int pageSize) {
        MessageFeedbackQuery query = new MessageFeedbackQuery();
        query.setRating(trimToNull(rating));
        query.setStatus(trimToNull(status));
        query.setPage(page);
        query.setPageSize(pageSize);
        PageResult<MessageFeedback> result = messageFeedbackRepository.page(query);
        Set<Long> userIds = result.records().stream()
                .map(MessageFeedback::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return new PageResult<>(
                result.records().stream().map(item -> toVO(item, users.get(item.getUserId()))).toList(),
                result.total(),
                result.page(),
                result.pageSize());
    }

    public AdminMessageFeedbackDetailVO detail(Long id) {
        MessageFeedback feedback = messageFeedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "反馈不存在"));
        User user = userRepository.findById(feedback.getUserId()).orElse(null);
        return toDetailVO(feedback, user);
    }

    @Transactional
    public void reply(Long id, AdminMessageFeedbackReplyRequest request) {
        MessageFeedback feedback = messageFeedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "反馈不存在"));
        if (!"bad".equalsIgnoreCase(feedback.getRating())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持回复点踩反馈");
        }
        boolean updating = STATUS_REPLIED.equalsIgnoreCase(feedback.getStatus());
        String reply = request.reply().trim();
        Long adminId = SecurityContexts.currentUser().userId();
        feedback.setStatus(STATUS_REPLIED);
        feedback.setAdminReply(reply);
        feedback.setAdminReplyBy(adminId);
        feedback.setAdminRepliedAt(LocalDateTime.now());
        messageFeedbackRepository.updateReply(feedback);

        String linkUrl = "/chat/" + feedback.getConversationId();
        String title = updating ? "反馈回复已更新" : "反馈已回复";
        notificationPublisher.publish(
                feedback.getUserId(),
                feedback.getWorkspaceId(),
                title,
                reply,
                CATEGORY_FEEDBACK,
                linkUrl);
    }

    private AdminMessageFeedbackDetailVO toDetailVO(MessageFeedback feedback, User user) {
        String messageContent = messageRepository.findById(feedback.getMessageId())
                .map(Message::getContent)
                .orElse("");
        return new AdminMessageFeedbackDetailVO(
                feedback.getId(),
                feedback.getWorkspaceId(),
                feedback.getConversationId(),
                feedback.getMessageId(),
                feedback.getUserId(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getNickname(),
                feedback.getRating(),
                feedback.getContent(),
                feedback.getStatus(),
                messageContent,
                feedback.getAdminReply(),
                feedback.getAdminReplyBy(),
                feedback.getAdminRepliedAt(),
                feedback.getCreatedAt());
    }

    private AdminMessageFeedbackVO toVO(MessageFeedback feedback, User user) {
        String excerpt = messageRepository.findById(feedback.getMessageId())
                .map(Message::getContent)
                .map(this::excerpt)
                .orElse("");
        return new AdminMessageFeedbackVO(
                feedback.getId(),
                feedback.getWorkspaceId(),
                feedback.getConversationId(),
                feedback.getMessageId(),
                feedback.getUserId(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getNickname(),
                feedback.getRating(),
                feedback.getContent(),
                feedback.getStatus(),
                excerpt,
                feedback.getAdminReply(),
                feedback.getAdminReplyBy(),
                feedback.getAdminRepliedAt(),
                feedback.getCreatedAt());
    }

    private String excerpt(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 200) {
            return normalized;
        }
        return normalized.substring(0, 200) + "…";
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
