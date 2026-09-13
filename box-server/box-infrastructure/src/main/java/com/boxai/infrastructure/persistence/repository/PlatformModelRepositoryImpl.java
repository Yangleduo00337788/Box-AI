package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.platform.PlatformModel;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.infrastructure.persistence.entity.PlatformModelDO;
import com.boxai.infrastructure.persistence.mapper.PlatformModelMapper;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PlatformModelRepositoryImpl implements PlatformModelRepository {

    private final PlatformModelMapper mapper;

    public PlatformModelRepositoryImpl(PlatformModelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PlatformModel save(PlatformModel model) {
        PlatformModelDO row = toDo(model);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        model.setId(row.getId());
        model.setCreatedAt(row.getCreatedAt());
        model.setUpdatedAt(row.getUpdatedAt());
        return model;
    }

    @Override
    public void update(PlatformModel model) {
        PlatformModelDO row = toDo(model);
        row.setId(model.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        model.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Optional<PlatformModel> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<PlatformModel> findByProviderAndCode(Long providerId, String modelCode) {
        if (providerId == null || modelCode == null || modelCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectOneByQuery(providerCodeQuery(providerId, modelCode)))
                .map(this::toDomain);
    }

    @Override
    public Optional<PlatformModel> findByProviderAndCodeIncludingDeleted(Long providerId, String modelCode) {
        if (providerId == null || modelCode == null || modelCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LogicDeleteManager.execWithoutLogicDelete(() ->
                        mapper.selectOneByQuery(providerCodeQuery(providerId, modelCode))))
                .map(this::toDomain);
    }

    @Override
    public void restore(Long id) {
        LogicDeleteManager.execWithoutLogicDelete(() -> {
            PlatformModelDO row = mapper.selectOneById(id);
            if (row == null) {
                return null;
            }
            row.setDeleted(0);
            row.setStatus(1);
            row.setUpdatedAt(LocalDateTime.now());
            mapper.update(row);
            return null;
        });
    }

    private static QueryWrapper providerCodeQuery(Long providerId, String modelCode) {
        return QueryWrapper.create()
                .eq("provider_id", providerId)
                .eq("model_code", modelCode.trim());
    }

    @Override
    public List<PlatformModel> listActive() {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("status", 1).orderBy("sort_order", false).orderBy("id", true))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<PlatformModel> listAll() {
        return mapper.selectListByQuery(QueryWrapper.create().orderBy("sort_order", false).orderBy("id", true))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<PlatformModel> listByProvider(Long providerId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("provider_id", providerId).orderBy("sort_order", false).orderBy("id", true))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void updateLimits(Long id, Integer contextWindow, Integer maxOutputTokens) {
        UpdateChain<PlatformModelDO> chain = UpdateChain.of(PlatformModelDO.class);
        chain.set("updated_at", LocalDateTime.now());
        if (contextWindow == null) {
            chain.setRaw("context_window", "NULL");
        } else {
            chain.set("context_window", contextWindow);
        }
        if (maxOutputTokens == null) {
            chain.setRaw("max_output_tokens", "NULL");
        } else {
            chain.set("max_output_tokens", maxOutputTokens);
        }
        chain.where("id = ?", id).update();
    }

    @Override
    public void deleteByProvider(Long providerId) {
        mapper.deleteByQuery(QueryWrapper.create().eq("provider_id", providerId));
    }

    private PlatformModel toDomain(PlatformModelDO row) {
        PlatformModel model = new PlatformModel();
        model.setId(row.getId());
        model.setProviderId(row.getProviderId());
        model.setModelCode(row.getModelCode());
        model.setModelName(row.getModelName());
        model.setDescription(row.getDescription());
        model.setModelType(row.getModelType());
        model.setSupportStreaming(row.getSupportStreaming() != null && row.getSupportStreaming() == 1);
        model.setContextWindow(row.getContextWindow());
        model.setMaxOutputTokens(row.getMaxOutputTokens());
        model.setSortOrder(row.getSortOrder());
        model.setStatus(row.getStatus());
        model.setDeleted(row.getDeleted());
        model.setCreatedAt(row.getCreatedAt());
        model.setUpdatedAt(row.getUpdatedAt());
        return model;
    }

    private PlatformModelDO toDo(PlatformModel model) {
        PlatformModelDO row = new PlatformModelDO();
        row.setProviderId(model.getProviderId());
        row.setModelCode(model.getModelCode());
        row.setModelName(model.getModelName());
        row.setDescription(model.getDescription());
        row.setModelType(model.getModelType() == null ? "CHAT" : model.getModelType());
        row.setSupportStreaming(model.getSupportStreaming() == null || model.getSupportStreaming() ? 1 : 0);
        row.setContextWindow(model.getContextWindow());
        row.setMaxOutputTokens(model.getMaxOutputTokens());
        row.setSortOrder(model.getSortOrder() == null ? 0 : model.getSortOrder());
        row.setStatus(model.getStatus() == null ? 1 : model.getStatus());
        return row;
    }
}
