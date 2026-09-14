package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.plugin.WorkspacePluginInstall;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.boxai.infrastructure.persistence.entity.WorkspacePluginInstallDO;
import com.boxai.infrastructure.persistence.mapper.WorkspacePluginInstallMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class WorkspacePluginInstallRepositoryImpl implements WorkspacePluginInstallRepository {

    private final WorkspacePluginInstallMapper mapper;

    public WorkspacePluginInstallRepositoryImpl(WorkspacePluginInstallMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<WorkspacePluginInstall> findByWorkspaceAndPlugin(Long workspaceId, Long pluginId) {
        WorkspacePluginInstallDO row = mapper.selectOneByQuery(
                QueryWrapper.create().eq("workspace_id", workspaceId).eq("plugin_id", pluginId));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public List<WorkspacePluginInstall> listByWorkspace(Long workspaceId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("workspace_id", workspaceId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Set<Long> listInstalledPluginIds(Long workspaceId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("workspace_id", workspaceId))
                .stream()
                .map(WorkspacePluginInstallDO::getPluginId)
                .collect(Collectors.toSet());
    }

    @Override
    public long countByPluginId(Long pluginId) {
        if (pluginId == null) {
            return 0;
        }
        return mapper.selectCountByQuery(QueryWrapper.create().eq("plugin_id", pluginId));
    }

    @Override
    public WorkspacePluginInstall save(WorkspacePluginInstall install) {
        WorkspacePluginInstallDO row = new WorkspacePluginInstallDO();
        row.setWorkspaceId(install.getWorkspaceId());
        row.setPluginId(install.getPluginId());
        row.setResourceType(install.getResourceType());
        row.setResourceId(install.getResourceId());
        row.setInstalledBy(install.getInstalledBy());
        row.setInstalledAt(install.getInstalledAt() == null ? LocalDateTime.now() : install.getInstalledAt());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        install.setId(row.getId());
        install.setInstalledAt(row.getInstalledAt());
        install.setCreatedAt(row.getCreatedAt());
        install.setUpdatedAt(row.getUpdatedAt());
        return install;
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private WorkspacePluginInstall toDomain(WorkspacePluginInstallDO row) {
        WorkspacePluginInstall install = new WorkspacePluginInstall();
        install.setId(row.getId());
        install.setWorkspaceId(row.getWorkspaceId());
        install.setPluginId(row.getPluginId());
        install.setResourceType(row.getResourceType());
        install.setResourceId(row.getResourceId());
        install.setInstalledBy(row.getInstalledBy());
        install.setInstalledAt(row.getInstalledAt());
        install.setCreatedAt(row.getCreatedAt());
        install.setUpdatedAt(row.getUpdatedAt());
        return install;
    }
}
