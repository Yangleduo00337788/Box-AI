package com.boxai.workspace.support;

import com.boxai.common.constant.RoleCodes;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RolePermissionQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RolePermissionSeeder {

    private final RolePermissionQuery rolePermissionQuery;

    public RolePermissionSeeder(RolePermissionQuery rolePermissionQuery) {
        this.rolePermissionQuery = rolePermissionQuery;
    }

    public void seedBuiltInRole(Role role) {
        if (role == null || role.getId() == null) {
            return;
        }
        if (!rolePermissionQuery.listPermissionCodes(role.getId()).isEmpty()) {
            return;
        }
        rolePermissionQuery.replacePermissions(role.getId(), defaultPermissions(role.getRoleCode()));
    }

    private List<String> defaultPermissions(String roleCode) {
        if (RoleCodes.TENANT_ADMIN.equals(roleCode)) {
            return rolePermissionQuery.listAllPermissions().stream()
                    .map(item -> item.getPermissionCode())
                    .toList();
        }
        if (RoleCodes.DEVELOPER.equals(roleCode)) {
            return List.of(
                    "agent:create", "agent:read", "agent:update", "agent:publish",
                    "workflow:create", "workflow:update", "workflow:execute",
                    "knowledge:create", "knowledge:upload",
                    "tool:create", "tool:execute",
                    "model:create", "model:update");
        }
        return List.of("agent:read", "workflow:execute", "tool:execute");
    }
}
