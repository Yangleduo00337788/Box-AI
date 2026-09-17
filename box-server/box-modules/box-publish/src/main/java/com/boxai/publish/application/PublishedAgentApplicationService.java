package com.boxai.publish.application;

import com.boxai.agent.api.AgentChatHistoryItem;
import com.boxai.agent.api.AgentChatRequest;
import com.boxai.agent.api.AgentChatVO;
import com.boxai.agent.application.AgentLongTermMemoryApplicationService;
import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.agent.chat.ChatCitationSupport;
import com.boxai.agent.chat.PreparedAgentChat;
import com.boxai.ai.ChatTurn;
import com.boxai.common.constant.PublishResourceTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.publish.PublishRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.agent.api.AgentEmbedConfigVO;
import com.boxai.agent.api.PublishedEmbedResolveVO;
import com.boxai.agent.application.EmbedDomainApplicationService;
import com.boxai.agent.support.AgentEmbedConfigSupport;
import com.boxai.domain.publish.EmbedCustomDomain;
import com.boxai.knowledge.application.KnowledgeRetrievalResult;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.trace.application.ExecutionRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PublishedAgentApplicationService {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final PublishRepository publishRepository;
    private final AgentChatPreparer agentChatPreparer;
    private final AgentChatExecutor agentChatExecutor;
    private final ExecutionRecorder executionRecorder;
    private final AgentLongTermMemoryApplicationService longTermMemoryApplicationService;
    private final EmbedDomainApplicationService embedDomainApplicationService;
    private final ObjectMapper objectMapper;

    public PublishedAgentApplicationService(AgentRepository agentRepository,
                                            AgentVersionRepository agentVersionRepository,
                                            PublishRepository publishRepository,
                                            AgentChatPreparer agentChatPreparer,
                                            AgentChatExecutor agentChatExecutor,
                                            ExecutionRecorder executionRecorder,
                                            AgentLongTermMemoryApplicationService longTermMemoryApplicationService,
                                            EmbedDomainApplicationService embedDomainApplicationService,
                                            ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.publishRepository = publishRepository;
        this.agentChatPreparer = agentChatPreparer;
        this.agentChatExecutor = agentChatExecutor;
        this.executionRecorder = executionRecorder;
        this.longTermMemoryApplicationService = longTermMemoryApplicationService;
        this.embedDomainApplicationService = embedDomainApplicationService;
        this.objectMapper = objectMapper;
    }

    public AgentEmbedConfigVO getEmbedConfig(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在"));
        if (agent.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_PUBLISHED, "智能体尚未发布");
        }
        publishRepository.findLatestActive(PublishResourceTypes.AGENT, agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_PUBLISHED, "智能体发布记录不存在"));
        AgentVersion version = agentVersionRepository.findById(agent.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "发布版本不存在"));
        return embedDomainApplicationService.attach(
                AgentEmbedConfigSupport.toVO(version.getConfigJson(), agent.getName()),
                agent.getId(),
                false);
    }

    public PublishedEmbedResolveVO resolveByHost(String host) {
        EmbedCustomDomain domain = embedDomainApplicationService.findVerifiedByHost(host)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "未找到已验证的自定义域名"));
        AgentEmbedConfigVO embed = getEmbedConfig(domain.getAgentId());
        return new PublishedEmbedResolveVO(domain.getAgentId(), domain.getDomain(), embed);
    }

    public AgentChatVO chat(Long agentId, AgentChatRequest request) {
        Agent agent = requirePublishedAgent(agentId);
        String message = request.message().trim();
        AgentVersion version = requirePublishedVersion(agent);
        List<ChatTurn> history = resolveHistory(version, request.history());
        KnowledgeRetrievalResult retrieval = agentChatPreparer.retrieveKnowledge(version, message);
        PreparedAgentChat prepared = applyToolConfirmation(
                agentChatPreparer.preparePublished(agentId, history, message, retrieval),
                request.toolConfirmationToken());
        Execution execution = executionRecorder.startAgentExecution(
                agentId,
                prepared.agentVersionId(),
                null,
                toInputJson(message));
        executionRecorder.recordRagSpan(execution, Map.of("query", message), retrieval.citations());
        try {
            String content = agentChatExecutor.chat(prepared, execution);
            executionRecorder.recordLlmSpan(execution, message, Map.of("content", content));
            executionRecorder.succeed(execution, toOutputJson(content), estimateTokens(content));
            longTermMemoryApplicationService.captureFromTurn(
                    version,
                    agentId,
                    workspaceId(),
                    WorkspaceContext.require().userId(),
                    message,
                    content);
            return new AgentChatVO(content, retrieval.citations(), execution.getId());
        } catch (RuntimeException e) {
            executionRecorder.fail(execution, e.getMessage());
            throw e;
        }
    }

    public SseEmitter streamChat(Long agentId, AgentChatRequest request, HttpServletResponse response) {
        Agent agent = requirePublishedAgent(agentId);
        String message = request.message().trim();
        AgentVersion version = requirePublishedVersion(agent);
        List<ChatTurn> history = resolveHistory(version, request.history());
        KnowledgeRetrievalResult retrieval = agentChatPreparer.retrieveKnowledge(version, message);
        PreparedAgentChat prepared = applyToolConfirmation(
                agentChatPreparer.preparePublished(agentId, history, message, retrieval),
                request.toolConfirmationToken());
        Execution execution = executionRecorder.startAgentExecution(
                agentId,
                prepared.agentVersionId(),
                null,
                toInputJson(message));
        executionRecorder.recordRagSpan(execution, Map.of("query", message), retrieval.citations());
        agentChatExecutor.assertQuotaAvailable();
        agentChatExecutor.assertPublishedChatRateLimit(agentId);
        configureSseResponse(response);
        Long workspaceId = workspaceId();
        Long userId = WorkspaceContext.require().userId();
        String citationsJson = ChatCitationSupport.toCitationsJson(objectMapper, retrieval.citations());
        return agentChatExecutor.stream(prepared, content -> {
            executionRecorder.recordLlmSpan(execution, message, Map.of("content", content));
            executionRecorder.succeed(execution, toOutputJson(content), estimateTokens(content));
            longTermMemoryApplicationService.captureFromTurn(
                    version, agentId, workspaceId, userId, message, content);
        }, execution.getId(), citationsJson, execution);
    }

    private PreparedAgentChat applyToolConfirmation(PreparedAgentChat prepared, String token) {
        if (token == null || token.isBlank()) {
            return prepared;
        }
        return prepared.withToolConfirmationToken(token.trim());
    }

    private List<ChatTurn> resolveHistory(AgentVersion version, List<AgentChatHistoryItem> history) {
        if (Boolean.FALSE.equals(version.getMemoryEnabled()) || history == null || history.isEmpty()) {
            return List.of();
        }
        int windowSize = version.getMemoryWindowSize() == null ? 20 : Math.max(0, version.getMemoryWindowSize());
        if (windowSize == 0) {
            return List.of();
        }
        int start = Math.max(0, history.size() - windowSize);
        List<ChatTurn> turns = new ArrayList<>();
        for (int index = start; index < history.size(); index++) {
            AgentChatHistoryItem item = history.get(index);
            if (item == null || item.content() == null || item.content().isBlank()) {
                continue;
            }
            String role = item.role() == null ? "" : item.role().toUpperCase();
            if ("USER".equals(role) || "ASSISTANT".equals(role)) {
                turns.add(new ChatTurn(role, item.content()));
            }
        }
        return turns;
    }

    private Agent requirePublishedAgent(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在"));
        if (!workspaceId().equals(agent.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该智能体");
        }
        if (agent.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_PUBLISHED, "智能体尚未发布");
        }
        publishRepository.findLatestActive(PublishResourceTypes.AGENT, agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_PUBLISHED, "智能体发布记录不存在"));
        return agent;
    }

    private AgentVersion requirePublishedVersion(Agent agent) {
        return agentVersionRepository.findById(agent.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "发布版本不存在"));
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }

    private String toInputJson(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("message", message));
        } catch (Exception e) {
            return "{\"message\":\"\"}";
        }
    }

    private String toOutputJson(String content) {
        try {
            return objectMapper.writeValueAsString(Map.of("content", content == null ? "" : content));
        } catch (Exception e) {
            return "{\"content\":\"\"}";
        }
    }

    private Integer estimateTokens(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return Math.max(1, content.length() / 4);
    }

    private void configureSseResponse(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
