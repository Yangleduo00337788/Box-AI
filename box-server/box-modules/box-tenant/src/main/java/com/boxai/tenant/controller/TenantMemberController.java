package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.SecurityContexts;
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
@RequestMapping("/api/v1/tenant/members")
public class TenantMemberController {

    private final TenantMemberApplicationService tenantMemberApplicationService;

    public TenantMemberController(TenantMemberApplicationService tenantMemberApplicationService) {
        this.tenantMemberApplicationService = tenantMemberApplicationService;
    }

    @GetMapping
    public Result<List<TenantMemberVO>> listMine() {
        LoginUser user = SecurityContexts.currentUser();
        return Result.success(tenantMemberApplicationService.listMyTenantMembers(user.userId()));
    }

    @PostMapping
    public Result<TenantMemberVO> add(@Valid @RequestBody AddTenantMemberRequest request) {
        LoginUser user = SecurityContexts.currentUser();
        return Result.success(tenantMemberApplicationService.addMyTenantMember(user.userId(), request));
    }

    @PutMapping("/{userId}/status")
    public Result<TenantMemberVO> updateStatus(@PathVariable Long userId,
                                               @Valid @RequestBody UpdateTenantMemberStatusRequest request) {
        LoginUser user = SecurityContexts.currentUser();
        return Result.success(tenantMemberApplicationService.updateMyTenantMemberStatus(
                user.userId(), userId, request.status()));
    }
}
