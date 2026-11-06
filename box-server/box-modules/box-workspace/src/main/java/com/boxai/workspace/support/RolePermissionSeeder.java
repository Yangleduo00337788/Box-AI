package com.boxai.workspace.support;

import com.boxai.domain.rbac.Role;
import org.springframework.stereotype.Component;

@Component
public class RolePermissionSeeder {

    /**
     * Built-in role permissions are resolved from {@code BuiltInRolePermissions}.
     * Do not insert {@code sys_role_permission} rows here: concurrent registration
     * deadlocks on {@code uk_role_permission}.
     */
    public void seedBuiltInRole(Role role) {
        // Intentionally empty.
    }
}
