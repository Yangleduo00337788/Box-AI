package com.boxai.agent.application;

import com.boxai.agent.api.AgentVO;
import com.boxai.agent.api.template.AgentTemplateVO;
import com.boxai.agent.api.template.CreateAgentTemplateRequest;
import com.boxai.agent.api.template.UpdateAgentTemplateRequest;
import com.boxai.agent.api.template.UpdateAgentTemplateStatusRequest;
import com.boxai.common.constant.ModelSources;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentTemplate;
import com.boxai.domain.agent.AgentTemplateRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.platform.PlatformModel;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.security.context.SecurityContexts;
import com.boxai.security.context.WorkspaceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class AgentTemplateApplicationService {

    private static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.7000");
    private static final BigDecimal DEFAULT_TOP_P = new BigDecimal("1.0000");
    private static final int DEFAULT_MAX_TOKENS = 4096;
    private static final Set<String> TEMPLATE_STATUSES = Set.of("DRAFT", "LISTED", "ARCHIVED");

    private final AgentTemplateRepository agentTemplateRepository;
    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final PlatformModelRepository platformModelRepository;
    private final AgentApplicationService agentApplicationService;

    public AgentTemplateApplicationService(AgentTemplateRepository agentTemplateRepository,
                                           AgentRepository agentRepository,
                                           AgentVersionRepository agentVersionRepository,
                                           PlatformModelApplicationService platformModelApplicationService,
                                           PlatformModelRepository platformModelRepository,
                                           AgentApplicationService agentApplicationService) {
        this.agentTemplateRepository = agentTemplateRepository;
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.platformModelApplicationService = platformModelApplicationService;
        this.platformModelRepository = platformModelRepository;
        this.agentApplicationService = agentApplicationService;
    }

    public List<AgentTemplateVO> listAdmin() {
        return agentTemplateRepository.listAll().stream().map(this::toVO).toList();
    }

    public List<AgentTemplateVO> listMarket() {
        return agentTemplateRepository.listListed().stream().map(this::toConsumerVO).toList();
    }

    @Transactional
    public AgentTemplateVO create(CreateAgentTemplateRequest request) {
        platformModelApplicationService.resolveForChat(request.platformModelId());
        Long adminId = SecurityContexts.currentUser().userId();
        AgentTemplate template = new AgentTemplate();
        template.setTemplateCode(request.templateCode().trim());
        template.setName(request.name().trim());
        template.setDescription(trimToNull(request.description()));
        template.setAvatarUrl(trimToNull(request.avatarUrl()));
        template.setCategory(trimToNull(request.category()));
        template.setSystemPrompt(trimToNull(request.systemPrompt()));
        template.setPlatformModelId(request.platformModelId());
        template.setTemperature(request.temperature() == null ? DEFAULT_TEMPERATURE : request.temperature());
        template.setTopP(request.topP() == null ? DEFAULT_TOP_P : request.topP());
        template.setMaxTokens(request.maxTokens() == null ? DEFAULT_MAX_TOKENS : request.maxTokens());
        template.setStreamEnabled(request.streamEnabled() == null || request.streamEnabled());
        template.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        template.setStatus("DRAFT");
        template.setInstallCount(0);
        template.setCreatedBy(adminId);
        template.setUpdatedBy(adminId);
        agentTemplateRepository.save(template);
        return toVO(template);
    }

    @Transactional
    public AgentTemplateVO update(Long id, UpdateAgentTemplateRequest request) {
        AgentTemplate template = requireTemplate(id);
        platformModelApplicationService.resolveForChat(request.platformModelId());
        template.setName(request.name().trim());
        template.setDescription(trimToNull(request.description()));
        template.setAvatarUrl(trimToNull(request.avatarUrl()));
        template.setCategory(trimToNull(request.category()));
        template.setSystemPrompt(trimToNull(request.systemPrompt()));
        template.setPlatformModelId(request.platformModelId());
        template.setTemperature(request.temperature() == null ? DEFAULT_TEMPERATURE : request.temperature());
        template.setTopP(request.topP() == null ? DEFAULT_TOP_P : request.topP());
        template.setMaxTokens(request.maxTokens() == null ? DEFAULT_MAX_TOKENS : request.maxTokens());
        template.setStreamEnabled(request.streamEnabled() == null || request.streamEnabled());
        template.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        template.setUpdatedBy(SecurityContexts.currentUser().userId());
        agentTemplateRepository.update(template);
        return toVO(template);
    }

    @Transactional
    public AgentTemplateVO updateStatus(Long id, UpdateAgentTemplateStatusRequest request) {
        AgentTemplate template = requireTemplate(id);
        String status = request.status().trim().toUpperCase();
        if (!TEMPLATE_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模板状态无效");
        }
        if ("LISTED".equals(status) && template.getPlatformModelId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上架前需配置平台模型");
        }
        if ("LISTED".equals(status)) {
            platformModelApplicationService.resolveForChat(template.getPlatformModelId());
        }
        template.setStatus(status);
        template.setUpdatedBy(SecurityContexts.currentUser().userId());
        agentTemplateRepository.update(template);
        return toVO(template);
    }

    @Transactional
    public AgentVO enable(Long templateId) {
        AgentTemplate template = requireListedTemplate(templateId);
        platformModelApplicationService.resolveForChat(template.getPlatformModelId());
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();

        Agent agent = new Agent();
        agent.setWorkspaceId(workspaceId);
        agent.setName(template.getName());
        agent.setDescription(template.getDescription());
        agent.setAvatarUrl(template.getAvatarUrl());
        agent.setSourceTemplateId(template.getId());
        agent.setStatus("DRAFT");
        agent.setCreatedBy(userId);
        agent.setUpdatedBy(userId);
        agentRepository.save(agent);

        AgentVersion version = new AgentVersion();
        version.setAgentId(agent.getId());
        version.setVersionNo(1);
        version.setVersionName("v1");
        version.setStatus("DRAFT");
        version.setSystemPrompt(template.getSystemPrompt());
        version.setModelSource(ModelSources.PLATFORM);
        version.setPlatformModelId(template.getPlatformModelId());
        version.setTemperature(template.getTemperature() == null ? DEFAULT_TEMPERATURE : template.getTemperature());
        version.setTopP(template.getTopP() == null ? DEFAULT_TOP_P : template.getTopP());
        version.setMaxTokens(template.getMaxTokens() == null ? DEFAULT_MAX_TOKENS : template.getMaxTokens());
        version.setStreamEnabled(template.getStreamEnabled() == null || template.getStreamEnabled());
        version.setMemoryEnabled(true);
        version.setMemoryWindowSize(20);
        version.setKnowledgeEnabled(false);
        version.setToolEnabled(false);
        version.setCreatedBy(userId);
        version.setUpdatedBy(userId);
        agentVersionRepository.save(version);

        agentTemplateRepository.incrementInstallCount(templateId);
        return agentApplicationService.detail(agent.getId());
    }

    private AgentTemplate requireTemplate(Long id) {
        return agentTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_TEMPLATE_NOT_FOUND, "智能体模板不存在"));
    }

    private AgentTemplate requireListedTemplate(Long id) {
        AgentTemplate template = requireTemplate(id);
        if (!"LISTED".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.AGENT_TEMPLATE_NOT_FOUND, "模板未上架或已下架");
        }
        return template;
    }

    private AgentTemplateVO toVO(AgentTemplate template) {
        String platformModelName = null;
        if (template.getPlatformModelId() != null) {
            platformModelName = platformModelRepository.findById(template.getPlatformModelId())
                    .map(PlatformModel::getModelName)
                    .orElse(null);
        }
        return new AgentTemplateVO(
                template.getId(),
                template.getTemplateCode(),
                template.getName(),
                template.getDescription(),
                template.getAvatarUrl(),
                template.getCategory(),
                template.getSystemPrompt(),
                template.getPlatformModelId(),
                platformModelName,
                template.getTemperature(),
                template.getTopP(),
                template.getMaxTokens(),
                template.getStreamEnabled(),
                template.getStatus(),
                template.getSortOrder(),
                template.getInstallCount(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }

    private AgentTemplateVO toConsumerVO(AgentTemplate template) {
        AgentTemplateVO vo = toVO(template);
        return new AgentTemplateVO(
                vo.id(),
                vo.templateCode(),
                vo.name(),
                vo.description(),
                vo.avatarUrl(),
                vo.category(),
                null,
                vo.platformModelId(),
                vo.platformModelName(),
                vo.temperature(),
                vo.topP(),
                vo.maxTokens(),
                vo.streamEnabled(),
                vo.status(),
                vo.sortOrder(),
                vo.installCount(),
                vo.createdAt(),
                vo.updatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
