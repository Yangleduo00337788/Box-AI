package com.boxai.agent.application;

import com.boxai.agent.api.AgentKnowledgeBindingVO;
import com.boxai.agent.api.AgentToolBindingVO;
import com.boxai.agent.api.BindAgentKnowledgeRequest;
import com.boxai.agent.api.BindAgentToolRequest;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentKnowledge;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentTool;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.security.context.WorkspaceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentBindingApplicationService {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final AgentKnowledgeRepository agentKnowledgeRepository;
    private final AgentToolRepository agentToolRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ToolRepository toolRepository;

    public AgentBindingApplicationService(AgentRepository agentRepository,
                                          AgentVersionRepository agentVersionRepository,
                                          AgentKnowledgeRepository agentKnowledgeRepository,
                                          AgentToolRepository agentToolRepository,
                                          KnowledgeBaseRepository knowledgeBaseRepository,
                                          ToolRepository toolRepository) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.agentKnowledgeRepository = agentKnowledgeRepository;
        this.agentToolRepository = agentToolRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.toolRepository = toolRepository;
    }

    public List<AgentKnowledgeBindingVO> listKnowledge(Long agentId) {
        Agent agent = requireAgent(agentId);
        AgentVersion draft = requireDraft(agent);
        return agentKnowledgeRepository.listByVersionId(draft.getId()).stream().map(this::toKnowledgeVO).toList();
    }

    @Transactional
    public AgentKnowledgeBindingVO bindKnowledge(Long agentId, BindAgentKnowledgeRequest request) {
        Agent agent = requireAgent(agentId);
        AgentVersion draft = requireDraft(agent);
        var kb = knowledgeBaseRepository.findById(request.knowledgeBaseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND, "知识库不存在"));
        if (!workspaceId().equals(kb.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该知识库");
        }
        agentKnowledgeRepository.findByVersionAndKnowledgeBase(draft.getId(), request.knowledgeBaseId())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "该知识库已绑定");
                });
        AgentKnowledge binding = new AgentKnowledge();
        binding.setAgentId(agent.getId());
        binding.setVersionId(draft.getId());
        binding.setKnowledgeBaseId(request.knowledgeBaseId());
        binding.setTopK(request.topK() == null ? 5 : request.topK());
        binding.setRetrievalMode(request.retrievalMode() == null ? "HYBRID" : request.retrievalMode());
        binding.setRerankEnabled(request.rerankEnabled() == null || request.rerankEnabled());
        binding.setCitationEnabled(request.citationEnabled() == null || request.citationEnabled());
        agentKnowledgeRepository.save(binding);
        draft.setKnowledgeEnabled(true);
        agentVersionRepository.update(draft);
        return toKnowledgeVO(binding);
    }

    @Transactional
    public void unbindKnowledge(Long agentId, Long knowledgeBaseId) {
        Agent agent = requireAgent(agentId);
        AgentVersion draft = requireDraft(agent);
        AgentKnowledge binding = agentKnowledgeRepository.findByVersionAndKnowledgeBase(draft.getId(), knowledgeBaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND, "绑定关系不存在"));
        agentKnowledgeRepository.delete(binding.getId());
        if (agentKnowledgeRepository.listByVersionId(draft.getId()).isEmpty()) {
            draft.setKnowledgeEnabled(false);
            agentVersionRepository.update(draft);
        }
    }

    public List<AgentToolBindingVO> listTools(Long agentId) {
        Agent agent = requireAgent(agentId);
        AgentVersion draft = requireDraft(agent);
        return agentToolRepository.listByVersionId(draft.getId()).stream().map(this::toToolVO).toList();
    }

    @Transactional
    public AgentToolBindingVO bindTool(Long agentId, BindAgentToolRequest request) {
        Agent agent = requireAgent(agentId);
        AgentVersion draft = requireDraft(agent);
        var tool = toolRepository.findById(request.toolId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在"));
        if (!workspaceId().equals(tool.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工具");
        }
        agentToolRepository.findByVersionAndTool(draft.getId(), request.toolId())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "该工具已绑定");
                });
        AgentTool binding = new AgentTool();
        binding.setAgentId(agent.getId());
        binding.setVersionId(draft.getId());
        binding.setToolId(request.toolId());
        binding.setEnabled(request.enabled() == null || request.enabled());
        binding.setRequireConfirmation(Boolean.TRUE.equals(request.requireConfirmation()));
        binding.setConfigJson(trimToNull(request.configJson()));
        agentToolRepository.save(binding);
        draft.setToolEnabled(true);
        agentVersionRepository.update(draft);
        return toToolVO(binding);
    }

    @Transactional
    public void unbindTool(Long agentId, Long toolId) {
        Agent agent = requireAgent(agentId);
        AgentVersion draft = requireDraft(agent);
        AgentTool binding = agentToolRepository.findByVersionAndTool(draft.getId(), toolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "绑定关系不存在"));
        agentToolRepository.delete(binding.getId());
        if (agentToolRepository.listByVersionId(draft.getId()).isEmpty()) {
            draft.setToolEnabled(false);
            agentVersionRepository.update(draft);
        }
    }

    void copyBindings(Long sourceVersionId, Long targetVersionId, Long agentId) {
        for (AgentKnowledge binding : agentKnowledgeRepository.listByVersionId(sourceVersionId)) {
            AgentKnowledge copy = new AgentKnowledge();
            copy.setAgentId(agentId);
            copy.setVersionId(targetVersionId);
            copy.setKnowledgeBaseId(binding.getKnowledgeBaseId());
            copy.setTopK(binding.getTopK());
            copy.setScoreThreshold(binding.getScoreThreshold());
            copy.setRetrievalMode(binding.getRetrievalMode());
            copy.setRerankEnabled(binding.getRerankEnabled());
            copy.setCitationEnabled(binding.getCitationEnabled());
            agentKnowledgeRepository.save(copy);
        }
        for (AgentTool binding : agentToolRepository.listByVersionId(sourceVersionId)) {
            AgentTool copy = new AgentTool();
            copy.setAgentId(agentId);
            copy.setVersionId(targetVersionId);
            copy.setToolId(binding.getToolId());
            copy.setEnabled(binding.getEnabled());
            copy.setRequireConfirmation(binding.getRequireConfirmation());
            copy.setConfigJson(binding.getConfigJson());
            agentToolRepository.save(copy);
        }
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

    private AgentKnowledgeBindingVO toKnowledgeVO(AgentKnowledge binding) {
        return new AgentKnowledgeBindingVO(
                binding.getId(),
                binding.getKnowledgeBaseId(),
                binding.getTopK(),
                binding.getRetrievalMode(),
                binding.getRerankEnabled(),
                binding.getCitationEnabled());
    }

    private AgentToolBindingVO toToolVO(AgentTool binding) {
        return new AgentToolBindingVO(
                binding.getId(),
                binding.getToolId(),
                binding.getEnabled(),
                binding.getRequireConfirmation(),
                binding.getConfigJson());
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
}
