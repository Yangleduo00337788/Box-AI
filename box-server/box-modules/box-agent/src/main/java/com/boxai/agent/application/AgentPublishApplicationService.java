package com.boxai.agent.application;

import com.boxai.agent.api.AgentPublishVO;
import com.boxai.common.constant.AuditActions;
import com.boxai.common.constant.AuditResourceTypes;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.constant.PublishResourceTypes;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.notification.NotificationPublisher;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.domain.publish.Publish;
import com.boxai.domain.publish.PublishRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AgentPublishApplicationService {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final AgentBindingApplicationService agentBindingApplicationService;
    private final PublishRepository publishRepository;
    private final WorkspacePermissionService workspacePermissionService;
    private final AuditLogService auditLogService;
    private final NotificationPublisher notificationPublisher;

    public AgentPublishApplicationService(AgentRepository agentRepository,
                                          AgentVersionRepository agentVersionRepository,
                                          AgentBindingApplicationService agentBindingApplicationService,
                                          PublishRepository publishRepository,
                                          WorkspacePermissionService workspacePermissionService,
                                          AuditLogService auditLogService,
                                          NotificationPublisher notificationPublisher) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.agentBindingApplicationService = agentBindingApplicationService;
        this.publishRepository = publishRepository;
        this.workspacePermissionService = workspacePermissionService;
        this.auditLogService = auditLogService;
        this.notificationPublisher = notificationPublisher;
    }

    public AgentPublishVO getPublishStatus(Long agentId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Agent agent = requireAgent(agentId);
        if (agent.getPublishedVersionId() == null) {
            return new AgentPublishVO(agent.getId(), agent.getStatus(), null, null, null, null);
        }
        AgentVersion published = agentVersionRepository.findById(agent.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "发布版本不存在"));
        return new AgentPublishVO(
                agent.getId(),
                agent.getStatus(),
                published.getId(),
                published.getVersionNo(),
                published.getVersionName(),
                published.getPublishedAt());
    }

    @Transactional
    public AgentPublishVO publish(Long agentId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_PUBLISH);
        Agent agent = requireAgent(agentId);
        AgentVersion draft = agentVersionRepository.findLatestDraft(agent.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_VERSION_NOT_FOUND, "智能体草稿版本不存在"));
        validateDraft(draft);

        draft.setStatus("PUBLISHED");
        draft.setPublishedAt(LocalDateTime.now());
        draft.setUpdatedBy(WorkspaceContext.require().userId());
        agentVersionRepository.update(draft);

        agent.setPublishedVersionId(draft.getId());
        agent.setStatus("PUBLISHED");
        agent.setUpdatedBy(WorkspaceContext.require().userId());
        agentRepository.update(agent);

        int nextVersionNo = agentVersionRepository.maxVersionNo(agent.getId()) + 1;
        AgentVersion newDraft = copyVersion(draft, nextVersionNo, "DRAFT");
        newDraft.setVersionName("v" + nextVersionNo);
        agentVersionRepository.save(newDraft);
        agentBindingApplicationService.copyBindings(draft.getId(), newDraft.getId(), agent.getId());

        publishRepository.revokeByResource(PublishResourceTypes.AGENT, agent.getId());
        Publish publish = new Publish();
        publish.setWorkspaceId(agent.getWorkspaceId());
        publish.setResourceType(PublishResourceTypes.AGENT);
        publish.setResourceId(agent.getId());
        publish.setVersionId(draft.getId());
        publish.setChannel("API");
        publish.setStatus("PUBLISHED");
        publish.setPublishedBy(WorkspaceContext.require().userId());
        publish.setPublishedAt(LocalDateTime.now());
        publishRepository.save(publish);

        auditLogService.recordSuccess(
                AuditActions.AGENT_PUBLISH,
                AuditResourceTypes.AGENT,
                agent.getId(),
                agent.getName(),
                "versionId=" + draft.getId());
        Long userId = WorkspaceContext.require().userId();
        notificationPublisher.publish(
                userId,
                agent.getWorkspaceId(),
                "智能体已发布",
                "「" + agent.getName() + "」已发布，可通过对话与开放 API 使用。",
                "AGENT",
                "/agents/" + agent.getId() + "/builder");
        return getPublishStatus(agentId);
    }

    @Transactional
    public AgentPublishVO unpublish(Long agentId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_PUBLISH);
        Agent agent = requireAgent(agentId);
        agent.setPublishedVersionId(null);
        agent.setStatus("DRAFT");
        agent.setUpdatedBy(WorkspaceContext.require().userId());
        agentRepository.update(agent);
        publishRepository.revokeByResource(PublishResourceTypes.AGENT, agent.getId());
        auditLogService.recordSuccess(
                AuditActions.AGENT_UNPUBLISH,
                AuditResourceTypes.AGENT,
                agent.getId(),
                agent.getName(),
                null);
        return getPublishStatus(agentId);
    }

    private void validateDraft(AgentVersion draft) {
        boolean hasPlatform = draft.getPlatformModelId() != null;
        boolean hasByok = draft.getModelId() != null;
        if (!hasPlatform && !hasByok) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前请先配置对话模型");
        }
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
        version.setRoutingPreference(source.getRoutingPreference());
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

    private Agent requireAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在"));
        if (!workspaceId().equals(agent.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该智能体");
        }
        return agent;
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }
}
