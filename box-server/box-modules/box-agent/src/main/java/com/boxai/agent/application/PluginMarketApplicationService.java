package com.boxai.agent.application;

import com.boxai.agent.api.plugin.PluginCatalogVO;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.WorkspacePluginInstall;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.boxai.security.context.WorkspaceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class PluginMarketApplicationService {

    private final PluginCatalogRepository pluginCatalogRepository;
    private final WorkspacePluginInstallRepository workspacePluginInstallRepository;
    private final PluginCategoryApplicationService pluginCategoryApplicationService;

    public PluginMarketApplicationService(PluginCatalogRepository pluginCatalogRepository,
                                          WorkspacePluginInstallRepository workspacePluginInstallRepository,
                                          PluginCategoryApplicationService pluginCategoryApplicationService) {
        this.pluginCatalogRepository = pluginCatalogRepository;
        this.workspacePluginInstallRepository = workspacePluginInstallRepository;
        this.pluginCategoryApplicationService = pluginCategoryApplicationService;
    }

    public List<PluginCatalogVO> list(String category) {
        pluginCategoryApplicationService.requireActiveCategory(category);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Set<Long> installedIds = workspacePluginInstallRepository.listInstalledPluginIds(workspaceId);
        return pluginCatalogRepository.listByCategory(category).stream()
                .map(plugin -> toVO(plugin, installedIds.contains(plugin.getId())))
                .toList();
    }

    @Transactional
    public void install(Long pluginId) {
        PluginCatalog plugin = requireListedPlugin(pluginId);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Long userId = WorkspaceContext.require().userId();
        if (workspacePluginInstallRepository.findByWorkspaceAndPlugin(workspaceId, pluginId).isPresent()) {
            return;
        }
        WorkspacePluginInstall install = new WorkspacePluginInstall();
        install.setWorkspaceId(workspaceId);
        install.setPluginId(pluginId);
        install.setInstalledBy(userId);
        workspacePluginInstallRepository.save(install);
        pluginCatalogRepository.incrementInstallCount(plugin.getId());
    }

    @Transactional
    public void uninstall(Long pluginId) {
        requireListedPlugin(pluginId);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        workspacePluginInstallRepository.findByWorkspaceAndPlugin(workspaceId, pluginId)
                .ifPresent(install -> {
                    workspacePluginInstallRepository.delete(install.getId());
                    pluginCatalogRepository.decrementInstallCount(pluginId);
                });
    }

    private PluginCatalog requireListedPlugin(Long pluginId) {
        PluginCatalog plugin = pluginCatalogRepository.findById(pluginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件不存在"));
        if (!"LISTED".equals(plugin.getStatus())) {
            throw new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件未上架或已下架");
        }
        return plugin;
    }

    private PluginCatalogVO toVO(PluginCatalog plugin, boolean installed) {
        return new PluginCatalogVO(
                plugin.getId(),
                plugin.getPluginCode(),
                plugin.getCategory(),
                plugin.getTitle(),
                plugin.getDescription(),
                plugin.getInstallCount(),
                installed);
    }
}
