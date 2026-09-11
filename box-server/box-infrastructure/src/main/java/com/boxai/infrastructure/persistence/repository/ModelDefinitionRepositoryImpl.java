package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.model.ModelDefinition;
import com.boxai.domain.model.ModelDefinitionRepository;
import com.boxai.infrastructure.persistence.entity.ModelDefinitionDO;
import com.boxai.infrastructure.persistence.mapper.ModelDefinitionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ModelDefinitionRepositoryImpl implements ModelDefinitionRepository {

    private final ModelDefinitionMapper mapper;

    public ModelDefinitionRepositoryImpl(ModelDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ModelDefinition save(ModelDefinition model) {
        ModelDefinitionDO row = toDo(model);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        model.setId(row.getId());
        return model;
    }

    @Override
    public void update(ModelDefinition model) {
        ModelDefinitionDO row = toDo(model);
        row.setId(model.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
    }

    @Override
    public Optional<ModelDefinition> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<ModelDefinition> listByProviderIds(List<Long> providerIds) {
        if (providerIds == null || providerIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectListByQuery(QueryWrapper.create().in("provider_id", providerIds).orderBy("id", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private ModelDefinition toDomain(ModelDefinitionDO row) {
        ModelDefinition model = new ModelDefinition();
        model.setId(row.getId());
        model.setProviderId(row.getProviderId());
        model.setModelCode(row.getModelCode());
        model.setModelName(row.getModelName());
        model.setModelType(row.getModelType());
        model.setSupportStreaming(row.getSupportStreaming() != null && row.getSupportStreaming() == 1);
        model.setSupportToolCalling(row.getSupportToolCalling() != null && row.getSupportToolCalling() == 1);
        model.setSupportVision(row.getSupportVision() != null && row.getSupportVision() == 1);
        model.setContextWindow(row.getContextWindow());
        model.setMaxOutputTokens(row.getMaxOutputTokens());
        model.setStatus(row.getStatus());
        model.setConfigJson(row.getConfigJson());
        return model;
    }

    private ModelDefinitionDO toDo(ModelDefinition model) {
        ModelDefinitionDO row = new ModelDefinitionDO();
        row.setProviderId(model.getProviderId());
        row.setModelCode(model.getModelCode());
        row.setModelName(model.getModelName());
        row.setModelType(model.getModelType() == null ? "CHAT" : model.getModelType());
        row.setSupportStreaming(Boolean.TRUE.equals(model.getSupportStreaming()) ? 1 : 0);
        row.setSupportToolCalling(Boolean.TRUE.equals(model.getSupportToolCalling()) ? 1 : 0);
        row.setSupportVision(Boolean.TRUE.equals(model.getSupportVision()) ? 1 : 0);
        row.setContextWindow(model.getContextWindow());
        row.setMaxOutputTokens(model.getMaxOutputTokens());
        row.setConfigJson(model.getConfigJson());
        row.setStatus(model.getStatus() == null ? 1 : model.getStatus());
        return row;
    }
}
