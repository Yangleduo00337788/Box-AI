package com.boxai.workspace.support;

import com.boxai.common.constant.RoleCodes;
import com.boxai.domain.rbac.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RolePermissionSeederTest {

    @Test
    void seedBuiltInRoleDoesNotThrow() {
        RolePermissionSeeder seeder = new RolePermissionSeeder();
        Role role = new Role();
        role.setId(1L);
        role.setRoleCode(RoleCodes.TENANT_ADMIN);
        role.setBuiltIn(1);
        assertDoesNotThrow(() -> seeder.seedBuiltInRole(role));
        assertDoesNotThrow(() -> seeder.seedBuiltInRole(null));
    }
}
