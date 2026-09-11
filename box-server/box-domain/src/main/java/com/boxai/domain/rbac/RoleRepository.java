package com.boxai.domain.rbac;

import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findByWorkspaceAndCode(Long workspaceId, String roleCode);

    Role save(Role role);
}
