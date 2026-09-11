package com.boxai.domain.plugin;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface WorkspacePluginInstallRepository {

    Optional<WorkspacePluginInstall> findByWorkspaceAndPlugin(Long workspaceId, Long pluginId);

    List<WorkspacePluginInstall> listByWorkspace(Long workspaceId);

    Set<Long> listInstalledPluginIds(Long workspaceId);

    WorkspacePluginInstall save(WorkspacePluginInstall install);

    void delete(Long id);
}
