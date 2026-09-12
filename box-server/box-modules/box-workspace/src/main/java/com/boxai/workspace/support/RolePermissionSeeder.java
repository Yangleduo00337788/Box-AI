package com.boxai.workspace.support;

import com.boxai.common.constant.PermissionCodes;
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
                    PermissionCodes.AGENT_CREATE,
                    PermissionCodes.AGENT_READ,
                    PermissionCodes.AGENT_UPDATE,
                    PermissionCodes.AGENT_DELETE,
                    PermissionCodes.AGENT_PUBLISH,
                    PermissionCodes.WORKFLOW_CREATE,
                    PermissionCodes.WORKFLOW_READ,
                    PermissionCodes.WORKFLOW_UPDATE,
                    PermissionCodes.WORKFLOW_EXECUTE,
                    PermissionCodes.WORKFLOW_DELETE,
                    PermissionCodes.KNOWLEDGE_CREATE,
                    PermissionCodes.KNOWLEDGE_READ,
                    PermissionCodes.KNOWLEDGE_UPDATE,
                    PermissionCodes.KNOWLEDGE_UPLOAD,
                    PermissionCodes.KNOWLEDGE_DELETE,
                    PermissionCodes.TOOL_CREATE,
                    PermissionCodes.TOOL_UPDATE,
                    PermissionCodes.TOOL_DELETE,
                    PermissionCodes.TOOL_EXECUTE,
                    PermissionCodes.MODEL_CREATE,
                    PermissionCodes.MODEL_UPDATE,
                    PermissionCodes.API_KEY_MANAGE);
        }
        return List.of(
                PermissionCodes.AGENT_READ,
                PermissionCodes.WORKFLOW_READ,
                PermissionCodes.WORKFLOW_EXECUTE,
                PermissionCodes.KNOWLEDGE_READ,
                PermissionCodes.TOOL_EXECUTE);
    }
}
