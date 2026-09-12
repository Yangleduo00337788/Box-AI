package com.boxai.agent.application;

import com.boxai.agent.api.AgentChatHistoryItem;
import com.boxai.agent.api.AgentChatRequest;
import com.boxai.agent.api.AgentChatVO;
import com.boxai.agent.api.AgentVO;
import com.boxai.agent.api.CreateAgentRequest;
import com.boxai.agent.api.UpdateAgentConfigRequest;
import com.boxai.agent.api.UpdateAgentMemoryRequest;
import com.boxai.agent.api.UpdateAgentModelRequest;
import com.boxai.agent.api.UpdateAgentPromptRequest;
import com.boxai.agent.api.UpdateAgentRequest;
import com.boxai.ai.ChatTurn;
import com.boxai.agent.chat.AgentChatExecutor;
import com.boxai.agent.chat.AgentChatPreparer;
import com.boxai.agent.chat.PreparedAgentChat;
import com.boxai.knowledge.application.KnowledgeRetrievalResult;
import com.boxai.common.constant.ModelSources;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.platform.PlatformModel;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.model.ModelDefinition;
import com.boxai.domain.model.ModelDefinitionRepository;
import com.boxai.domain.model.ModelProvider;
import com.boxai.domain.model.ModelProviderRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.trace.application.ExecutionRecorder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AgentApplicationService {

    private static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.7000");
    private static final BigDecimal DEFAULT_TOP_P = new BigDecimal("1.0000");
    private static final int DEFAULT_MAX_TOKENS = 4096;

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final ModelDefinitionRepository modelDefinitionRepository;
    private final ModelProviderRepository modelProviderRepository;
    private final AgentChatPreparer agentChatPreparer;
    private final AgentChatExecutor agentChatExecutor;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final PlatformModelRepository platformModelRepository;
    private final ExecutionRecorder executionRecorder;
    private final AgentBindingApplicationService agentBindingApplicationService;
    private final AgentPublishApplicationService agentPublishApplicationService;
    private final WorkspacePermissionService workspacePermissionService;
    private final AgentLongTermMemoryApplicationService longTermMemoryApplicationService;

    public AgentApplicationService(AgentRepository agentRepository,
                                   AgentVersionRepository agentVersionRepository,
                                   ModelDefinitionRepository modelDefinitionRepository,
                                   ModelProviderRepository modelProviderRepository,
                                   AgentChatPreparer agentChatPreparer,
                                   AgentChatExecutor agentChatExecutor,
                                   PlatformModelApplicationService platformModelApplicationService,
                                   PlatformModelRepository platformModelRepository,
                                   ExecutionRecorder executionRecorder,
                                   AgentBindingApplicationService agentBindingApplicationService,
                                   AgentPublishApplicationService agentPublishApplicationService,
                                   WorkspacePermissionService workspacePermissionService,
                                   AgentLongTermMemoryApplicationService longTermMemoryApplicationService) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.modelDefinitionRepository = modelDefinitionRepository;
        this.modelProviderRepository = modelProviderRepository;
        this.agentChatPreparer = agentChatPreparer;
        this.agentChatExecutor = agentChatExecutor;
        this.platformModelApplicationService = platformModelApplicationService;
        this.platformModelRepository = platformModelRepository;
        this.executionRecorder = executionRecorder;
        this.agentBindingApplicationService = agentBindingApplicationService;
        this.agentPublishApplicationService = agentPublishApplicationService;
        this.workspacePermissionService = workspacePermissionService;
        this.longTermMemoryApplicationService = longTermMemoryApplicationService;
    }

    public List<AgentVO> list() {
        workspacePermissionService.requirePermission("agent:read");
        return agentRepository.listByWorkspace(workspaceId()).stream().map(this::toVO).toList();
    }

    public AgentVO detail(Long id) {
        workspacePermissionService.requirePermission("agent:read");
        return toVO(requireAgent(id));
    }

    @Transactional
    public AgentVO create(CreateAgentRequest request) {
        workspacePermissionService.requirePermission("agent:create");
        Long userId = WorkspaceContext.require().userId();
        Agent agent = new Agent();
        agent.setWorkspaceId(workspaceId());
        agent.setName(request.name().trim());
        agent.setDescription(trimToNull(request.description()));
        agent.setAvatarUrl(trimToNull(request.avatarUrl()));
        agent.setStatus("DRAFT");
        agent.setCreatedBy(userId);
        agent.setUpdatedBy(userId);
        agentRepository.save(agent);

        AgentVersion version = new AgentVersion();
        version.setAgentId(agent.getId());
        version.setVersionNo(1);
        version.setVersionName("v1");
        version.setStatus("DRAFT");
        if (request.platformModelId() != null) {
            platformModelApplicationService.resolveForChat(request.platformModelId());
            version.setModelSource(ModelSources.PLATFORM);
            version.setPlatformModelId(request.platformModelId());
        } else if (request.modelId() != null) {
            platformModelApplicationService.assertByokAllowed(userId);
            requireModel(request.modelId());
            version.setModelSource(ModelSources.BYOK);
            version.setModelId(request.modelId());
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择平台模型");
        }
        version.setTemperature(DEFAULT_TEMPERATURE);
        version.setTopP(DEFAULT_TOP_P);
        version.setMaxTokens(DEFAULT_MAX_TOKENS);
        version.setStreamEnabled(true);
        version.setMemoryEnabled(true);
        version.setMemoryWindowSize(20);
        version.setLongTermMemoryEnabled(false);
        version.setKnowledgeEnabled(false);
        version.setToolEnabled(false);
        version.setCreatedBy(userId);
        version.setUpdatedBy(userId);
        agentVersionRepository.save(version);

        return toVO(agent);
    }

    @Transactional
    public AgentVO update(Long id, UpdateAgentRequest request) {
        workspacePermissionService.requirePermission("agent:update");
        Agent agent = requireAgent(id);
        Long userId = WorkspaceContext.require().userId();
        agent.setName(request.name().trim());
        agent.setDescription(trimToNull(request.description()));
        agent.setAvatarUrl(trimToNull(request.avatarUrl()));
        agent.setUpdatedBy(userId);
        agentRepository.update(agent);

        if (request.modelId() != null) {
            requireModel(request.modelId());
            AgentVersion draft = requireDraft(agent);
            draft.setModelId(request.modelId());
            draft.setUpdatedBy(userId);
            agentVersionRepository.update(draft);
        }

        return toVO(agent);
    }

    @Transactional
    public AgentVO updatePrompt(Long id, UpdateAgentPromptRequest request) {
        workspacePermissionService.requirePermission("agent:update");
        Agent agent = requireAgent(id);
        Long userId = WorkspaceContext.require().userId();
        AgentVersion draft = requireDraft(agent);
        draft.setSystemPrompt(trimToNull(request.systemPrompt()));
        draft.setUpdatedBy(userId);
        agentVersionRepository.update(draft);
        agent.setUpdatedBy(userId);
        agentRepository.update(agent);
        return toVO(agent);
    }

    @Transactional
    public AgentVO updateMemoryConfig(Long id, UpdateAgentMemoryRequest request) {
        workspacePermissionService.requirePermission("agent:update");
        Agent agent = requireAgent(id);
        Long userId = WorkspaceContext.require().userId();
        AgentVersion draft = requireDraft(agent);
        if (request.memoryEnabled() != null) {
            draft.setMemoryEnabled(request.memoryEnabled());
        }
        if (request.memoryWindowSize() != null) {
            draft.setMemoryWindowSize(request.memoryWindowSize());
        }
        if (request.longTermMemoryEnabled() != null) {
            draft.setLongTermMemoryEnabled(request.longTermMemoryEnabled());
        }
        draft.setUpdatedBy(userId);
        agentVersionRepository.update(draft);
        agent.setUpdatedBy(userId);
        agentRepository.update(agent);
        return toVO(agent);
    }

    @Transactional
    public AgentVO updateConfig(Long id, UpdateAgentConfigRequest request) {
        workspacePermissionService.requirePermission("agent:update");
        Agent agent = requireAgent(id);
        Long userId = WorkspaceContext.require().userId();
        AgentVersion draft = requireDraft(agent);
        draft.setConfigJson(request.configJson());
        draft.setUpdatedBy(userId);
        agentVersionRepository.update(draft);
        agent.setUpdatedBy(userId);
        agentRepository.update(agent);
        return toVO(agent);
    }

    @Transactional
    public AgentVO updateModelConfig(Long id, UpdateAgentModelRequest request) {
        workspacePermissionService.requirePermission("agent:update");
        Agent agent = requireAgent(id);
        Long userId = WorkspaceContext.require().userId();
        AgentVersion draft = requireDraft(agent);
        applyModelSelection(draft, request.modelSource(), request.platformModelId(), request.modelId(), userId);
        draft.setTemperature(request.temperature() == null ? DEFAULT_TEMPERATURE : request.temperature());
        draft.setTopP(request.topP() == null ? DEFAULT_TOP_P : request.topP());
        draft.setMaxTokens(request.maxTokens() == null ? DEFAULT_MAX_TOKENS : request.maxTokens());
        draft.setStreamEnabled(request.streamEnabled() == null || request.streamEnabled());
        draft.setUpdatedBy(userId);
        agentVersionRepository.update(draft);
        agent.setUpdatedBy(userId);
        agentRepository.update(agent);
        return toVO(agent);
    }

    public AgentChatVO chat(Long id, AgentChatRequest request) {
        workspacePermissionService.requirePermission("agent:read");
        Agent agent = requireAgent(id);
        AgentVersion draft = requireDraft(agent);
        String message = request.message().trim();
        List<ChatTurn> history = resolveHistory(draft, request.history());
        KnowledgeRetrievalResult retrieval = agentChatPreparer.retrieveKnowledge(draft, message);
        PreparedAgentChat prepared = agentChatPreparer.prepare(id, history, message);
        Execution execution = executionRecorder.startAgentExecution(
                id,
                prepared.agentVersionId(),
                null,
                toInputJson(message));
        try {
            executionRecorder.recordRagSpan(execution, Map.of("query", message), retrieval.citations());
            String content = agentChatExecutor.chat(prepared, execution);
            executionRecorder.recordLlmSpan(execution, message, Map.of("content", content));
            executionRecorder.succeed(execution, toOutputJson(content), estimateTokens(content));
            longTermMemoryApplicationService.captureFromTurn(
                    draft,
                    id,
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

    public SseEmitter streamChat(Long id, AgentChatRequest request, HttpServletResponse response) {
        workspacePermissionService.requirePermission("agent:read");
        Agent agent = requireAgent(id);
        AgentVersion draft = requireDraft(agent);
        String message = request.message().trim();
        List<ChatTurn> history = resolveHistory(draft, request.history());
        KnowledgeRetrievalResult retrieval = agentChatPreparer.retrieveKnowledge(draft, message);
        PreparedAgentChat prepared = agentChatPreparer.prepare(id, history, message);
        Execution execution = executionRecorder.startAgentExecution(
                id,
                prepared.agentVersionId(),
                null,
                toInputJson(message));
        executionRecorder.recordRagSpan(execution, Map.of("query", message), retrieval.citations());
        agentChatExecutor.assertQuotaAvailable();
        configureSseResponse(response);
        Long workspaceId = workspaceId();
        Long userId = WorkspaceContext.require().userId();
        return agentChatExecutor.stream(prepared, content -> {
            executionRecorder.recordLlmSpan(execution, message, Map.of("content", content));
            executionRecorder.succeed(execution, toOutputJson(content), estimateTokens(content));
            longTermMemoryApplicationService.captureFromTurn(
                    draft, id, workspaceId, userId, message, content);
        });
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission("agent:delete");
        Agent agent = requireAgent(id);
        if ("PUBLISHED".equalsIgnoreCase(agent.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已发布的智能体请先取消发布后再删除");
        }
        agentRepository.delete(id);
    }

    @Transactional
    public AgentVO duplicate(Long id) {
        workspacePermissionService.requirePermission("agent:create");
        Agent source = requireAgent(id);
        AgentVersion sourceDraft = requireDraft(source);
        Long userId = WorkspaceContext.require().userId();

        Agent copy = new Agent();
        copy.setWorkspaceId(workspaceId());
        copy.setName(source.getName() + " 副本");
        copy.setDescription(source.getDescription());
        copy.setAvatarUrl(source.getAvatarUrl());
        copy.setStatus("DRAFT");
        copy.setCreatedBy(userId);
        copy.setUpdatedBy(userId);
        agentRepository.save(copy);

        AgentVersion version = new AgentVersion();
        version.setAgentId(copy.getId());
        version.setVersionNo(1);
        version.setVersionName("v1");
        version.setStatus("DRAFT");
        version.setSystemPrompt(sourceDraft.getSystemPrompt());
        version.setModelId(sourceDraft.getModelId());
        version.setPlatformModelId(sourceDraft.getPlatformModelId());
        version.setModelSource(sourceDraft.getModelSource());
        version.setTemperature(sourceDraft.getTemperature());
        version.setTopP(sourceDraft.getTopP());
        version.setMaxTokens(sourceDraft.getMaxTokens());
        version.setStreamEnabled(sourceDraft.getStreamEnabled());
        version.setMemoryEnabled(sourceDraft.getMemoryEnabled());
        version.setMemoryWindowSize(sourceDraft.getMemoryWindowSize());
        version.setLongTermMemoryEnabled(sourceDraft.getLongTermMemoryEnabled());
        version.setKnowledgeEnabled(sourceDraft.getKnowledgeEnabled());
        version.setToolEnabled(sourceDraft.getToolEnabled());
        version.setConfigJson(sourceDraft.getConfigJson());
        version.setCreatedBy(userId);
        version.setUpdatedBy(userId);
        agentVersionRepository.save(version);
        agentBindingApplicationService.copyBindings(sourceDraft.getId(), version.getId(), copy.getId());
        return toVO(copy);
    }

    @Transactional
    public AgentVO archive(Long id) {
        workspacePermissionService.requirePermission("agent:update");
        Agent agent = requireAgent(id);
        if ("ARCHIVED".equals(agent.getStatus())) {
            return toVO(agent);
        }
        if ("PUBLISHED".equals(agent.getStatus())) {
            agentPublishApplicationService.unpublish(id);
            agent = requireAgent(id);
        }
        agent.setStatus("ARCHIVED");
        agent.setUpdatedBy(WorkspaceContext.require().userId());
        agentRepository.update(agent);
        return toVO(agent);
    }

    private List<ChatTurn> resolveHistory(AgentVersion draft, List<AgentChatHistoryItem> history) {
        if (Boolean.FALSE.equals(draft.getMemoryEnabled()) || history == null || history.isEmpty()) {
            return List.of();
        }
        int windowSize = draft.getMemoryWindowSize() == null ? 20 : Math.max(0, draft.getMemoryWindowSize());
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

    private Agent requireAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在"));
        if (!workspaceId().equals(agent.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该智能体");
        }
        return agent;
    }

    private AgentVersion requireDraft(Agent agent) {
        return agentVersionRepository.findLatestDraft(agent.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "智能体草稿版本不存在"));
    }

    private ModelDefinition requireModel(Long modelId) {
        ModelDefinition model = modelDefinitionRepository.findById(modelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在"));
        ModelProvider provider = modelProviderRepository.findById(model.getProviderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND, "Provider 不存在"));
        if (!workspaceId().equals(provider.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该模型");
        }
        return model;
    }

    private AgentVO toVO(Agent agent) {
        AgentVersion draft = agentVersionRepository.findLatestDraft(agent.getId()).orElse(null);
        Integer draftVersion = draft == null ? null : draft.getVersionNo();
        Integer publishedVersion = null;
        String modelSource = draft == null ? ModelSources.PLATFORM : draft.getModelSource();
        Long modelId = draft == null ? null : draft.getModelId();
        String modelName = null;
        Long platformModelId = draft == null ? null : draft.getPlatformModelId();
        String platformModelName = null;
        String systemPrompt = draft == null ? null : draft.getSystemPrompt();
        BigDecimal temperature = draft == null ? null : draft.getTemperature();
        BigDecimal topP = draft == null ? null : draft.getTopP();
        Integer maxTokens = draft == null ? null : draft.getMaxTokens();
        Boolean streamEnabled = draft == null ? null : draft.getStreamEnabled();
        Boolean memoryEnabled = draft == null ? null : draft.getMemoryEnabled();
        Integer memoryWindowSize = draft == null ? null : draft.getMemoryWindowSize();
        Boolean longTermMemoryEnabled = draft == null ? null : draft.getLongTermMemoryEnabled();
        String configJson = draft == null ? null : draft.getConfigJson();

        if (agent.getPublishedVersionId() != null) {
            AgentVersion published = agentVersionRepository.findById(agent.getPublishedVersionId()).orElse(null);
            if (published != null) {
                publishedVersion = published.getVersionNo();
            }
        }

        if (modelId != null) {
            modelName = modelDefinitionRepository.findById(modelId).map(ModelDefinition::getModelName).orElse(null);
        }
        if (platformModelId != null) {
            platformModelName = platformModelRepository.findById(platformModelId).map(PlatformModel::getModelName).orElse(null);
        }

        return new AgentVO(
                agent.getId(),
                agent.getName(),
                agent.getDescription(),
                agent.getAvatarUrl(),
                agent.getStatus(),
                draftVersion,
                publishedVersion,
                modelSource,
                modelId,
                modelName,
                platformModelId,
                platformModelName,
                systemPrompt,
                temperature,
                topP,
                maxTokens,
                streamEnabled,
                memoryEnabled,
                memoryWindowSize,
                longTermMemoryEnabled,
                configJson,
                agent.getCreatedBy(),
                agent.getCreatedAt(),
                agent.getUpdatedAt());
    }

    private void applyModelSelection(AgentVersion draft, String modelSource, Long platformModelId, Long modelId, Long userId) {
        if (ModelSources.PLATFORM.equals(modelSource)) {
            if (platformModelId == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择平台模型");
            }
            platformModelApplicationService.resolveForChat(platformModelId);
            draft.setModelSource(ModelSources.PLATFORM);
            draft.setPlatformModelId(platformModelId);
            draft.setModelId(null);
            return;
        }
        if (!ModelSources.BYOK.equals(modelSource)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型来源无效");
        }
        platformModelApplicationService.assertByokAllowed(userId);
        if (modelId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择自带模型");
        }
        requireModel(modelId);
        draft.setModelSource(ModelSources.BYOK);
        draft.setModelId(modelId);
        draft.setPlatformModelId(null);
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
        return "{\"message\":" + quoteJson(message) + "}";
    }

    private String toOutputJson(String content) {
        return "{\"content\":" + quoteJson(content) + "}";
    }

    private String quoteJson(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private int estimateTokens(String content) {
        return content == null ? 0 : Math.max(content.length(), 1);
    }
}
