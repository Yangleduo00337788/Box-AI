package com.boxai.domain.workspace;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository {

    Workspace save(Workspace workspace);

    Optional<Workspace> findById(Long id);

    Optional<Workspace> findBySlug(String slug);

    List<WorkspaceMember> listMembersByUserId(Long userId);

    WorkspaceMember addMember(WorkspaceMember member);

    Optional<WorkspaceMember> findMember(Long workspaceId, Long userId);

    int countByTenantId(Long tenantId);
}
