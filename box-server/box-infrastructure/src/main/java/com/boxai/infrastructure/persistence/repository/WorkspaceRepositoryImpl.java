package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.infrastructure.persistence.entity.RoleDO;
import com.boxai.infrastructure.persistence.entity.WorkspaceDO;
import com.boxai.infrastructure.persistence.entity.WorkspaceMemberDO;
import com.boxai.infrastructure.persistence.mapper.RoleMapper;
import com.boxai.infrastructure.persistence.mapper.WorkspaceMapper;
import com.boxai.infrastructure.persistence.mapper.WorkspaceMemberMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkspaceRepositoryImpl implements WorkspaceRepository {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper memberMapper;
    private final RoleMapper roleMapper;

    public WorkspaceRepositoryImpl(WorkspaceMapper workspaceMapper, WorkspaceMemberMapper memberMapper, RoleMapper roleMapper) {
        this.workspaceMapper = workspaceMapper;
        this.memberMapper = memberMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public Workspace save(Workspace workspace) {
        WorkspaceDO row = new WorkspaceDO();
        row.setTenantId(workspace.getTenantId());
        row.setName(workspace.getName());
        row.setSlug(workspace.getSlug());
        row.setDescription(workspace.getDescription());
        row.setAvatarUrl(workspace.getAvatarUrl());
        row.setOwnerId(workspace.getOwnerId());
        row.setStatus(workspace.getStatus() == null ? 1 : workspace.getStatus());
        row.setCreatedBy(workspace.getOwnerId());
        row.setUpdatedBy(workspace.getOwnerId());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        workspaceMapper.insert(row);
        workspace.setId(row.getId());
        return workspace;
    }

    @Override
    public void update(Workspace workspace) {
        WorkspaceDO row = workspaceMapper.selectOneById(workspace.getId());
        if (row == null) {
            return;
        }
        row.setName(workspace.getName());
        row.setSlug(workspace.getSlug());
        row.setDescription(workspace.getDescription());
        row.setAvatarUrl(workspace.getAvatarUrl());
        row.setStatus(workspace.getStatus() == null ? 1 : workspace.getStatus());
        row.setUpdatedBy(workspace.getOwnerId());
        row.setUpdatedAt(LocalDateTime.now());
        workspaceMapper.update(row);
    }

    @Override
    public void deleteById(Long id) {
        workspaceMapper.deleteById(id);
    }

    @Override
    public Optional<Workspace> findById(Long id) {
        return Optional.ofNullable(workspaceMapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Workspace> findBySlug(String slug) {
        WorkspaceDO row = workspaceMapper.selectOneByQuery(QueryWrapper.create().eq("slug", slug));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public List<WorkspaceMember> listMembersByUserId(Long userId) {
        List<WorkspaceMemberDO> rows = memberMapper.selectListByQuery(QueryWrapper.create().eq("user_id", userId).eq("status", 1));
        List<WorkspaceMember> result = new ArrayList<>();
        for (WorkspaceMemberDO row : rows) {
            WorkspaceDO workspace = workspaceMapper.selectOneById(row.getWorkspaceId());
            if (workspace == null || workspace.getStatus() == null || workspace.getStatus() != 1) {
                continue;
            }
            WorkspaceMember member = toMember(row);
            member.setWorkspaceName(workspace.getName());
            member.setWorkspaceSlug(workspace.getSlug());
            member.setWorkspaceDescription(workspace.getDescription());
            member.setWorkspaceAvatarUrl(workspace.getAvatarUrl());
            member.setWorkspaceStatus(workspace.getStatus());
            Optional.ofNullable(roleMapper.selectOneById(row.getRoleId())).ifPresent(role -> member.setRoleCode(role.getRoleCode()));
            result.add(member);
        }
        return result;
    }

    @Override
    public List<WorkspaceMember> listMembersByWorkspaceId(Long workspaceId) {
        List<WorkspaceMemberDO> rows = memberMapper.selectListByQuery(
                QueryWrapper.create().eq("workspace_id", workspaceId).eq("status", 1));
        List<WorkspaceMember> result = new ArrayList<>();
        for (WorkspaceMemberDO row : rows) {
            WorkspaceMember member = toMember(row);
            RoleDO role = roleMapper.selectOneById(row.getRoleId());
            if (role != null) {
                member.setRoleCode(role.getRoleCode());
            }
            result.add(member);
        }
        return result;
    }

    @Override
    public WorkspaceMember addMember(WorkspaceMember member) {
        WorkspaceMemberDO row = new WorkspaceMemberDO();
        row.setWorkspaceId(member.getWorkspaceId());
        row.setUserId(member.getUserId());
        row.setRoleId(member.getRoleId());
        row.setStatus(1);
        row.setJoinedAt(LocalDateTime.now());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setCreatedBy(member.getUserId());
        row.setUpdatedBy(member.getUserId());
        row.setDeleted(0);
        memberMapper.insert(row);
        member.setId(row.getId());
        return member;
    }

    @Override
    public void updateMember(WorkspaceMember member) {
        WorkspaceMemberDO row = memberMapper.selectOneById(member.getId());
        if (row == null) {
            return;
        }
        row.setRoleId(member.getRoleId());
        row.setStatus(member.getStatus());
        row.setUpdatedAt(LocalDateTime.now());
        memberMapper.update(row);
    }

    @Override
    public void removeMember(Long workspaceId, Long userId) {
        WorkspaceMemberDO row = memberMapper.selectOneByQuery(
                QueryWrapper.create().eq("workspace_id", workspaceId).eq("user_id", userId));
        if (row != null) {
            memberMapper.deleteById(row.getId());
        }
    }

    @Override
    public Optional<WorkspaceMember> findMember(Long workspaceId, Long userId) {
        WorkspaceMemberDO row = memberMapper.selectOneByQuery(
                QueryWrapper.create().eq("workspace_id", workspaceId).eq("user_id", userId));
        if (row == null) {
            return Optional.empty();
        }
        WorkspaceMember member = toMember(row);
        RoleDO role = roleMapper.selectOneById(row.getRoleId());
        if (role != null) {
            member.setRoleCode(role.getRoleCode());
        }
        return Optional.of(member);
    }

    @Override
    public int countByTenantId(Long tenantId) {
        Long count = workspaceMapper.selectCountByQuery(
                QueryWrapper.create().eq("tenant_id", tenantId).eq("status", 1));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public List<Workspace> listByTenantId(Long tenantId) {
        return workspaceMapper.selectListByQuery(
                        QueryWrapper.create().eq("tenant_id", tenantId).orderBy("updated_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Workspace toDomain(WorkspaceDO row) {
        Workspace workspace = new Workspace();
        workspace.setId(row.getId());
        workspace.setTenantId(row.getTenantId());
        workspace.setName(row.getName());
        workspace.setSlug(row.getSlug());
        workspace.setDescription(row.getDescription());
        workspace.setAvatarUrl(row.getAvatarUrl());
        workspace.setOwnerId(row.getOwnerId());
        workspace.setStatus(row.getStatus());
        workspace.setCreatedAt(row.getCreatedAt());
        return workspace;
    }

    private WorkspaceMember toMember(WorkspaceMemberDO row) {
        WorkspaceMember member = new WorkspaceMember();
        member.setId(row.getId());
        member.setWorkspaceId(row.getWorkspaceId());
        member.setUserId(row.getUserId());
        member.setRoleId(row.getRoleId());
        member.setStatus(row.getStatus());
        return member;
    }
}
