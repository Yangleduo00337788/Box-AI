package com.boxai.workspace.application;

import com.boxai.common.constant.AuditActions;
import com.boxai.common.constant.AuditResourceTypes;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.security.audit.AuditLogService;
import com.boxai.domain.rbac.Permission;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RolePermissionQuery;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.workspace.api.CreateRoleRequest;
import com.boxai.workspace.api.PermissionVO;
import com.boxai.workspace.api.RoleVO;
import com.boxai.workspace.api.UpdateRoleRequest;
import com.boxai.workspace.support.RolePermissionSeeder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final RolePermissionQuery rolePermissionQuery;
    private final RolePermissionSeeder rolePermissionSeeder;
    private final WorkspacePermissionService workspacePermissionService;
    private final AuditLogService auditLogService;

    public RoleApplicationService(RoleRepository roleRepository,
                                  RolePermissionQuery rolePermissionQuery,
                                  RolePermissionSeeder rolePermissionSeeder,
                                  WorkspacePermissionService workspacePermissionService,
                                  AuditLogService auditLogService) {
        this.roleRepository = roleRepository;
        this.rolePermissionQuery = rolePermissionQuery;
        this.rolePermissionSeeder = rolePermissionSeeder;
        this.workspacePermissionService = workspacePermissionService;
        this.auditLogService = auditLogService;
    }

    public List<RoleVO> list() {
        workspacePermissionService.requirePermission(PermissionCodes.ROLE_MANAGE);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        return roleRepository.listByWorkspace(workspaceId).stream()
                .map(this::ensureSeededAndToVO)
                .toList();
    }

    public List<PermissionVO> listPermissions() {
        workspacePermissionService.requirePermission(PermissionCodes.ROLE_MANAGE);
        return rolePermissionQuery.listAllPermissions().stream().map(this::toPermissionVO).toList();
    }

    @Transactional
    public RoleVO create(CreateRoleRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.ROLE_MANAGE);
        Role role = new Role();
        role.setWorkspaceId(WorkspaceContext.require().workspaceId());
        role.setRoleCode("CUSTOM_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        role.setRoleName(request.roleName().trim());
        role.setDescription(trimToNull(request.description()));
        role.setBuiltIn(0);
        roleRepository.save(role);
        rolePermissionQuery.replacePermissions(role.getId(), request.permissionCodes());
        auditLogService.recordSuccess(
                AuditActions.ROLE_CREATE,
                AuditResourceTypes.ROLE,
                role.getId(),
                role.getRoleName(),
                null);
        return toVO(role);
    }

    @Transactional
    public RoleVO update(Long id, UpdateRoleRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.ROLE_MANAGE);
        Role role = requireRole(id);
        if (role.getBuiltIn() != null && role.getBuiltIn() == 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内置角色不可编辑");
        }
        role.setRoleName(request.roleName().trim());
        role.setDescription(trimToNull(request.description()));
        roleRepository.update(role);
        rolePermissionQuery.replacePermissions(role.getId(), request.permissionCodes());
        auditLogService.recordSuccess(
                AuditActions.ROLE_UPDATE,
                AuditResourceTypes.ROLE,
                role.getId(),
                role.getRoleName(),
                null);
        return toVO(role);
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission(PermissionCodes.ROLE_MANAGE);
        Role role = requireRole(id);
        if (role.getBuiltIn() != null && role.getBuiltIn() == 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内置角色不可删除");
        }
        roleRepository.delete(id);
        auditLogService.recordSuccess(
                AuditActions.ROLE_DELETE,
                AuditResourceTypes.ROLE,
                id,
                role.getRoleName(),
                null);
    }

    private RoleVO ensureSeededAndToVO(Role role) {
        rolePermissionSeeder.seedBuiltInRole(role);
        return toVO(role);
    }

    private RoleVO toVO(Role role) {
        return new RoleVO(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getDescription(),
                role.getBuiltIn() != null && role.getBuiltIn() == 1,
                rolePermissionQuery.listPermissionCodes(role.getId()));
    }

    private PermissionVO toPermissionVO(Permission permission) {
        return new PermissionVO(
                permission.getId(),
                permission.getPermissionCode(),
                permission.getPermissionName(),
                permission.getResourceType(),
                permission.getAction());
    }

    private Role requireRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));
        if (!WorkspaceContext.require().workspaceId().equals(role.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该角色");
        }
        return role;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
