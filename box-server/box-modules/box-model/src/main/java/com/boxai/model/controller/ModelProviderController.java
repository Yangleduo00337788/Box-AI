package com.boxai.model.controller;

import com.boxai.common.result.Result;
import com.boxai.model.api.CreateProviderRequest;
import com.boxai.model.api.ProviderVO;
import com.boxai.model.application.ModelApplicationService;
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
@RequestMapping("/api/v1/model-providers")
public class ModelProviderController {

    private final ModelApplicationService modelApplicationService;

    public ModelProviderController(ModelApplicationService modelApplicationService) {
        this.modelApplicationService = modelApplicationService;
    }

    @GetMapping
    public Result<List<ProviderVO>> list() {
        return Result.success(modelApplicationService.listProviders());
    }

    @PostMapping
    public Result<ProviderVO> create(@Valid @RequestBody CreateProviderRequest request) {
        return Result.success(modelApplicationService.createProvider(request));
    }

    @PutMapping("/{id}")
    public Result<ProviderVO> update(@PathVariable Long id, @Valid @RequestBody CreateProviderRequest request) {
        return Result.success(modelApplicationService.updateProvider(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelApplicationService.deleteProvider(id);
        return Result.success();
    }
}
