package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.model.ModelProvider;
import com.boxai.domain.model.ModelProviderRepository;
import com.boxai.infrastructure.persistence.entity.ModelProviderDO;
import com.boxai.infrastructure.persistence.mapper.ModelProviderMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ModelProviderRepositoryImpl implements ModelProviderRepository {

    private final ModelProviderMapper mapper;

    public ModelProviderRepositoryImpl(ModelProviderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ModelProvider save(ModelProvider provider) {
        ModelProviderDO row = toDo(provider);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        provider.setId(row.getId());
        return provider;
    }

    @Override
    public void update(ModelProvider provider) {
        ModelProviderDO row = toDo(provider);
        row.setId(provider.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
    }

    @Override
    public Optional<ModelProvider> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<ModelProvider> listByWorkspace(Long workspaceId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("workspace_id", workspaceId).orderBy("id", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private ModelProvider toDomain(ModelProviderDO row) {
        ModelProvider provider = new ModelProvider();
        provider.setId(row.getId());
        provider.setWorkspaceId(row.getWorkspaceId());
        provider.setProviderCode(row.getProviderCode());
        provider.setProviderName(row.getProviderName());
        provider.setProviderType(row.getProviderType());
        provider.setBaseUrl(row.getBaseUrl());
        provider.setStatus(row.getStatus());
        provider.setConfigJson(row.getConfigJson());
        return provider;
    }

    private ModelProviderDO toDo(ModelProvider provider) {
        ModelProviderDO row = new ModelProviderDO();
        row.setWorkspaceId(provider.getWorkspaceId());
        row.setProviderCode(provider.getProviderCode());
        row.setProviderName(provider.getProviderName());
        row.setProviderType(provider.getProviderType());
        row.setBaseUrl(provider.getBaseUrl());
        row.setStatus(provider.getStatus() == null ? 1 : provider.getStatus());
        row.setConfigJson(provider.getConfigJson());
        return row;
    }
}
