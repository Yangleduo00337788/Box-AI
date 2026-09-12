package com.boxai.workspace.controller;

import com.boxai.common.result.Result;
import com.boxai.workspace.api.CreateRoleRequest;
import com.boxai.workspace.api.PermissionVO;
import com.boxai.workspace.api.RoleVO;
import com.boxai.workspace.api.UpdateRoleRequest;
import com.boxai.workspace.application.RoleApplicationService;
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
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleApplicationService roleApplicationService;

    public RoleController(RoleApplicationService roleApplicationService) {
        this.roleApplicationService = roleApplicationService;
    }

    @GetMapping
    public Result<List<RoleVO>> list() {
        return Result.success(roleApplicationService.list());
    }

    @GetMapping("/permissions")
    public Result<List<PermissionVO>> listPermissions() {
        return Result.success(roleApplicationService.listPermissions());
    }

    @PostMapping
    public Result<RoleVO> create(@Valid @RequestBody CreateRoleRequest request) {
        return Result.success(roleApplicationService.create(request));
    }

    @PutMapping("/{id}")
    public Result<RoleVO> update(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return Result.success(roleApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleApplicationService.delete(id);
        return Result.success();
    }
}
