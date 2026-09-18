package com.boxai.agent.application;

import com.boxai.agent.api.plugin.PluginCatalogVO;
import com.boxai.common.constant.MarketReviewStatuses;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.WorkspacePluginInstall;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PluginMarketApplicationService {

    private final PluginCatalogRepository pluginCatalogRepository;
    private final WorkspacePluginInstallRepository workspacePluginInstallRepository;
    private final PluginCategoryApplicationService pluginCategoryApplicationService;
    private final WorkspacePermissionService workspacePermissionService;
    private final PluginInstallProvisioner pluginInstallProvisioner;
    private final MarketRolloutResolver marketRolloutResolver;

    public PluginMarketApplicationService(PluginCatalogRepository pluginCatalogRepository,
                                          WorkspacePluginInstallRepository workspacePluginInstallRepository,
                                          PluginCategoryApplicationService pluginCategoryApplicationService,
                                          WorkspacePermissionService workspacePermissionService,
                                          PluginInstallProvisioner pluginInstallProvisioner,
                                          MarketRolloutResolver marketRolloutResolver) {
        this.pluginCatalogRepository = pluginCatalogRepository;
        this.workspacePluginInstallRepository = workspacePluginInstallRepository;
        this.pluginCategoryApplicationService = pluginCategoryApplicationService;
        this.workspacePermissionService = workspacePermissionService;
        this.pluginInstallProvisioner = pluginInstallProvisioner;
        this.marketRolloutResolver = marketRolloutResolver;
    }

    public List<PluginCatalogVO> list(String category) {
        workspacePermissionService.requirePermission(PermissionCodes.TOOL_EXECUTE);
        pluginCategoryApplicationService.requireActiveCategory(category);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Map<Long, WorkspacePluginInstall> installMap = workspacePluginInstallRepository.listByWorkspace(workspaceId)
                .stream()
                .collect(Collectors.toMap(WorkspacePluginInstall::getPluginId, Function.identity(), (left, right) -> left));
        return pluginCatalogRepository.listByCategory(category).stream()
                .filter(plugin -> marketRolloutResolver.isVisible(
                        plugin.getId(),
                        plugin.getVisibility(),
                        plugin.getTenantIdsJson(),
                        plugin.getRolloutPercent()))
                .map(plugin -> toVO(plugin, installMap.get(plugin.getId())))
                .toList();
    }

    @Transactional
    public void install(Long pluginId) {
        workspacePermissionService.requirePermission(PermissionCodes.TOOL_CREATE);
        PluginCatalog plugin = requireListedPlugin(pluginId);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Long userId = WorkspaceContext.require().userId();
        if (workspacePluginInstallRepository.findByWorkspaceAndPlugin(workspaceId, pluginId).isPresent()) {
            return;
        }
        PluginInstallProvisioner.ProvisionResult provision =
                pluginInstallProvisioner.provision(plugin, workspaceId, userId);
        WorkspacePluginInstall install = new WorkspacePluginInstall();
        install.setWorkspaceId(workspaceId);
        install.setPluginId(pluginId);
        if (provision != null) {
            install.setResourceType(provision.resourceType());
            install.setResourceId(provision.resourceId());
        }
        install.setInstalledBy(userId);
        workspacePluginInstallRepository.save(install);
        pluginCatalogRepository.incrementInstallCount(plugin.getId());
    }

    @Transactional
    public void uninstall(Long pluginId) {
        workspacePermissionService.requirePermission(PermissionCodes.TOOL_CREATE);
        requireListedPlugin(pluginId);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        workspacePluginInstallRepository.findByWorkspaceAndPlugin(workspaceId, pluginId)
                .ifPresent(install -> {
                    pluginInstallProvisioner.deprovision(install);
                    workspacePluginInstallRepository.delete(install.getId());
                    pluginCatalogRepository.decrementInstallCount(pluginId);
                });
    }

    private PluginCatalog requireListedPlugin(Long pluginId) {
        PluginCatalog plugin = pluginCatalogRepository.findById(pluginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件不存在"));
        if (!MarketReviewStatuses.visibleToConsumers(plugin.getStatus(), plugin.getReviewStatus())) {
            throw new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件未上架、未通过审核或已下架");
        }
        if (!marketRolloutResolver.isVisible(
                plugin.getId(),
                plugin.getVisibility(),
                plugin.getTenantIdsJson(),
                plugin.getRolloutPercent())) {
            throw new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件尚未向当前租户放量");
        }
        return plugin;
    }

    private PluginCatalogVO toVO(PluginCatalog plugin, WorkspacePluginInstall install) {
        boolean installed = install != null;
        String resourceType = installed ? install.getResourceType() : null;
        Long resourceId = installed ? install.getResourceId() : null;
        return new PluginCatalogVO(
                plugin.getId(),
                plugin.getPluginCode(),
                plugin.getCategory(),
                plugin.getTitle(),
                plugin.getDescription(),
                plugin.getInstallCount(),
                installed,
                resourceType,
                resourceId,
                resolveTargetPath(resourceType, resourceId));
    }

    private String resolveTargetPath(String resourceType, Long resourceId) {
        if (resourceType == null || resourceId == null) {
            return null;
        }
        return switch (resourceType) {
            case "tool" -> "/tools";
            case "knowledge" -> "/knowledge";
            case "workflow" -> "/workflows/" + resourceId + "/editor";
            case "mcp" -> "/mcp";
            default -> null;
        };
    }
}
