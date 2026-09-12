package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.infrastructure.persistence.entity.RoleDO;
import com.boxai.infrastructure.persistence.mapper.RoleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;

    public RoleRepositoryImpl(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public Optional<Role> findByWorkspaceAndCode(Long workspaceId, String roleCode) {
        RoleDO row = roleMapper.selectOneByQuery(
                QueryWrapper.create().eq("workspace_id", workspaceId).eq("role_code", roleCode));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return Optional.ofNullable(roleMapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<Role> listByWorkspace(Long workspaceId) {
        return roleMapper.selectListByQuery(QueryWrapper.create().eq("workspace_id", workspaceId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Role save(Role role) {
        RoleDO row = new RoleDO();
        row.setWorkspaceId(role.getWorkspaceId());
        row.setRoleCode(role.getRoleCode());
        row.setRoleName(role.getRoleName());
        row.setDescription(role.getDescription());
        row.setBuiltIn(role.getBuiltIn() == null ? 1 : role.getBuiltIn());
        row.setStatus(1);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        roleMapper.insert(row);
        role.setId(row.getId());
        return role;
    }

    @Override
    public void update(Role role) {
        RoleDO row = new RoleDO();
        row.setId(role.getId());
        row.setRoleName(role.getRoleName());
        row.setDescription(role.getDescription());
        row.setUpdatedAt(LocalDateTime.now());
        roleMapper.update(row);
    }

    @Override
    public void delete(Long id) {
        roleMapper.deleteById(id);
    }

    private Role toDomain(RoleDO row) {
        Role role = new Role();
        role.setId(row.getId());
        role.setWorkspaceId(row.getWorkspaceId());
        role.setRoleCode(row.getRoleCode());
        role.setRoleName(row.getRoleName());
        role.setDescription(row.getDescription());
        role.setBuiltIn(row.getBuiltIn());
        return role;
    }
}
