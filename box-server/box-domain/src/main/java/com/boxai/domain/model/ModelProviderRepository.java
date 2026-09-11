package com.boxai.domain.model;

import java.util.List;
import java.util.Optional;

public interface ModelProviderRepository {

    ModelProvider save(ModelProvider provider);

    void update(ModelProvider provider);

    Optional<ModelProvider> findById(Long id);

    List<ModelProvider> listByWorkspace(Long workspaceId);

    void delete(Long id);
}
