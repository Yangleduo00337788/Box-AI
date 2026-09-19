package com.boxai.security.permission;

import com.boxai.common.constant.PlatformAdminRoles;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PlatformAdminAccess {

    private static final Set<String> ALL_ROLES = Set.of(
            PlatformAdminRoles.SUPER_ADMIN,
            PlatformAdminRoles.OPS,
            PlatformAdminRoles.FINANCE,
            PlatformAdminRoles.CONTENT);

    private static final List<Rule> RULES = List.of(
            new Rule("/api/v1/admin/auth", null, ALL_ROLES),
            new Rule("/api/v1/admin/assets", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.CONTENT)),
            new Rule("/api/v1/admin/users", Set.of("POST", "PUT"), Set.of(PlatformAdminRoles.SUPER_ADMIN)),
            new Rule("/api/v1/admin/users", null, Set.of(PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS)),
            new Rule("/api/v1/admin/billing", null, Set.of(PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.FINANCE)),
            new Rule("/api/v1/admin/system", null, Set.of(PlatformAdminRoles.SUPER_ADMIN)),
            new Rule("/api/v1/admin/analytics", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.FINANCE)),
            new Rule("/api/v1/admin/tenants", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.FINANCE)),
            new Rule("/api/v1/admin/plans", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.FINANCE)),
            new Rule("/api/v1/admin/audit-logs", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.FINANCE)),
            new Rule("/api/v1/admin/message-feedbacks", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.CONTENT)),
            new Rule("/api/v1/admin/notifications", null, ALL_ROLES),
            new Rule("/api/v1/admin/platform", null, Set.of(PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS)),
            new Rule("/api/v1/admin/ops", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.CONTENT)),
            new Rule("/api/v1/admin/plugins", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.CONTENT)),
            new Rule("/api/v1/admin/plugin-categories", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.CONTENT)),
            new Rule("/api/v1/admin/agent-templates", null, Set.of(
                    PlatformAdminRoles.SUPER_ADMIN, PlatformAdminRoles.OPS, PlatformAdminRoles.CONTENT)));

    private PlatformAdminAccess() {
    }

    public static boolean isPublicPath(String uri) {
        String path = normalize(uri);
        return path.startsWith("/api/v1/admin/auth/login")
                || path.startsWith("/api/v1/admin/auth/verification-code")
                || path.startsWith("/api/v1/admin/auth/password/reset");
    }

    public static boolean allows(String platformAdminRole, String method, String uri) {
        String path = normalize(uri);
        if (isPublicPath(path)) {
            return true;
        }
        String role = normalizeRole(platformAdminRole);
        if (PlatformAdminRoles.SUPER_ADMIN.equals(role)) {
            return true;
        }
        String httpMethod = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
        for (Rule rule : RULES) {
            if (path.startsWith(rule.prefix)
                    && (rule.methods == null || rule.methods.contains(httpMethod))) {
                return rule.roles.contains(role);
            }
        }
        return false;
    }

    public static String normalizeRole(String platformAdminRole) {
        if (platformAdminRole == null || platformAdminRole.isBlank()) {
            return PlatformAdminRoles.SUPER_ADMIN;
        }
        return platformAdminRole.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalize(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        int index = uri.indexOf("/api/v1/admin");
        String path = index >= 0 ? uri.substring(index) : uri;
        int query = path.indexOf('?');
        return query >= 0 ? path.substring(0, query) : path;
    }

    private record Rule(String prefix, Set<String> methods, Set<String> roles) {
    }
}
