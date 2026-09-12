package com.boxai.domain.workspace;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository {

    Workspace save(Workspace workspace);

    Optional<Workspace> findById(Long id);

    Optional<Workspace> findBySlug(String slug);

    List<WorkspaceMember> listMembersByUserId(Long userId);

    List<WorkspaceMember> listMembersByWorkspaceId(Long workspaceId);

    WorkspaceMember addMember(WorkspaceMember member);

    void updateMember(WorkspaceMember member);

    void removeMember(Long workspaceId, Long userId);

    Optional<WorkspaceMember> findMember(Long workspaceId, Long userId);

    int countByTenantId(Long tenantId);

    List<Workspace> listByTenantId(Long tenantId);
}
