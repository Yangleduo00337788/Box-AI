package com.boxai.common.constant;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInRolePermissionsTest {

    @Test
    void tenantAdminUsesCatalogOfAllPermissions() {
        List<String> all = List.of(PermissionCodes.AGENT_READ, PermissionCodes.ROLE_MANAGE);
        assertEquals(all, BuiltInRolePermissions.codesFor(RoleCodes.TENANT_ADMIN, all));
    }

    @Test
    void developerIncludesPublishButNotRoleManage() {
        List<String> codes = BuiltInRolePermissions.codesFor(RoleCodes.DEVELOPER, List.of());
        assertTrue(codes.contains(PermissionCodes.AGENT_PUBLISH));
        assertTrue(codes.contains(PermissionCodes.API_KEY_MANAGE));
        assertEquals(-1, codes.indexOf(PermissionCodes.ROLE_MANAGE));
    }

    @Test
    void unknownRoleCodeFallsBackToMember() {
        List<String> codes = BuiltInRolePermissions.codesFor("CUSTOM_X", List.of());
        assertEquals(BuiltInRolePermissions.memberCodes(), codes);
    }
}
