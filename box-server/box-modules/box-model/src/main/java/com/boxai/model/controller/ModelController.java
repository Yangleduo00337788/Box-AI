package com.boxai.model.controller;

import com.boxai.common.result.Result;
import com.boxai.model.api.CreateModelRequest;
import com.boxai.model.api.ModelVO;
import com.boxai.model.api.TestChatRequest;
import com.boxai.model.api.TestChatVO;
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
@RequestMapping("/api/v1/models")
public class ModelController {

    private final ModelApplicationService modelApplicationService;

    public ModelController(ModelApplicationService modelApplicationService) {
        this.modelApplicationService = modelApplicationService;
    }

    @GetMapping
    public Result<List<ModelVO>> list() {
        return Result.success(modelApplicationService.listModels());
    }

    @PostMapping
    public Result<ModelVO> create(@Valid @RequestBody CreateModelRequest request) {
        return Result.success(modelApplicationService.createModel(request));
    }

    @PutMapping("/{id}")
    public Result<ModelVO> update(@PathVariable Long id, @Valid @RequestBody CreateModelRequest request) {
        return Result.success(modelApplicationService.updateModel(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelApplicationService.deleteModel(id);
        return Result.success();
    }

    @PostMapping("/{id}/chat")
    public Result<TestChatVO> testChat(@PathVariable Long id, @Valid @RequestBody TestChatRequest request) {
        return Result.success(modelApplicationService.testChat(id, request.message()));
    }
}
