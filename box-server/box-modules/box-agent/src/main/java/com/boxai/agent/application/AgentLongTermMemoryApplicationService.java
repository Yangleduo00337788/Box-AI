package com.boxai.agent.application;

import com.boxai.agent.api.AgentLongTermMemoryVO;
import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.EmbeddingModelGateway;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentLongTermMemory;
import com.boxai.domain.agent.AgentLongTermMemoryRepository;
import com.boxai.domain.agent.AgentLongTermMemorySearchIndex;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AgentLongTermMemoryApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AgentLongTermMemoryApplicationService.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final String EXTRACTION_SYSTEM_PROMPT =
            "你是记忆提取助手。从对话中提取值得长期记住的用户信息（姓名、偏好、背景、目标等）。"
                    + "若无值得记住的内容，仅回复 NONE。每行一条记忆，不要编号，不要解释。";

    private final AgentRepository agentRepository;
    private final AgentLongTermMemoryRepository memoryRepository;
    private final AgentLongTermMemorySearchIndex searchIndex;
    private final EmbeddingModelGateway embeddingModelGateway;
    private final ChatModelGateway chatModelGateway;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final QuotaApplicationService quotaApplicationService;
    private final WorkspacePermissionService workspacePermissionService;
    private final String defaultEmbeddingModel;

    public AgentLongTermMemoryApplicationService(AgentRepository agentRepository,
                                                 AgentLongTermMemoryRepository memoryRepository,
                                                 AgentLongTermMemorySearchIndex searchIndex,
                                                 EmbeddingModelGateway embeddingModelGateway,
                                                 ChatModelGateway chatModelGateway,
                                                 PlatformModelApplicationService platformModelApplicationService,
                                                 QuotaApplicationService quotaApplicationService,
                                                 WorkspacePermissionService workspacePermissionService,
                                                 @Value("${box.ai.embedding.model-name:text-embedding-3-small}") String defaultEmbeddingModel) {
        this.agentRepository = agentRepository;
        this.memoryRepository = memoryRepository;
        this.searchIndex = searchIndex;
        this.embeddingModelGateway = embeddingModelGateway;
        this.chatModelGateway = chatModelGateway;
        this.platformModelApplicationService = platformModelApplicationService;
        this.quotaApplicationService = quotaApplicationService;
        this.workspacePermissionService = workspacePermissionService;
        this.defaultEmbeddingModel = defaultEmbeddingModel;
    }

    public String buildContext(AgentVersion version, Long agentId, String query) {
        if (!Boolean.TRUE.equals(version.getLongTermMemoryEnabled()) || query == null || query.isBlank()) {
            return "";
        }
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        float[] queryVector = embedText(workspaceId, query);
        if (queryVector.length == 0) {
            return "";
        }
        searchIndex.ensureIndex();
        List<Long> memoryIds = searchIndex.searchByVector(agentId, userId, queryVector, DEFAULT_TOP_K);
        if (memoryIds.isEmpty()) {
            return "";
        }
        List<AgentLongTermMemory> memories = memoryRepository.listByIds(memoryIds);
        if (memories.isEmpty()) {
            return "";
        }
        Set<Long> order = new LinkedHashSet<>(memoryIds);
        List<String> lines = new ArrayList<>();
        for (Long memoryId : order) {
            memories.stream()
                    .filter(item -> memoryId.equals(item.getId()))
                    .findFirst()
                    .ifPresent(item -> lines.add("- " + item.getContent()));
        }
        return String.join("\n", lines);
    }

    public void captureFromTurn(AgentVersion version,
                                Long agentId,
                                Long workspaceId,
                                Long userId,
                                String userMessage,
                                String assistantMessage) {
        if (!Boolean.TRUE.equals(version.getLongTermMemoryEnabled())) {
            return;
        }
        if (userMessage == null || userMessage.isBlank() || assistantMessage == null || assistantMessage.isBlank()) {
            return;
        }
        try {
            ModelRuntimeConfig runtimeConfig = resolveChatConfig();
            if (runtimeConfig == null) {
                return;
            }
            quotaApplicationService.assertAiQuotaAvailable(workspaceId);
            String prompt = "用户：" + userMessage.trim() + "\n助手：" + assistantMessage.trim();
            String extracted = chatModelGateway.chat(
                    runtimeConfig,
                    EXTRACTION_SYSTEM_PROMPT,
                    prompt,
                    0.2,
                    1.0,
                    256);
            quotaApplicationService.consumeAiUsage(workspaceId, Math.max(prompt.length() + extracted.length(), 1L));
            List<String> facts = parseFacts(extracted);
            for (String fact : facts) {
                saveMemory(agentId, workspaceId, userId, fact);
            }
        } catch (Exception e) {
            log.warn("Long-term memory extraction skipped for agent {}: {}", agentId, e.getMessage());
        }
    }

    public List<AgentLongTermMemoryVO> list(Long agentId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        requireAgent(agentId);
        Long userId = WorkspaceContext.require().userId();
        return memoryRepository.listByAgentAndUser(agentId, userId).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public void delete(Long agentId, Long memoryId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_UPDATE);
        requireAgent(agentId);
        Long userId = WorkspaceContext.require().userId();
        AgentLongTermMemory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "记忆不存在"));
        if (!agentId.equals(memory.getAgentId()) || !userId.equals(memory.getUserId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权删除该记忆");
        }
        searchIndex.deleteMemory(memory.getEsDocumentId());
        memoryRepository.delete(memoryId);
    }

    private void saveMemory(Long agentId, Long workspaceId, Long userId, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        float[] vector = embedText(workspaceId, content);
        if (vector.length == 0) {
            return;
        }
        AgentLongTermMemory memory = new AgentLongTermMemory();
        memory.setAgentId(agentId);
        memory.setWorkspaceId(workspaceId);
        memory.setUserId(userId);
        memory.setContent(content.trim());
        memory.setEsDocumentId(UUID.randomUUID().toString());
        memoryRepository.save(memory);
        searchIndex.ensureIndex();
        searchIndex.indexMemory(memory, vector);
    }

    private float[] embedText(Long workspaceId, String text) {
        ModelRuntimeConfig embeddingConfig = resolveEmbeddingConfig();
        if (embeddingConfig == null || text == null || text.isBlank()) {
            return new float[0];
        }
        try {
            quotaApplicationService.assertAiQuotaAvailable(workspaceId);
            float[] vector = embeddingModelGateway.embed(embeddingConfig, text);
            quotaApplicationService.consumeEmbeddingUsage(workspaceId, Math.max(text.length(), 1L));
            return vector;
        } catch (Exception e) {
            log.warn("Long-term memory embedding failed: {}", e.getMessage());
            return new float[0];
        }
    }

    private ModelRuntimeConfig resolveEmbeddingConfig() {
        Long modelId = platformModelApplicationService.findFirstRunnableModelId().orElse(null);
        if (modelId == null) {
            return null;
        }
        ResolvedPlatformModel resolved = platformModelApplicationService.resolveForChat(modelId);
        ModelRuntimeConfig chatConfig = resolved.runtimeConfig();
        return new ModelRuntimeConfig(chatConfig.baseUrl(), chatConfig.apiKey(), defaultEmbeddingModel);
    }

    private ModelRuntimeConfig resolveChatConfig() {
        return platformModelApplicationService.findFirstRunnableModelId()
                .map(platformModelApplicationService::resolveForChat)
                .map(ResolvedPlatformModel::runtimeConfig)
                .orElse(null);
    }

    private List<String> parseFacts(String extracted) {
        if (extracted == null || extracted.isBlank()) {
            return List.of();
        }
        List<String> facts = new ArrayList<>();
        for (String line : extracted.split("\\R")) {
            String fact = line == null ? "" : line.trim();
            if (fact.isBlank()) {
                continue;
            }
            if ("NONE".equalsIgnoreCase(fact) || "无".equals(fact)) {
                continue;
            }
            if (fact.startsWith("- ")) {
                fact = fact.substring(2).trim();
            }
            if (!fact.isBlank()) {
                facts.add(fact);
            }
        }
        return facts;
    }

    private Agent requireAgent(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在"));
        if (!WorkspaceContext.require().workspaceId().equals(agent.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该智能体");
        }
        return agent;
    }

    private AgentLongTermMemoryVO toVO(AgentLongTermMemory memory) {
        return new AgentLongTermMemoryVO(
                memory.getId(),
                memory.getContent(),
                memory.getCreatedAt(),
                memory.getUpdatedAt());
    }
}
