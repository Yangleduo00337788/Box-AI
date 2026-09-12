package com.boxai.security.permission;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.rbac.Permission;
import com.boxai.domain.rbac.RolePermissionQuery;
import com.boxai.security.context.WorkspaceContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspacePermissionService {

    private final RolePermissionQuery rolePermissionQuery;

    public WorkspacePermissionService(RolePermissionQuery rolePermissionQuery) {
        this.rolePermissionQuery = rolePermissionQuery;
    }

    public boolean hasPermission(String permissionCode) {
        WorkspaceContext context = WorkspaceContext.require();
        if (RoleCodes.TENANT_ADMIN.equals(context.roleCode())) {
            return true;
        }
        return rolePermissionQuery.hasPermission(context.roleId(), permissionCode);
    }

    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权执行此操作");
        }
    }

    public List<String> listCurrentPermissionCodes() {
        WorkspaceContext context = WorkspaceContext.require();
        if (RoleCodes.TENANT_ADMIN.equals(context.roleCode())) {
            return rolePermissionQuery.listAllPermissions().stream()
                    .map(Permission::getPermissionCode)
                    .distinct()
                    .sorted()
                    .toList();
        }
        if (context.roleId() == null) {
            return List.of();
        }
        return rolePermissionQuery.listPermissionCodes(context.roleId());
    }
}
