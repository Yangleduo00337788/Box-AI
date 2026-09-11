package com.boxai.domain.user;

import java.util.List;

public interface UserSidebarPinRepository {

    List<UserSidebarPin> listByUserAndWorkspace(Long userId, Long workspaceId);

    void replaceAll(Long userId, Long workspaceId, List<UserSidebarPin> pins);
}
