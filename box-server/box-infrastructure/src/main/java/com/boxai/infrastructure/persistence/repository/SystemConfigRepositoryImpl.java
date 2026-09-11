package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import com.boxai.infrastructure.persistence.entity.SystemConfigDO;
import com.boxai.infrastructure.persistence.mapper.SystemConfigMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SystemConfigRepositoryImpl implements SystemConfigRepository {

    private final SystemConfigMapper mapper;

    public SystemConfigRepositoryImpl(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<SystemConfig> findByKey(String configKey) {
        return Optional.ofNullable(mapper.selectOneById(configKey)).map(this::toDomain);
    }

    @Override
    public List<SystemConfig> findByKeys(List<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return List.of();
        }
        return mapper.selectListByQuery(QueryWrapper.create().in("config_key", configKeys))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SystemConfig> listAll() {
        return mapper.selectListByQuery(QueryWrapper.create().orderBy("config_key", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void upsert(SystemConfig config) {
        SystemConfigDO row = toDo(config);
        if (mapper.selectOneById(config.getConfigKey()) == null) {
            row.setCreatedAt(java.time.LocalDateTime.now());
            row.setUpdatedAt(row.getCreatedAt());
            mapper.insert(row);
        } else {
            row.setUpdatedAt(java.time.LocalDateTime.now());
            mapper.update(row);
        }
    }

    private SystemConfigDO toDo(SystemConfig config) {
        SystemConfigDO row = new SystemConfigDO();
        row.setConfigKey(config.getConfigKey());
        row.setConfigValue(config.getConfigValue());
        row.setDescription(config.getDescription());
        return row;
    }

    private SystemConfig toDomain(SystemConfigDO row) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(row.getConfigKey());
        config.setConfigValue(row.getConfigValue());
        config.setDescription(row.getDescription());
        config.setCreatedAt(row.getCreatedAt());
        config.setUpdatedAt(row.getUpdatedAt());
        return config;
    }
}
