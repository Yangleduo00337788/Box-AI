package com.boxai.conversation.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.conversation.api.ConversationShareCreatedVO;
import com.boxai.conversation.api.CreateConversationShareRequest;
import com.boxai.conversation.api.MessageFeedbackRequest;
import com.boxai.conversation.api.SharedConversationVO;
import com.boxai.conversation.api.SharedMessageVO;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationShareApplicationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationShareRepository conversationShareRepository;
    private final MessageFeedbackRepository messageFeedbackRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ConversationShareApplicationService(ConversationRepository conversationRepository,
                                               MessageRepository messageRepository,
                                               ConversationShareRepository conversationShareRepository,
                                               MessageFeedbackRepository messageFeedbackRepository,
                                               AgentRepository agentRepository,
                                               UserRepository userRepository,
                                               ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationShareRepository = conversationShareRepository;
        this.messageFeedbackRepository = messageFeedbackRepository;
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConversationShareCreatedVO createShare(Long conversationId, CreateConversationShareRequest request) {
        WorkspaceContext ctx = WorkspaceContext.require();
        Conversation conversation = requireConversation(conversationId, ctx);
        List<Message> allMessages = messageRepository.listByConversationId(conversationId);
        Set<Long> allowedIds = allMessages.stream().map(Message::getId).collect(Collectors.toSet());
        Map<Long, Message> byId = allMessages.stream()
                .collect(Collectors.toMap(Message::getId, m -> m, (a, b) -> a, LinkedHashMap::new));
        List<Message> selected = request.messageIds().stream()
                .distinct()
                .filter(allowedIds::contains)
                .map(byId::get)
                .filter(m -> m != null)
                .sorted((a, b) -> Integer.compare(a.getSequenceNo(), b.getSequenceNo()))
                .toList();
        if (selected.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要分享的消息");
        }

        String userName = userRepository.findById(ctx.userId())
                .map(this::displayUserName)
                .orElse("用户");
        String agentName = agentRepository.findById(conversation.getAgentId())
                .map(Agent::getName)
                .orElse("Box AI");

        List<SharedMessageVO> sharedMessages = selected.stream()
                .map(m -> new SharedMessageVO(m.getRole(), m.getContent()))
                .toList();

        String title = request.title() != null && !request.title().isBlank()
                ? request.title().trim()
                : conversation.getTitle() != null ? conversation.getTitle() : "分享的对话";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userName", userName);
        payload.put("agentName", agentName);
        payload.put("messages", sharedMessages);

        String payloadJson = writeJson(payload);
        String token = UUID.randomUUID().toString().replace("-", "");

        ConversationShare share = new ConversationShare();
        share.setToken(token);
        share.setWorkspaceId(conversation.getWorkspaceId());
        share.setConversationId(conversationId);
        share.setTitle(title);
        share.setPayloadJson(payloadJson);
        share.setCreatedBy(ctx.userId());
        conversationShareRepository.save(share);

        return new ConversationShareCreatedVO(token, "/share/" + token);
    }

    public SharedConversationVO getPublicShare(String token) {
        ConversationShare share = conversationShareRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分享不存在或已失效"));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(share.getPayloadJson(), Map.class);
            String userName = String.valueOf(payload.getOrDefault("userName", "用户"));
            String agentName = String.valueOf(payload.getOrDefault("agentName", "Box AI"));
            List<SharedMessageVO> messages = objectMapper.convertValue(
                    payload.get("messages"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SharedMessageVO.class));
            return new SharedConversationVO(share.getTitle(), share.getCreatedAt(), userName, agentName, messages);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分享数据解析失败");
        }
    }

    @Transactional
    public void submitFeedback(Long conversationId, Long messageId, MessageFeedbackRequest request) {
        WorkspaceContext ctx = WorkspaceContext.require();
        requireConversation(conversationId, ctx);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "消息不存在"));
        if (!message.getConversationId().equals(conversationId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息不属于当前会话");
        }
        MessageFeedback feedback = new MessageFeedback();
        feedback.setWorkspaceId(ctx.workspaceId());
        feedback.setConversationId(conversationId);
        feedback.setMessageId(messageId);
        feedback.setUserId(ctx.userId());
        feedback.setRating(request.rating());
        feedback.setContent(request.content());
        if ("bad".equalsIgnoreCase(request.rating())) {
            feedback.setStatus("PENDING");
        } else if ("good".equalsIgnoreCase(request.rating())) {
            feedback.setStatus("RECORDED");
        }
        messageFeedbackRepository.save(feedback);
    }

    private Conversation requireConversation(Long id, WorkspaceContext ctx) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
        if (!conversation.getWorkspaceId().equals(ctx.workspaceId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        if (!conversation.getUserId().equals(ctx.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return conversation;
    }

    private String displayUserName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail() != null ? user.getEmail() : "用户";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分享数据序列化失败");
        }
    }
}
