package com.boxai.domain.model;

import java.util.List;
import java.util.Optional;

public interface ModelDefinitionRepository {

    ModelDefinition save(ModelDefinition model);

    void update(ModelDefinition model);

    Optional<ModelDefinition> findById(Long id);

    List<ModelDefinition> listByProviderIds(List<Long> providerIds);

    void delete(Long id);
}
