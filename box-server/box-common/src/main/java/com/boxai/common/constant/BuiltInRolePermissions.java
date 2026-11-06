package com.boxai.common.constant;

import java.util.List;

/**
 * Workspace built-in roles keep permission catalogs in code so registration
 * does not insert {@code sys_role_permission} rows under concurrent load.
 */
public final class BuiltInRolePermissions {

    private BuiltInRolePermissions() {
    }

    public static List<String> codesFor(String roleCode, List<String> allPermissionCodes) {
        if (RoleCodes.TENANT_ADMIN.equals(roleCode) || RoleCodes.SUPER_ADMIN.equals(roleCode)) {
            return allPermissionCodes == null ? List.of() : List.copyOf(allPermissionCodes);
        }
        if (RoleCodes.DEVELOPER.equals(roleCode)) {
            return developerCodes();
        }
        return memberCodes();
    }

    public static List<String> developerCodes() {
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

    public static List<String> memberCodes() {
        return List.of(
                PermissionCodes.AGENT_READ,
                PermissionCodes.WORKFLOW_READ,
                PermissionCodes.WORKFLOW_EXECUTE,
                PermissionCodes.KNOWLEDGE_READ,
                PermissionCodes.TOOL_EXECUTE);
    }
}
