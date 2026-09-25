package com.boxai.agent.application;

import com.boxai.agent.api.plugin.CreateWorkspacePluginRequest;
import com.boxai.agent.api.plugin.PluginCatalogVO;
import com.boxai.common.constant.MarketReviewStatuses;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkspacePluginCatalogApplicationService {

    private final PluginCatalogRepository pluginCatalogRepository;
    private final PluginCategoryApplicationService pluginCategoryApplicationService;
    private final WorkspacePermissionService workspacePermissionService;
    private final PluginMarketApplicationService pluginMarketApplicationService;
    private final ObjectMapper objectMapper;

    public WorkspacePluginCatalogApplicationService(PluginCatalogRepository pluginCatalogRepository,
                                                     PluginCategoryApplicationService pluginCategoryApplicationService,
                                                     WorkspacePermissionService workspacePermissionService,
                                                     PluginMarketApplicationService pluginMarketApplicationService,
                                                     ObjectMapper objectMapper) {
        this.pluginCatalogRepository = pluginCatalogRepository;
        this.pluginCategoryApplicationService = pluginCategoryApplicationService;
        this.workspacePermissionService = workspacePermissionService;
        this.pluginMarketApplicationService = pluginMarketApplicationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PluginCatalogVO create(CreateWorkspacePluginRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.TOOL_CREATE);
        pluginCategoryApplicationService.requireActiveCategory(request.category());
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Long userId = WorkspaceContext.require().userId();
        String manifestJson = PluginManifest.normalizeAndValidate(request.category(), request.manifestJson(), objectMapper);

        PluginCatalog plugin = new PluginCatalog();
        plugin.setPluginCode("ws-" + workspaceId + "-" + UUID.randomUUID().toString().substring(0, 8));
        plugin.setCategory(request.category().trim());
        plugin.setTitle(request.title().trim());
        plugin.setDescription(request.description());
        plugin.setManifestJson(manifestJson);
        plugin.setStatus("LISTED");
        plugin.setReviewStatus(MarketReviewStatuses.APPROVED);
        plugin.setSourceType("USER");
        plugin.setSubmittedBy(userId);
        plugin.setSubmittedWorkspaceId(workspaceId);
        plugin.setVisibility("GLOBAL");
        plugin.setRolloutPercent(100);
        plugin.setSortOrder(0);
        plugin.setInstallCount(0);
        pluginCatalogRepository.save(plugin);

        pluginMarketApplicationService.install(plugin.getId());
        return pluginMarketApplicationService.findVo(plugin.getId());
    }
}
