package com.boxai.domain.user;

import java.util.Optional;

public interface UserWorkspaceSelectionRepository {

    Optional<UserWorkspaceSelection> findByUserAndWorkspace(Long userId, Long workspaceId);

    void save(UserWorkspaceSelection selection);

    void update(UserWorkspaceSelection selection);

    void saveOrUpdate(UserWorkspaceSelection selection);
}
