package com.boxai.domain.plugin;

import java.util.List;
import java.util.Optional;

public interface PluginCatalogRepository {

    Optional<PluginCatalog> findById(Long id);

    Optional<PluginCatalog> findByCode(String pluginCode);

    List<PluginCatalog> listByCategory(String category);

    List<PluginCatalog> listAllForAdmin();

    List<PluginCatalog> searchByTitle(String keyword, int limit);

    PluginCatalog save(PluginCatalog plugin);

    void update(PluginCatalog plugin);

    void incrementInstallCount(Long id);

    void decrementInstallCount(Long id);
}
