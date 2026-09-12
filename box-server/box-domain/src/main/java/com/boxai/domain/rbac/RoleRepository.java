package com.boxai.domain.rbac;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findByWorkspaceAndCode(Long workspaceId, String roleCode);

    Optional<Role> findById(Long id);

    List<Role> listByWorkspace(Long workspaceId);

    Role save(Role role);

    void update(Role role);

    void delete(Long id);
}
