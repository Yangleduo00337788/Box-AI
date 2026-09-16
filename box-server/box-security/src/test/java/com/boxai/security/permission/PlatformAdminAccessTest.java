package com.boxai.security.permission;

import com.boxai.common.constant.PlatformAdminRoles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformAdminAccessTest {

    @Test
    void superAdminCanAccessSystemConfig() {
        assertTrue(PlatformAdminAccess.allows(
                PlatformAdminRoles.SUPER_ADMIN, "PUT", "/api/v1/admin/system/config"));
    }

    @Test
    void financeCanReadBillingButNotUsersWrite() {
        assertTrue(PlatformAdminAccess.allows(
                PlatformAdminRoles.FINANCE, "GET", "/api/v1/admin/billing/invoices"));
        assertFalse(PlatformAdminAccess.allows(
                PlatformAdminRoles.FINANCE, "GET", "/api/v1/admin/users"));
        assertFalse(PlatformAdminAccess.allows(
                PlatformAdminRoles.FINANCE, "POST", "/api/v1/admin/users"));
    }

    @Test
    void opsCannotCreateAdminsOrOpenBilling() {
        assertTrue(PlatformAdminAccess.allows(
                PlatformAdminRoles.OPS, "GET", "/api/v1/admin/users"));
        assertFalse(PlatformAdminAccess.allows(
                PlatformAdminRoles.OPS, "POST", "/api/v1/admin/users"));
        assertFalse(PlatformAdminAccess.allows(
                PlatformAdminRoles.OPS, "GET", "/api/v1/admin/billing/invoices"));
        assertFalse(PlatformAdminAccess.allows(
                PlatformAdminRoles.OPS, "GET", "/api/v1/admin/system/config"));
    }

    @Test
    void contentCanManageCatalogButNotTenants() {
        assertTrue(PlatformAdminAccess.allows(
                PlatformAdminRoles.CONTENT, "PUT", "/api/v1/admin/plugins/1/review"));
        assertTrue(PlatformAdminAccess.allows(
                PlatformAdminRoles.CONTENT, "PUT", "/api/v1/admin/agent-templates/1/review"));
        assertTrue(PlatformAdminAccess.allows(
                PlatformAdminRoles.CONTENT, "GET", "/api/v1/admin/ops/placements"));
        assertFalse(PlatformAdminAccess.allows(
                PlatformAdminRoles.CONTENT, "GET", "/api/v1/admin/tenants"));
        assertFalse(PlatformAdminAccess.allows(
                PlatformAdminRoles.CONTENT, "GET", "/api/v1/admin/analytics/overview"));
    }

    @Test
    void blankRoleKeepsLegacyFullAccessUntilRelogin() {
        assertTrue(PlatformAdminAccess.allows(null, "PUT", "/api/v1/admin/system/config"));
    }
}
