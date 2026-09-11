package com.boxai.agent.chat;

import com.boxai.ai.ChatTurn;
import com.boxai.ai.ToolDefinition;
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
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.knowledge.application.KnowledgeRetrievalService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AgentChatPreparer {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final ModelDefinitionRepository modelDefinitionRepository;
    private final ModelProviderRepository modelProviderRepository;
    private final ModelCredentialRepository credentialRepository;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final SecretCipher secretCipher;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final AgentToolRuntimeService agentToolRuntimeService;

    public AgentChatPreparer(AgentRepository agentRepository,
                             AgentVersionRepository agentVersionRepository,
                             ModelDefinitionRepository modelDefinitionRepository,
                             ModelProviderRepository modelProviderRepository,
                             ModelCredentialRepository credentialRepository,
                             PlatformModelApplicationService platformModelApplicationService,
                             SecretCipher secretCipher,
                             KnowledgeRetrievalService knowledgeRetrievalService,
                             AgentToolRuntimeService agentToolRuntimeService) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.modelDefinitionRepository = modelDefinitionRepository;
        this.modelProviderRepository = modelProviderRepository;
        this.credentialRepository = credentialRepository;
        this.platformModelApplicationService = platformModelApplicationService;
        this.secretCipher = secretCipher;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.agentToolRuntimeService = agentToolRuntimeService;
    }

    public PreparedAgentChat prepare(Long agentId, List<ChatTurn> history, String userMessage) {
        Agent agent = requireAgent(agentId);
        AgentVersion version = agentVersionRepository.findLatestDraft(agent.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "智能体草稿版本不存在"));
        return prepareWithVersion(agent, version, history, userMessage);
    }

    public PreparedAgentChat preparePublished(Long agentId, List<ChatTurn> history, String userMessage) {
        Agent agent = requireAgent(agentId);
        if (agent.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_PUBLISHED, "智能体尚未发布");
        }
        AgentVersion version = agentVersionRepository.findById(agent.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "发布版本不存在"));
        return prepareWithVersion(agent, version, history, userMessage);
    }

    private PreparedAgentChat prepareWithVersion(Agent agent,
                                                 AgentVersion draft,
                                                 List<ChatTurn> history,
                                                 String userMessage) {
        List<ChatTurn> turns = buildTurns(draft, history, userMessage);
        String modelSource = draft.getModelSource() == null ? ModelSources.PLATFORM : draft.getModelSource();
        if (ModelSources.PLATFORM.equals(modelSource)) {
            Long platformModelId = draft.getPlatformModelId();
            if (!platformModelApplicationService.isRunnable(platformModelId)) {
                Long runnableId = platformModelApplicationService.findFirstRunnableModelId()
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.PLATFORM_CREDENTIAL_MISSING,
                                "平台尚未配置可用模型密钥：请在管理端「平台模型池」为已上架模型绑定密钥"));
                if (!Objects.equals(platformModelId, runnableId)) {
                    draft.setPlatformModelId(runnableId);
                    agentVersionRepository.update(draft);
                }
                platformModelId = runnableId;
            }
            ResolvedPlatformModel resolved = platformModelApplicationService.resolveForChat(platformModelId);
            return buildPrepared(resolved.runtimeConfig(), turns, draft, resolved.platformCredentialId(), true, resolved.platformModelId());
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
        return buildPrepared(
                new com.boxai.ai.ModelRuntimeConfig(provider.getBaseUrl(), apiKey, model.getModelCode()),
                turns,
                draft,
                credential.getId(),
                false,
                model.getId());
    }

    private PreparedAgentChat buildPrepared(com.boxai.ai.ModelRuntimeConfig runtimeConfig,
                                            List<ChatTurn> turns,
                                            AgentVersion draft,
                                            Long credentialId,
                                            boolean platformCredential,
                                            Long modelId) {
        List<ToolDefinition> tools = Boolean.TRUE.equals(draft.getToolEnabled())
                ? agentToolRuntimeService.resolveTools(draft.getId()).stream()
                .map(item -> new ToolDefinition(item.toolKey(), item.description()))
                .toList()
                : List.of();
        return new PreparedAgentChat(
                runtimeConfig,
                turns,
                toDouble(draft.getTemperature()),
                toDouble(draft.getTopP()),
                draft.getMaxTokens(),
                credentialId,
                platformCredential,
                modelId,
                draft.getId(),
                tools);
    }

    private List<ChatTurn> buildTurns(AgentVersion draft, List<ChatTurn> history, String userMessage) {
        List<ChatTurn> turns = new ArrayList<>();
        String systemPrompt = draft.getSystemPrompt();
        if (Boolean.TRUE.equals(draft.getKnowledgeEnabled())) {
            String ragContext = knowledgeRetrievalService.buildContext(draft.getId(), userMessage);
            if (ragContext != null && !ragContext.isBlank()) {
                String ragBlock = "以下是与用户问题相关的知识库内容，请优先参考：\n" + ragContext;
                systemPrompt = systemPrompt == null || systemPrompt.isBlank()
                        ? ragBlock
                        : systemPrompt + "\n\n" + ragBlock;
            }
        }
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
                    turns.add(new ChatTurn(role, turn.content()));
                }
            }
        }
        turns.add(new ChatTurn("USER", userMessage.trim()));
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
