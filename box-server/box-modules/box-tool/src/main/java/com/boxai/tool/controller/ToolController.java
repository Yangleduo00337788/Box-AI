package com.boxai.tool.controller;

import com.boxai.common.result.Result;
import com.boxai.tool.api.CreateToolRequest;
import com.boxai.tool.api.ToolTestRequest;
import com.boxai.tool.api.ToolTestResultVO;
import com.boxai.tool.api.ToolVO;
import com.boxai.tool.api.UpdateToolRequest;
import com.boxai.tool.application.ToolApplicationService;
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
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final ToolApplicationService toolApplicationService;

    public ToolController(ToolApplicationService toolApplicationService) {
        this.toolApplicationService = toolApplicationService;
    }

    @GetMapping
    public Result<List<ToolVO>> list() {
        return Result.success(toolApplicationService.list());
    }

    @PostMapping
    public Result<ToolVO> create(@Valid @RequestBody CreateToolRequest request) {
        return Result.success(toolApplicationService.create(request));
    }

    @GetMapping("/{id}")
    public Result<ToolVO> detail(@PathVariable Long id) {
        return Result.success(toolApplicationService.detail(id));
    }

    @PutMapping("/{id}")
    public Result<ToolVO> update(@PathVariable Long id, @Valid @RequestBody UpdateToolRequest request) {
        return Result.success(toolApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        toolApplicationService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    public Result<ToolTestResultVO> test(@PathVariable Long id,
                                         @RequestBody(required = false) ToolTestRequest request) {
        return Result.success(toolApplicationService.test(id, request));
    }
}
