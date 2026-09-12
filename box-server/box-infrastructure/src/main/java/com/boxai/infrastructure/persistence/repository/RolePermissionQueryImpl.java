package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.rbac.Permission;
import com.boxai.domain.rbac.RolePermissionQuery;
import com.boxai.infrastructure.persistence.entity.PermissionDO;
import com.boxai.infrastructure.persistence.entity.RolePermissionDO;
import com.boxai.infrastructure.persistence.mapper.PermissionMapper;
import com.boxai.infrastructure.persistence.mapper.RolePermissionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class RolePermissionQueryImpl implements RolePermissionQuery {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public RolePermissionQueryImpl(PermissionMapper permissionMapper,
                                   RolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public List<Permission> listAllPermissions() {
        return permissionMapper.selectAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<String> listPermissionCodes(Long roleId) {
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
        for (String code : permissionCodes) {
            Long permissionId = codeToId.get(code);
            if (permissionId == null) {
                continue;
            }
            RolePermissionDO row = new RolePermissionDO();
            row.setRoleId(roleId);
            row.setPermissionId(permissionId);
            row.setCreatedAt(LocalDateTime.now());
            rolePermissionMapper.insert(row);
        }
    }

    @Override
    public boolean hasPermission(Long roleId, String permissionCode) {
        if (roleId == null || permissionCode == null) {
            return false;
        }
        return listPermissionCodes(roleId).contains(permissionCode);
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
