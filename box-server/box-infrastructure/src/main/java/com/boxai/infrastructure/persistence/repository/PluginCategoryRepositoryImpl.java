package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.plugin.PluginCategory;
import com.boxai.domain.plugin.PluginCategoryRepository;
import com.boxai.infrastructure.persistence.entity.PluginCategoryDO;
import com.boxai.infrastructure.persistence.mapper.PluginCategoryMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PluginCategoryRepositoryImpl implements PluginCategoryRepository {

    private final PluginCategoryMapper mapper;

    public PluginCategoryRepositoryImpl(PluginCategoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PluginCategory> listActive() {
        return mapper.selectListByQuery(QueryWrapper.create()
                        .eq("status", "ACTIVE")
                        .orderBy("sort_order", false)
                        .orderBy("category_code", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PluginCategory> listAll() {
        return mapper.selectListByQuery(QueryWrapper.create()
                        .orderBy("sort_order", false)
                        .orderBy("category_code", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<PluginCategory> findByCode(String categoryCode) {
        return Optional.ofNullable(mapper.selectOneById(categoryCode)).map(this::toDomain);
    }

    @Override
    public boolean existsActive(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return false;
        }
        return mapper.selectCountByQuery(QueryWrapper.create()
                .eq("category_code", categoryCode.trim())
                .eq("status", "ACTIVE")) > 0;
    }

    @Override
    public void save(PluginCategory category) {
        PluginCategoryDO row = toDo(category);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.insert(row);
        category.setCreatedAt(row.getCreatedAt());
        category.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void update(PluginCategory category) {
        PluginCategoryDO row = toDo(category);
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        category.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void delete(String categoryCode) {
        mapper.deleteById(categoryCode);
    }

    private PluginCategory toDomain(PluginCategoryDO row) {
        PluginCategory category = new PluginCategory();
        category.setCategoryCode(row.getCategoryCode());
        category.setLabel(row.getLabel());
        category.setDescription(row.getDescription());
        category.setSortOrder(row.getSortOrder());
        category.setStatus(row.getStatus());
        category.setCreatedAt(row.getCreatedAt());
        category.setUpdatedAt(row.getUpdatedAt());
        return category;
    }

    private PluginCategoryDO toDo(PluginCategory category) {
        PluginCategoryDO row = new PluginCategoryDO();
        row.setCategoryCode(category.getCategoryCode());
        row.setLabel(category.getLabel());
        row.setDescription(category.getDescription());
        row.setSortOrder(category.getSortOrder() == null ? 0 : category.getSortOrder());
        row.setStatus(category.getStatus() == null ? "ACTIVE" : category.getStatus());
        return row;
    }
}
