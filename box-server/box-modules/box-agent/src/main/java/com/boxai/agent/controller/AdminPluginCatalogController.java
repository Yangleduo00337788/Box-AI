package com.boxai.agent.controller;

import com.boxai.agent.api.plugin.AdminPluginCatalogVO;
import com.boxai.agent.api.plugin.CreatePluginCatalogRequest;
import com.boxai.agent.api.plugin.UpdatePluginCatalogRequest;
import com.boxai.agent.application.AdminPluginCatalogApplicationService;
import com.boxai.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/plugins")
public class AdminPluginCatalogController {

    private final AdminPluginCatalogApplicationService adminPluginCatalogApplicationService;

    public AdminPluginCatalogController(AdminPluginCatalogApplicationService adminPluginCatalogApplicationService) {
        this.adminPluginCatalogApplicationService = adminPluginCatalogApplicationService;
    }

    @GetMapping
    public Result<List<AdminPluginCatalogVO>> list(@RequestParam(required = false) String category) {
        return Result.success(adminPluginCatalogApplicationService.list(category));
    }

    @PostMapping
    public Result<AdminPluginCatalogVO> create(@Valid @RequestBody CreatePluginCatalogRequest request) {
        return Result.success(adminPluginCatalogApplicationService.create(request));
    }

    @PutMapping("/{id}")
    public Result<AdminPluginCatalogVO> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdatePluginCatalogRequest request) {
        return Result.success(adminPluginCatalogApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminPluginCatalogApplicationService.delete(id);
        return Result.success(null);
    }
}
