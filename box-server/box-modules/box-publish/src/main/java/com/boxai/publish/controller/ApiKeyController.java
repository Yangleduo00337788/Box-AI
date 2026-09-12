package com.boxai.publish.controller;

import com.boxai.common.result.Result;
import com.boxai.publish.api.ApiKeyVO;
import com.boxai.publish.api.CreateApiKeyRequest;
import com.boxai.publish.api.CreateApiKeyResponse;
import com.boxai.publish.application.ApiKeyApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyApplicationService apiKeyApplicationService;

    public ApiKeyController(ApiKeyApplicationService apiKeyApplicationService) {
        this.apiKeyApplicationService = apiKeyApplicationService;
    }

    @GetMapping
    public Result<List<ApiKeyVO>> list() {
        return Result.success(apiKeyApplicationService.list());
    }

    @PostMapping
    public Result<CreateApiKeyResponse> create(@Valid @RequestBody CreateApiKeyRequest request) {
        return Result.success(apiKeyApplicationService.create(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        apiKeyApplicationService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/disable")
    public Result<ApiKeyVO> disable(@PathVariable Long id) {
        return Result.success(apiKeyApplicationService.disable(id));
    }

    @PostMapping("/{id}/enable")
    public Result<ApiKeyVO> enable(@PathVariable Long id) {
        return Result.success(apiKeyApplicationService.enable(id));
    }

    @PostMapping("/{id}/rotate")
    public Result<CreateApiKeyResponse> rotate(@PathVariable Long id) {
        return Result.success(apiKeyApplicationService.rotate(id));
    }
}
