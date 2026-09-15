package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.invitation.WorkspaceInvitation;
import com.boxai.domain.invitation.WorkspaceInvitationRepository;
import com.boxai.infrastructure.persistence.entity.WorkspaceInvitationDO;
import com.boxai.infrastructure.persistence.mapper.WorkspaceInvitationMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class WorkspaceInvitationRepositoryImpl implements WorkspaceInvitationRepository {

    private final WorkspaceInvitationMapper workspaceInvitationMapper;

    public WorkspaceInvitationRepositoryImpl(WorkspaceInvitationMapper workspaceInvitationMapper) {
        this.workspaceInvitationMapper = workspaceInvitationMapper;
    }

    @Override
    public WorkspaceInvitation save(WorkspaceInvitation invitation) {
        WorkspaceInvitationDO row = toDo(invitation);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        workspaceInvitationMapper.insert(row);
        invitation.setId(row.getId());
        invitation.setCreatedAt(row.getCreatedAt());
        invitation.setUpdatedAt(row.getUpdatedAt());
        return invitation;
    }

    @Override
    public void update(WorkspaceInvitation invitation) {
        WorkspaceInvitationDO row = toDo(invitation);
        row.setId(invitation.getId());
        row.setUpdatedAt(LocalDateTime.now());
        workspaceInvitationMapper.update(row);
        invitation.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<WorkspaceInvitation> findByToken(String token) {
        return Optional.ofNullable(workspaceInvitationMapper.selectOneByQuery(
                        QueryWrapper.create().eq("token", token)))
                .map(this::toDomain);
    }

    @Override
    public Optional<WorkspaceInvitation> findById(Long id) {
        return Optional.ofNullable(workspaceInvitationMapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<WorkspaceInvitation> listPendingByWorkspace(Long workspaceId) {
        return workspaceInvitationMapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("workspace_id", workspaceId)
                                .eq("status", "PENDING")
                                .orderBy("id", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WorkspaceInvitation> listByEmail(String email) {
        return workspaceInvitationMapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("email", email.trim().toLowerCase(Locale.ROOT))
                                .orderBy("id", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private WorkspaceInvitation toDomain(WorkspaceInvitationDO row) {
        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setId(row.getId());
        invitation.setWorkspaceId(row.getWorkspaceId());
        invitation.setTenantId(row.getTenantId());
        invitation.setEmail(row.getEmail());
        invitation.setRoleCode(row.getRoleCode());
        invitation.setToken(row.getToken());
        invitation.setStatus(row.getStatus());
        invitation.setInvitedBy(row.getInvitedBy());
        invitation.setExpiresAt(row.getExpiresAt());
        invitation.setAcceptedAt(row.getAcceptedAt());
        invitation.setAcceptedUserId(row.getAcceptedUserId());
        invitation.setCreatedAt(row.getCreatedAt());
        invitation.setUpdatedAt(row.getUpdatedAt());
        return invitation;
    }

    private WorkspaceInvitationDO toDo(WorkspaceInvitation invitation) {
        WorkspaceInvitationDO row = new WorkspaceInvitationDO();
        row.setWorkspaceId(invitation.getWorkspaceId());
        row.setTenantId(invitation.getTenantId());
        row.setEmail(invitation.getEmail());
        row.setRoleCode(invitation.getRoleCode());
        row.setToken(invitation.getToken());
        row.setStatus(invitation.getStatus());
        row.setInvitedBy(invitation.getInvitedBy());
        row.setExpiresAt(invitation.getExpiresAt());
        row.setAcceptedAt(invitation.getAcceptedAt());
        row.setAcceptedUserId(invitation.getAcceptedUserId());
        return row;
    }
}
