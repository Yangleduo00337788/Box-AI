package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.tool.ToolDatabaseConfig;
import com.boxai.domain.tool.ToolDatabaseConfigRepository;
import com.boxai.infrastructure.persistence.entity.ToolDatabaseConfigDO;
import com.boxai.infrastructure.persistence.mapper.ToolDatabaseConfigMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ToolDatabaseConfigRepositoryImpl implements ToolDatabaseConfigRepository {

    private final ToolDatabaseConfigMapper mapper;

    public ToolDatabaseConfigRepositoryImpl(ToolDatabaseConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ToolDatabaseConfig save(ToolDatabaseConfig config) {
        ToolDatabaseConfigDO row = toDo(config);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.insert(row);
        config.setId(row.getId());
        config.setCreatedAt(row.getCreatedAt());
        config.setUpdatedAt(row.getUpdatedAt());
        return config;
    }

    @Override
    public void update(ToolDatabaseConfig config) {
        ToolDatabaseConfigDO row = toDo(config);
        row.setId(config.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        config.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<ToolDatabaseConfig> findByToolId(Long toolId) {
        return Optional.ofNullable(
                        mapper.selectOneByQuery(QueryWrapper.create().eq("tool_id", toolId)))
                .map(this::toDomain);
    }

    @Override
    public void deleteByToolId(Long toolId) {
        mapper.deleteByQuery(QueryWrapper.create().eq("tool_id", toolId));
    }

    private ToolDatabaseConfig toDomain(ToolDatabaseConfigDO row) {
        ToolDatabaseConfig config = new ToolDatabaseConfig();
        config.setId(row.getId());
        config.setToolId(row.getToolId());
        config.setDatabaseType(row.getDatabaseType());
        config.setHost(row.getHost());
        config.setPort(row.getPort());
        config.setDatabaseName(row.getDatabaseName());
        config.setUsername(row.getUsername());
        config.setPasswordCiphertext(row.getPasswordCiphertext());
        config.setAllowedOperationsJson(row.getAllowedOperations());
        config.setMaxRows(row.getMaxRows());
        config.setTimeoutMs(row.getTimeoutMs());
        config.setCreatedAt(row.getCreatedAt());
        config.setUpdatedAt(row.getUpdatedAt());
        return config;
    }

    private ToolDatabaseConfigDO toDo(ToolDatabaseConfig config) {
        ToolDatabaseConfigDO row = new ToolDatabaseConfigDO();
        row.setToolId(config.getToolId());
        row.setDatabaseType(config.getDatabaseType());
        row.setHost(config.getHost());
        row.setPort(config.getPort());
        row.setDatabaseName(config.getDatabaseName());
        row.setUsername(config.getUsername());
        row.setPasswordCiphertext(config.getPasswordCiphertext());
        row.setAllowedOperations(config.getAllowedOperationsJson());
        row.setMaxRows(config.getMaxRows());
        row.setTimeoutMs(config.getTimeoutMs());
        return row;
    }
}
