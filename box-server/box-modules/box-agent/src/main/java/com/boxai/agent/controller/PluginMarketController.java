package com.boxai.agent.controller;

import com.boxai.agent.api.plugin.CreateWorkspacePluginRequest;
import com.boxai.agent.api.plugin.PluginCatalogVO;
import com.boxai.agent.api.plugin.PluginCategoryVO;
import com.boxai.agent.application.PluginCategoryApplicationService;
import com.boxai.agent.application.PluginMarketApplicationService;
import com.boxai.agent.application.WorkspacePluginCatalogApplicationService;
import com.boxai.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
public class PluginMarketController {

    private final PluginMarketApplicationService pluginMarketApplicationService;
    private final PluginCategoryApplicationService pluginCategoryApplicationService;
    private final WorkspacePluginCatalogApplicationService workspacePluginCatalogApplicationService;

    public PluginMarketController(PluginMarketApplicationService pluginMarketApplicationService,
                                  PluginCategoryApplicationService pluginCategoryApplicationService,
                                  WorkspacePluginCatalogApplicationService workspacePluginCatalogApplicationService) {
        this.pluginMarketApplicationService = pluginMarketApplicationService;
        this.pluginCategoryApplicationService = pluginCategoryApplicationService;
        this.workspacePluginCatalogApplicationService = workspacePluginCatalogApplicationService;
    }

    @GetMapping("/plugin-categories")
    public Result<List<PluginCategoryVO>> listCategories() {
        return Result.success(pluginCategoryApplicationService.listActive());
    }

    @GetMapping("/plugins")
    public Result<List<PluginCatalogVO>> listPlugins(@RequestParam(required = false) String category) {
        return Result.success(pluginMarketApplicationService.list(category));
    }

    @PostMapping("/workspace-plugins")
    public Result<PluginCatalogVO> createWorkspacePlugin(@Valid @RequestBody CreateWorkspacePluginRequest request) {
        return Result.success(workspacePluginCatalogApplicationService.create(request));
    }

    @PostMapping("/plugins/{id}/install")
    public Result<Void> install(@PathVariable Long id) {
        pluginMarketApplicationService.install(id);
        return Result.success(null);
    }

    @DeleteMapping("/plugins/{id}/install")
    public Result<Void> uninstall(@PathVariable Long id) {
        pluginMarketApplicationService.uninstall(id);
        return Result.success(null);
    }
}
