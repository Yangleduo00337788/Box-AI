package com.boxai.domain.rbac;

import java.util.List;

public interface RolePermissionQuery {

    List<Permission> listAllPermissions();

    List<String> listPermissionCodes(Long roleId);

    void replacePermissions(Long roleId, List<String> permissionCodes);

    boolean hasPermission(Long roleId, String permissionCode);
}
