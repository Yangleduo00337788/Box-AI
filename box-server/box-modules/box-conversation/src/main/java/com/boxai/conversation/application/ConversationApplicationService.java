package com.boxai.conversation.application;

import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.agent.chat.PreparedAgentChat;
import com.boxai.ai.ChatTurn;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.conversation.api.ConversationVO;
import com.boxai.conversation.api.CreateConversationRequest;
import com.boxai.conversation.api.MessageVO;
import com.boxai.conversation.api.SendMessageRequest;
import com.boxai.conversation.api.SendMessageVO;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.conversation.Conversation;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.domain.conversation.Message;
import com.boxai.domain.conversation.MessageRepository;
import com.boxai.security.context.WorkspaceContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConversationApplicationService {

    private static final int MAX_HISTORY_MESSAGES = 20;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AgentRepository agentRepository;
    private final AgentChatPreparer agentChatPreparer;
    private final AgentChatExecutor agentChatExecutor;
    private final TransactionTemplate transactionTemplate;

    public ConversationApplicationService(ConversationRepository conversationRepository,
                                          MessageRepository messageRepository,
                                          AgentRepository agentRepository,
                                          AgentChatPreparer agentChatPreparer,
                                          AgentChatExecutor agentChatExecutor,
                                          PlatformTransactionManager transactionManager) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.agentRepository = agentRepository;
        this.agentChatPreparer = agentChatPreparer;
        this.agentChatExecutor = agentChatExecutor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public ConversationVO create(CreateConversationRequest request) {
        Agent agent = requireAgent(request.agentId());
        Long userId = WorkspaceContext.require().userId();
        Conversation conversation = new Conversation();
        conversation.setWorkspaceId(workspaceId());
        conversation.setAgentId(agent.getId());
        conversation.setUserId(userId);
        conversation.setTitle(trimToNull(request.title()));
        conversation.setStatus("ACTIVE");
        conversation.setMessageCount(0);
        conversationRepository.save(conversation);
        return toVO(conversation, agent.getName());
    }

    public List<ConversationVO> list() {
        Long userId = WorkspaceContext.require().userId();
        List<Conversation> conversations = conversationRepository.listByWorkspaceAndUser(workspaceId(), userId);
        Map<Long, Agent> agentMap = agentRepository.listByWorkspace(workspaceId()).stream()
                .collect(Collectors.toMap(Agent::getId, Function.identity()));
        return conversations.stream()
                .map(item -> toVO(item, agentName(agentMap.get(item.getAgentId()))))
                .toList();
    }

    public ConversationVO detail(Long id) {
        Conversation conversation = requireConversation(id);
        Agent agent = requireAgent(conversation.getAgentId());
        return toVO(conversation, agent.getName());
    }

    public List<MessageVO> listMessages(Long id) {
        Conversation conversation = requireConversation(id);
        return messageRepository.listByConversationId(conversation.getId()).stream().map(this::toMessageVO).toList();
    }

    @Transactional
    public SendMessageVO sendMessage(Long id, SendMessageRequest request) {
        Conversation conversation = requireConversation(id);
        String content = request.message().trim();
        PreparedAgentChat prepared = buildPreparedChat(conversation, content);
        agentChatExecutor.assertQuotaAvailable();
        Message userMessage = appendMessage(conversation, "USER", content, null);
        String assistantContent = agentChatExecutor.chat(prepared);
        Message assistantMessage = appendMessage(conversation, "ASSISTANT", assistantContent, prepared.modelId());
        maybeUpdateTitle(conversation, content);
        updateConversationMeta(conversation, 2);
        return new SendMessageVO(toMessageVO(userMessage), toMessageVO(assistantMessage));
    }

    @Transactional
    public SseEmitter streamMessage(Long id, SendMessageRequest request, HttpServletResponse response) {
        Conversation conversation = requireConversation(id);
        String content = request.message().trim();
        PreparedAgentChat prepared = buildPreparedChat(conversation, content);
        agentChatExecutor.assertQuotaAvailable();
        configureSseResponse(response);
        appendMessage(conversation, "USER", content, null);
        maybeUpdateTitle(conversation, content);
        updateConversationMeta(conversation, 1);
        Long conversationId = conversation.getId();
        Long modelId = prepared.modelId();
        return agentChatExecutor.stream(prepared, assistantContent -> transactionTemplate.executeWithoutResult(status -> {
            Conversation fresh = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在"));
            appendMessage(fresh, "ASSISTANT", assistantContent, modelId);
            updateConversationMeta(fresh, 1);
        }));
    }

    private PreparedAgentChat buildPreparedChat(Conversation conversation, String userMessage) {
        return agentChatPreparer.prepare(conversation.getAgentId(), historyTurns(conversation), userMessage);
    }

    private List<ChatTurn> historyTurns(Conversation conversation) {
        List<Message> history = messageRepository.listByConversationId(conversation.getId());
        List<ChatTurn> turns = new ArrayList<>();
        int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        for (int i = start; i < history.size(); i++) {
            Message item = history.get(i);
            if ("USER".equals(item.getRole()) || "ASSISTANT".equals(item.getRole())) {
                turns.add(new ChatTurn(item.getRole(), item.getContent()));
            }
        }
        return turns;
    }

    private Message appendMessage(Conversation conversation, String role, String content, Long modelId) {
        int sequenceNo = messageRepository.findMaxSequenceNo(conversation.getId()).orElse(0) + 1;
        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setWorkspaceId(conversation.getWorkspaceId());
        message.setRole(role);
        message.setContent(content);
        message.setContentType("TEXT");
        message.setSequenceNo(sequenceNo);
        message.setModelId(modelId);
        return messageRepository.save(message);
    }

    private void maybeUpdateTitle(Conversation conversation, String firstUserMessage) {
        if (conversation.getTitle() != null && !conversation.getTitle().isBlank()) {
            return;
        }
        String title = firstUserMessage.length() > 30 ? firstUserMessage.substring(0, 30) + "…" : firstUserMessage;
        conversation.setTitle(title);
        conversationRepository.update(conversation);
    }

    private void updateConversationMeta(Conversation conversation, int addedCount) {
        conversation.setMessageCount((conversation.getMessageCount() == null ? 0 : conversation.getMessageCount()) + addedCount);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.update(conversation);
    }

    private Conversation requireConversation(Long id) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在"));
        if (!workspaceId().equals(conversation.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该会话");
        }
        if (!WorkspaceContext.require().userId().equals(conversation.getUserId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该会话");
        }
        return conversation;
    }

    private Agent requireAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在"));
        if (!workspaceId().equals(agent.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该智能体");
        }
        return agent;
    }

    private ConversationVO toVO(Conversation conversation, String agentName) {
        return new ConversationVO(
                conversation.getId(),
                conversation.getAgentId(),
                agentName,
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getMessageCount(),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }

    private MessageVO toMessageVO(Message message) {
        return new MessageVO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getContentType(),
                message.getSequenceNo(),
                message.getCreatedAt());
    }

    private String agentName(Agent agent) {
        return agent == null ? null : agent.getName();
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void configureSseResponse(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
