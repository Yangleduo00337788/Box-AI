package com.boxai.tool.controller;

import com.boxai.common.result.Result;
import com.boxai.tool.api.CreateMcpServerRequest;
import com.boxai.tool.api.McpServerVO;
import com.boxai.tool.api.UpdateMcpServerRequest;
import com.boxai.tool.application.McpServerApplicationService;
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
@RequestMapping("/api/v1/mcp-servers")
public class McpServerController {

    private final McpServerApplicationService mcpServerApplicationService;

    public McpServerController(McpServerApplicationService mcpServerApplicationService) {
        this.mcpServerApplicationService = mcpServerApplicationService;
    }

    @GetMapping
    public Result<List<McpServerVO>> list() {
        return Result.success(mcpServerApplicationService.list());
    }

    @PostMapping
    public Result<McpServerVO> create(@Valid @RequestBody CreateMcpServerRequest request) {
        return Result.success(mcpServerApplicationService.create(request));
    }

    @GetMapping("/{id}")
    public Result<McpServerVO> detail(@PathVariable Long id) {
        return Result.success(mcpServerApplicationService.detail(id));
    }

    @PutMapping("/{id}")
    public Result<McpServerVO> update(@PathVariable Long id, @Valid @RequestBody UpdateMcpServerRequest request) {
        return Result.success(mcpServerApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mcpServerApplicationService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/sync")
    public Result<McpServerVO> sync(@PathVariable Long id) {
        return Result.success(mcpServerApplicationService.sync(id));
    }
}
