package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.tenant.api.AddTenantMemberRequest;
import com.boxai.tenant.api.TenantMemberVO;
import com.boxai.tenant.api.UpdateTenantMemberStatusRequest;
import com.boxai.tenant.application.TenantMemberApplicationService;
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
@RequestMapping("/api/v1/admin/tenants/{tenantId}/members")
public class AdminTenantMemberController {

    private final TenantMemberApplicationService tenantMemberApplicationService;

    public AdminTenantMemberController(TenantMemberApplicationService tenantMemberApplicationService) {
        this.tenantMemberApplicationService = tenantMemberApplicationService;
    }

    @GetMapping
    public Result<List<TenantMemberVO>> list(@PathVariable Long tenantId) {
        return Result.success(tenantMemberApplicationService.listMembers(tenantId));
    }

    @PostMapping
    public Result<TenantMemberVO> add(@PathVariable Long tenantId,
                                      @Valid @RequestBody AddTenantMemberRequest request) {
        return Result.success(tenantMemberApplicationService.addMember(tenantId, request));
    }

    @PutMapping("/{userId}/status")
    public Result<TenantMemberVO> updateStatus(@PathVariable Long tenantId,
                                               @PathVariable Long userId,
                                               @Valid @RequestBody UpdateTenantMemberStatusRequest request) {
        return Result.success(tenantMemberApplicationService.updateMemberStatus(tenantId, userId, request.status()));
    }
}
