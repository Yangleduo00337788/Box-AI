package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.tenant.api.AssignTenantPlanRequest;
import com.boxai.tenant.api.CreateTenantRequest;
import com.boxai.tenant.api.QuotaSnapshotVO;
import com.boxai.tenant.api.TenantVO;
import com.boxai.tenant.api.UpdateTenantStatusRequest;
import com.boxai.tenant.application.QuotaApplicationService;
import com.boxai.tenant.application.TenantApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tenants")
public class AdminTenantController {

    private final TenantApplicationService tenantApplicationService;
    private final QuotaApplicationService quotaApplicationService;

    public AdminTenantController(TenantApplicationService tenantApplicationService,
                                 QuotaApplicationService quotaApplicationService) {
        this.tenantApplicationService = tenantApplicationService;
        this.quotaApplicationService = quotaApplicationService;
    }

    @GetMapping
    public Result<List<TenantVO>> list() {
        return Result.success(tenantApplicationService.listAll());
    }

    @PostMapping
    public Result<TenantVO> create(@Valid @RequestBody CreateTenantRequest request) {
        return Result.success(tenantApplicationService.create(request));
    }

    @PutMapping("/{id}/status")
    public Result<TenantVO> updateStatus(@PathVariable Long id,
                                         @Valid @RequestBody UpdateTenantStatusRequest request) {
        return Result.success(tenantApplicationService.updateStatus(id, request.status()));
    }

    @PutMapping("/{id}/plan")
    public Result<TenantVO> assignPlan(@PathVariable Long id,
                                       @Valid @RequestBody AssignTenantPlanRequest request) {
        return Result.success(tenantApplicationService.assignPlan(id, request.planId()));
    }

    @GetMapping("/{id}/quota")
    public Result<QuotaSnapshotVO> quota(@PathVariable Long id) {
        return Result.success(quotaApplicationService.getQuotaForTenant(id));
    }
}
