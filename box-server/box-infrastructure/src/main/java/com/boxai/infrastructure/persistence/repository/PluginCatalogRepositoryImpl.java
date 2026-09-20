package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.infrastructure.persistence.entity.PluginCatalogDO;
import com.boxai.infrastructure.persistence.mapper.PluginCatalogMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PluginCatalogRepositoryImpl implements PluginCatalogRepository {

    private final PluginCatalogMapper mapper;

    public PluginCatalogRepositoryImpl(PluginCatalogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<PluginCatalog> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<PluginCatalog> findByCode(String pluginCode) {
        if (pluginCode == null || pluginCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectOneByQuery(QueryWrapper.create()
                .eq("plugin_code", pluginCode.trim()))).map(this::toDomain);
    }

    @Override
    public List<PluginCatalog> listByCategory(Long workspaceId, String category) {
        QueryWrapper query = consumerVisibleQuery(workspaceId)
                .orderBy("sort_order", false)
                .orderBy("id", true);
        if (category != null && !category.isBlank()) {
            query.eq("category", category.trim());
        }
        return mapper.selectListByQuery(query).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PluginCatalog> listAllForAdmin() {
        return mapper.selectListByQuery(QueryWrapper.create()
                        .orderBy("sort_order", false)
                        .orderBy("id", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PluginCatalog> searchByTitle(Long workspaceId, String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return mapper.selectListByQuery(consumerVisibleQuery(workspaceId)
                        .and("(title LIKE ? OR description LIKE ?)", "%" + keyword.trim() + "%", "%" + keyword.trim() + "%")
                        .orderBy("sort_order", false)
                        .orderBy("id", true)
                        .limit(limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByCategory(String category) {
        if (category == null || category.isBlank()) {
            return 0;
        }
        return mapper.selectCountByQuery(QueryWrapper.create().eq("category", category.trim()));
    }

    @Override
    public PluginCatalog save(PluginCatalog plugin) {
        PluginCatalogDO row = toDo(plugin);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        if (row.getInstallCount() == null) {
            row.setInstallCount(0);
        }
        mapper.insert(row);
        plugin.setId(row.getId());
        plugin.setCreatedAt(row.getCreatedAt());
        plugin.setUpdatedAt(row.getUpdatedAt());
        return plugin;
    }

    @Override
    public void update(PluginCatalog plugin) {
        PluginCatalogDO row = toDo(plugin);
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        plugin.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void incrementInstallCount(Long id) {
        UpdateChain.of(PluginCatalogDO.class)
                .setRaw("install_count", "install_count + 1")
                .set("updated_at", LocalDateTime.now())
                .where("id = ?", id)
                .update();
    }

    @Override
    public void decrementInstallCount(Long id) {
        UpdateChain.of(PluginCatalogDO.class)
                .setRaw("install_count", "GREATEST(install_count - 1, 0)")
                .set("updated_at", LocalDateTime.now())
                .where("id = ?", id)
                .update();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private QueryWrapper consumerVisibleQuery(Long workspaceId) {
        long workspace = workspaceId == null ? -1L : workspaceId;
        return QueryWrapper.create()
                .eq("status", "LISTED")
                .and(
                        "( (IFNULL(source_type, 'ADMIN') = 'ADMIN' AND review_status = 'APPROVED')"
                                + " OR (source_type = 'USER' AND submitted_workspace_id = ?) )",
                        workspace);
    }

    private PluginCatalogDO toDo(PluginCatalog plugin) {
        PluginCatalogDO row = new PluginCatalogDO();
        row.setId(plugin.getId());
        row.setPluginCode(plugin.getPluginCode());
        row.setCategory(plugin.getCategory());
        row.setTitle(plugin.getTitle());
        row.setDescription(plugin.getDescription());
        row.setManifestJson(plugin.getManifestJson());
        row.setStatus(plugin.getStatus());
        row.setReviewStatus(plugin.getReviewStatus() == null ? "PENDING_REVIEW" : plugin.getReviewStatus());
        row.setSourceType(plugin.getSourceType() == null ? "ADMIN" : plugin.getSourceType());
        row.setSubmittedBy(plugin.getSubmittedBy());
        row.setSubmittedWorkspaceId(plugin.getSubmittedWorkspaceId());
        row.setVisibility(plugin.getVisibility() == null ? "GLOBAL" : plugin.getVisibility());
        row.setTenantIdsJson(plugin.getTenantIdsJson());
        row.setRolloutPercent(plugin.getRolloutPercent() == null ? 100 : plugin.getRolloutPercent());
        row.setSortOrder(plugin.getSortOrder());
        row.setInstallCount(plugin.getInstallCount());
        return row;
    }

    private PluginCatalog toDomain(PluginCatalogDO row) {
        PluginCatalog plugin = new PluginCatalog();
        plugin.setId(row.getId());
        plugin.setPluginCode(row.getPluginCode());
        plugin.setCategory(row.getCategory());
        plugin.setTitle(row.getTitle());
        plugin.setDescription(row.getDescription());
        plugin.setManifestJson(row.getManifestJson());
        plugin.setStatus(row.getStatus());
        plugin.setReviewStatus(row.getReviewStatus());
        plugin.setSourceType(row.getSourceType() == null ? "ADMIN" : row.getSourceType());
        plugin.setSubmittedBy(row.getSubmittedBy());
        plugin.setSubmittedWorkspaceId(row.getSubmittedWorkspaceId());
        plugin.setVisibility(row.getVisibility());
        plugin.setTenantIdsJson(row.getTenantIdsJson());
        plugin.setRolloutPercent(row.getRolloutPercent());
        plugin.setSortOrder(row.getSortOrder());
        plugin.setInstallCount(row.getInstallCount());
        plugin.setCreatedAt(row.getCreatedAt());
        plugin.setUpdatedAt(row.getUpdatedAt());
        return plugin;
    }
}
