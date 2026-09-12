package com.boxai.agent.application;

import com.boxai.agent.api.AgentVersionCompareVO;
import com.boxai.agent.api.AgentVersionDiffVO;
import com.boxai.agent.api.AgentVersionVO;
import com.boxai.agent.api.CreateAgentVersionRequest;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentKnowledge;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.domain.agent.AgentMcp;
import com.boxai.domain.agent.AgentMcpRepository;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentTool;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AgentVersionApplicationService {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final AgentBindingApplicationService agentBindingApplicationService;
    private final AgentKnowledgeRepository agentKnowledgeRepository;
    private final AgentToolRepository agentToolRepository;
    private final AgentMcpRepository agentMcpRepository;
    private final WorkspacePermissionService workspacePermissionService;

    public AgentVersionApplicationService(AgentRepository agentRepository,
                                          AgentVersionRepository agentVersionRepository,
                                          AgentBindingApplicationService agentBindingApplicationService,
                                          AgentKnowledgeRepository agentKnowledgeRepository,
                                          AgentToolRepository agentToolRepository,
                                          AgentMcpRepository agentMcpRepository,
                                          WorkspacePermissionService workspacePermissionService) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.agentBindingApplicationService = agentBindingApplicationService;
        this.agentKnowledgeRepository = agentKnowledgeRepository;
        this.agentToolRepository = agentToolRepository;
        this.agentMcpRepository = agentMcpRepository;
        this.workspacePermissionService = workspacePermissionService;
    }

    public List<AgentVersionVO> list(Long agentId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Agent agent = requireAgent(agentId);
        AgentVersion draft = agentVersionRepository.findLatestDraft(agentId).orElse(null);
        return agentVersionRepository.listByAgentId(agentId).stream()
                .map(version -> toVO(version, agent, draft))
                .toList();
    }

    public AgentVersionCompareVO compare(Long agentId, Long baseVersionId, Long targetVersionId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        requireAgent(agentId);
        AgentVersion base = requireVersion(agentId, baseVersionId);
        AgentVersion target = requireVersion(agentId, targetVersionId);
        List<AgentVersionDiffVO> diffs = new ArrayList<>();
        addDiff(diffs, "systemPrompt", "System Prompt", base.getSystemPrompt(), target.getSystemPrompt());
        addDiff(diffs, "modelSource", "模型来源", base.getModelSource(), target.getModelSource());
        addDiff(diffs, "platformModelId", "平台模型 ID", valueOf(base.getPlatformModelId()), valueOf(target.getPlatformModelId()));
        addDiff(diffs, "modelId", "BYOK 模型 ID", valueOf(base.getModelId()), valueOf(target.getModelId()));
        addDiff(diffs, "temperature", "Temperature", valueOf(base.getTemperature()), valueOf(target.getTemperature()));
        addDiff(diffs, "topP", "Top P", valueOf(base.getTopP()), valueOf(target.getTopP()));
        addDiff(diffs, "maxTokens", "Max Tokens", valueOf(base.getMaxTokens()), valueOf(target.getMaxTokens()));
        addDiff(diffs, "streamEnabled", "流式输出", valueOf(base.getStreamEnabled()), valueOf(target.getStreamEnabled()));
        addDiff(diffs, "memoryEnabled", "会话记忆", valueOf(base.getMemoryEnabled()), valueOf(target.getMemoryEnabled()));
        addDiff(diffs, "memoryWindowSize", "记忆窗口", valueOf(base.getMemoryWindowSize()), valueOf(target.getMemoryWindowSize()));
        addDiff(diffs, "longTermMemoryEnabled", "长期记忆", valueOf(base.getLongTermMemoryEnabled()), valueOf(target.getLongTermMemoryEnabled()));
        addDiff(diffs, "knowledgeEnabled", "知识库开关", valueOf(base.getKnowledgeEnabled()), valueOf(target.getKnowledgeEnabled()));
        addDiff(diffs, "toolEnabled", "工具开关", valueOf(base.getToolEnabled()), valueOf(target.getToolEnabled()));
        addDiff(diffs, "configJson", "高级配置", base.getConfigJson(), target.getConfigJson());
        addDiff(diffs, "knowledgeBindings", "知识库绑定", bindingIds(base.getId(), true), bindingIds(target.getId(), true));
        addDiff(diffs, "toolBindings", "工具绑定", bindingIds(base.getId(), false), bindingIds(target.getId(), false));
        addDiff(diffs, "mcpBindings", "MCP 绑定", mcpIds(base.getId()), mcpIds(target.getId()));
        return new AgentVersionCompareVO(baseVersionId, targetVersionId, diffs);
    }

    @Transactional
    public AgentVersionVO createSnapshot(Long agentId, CreateAgentVersionRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_UPDATE);
        Agent agent = requireAgent(agentId);
        AgentVersion source = resolveSourceVersion(agentId, request == null ? null : request.sourceVersionId());
        int nextNo = agentVersionRepository.maxVersionNo(agentId) + 1;
        AgentVersion snapshot = copyVersion(source, nextNo, "ARCHIVED");
        snapshot.setVersionName("v" + nextNo);
        agentVersionRepository.save(snapshot);
        agentBindingApplicationService.copyBindings(source.getId(), snapshot.getId(), agent.getId());
        AgentVersion draft = agentVersionRepository.findLatestDraft(agentId).orElse(null);
        return toVO(snapshot, agent, draft);
    }

    @Transactional
    public void restore(Long agentId, Long versionId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_UPDATE);
        Agent agent = requireAgent(agentId);
        if ("PUBLISHED".equals(agent.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先取消发布后再恢复版本");
        }
        AgentVersion target = requireVersion(agentId, versionId);
        AgentVersion draft = requireDraft(agent);
        if (draft.getId().equals(versionId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能恢复到当前草稿");
        }
        applyVersion(target, draft);
        agentVersionRepository.update(draft);
        agentBindingApplicationService.replaceBindings(draft.getId(), target.getId(), agent.getId());
    }

    @Transactional
    public AgentVersionVO archive(Long agentId, Long versionId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_UPDATE);
        Agent agent = requireAgent(agentId);
        AgentVersion version = requireVersion(agentId, versionId);
        AgentVersion draft = requireDraft(agent);
        if (version.getId().equals(draft.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能归档当前草稿");
        }
        if (version.getId().equals(agent.getPublishedVersionId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能归档当前发布版本");
        }
        if (!"ARCHIVED".equals(version.getStatus())) {
            version.setStatus("ARCHIVED");
            version.setUpdatedBy(WorkspaceContext.require().userId());
            agentVersionRepository.update(version);
        }
        return toVO(version, agent, draft);
    }

    private AgentVersion resolveSourceVersion(Long agentId, Long sourceVersionId) {
        if (sourceVersionId == null) {
            return requireDraft(requireAgent(agentId));
        }
        return requireVersion(agentId, sourceVersionId);
    }

    private void applyVersion(AgentVersion source, AgentVersion target) {
        target.setSystemPrompt(source.getSystemPrompt());
        target.setModelId(source.getModelId());
        target.setPlatformModelId(source.getPlatformModelId());
        target.setModelSource(source.getModelSource());
        target.setTemperature(source.getTemperature());
        target.setTopP(source.getTopP());
        target.setMaxTokens(source.getMaxTokens());
        target.setStreamEnabled(source.getStreamEnabled());
        target.setMemoryEnabled(source.getMemoryEnabled());
        target.setMemoryWindowSize(source.getMemoryWindowSize());
        target.setLongTermMemoryEnabled(source.getLongTermMemoryEnabled());
        target.setKnowledgeEnabled(source.getKnowledgeEnabled());
        target.setToolEnabled(source.getToolEnabled());
        target.setConfigJson(source.getConfigJson());
        target.setUpdatedBy(WorkspaceContext.require().userId());
    }

    private AgentVersion copyVersion(AgentVersion source, int versionNo, String status) {
        AgentVersion version = new AgentVersion();
        version.setAgentId(source.getAgentId());
        version.setVersionNo(versionNo);
        version.setStatus(status);
        version.setSystemPrompt(source.getSystemPrompt());
        version.setModelId(source.getModelId());
        version.setPlatformModelId(source.getPlatformModelId());
        version.setModelSource(source.getModelSource());
        version.setTemperature(source.getTemperature());
        version.setTopP(source.getTopP());
        version.setMaxTokens(source.getMaxTokens());
        version.setStreamEnabled(source.getStreamEnabled());
        version.setMemoryEnabled(source.getMemoryEnabled());
        version.setMemoryWindowSize(source.getMemoryWindowSize());
        version.setLongTermMemoryEnabled(source.getLongTermMemoryEnabled());
        version.setKnowledgeEnabled(source.getKnowledgeEnabled());
        version.setToolEnabled(source.getToolEnabled());
        version.setConfigJson(source.getConfigJson());
        version.setCreatedBy(WorkspaceContext.require().userId());
        version.setUpdatedBy(WorkspaceContext.require().userId());
        return version;
    }

    private AgentVersionVO toVO(AgentVersion version, Agent agent, AgentVersion draft) {
        boolean currentDraft = draft != null && draft.getId().equals(version.getId());
        boolean published = agent.getPublishedVersionId() != null && agent.getPublishedVersionId().equals(version.getId());
        return new AgentVersionVO(
                version.getId(),
                version.getVersionNo(),
                version.getVersionName(),
                version.getStatus(),
                version.getPublishedAt(),
                version.getCreatedAt(),
                version.getUpdatedAt(),
                currentDraft,
                published);
    }

    private void addDiff(List<AgentVersionDiffVO> diffs, String field, String label, String base, String target) {
        String baseValue = base == null ? "" : base;
        String targetValue = target == null ? "" : target;
        diffs.add(new AgentVersionDiffVO(field, label, baseValue, targetValue, !Objects.equals(baseValue, targetValue)));
    }

    private String bindingIds(Long versionId, boolean knowledge) {
        if (knowledge) {
            return agentKnowledgeRepository.listByVersionId(versionId).stream()
                    .map(AgentKnowledge::getKnowledgeBaseId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }
        return agentToolRepository.listByVersionId(versionId).stream()
                .map(AgentTool::getToolId)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private String mcpIds(Long versionId) {
        return agentMcpRepository.listByVersionId(versionId).stream()
                .map(AgentMcp::getMcpServerId)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Agent requireAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在"));
        if (!workspaceId().equals(agent.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该智能体");
        }
        return agent;
    }

    private AgentVersion requireVersion(Long agentId, Long versionId) {
        AgentVersion version = agentVersionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "版本不存在"));
        if (!agentId.equals(version.getAgentId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "版本不属于该智能体");
        }
        return version;
    }

    private AgentVersion requireDraft(Agent agent) {
        return agentVersionRepository.findLatestDraft(agent.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "智能体草稿版本不存在"));
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }
}
