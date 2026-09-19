package com.boxai.agent.chat;

import com.boxai.ai.ChatTurn;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.ToolDefinition;
import com.boxai.common.ai.PlatformModelClassifier;
import com.boxai.common.constant.ModelSources;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.crypto.SecretCipher;
import com.boxai.domain.model.ModelCredential;
import com.boxai.domain.model.ModelCredentialRepository;
import com.boxai.domain.model.ModelDefinition;
import com.boxai.domain.model.ModelDefinitionRepository;
import com.boxai.domain.model.ModelProvider;
import com.boxai.domain.model.ModelProviderRepository;
import com.boxai.model.application.ModelRouterApplicationService;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.boxai.common.security.PromptInjectionGuard;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.agent.application.AgentLongTermMemoryApplicationService;
import com.boxai.knowledge.application.KnowledgeRetrievalResult;
import com.boxai.knowledge.application.KnowledgeRetrievalService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgentChatPreparer {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final ModelDefinitionRepository modelDefinitionRepository;
    private final ModelProviderRepository modelProviderRepository;
    private final ModelCredentialRepository credentialRepository;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final ModelRouterApplicationService modelRouterApplicationService;
    private final SecretCipher secretCipher;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final AgentToolRuntimeService agentToolRuntimeService;
    private final AgentLongTermMemoryApplicationService longTermMemoryApplicationService;
    private final ChatUserMessageImageEnricher chatUserMessageImageEnricher;

    public AgentChatPreparer(AgentRepository agentRepository,
                             AgentVersionRepository agentVersionRepository,
                             ModelDefinitionRepository modelDefinitionRepository,
                             ModelProviderRepository modelProviderRepository,
                             ModelCredentialRepository credentialRepository,
                             PlatformModelApplicationService platformModelApplicationService,
                             ModelRouterApplicationService modelRouterApplicationService,
                             SecretCipher secretCipher,
                             KnowledgeRetrievalService knowledgeRetrievalService,
                             AgentToolRuntimeService agentToolRuntimeService,
                             AgentLongTermMemoryApplicationService longTermMemoryApplicationService,
                             ChatUserMessageImageEnricher chatUserMessageImageEnricher) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.modelDefinitionRepository = modelDefinitionRepository;
        this.modelProviderRepository = modelProviderRepository;
        this.credentialRepository = credentialRepository;
        this.platformModelApplicationService = platformModelApplicationService;
        this.modelRouterApplicationService = modelRouterApplicationService;
        this.secretCipher = secretCipher;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.agentToolRuntimeService = agentToolRuntimeService;
        this.longTermMemoryApplicationService = longTermMemoryApplicationService;
        this.chatUserMessageImageEnricher = chatUserMessageImageEnricher;
    }

    public PreparedAgentChat prepare(Long agentId, List<ChatTurn> history, String userMessage) {
        return prepare(agentId, history, userMessage, null, null);
    }

    public PreparedAgentChat prepare(Long agentId, List<ChatTurn> history, String userMessage, Long platformModelOverride) {
        return prepare(agentId, history, userMessage, platformModelOverride, null);
    }

    public PreparedAgentChat prepare(Long agentId,
                                     List<ChatTurn> history,
                                     String userMessage,
                                     Long platformModelOverride,
                                     KnowledgeRetrievalResult retrieval) {
        Agent agent = requireAgent(agentId);
        AgentVersion version = agentVersionRepository.findLatestDraft(agent.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "智能体草稿版本不存在"));
        return prepareWithVersion(agent, version, history, userMessage, platformModelOverride, retrieval);
    }

    public KnowledgeRetrievalResult retrieveKnowledge(AgentVersion version, String userMessage) {
        if (!Boolean.TRUE.equals(version.getKnowledgeEnabled())) {
            return KnowledgeRetrievalResult.empty();
        }
        return knowledgeRetrievalService.retrieve(version.getId(), userMessage);
    }

    public PreparedAgentChat preparePublished(Long agentId, List<ChatTurn> history, String userMessage) {
        return preparePublished(agentId, history, userMessage, null);
    }

    public PreparedAgentChat preparePublished(Long agentId,
                                              List<ChatTurn> history,
                                              String userMessage,
                                              KnowledgeRetrievalResult retrieval) {
        Agent agent = requireAgent(agentId);
        if (agent.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_PUBLISHED, "智能体尚未发布");
        }
        AgentVersion version = agentVersionRepository.findById(agent.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "发布版本不存在"));
        return prepareWithVersion(agent, version, history, userMessage, null, retrieval);
    }

    private PreparedAgentChat prepareWithVersion(Agent agent,
                                                 AgentVersion draft,
                                                 List<ChatTurn> history,
                                                 String userMessage,
                                                 Long platformModelOverride,
                                                 KnowledgeRetrievalResult retrieval) {
        ResolvedAgentChatModel resolved = resolveAgentChatModel(draft, platformModelOverride);
        List<ChatTurn> turns = buildTurns(
                agent.getId(),
                draft,
                history,
                userMessage,
                retrieval,
                resolved);
        return buildPrepared(
                agent.getId(),
                resolved.runtimeConfig(),
                turns,
                draft,
                resolved.credentialId(),
                resolved.platformCredential(),
                resolved.modelId(),
                null);
    }

    private record ResolvedAgentChatModel(
            ModelRuntimeConfig runtimeConfig,
            Long credentialId,
            boolean platformCredential,
            Long modelId,
            boolean sendImagesMultimodal
    ) {
    }

    private ResolvedAgentChatModel resolveAgentChatModel(AgentVersion draft, Long platformModelOverride) {
        String modelSource = draft.getModelSource() == null ? ModelSources.PLATFORM : draft.getModelSource();
        if (ModelSources.AUTO.equals(modelSource)) {
            if (platformModelOverride != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "智能路由模式下不支持手动指定平台模型");
            }
            Long selectedModelId = modelRouterApplicationService.selectFromPlatformModels(
                    platformModelApplicationService.listRunnablePlatformModels(),
                    modelRouterApplicationService.parsePreference(draft.getRoutingPreference()));
            if (selectedModelId == null) {
                throw new BusinessException(ErrorCode.PLATFORM_MODEL_NOT_FOUND, "没有可用于智能路由的平台模型");
            }
            ResolvedPlatformModel resolved = platformModelApplicationService.resolveForChat(selectedModelId);
            ModelRuntimeConfig runtime = resolved.runtimeConfig();
            boolean multimodal = PlatformModelClassifier.isOcrOrVisionModel(runtime.modelName(), null, null);
            return new ResolvedAgentChatModel(
                    runtime,
                    resolved.platformCredentialId(),
                    true,
                    resolved.platformModelId(),
                    multimodal);
        }
        if (ModelSources.PLATFORM.equals(modelSource)) {
            Long platformModelId;
            if (platformModelOverride != null) {
                if (!platformModelApplicationService.isRunnable(platformModelOverride)) {
                    throw new BusinessException(ErrorCode.PLATFORM_MODEL_NOT_FOUND, "所选平台模型不可用");
                }
                platformModelId = platformModelOverride;
            } else {
                platformModelId = platformModelApplicationService.requireRunnableModelId(draft.getPlatformModelId());
            }
            ResolvedPlatformModel resolved = platformModelApplicationService.resolveForChat(platformModelId);
            ModelRuntimeConfig runtime = resolved.runtimeConfig();
            boolean multimodal = PlatformModelClassifier.isOcrOrVisionModel(runtime.modelName(), null, null);
            return new ResolvedAgentChatModel(
                    runtime,
                    resolved.platformCredentialId(),
                    true,
                    resolved.platformModelId(),
                    multimodal);
        }
        if (platformModelOverride != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前智能体使用自定义模型，不支持在对话中切换平台模型");
        }
        if (draft.getModelId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先配置对话模型");
        }
        platformModelApplicationService.assertByokAllowed(WorkspaceContext.require().userId());
        ModelDefinition model = requireWorkspaceModel(draft.getModelId());
        ModelProvider provider = modelProviderRepository.findById(model.getProviderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND, "Provider 不存在"));
        ModelCredential credential = credentialRepository.findActiveByProvider(workspaceId(), provider.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDENTIAL_MISSING, "请先为该 Provider 配置 API Key"));
        String apiKey = secretCipher.decrypt(credential.getEncryptedApiKey());
        boolean multimodal = Boolean.TRUE.equals(model.getSupportVision())
                || PlatformModelClassifier.isOcrOrVisionModel(
                model.getModelCode(), model.getModelName(), null);
        return new ResolvedAgentChatModel(
                new ModelRuntimeConfig(provider.getBaseUrl(), apiKey, model.getModelCode()),
                credential.getId(),
                false,
                model.getId(),
                multimodal);
    }

    private PreparedAgentChat buildPrepared(Long agentId,
                                            com.boxai.ai.ModelRuntimeConfig runtimeConfig,
                                            List<ChatTurn> turns,
                                            AgentVersion draft,
                                            Long credentialId,
                                            boolean platformCredential,
                                            Long modelId,
                                            String toolConfirmationToken) {
        List<ToolDefinition> tools = Boolean.TRUE.equals(draft.getToolEnabled())
                ? agentToolRuntimeService.resolveTools(draft.getId()).stream()
                .map(item -> new ToolDefinition(item.toolKey(), item.description()))
                .toList()
                : List.of();
        return new PreparedAgentChat(
                agentId,
                runtimeConfig,
                turns,
                toDouble(draft.getTemperature()),
                toDouble(draft.getTopP()),
                draft.getMaxTokens(),
                credentialId,
                platformCredential,
                modelId,
                draft.getId(),
                tools,
                toolConfirmationToken);
    }

    private List<ChatTurn> buildTurns(Long agentId,
                                      AgentVersion draft,
                                      List<ChatTurn> history,
                                      String userMessage,
                                      KnowledgeRetrievalResult retrieval,
                                      ResolvedAgentChatModel resolvedModel) {
        boolean enrichImagesWithOcr = !resolvedModel.sendImagesMultimodal();
        boolean sendImagesMultimodal = resolvedModel.sendImagesMultimodal();
        List<ChatTurn> turns = new ArrayList<>();
        String systemPrompt = draft.getSystemPrompt();
        if (Boolean.TRUE.equals(draft.getKnowledgeEnabled())) {
            KnowledgeRetrievalResult effective = retrieval != null
                    ? retrieval
                    : knowledgeRetrievalService.retrieve(draft.getId(), userMessage);
            String ragContext = effective.context();
            if (ragContext != null && !ragContext.isBlank()) {
                String ragBlock = "以下是与用户问题相关的知识库内容，请优先参考，并在回答中标注引用编号：\n"
                        + PromptInjectionGuard.wrapUntrustedContext("knowledge", ragContext);
                systemPrompt = systemPrompt == null || systemPrompt.isBlank()
                        ? ragBlock
                        : systemPrompt + "\n\n" + ragBlock;
            }
        }
        if (Boolean.TRUE.equals(draft.getLongTermMemoryEnabled())) {
            String memoryContext = longTermMemoryApplicationService.buildContext(draft, agentId, userMessage);
            if (memoryContext != null && !memoryContext.isBlank()) {
                String memoryBlock = "以下是关于该用户的长期记忆，可在回答时参考：\n"
                        + PromptInjectionGuard.wrapUntrustedContext("memory", memoryContext);
                systemPrompt = systemPrompt == null || systemPrompt.isBlank()
                        ? memoryBlock
                        : systemPrompt + "\n\n" + memoryBlock;
            }
        }
        String policy = PromptInjectionGuard.systemPolicy();
        systemPrompt = systemPrompt == null || systemPrompt.isBlank() ? policy : policy + "\n\n" + systemPrompt;
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            turns.add(new ChatTurn("SYSTEM", systemPrompt));
        }
        if (Boolean.TRUE.equals(draft.getMemoryEnabled()) && history != null) {
            for (ChatTurn turn : history) {
                if (turn == null || turn.content() == null || turn.content().isBlank()) {
                    continue;
                }
                String role = turn.role() == null ? "" : turn.role().toUpperCase();
                if ("USER".equals(role) || "ASSISTANT".equals(role)) {
                    String content = turn.content();
                    if ("USER".equals(role) && enrichImagesWithOcr) {
                        content = chatUserMessageImageEnricher.enrichWithImageText(content);
                    }
                    turns.add(new ChatTurn(role, content));
                }
            }
        }
        String userContent = enrichImagesWithOcr
                ? chatUserMessageImageEnricher.enrichWithImageText(userMessage)
                : userMessage;
        String wrappedUserContent = sendImagesMultimodal && userContent.contains("[图片:")
                ? PromptInjectionGuard.wrapUserMessageForVision(userContent)
                : PromptInjectionGuard.wrapUserMessage(userContent);
        turns.add(new ChatTurn("USER", wrappedUserContent));
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

    private ModelDefinition requireWorkspaceModel(Long modelId) {
        ModelDefinition model = modelDefinitionRepository.findById(modelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在"));
        ModelProvider provider = modelProviderRepository.findById(model.getProviderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND, "Provider 不存在"));
        if (!workspaceId().equals(provider.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该模型");
        }
        return model;
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
