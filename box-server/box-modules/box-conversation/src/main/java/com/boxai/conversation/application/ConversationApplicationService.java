package com.boxai.conversation.application;

import com.boxai.agent.application.AgentLongTermMemoryApplicationService;
import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.agent.chat.PreparedAgentChat;
import com.boxai.ai.ChatTurn;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.conversation.api.ConversationVO;
import com.boxai.conversation.api.CreateConversationRequest;
import com.boxai.conversation.api.RenameConversationRequest;
import com.boxai.conversation.api.MessageVO;
import com.boxai.conversation.api.SendMessageRequest;
import com.boxai.conversation.api.SendMessageVO;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.conversation.Conversation;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.domain.conversation.Message;
import com.boxai.domain.conversation.MessageRepository;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.domain.trace.Execution;
import com.boxai.knowledge.application.KnowledgeRetrievalResult;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.knowledge.api.KnowledgeCitationVO;
import com.boxai.trace.application.ExecutionRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final int DEFAULT_HISTORY_MESSAGES = 20;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final AgentChatPreparer agentChatPreparer;
    private final AgentChatExecutor agentChatExecutor;
    private final ExecutionRecorder executionRecorder;
    private final WorkspacePermissionService workspacePermissionService;
    private final AgentLongTermMemoryApplicationService longTermMemoryApplicationService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public ConversationApplicationService(ConversationRepository conversationRepository,
                                          MessageRepository messageRepository,
                                          AgentRepository agentRepository,
                                          AgentVersionRepository agentVersionRepository,
                                          AgentChatPreparer agentChatPreparer,
                                          AgentChatExecutor agentChatExecutor,
                                          ExecutionRecorder executionRecorder,
                                          WorkspacePermissionService workspacePermissionService,
                                          AgentLongTermMemoryApplicationService longTermMemoryApplicationService,
                                          PlatformTransactionManager transactionManager,
                                          ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.agentChatPreparer = agentChatPreparer;
        this.agentChatExecutor = agentChatExecutor;
        this.executionRecorder = executionRecorder;
        this.workspacePermissionService = workspacePermissionService;
        this.longTermMemoryApplicationService = longTermMemoryApplicationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConversationVO create(CreateConversationRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
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
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Long userId = WorkspaceContext.require().userId();
        List<Conversation> conversations = conversationRepository.listByWorkspaceAndUser(workspaceId(), userId);
        Map<Long, Agent> agentMap = agentRepository.listByWorkspace(workspaceId()).stream()
                .collect(Collectors.toMap(Agent::getId, Function.identity()));
        return conversations.stream()
                .map(item -> toVO(item, agentName(agentMap.get(item.getAgentId()))))
                .toList();
    }

    public ConversationVO detail(Long id) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Conversation conversation = requireConversation(id);
        Agent agent = requireAgent(conversation.getAgentId());
        return toVO(conversation, agent.getName());
    }

    @Transactional
    public ConversationVO rename(Long id, RenameConversationRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Conversation conversation = requireConversation(id);
        conversation.setTitle(request.title().trim());
        conversationRepository.update(conversation);
        Agent agent = requireAgent(conversation.getAgentId());
        return toVO(conversation, agent.getName());
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Conversation conversation = requireConversation(id);
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation.getId());
    }

    public List<MessageVO> listMessages(Long id) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Conversation conversation = requireConversation(id);
        return messageRepository.listByConversationId(conversation.getId()).stream().map(this::toMessageVO).toList();
    }

    @Transactional
    public SendMessageVO sendMessage(Long id, SendMessageRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Conversation conversation = requireConversation(id);
        String content = request.message().trim();
        AgentVersion draft = agentVersionRepository.findLatestDraft(conversation.getAgentId()).orElse(null);
        Execution execution = executionRecorder.startAgentExecution(
                conversation.getAgentId(),
                draft == null ? null : draft.getId(),
                conversation.getId(),
                toInputJson(content));
        KnowledgeRetrievalResult retrieval = draft == null
                ? KnowledgeRetrievalResult.empty()
                : agentChatPreparer.retrieveKnowledge(draft, content);
        PreparedAgentChat prepared = buildPreparedChat(
                conversation,
                content,
                request.platformModelId(),
                request.toolConfirmationToken());
        executionRecorder.recordRagSpan(execution, Map.of("query", content), retrieval.citations());
        agentChatExecutor.assertQuotaAvailable();
        Message userMessage = appendMessage(conversation, "USER", content, null);
        try {
            String assistantContent = agentChatExecutor.chat(prepared, execution);
            executionRecorder.recordLlmSpan(execution, content, Map.of("content", assistantContent));
            Message assistantMessage = appendMessage(
                    conversation,
                    "ASSISTANT",
                    assistantContent,
                    prepared.modelId(),
                    citationsMetadata(retrieval.citations()));
            maybeUpdateTitle(conversation, content);
            updateConversationMeta(conversation, 2);
            executionRecorder.succeed(execution, toOutputJson(assistantContent), estimateTokens(assistantContent));
            captureLongTermMemory(draft, conversation, content, assistantContent);
            return new SendMessageVO(toMessageVO(userMessage), toMessageVO(assistantMessage));
        } catch (RuntimeException e) {
            executionRecorder.fail(execution, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public SseEmitter streamMessage(Long id, SendMessageRequest request, HttpServletResponse response) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Conversation conversation = requireConversation(id);
        String content = request.message().trim();
        AgentVersion draft = agentVersionRepository.findLatestDraft(conversation.getAgentId()).orElse(null);
        KnowledgeRetrievalResult retrieval = draft == null
                ? KnowledgeRetrievalResult.empty()
                : agentChatPreparer.retrieveKnowledge(draft, content);
        PreparedAgentChat prepared = buildPreparedChat(
                conversation,
                content,
                request.platformModelId(),
                request.toolConfirmationToken());
        agentChatExecutor.assertQuotaAvailable();
        configureSseResponse(response);
        appendMessage(conversation, "USER", content, null);
        maybeUpdateTitle(conversation, content);
        updateConversationMeta(conversation, 1);
        Long conversationId = conversation.getId();
        Long modelId = prepared.modelId();
        Execution execution = executionRecorder.startAgentExecution(
                conversation.getAgentId(),
                prepared.agentVersionId(),
                conversationId,
                toInputJson(content));
        executionRecorder.recordRagSpan(execution, Map.of("query", content), retrieval.citations());
        String citationsJson = citationsJson(retrieval.citations());
        return agentChatExecutor.stream(prepared, assistantContent -> transactionTemplate.executeWithoutResult(status -> {
            Conversation fresh = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在"));
            appendMessage(fresh, "ASSISTANT", assistantContent, modelId, citationsMetadata(retrieval.citations()));
            updateConversationMeta(fresh, 1);
            executionRecorder.recordLlmSpan(execution, content, Map.of("content", assistantContent));
            executionRecorder.succeed(execution, toOutputJson(assistantContent), estimateTokens(assistantContent));
            captureLongTermMemory(draft, fresh, content, assistantContent);
        }), execution.getId(), citationsJson, execution);
    }

    private PreparedAgentChat buildPreparedChat(Conversation conversation,
                                                String userMessage,
                                                Long platformModelId,
                                                String toolConfirmationToken) {
        PreparedAgentChat prepared = agentChatPreparer.prepare(
                conversation.getAgentId(),
                historyTurns(conversation, userMessage),
                userMessage,
                platformModelId);
        if (toolConfirmationToken == null || toolConfirmationToken.isBlank()) {
            return prepared;
        }
        return prepared.withToolConfirmationToken(toolConfirmationToken.trim());
    }

    private List<ChatTurn> historyTurns(Conversation conversation, String currentUserMessage) {
        AgentVersion draft = agentVersionRepository.findLatestDraft(conversation.getAgentId()).orElse(null);
        if (draft != null && Boolean.FALSE.equals(draft.getMemoryEnabled())) {
            return List.of();
        }
        int windowSize = draft == null || draft.getMemoryWindowSize() == null
                ? DEFAULT_HISTORY_MESSAGES
                : Math.max(0, draft.getMemoryWindowSize());
        if (windowSize == 0) {
            return List.of();
        }
        List<Message> history = messageRepository.listByConversationId(conversation.getId());
        List<ChatTurn> turns = new ArrayList<>();
        int end = history.size();
        if (end > 0) {
            Message last = history.get(end - 1);
            if ("USER".equals(last.getRole())
                    && currentUserMessage != null
                    && currentUserMessage.equals(last.getContent())) {
                end -= 1;
            }
        }
        int start = Math.max(0, end - windowSize);
        for (int i = start; i < end; i++) {
            Message item = history.get(i);
            if ("USER".equals(item.getRole()) || "ASSISTANT".equals(item.getRole())) {
                turns.add(new ChatTurn(item.getRole(), item.getContent()));
            }
        }
        return turns;
    }

    @Transactional
    public void deleteMessage(Long conversationId, Long messageId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Conversation conversation = requireConversation(conversationId);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "消息不存在"));
        if (!conversation.getId().equals(message.getConversationId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权删除该消息");
        }
        messageRepository.deleteById(messageId);
        int count = messageRepository.listByConversationId(conversationId).size();
        conversation.setMessageCount(count);
        conversationRepository.update(conversation);
    }

    public SseEmitter regenerateMessage(Long conversationId, Long platformModelId, HttpServletResponse response) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Conversation conversation = requireConversation(conversationId);
        List<Message> history = messageRepository.listByConversationId(conversationId);
        if (history.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "暂无可重新生成的消息");
        }
        Message last = history.get(history.size() - 1);
        if (!"ASSISTANT".equals(last.getRole())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "最后一条消息不是助手回复，无法重新生成");
        }
        Message userMessage = null;
        for (int i = history.size() - 2; i >= 0; i--) {
            if ("USER".equals(history.get(i).getRole())) {
                userMessage = history.get(i);
                break;
            }
        }
        if (userMessage == null || userMessage.getContent() == null || userMessage.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到对应的用户消息");
        }
        messageRepository.deleteById(last.getId());
        conversation.setMessageCount(Math.max(0, (conversation.getMessageCount() == null ? 0 : conversation.getMessageCount()) - 1));
        conversationRepository.update(conversation);

        String content = userMessage.getContent().trim();
        AgentVersion draft = agentVersionRepository.findLatestDraft(conversation.getAgentId()).orElse(null);
        KnowledgeRetrievalResult retrieval = draft == null
                ? KnowledgeRetrievalResult.empty()
                : agentChatPreparer.retrieveKnowledge(draft, content);
        PreparedAgentChat prepared = buildPreparedChat(conversation, content, platformModelId, null);
        agentChatExecutor.assertQuotaAvailable();
        configureSseResponse(response);
        Long modelId = prepared.modelId();
        Long convId = conversation.getId();
        Execution execution = executionRecorder.startAgentExecution(
                conversation.getAgentId(),
                prepared.agentVersionId(),
                convId,
                toInputJson(content));
        executionRecorder.recordRagSpan(execution, Map.of("query", content), retrieval.citations());
        String citationsJson = citationsJson(retrieval.citations());
        return agentChatExecutor.stream(prepared, assistantContent -> transactionTemplate.executeWithoutResult(status -> {
            Conversation fresh = conversationRepository.findById(convId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在"));
            appendMessage(fresh, "ASSISTANT", assistantContent, modelId, citationsMetadata(retrieval.citations()));
            updateConversationMeta(fresh, 1);
            executionRecorder.recordLlmSpan(execution, content, Map.of("content", assistantContent));
            executionRecorder.succeed(execution, toOutputJson(assistantContent), estimateTokens(assistantContent));
            captureLongTermMemory(draft, fresh, content, assistantContent);
        }), execution.getId(), citationsJson, execution);
    }

    private void captureLongTermMemory(AgentVersion version,
                                       Conversation conversation,
                                       String userMessage,
                                       String assistantMessage) {
        if (version == null || conversation == null) {
            return;
        }
        longTermMemoryApplicationService.captureFromTurn(
                version,
                conversation.getAgentId(),
                conversation.getWorkspaceId(),
                conversation.getUserId(),
                userMessage,
                assistantMessage);
    }

    private Message appendMessage(Conversation conversation, String role, String content, Long modelId) {
        return appendMessage(conversation, role, content, modelId, null);
    }

    private Message appendMessage(Conversation conversation,
                                  String role,
                                  String content,
                                  Long modelId,
                                  String metadataJson) {
        int sequenceNo = messageRepository.findMaxSequenceNo(conversation.getId()).orElse(0) + 1;
        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setWorkspaceId(conversation.getWorkspaceId());
        message.setRole(role);
        message.setContent(content);
        message.setContentType("TEXT");
        message.setSequenceNo(sequenceNo);
        message.setModelId(modelId);
        message.setMetadataJson(metadataJson);
        return messageRepository.save(message);
    }

    private String citationsMetadata(List<KnowledgeCitationVO> citations) {
        if (citations == null || citations.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(Map.of("citations", citations));
        } catch (Exception ex) {
            return null;
        }
    }

    private String citationsJson(List<KnowledgeCitationVO> citations) {
        if (citations == null || citations.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (Exception ex) {
            return null;
        }
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
                message.getMetadataJson(),
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

    private String toInputJson(String message) {
        return "{\"message\":\"" + escapeJson(message) + "\"}";
    }

    private String toOutputJson(String content) {
        return "{\"content\":\"" + escapeJson(content) + "\"}";
    }

    private int estimateTokens(String content) {
        return content == null || content.isEmpty() ? 0 : Math.max(1, content.length() / 4);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
