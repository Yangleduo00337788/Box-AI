package com.boxai.infrastructure.persistence.repository;

import com.boxai.common.constant.BuiltInRolePermissions;
import com.boxai.domain.rbac.Permission;
import com.boxai.domain.rbac.RolePermissionQuery;
import com.boxai.infrastructure.persistence.entity.PermissionDO;
import com.boxai.infrastructure.persistence.entity.RoleDO;
import com.boxai.infrastructure.persistence.entity.RolePermissionDO;
import com.boxai.infrastructure.persistence.mapper.PermissionMapper;
import com.boxai.infrastructure.persistence.mapper.RoleMapper;
import com.boxai.infrastructure.persistence.mapper.RolePermissionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class RolePermissionQueryImpl implements RolePermissionQuery {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleMapper roleMapper;

    public RolePermissionQueryImpl(PermissionMapper permissionMapper,
                                   RolePermissionMapper rolePermissionMapper,
                                   RoleMapper roleMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public List<Permission> listAllPermissions() {
        return permissionMapper.selectAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<String> listPermissionCodes(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        RoleDO role = roleMapper.selectOneById(roleId);
        if (isBuiltIn(role)) {
            return BuiltInRolePermissions.codesFor(role.getRoleCode(), allPermissionCodes());
        }
        List<Long> permissionIds = rolePermissionMapper.selectListByQuery(
                        QueryWrapper.create().eq("role_id", roleId))
                .stream()
                .map(RolePermissionDO::getPermissionId)
                .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectListByQuery(
                        QueryWrapper.create().in("id", permissionIds))
                .stream()
                .map(PermissionDO::getPermissionCode)
                .toList();
    }

    @Override
    @Transactional
    public void replacePermissions(Long roleId, List<String> permissionCodes) {
        rolePermissionMapper.deleteByQuery(QueryWrapper.create().eq("role_id", roleId));
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return;
        }
        Map<String, Long> codeToId = permissionMapper.selectAll().stream()
                .collect(Collectors.toMap(PermissionDO::getPermissionCode, PermissionDO::getId, (a, b) -> a, HashMap::new));
        LocalDateTime now = LocalDateTime.now();
        List<RolePermissionDO> rows = permissionCodes.stream()
                .map(codeToId::get)
                .filter(Objects::nonNull)
                .distinct()
                .map(permissionId -> {
                    RolePermissionDO row = new RolePermissionDO();
                    row.setRoleId(roleId);
                    row.setPermissionId(permissionId);
                    row.setCreatedAt(now);
                    return row;
                })
                .toList();
        if (!rows.isEmpty()) {
            rolePermissionMapper.insertBatch(new ArrayList<>(rows));
        }
    }

    @Override
    public boolean hasPermission(Long roleId, String permissionCode) {
        if (roleId == null || permissionCode == null) {
            return false;
        }
        return listPermissionCodes(roleId).contains(permissionCode);
    }

    private List<String> allPermissionCodes() {
        return permissionMapper.selectAll().stream()
                .map(PermissionDO::getPermissionCode)
                .toList();
    }

    private static boolean isBuiltIn(RoleDO role) {
        return role != null && role.getBuiltIn() != null && role.getBuiltIn() == 1;
    }

    private Permission toDomain(PermissionDO row) {
        Permission permission = new Permission();
        permission.setId(row.getId());
        permission.setPermissionCode(row.getPermissionCode());
        permission.setPermissionName(row.getPermissionName());
        permission.setResourceType(row.getResourceType());
        permission.setAction(row.getAction());
        return permission;
    }
}
