package com.boxai.agent.controller;

import com.boxai.agent.api.plugin.PluginCatalogAssetVO;
import com.boxai.agent.application.PluginCatalogAssetApplicationService;
import com.boxai.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/plugins/catalog-assets")
public class AdminPluginCatalogAssetController {

    private final PluginCatalogAssetApplicationService pluginCatalogAssetApplicationService;

    public AdminPluginCatalogAssetController(PluginCatalogAssetApplicationService pluginCatalogAssetApplicationService) {
        this.pluginCatalogAssetApplicationService = pluginCatalogAssetApplicationService;
    }

    @PostMapping
    public Result<PluginCatalogAssetVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(pluginCatalogAssetApplicationService.upload(file));
    }
}
