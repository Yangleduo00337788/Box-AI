package com.boxai.domain.invitation;

import java.util.List;
import java.util.Optional;

public interface WorkspaceInvitationRepository {

    WorkspaceInvitation save(WorkspaceInvitation invitation);

    void update(WorkspaceInvitation invitation);

    Optional<WorkspaceInvitation> findByToken(String token);

    Optional<WorkspaceInvitation> findById(Long id);

    List<WorkspaceInvitation> listPendingByWorkspace(Long workspaceId);

    List<WorkspaceInvitation> listByEmail(String email);
}
