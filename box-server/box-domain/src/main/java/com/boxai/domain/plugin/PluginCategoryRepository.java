package com.boxai.domain.plugin;

import java.util.List;
import java.util.Optional;

public interface PluginCategoryRepository {

    List<PluginCategory> listActive();

    List<PluginCategory> listAll();

    Optional<PluginCategory> findByCode(String categoryCode);

    boolean existsActive(String categoryCode);

    void save(PluginCategory category);

    void update(PluginCategory category);

    void delete(String categoryCode);
}
