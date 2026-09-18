package com.boxai.agent.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.market.MarketRolloutSupport;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.WorkspaceContext;
import org.springframework.stereotype.Component;

@Component
public class MarketRolloutResolver {

    private final WorkspaceRepository workspaceRepository;

    public MarketRolloutResolver(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public Long currentTenantId() {
        return resolveTenantId(WorkspaceContext.require().workspaceId());
    }

    public boolean isVisible(Long itemId, String visibility, String tenantIdsJson, Integer rolloutPercent) {
        return MarketRolloutSupport.isVisibleToTenant(
                currentTenantId(),
                itemId,
                visibility,
                tenantIdsJson,
                rolloutPercent);
    }

    private Long resolveTenantId(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "工作空间不存在"));
        if (workspace.getTenantId() == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "工作空间未绑定租户");
        }
        return workspace.getTenantId();
    }
}
