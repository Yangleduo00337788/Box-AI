package com.boxai.trace.controller;

import com.boxai.common.result.Result;
import com.boxai.trace.api.ExecutionVO;
import com.boxai.trace.application.ExecutionApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionController {

    private final ExecutionApplicationService executionApplicationService;

    public ExecutionController(ExecutionApplicationService executionApplicationService) {
        this.executionApplicationService = executionApplicationService;
    }

    @GetMapping
    public Result<List<ExecutionVO>> list(@RequestParam(defaultValue = "50") int limit) {
        return Result.success(executionApplicationService.list(limit));
    }

    @GetMapping("/{id}")
    public Result<ExecutionVO> detail(@PathVariable Long id) {
        return Result.success(executionApplicationService.detail(id));
    }
}
