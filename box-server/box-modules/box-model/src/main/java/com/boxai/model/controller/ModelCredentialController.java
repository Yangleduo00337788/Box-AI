package com.boxai.model.controller;

import com.boxai.common.result.Result;
import com.boxai.model.api.CreateCredentialRequest;
import com.boxai.model.api.CredentialVO;
import com.boxai.model.application.ModelApplicationService;
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
@RequestMapping("/api/v1/model-api-keys")
public class ModelCredentialController {

    private final ModelApplicationService modelApplicationService;

    public ModelCredentialController(ModelApplicationService modelApplicationService) {
        this.modelApplicationService = modelApplicationService;
    }

    @GetMapping
    public Result<List<CredentialVO>> list() {
        return Result.success(modelApplicationService.listCredentials());
    }

    @PostMapping
    public Result<CredentialVO> create(@Valid @RequestBody CreateCredentialRequest request) {
        return Result.success(modelApplicationService.createCredential(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelApplicationService.deleteCredential(id);
        return Result.success();
    }
}
