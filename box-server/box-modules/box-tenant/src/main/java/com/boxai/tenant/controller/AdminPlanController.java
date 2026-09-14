package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.tenant.api.CreatePlanRequest;
import com.boxai.tenant.api.PlanVO;
import com.boxai.tenant.api.UpdatePlanRequest;
import com.boxai.tenant.application.PlanApplicationService;
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
@RequestMapping("/api/v1/admin/plans")
public class AdminPlanController {

    private final PlanApplicationService planApplicationService;

    public AdminPlanController(PlanApplicationService planApplicationService) {
        this.planApplicationService = planApplicationService;
    }

    @GetMapping
    public Result<List<PlanVO>> list() {
        return Result.success(planApplicationService.listAll());
    }

    @GetMapping("/{id}")
    public Result<PlanVO> detail(@PathVariable Long id) {
        return Result.success(planApplicationService.detail(id));
    }

    @PostMapping
    public Result<PlanVO> create(@Valid @RequestBody CreatePlanRequest request) {
        return Result.success(planApplicationService.create(request));
    }

    @PutMapping("/{id}")
    public Result<PlanVO> update(@PathVariable Long id, @Valid @RequestBody UpdatePlanRequest request) {
        return Result.success(planApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        planApplicationService.delete(id);
        return Result.success(null);
    }
}
