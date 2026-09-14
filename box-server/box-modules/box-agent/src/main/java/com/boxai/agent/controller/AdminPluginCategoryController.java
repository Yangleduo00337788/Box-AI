package com.boxai.agent.controller;

import com.boxai.agent.api.plugin.CreatePluginCategoryRequest;
import com.boxai.agent.api.plugin.PluginCategoryVO;
import com.boxai.agent.api.plugin.UpdatePluginCategoryRequest;
import com.boxai.agent.application.PluginCategoryApplicationService;
import com.boxai.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/plugin-categories")
public class AdminPluginCategoryController {

    private final PluginCategoryApplicationService pluginCategoryApplicationService;

    public AdminPluginCategoryController(PluginCategoryApplicationService pluginCategoryApplicationService) {
        this.pluginCategoryApplicationService = pluginCategoryApplicationService;
    }

    @GetMapping
    public Result<List<PluginCategoryVO>> list() {
        return Result.success(pluginCategoryApplicationService.listAll());
    }

    @PostMapping
    public Result<PluginCategoryVO> create(@Valid @RequestBody CreatePluginCategoryRequest request) {
        return Result.success(pluginCategoryApplicationService.create(
                request.categoryCode(),
                request.label(),
                request.description(),
                request.sortOrder()));
    }

    @PutMapping("/{categoryCode}")
    public Result<PluginCategoryVO> update(@PathVariable String categoryCode,
                                         @Valid @RequestBody UpdatePluginCategoryRequest request) {
        return Result.success(pluginCategoryApplicationService.update(
                categoryCode,
                request.label(),
                request.description(),
                request.sortOrder(),
                request.status()));
    }

    @DeleteMapping("/{categoryCode}")
    public Result<Void> delete(@PathVariable String categoryCode) {
        pluginCategoryApplicationService.delete(categoryCode);
        return Result.success(null);
    }
}
